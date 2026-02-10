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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.WebPageHierarchy;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.SqlUtils;

/**
 * Persists and retrieves web page hierarchy objects
 *
 * @author matt rajkowski
 * @created 2/8/26 8:00 AM
 */
public class WebPageHierarchyRepository {

  private static Log LOG = LogFactory.getLog(WebPageHierarchyRepository.class);

  private static String TABLE_NAME = "web_page_hierarchy";
  private static String SQL_EXCEPTION = "SQLException: ";
  private static String COL_WEB_PAGE_ID = "web_page_id";
  private static String COL_PARENT_PAGE_ID = "parent_page_id";
  private static String COL_SORT_ORDER = "sort_order";
  private static String COL_DEPTH = "depth";
  private static String COL_PATH = "path";
  private static String COL_CREATED = "created";
  private static String COL_MODIFIED = "modified";
  private static String WHERE_WEB_PAGE_ID = "web_page_id = ?";

  /**
   * Finds a hierarchy record by web page ID
   *
   * @param pageId the web page ID
   * @return the hierarchy record or null if not found
   */
  public static WebPageHierarchy findByPageId(long pageId) {
    if (pageId == -1) {
      return null;
    }
    return (WebPageHierarchy) DB.selectRecordFrom(
        TABLE_NAME,
        DB.WHERE(WHERE_WEB_PAGE_ID, pageId),
        WebPageHierarchyRepository::buildRecord);
  }

  /**
   * Finds a hierarchy record by web page ID using an existing connection
   *
   * @param connection the database connection
   * @param pageId the web page ID
   * @return the hierarchy record or null if not found
   */
  public static WebPageHierarchy findByPageId(Connection connection, long pageId) {
    if (pageId == -1) {
      return null;
    }
    String sql = "SELECT web_page_id, parent_page_id, sort_order, depth, path, created, modified FROM " + TABLE_NAME + " WHERE web_page_id = ?";
    try (PreparedStatement pst = connection.prepareStatement(sql)) {
      pst.setLong(1, pageId);
      try (ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
          return buildRecord(rs);
        }
      }
    } catch (SQLException se) {
      LOG.error(SQL_EXCEPTION + se.getMessage());
    }
    return null;
  }

  /**
   * Saves a hierarchy record (insert or update)
   *
   * @param record the hierarchy record to save
   * @return the saved record
   */
  public static WebPageHierarchy save(WebPageHierarchy record) {
    WebPageHierarchy existingRecord = findByPageId(record.getWebPageId());
    if (existingRecord != null) {
      return update(record);
    }
    return add(record);
  }

  /**
   * Saves a hierarchy record using an existing connection (insert or update)
   *
   * @param connection the database connection
   * @param record the hierarchy record to save
   * @return the saved record
   */
  public static WebPageHierarchy save(Connection connection, WebPageHierarchy record) {
    WebPageHierarchy existingRecord = findByPageId(connection, record.getWebPageId());
    if (existingRecord != null) {
      return update(connection, record);
    }
    return add(connection, record);
  }

  /**
   * Inserts a new hierarchy record
   *
   * @param record the hierarchy record to insert
   * @return the inserted record
   */
  private static WebPageHierarchy add(WebPageHierarchy record) {
    SqlUtils insertValues = new SqlUtils()
        .add(COL_WEB_PAGE_ID, record.getWebPageId())
        .add(COL_PARENT_PAGE_ID, record.getParentPageId() != null ? record.getParentPageId() : -1, -1)
        .add(COL_SORT_ORDER, record.getSortOrder())
        .add(COL_DEPTH, record.getDepth())
        .add(COL_PATH, StringUtils.trimToNull(record.getPath()))
        .add(COL_CREATED, record.getCreated() != null ? record.getCreated() : new Timestamp(System.currentTimeMillis()))
        .add(COL_MODIFIED, record.getModified() != null ? record.getModified() : new Timestamp(System.currentTimeMillis()));
    DB.insertInto(TABLE_NAME, insertValues, null);
    return record;
  }

  /**
   * Inserts a new hierarchy record using an existing connection
   *
   * @param connection the database connection
   * @param record the hierarchy record to insert
   * @return the inserted record
   */
  private static WebPageHierarchy add(Connection connection, WebPageHierarchy record) {
    SqlUtils insertValues = new SqlUtils()
        .add(COL_WEB_PAGE_ID, record.getWebPageId())
        .add(COL_PARENT_PAGE_ID, record.getParentPageId() != null ? record.getParentPageId() : -1, -1)
        .add(COL_SORT_ORDER, record.getSortOrder())
        .add(COL_DEPTH, record.getDepth())
        .add(COL_PATH, StringUtils.trimToNull(record.getPath()))
        .add(COL_CREATED, record.getCreated() != null ? record.getCreated() : new Timestamp(System.currentTimeMillis()))
        .add(COL_MODIFIED, record.getModified() != null ? record.getModified() : new Timestamp(System.currentTimeMillis()));
    try {
      DB.insertInto(connection, TABLE_NAME, insertValues, null);
    } catch (SQLException se) {
      LOG.error(SQL_EXCEPTION + se.getMessage());
      return null;
    }
    return record;
  }

  /**
   * Updates an existing hierarchy record
   *
   * @param record the hierarchy record to update
   * @return the updated record or null if update failed
   */
  private static WebPageHierarchy update(WebPageHierarchy record) {
    SqlUtils updateValues = new SqlUtils()
        .add(COL_PARENT_PAGE_ID, record.getParentPageId() != null ? record.getParentPageId() : -1, -1)
        .add(COL_SORT_ORDER, record.getSortOrder())
        .add(COL_DEPTH, record.getDepth())
        .add(COL_PATH, StringUtils.trimToNull(record.getPath()))
        .add(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
    if (DB.update(TABLE_NAME, updateValues, DB.WHERE(WHERE_WEB_PAGE_ID, record.getWebPageId()))) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  /**
   * Updates an existing hierarchy record using an existing connection
   *
   * @param connection the database connection
   * @param record the hierarchy record to update
   * @return the updated record or null if update failed
   */
  private static WebPageHierarchy update(Connection connection, WebPageHierarchy record) {
    SqlUtils updateValues = new SqlUtils()
        .add(COL_PARENT_PAGE_ID, record.getParentPageId() != null ? record.getParentPageId() : -1, -1)
        .add(COL_SORT_ORDER, record.getSortOrder())
        .add(COL_DEPTH, record.getDepth())
        .add(COL_PATH, StringUtils.trimToNull(record.getPath()))
        .add(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
    try {
      if (DB.update(connection, TABLE_NAME, updateValues, DB.WHERE(WHERE_WEB_PAGE_ID, record.getWebPageId()))) {
        return record;
      }
    } catch (SQLException se) {
      LOG.error(SQL_EXCEPTION + se.getMessage());
      return null;
    }
    LOG.error("The update failed!");
    return null;
  }

  /**
   * Gets the next sort order for a parent page ID
   *
   * @param connection the database connection
   * @param parentId the parent page ID (null for root level)
   * @return the next sort order
   */
  public static int getNextSortOrder(Connection connection, Long parentId) {
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT COALESCE(MAX(sort_order), 0) + 1 ");
    sql.append("FROM ").append(TABLE_NAME).append(" ");
    if (parentId == null) {
      sql.append("WHERE parent_page_id IS NULL OR parent_page_id = -1");
    } else {
      sql.append("WHERE parent_page_id = ?");
    }

    try (PreparedStatement pst = connection.prepareStatement(sql.toString())) {
      if (parentId != null) {
        pst.setLong(1, parentId);
      }
      try (ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException se) {
      LOG.error(SQL_EXCEPTION + se.getMessage());
    }
    return 1;
  }

  /**
   * Shifts sort orders to make room for inserting at a specific position
   *
   * @param connection the database connection
   * @param parentId the parent page ID (null for root level)
   * @param startingSortOrder the sort order at which to start shifting
   */
  public static void shiftSortOrders(Connection connection, Long parentId, int startingSortOrder) {
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE ").append(TABLE_NAME).append(" ");
    sql.append("SET sort_order = sort_order + 1, ");
    sql.append("modified = ? ");
    sql.append("WHERE sort_order >= ? ");
    if (parentId == null) {
      sql.append("AND (parent_page_id IS NULL OR parent_page_id = -1)");
    } else {
      sql.append("AND parent_page_id = ?");
    }

    try (PreparedStatement pst = connection.prepareStatement(sql.toString())) {
      pst.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      pst.setInt(2, startingSortOrder);
      if (parentId != null) {
        pst.setLong(3, parentId);
      }
      pst.executeUpdate();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
  }

  /**
   * Updates descendant paths when a page is moved
   *
   * @param connection the database connection
   * @param oldPath the old path prefix
   * @param newPath the new path prefix
   * @param depthDelta the change in depth
   * @param pageId the page ID being moved (to exclude from update)
   */
  public static void updateDescendantPaths(Connection connection, String oldPath, String newPath, int depthDelta, long pageId) {
    if (StringUtils.isBlank(oldPath)) {
      return;
    }
    StringBuilder sql = new StringBuilder();
    sql.append("UPDATE ").append(TABLE_NAME).append(" ");
    sql.append("SET path = REPLACE(path, ?, ?), ");
    sql.append("depth = depth + ?, ");
    sql.append("modified = ? ");
    sql.append("WHERE path LIKE ? AND web_page_id <> ?");

    try (PreparedStatement pst = connection.prepareStatement(sql.toString())) {
      pst.setString(1, oldPath);
      pst.setString(2, newPath);
      pst.setInt(3, depthDelta);
      pst.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
      pst.setString(5, oldPath + "%");
      pst.setLong(6, pageId);
      pst.executeUpdate();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
  }

  /**
   * Removes hierarchy records by path pattern
   *
   * @param connection the database connection
   * @param path the path pattern to match
   * @return true if records were deleted
   */
  public static boolean removeByPath(Connection connection, String path) {
    if (StringUtils.isBlank(path)) {
      return false;
    }
    try {
      DB.deleteFrom(connection, TABLE_NAME, DB.WHERE("path LIKE ?", path + "%"));
      return true;
    } catch (Exception e) {
      LOG.error("Error removing by path: " + e.getMessage());
      return false;
    }
  }

  /**
   * Builds a path string for a page given its parent path
   *
   * @param parentPath the parent's path
   * @param pageId the page ID
   * @return the constructed path
   */
  public static String buildPath(String parentPath, long pageId) {
    String normalized = StringUtils.defaultIfBlank(parentPath, "/");
    if (!normalized.endsWith("/")) {
      normalized += "/";
    }
    return normalized + pageId + "/";
  }

  /**
   * Builds a hierarchy record from a result set
   *
   * @param rs the result set
   * @return the hierarchy record
   */
  private static WebPageHierarchy buildRecord(ResultSet rs) {
    try {
      WebPageHierarchy record = new WebPageHierarchy();
      record.setWebPageId(rs.getLong(COL_WEB_PAGE_ID));
      long parentId = DB.getLong(rs, COL_PARENT_PAGE_ID, -1);
      record.setParentPageId(parentId > 0 ? parentId : null);
      record.setSortOrder(rs.getInt(COL_SORT_ORDER));
      record.setDepth(rs.getInt(COL_DEPTH));
      record.setPath(rs.getString(COL_PATH));
      record.setCreated(rs.getTimestamp(COL_CREATED));
      record.setModified(rs.getTimestamp(COL_MODIFIED));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
