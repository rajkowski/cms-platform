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

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.cms.DateCommand;
import com.simisinc.platform.application.ecommerce.ProductInventoryCommand;
import com.simisinc.platform.application.ecommerce.ProductSkuJSONCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.ecommerce.Product;
import com.simisinc.platform.domain.model.ecommerce.ProductSku;
import com.simisinc.platform.domain.model.ecommerce.ProductSkuAttribute;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves product sku objects
 *
 * @author matt rajkowski
 * @created 3/17/19 4:46 PM
 */
public class ProductSkuRepository {

  private static Log LOG = LogFactory.getLog(ProductSkuRepository.class);

  private static String TABLE_NAME = "product_skus";
  private static String ADDITIONAL_SELECT = "products.active_date AS product_active_date," +
      "products.deactivate_on AS product_deactivate_on";
  private static String[] PRIMARY_KEY = new String[] { "sku_id" };

  private static DataResult<ProductSku> query(ProductSkuSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("product_skus.*", ADDITIONAL_SELECT)
        .FROM(TABLE_NAME)
        .LEFT_JOIN("products")
        .ON("product_skus.product_id = products.product_id")
        .WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.WHERE("sku_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getSku())) {
        select.AND("sku = ?", specification.getSku());
      }
      if (specification.getProductId() > -1) {
        select.AND("product_skus.product_id = ?", specification.getProductId());
      }
      if (StringUtils.isNotBlank(specification.getProductUniqueId())) {
        select.AND("products.product_unique_id = ?", specification.getProductUniqueId());
      }
      if (specification.getIsNotId() != -1) {
        select.AND("sku_id <> ?", specification.getIsNotId());
      }
      if (specification.getShowOnline() != DataConstants.UNDEFINED) {
        select.AND("product_skus.enabled = ?", specification.getShowOnline());
      }
      if (specification.getWithProductSkuAttributeList() != null && !specification.getWithProductSkuAttributeList().isEmpty()) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (ProductSkuAttribute skuAttribute : specification.getWithProductSkuAttributeList()) {
          if (count > 0) {
            sb.append(",");
          }
          ++count;
          sb.append("{");
          sb.append("\"").append("name").append("\"").append(":").append("\"").append(JsonCommand.toJson(skuAttribute.getName()))
              .append("\"").append(",");
          sb.append("\"").append("value").append("\"").append(":").append("\"").append(JsonCommand.toJson(skuAttribute.getValue()))
              .append("\"");
          sb.append("}");
        }
        if (!sb.isEmpty()) {
          select.AND("attributes @> ?::jsonb", "[" + sb + "]");
        }
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(ProductSkuRepository::buildRecord);
  }

  public static List<ProductSku> findAll(ProductSkuSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("sku_order, sku");
    return query(specification, constraints).getRecords();
  }

  public static ProductSku findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("product_skus.*", ADDITIONAL_SELECT)
        .FROM(TABLE_NAME)
        .LEFT_JOIN("products")
        .ON("product_skus.product_id = products.product_id")
        .WHERE("sku_id = ?", id)
        .returnRecord(ProductSkuRepository::buildRecord);
  }

  public static List<ProductSku> findAllByProductId(long productId) {
    if (productId == -1) {
      return null;
    }
    return DB.SELECT("product_skus.*", ADDITIONAL_SELECT)
        .FROM(TABLE_NAME)
        .LEFT_JOIN("products")
        .ON("product_skus.product_id = products.product_id")
        .WHERE("product_skus.product_id = ?", productId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("sku_id").setUseCount(false))
        .returnDataResult(ProductSkuRepository::buildRecord).getRecords();
  }

  public static void saveProductSKUList(Connection connection, Product product) throws SQLException {
    if (product.getProducts() == null) {
      return;
    }
    for (ProductSku record : product.getProducts()) {
      // Pass values from the product
      record.setProductId(product.getId());
      record.setCreatedBy(product.getCreatedBy());
      record.setModifiedBy(product.getModifiedBy());
      // Determine the action
      if (StringUtils.isBlank(record.getSku())) {
        if (record.getId() > -1) {
          // @Delete the SKU
          // Skip it
        }
        continue;
      }
      save(connection, record);
    }
  }

  public static void remove(ProductSku record) {
    DB.DELETE().FROM(TABLE_NAME).WHERE("sku_id = ?", record.getId()).execute();
  }

  public static void removeAll(Connection connection, Product product) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("product_id = ?", product.getId()).execute(connection);
  }

  public static ProductSku save(Connection connection, ProductSku record) throws SQLException {
    if (record.getId() > -1) {
      return update(connection, record);
    }
    return add(connection, record);
  }

  public static ProductSku add(Connection connection, ProductSku record) throws SQLException {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("product_id", record.getProductId())
        .FIELD("sku", StringUtils.trimToNull(record.getSku()))
        .FIELD("currency", record.getCurrency())
        .FIELD("price", record.getPrice())
        .FIELD("strike_price", record.getStrikePrice())
        .FIELD("cost_of_good", record.getCostOfGood())
        .FIELD("barcode", record.getBarcode())
        .FIELD("active_date", record.getActiveDate())
        .FIELD("deactivate_on", record.getDeactivateOnDate())
        .FIELD("available_date", record.getAvailableDate())
        .FIELD("inventory_qty", record.getInventoryQty())
        .FIELD("inventory_qty_low", record.getInventoryLow())
        .FIELD("inventory_qty_incoming", record.getInventoryIncoming())
        .FIELD("minimum_purchase_qty", record.getMinimumPurchaseQty())
        .FIELD("maximum_purchase_qty", record.getMaximumPurchaseQty())
        .FIELD("allow_backorders", record.getAllowBackorders())
        .FIELD("package_height", record.getPackageHeight())
        .FIELD("package_length", record.getPackageLength())
        .FIELD("package_width", record.getPackageWidth())
        .FIELD("package_weight_lbs", record.getPackageWeightPounds())
        .FIELD("package_weight_ozs", record.getPackageWeightOunces())
        .FIELD("enabled", record.getEnabled())
        .FIELD("created_by", record.getCreatedBy() != -1 ? record.getCreatedBy() : null)
        .FIELD("modified_by", record.getModifiedBy() != -1 ? record.getModifiedBy() : null)
        .FIELD("attributes", ProductSkuJSONCommand.createJSONString(record), CastType.JSONB);
    record.setId(insert.execute(connection));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static ProductSku update(Connection connection, ProductSku record) throws SQLException {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("sku", StringUtils.trimToNull(record.getSku()))
        .SET("currency", record.getCurrency())
        .SET("price", record.getPrice())
        .SET("strike_price", record.getStrikePrice())
        .SET("cost_of_good", record.getCostOfGood())
        .SET("barcode", record.getBarcode())
        .SET("active_date", record.getActiveDate())
        .SET("deactivate_on", record.getDeactivateOnDate())
        .SET("available_date", record.getAvailableDate())
        .SET("inventory_qty_low", record.getInventoryLow())
        .SET("inventory_qty_incoming", record.getInventoryIncoming())
        .SET("minimum_purchase_qty", record.getMinimumPurchaseQty())
        .SET("maximum_purchase_qty", record.getMaximumPurchaseQty())
        .SET("allow_backorders", record.getAllowBackorders())
        .SET("package_height", record.getPackageHeight())
        .SET("package_length", record.getPackageLength())
        .SET("package_width", record.getPackageWidth())
        .SET("package_weight_lbs", record.getPackageWeightPounds())
        .SET("package_weight_ozs", record.getPackageWeightOunces())
        .SET("enabled", record.getEnabled())
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("attributes", ProductSkuJSONCommand.createJSONString(record), CastType.JSONB);
    // Determine if Qty State is being used
    if (record.getInventoryQtyState() > -1) {
      // The update will use an offset, not a setter for the inventory in case an order
      // modifies the available amount
      int difference = record.getInventoryQty() - record.getInventoryQtyState();
      updateInventoryCount(connection, record.getId(), difference);
    } else {
      // state is not being used, so set the value
      update.SET("inventory_qty", record.getInventoryQty() == 0 ? null : record.getInventoryQty());
    }
    if (update.WHERE("sku_id = ?", record.getId()).execute(connection).booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static PreparedStatement createPreparedStatementForInventoryCount(Connection connection, long productSkuId, int value)
      throws SQLException {
    String SQL_QUERY = "UPDATE product_skus " +
        "SET inventory_qty = inventory_qty + ? " +
        "WHERE sku_id = ?";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setInt(++i, value);
    pst.setLong(++i, productSkuId);
    return pst;
  }

  public static boolean updateInventoryCount(Connection connection, long productSkuId, int value) throws SQLException {
    // Adjust the count
    try (PreparedStatement pst = createPreparedStatementForInventoryCount(connection, productSkuId, value)) {
      return pst.execute();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    throw new SQLException("Inventory could not be updated");
  }

  public static boolean updateSquareVariationIdForProductSkuId(long productSkuId, String squareVariationId) {
    DB.UPDATE(TABLE_NAME)
        .SET("square_variation_id", squareVariationId)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("sku_id = ?", productSkuId)
        .execute();
    return true;
  }

  private static ProductSku buildRecord(ResultSet rs) {
    try {
      ProductSku record = new ProductSku();
      record.setId(rs.getLong("sku_id"));
      record.setProductId(rs.getLong("product_id"));
      record.setSku(rs.getString("sku"));
      record.setCurrency(rs.getString("currency"));
      record.setPrice(rs.getBigDecimal("price"));
      record.setStrikePrice(rs.getBigDecimal("strike_price"));
      record.setCostOfGood(rs.getBigDecimal("cost_of_good"));
      record.setBarcode(rs.getString("barcode"));
      ProductSkuJSONCommand.populateFromJSONString(record, rs.getString("attributes"));
      record.setActiveDate(rs.getTimestamp("active_date"));
      record.setDeactivateOnDate(rs.getTimestamp("deactivate_on"));
      record.setAvailableDate(rs.getTimestamp("available_date"));
      record.setInventoryQty(rs.getInt("inventory_qty"));
      record.setInventoryLow(rs.getInt("inventory_qty_low"));
      record.setInventoryIncoming(rs.getInt("inventory_qty_incoming"));
      record.setMinimumPurchaseQty(rs.getInt("minimum_purchase_qty"));
      record.setMaximumPurchaseQty(rs.getInt("maximum_purchase_qty"));
      record.setAllowBackorders(rs.getBoolean("allow_backorders"));
      record.setPackageHeight(rs.getBigDecimal("package_height"));
      record.setPackageLength(rs.getBigDecimal("package_length"));
      record.setPackageWidth(rs.getBigDecimal("package_width"));
      record.setPackageWeightPounds(rs.getInt("package_weight_lbs"));
      record.setPackageWeightOunces(rs.getInt("package_weight_ozs"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setSquareVariationId(rs.getString("square_variation_id"));
      // joined tables
      Timestamp productActiveDate = rs.getTimestamp("product_active_date");
      Timestamp productDeactiveDate = rs.getTimestamp("product_deactivate_on");
      // helpers
      if (ProductInventoryCommand.isAvailable(record, new BigDecimal(1))) {
        // There's at least 1
        record.setStatus(ProductSku.STATUS_AVAILABLE);
      } else {
        // None, so see if there's any on the way
        if (ProductInventoryCommand.hasMoreOnTheWay(record)) {
          record.setStatus(ProductSku.STATUS_MORE_ON_THE_WAY);
        } else {
          record.setStatus(ProductSku.STATUS_SOLD_OUT);
        }
      }
      // Override the status if there is a date condition
      if (productActiveDate != null) {
        // Now see if the product status can be seen
        if (DateCommand.isAfterNow(productActiveDate)) {
          // The date hasn't been reached yet, so it's coming soon
          record.setStatus(ProductSku.STATUS_COMING_SOON);
        } else if (DateCommand.isAfterNow(productDeactiveDate)) {
          // The date has already passed, so it's unavailable/expired
          record.setStatus(ProductSku.STATUS_UNAVAILABLE);
        }
      }
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  public static void export(DataConstraints constraints, File file) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("sku");
    Select select = DB.SELECT(
        "sku AS \"SKU\"",
        "products.name AS \"Name\"",
        "products.caption AS \"Caption\"",
        "TRIM(concat_ws(' ', products.name, products.caption)) AS \"ItemName\"",
        "price AS \"Value\"",
        "(SELECT JSONB_AGG(t -> 'value') FROM JSONB_ARRAY_ELEMENTS(attributes) AS x(t) WHERE t ->> 'value' <> '') AS \"Attributes\"",
        "products.description AS \"Description\"",
        "barcode AS \"UPC\"",
        "product_skus.enabled AS \"Enabled\"")
        .FROM(TABLE_NAME)
        .JOIN("products")
        .ON("product_skus.product_id = products.product_id")
        .WITH(constraints);
    writeCsvExport(select, file);
  }

  private static void writeCsvExport(Select select, File file) {
    if (select == null || file == null) {
      return;
    }
    try (Connection connection = DB.getConnection();
        java.sql.PreparedStatement statement = connection.prepareStatement(select.getSql());
        java.sql.ResultSet rs = statement.executeQuery()) {
      java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file));
      writer.write("SKU,Name,Caption,ItemName,Value,Attributes,Description,UPC,Enabled\n");
      while (rs.next()) {
        writer.write(rs.getString(1) + "," + rs.getString(2) + "," + rs.getString(3) + "," + rs.getString(4) + "," + rs.getString(5)
            + "," + rs.getString(6) + "," + rs.getString(7) + "," + rs.getString(8) + "," + rs.getString(9) + "\n");
      }
      writer.flush();
    } catch (SQLException | java.io.IOException se) {
      LOG.error("Export SQLException", se);
    }
  }
}
