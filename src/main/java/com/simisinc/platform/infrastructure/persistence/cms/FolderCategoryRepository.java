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
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.FolderCategory;

/**
 * Persists and retrieves folder category objects
 *
 * @author matt rajkowski
 * @created 9/6/2019 2:02 PM
 */
public class FolderCategoryRepository {

  private static Log LOG = LogFactory.getLog(FolderCategoryRepository.class);

  private static String TABLE_NAME = "folder_categories";
  private static String[] PRIMARY_KEY = new String[] { "category_id" };

  public static FolderCategory findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("category_id = ?", id)
        .returnRecord(FolderCategoryRepository::buildRecord);
  }

  public static List<FolderCategory> findAllByFolderId(long folderId) {
    if (folderId == -1) {
      return null;
    }
    DataResult<FolderCategory> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("folder_id = ?", folderId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("category_id").setUseCount(false))
        .returnDataResult(FolderCategoryRepository::buildRecord);
    return result.getRecords();
  }

  public static List<FolderCategory> findAll() {
    DataResult<FolderCategory> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("category_id"))
        .returnDataResult(FolderCategoryRepository::buildRecord);
    return result.getRecords();
  }

  public static FolderCategory add(FolderCategory record) {
    long id = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("folder_id", record.getFolderId())
        .FIELD("name", record.getName())
        .FIELD("enabled", record.getEnabled())
        .execute();
    record.setId(id);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void insertFolderCategoryList(Connection connection, Folder folder) throws SQLException {
    if (folder.getFolderCategoryList() == null) {
      return;
    }
    for (FolderCategory category : folder.getFolderCategoryList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("folder_id", folder.getId())
          .FIELD("name", category.getName())
          .FIELD("enabled", category.getEnabled())
          .execute(connection);
    }
  }

  public static void updateFolderCategoryList(Connection connection, Folder folder) throws SQLException {
    if (folder.getFolderCategoryList() == null) {
      return;
    }
    for (FolderCategory category : folder.getFolderCategoryList()) {
      if (category.getId() == -1) {
        DB.INSERT().INTO(TABLE_NAME)
            .FIELD("folder_id", folder.getId())
            .FIELD("name", category.getName())
            .FIELD("enabled", category.getEnabled())
            .execute(connection);
      } else {
        DB.UPDATE(TABLE_NAME)
            .SET("name", category.getName())
            .SET("enabled", category.getEnabled())
            .WHERE("category_id = ?", category.getId())
            .execute(connection);
      }
    }
  }

  public static void removeAll(Connection connection, Folder folder) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("folder_id = ?", folder.getId()).execute(connection);
  }

  public static void remove(Connection connection, FolderCategory folderCategory) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("category_id = ?", folderCategory.getId()).execute(connection);
  }

  private static FolderCategory buildRecord(ResultSet rs) {
    try {
      FolderCategory record = new FolderCategory();
      record.setId(rs.getLong("category_id"));
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
