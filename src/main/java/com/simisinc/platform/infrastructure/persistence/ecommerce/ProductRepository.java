/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simisinc.platform.infrastructure.persistence.ecommerce;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.ecommerce.ProductJSONCommand;
import com.simisinc.platform.domain.model.ecommerce.Product;
import com.simisinc.platform.domain.model.ecommerce.ProductSku;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves product objects
 *
 * @author matt rajkowski
 * @created 3/17/19 4:46 PM
 */
public class ProductRepository {

  private static Log LOG = LogFactory.getLog(ProductRepository.class);

  private static String TABLE_NAME = "products";
  private static String[] PRIMARY_KEY = new String[] { "product_id" };

  private static DataResult<Product> query(ProductSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("products.*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("products.product_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getProductUniqueId())) {
        select.AND("products.product_unique_id = ?", specification.getProductUniqueId());
      }
      if (specification.getWithProductUniqueIdList() != null && !specification.getWithProductUniqueIdList().isEmpty()) {
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < specification.getWithProductUniqueIdList().size(); i++) {
          if (i > 0) {
            placeholders.append(", ");
          }
          placeholders.append("?");
          values.add(specification.getWithProductUniqueIdList().get(i));
        }
        if (!placeholders.isEmpty()) {
          select.AND("products.product_unique_id IN (" + placeholders + ")", values.toArray());
        }
      }
      if (specification.getIsForSale() != DataConstants.UNDEFINED) {
        if (specification.getIsForSale() == DataConstants.TRUE) {
          select.AND("products.enabled = true");
          select.AND("EXISTS (SELECT 1 FROM product_skus WHERE product_id = products.product_id AND enabled = ?)", true);
        }
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(ProductRepository::buildRecord);
  }

  public static List<Product> findAll() {
    return findAll(null, null);
  }

  public static List<Product> findAll(ProductSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("product_order, name, caption");
    DataResult result = query(specification, constraints);
    List<Product> productList = (List<Product>) result.getRecords();
    for (Product product : productList) {
      populateRelatedData(product);
    }
    return productList;
  }

  public static Product findById(long id) {
    return findById(id, true);
  }

  public static Product findById(long id, boolean includeRelatedData) {
    if (id == -1) {
      return null;
    }
    Product product = DB.SELECT("products.*")
        .FROM(TABLE_NAME)
        .WHERE("product_id = ?", id)
        .returnRecord(ProductRepository::buildRecord);
    if (includeRelatedData) {
      populateRelatedData(product);
    }
    return product;
  }

  public static Product findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    Product product = DB.SELECT("products.*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(name) = ?", name.toLowerCase())
        .returnRecord(ProductRepository::buildRecord);
    populateRelatedData(product);
    return product;
  }

  public static Product findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    Product product = DB.SELECT("products.*")
        .FROM(TABLE_NAME)
        .WHERE("product_unique_id = ?", uniqueId)
        .returnRecord(ProductRepository::buildRecord);
    populateRelatedData(product);
    return product;
  }

  public static Product findBySku(String sku) {
    if (StringUtils.isBlank(sku)) {
      return null;
    }
    Product product = DB.SELECT("products.*")
        .FROM(TABLE_NAME)
        .WHERE("EXISTS (SELECT 1 FROM product_skus WHERE product_id = products.product_id AND sku = ?)", sku.toUpperCase().trim())
        .returnRecord(ProductRepository::buildRecord);
    populateRelatedData(product);
    return product;
  }

  private static void populateRelatedData(Product product) {
    if (product == null) {
      return;
    }
    ProductSkuSpecification specification = new ProductSkuSpecification();
    specification.setProductId(product.getId());
    List<ProductSku> productSKUList = ProductSkuRepository.findAll(specification, null);
    product.setProducts(productSKUList);
  }

  public static boolean remove(Product record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      ProductSkuRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("product_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The delete failed!");
    return false;
  }

  public static Product save(Product record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static Product add(Product record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("product_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("product_order", record.getOrder())
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("description", StringUtils.trimToNull(record.getDescription()))
        .FIELD("caption", StringUtils.trimToNull(record.getCaption()))
        .FIELD("is_good", record.getIsGood())
        .FIELD("is_service", record.getIsService())
        .FIELD("is_virtual", record.getIsVirtual())
        .FIELD("is_download", record.getIsDownload())
        .FIELD("fulfillment_id", record.getFulfillmentId() != -1 ? record.getFulfillmentId() : null)
        .FIELD("taxable", record.getTaxable())
        .FIELD("tax_code", StringUtils.trimToNull(record.getTaxCode()))
        .FIELD("active_date", record.getActiveDate())
        .FIELD("deactivate_on", record.getDeactivateOnDate())
        .FIELD("available_date", record.getAvailableDate())
        .FIELD("shippable", record.getShippable())
        .FIELD("package_height", record.getPackageHeight())
        .FIELD("package_length", record.getPackageLength())
        .FIELD("package_width", record.getPackageWidth())
        .FIELD("package_weight_lbs", record.getPackageWeightPounds())
        .FIELD("package_weight_ozs", record.getPackageWeightOunces())
        .FIELD("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .FIELD("product_url", StringUtils.trimToNull(record.getProductUrl()))
        .FIELD("exclude_us_states", StringUtils.trimToNull(record.getExcludeUsStates()))
        .FIELD("created_by", record.getCreatedBy() != -1 ? record.getCreatedBy() : null)
        .FIELD("modified_by", record.getModifiedBy() != -1 ? record.getModifiedBy() : null)
        .FIELD("enabled", true)
        .FIELD("sku_attributes", ProductJSONCommand.createJSONString(record), CastType.JSONB);
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      record.setId(insert.execute(connection));
      // Manage the Product SKUs
      ProductSkuRepository.saveProductSKUList(connection, record);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  public static Product update(Product record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("product_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("product_order", record.getOrder())
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("caption", StringUtils.trimToNull(record.getCaption()))
        .SET("is_good", record.getIsGood())
        .SET("is_service", record.getIsService())
        .SET("is_virtual", record.getIsVirtual())
        .SET("is_download", record.getIsDownload())
        .SET("fulfillment_id", record.getFulfillmentId() != -1 ? record.getFulfillmentId() : null)
        .SET("taxable", record.getTaxable())
        .SET("tax_code", StringUtils.trimToNull(record.getTaxCode()))
        .SET("active_date", record.getActiveDate())
        .SET("deactivate_on", record.getDeactivateOnDate())
        .SET("available_date", record.getAvailableDate())
        .SET("shippable", record.getShippable())
        .SET("package_height", record.getPackageHeight())
        .SET("package_length", record.getPackageLength())
        .SET("package_width", record.getPackageWidth())
        .SET("package_weight_lbs", record.getPackageWeightPounds())
        .SET("package_weight_ozs", record.getPackageWeightOunces())
        .SET("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .SET("product_url", StringUtils.trimToNull(record.getProductUrl()))
        .SET("exclude_us_states", StringUtils.trimToNull(record.getExcludeUsStates()))
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("sku_attributes", ProductJSONCommand.createJSONString(record), CastType.JSONB);
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      update.WHERE("product_id = ?", record.getId()).execute(connection);
      // Manage the Product SKUs
      ProductSkuRepository.saveProductSKUList(connection, record);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static boolean updateSquareCatalogIdForProductId(long productId, String squareCatalogId) {
    DB.UPDATE(TABLE_NAME)
        .SET("square_catalog_id", squareCatalogId)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("product_id = ?", productId)
        .execute();
    return true;
  }

  private static Product buildRecord(ResultSet rs) {
    try {
      Product record = new Product();
      record.setId(rs.getLong("product_id"));
      record.setOrder(rs.getInt("product_order"));
      record.setName(rs.getString("name"));
      record.setUniqueId(rs.getString("product_unique_id"));
      record.setDescription(rs.getString("description"));
      record.setCaption(rs.getString("caption"));
      record.setIsGood(rs.getBoolean("is_good"));
      record.setIsService(rs.getBoolean("is_service"));
      record.setIsVirtual(rs.getBoolean("is_virtual"));
      record.setIsDownload(rs.getBoolean("is_download"));
      record.setActiveDate(rs.getTimestamp("active_date"));
      record.setDeactivateOnDate(rs.getTimestamp("deactivate_on"));
      record.setAvailableDate(rs.getTimestamp("available_date"));
      record.setShippable(rs.getBoolean("shippable"));
      record.setPackageHeight(rs.getBigDecimal("package_height"));
      record.setPackageLength(rs.getBigDecimal("package_length"));
      record.setPackageWidth(rs.getBigDecimal("package_width"));
      record.setPackageWeightPounds(rs.getInt("package_weight_lbs"));
      record.setPackageWeightOunces(rs.getInt("package_weight_ozs"));
      ProductJSONCommand.populateFromJSONString(record, rs.getString("sku_attributes"));
      record.setImageUrl(rs.getString("image_url"));
      record.setProductUrl(rs.getString("product_url"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setTaxable(rs.getBoolean("taxable"));
      record.setTaxCode(rs.getString("tax_code"));
      record.setSquareCatalogId(rs.getString("square_catalog_id"));
      record.setFulfillmentId(DB.getInt(rs, "fulfillment_id", -1));
      record.setExcludeUsStates(rs.getString("exclude_us_states"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
