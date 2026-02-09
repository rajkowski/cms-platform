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

package com.simisinc.platform.application.cms;

import com.simisinc.platform.domain.model.cms.PageTreeNode;
import com.simisinc.platform.infrastructure.database.DB;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads hierarchical page structure for tree display in the Visual Content Editor
 *
 * @author matt rajkowski
 * @created 2/7/26 12:00 PM
 */
public class LoadPageTreeCommand {

  private static Log LOG = LogFactory.getLog(LoadPageTreeCommand.class);

  /**
   * Loads child pages for a given parent page ID, supporting lazy loading
   *
   * @param parentId the parent page ID (null for root level pages)
   * @return list of PageTreeNode objects sorted by sort_order, then title
   */
  public static List<PageTreeNode> loadPageTree(Long parentId) {
    List<PageTreeNode> nodes = new ArrayList<>();

    // Build the SQL query to join web_page_hierarchy with web_pages
    StringBuilder sql = new StringBuilder();
    sql.append("SELECT p.web_page_id, p.page_title, p.link, h.sort_order, h.depth, ");
    sql.append("(SELECT COUNT(*) FROM web_page_hierarchy h2 WHERE h2.parent_page_id = p.web_page_id) AS child_count ");
    sql.append("FROM web_pages p ");
    sql.append("INNER JOIN web_page_hierarchy h ON p.web_page_id = h.web_page_id ");
    
    if (parentId == null) {
      sql.append("WHERE h.parent_page_id IS NULL ");
    } else {
      sql.append("WHERE h.parent_page_id = ? ");
    }
    
    sql.append("ORDER BY h.sort_order, p.page_title");

    try (Connection connection = DB.getConnection();
         PreparedStatement pst = connection.prepareStatement(sql.toString())) {
      
      // Set the parent ID parameter if not null
      if (parentId != null) {
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
      LOG.error("loadPageTree error", se);
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
      LOG.error("hasChildren error", se);
    }
    
    return false;
  }
}
