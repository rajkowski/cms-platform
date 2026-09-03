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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.FileVersion;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.SubFolder;

/**
 * Persists and retrieves file version objects
 *
 * @author matt rajkowski
 * @created 12/12/18 3:03 PM
 */
public class FileVersionRepository {

  private static Log LOG = LogFactory.getLog(FileVersionRepository.class);

  private static String TABLE_NAME = "file_versions";
  private static String[] PRIMARY_KEY = new String[] { "version_id" };

  private static DataResult<FileVersion> query(FileVersionSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("file_versions.*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("version_id = ?", specification.getId());
      }
      if (specification.getFileId() > -1) {
        select.AND("file_id = ?", specification.getFileId());
      }
      if (specification.getFolderId() > -1) {
        select.AND("folder_id = ?", specification.getFolderId());
      }
      if (specification.getSubFolderId() > -1) {
        select.AND("sub_folder_id = ?", specification.getSubFolderId());
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(FileVersionRepository::buildRecord);
  }

  public static FileVersion findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("file_versions.*")
        .FROM(TABLE_NAME)
        .WHERE("version_id = ?", id)
        .returnRecord(FileVersionRepository::buildRecord);
  }

  public static List<FileVersion> findAll(FileVersionSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("created DESC");
    return query(specification, constraints).getRecords();
  }

  public static FileVersion save(FileVersion record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return null;
  }

  public static FileItem add(Connection connection, FileItem record) throws SQLException {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("file_id", record.getId())
        .FIELD("folder_id", record.getFolderId())
        .FIELD("filename", StringUtils.trimToNull(record.getFilename()))
        .FIELD("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("version", StringUtils.trimToNull(record.getVersion()))
        .FIELD("extension", StringUtils.trimToNull(record.getExtension()))
        .FIELD("path", StringUtils.trimToNull(record.getFileServerPath()))
        .FIELD("web_path", StringUtils.trimToNull(record.getWebPath()))
        .FIELD("file_length", record.getFileLength())
        .FIELD("file_type", record.getFileType())
        .FIELD("mime_type", record.getMimeType())
        .FIELD("file_hash", record.getFileHash())
        .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
        .FIELD("created_by", record.getCreatedBy());
    if (record.getSubFolderId() != -1) {
      insert.FIELD("sub_folder_id", record.getSubFolderId());
    }
    if (record.getCategoryId() != -1) {
      insert.FIELD("category_id", record.getCategoryId());
    }
    if (record.getWidth() != -1) {
      insert.FIELD("width", record.getWidth());
    }
    if (record.getHeight() != -1) {
      insert.FIELD("height", record.getHeight());
    }
    insert.execute(connection);
    return record;
  }

  private static FileVersion update(FileVersion record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("folder_id", record.getFolderId())
        .SET("sub_folder_id", record.getSubFolderId() == -1L ? null : record.getSubFolderId())
        .SET("category_id", record.getCategoryId() == -1L ? null : record.getCategoryId())
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("version", StringUtils.trimToNull(record.getVersion()))
        .SET("summary", StringUtils.trimToNull(record.getSummary()))
        .SET("modified_by", record.getModifiedBy());
    if (record.getWidth() != -1) {
      update.SET("width", record.getWidth());
    }
    if (record.getHeight() != -1) {
      update.SET("height", record.getHeight());
    }
    if (update.WHERE("version_id = ?", record.getId()).execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static FileItem update(Connection connection, FileItem record) throws SQLException {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("folder_id", record.getFolderId())
        .SET("sub_folder_id", record.getSubFolderId() == -1L ? null : record.getSubFolderId())
        .SET("category_id", record.getCategoryId() == -1L ? null : record.getCategoryId())
        .WHERE("file_id = ?", record.getId());
    if (update.execute(connection).booleanValue()) {
      return record;
    }
    LOG.error("The update fileItem failed!");
    return null;
  }

  public static void remove(FileVersion record) {
    DB.DELETE().FROM(TABLE_NAME).WHERE("version_id = ?", record.getId()).execute();
  }

  public static void removeAll(Connection connection, Folder record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("folder_id = ?", record.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, SubFolder record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("sub_folder_id = ?", record.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, FileItem record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("file_id = ?", record.getId()).execute(connection);
  }

  public static long findTotalFileSize() {
    return DB.SELECT("SUM(file_length) AS total_file_length").FROM(TABLE_NAME).returnValue(Long.class);
  }

  private static FileVersion buildRecord(ResultSet rs) {
    try {
      FileVersion record = new FileVersion();
      record.setId(rs.getLong("version_id"));
      record.setFileId(rs.getLong("file_id"));
      record.setFolderId(rs.getLong("folder_id"));
      record.setFilename(rs.getString("filename"));
      record.setTitle(rs.getString("title"));
      record.setVersion(rs.getString("version"));
      record.setExtension(rs.getString("extension"));
      record.setFileServerPath(rs.getString("path"));
      record.setFileLength(rs.getLong("file_length"));
      String fileType = rs.getString("file_type");
      if (fileType != null) {
        record.setFileType(fileType.toLowerCase());
      } else {
        record.setFileType(null);
      }
      record.setMimeType(rs.getString("mime_type"));
      record.setFileHash(rs.getString("file_hash"));
      record.setWidth(rs.getInt("width"));
      record.setHeight(rs.getInt("height"));
      record.setSummary(rs.getString("summary"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setDownloadCount(rs.getLong("download_count"));
      record.setSubFolderId(DB.getLong(rs, "sub_folder_id", -1L));
      record.setCategoryId(DB.getLong(rs, "category_id", -1L));
      record.setWebPath(rs.getString("web_path"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
