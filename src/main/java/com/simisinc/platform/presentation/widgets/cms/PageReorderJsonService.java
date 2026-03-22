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

package com.simisinc.platform.presentation.widgets.cms;

import java.sql.Connection;
import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.cms.WebPageHierarchy;
import com.simisinc.platform.infrastructure.database.AutoRollback;
import com.simisinc.platform.infrastructure.database.AutoStartTransaction;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHierarchyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Handles JSON/AJAX POST requests for /json/pages/reorder endpoint
 * Reorders pages by updating their hierarchy
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class PageReorderJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(PageReorderJsonService.class);

  /**
   * Handles POST requests to reorder pages
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + PageReorderJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    try {
      long pageId = context.getParameterAsLong("pageId", -1);
      long targetPageId = context.getParameterAsLong("targetPageId", -1);
      String position = context.getParameter("position"); // 'before', 'after', 'inside'

      if (pageId == -1) {
        return context.writeError("Page ID is required");
      }

      // Load the page
      WebPage page = WebPageRepository.findById(pageId);
      if (page == null) {
        return context.writeError("Page not found");
      }

      Long newParentId = null;
      int newSortOrder = 1;
      int newDepth = 0;
      String oldPath = null;

      try (Connection connection = DB.getConnection();
          AutoStartTransaction transactionStart = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {

        WebPageHierarchy currentRecord = WebPageHierarchyRepository.findByPageId(connection, pageId);
        if (currentRecord == null) {
          return context.writeError("Page hierarchy record not found");
        }

        oldPath = currentRecord.getPath();

        // Determine placement based on position parameter
        if ("inside".equals(position) && targetPageId > 0) {
          // Make it a child of the target
          newParentId = targetPageId;
          WebPageHierarchy targetRecord = WebPageHierarchyRepository.findByPageId(connection, targetPageId);
          if (targetRecord == null) {
            return context.writeError("Target page not found in hierarchy");
          }
          if (targetRecord.getPath() != null && currentRecord.getPath() != null
              && targetRecord.getPath().startsWith(currentRecord.getPath())) {
            return context.writeError("Cannot move a page beneath its own descendant");
          }
          newSortOrder = WebPageHierarchyRepository.getNextSortOrder(connection, newParentId);
          newDepth = targetRecord.getDepth() + 1;
        } else if (("before".equals(position) || "after".equals(position)) && targetPageId > 0) {
          // Insert before or after the target as sibling
          WebPageHierarchy targetRecord = WebPageHierarchyRepository.findByPageId(connection, targetPageId);
          if (targetRecord == null) {
            return context.writeError("Target page not found in hierarchy");
          }
          newParentId = targetRecord.getParentPageId();
          newDepth = targetRecord.getDepth();

          // Shift sort orders to make space
          if ("before".equals(position)) {
            newSortOrder = targetRecord.getSortOrder();
            WebPageHierarchyRepository.shiftSortOrders(connection, newParentId, newSortOrder);
          } else {
            newSortOrder = targetRecord.getSortOrder() + 1;
            WebPageHierarchyRepository.shiftSortOrders(connection, newParentId, newSortOrder);
          }
        } else if (targetPageId > 0) {
          // Legacy behavior - make it a child of the target (no position specified)
          newParentId = targetPageId;
          WebPageHierarchy targetRecord = WebPageHierarchyRepository.findByPageId(connection, targetPageId);
          if (targetRecord == null) {
            return context.writeError("Target page not found in hierarchy");
          }
          if (targetRecord.getPath() != null && currentRecord.getPath() != null
              && targetRecord.getPath().startsWith(currentRecord.getPath())) {
            return context.writeError("Cannot move a page beneath its own descendant");
          }
          newSortOrder = WebPageHierarchyRepository.getNextSortOrder(connection, newParentId);
          newDepth = targetRecord.getDepth() + 1;
        } else if (targetPageId == -1 || "inside".equals(position)) {
          // Move to root
          newParentId = null;
          newSortOrder = WebPageHierarchyRepository.getNextSortOrder(connection, null);
          newDepth = 0;
        } else {
          return context.writeError("Invalid target page ID");
        }

        String parentPath = "/";
        if (newParentId != null) {
          WebPageHierarchy parentRecord = WebPageHierarchyRepository.findByPageId(connection, newParentId);
          if (parentRecord != null) {
            parentPath = parentRecord.getPath();
          }
        }
        String newPath = WebPageHierarchyRepository.buildPath(parentPath, pageId);

        currentRecord.setParentPageId(newParentId);
        currentRecord.setSortOrder(newSortOrder);
        currentRecord.setDepth(newDepth);
        currentRecord.setPath(newPath);
        currentRecord.setModified(new Timestamp(System.currentTimeMillis()));

        WebPageHierarchy updatedRecord = WebPageHierarchyRepository.save(connection, currentRecord);
        if (updatedRecord == null) {
          return context.writeError("Failed to update page hierarchy");
        }

        if (oldPath != null && !oldPath.equals(newPath)) {
          WebPageHierarchyRepository.updateDescendantPaths(connection, oldPath, newPath, newDepth - currentRecord.getDepth(), pageId);
        }

        transaction.commit();
      }

      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"id\": ").append(page.getId()).append(",");
      if (newParentId != null) {
        json.append("\"parent_id\": ").append(newParentId).append(",");
      } else {
        json.append("\"parent_id\": null,");
      }
      json.append("\"message\": \"Page reordered successfully\"");
      json.append("}");

      return context.writeOk(json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error reordering page: " + e.getMessage(), e);
      return context.writeError("An unexpected error occurred");
    }
  }

}
