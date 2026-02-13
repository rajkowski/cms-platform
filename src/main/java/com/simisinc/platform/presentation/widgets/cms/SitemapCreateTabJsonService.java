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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.SaveMenuTabCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX POST requests for /json/sitemap/create-tab endpoint
 * Creates a new menu tab from a dragged page
 *
 * @author matt rajkowski
 * @created 2/9/26 8:30 PM
 */
public class SitemapCreateTabJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(SitemapCreateTabJsonService.class);

  /**
   * Handles POST requests to create a new menu tab
   *
   * @param context the widget context
   * @return context with JSON response
   */
  @Override
  public WidgetContext post(WidgetContext context) {

    // Check permissions - require admin or content-manager role
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      return writeError(context, "Permission denied");
    }

    try {
      // Get tab title and optional link
      String title = context.getParameter("title");
      String link = context.getParameter("link");

      if (StringUtils.isBlank(title)) {
        return writeError(context, "Tab title is required");
      }

      // Create the new menu tab
      MenuTab newTab = new MenuTab();
      newTab.setName(title);
      if (StringUtils.isNotBlank(link)) {
        if (!link.startsWith("/")) {
          newTab.setLink("/" + link);
        } else {
          newTab.setLink(link);
        }
      }

      MenuTab savedTab = SaveMenuTabCommand.appendNewTab(newTab);

      // Trigger cache refresh
      CacheManager.invalidateObjectCacheKey(CacheManager.MENU_TAB_LIST);

      // Build success response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"id\": ").append(savedTab.getId()).append(",");
      json.append("\"name\": \"").append(JsonCommand.toJson(savedTab.getName())).append("\",");
      json.append("\"link\": \"").append(JsonCommand.toJson(savedTab.getLink())).append("\"");
      json.append("}");

      return writeOk(context, json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error creating menu tab: " + e.getMessage(), e);
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
