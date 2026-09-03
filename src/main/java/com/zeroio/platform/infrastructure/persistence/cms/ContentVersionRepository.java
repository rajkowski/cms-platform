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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataResult;
import com.zeroio.platform.domain.model.cms.ContentVersion;

/**
 * Persists and retrieves content version objects
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ContentVersionRepository {

  private static Log LOG = LogFactory.getLog(ContentVersionRepository.class);

  private static String TABLE_NAME = "content_versions";
  private static String[] PRIMARY_KEY = new String[] { "version_id" };

  /**
   * Find all versions for a specific content ID, ordered by created date descending
   *
   * @param contentId the content ID
   * @return list of content versions
   */
  public static List<ContentVersion> findAllByContentId(long contentId) {
    if (contentId <= 0) {
      return null;
    }
    DataResult<ContentVersion> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("content_id = ?", contentId)
        .ORDER_BY("created DESC")
        .returnDataResult(ContentVersionRepository::buildRecord);
    if (result.hasRecords()) {
      return (List<ContentVersion>) result.getRecords();
    }
    return null;
  }

  /**
   * Save a content version record
   *
   * @param record the content version to save
   * @return the saved content version
   */
  public static ContentVersion save(ContentVersion record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  /**
   * Add a new content version record
   *
   * @param record the content version to add
   * @return the added content version
   */
  public static ContentVersion add(ContentVersion record) {
    // Calculate the next version number for this content
    int nextVersionNumber = getNextVersionNumber(record.getContentId());
    record.setVersionNumber(nextVersionNumber);

    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("content_id", record.getContentId())
        .FIELD("version_number", record.getVersionNumber())
        .FIELD("content", StringUtils.trimToNull(record.getContent()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("notes", StringUtils.trimToNull(record.getNotes()))
        .execute();
    record.setId(generatedId);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  /**
   * Update an existing content version record
   *
   * @param record the content version to update
   * @return the updated content version
   */
  public static ContentVersion update(ContentVersion record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("content", StringUtils.trimToNull(record.getContent()))
        .SET("notes", StringUtils.trimToNull(record.getNotes()))
        .WHERE("version_id = ?", record.getId())
        .execute();
    if (updated) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  /**
   * Get the next version number for a specific content ID
   *
   * @param contentId the content ID
   * @return the next version number (1 if no versions exist, otherwise max + 1)
   */
  private static int getNextVersionNumber(long contentId) {
    if (contentId <= 0) {
      return 1;
    }

    long maxVersion = DB.SELECT("MAX(version_number)")
        .FROM(TABLE_NAME)
        .WHERE("content_id = ?", contentId)
        .returnValue(Long.class);
    return (maxVersion > 0) ? (int) (maxVersion + 1) : 1;
  }

  /**
   * Get the latest version number for a specific content ID
   *
   * @param contentId the content ID
   * @return the latest version number, or 0 if no versions exist
   */
  public static int getLatestVersionNumber(long contentId) {
    if (contentId <= 0) {
      return 0;
    }
    long maxVersion = DB.SELECT("MAX(version_number)")
        .FROM(TABLE_NAME)
        .WHERE("content_id = ?", contentId)
        .returnValue(Long.class);
    return (int) maxVersion;
  }

  /**
   * Build a ContentVersion record from a result set
   *
   * @param rs the result set
   * @return the content version
   */
  private static ContentVersion buildRecord(ResultSet rs) {
    try {
      ContentVersion record = new ContentVersion();
      record.setId(rs.getLong("version_id"));
      record.setContentId(rs.getLong("content_id"));
      record.setVersionNumber(rs.getInt("version_number"));
      record.setContent(rs.getString("content"));
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
