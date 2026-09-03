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
import com.github.rajkowski.database.Insert;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFolder;
import com.simisinc.platform.domain.model.items.ItemFolderGroup;
import com.simisinc.platform.domain.model.items.PrivacyType;

/**
 * Persists and retrieves item folder group objects
 *
 * @author matt rajkowski
 * @created 4/19/2021 1:00 PM
 */
public class ItemFolderGroupRepository {

  private static Log LOG = LogFactory.getLog(ItemFolderGroupRepository.class);

  private static String TABLE_NAME = "item_folder_groups";
  private static String[] PRIMARY_KEY = new String[] { "allowed_id" };

  public static List<ItemFolderGroup> findAllByFolderId(long folderId) {
    if (folderId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("folder_id = ?", folderId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("allowed_id").setUseCount(false))
        .returnDataResult(ItemFolderGroupRepository::buildRecord).getRecords();
  }

  public static List<ItemFolderGroup> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("allowed_id"))
        .returnDataResult(ItemFolderGroupRepository::buildRecord).getRecords();
  }

  public static ItemFolderGroup add(ItemFolderGroup record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("item_id", record.getItemId())
        .FIELD("folder_id", record.getFolderId())
        .FIELD("group_id", record.getGroupId())
        .FIELD("privacy_type", record.getPrivacyType())
        .FIELD("view_all", (record.getPrivacyType() == PrivacyType.PUBLIC || record.getPrivacyType() == PrivacyType.PUBLIC_READ_ONLY))
        .FIELD("add_permission", record.getAddPermission())
        .FIELD("edit_permission", record.getEditPermission())
        .FIELD("delete_permission", record.getDeletePermission());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void insertFolderGroupList(Connection connection, ItemFolder folder) throws SQLException {
    if (folder.getFolderGroupList() == null) {
      return;
    }
    for (ItemFolderGroup allowedGroup : folder.getFolderGroupList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("item_id", folder.getItemId())
          .FIELD("folder_id", folder.getId())
          .FIELD("group_id", allowedGroup.getGroupId())
          .FIELD("privacy_type", allowedGroup.getPrivacyType())
          .FIELD("view_all",
              (allowedGroup.getPrivacyType() == PrivacyType.PUBLIC || allowedGroup.getPrivacyType() == PrivacyType.PUBLIC_READ_ONLY))
          .FIELD("add_permission", allowedGroup.getAddPermission())
          .FIELD("edit_permission", allowedGroup.getEditPermission())
          .FIELD("delete_permission", allowedGroup.getDeletePermission())
          .execute(connection);
    }
  }

  public static void removeAll(Connection connection, Item item) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", item.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, ItemFolder folder) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("folder_id = ?", folder.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Group group) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("group_id = ?", group.getId()).execute(connection);
  }

  private static ItemFolderGroup buildRecord(ResultSet rs) {
    try {
      ItemFolderGroup record = new ItemFolderGroup();
      record.setId(rs.getLong("allowed_id"));
      record.setItemId(rs.getLong("item_id"));
      record.setFolderId(rs.getLong("folder_id"));
      record.setGroupId(rs.getLong("group_id"));
      record.setPrivacyType(rs.getInt("privacy_type"));
      record.setAddPermission(rs.getBoolean("add_permission"));
      record.setEditPermission(rs.getBoolean("edit_permission"));
      record.setDeletePermission(rs.getBoolean("delete_permission"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
