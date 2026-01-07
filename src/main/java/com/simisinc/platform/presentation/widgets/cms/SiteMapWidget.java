/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.DeleteMenuTabCommand;
import com.simisinc.platform.application.cms.LoadMenuTabsCommand;
import com.simisinc.platform.application.cms.SaveMenuTabCommand;
import com.simisinc.platform.domain.model.cms.MenuItem;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.MenuItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.MenuTabRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * A dynamic site map editor with live preview
 *
 * @author matt rajkowski
 * @created 1/6/26 8:45 PM
 */
public class SiteMapWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/admin/sitemap.jsp";

  public WidgetContext execute(WidgetContext context) {

    List<MenuTab> menuTabList = LoadMenuTabsCommand.findAllIncludeMenuItemList();
    context.getRequest().setAttribute("menuTabList", menuTabList);

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Show the editor
    context.setJsp(JSP);
    return context;
  }

  public WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException {
    WidgetContext updatedContext = null;
    // Check if we have new JSON format data
    String sitemapData = context.getParameter("sitemapData");
    if (StringUtils.isBlank(sitemapData)) {
      context.setErrorMessage("No sitemap data was provided");
      return context;
    }
    updatedContext = processJsonSitemapData(context, sitemapData);
    // Trigger cache refresh
    CacheManager.invalidateObjectCacheKey(CacheManager.MENU_TAB_LIST);
    return updatedContext;
  }

  private WidgetContext processJsonSitemapData(WidgetContext context, String sitemapData) {
    LOG.debug("processJsonSitemapData...");

    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode rootNode = mapper.readTree(sitemapData);
      JsonNode tabsNode = rootNode.get("tabs");
      JsonNode deletedTabsNode = rootNode.get("deletedTabs");
      JsonNode deletedItemsNode = rootNode.get("deletedItems");

      // Process deletions first
      if (deletedItemsNode != null && deletedItemsNode.isArray()) {
        for (JsonNode itemIdNode : deletedItemsNode) {
          long itemId = itemIdNode.asLong();
          MenuItem menuItem = MenuItemRepository.findById(itemId);
          if (menuItem != null) {
            try {
              DeleteMenuTabCommand.deleteMenuItem(menuItem);
            } catch (Exception e) {
              LOG.error("Error deleting menu item " + itemId + ": " + e.getMessage());
            }
          }
        }
      }

      if (deletedTabsNode != null && deletedTabsNode.isArray()) {
        for (JsonNode tabIdNode : deletedTabsNode) {
          long tabId = tabIdNode.asLong();
          MenuTab menuTab = MenuTabRepository.findById(tabId);
          if (menuTab != null) {
            try {
              DeleteMenuTabCommand.deleteMenuTab(menuTab);
            } catch (Exception e) {
              LOG.error("Error deleting menu tab " + tabId + ": " + e.getMessage());
            }
          }
        }
      }

      if (tabsNode != null && tabsNode.isArray()) {
        // Process each tab
        for (JsonNode tabNode : tabsNode) {
          long tabId = tabNode.get("id").asLong();
          String name = tabNode.get("name").asText();
          String link = tabNode.get("link").asText();
          String icon = tabNode.get("icon").asText();
          int order = tabNode.get("order").asInt();
          boolean isHome = tabNode.get("isHome").asBoolean();

          // Handle new tabs (negative IDs)
          if (tabId < 0) {
            // Create new tab
            MenuTab newTab = new MenuTab();
            newTab.setName(name);
            newTab.setLink(link);
            newTab.setIcon(icon);
            try {
              MenuTab savedTab = SaveMenuTabCommand.appendNewTab(newTab);
              if (savedTab != null) {
                tabId = savedTab.getId(); // Update with real ID for menu items
              }
            } catch (DataException e) {
              LOG.error("Error creating new tab: " + e.getMessage());
              continue;
            }
          } else if (!isHome) {
            // Update existing tab
            MenuTab existingTab = MenuTabRepository.findById(tabId);
            if (existingTab != null) {
              existingTab.setName(name);
              existingTab.setLink(link);
              existingTab.setIcon(icon);
              try {
                SaveMenuTabCommand.renameTab(existingTab);
              } catch (DataException e) {
                LOG.error("Error updating tab: " + e.getMessage());
              }
            }
          }

          // Process menu items for this tab
          JsonNode itemsNode = tabNode.get("items");
          if (itemsNode != null && itemsNode.isArray() && !isHome) {
            for (JsonNode itemNode : itemsNode) {
              long itemId = itemNode.get("id").asLong();
              String itemName = itemNode.get("name").asText();
              String itemLink = itemNode.get("link").asText();
              int itemOrder = itemNode.get("order").asInt();

              if (itemId < 0) {
                // Create new menu item
                try {
                  MenuTab parentTab = MenuTabRepository.findById(tabId);
                  if (parentTab != null) {
                    SaveMenuTabCommand.appendNewMenuItem(parentTab, itemName, itemLink);
                  }
                } catch (DataException e) {
                  LOG.error("Error creating new menu item: " + e.getMessage());
                }
              } else {
                // Update existing menu item
                MenuItem existingItem = MenuItemRepository.findById(itemId);
                if (existingItem != null && (!itemName.equals(existingItem.getName()) || !itemLink.equals(existingItem.getLink()))) {
                  existingItem.setName(itemName);
                  existingItem.setLink(itemLink);
                  try {
                    SaveMenuTabCommand.renameMenuItem(existingItem);
                  } catch (DataException e) {
                    LOG.error("Error updating menu item: " + e.getMessage());
                  }
                }
              }
            }
          }
        }

        // Update tab order - collect real tab IDs in order
        Long[] tabOrder = new Long[tabsNode.size()];
        int orderIndex = 0;
        for (JsonNode tabNode : tabsNode) {
          long tabId = tabNode.get("id").asLong();
          boolean isHome = tabNode.get("isHome").asBoolean();
          if (isHome) {
            tabOrder[orderIndex] = 0L; // Home tab is always ID 0 in the system
          } else {
            tabOrder[orderIndex] = tabId < 0 ? 0L : tabId; // For new tabs, we'd need to look up the real ID
          }
          orderIndex++;
        }

        if (tabOrder.length > 0) {
          SaveMenuTabCommand.updateTabOrder(tabOrder);
        }

        // Update menu item order
        for (JsonNode tabNode : tabsNode) {
          long tabId = tabNode.get("id").asLong();
          boolean isHome = tabNode.get("isHome").asBoolean();

          if (!isHome) {
            JsonNode itemsNode = tabNode.get("items");
            if (itemsNode != null && itemsNode.isArray()) {
              int itemOrderValue = 0;
              for (JsonNode itemNode : itemsNode) {
                long itemId = itemNode.get("id").asLong();
                if (itemId > 0) { // Only update existing items
                  SaveMenuTabCommand.updateMenuItemOrder(tabId, itemId, itemOrderValue);
                }
                itemOrderValue++;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Error processing JSON sitemap data: " + e.getMessage(), e);
      context.setErrorMessage("Error saving sitemap changes: " + e.getMessage());
      return context;
    }
    context.setRedirect("/admin/sitemap");
    return context;
  }
}
