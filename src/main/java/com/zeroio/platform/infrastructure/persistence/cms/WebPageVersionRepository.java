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

import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.database.DataResult;
import com.simisinc.platform.infrastructure.database.SqlUtils;
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
    return (WebPageVersion) DB.selectRecordFrom(
        TABLE_NAME,
        DB.WHERE("version_id = ?", id),
        WebPageVersionRepository::buildRecord);
  }

  public static List<WebPageVersion> findAllByWebPageId(long webPageId) {
    if (webPageId <= 0) {
      return null;
    }
    DataConstraints constraints = new DataConstraints();
    constraints.setDefaultColumnToSortBy("created DESC");
    DataResult result = DB.selectAllFrom(
        TABLE_NAME,
        DB.WHERE("web_page_id = ?", webPageId),
        constraints,
        WebPageVersionRepository::buildRecord);
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
    SqlUtils insertValues = new SqlUtils()
        .add("web_page_id", record.getWebPageId())
        .add("page_xml", StringUtils.trimToNull(record.getPageXml()))
        .add("created_by", record.getCreatedBy())
        .add("notes", StringUtils.trimToNull(record.getNotes()));
    record.setId(DB.insertInto(TABLE_NAME, insertValues, PRIMARY_KEY));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static WebPageVersion update(WebPageVersion record) {
    SqlUtils updateValues = new SqlUtils()
        .add("page_xml", StringUtils.trimToNull(record.getPageXml()))
        .add("notes", StringUtils.trimToNull(record.getNotes()));
    if (DB.update(TABLE_NAME,
        updateValues,
        DB.WHERE("version_id = ?", record.getId()))) {
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
    DB.deleteFrom(connection, TABLE_NAME,
        DB.WHERE("web_page_id = ?", record.getId()));
  }
}