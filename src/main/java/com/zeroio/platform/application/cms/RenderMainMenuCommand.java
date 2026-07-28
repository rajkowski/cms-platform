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
package com.zeroio.platform.application.cms;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.LoadMenuTabsCommand;
import com.simisinc.platform.application.cms.ValidateUserAccessToWebPageCommand;
import com.simisinc.platform.domain.model.cms.MenuItem;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.presentation.controller.UserSession;

/**
 * Render the main menu
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class RenderMainMenuCommand {

  private static Log LOG = LogFactory.getLog(RenderMainMenuCommand.class);

  public static List<MenuTab> renderMainMenu(String pagePath, UserSession userSession, Collection collection,
      boolean checkUser, boolean highlightActiveTab, boolean highlightSubmenuItem) {

    List<MenuTab> menuTabList = LoadMenuTabsCommand.loadActiveIncludeMenuItemList();
    List<MenuTab> menuTabListToUse = new ArrayList<>();

    int menuTabCounter = 0;
    for (MenuTab menuTab : menuTabList) {
      ++menuTabCounter;
      // Remove redundant Home (the first one)
      if (menuTabCounter == 1 && menuTab.getLink().equals("/")) {
        continue;
      }
      // Verify the content manager, or that the page has content for other users, based on content, roles and groups
      if (userSession.hasRole("admin") || userSession.hasRole("content-manager") || !checkUser ||
          ValidateUserAccessToWebPageCommand.hasAccess(menuTab.getLink(), userSession)) {
        // Copy the MenuTab, since a cache was used
        MenuTab thisMenuTab = new MenuTab();
        thisMenuTab.setName(menuTab.getName());
        thisMenuTab.setLink(menuTab.getLink());
        thisMenuTab.setIcon(menuTab.getIcon());
        // Determine if the menuTab should be highlighted
        if (highlightActiveTab) {
          // Is active when menuTab matches the page path, or the collection name is a match
          if ((menuTab.getLink().equals(pagePath)) ||
              (collection != null && collection.getName().equalsIgnoreCase(menuTab.getName())) ||
              (collection != null && StringUtils.isNotBlank(collection.getListingsLink())
                  && menuTab.getLink().equals(collection.getListingsLink()))) {
            thisMenuTab.setActive(true);
          }
        }
        // Process the sub-menu items
        if (menuTab.getMenuItemList() != null) {
          List<MenuItem> thisMenuItemList = new ArrayList<>();
          for (MenuItem menuItem : menuTab.getMenuItemList()) {
            if (ValidateUserAccessToWebPageCommand.hasAccess(menuItem.getLink(), userSession)) {
              // Copy the menu item, since a cache was used
              MenuItem thisMenuItem = new MenuItem();
              thisMenuItem.setName(menuItem.getName());
              thisMenuItem.setLink(menuItem.getLink());
              // Is active when menuItem matches the page path
              if (thisMenuItem.getLink().equals(pagePath)) {
                if (highlightActiveTab) {
                  thisMenuTab.setActive(true);
                }
                if ((highlightSubmenuItem)) {
                  thisMenuItem.setActive(true);
                }
              }
              thisMenuItemList.add(thisMenuItem);
            }
          }
          if (!thisMenuItemList.isEmpty()) {
            thisMenuTab.setMenuItemList(thisMenuItemList);
          }
          menuTabListToUse.add(thisMenuTab);
        }
      }
    }
    return menuTabListToUse;
  }
}
