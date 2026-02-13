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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.MenuItem;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.MenuItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.MenuTabRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX GET requests for /json/sitemap/structure endpoint
 * Returns complete sitemap with menu tabs and items
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class SitemapStructureJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(SitemapStructureJsonService.class);

  /**
   * Handles GET requests for sitemap structure
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext execute(WidgetContext context) {

    // Check permissions - require admin or content-manager role
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      return writeError(context, "Permission denied");
    }

    try {
      // Load all site menus (tabs)
      List<MenuTab> menus = MenuTabRepository.findAll();

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"menuTabs\": [");

      if (menus != null && !menus.isEmpty()) {
        boolean firstMenu = true;
        for (MenuTab menu : menus) {
          boolean isHomeTab = false;
          if (firstMenu && "/".equals(menu.getLink())) {
            isHomeTab = true;
          }
          if (!firstMenu) {
            json.append(",");
          }
          firstMenu = false;

          json.append("{");
          json.append("\"id\": ").append(menu.getId()).append(",");
          json.append("\"title\": \"").append(JsonCommand.toJson(menu.getName())).append("\",");
          json.append("\"link\": \"").append(JsonCommand.toJson(menu.getLink())).append("\",");
          json.append("\"order\": ").append(menu.getTabOrder()).append(",");
          if (isHomeTab) {
            json.append("\"home\": true,");
          }

          // Load menu items for this menu
          List<MenuItem> items = MenuItemRepository.findAllByMenuTab(menu);
          json.append("\"items\": [");

          if (items != null && !items.isEmpty()) {
            boolean firstItem = true;
            for (MenuItem item : items) {
              if (!firstItem) {
                json.append(",");
              }
              firstItem = false;

              json.append("{");
              json.append("\"id\": ").append(item.getId()).append(",");
              json.append("\"title\": \"").append(JsonCommand.toJson(item.getName())).append("\",");
              json.append("\"link\": \"").append(JsonCommand.toJson(item.getLink())).append("\",");
              json.append("\"order\": ").append(item.getItemOrder());
              json.append("}");
            }
          }

          json.append("]");
          json.append("}");
        }
      }

      json.append("]");

      // Include page library listing
      json.append(",\"pageLibrary\":[");
      List<WebPage> pages = WebPageRepository.findAll();
      if (pages != null && !pages.isEmpty()) {
        boolean firstPage = true;
        for (WebPage page : pages) {
          if (!firstPage) {
            json.append(",");
          }
          firstPage = false;
          String title = StringUtils.isNotBlank(page.getTitle()) ? page.getTitle() : page.getLink();
          json.append("{");
          json.append("\"id\": ").append(page.getId()).append(",");
          json.append("\"title\": \"").append(JsonCommand.toJson(title)).append("\",");
          json.append("\"link\": \"").append(JsonCommand.toJson(page.getLink())).append("\"");
          json.append("}");
        }
      }
      json.append("]");
      json.append("}");

      return writeOk(context, json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error loading sitemap structure: " + e.getMessage(), e);
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
