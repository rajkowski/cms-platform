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
import com.simisinc.platform.infrastructure.persistence.cms.MenuTabRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX POST requests for /json/sitemap/update-tab endpoint
 * Updates an existing menu tab
 *
 * @author matt rajkowski
 * @created 2/12/26 9:00 PM
 */
public class SitemapUpdateTabJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908897L;
  private static Log LOG = LogFactory.getLog(SitemapUpdateTabJsonService.class);

  /**
   * Handles POST requests to update a menu tab
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
      // Get tab ID
      long tabId = context.getParameterAsLong("tabId", -1);

      if (tabId == -1) {
        return writeError(context, "Tab ID is required");
      }

      // Load the menu tab
      MenuTab menuTab = MenuTabRepository.findById(tabId);
      if (menuTab == null) {
        return writeError(context, "Menu tab not found");
      }

      // Prevent updating the home tab
      if (MenuTabRepository.findAll().get(0).getId() == tabId && "/".equals(menuTab.getLink())) {
        return writeError(context, "Cannot update the home tab");
      }

      // Get and validate update parameters
      String title = context.getParameter("title");
      String link = context.getParameter("link");
      String icon = context.getParameter("icon");

      if (StringUtils.isNotBlank(title)) {
        menuTab.setName(title);
      }

      if (StringUtils.isNotBlank(link)) {
        // Ensure link starts with /
        if (!link.startsWith("/") && !link.startsWith("#")) {
          link = "/" + link;
        }
        menuTab.setLink(link);
      }

      if (icon != null) {
        menuTab.setIcon(StringUtils.trimToNull(icon));
      }

      // Save the updated tab
      MenuTab savedTab = SaveMenuTabCommand.renameTab(menuTab);

      // Trigger cache refresh
      CacheManager.invalidateObjectCacheKey(CacheManager.MENU_TAB_LIST);

      // Build success response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"id\": ").append(savedTab.getId()).append(",");
      json.append("\"title\": \"").append(JsonCommand.toJson(savedTab.getName())).append("\",");
      json.append("\"link\": \"").append(JsonCommand.toJson(savedTab.getLink())).append("\"");
      if (savedTab.getIcon() != null) {
        json.append(",\"icon\": \"").append(JsonCommand.toJson(savedTab.getIcon())).append("\"");
      }
      json.append("}");

      return writeOk(context, json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error updating menu tab: " + e.getMessage(), e);
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
