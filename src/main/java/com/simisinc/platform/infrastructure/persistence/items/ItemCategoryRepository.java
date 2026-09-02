/*
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

package com.simisinc.platform.infrastructure.persistence.items;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemCategory;

/**
 * Persists and retrieves item category objects
 *
 * @author matt rajkowski
 * @created 5/29/18 12:04 PM
 */
public class ItemCategoryRepository {

  private static Log LOG = LogFactory.getLog(ItemCategoryRepository.class);

  private static String TABLE_NAME = "item_categories";
  private static String[] PRIMARY_KEY = new String[] { "id" };

  public static ItemCategory save(ItemCategory record) {
    if (record.getId() > -1) {
      // not implemented
      return null;
    }
    return add(record);
  }

  private static ItemCategory add(ItemCategory record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("item_id", record.getItemId())
        .FIELD("category_id", record.getCategoryId() == -1 ? null : record.getCategoryId())
        .FIELD("collection_id", record.getCollectionId())
        .FIELD("dataset_id", record.getDatasetId() == -1 ? null : record.getDatasetId());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void insertItemCategoryList(Connection connection, Item item) throws SQLException {
    if (item.getCategoryIdList() == null) {
      return;
    }
    for (Long categoryId : item.getCategoryIdList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("item_id", item.getId())
          .FIELD("collection_id", item.getCollectionId())
          .FIELD_UNLESS_MATCHES("dataset_id", item.getDatasetId(), -1)
          .FIELD("category_id", categoryId)
          .execute(connection);
    }
  }

  public static void insertItemCategoryId(Connection connection, Item item, long categoryId) throws SQLException {
    if (item == null) {
      return;
    }
    DB.INSERT().INTO(TABLE_NAME)
        .FIELD("item_id", item.getId())
        .FIELD("collection_id", item.getCollectionId())
        .FIELD_UNLESS_MATCHES("dataset_id", item.getDatasetId(), -1)
        .FIELD("category_id", categoryId)
        .execute(connection);
  }

  public static void removeAll(Connection connection, Item item) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", item.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Category category) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("category_id = ?", category.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Collection collection) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", collection.getId()).execute(connection);
  }

  public static void removeItemCategoryId(Connection connection, Item item, long categoryId) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME)
        .WHERE("item_id = ?", item.getId())
        .AND("category_id = ?", categoryId)
        .execute(connection);
  }

  private static DataResult<ItemCategory> query(ItemCategorySpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME);
    if (specification != null && specification.getItemId() != -1) {
      select.WHERE("item_id = ?", specification.getItemId());
    }
    return select.WITH(constraints).returnDataResult(ItemCategoryRepository::buildRecord);
  }

  public static ItemCategory findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("id = ?", id)
        .returnRecord(ItemCategoryRepository::buildRecord);
  }

  public static List<ItemCategory> findAll(ItemCategorySpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("id");
    return query(specification, constraints).getRecords();
  }

  public static List<ItemCategory> findAllByItemId(long itemId) {
    if (itemId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", itemId)
        .returnDataResult(ItemCategoryRepository::buildRecord).getRecords();
  }

  private static ItemCategory buildRecord(ResultSet rs) {
    try {
      ItemCategory record = new ItemCategory();
      record.setId(rs.getLong("id"));
      record.setItemId(rs.getLong("item_id"));
      record.setCategoryId(rs.getLong("category_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setDatasetId(rs.getLong("dataset_id"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
