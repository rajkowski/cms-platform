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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.MenuTabRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Handles JSON/AJAX POST requests for /json/sitemap/reorder-tab endpoint
 * Reorders menu tabs
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class SitemapReorderTabJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(SitemapReorderTabJsonService.class);

  /**
   * Handles POST requests to reorder menu tabs
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + SitemapReorderTabJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    try {
      // Get the dragged tab ID and target tab ID
      long tabId = context.getParameterAsLong("tabId", -1);
      long targetTabId = context.getParameterAsLong("targetTabId", -1);

      if (tabId == -1 || targetTabId == -1) {
        return context.writeError("Tab ID and target tab ID are required");
      }

      // Load the dragged tab and target tab
      MenuTab draggedTab = MenuTabRepository.findById(tabId);
      MenuTab targetTab = MenuTabRepository.findById(targetTabId);

      if (draggedTab == null || targetTab == null) {
        return context.writeError("One or both tabs not found");
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
        return context.writeError("Target tab not found in list");
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

      return context.writeOk(json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error reordering menu tab: " + e.getMessage(), e);
      return context.writeError("An unexpected error occurred");
    }
  }

}
