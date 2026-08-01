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
package com.zeroio.platform.application.cms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.PageTreeNode;
import com.simisinc.platform.infrastructure.database.DB;

/**
 * Loads hierarchical page structure for tree display in the Visual Content Editor
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class LoadPageTreeCommand {

  private static final String SQL_FROM_WEB_PAGES = "FROM web_pages p ";
  private static final String SQL_JOIN_HIERARCHY = "INNER JOIN web_page_hierarchy h ON p.web_page_id = h.web_page_id ";
  private static Log log = LogFactory.getLog(LoadPageTreeCommand.class);

  private LoadPageTreeCommand() {
  }

  /**
   * Loads child pages for a given parent page ID, supporting lazy loading
   *
   * @param parentId the parent page ID (-1 for root level pages)
   * @return list of PageTreeNode objects sorted by sort_order, then title
   */
  public static List<PageTreeNode> loadPageTree(long parentId) {
    List<PageTreeNode> nodes = new ArrayList<>();

    // Build the SQL query to join web_page_hierarchy with web_pages
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT p.web_page_id, p.page_title, p.link, h.sort_order, h.depth, ");
    sql.append("(SELECT COUNT(*) FROM web_page_hierarchy h2 WHERE h2.parent_page_id = p.web_page_id) AS child_count ");
    sql.append(SQL_FROM_WEB_PAGES);
    sql.append(SQL_JOIN_HIERARCHY);

    if (parentId == -1) {
      sql.append("WHERE h.parent_page_id IS NULL ");
    } else {
      sql.append("WHERE h.parent_page_id = ? ");
    }

    sql.append("ORDER BY h.sort_order, p.page_title");

    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sql.toString())) {

      // Set the parent ID parameter if not null
      if (parentId != -1) {
        pst.setLong(1, parentId);
      }

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          PageTreeNode node = new PageTreeNode();
          node.setId(rs.getLong("web_page_id"));
          node.setTitle(rs.getString("page_title"));
          node.setLink(rs.getString("link"));
          node.setLevel(rs.getInt("depth"));
          node.setHasChildren(rs.getInt("child_count") > 0);
          nodes.add(node);
        }
      }
    } catch (SQLException se) {
      log.error("loadPageTree error", se);
    }

    return nodes;
  }

  /**
   * Checks if a page has children
   *
   * @param pageId the page ID to check
   * @return true if the page has children, false otherwise
   */
  public static boolean hasChildren(long pageId) {
    String sql = "SELECT COUNT(*) FROM web_page_hierarchy WHERE parent_page_id = ?";

    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sql)) {

      pst.setLong(1, pageId);

      try (ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        }
      }
    } catch (SQLException se) {
      log.error("hasChildren error", se);
    }

    return false;
  }

  /**
   * Loads a page subtree in one SQL query using a recursive CTE.
   *
   * @param parentId root parent page ID
   * @param maxDepth maximum depth to retrieve (1 = direct children)
   * @param filterPublished true to include only enabled, non-draft pages
   * @return ordered list of page tree nodes with relative level starting at 1
   */
  public static List<PageTreeNode> loadPageTree(long parentId, int maxDepth, boolean filterPublished) {
    List<PageTreeNode> nodes = new ArrayList<>();

    StringBuilder sql = new StringBuilder();
    sql.append("WITH RECURSIVE page_tree AS ( ");
    sql.append("SELECT p.web_page_id, p.page_title, p.page_description, p.link, p.enabled, p.draft, ");
    sql.append("h.sort_order, 1 AS tree_level, ");
    sql.append(
        "LPAD(COALESCE(CAST(h.sort_order AS TEXT), '0'), 10, '0') || ':' || LOWER(COALESCE(p.page_title, '')) AS sort_path ");
    sql.append(SQL_FROM_WEB_PAGES);
    sql.append(SQL_JOIN_HIERARCHY);
    if (parentId == -1) {
      sql.append("WHERE h.parent_page_id IS NULL ");
    } else {
      sql.append("WHERE h.parent_page_id = ? ");
    }
    sql.append("UNION ALL ");
    sql.append("SELECT p.web_page_id, p.page_title, p.page_description, p.link, p.enabled, p.draft, ");
    sql.append("h.sort_order, pt.tree_level + 1 AS tree_level, ");
    sql.append(
        "pt.sort_path || '/' || LPAD(COALESCE(CAST(h.sort_order AS TEXT), '0'), 10, '0') || ':' || LOWER(COALESCE(p.page_title, '')) AS sort_path ");
    sql.append(SQL_FROM_WEB_PAGES);
    sql.append(SQL_JOIN_HIERARCHY);
    sql.append("INNER JOIN page_tree pt ON h.parent_page_id = pt.web_page_id ");
    sql.append("WHERE pt.tree_level < ? ");
    sql.append(") ");
    sql.append("SELECT pt.web_page_id, pt.page_title, pt.page_description, pt.link, pt.tree_level, ");
    sql.append("(SELECT COUNT(*) FROM web_page_hierarchy h2 WHERE h2.parent_page_id = pt.web_page_id) AS child_count ");
    sql.append("FROM page_tree pt ");
    sql.append("WHERE (? = FALSE OR (pt.enabled = TRUE AND pt.draft = FALSE)) ");
    sql.append("ORDER BY pt.sort_path");

    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sql.toString())) {
      int paramIndex = 0;
      if (parentId > -1) {
        pst.setLong(++paramIndex, parentId);
      }
      pst.setInt(++paramIndex, Math.max(1, maxDepth));
      pst.setBoolean(++paramIndex, filterPublished);

      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          PageTreeNode node = new PageTreeNode();
          node.setId(rs.getLong("web_page_id"));
          node.setTitle(rs.getString("page_title"));
          node.setDescription(rs.getString("page_description"));
          node.setLink(rs.getString("link"));
          node.setLevel(rs.getInt("tree_level"));
          node.setHasChildren(rs.getInt("child_count") > 0);
          nodes.add(node);
        }
      }
    } catch (SQLException se) {
      log.error("loadPageTree subtree error", se);
    }

    return nodes;
  }
}
