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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.cms.WebPageHierarchy;
import com.simisinc.platform.infrastructure.database.AutoRollback;
import com.simisinc.platform.infrastructure.database.AutoStartTransaction;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHierarchyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX POST requests for /json/pages/reorder endpoint
 * Reorders pages by updating their hierarchy
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class PageReorderJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(PageReorderJsonService.class);

  /**
   * Handles POST requests to reorder pages
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext post(WidgetContext context) {

    // Check permissions - require admin or content-manager role
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      return writeError(context, "Permission denied");
    }

    try {
      long pageId = context.getParameterAsLong("pageId", -1);
      long targetPageId = context.getParameterAsLong("targetPageId", -1);
      long newParentIdParam = context.getParameterAsLong("newParentId", -1);

      if (pageId == -1) {
        return writeError(context, "Page ID is required");
      }

      // Load the page
      WebPage page = WebPageRepository.findById(pageId);
      if (page == null) {
        return writeError(context, "Page not found");
      }

      Long newParentId = null;
      if (newParentIdParam > 0) {
        newParentId = newParentIdParam;
      } else if (targetPageId > 0) {
        newParentId = targetPageId;
      }

      if (newParentId != null && newParentId == pageId) {
        return writeError(context, "A page cannot be its own parent");
      }

      try (Connection connection = DB.getConnection();
          AutoStartTransaction transactionStart = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {

        WebPageHierarchy currentRecord = WebPageHierarchyRepository.findByPageId(connection, pageId);
        if (currentRecord == null) {
          return writeError(context, "Page hierarchy record not found");
        }

        WebPageHierarchy parentRecord = null;
        if (newParentId != null) {
          parentRecord = WebPageHierarchyRepository.findByPageId(connection, newParentId);
          if (parentRecord == null) {
            return writeError(context, "Parent page not found in hierarchy");
          }
          if (parentRecord.getPath() != null && currentRecord.getPath() != null && parentRecord.getPath().startsWith(currentRecord.getPath())) {
            return writeError(context, "Cannot move a page beneath its own descendant");
          }
        }

        int nextSortOrder = WebPageHierarchyRepository.getNextSortOrder(connection, newParentId);
        int newDepth = parentRecord != null ? parentRecord.getDepth() + 1 : 0;
        String parentPath = parentRecord != null ? parentRecord.getPath() : "/";
        String newPath = WebPageHierarchyRepository.buildPath(parentPath, pageId);
        String oldPath = currentRecord.getPath();

        currentRecord.setParentPageId(newParentId);
        currentRecord.setSortOrder(nextSortOrder);
        currentRecord.setDepth(newDepth);
        currentRecord.setPath(newPath);
        currentRecord.setModified(new Timestamp(System.currentTimeMillis()));

        WebPageHierarchy updatedRecord = WebPageHierarchyRepository.save(connection, currentRecord);
        if (updatedRecord == null) {
          return writeError(context, "Failed to update page hierarchy");
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

      return writeOk(context, json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error reordering page: " + e.getMessage(), e);
      return writeError(context, "An unexpected error occurred");
    }
  }

  private WidgetContext writeOk(WidgetContext context, String dataJson, String metaJson) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"status\":\"ok\"");
    if (dataJson != null) {
      json.append(",\"data\":").append(dataJson);
    }
    if (metaJson != null) {
      json.append(",\"meta\":").append(metaJson);
    }
    json.append("}");
    context.setJson(json.toString());
    return context;
  }

  private WidgetContext writeError(WidgetContext context, String message) {
    context.setJson("{\"status\":\"error\",\"error\":\"" + JsonCommand.toJson(StringUtils.defaultString(message)) + "\"}");
    context.setSuccess(false);
    return context;
  }

}
