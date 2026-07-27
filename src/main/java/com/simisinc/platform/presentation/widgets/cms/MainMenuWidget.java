/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.cms.RenderMainMenuCommand;

/**
 * Displays the site's main menu
 *
 * @author matt rajkowski
 * @created 1/18/21 8:57 PM
 */
public class MainMenuWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/cms/main-menu.jsp";
  static String TEMPLATE = "/cms/main-menu.html";
  static String FLAT_JSP = "/cms/main-menu-flat.jsp";
  static String FLAT_TEMPLATE = "/cms/main-menu-flat.jsp";
  static String NESTED_JSP = "/cms/main-menu-nested.jsp";
  static String NESTED_TEMPLATE = "/cms/main-menu-nested.html";

  public WidgetContext execute(WidgetContext context) {

    // Determine if the site menu can be shown
    boolean siteIsOnline = LoadSitePropertyCommand.loadByNameAsBoolean("site.online");
    if (!context.getUserSession().isLoggedIn() && !siteIsOnline) {
      return context;
    }

    // Check for preferences
    String view = context.getPreferences().get("view");
    boolean checkUser = "true".equals(context.getPreferences().getOrDefault("checkUser", "true"));
    boolean highlightActiveTab = Boolean.parseBoolean(context.getPreferences().getOrDefault("useHighlight", "true"));
    context.getRequest().setAttribute("useHighlight", highlightActiveTab ? "true" : "false");
    boolean highlightSubmenuItem = Boolean.parseBoolean(context.getPreferences().getOrDefault("highlightSubmenuItem", "true"));
    context.getRequest().setAttribute("highlightSubmenuItem", highlightSubmenuItem ? "true" : "false");
    boolean useSmallHighlight = Boolean.parseBoolean(context.getPreferences().getOrDefault("useSmallHighlight", "false"));
    context.getRequest().setAttribute("useSmallHighlight", useSmallHighlight ? "true" : "false");
    context.getRequest().setAttribute("showAdmin", context.getPreferences().getOrDefault("showAdmin", "true"));
    context.getRequest().setAttribute("menuClass", context.getPreferences().get("class"));
    context.getRequest().setAttribute("submenuIcon", context.getPreferences().get("submenuIcon"));
    context.getRequest().setAttribute("submenuIconClass", context.getPreferences().get("submenuIconClass"));

    // Check for a collection to match the title to
    Collection collection = null;
    String collectionUniqueId = context.getCoreData().get("collectionUniqueId");
    if (collectionUniqueId != null) {
      collection = LoadCollectionCommand.loadCollectionByUniqueId(collectionUniqueId);
      context.getRequest().setAttribute("collection", collection);
    }

    // Prepare the menu based on the user
    List<MenuTab> menuTabListToUse = RenderMainMenuCommand.renderMainMenu(context.getRequest().getPagePath(), context.getUserSession(),
        collection, checkUser, highlightActiveTab, highlightSubmenuItem);
    context.getRequest().setAttribute(RequestConstants.MASTER_MENU_TAB_LIST, menuTabListToUse);

    // Show the JSP
    if ("flat".equals(view)) {
      context.setJsp(FLAT_JSP);
      context.setTemplate(FLAT_TEMPLATE);
    } else if ("nested".equals(view)) {
      context.setJsp(NESTED_JSP);
      context.setTemplate(NESTED_TEMPLATE);
    } else {
      context.setJsp(JSP);
      context.setTemplate(TEMPLATE);
    }
    return context;
  }
}
