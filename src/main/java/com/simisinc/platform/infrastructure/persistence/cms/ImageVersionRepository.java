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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.database.SqlUtils;

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
    return (ImageVersion) DB.selectRecordFrom(
        TABLE_NAME,
        DB.WHERE("version_id = ?", id),
        ImageVersionRepository::buildRecord);
  }

  public static List<ImageVersion> findAllByImageId(long imageId) {
    if (imageId == -1) {
      return null;
    }
    return (List<ImageVersion>) DB.selectAllFrom(
        TABLE_NAME,
        DB.WHERE("image_id = ?", imageId),
        new DataConstraints().setDefaultColumnToSortBy("version_number DESC"),
        ImageVersionRepository::buildRecord).getRecords();
  }

  public static ImageVersion save(ImageVersion record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static ImageVersion add(ImageVersion record) {
    SqlUtils insertValues = new SqlUtils()
        .add("image_id", record.getImageId())
        .add("version_number", record.getVersionNumber())
        .add("filename", StringUtils.trimToNull(record.getFilename()))
        .add("path", StringUtils.trimToNull(record.getFileServerPath()))
        .add("file_length", record.getFileLength())
        .add("file_type", record.getFileType())
        .add("width", record.getWidth())
        .add("height", record.getHeight())
        .add("is_current", record.getIsCurrent())
        .add("created_by", record.getCreatedBy())
        .add("notes", StringUtils.trimToNull(record.getNotes()));
    record.setId(DB.insertInto(TABLE_NAME, insertValues, PRIMARY_KEY));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static ImageVersion update(ImageVersion record) {
    SqlUtils updateValues = new SqlUtils()
        .add("is_current", record.getIsCurrent())
        .add("notes", StringUtils.trimToNull(record.getNotes()));
    if (DB.update(TABLE_NAME, updateValues, DB.WHERE("version_id = ?", record.getId()))) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void remove(ImageVersion record) {
    DB.deleteFrom(TABLE_NAME, DB.WHERE("version_id = ?", record.getId()));
  }

  public static boolean markAsNotCurrent(long imageId) {
    SqlUtils updateValues = new SqlUtils()
        .add("is_current", false);
    return DB.update(TABLE_NAME, updateValues, DB.WHERE("image_id = ?", imageId));
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
