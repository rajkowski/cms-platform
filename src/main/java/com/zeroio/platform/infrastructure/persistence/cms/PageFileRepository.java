/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
package com.zeroio.platform.infrastructure.persistence.cms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.SubFolder;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.zeroio.platform.domain.model.cms.PageFile;

/**
 * Retrieves PageFile records by joining web_page_files with files
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class PageFileRepository {

  private static Log LOG = LogFactory.getLog(PageFileRepository.class);

  private static String TABLE_NAME = "web_page_files";
  private static String PRIMARY_KEY = "web_page_file_id";

  /**
   * Returns all files associated with the given web page id, ordered by filename.
   */
  @SuppressWarnings("unchecked")
  public static List<PageFile> findByWebPageId(long webPageId) {
    if (webPageId == -1) {
      return null;
    }
    DataConstraints constraints = new DataConstraints();
    constraints.setUseCount(false);

    Select query = DB.SELECT(
        "web_page_files.web_page_file_id",
        "web_page_files.web_page_id",
        "web_page_files.file_id",
        "web_page_files.created",
        "web_page_files.created_by",
        "files.filename",
        "files.title",
        "files.extension",
        "files.file_length",
        "files.file_type",
        "files.mime_type",
        "files.web_path",
        "files.version",
        "files.summary",
        "files.modified",
        "files.modified_by")
        .FROM(TABLE_NAME)
        .JOIN("files")
        .ON("web_page_files.file_id = files.file_id")
        .WHERE("web_page_files.web_page_id = ?", webPageId)
        .ORDER_BY("files.filename")
        .WITH(constraints);
    return query.returnDataResult(PageFileRepository::buildRecord).getRecords();
  }

  private static PageFile buildRecord(ResultSet rs) {
    try {
      PageFile record = new PageFile();
      record.setId(rs.getLong("web_page_file_id"));
      record.setWebPageId(rs.getLong("web_page_id"));
      record.setFileId(rs.getLong("file_id"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setFilename(rs.getString("filename"));
      record.setTitle(rs.getString("title"));
      record.setExtension(rs.getString("extension"));
      record.setFileLength(rs.getLong("file_length"));
      String fileType = rs.getString("file_type");
      record.setFileType(fileType != null ? fileType.toLowerCase() : null);
      record.setMimeType(rs.getString("mime_type"));
      record.setWebPath(rs.getString("web_path"));
      record.setVersion(rs.getString("version"));
      record.setSummary(rs.getString("summary"));
      record.setFileModified(rs.getTimestamp("modified"));
      record.setFileModifiedBy(rs.getLong("modified_by"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  /**
   * Find a single page-file record by its ID
   */
  public static PageFile findById(long pageFileId) {
    if (pageFileId == -1) {
      return null;
    }
    return DB.SELECT(
        "web_page_files.web_page_file_id",
        "web_page_files.web_page_id",
        "web_page_files.file_id",
        "web_page_files.created",
        "web_page_files.created_by",
        "files.filename",
        "files.title",
        "files.extension",
        "files.file_length",
        "files.file_type",
        "files.mime_type",
        "files.web_path",
        "files.version",
        "files.summary",
        "files.modified",
        "files.modified_by")
        .FROM(TABLE_NAME)
        .JOIN("files")
        .ON("web_page_files.file_id = files.file_id")
        .WHERE("web_page_files.web_page_file_id = ?", pageFileId)
        .returnRecord(PageFileRepository::buildRecord);
  }

  /**
   * Remove a page-file relationship
   */
  public static boolean remove(PageFile pageFile) {
    if (pageFile == null || pageFile.getId() == -1) {
      return false;
    }
    try {
      return DB.DELETE().FROM(TABLE_NAME).WHERE("web_page_file_id = ?", pageFile.getId()).execute();
    } catch (Exception e) {
      LOG.error("remove", e);
      return false;
    }
  }

  /**
   * Saves a new page-file relationship
   */
  public static PageFile save(PageFile pageFile) {
    if (pageFile == null) {
      return null;
    }
    if (pageFile.getId() > -1) {
      return update(pageFile);
    }
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("web_page_id", pageFile.getWebPageId())
        .FIELD("file_id", pageFile.getFileId())
        .FIELD("created_by", pageFile.getCreatedBy())
        .execute();
    pageFile.setId(generatedId);
    if (pageFile.getId() == -1) {
      LOG.error("An id was not returned when saving: " + pageFile.getWebPageId());
      return null;
    }
    return pageFile;
  }

  public static PageFile update(PageFile pageFile) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("web_page_id", pageFile.getWebPageId())
        .SET("file_id", pageFile.getFileId())
        .WHERE("web_page_file_id = ?", pageFile.getId())
        .execute();
    if (updated) {
      return pageFile;
    }
    LOG.error("The page file update failed!");
    return null;
  }

  public static void removeAll(Connection connection, FileItem record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.DELETE().FROM(TABLE_NAME).WHERE("file_id = ?", record.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Folder record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.DELETE().FROM(TABLE_NAME)
        .WHERE("file_id IN (SELECT file_id FROM files WHERE folder_id = ?)", record.getId())
        .execute(connection);
  }

  public static void removeAll(Connection connection, SubFolder record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.DELETE().FROM(TABLE_NAME)
        .WHERE("file_id IN (SELECT file_id FROM files WHERE sub_folder_id = ?)", record.getId())
        .execute(connection);
  }

  public static void removeAll(Connection connection, WebPage record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.DELETE().FROM(TABLE_NAME).WHERE("web_page_id = ?", record.getId()).execute(connection);
  }
}
