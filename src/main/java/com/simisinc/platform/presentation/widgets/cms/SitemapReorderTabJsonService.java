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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.MenuTabRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX POST requests for /json/sitemap/reorder-tab endpoint
 * Reorders menu tabs
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class SitemapReorderTabJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(SitemapReorderTabJsonService.class);

  /**
   * Handles POST requests to reorder menu tabs
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
      // Get the dragged tab ID and target tab ID
      long tabId = context.getParameterAsLong("tabId", -1);
      long targetTabId = context.getParameterAsLong("targetTabId", -1);

      if (tabId == -1 || targetTabId == -1) {
        return writeError(context, "Tab ID and target tab ID are required");
      }

      // Load the dragged tab and target tab
      MenuTab draggedTab = MenuTabRepository.findById(tabId);
      MenuTab targetTab = MenuTabRepository.findById(targetTabId);

      if (draggedTab == null || targetTab == null) {
        return writeError(context, "One or both tabs not found");
      }

      // Get all tabs sorted by current order
      List<MenuTab> allTabs = new ArrayList<>(MenuTabRepository.findAll());

      // Remove the dragged tab from its current position
      allTabs.removeIf(tab -> tab.getId() == draggedTab.getId());

      // Find the insertion position (index of target tab)
      int insertPosition = -1;
      for (int i = 0; i < allTabs.size(); i++) {
        if (allTabs.get(i).getId() == targetTab.getId()) {
          insertPosition = i;
          break;
        }
      }

      if (insertPosition == -1) {
        return writeError(context, "Target tab not found in list");
      }

      // Insert dragged tab at the target position
      allTabs.add(insertPosition, draggedTab);

      // Reassign all tab orders sequentially and save
      for (int i = 0; i < allTabs.size(); i++) {
        MenuTab tab = allTabs.get(i);
        tab.setTabOrder(i);
        MenuTabRepository.save(tab);
      }

      // Trigger cache refresh
      CacheManager.invalidateObjectCacheKey(CacheManager.MENU_TAB_LIST);

      // Build success response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"id\": ").append(draggedTab.getId()).append(",");
      json.append("\"order\": ").append(draggedTab.getTabOrder());
      json.append("}");

      return writeOk(context, json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error reordering menu tab: " + e.getMessage(), e);
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
