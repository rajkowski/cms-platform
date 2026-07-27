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

import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.SubFolder;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.database.DataResult;
import com.simisinc.platform.infrastructure.database.SqlJoins;
import com.simisinc.platform.infrastructure.database.SqlUtils;
import com.simisinc.platform.infrastructure.database.SqlWhere;
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
    SqlUtils select = new SqlUtils()
        .add("web_page_files.web_page_file_id")
        .add("web_page_files.web_page_id")
        .add("web_page_files.file_id")
        .add("web_page_files.created")
        .add("web_page_files.created_by")
        .add("files.filename")
        .add("files.title")
        .add("files.extension")
        .add("files.file_length")
        .add("files.file_type")
        .add("files.mime_type")
        .add("files.web_path")
        .add("files.version")
        .add("files.summary")
        .add("files.modified")
        .add("files.modified_by");
    SqlJoins joins = new SqlJoins();
    joins.add("JOIN files ON (web_page_files.file_id = files.file_id)");
    SqlUtils orderBy = new SqlUtils().add("files.filename");
    DataConstraints constraints = new DataConstraints();
    constraints.setUseCount(false);
    SqlWhere where = DB.WHERE("web_page_files.web_page_id = ?", webPageId);
    DataResult result = DB.selectAllFrom(
        TABLE_NAME, select, joins, where, orderBy, constraints, PageFileRepository::buildRecord);
    return (List<PageFile>) result.getRecords();
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
    SqlUtils select = new SqlUtils()
        .add("web_page_files.web_page_file_id")
        .add("web_page_files.web_page_id")
        .add("web_page_files.file_id")
        .add("web_page_files.created")
        .add("web_page_files.created_by")
        .add("files.filename")
        .add("files.title")
        .add("files.extension")
        .add("files.file_length")
        .add("files.file_type")
        .add("files.mime_type")
        .add("files.web_path")
        .add("files.version")
        .add("files.summary")
        .add("files.modified")
        .add("files.modified_by");
    SqlJoins joins = new SqlJoins();
    joins.add("JOIN files ON (web_page_files.file_id = files.file_id)");
    SqlWhere where = DB.WHERE("web_page_files.web_page_file_id = ?", pageFileId);
    return (PageFile) DB.selectRecordFrom(TABLE_NAME, select, joins, where, PageFileRepository::buildRecord);
  }

  /**
   * Remove a page-file relationship
   */
  public static boolean remove(PageFile pageFile) {
    if (pageFile == null || pageFile.getId() == -1) {
      return false;
    }
    try {
      DB.deleteFrom(TABLE_NAME, DB.WHERE("web_page_file_id = ?", pageFile.getId()));
      return true;
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
    SqlUtils insertValues = new SqlUtils()
        .add("web_page_id", pageFile.getWebPageId())
        .add("file_id", pageFile.getFileId())
        .add("created_by", pageFile.getCreatedBy());
    pageFile.setId(DB.insertInto(TABLE_NAME, insertValues, new String[] { PRIMARY_KEY }));
    if (pageFile.getId() == -1) {
      LOG.error("An id was not returned when saving: " + pageFile.getWebPageId());
      return null;
    }
    return pageFile;
  }

  public static PageFile update(PageFile pageFile) {
    SqlUtils updateValues = new SqlUtils()
        .add("web_page_id", pageFile.getWebPageId())
        .add("file_id", pageFile.getFileId());
    if (DB.update(TABLE_NAME, updateValues, DB.WHERE("web_page_file_id = ?", pageFile.getId()))) {
      return pageFile;
    }
    LOG.error("The page file update failed!");
    return null;
  }

  public static void removeAll(Connection connection, FileItem record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.deleteFrom(connection, TABLE_NAME, DB.WHERE("file_id = ?", record.getId()));
  }

  public static void removeAll(Connection connection, Folder record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.deleteFrom(connection, TABLE_NAME,
        DB.WHERE("file_id IN (SELECT file_id FROM files WHERE folder_id = ?)", record.getId()));
  }

  public static void removeAll(Connection connection, SubFolder record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.deleteFrom(connection, TABLE_NAME,
        DB.WHERE("file_id IN (SELECT file_id FROM files WHERE sub_folder_id = ?)", record.getId()));
  }

  public static void removeAll(Connection connection, WebPage record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.deleteFrom(connection, TABLE_NAME,
        DB.WHERE("web_page_id = ?", record.getId()));
  }
}
