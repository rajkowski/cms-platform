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

package com.simisinc.platform.infrastructure.persistence.cms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.FolderGroup;
import com.simisinc.platform.domain.model.items.PrivacyType;

/**
 * Properties for querying objects from the repository
 *
 * @author matt rajkowski
 * @created 12/12/18 2:02 PM
 */
public class FolderGroupRepository {

  private static Log LOG = LogFactory.getLog(FolderGroupRepository.class);

  private static String TABLE_NAME = "folder_groups";
  private static String[] PRIMARY_KEY = new String[] { "allowed_id" };

  public static List<FolderGroup> findAllByFolderId(long folderId) {
    if (folderId == -1) {
      return null;
    }
    DataResult<FolderGroup> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("folder_id = ?", folderId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("allowed_id").setUseCount(false))
        .returnDataResult(FolderGroupRepository::buildRecord);
    return result.getRecords();
  }

  public static List<FolderGroup> findAll() {
    DataResult<FolderGroup> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("allowed_id"))
        .returnDataResult(FolderGroupRepository::buildRecord);
    return result.getRecords();
  }

  public static void insertFolderGroupList(Connection connection, Folder folder) throws SQLException {
    if (folder.getFolderGroupList() == null) {
      return;
    }
    for (FolderGroup allowedGroup : folder.getFolderGroupList()) {
      DB.INSERT().INTO(TABLE_NAME)
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

  public static FolderGroup add(FolderGroup record) {
    long id = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("folder_id", record.getFolderId())
        .FIELD("group_id", record.getGroupId())
        .FIELD("privacy_type", record.getPrivacyType())
        .FIELD("view_all", (record.getPrivacyType() == PrivacyType.PUBLIC || record.getPrivacyType() == PrivacyType.PUBLIC_READ_ONLY))
        .FIELD("add_permission", record.getAddPermission())
        .FIELD("edit_permission", record.getEditPermission())
        .FIELD("delete_permission", record.getDeletePermission())
        .execute();
    record.setId(id);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    // Update the folder's pointer fields (has_allowed_groups and privacy_types)
    FolderRepository.updateGroupPointers(record.getFolderId());
    return record;
  }

  public static boolean update(FolderGroup record) {
    boolean result = DB.UPDATE(TABLE_NAME)
        .SET("privacy_type", record.getPrivacyType())
        .SET("view_all", (record.getPrivacyType() == PrivacyType.PUBLIC || record.getPrivacyType() == PrivacyType.PUBLIC_READ_ONLY))
        .SET("add_permission", record.getAddPermission())
        .SET("edit_permission", record.getEditPermission())
        .SET("delete_permission", record.getDeletePermission())
        .WHERE("allowed_id = ?", record.getId())
        .execute();
    if (result) {
      // Keep the folder's pointer fields in sync after updating a group's privacy type
      FolderRepository.updateGroupPointers(record.getFolderId());
    }
    return result;
  }

  public static boolean remove(long id) {
    // Determine folder before deleting to update has_allowed_groups pointer
    FolderGroup existing = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("allowed_id = ?", id)
        .returnRecord(FolderGroupRepository::buildRecord);
    boolean deleted = DB.DELETE().FROM(TABLE_NAME).WHERE("allowed_id = ?", id).execute();
    if (deleted && existing != null) {
      // Keep the folder's pointer fields in sync after removing a group
      FolderRepository.updateGroupPointers(existing.getFolderId());
    }
    return deleted;
  }

  public static void removeAll(Connection connection, Folder folder) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("folder_id = ?", folder.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Group group) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("group_id = ?", group.getId()).execute(connection);
  }

  private static FolderGroup buildRecord(ResultSet rs) {
    try {
      FolderGroup record = new FolderGroup();
      record.setId(rs.getLong("allowed_id"));
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
