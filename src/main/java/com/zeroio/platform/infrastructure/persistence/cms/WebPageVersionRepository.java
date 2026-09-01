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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.zeroio.platform.domain.model.cms.WebPageVersion;

/**
 * Persists and retrieves web page version objects
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class WebPageVersionRepository {

  private static Log LOG = LogFactory.getLog(WebPageVersionRepository.class);

  private static String TABLE_NAME = "web_page_versions";
  private static String[] PRIMARY_KEY = new String[] { "version_id" };

  public static WebPageVersion findById(long id) {
    if (id <= 0) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("version_id = ?", id)
        .returnRecord(WebPageVersionRepository::buildRecord);
  }

  public static List<WebPageVersion> findAllByWebPageId(long webPageId) {
    if (webPageId <= 0) {
      return null;
    }
    DataConstraints constraints = new DataConstraints();
    constraints.setDefaultColumnToSortBy("created DESC");
    DataResult<WebPageVersion> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("web_page_id = ?", webPageId)
        .WITH(constraints)
        .returnDataResult(WebPageVersionRepository::buildRecord);
    if (result.hasRecords()) {
      return (List<WebPageVersion>) result.getRecords();
    }
    return null;
  }

  public static WebPageVersion save(WebPageVersion record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static WebPageVersion add(WebPageVersion record) {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("web_page_id", record.getWebPageId())
        .FIELD("page_xml", StringUtils.trimToNull(record.getPageXml()))
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

  public static WebPageVersion update(WebPageVersion record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("page_xml", StringUtils.trimToNull(record.getPageXml()))
        .SET("notes", StringUtils.trimToNull(record.getNotes()))
        .WHERE("version_id = ?", record.getId())
        .execute();
    if (updated) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static WebPageVersion buildRecord(ResultSet rs) {
    try {
      WebPageVersion record = new WebPageVersion();
      record.setId(rs.getLong("version_id"));
      record.setWebPageId(rs.getLong("web_page_id"));
      record.setPageXml(rs.getString("page_xml"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setNotes(rs.getString("notes"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  public static void removeAll(Connection connection, WebPage record) throws SQLException {
    if (record == null) {
      return;
    }
    DB.DELETE().FROM(TABLE_NAME).WHERE("web_page_id = ?", record.getId()).execute(connection);
  }
}