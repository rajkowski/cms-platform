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

import com.simisinc.platform.application.cms.LoadPageTreeCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.PageTreeNode;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX GET requests for /json/pages/children endpoint
 * Returns child pages for lazy tree loading
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class PageChildrenJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(PageChildrenJsonService.class);

  /**
   * Handles GET requests for child pages
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext execute(WidgetContext context) {

    // Check permissions - require admin or content-editor role
    // Changed from content-manager to content-editor for consistency with WebPageListAjax
    // and other Visual Page Editor endpoints
    if (!context.hasRole("admin") && !context.hasRole("content-editor")) {
      return writeError(context, "Permission denied");
    }

    try {
      // Get parent ID parameter (null means root level pages)
      String parentIdParam = context.getParameter("parentId");
      Long parentId = null;
      
      // Handle various "no parent" values: null, "null", "0", "undefined", empty string
      if (StringUtils.isNotBlank(parentIdParam) && !"null".equals(parentIdParam) && !"0".equals(parentIdParam) && !"undefined".equals(parentIdParam)) {
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

      return writeOk(context, json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error loading page children: " + e.getMessage(), e);
      return writeError(context, e.getMessage());
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
