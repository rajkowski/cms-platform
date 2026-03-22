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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.cms.LoadPageTreeCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.PageTreeNode;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Handles JSON/AJAX GET requests for /json/pages/children endpoint
 * Returns child pages for lazy tree loading
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class PageChildrenJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(PageChildrenJsonService.class);

  /**
   * Handles GET requests for child pages
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public JsonServiceContext get(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + PageChildrenJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    try {
      // Get parent ID parameter (null means root level pages)
      String parentIdParam = context.getParameter("parentId");
      Long parentId = null;

      // Handle various "no parent" values: null, "null", "0", "undefined", empty string
      if (StringUtils.isNotBlank(parentIdParam) && !"null".equals(parentIdParam) && !"0".equals(parentIdParam)
          && !"undefined".equals(parentIdParam)) {
        parentId = Long.parseLong(parentIdParam);
      }

      // Load child pages using LoadPageTreeCommand
      List<PageTreeNode> children = LoadPageTreeCommand.loadPageTree(parentId);

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("[");

      if (children != null && !children.isEmpty()) {
        boolean first = true;
        for (PageTreeNode node : children) {
          if (!first) {
            json.append(",");
          }
          first = false;

          json.append("{");
          json.append("\"id\": ").append(node.getId()).append(",");
          json.append("\"title\": \"").append(JsonCommand.toJson(node.getTitle())).append("\",");
          json.append("\"link\": \"").append(JsonCommand.toJson(node.getLink())).append("\",");
          json.append("\"hasChildren\": ").append(node.isHasChildren());
          json.append("}");
        }
      }

      json.append("]");

      return context.writeOk(json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error loading page children: " + e.getMessage(), e);
      return context.writeError(e.getMessage());
    }
  }
}
