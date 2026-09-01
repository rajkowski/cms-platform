/*
 * Copyright 2026 Matt Rajkowski
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
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.ImageVersion;

/**
 * Persists and retrieves image version objects
 *
 * @author matt rajkowski
 * @created 1/31/26 9:20 AM
 */
public class ImageVersionRepository {

  private static Log LOG = LogFactory.getLog(ImageVersionRepository.class);

  private static String TABLE_NAME = "image_versions";
  private static String[] PRIMARY_KEY = new String[] { "version_id" };

  public static ImageVersion findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("version_id = ?", id)
        .returnRecord(ImageVersionRepository::buildRecord);
  }

  public static List<ImageVersion> findAllByImageId(long imageId) {
    if (imageId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("image_id = ?", imageId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("version_number DESC"))
        .returnDataResult(ImageVersionRepository::buildRecord).getRecords();
  }

  public static ImageVersion save(ImageVersion record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static ImageVersion add(ImageVersion record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("image_id", record.getImageId())
        .FIELD("version_number", record.getVersionNumber())
        .FIELD("filename", StringUtils.trimToNull(record.getFilename()))
        .FIELD("path", StringUtils.trimToNull(record.getFileServerPath()))
        .FIELD("file_length", record.getFileLength())
        .FIELD("file_type", record.getFileType())
        .FIELD("width", record.getWidth())
        .FIELD("height", record.getHeight())
        .FIELD("is_current", record.getIsCurrent())
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("notes", StringUtils.trimToNull(record.getNotes()));
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static ImageVersion update(ImageVersion record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("is_current", record.getIsCurrent())
        .SET("notes", StringUtils.trimToNull(record.getNotes()))
        .WHERE("version_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void remove(ImageVersion record) {
    DB.DELETE().FROM(TABLE_NAME).WHERE("version_id = ?", record.getId()).execute();
  }

  public static void removeAll(Connection connection, Image record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("image_id = ?", record.getId()).execute(connection);
  }

  public static boolean markAsNotCurrent(long imageId) {
    return DB.UPDATE(TABLE_NAME)
        .SET("is_current", false)
        .WHERE("image_id = ?", imageId)
        .execute();
  }

  private static ImageVersion buildRecord(ResultSet rs) {
    try {
      ImageVersion record = new ImageVersion();
      record.setId(rs.getLong("version_id"));
      record.setImageId(rs.getLong("image_id"));
      record.setVersionNumber(rs.getInt("version_number"));
      record.setFilename(rs.getString("filename"));
      record.setFileServerPath(rs.getString("path"));
      record.setFileLength(rs.getLong("file_length"));
      record.setFileType(rs.getString("file_type"));
      record.setWidth(rs.getInt("width"));
      record.setHeight(rs.getInt("height"));
      record.setIsCurrent(rs.getBoolean("is_current"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setNotes(rs.getString("notes"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
