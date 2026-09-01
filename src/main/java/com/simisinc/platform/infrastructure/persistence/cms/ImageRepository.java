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

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.cms.Image;

/**
 * Persists and retrieves image objects
 *
 * @author matt rajkowski
 * @created 5/3/18 3:30 PM
 */
public class ImageRepository {

  private static Log LOG = LogFactory.getLog(ImageRepository.class);

  private static String TABLE_NAME = "images";
  private static String[] PRIMARY_KEY = new String[] { "image_id" };

  private static DataResult<Image> query(ImageSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() != -1) {
        select.AND("image_id = ?", specification.getId());
      }
      if (specification.getCreatedBy() != -1) {
        select.AND("created_by = ?", specification.getCreatedBy());
      }
      if (specification.getFilename() != null) {
        select.AND("LOWER(filename) = ?", specification.getFilename().toLowerCase());
      }
      if (specification.getFileType() != null) {
        select.AND("LOWER(file_type) = ?", specification.getFileType().toLowerCase());
      }
      if (specification.getSearchTerm() != null) {
        String searchValue = "%" + specification.getSearchTerm().toLowerCase() + "%";
        select.AND("(LOWER(filename) LIKE ? OR LOWER(title) LIKE ? OR LOWER(alt_text) LIKE ? OR LOWER(description) LIKE ?)",
            new String[] { searchValue, searchValue, searchValue, searchValue });
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(ImageRepository::buildRecord);
  }

  public static Image findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("image_id = ?", id)
        .returnRecord(ImageRepository::buildRecord);
  }

  public static Image findByWebPathAndId(String versionWebPath, long id) {
    if (StringUtils.isBlank(versionWebPath) || id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("web_path = ?", versionWebPath)
        .AND("image_id = ?", id)
        .returnRecord(ImageRepository::buildRecord);
  }

  public static Image findByWebPath(String versionWebPath) {
    if (StringUtils.isBlank(versionWebPath)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("web_path = ?", versionWebPath)
        .returnRecord(ImageRepository::buildRecord);
  }

  public static List<Image> findAll() {
    return findAll(null, null);
  }

  public static List<Image> findAll(ImageSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("created DESC");
    return query(specification, constraints).getRecords();
  }

  public static Image save(Image record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static Image add(Image record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("filename", StringUtils.trimToNull(record.getFilename()))
        .FIELD("path", StringUtils.trimToNull(record.getFileServerPath()))
        .FIELD("web_path", StringUtils.trimToNull(record.getWebPath()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("file_length", record.getFileLength())
        .FIELD("file_type", record.getFileType())
        .FIELD("width", record.getWidth())
        .FIELD("height", record.getHeight());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static Image update(Image record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy())
        .SET("modified", new java.sql.Timestamp(System.currentTimeMillis()))
        .SET("processed", record.getProcessed())
        .SET("processed_path", StringUtils.trimToNull(record.getProcessedPath()))
        .SET("processed_file_length", record.getProcessedFileLength())
        .SET("processed_file_type", StringUtils.trimToNull(record.getProcessedFileType()))
        .SET("processed_width", record.getProcessedWidth())
        .SET("processed_height", record.getProcessedHeight())
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("alt_text", StringUtils.trimToNull(record.getAltText()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("version_number", record.getVersionNumber())
        .SET("filename", StringUtils.trimToNull(record.getFilename()))
        .SET("path", StringUtils.trimToNull(record.getFileServerPath()))
        .SET("file_length", record.getFileLength())
        .SET("file_type", StringUtils.trimToNull(record.getFileType()))
        .SET("width", record.getWidth())
        .SET("height", record.getHeight())
        .WHERE("image_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Image record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      ImageVersionRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("image_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static long findTotalFileSize() {
    DB.SELECT("SUM(file_length)").FROM(TABLE_NAME).returnValue(Long.class);
  }

  private static Image buildRecord(ResultSet rs) {
    try {
      Image record = new Image();
      record.setId(rs.getLong("image_id"));
      record.setFilename(rs.getString("filename"));
      record.setFileServerPath(rs.getString("path"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setProcessed(rs.getTimestamp("processed"));
      record.setFileLength(rs.getLong("file_length"));
      record.setFileType(rs.getString("file_type"));
      record.setWidth(rs.getInt("width"));
      record.setHeight(rs.getInt("height"));
      record.setProcessedPath(rs.getString("processed_path"));
      record.setProcessedFileLength(rs.getLong("processed_file_length"));
      record.setProcessedFileType(rs.getString("processed_file_type"));
      record.setProcessedWidth(rs.getInt("processed_width"));
      record.setProcessedHeight(rs.getInt("processed_height"));
      record.setWebPath(rs.getString("web_path"));
      record.setTitle(rs.getString("title"));
      record.setAltText(rs.getString("alt_text"));
      record.setDescription(rs.getString("description"));
      record.setVersionNumber(rs.getInt("version_number"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
