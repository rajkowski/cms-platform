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
 * Handles JSON/AJAX POST requests for /json/pages/add-to-hierarchy endpoint
 * Inserts a page into the web_page_hierarchy table
 *
 * @author matt rajkowski
 * @created 2/7/26 5:00 PM
 */
public class PageAddToHierarchyJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(PageAddToHierarchyJsonService.class);

  /**
   * Handles POST requests to add a page to the hierarchy
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + PageAddToHierarchyJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    try {
      long pageId = context.getParameterAsLong("pageId", -1);
      long parentPageId = context.getParameterAsLong("parentPageId", -1);

      if (pageId == -1) {
        return context.writeError("Page ID is required");
      }

      WebPage page = WebPageRepository.findById(pageId);
      if (page == null) {
        return context.writeError("Page not found");
      }

      Long parentId = parentPageId > 0 ? parentPageId : null;

      try (Connection connection = DB.getConnection();
          AutoStartTransaction transactionStart = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {

        WebPageHierarchy existingRecord = WebPageHierarchyRepository.findByPageId(connection, pageId);
        if (existingRecord != null) {
          return context.writeOk("{\"message\":\"Page is already in hierarchy\"}", null);
        }

        WebPageHierarchy parentRecord = null;
        if (parentId != null) {
          parentRecord = WebPageHierarchyRepository.findByPageId(connection, parentId);
          if (parentRecord == null) {
            return context.writeError("Parent page not found in hierarchy");
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

      return context.writeOk("{\"message\":\"Page added to hierarchy\"}", null);

    } catch (Exception e) {
      LOG.error("Error adding page to hierarchy: " + e.getMessage(), e);
      return context.writeError("An unexpected error occurred");
    }
  }
}
