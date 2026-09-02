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

package com.simisinc.platform.infrastructure.persistence.items;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Insert;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFolder;
import com.simisinc.platform.domain.model.items.ItemFolderCategory;

/**
 * Persists and retrieves item folder category objects
 *
 * @author matt rajkowski
 * @created 4/19/2021 1:00 PM
 */
public class ItemFolderCategoryRepository {

  private static Log LOG = LogFactory.getLog(ItemFolderCategoryRepository.class);

  private static String TABLE_NAME = "item_folder_categories";
  private static String[] PRIMARY_KEY = new String[] { "category_id" };

  public static ItemFolderCategory findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("category_id = ?", id)
        .returnRecord(ItemFolderCategoryRepository::buildRecord);
  }

  public static List<ItemFolderCategory> findAllByFolderId(long folderId) {
    if (folderId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("folder_id = ?", folderId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("category_id").setUseCount(false))
        .returnDataResult(ItemFolderCategoryRepository::buildRecord).getRecords();
  }

  public static List<ItemFolderCategory> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("category_id"))
        .returnDataResult(ItemFolderCategoryRepository::buildRecord).getRecords();
  }

  public static ItemFolderCategory add(ItemFolderCategory record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("item_id", record.getItemId())
        .FIELD("folder_id", record.getFolderId())
        .FIELD("name", record.getName())
        .FIELD("enabled", record.getEnabled());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void insertFolderCategoryList(Connection connection, ItemFolder folder) throws SQLException {
    if (folder.getFolderCategoryList() == null) {
      return;
    }
    for (ItemFolderCategory category : folder.getFolderCategoryList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("folder_id", folder.getId())
          .FIELD("name", category.getName())
          .FIELD("enabled", category.getEnabled())
          .execute(connection);
    }
  }

  public static void updateFolderCategoryList(Connection connection, ItemFolder folder) throws SQLException {
    if (folder.getFolderCategoryList() == null) {
      return;
    }
    for (ItemFolderCategory category : folder.getFolderCategoryList()) {
      // Determine if inserting or updating
      if (category.getId() == -1) {
        // New category
        DB.INSERT().INTO(TABLE_NAME)
            .FIELD("folder_id", folder.getId())
            .FIELD("name", category.getName())
            .FIELD("enabled", category.getEnabled())
            .execute(connection);
      } else {
        // Update existing
        DB.UPDATE(TABLE_NAME)
            .SET("name", category.getName())
            .SET("enabled", category.getEnabled())
            .WHERE("category_id = ?", category.getId())
            .execute(connection);
      }
    }
  }

  public static void removeAll(Connection connection, Item item) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", item.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, ItemFolder folder) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("folder_id = ?", folder.getId()).execute(connection);
  }

  public static void remove(Connection connection, ItemFolderCategory folderCategory) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("category_id = ?", folderCategory.getId()).execute(connection);
  }

  private static ItemFolderCategory buildRecord(ResultSet rs) {
    try {
      ItemFolderCategory record = new ItemFolderCategory();
      record.setId(rs.getLong("category_id"));
      record.setItemId(rs.getLong("item_id"));
      record.setFolderId(rs.getLong("folder_id"));
      record.setName(rs.getString("name"));
      record.setEnabled(rs.getBoolean("enabled"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
