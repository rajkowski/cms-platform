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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.ecommerce.ProductCategory;

/**
 * Persists and retrieves product category objects
 *
 * @author matt rajkowski
 * @created 4/10/21 5:10 PM
 */
public class ProductCategoryRepository {

  private static Log LOG = LogFactory.getLog(ProductCategoryRepository.class);

  private static String TABLE_NAME = "lookup_product_categories";
  private static String[] PRIMARY_KEY = new String[] { "category_id" };

  public static List<ProductCategory> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("display_order, name").setUseCount(false))
        .returnDataResult(ProductCategoryRepository::buildRecord).getRecords();
  }

  public static ProductCategory findById(long categoryId) {
    if (categoryId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("category_id = ?", categoryId)
        .returnRecord(ProductCategoryRepository::buildRecord);
  }

  public static ProductCategory findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("UPPER(category_unique_id) = ?", uniqueId.toUpperCase())
        .returnRecord(ProductCategoryRepository::buildRecord);
  }

  public static ProductCategory save(ProductCategory record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static ProductCategory add(ProductCategory record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      Insert insert = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("category_unique_id", record.getUniqueId())
          .FIELD("name", record.getName())
          .FIELD("description", record.getDescription())
          .FIELD("created_by", record.getCreatedBy() == -1 ? null : record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy())
          .FIELD("enabled", record.getEnabled());
      if (record.getDisplayOrder() > 0) {
        insert.FIELD("display_order", record.getDisplayOrder());
      }
      record.setId(insert.execute(connection));
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static ProductCategory update(ProductCategory record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("category_unique_id", record.getUniqueId())
        .SET("name", record.getName())
        .SET("description", record.getDescription())
        .SET("enabled", record.getEnabled())
        .SET("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (record.getDisplayOrder() > 0) {
      update.SET("display_order", record.getDisplayOrder());
    }
    update.WHERE("category_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(ProductCategory record) {
    return DB.DELETE().FROM(TABLE_NAME).WHERE("category_id = ?", record.getId()).execute();
  }

  private static ProductCategory buildRecord(ResultSet rs) {
    try {
      ProductCategory record = new ProductCategory();
      record.setId(rs.getLong("category_id"));
      record.setUniqueId(rs.getString("category_unique_id"));
      record.setName(rs.getString("name"));
      record.setDescription(rs.getString("description"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setDisplayOrder(DB.getInt(rs, "display_order", 0));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
