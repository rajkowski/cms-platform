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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.MenuItem;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.MenuItemRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX POST requests for /json/sitemap/delete-item endpoint
 * Deletes a menu item from the site navigation
 *
 * @author matt rajkowski
 * @created 2/9/26 8:45 PM
 */
public class SitemapDeleteItemJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908896L;
  private static Log LOG = LogFactory.getLog(SitemapDeleteItemJsonService.class);

  /**
   * Handles POST requests to delete a menu item
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
      // Get item ID
      long itemId = context.getParameterAsLong("itemId", -1);

      if (itemId == -1) {
        return writeError(context, "Item ID is required");
      }

      // Load the menu item
      MenuItem menuItem = MenuItemRepository.findById(itemId);
      if (menuItem == null) {
        return writeError(context, "Menu item not found");
      }

      // Delete the item
      if (MenuItemRepository.remove(menuItem)) {
        // Trigger cache refresh
        CacheManager.invalidateObjectCacheKey(CacheManager.MENU_TAB_LIST);
        return writeOk(context, "{\"id\": " + itemId + "}", null);
      } else {
        return writeError(context, "Failed to delete menu item");
      }

    } catch (Exception e) {
      LOG.error("Error deleting menu item: " + e.getMessage(), e);
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
