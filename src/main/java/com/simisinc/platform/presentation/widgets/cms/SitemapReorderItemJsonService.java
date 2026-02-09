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
import com.simisinc.platform.infrastructure.persistence.cms.MenuItemRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX POST requests for /json/sitemap/reorder-item endpoint
 * Moves menu items to different menus or reorders within menu
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class SitemapReorderItemJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(SitemapReorderItemJsonService.class);

  /**
   * Handles POST requests to reorder menu items
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
      // Get item ID and optional target item ID or new menu ID
      long itemId = context.getParameterAsLong("itemId", -1);
      long targetItemId = context.getParameterAsLong("targetItemId", -1);
      long newMenuTabId = context.getParameterAsLong("newMenuTabId", -1);
      int newPosition = context.getParameterAsInt("newPosition", 0);

      if (itemId == -1) {
        return writeError(context, "Item ID is required");
      }

      // At least one of targetItemId or newMenuTabId must be provided
      if (targetItemId == -1 && newMenuTabId == -1) {
        return writeError(context, "Target item or menu ID is required");
      }

      // Load the menu item
      MenuItem item = MenuItemRepository.findById(itemId);
      if (item == null) {
        return writeError(context, "Menu item not found");
      }

      // If newMenuTabId is provided, update the menu ID
      if (newMenuTabId != -1) {
        item.setMenuTabId(newMenuTabId);
      }

      // If targetItemId is provided, update the order based on target item
      if (targetItemId != -1) {
        MenuItem targetItem = MenuItemRepository.findById(targetItemId);
        if (targetItem != null) {
          item.setItemOrder(targetItem.getItemOrder());
          // Also update menu if different
          if (newMenuTabId == -1) {
            item.setMenuTabId(targetItem.getMenuTabId());
          }
        }
      }

      // Save the updated item
      MenuItemRepository.save(item);

      // Build success response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"id\": ").append(item.getId()).append(",");
      json.append("\"menuId\": ").append(item.getMenuTabId()).append(",");
      json.append("\"order\": ").append(item.getItemOrder());
      json.append("}");

      return writeOk(context, json.toString(), null);


    } catch (Exception e) {
      LOG.error("Error reordering menu item: " + e.getMessage(), e);
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
