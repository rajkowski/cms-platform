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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.MenuTabRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Handles JSON/AJAX POST requests for /json/sitemap/delete-tab endpoint
 * Deletes a menu tab from the site navigation
 *
 * @author matt rajkowski
 * @created 2/9/26 8:45 PM
 */
public class SitemapDeleteTabJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(SitemapDeleteTabJsonService.class);

  /**
   * Handles POST requests to delete a menu tab
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + SitemapDeleteTabJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    try {
      // Get tab ID
      long tabId = context.getParameterAsLong("tabId", -1);

      if (tabId == -1) {
        return context.writeError("Tab ID is required");
      }

      // Load the menu tab
      MenuTab menuTab = MenuTabRepository.findById(tabId);
      if (menuTab == null) {
        return context.writeError("Menu tab not found");
      }

      // Check if this is the first tab and has a link of '/', if so, deleting it
      if (MenuTabRepository.findAll().get(0).getId() == tabId && "/".equals(menuTab.getLink())) {
        return context.writeError("Cannot delete the home tab");
      }

      // Delete the tab
      if (MenuTabRepository.remove(menuTab)) {
        // Trigger cache refresh
        CacheManager.invalidateObjectCacheKey(CacheManager.MENU_TAB_LIST);
        return context.writeOk("{\"id\": " + tabId + "}", null);
      } else {
        return context.writeError("Failed to delete menu tab");
      }
    } catch (Exception e) {
      LOG.error("Error deleting menu tab: " + e.getMessage(), e);
      return context.writeError(e.getMessage());
    }
  }
}
