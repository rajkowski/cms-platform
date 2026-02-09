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
 * Handles JSON/AJAX POST requests for /json/pages/add-to-hierarchy endpoint
 * Inserts a page into the web_page_hierarchy table
 *
 * @author matt rajkowski
 * @created 2/7/26 5:00 PM
 */
public class PageAddToHierarchyJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(PageAddToHierarchyJsonService.class);

  /**
   * Handles POST requests to add a page to the hierarchy
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
      long parentPageId = context.getParameterAsLong("parentPageId", -1);

      if (pageId == -1) {
        return writeError(context, "Page ID is required");
      }

      WebPage page = WebPageRepository.findById(pageId);
      if (page == null) {
        return writeError(context, "Page not found");
      }

      Long parentId = parentPageId > 0 ? parentPageId : null;

      try (Connection connection = DB.getConnection();
          AutoStartTransaction transactionStart = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {

        WebPageHierarchy existingRecord = WebPageHierarchyRepository.findByPageId(connection, pageId);
        if (existingRecord != null) {
          return writeOk(context, "{\"message\":\"Page is already in hierarchy\"}", null);
        }

        WebPageHierarchy parentRecord = null;
        if (parentId != null) {
          parentRecord = WebPageHierarchyRepository.findByPageId(connection, parentId);
          if (parentRecord == null) {
            return writeError(context, "Parent page not found in hierarchy");
          }
        }

        int nextSortOrder = WebPageHierarchyRepository.getNextSortOrder(connection, parentId);
        int newDepth = parentRecord != null ? parentRecord.getDepth() + 1 : 0;
        String parentPath = parentRecord != null ? parentRecord.getPath() : "/";
        String newPath = WebPageHierarchyRepository.buildPath(parentPath, pageId);

        WebPageHierarchy record = new WebPageHierarchy();
        record.setWebPageId(pageId);
        record.setParentPageId(parentId);
        record.setSortOrder(nextSortOrder);
        record.setDepth(newDepth);
        record.setPath(newPath);
        record.setCreated(new Timestamp(System.currentTimeMillis()));
        record.setModified(new Timestamp(System.currentTimeMillis()));

        WebPageHierarchyRepository.save(connection, record);
        transaction.commit();
      }

      return writeOk(context, "{\"message\":\"Page added to hierarchy\"}", null);

    } catch (Exception e) {
      LOG.error("Error adding page to hierarchy: " + e.getMessage(), e);
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
    context.setJson("{\"status\":\"error\",\"error\":\"" + JsonCommand.toJson(message) + "\"}");
    context.setSuccess(false);
    return context;
  }

}
