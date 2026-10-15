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

package com.zeroio.platform.presentation.widgets.cms;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.FontCommand;
import com.simisinc.platform.application.cms.LoadTableOfContentsCommand;
import com.simisinc.platform.domain.model.cms.TableOfContents;
import com.simisinc.platform.domain.model.cms.TableOfContentsLink;
import com.simisinc.platform.presentation.controller.WebComponentCommand;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.simisinc.platform.presentation.widgets.cms.PreferenceEntriesList;
import com.zeroio.platform.application.cms.ResolveHeaderLinkCommand;

/**
 * Displays a menu as standalone links, when container is specified a drop-down menu is used
 *
 * @author matt rajkowski
 * @created 1/29/2021 12:00 PM
 */
public class MenuWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(MenuWidget.class);

  static String JSP = "/cms/menu.jsp";
  static String TEMPLATE = "/cms/menu.html";

  public WidgetContext execute(WidgetContext context) {

    context.getRequest().setAttribute("menuId", context.getPreferences().get("id"));
    context.getRequest().setAttribute("menuClass", context.getPreferences().get("class"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));
    context.getRequest().setAttribute("wrap", context.getPreferences().getOrDefault("wrap", "true"));
    boolean highlightActiveMenuItem = Boolean
        .parseBoolean(context.getPreferences().getOrDefault("useHighlight", "false"));
    context.getRequest().setAttribute("useHighlight", highlightActiveMenuItem ? "true" : "false");
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "false"));
    boolean redirectToFirstTabWithAccess = "true"
        .equals(context.getPreferences().getOrDefault("redirectToFirstTabWithAccess", "false"));
    boolean showWhenOneEntry = "true".equals(context.getPreferences().getOrDefault("showWhenOneEntry", "true"));

    List<Map<String, String>> linkList = new ArrayList<>();
    List<String> addedContainers = new ArrayList<>();
    List<String> skippedContainers = new ArrayList<>();

    // Add links from the tocUniqueId preference
    String tableOfContentsUniqueId = context.getPreferences().get("tocUniqueId");
    if (StringUtils.isNotBlank(tableOfContentsUniqueId)) {
      LOG.debug("Checking tocUniqueId: " + tableOfContentsUniqueId);
      TableOfContents tableOfContents = LoadTableOfContentsCommand.loadByUniqueId(tableOfContentsUniqueId, false);
      if (tableOfContents != null && tableOfContents.hasEntries()) {
        for (TableOfContentsLink link : tableOfContents.getEntries()) {
          addLink(context, linkList, link.getName(), link.getLink(), null, null, null);
        }
      }
      // Configure the content editor
      if (context.hasRole("admin") || context.hasRole("content-manager")) {
        LOG.debug("Has content manager access!");
        context.getRequest().setAttribute("uniqueId", tableOfContentsUniqueId);
        context.getRequest().setAttribute("showEditor", "true");
        context.getRequest().setAttribute("returnPage", context.getRequest().getRequestURI());
        showWhenEmpty = true;
      }
    }

    // The TOC must have entries, or the links preference
    PreferenceEntriesList entriesList = context.getPreferenceAsDataList("links");
    if (linkList.isEmpty() && entriesList.isEmpty() && !showWhenEmpty) {
      LOG.debug("Links preference is empty");
      return null;
    }

    // Add links from the specified entriesList
    for (Map<String, String> valueMap : entriesList) {
      try {
        // Check the link preferences, and any special settings
        String link = valueMap.get("link");
        String menuItemClass = valueMap.get("class");
        String container = valueMap.get("container");
        String roleValue = valueMap.get("role");
        String groupValue = valueMap.get("group");
        String ruleValue = valueMap.get("rule");

        // Determine access requirements
        boolean hasAccess = true;
        if (StringUtils.isNotBlank(roleValue) || StringUtils.isNotBlank(groupValue)) {
          hasAccess = checkUserAccess(context, roleValue, groupValue);
        }

        // Determine if this container can be shown
        if (StringUtils.isNotBlank(container)) {
          if (!addedContainers.contains(container) && !skippedContainers.contains(container)) {
            // First time this container has been seen, that determines the menu displaying
            if (hasAccess) {
              addedContainers.add(container);
            } else {
              skippedContainers.add(container);
            }
          } else if (skippedContainers.contains(container)) {
            continue;
          }
        }

        // Determine if this menu item can be shown
        if (!hasAccess) {
          continue;
        }

        // See if the link has a rule
        if (LOG.isDebugEnabled() && ruleValue != null) {
          LOG.debug("Checking link rules: " + link + ": [" + ruleValue + "]");
        }
        if (!checkRules(ruleValue)) {
          continue;
        }
        link = ResolveHeaderLinkCommand.resolveLink(link, context.getContextPath());

        // Adjust for being flexible with icons
        String icon = valueMap.get("icon");
        if (StringUtils.isNotBlank(icon) && !icon.contains(" ")) {
          icon = FontCommand.fas() + " " + icon;
        }

        // Prepare the link object
        String menuName = valueMap.get("name");
        if ("${user.name}".equals(menuName)) {
          menuName = context.getUserSession().getUser().getFullName();
        }
        Map<String, String> properties = new HashMap<>();
        addProperty(context, properties, "name", menuName);
        addProperty(context, properties, "link", link);
        addProperty(context, properties, "class", menuItemClass);
        addProperty(context, properties, "container", container);
        addProperty(context, properties, "icon", icon);
        addProperty(context, properties, "icon-only", valueMap.get("icon-only"));
        if (highlightActiveMenuItem && link.equals(context.getRequest().getPagePath())) {
          addProperty(context, properties, "active", "true");
        }
        linkList.add(properties);
      } catch (Exception e) {
        LOG.error("Could not get property: " + e.getMessage());
      }
    }
    if (!showWhenEmpty && linkList.isEmpty()) {
      return context;
    }
    if (!showWhenOneEntry && linkList.size() == 1) {
      return context;
    }
    context.getRequest().setAttribute("linkList", linkList);

    // A feature so that this landing page redirects to the first page the user has access to
    if (redirectToFirstTabWithAccess) {
      if (linkList.isEmpty()) {
        return null;
      }
      Map<String, String> linkProperties = linkList.get(0);
      String link = linkProperties.get("link");
      context.setRedirect(link);
      return context;
    }

    // Show the JSP
    context.setJsp(JSP);
    context.setTemplate(TEMPLATE);
    return context;
  }

  private static boolean checkUserAccess(WidgetContext context, String roleValue, String groupValue) {

    List<String> roles = new ArrayList<>();
    if (StringUtils.isNotBlank(roleValue)) {
      roles = Stream.of(roleValue.split(","))
          .map(String::trim)
          .collect(toList());
    }

    List<String> groups = new ArrayList<>();
    if (StringUtils.isNotBlank(groupValue)) {
      groups = Stream.of(groupValue.split(","))
          .map(String::trim)
          .collect(toList());
    }

    return WebComponentCommand.allowsUser(roles, groups, context.getUserSession());
  }

  private static boolean checkRules(String ruleValue) {
    if (StringUtils.isBlank(ruleValue)) {
      return true;
    }
    List<String> rules = Stream.of(ruleValue.split(","))
        .map(String::trim)
        .collect(toList());
    for (String rule : rules) {
      // Assumes rule is just the site property that must be true
      if (!"true".equals(LoadSitePropertyCommand.loadByName(rule))) {
        return false;
      }
    }
    return true;
  }

  private static void addProperty(WidgetContext context, Map<String, String> map, String name, String value) {
    if (StringUtils.isBlank(value)) {
      return;
    }
    String pagePath = (String) context.getRequest().getAttribute("pagePath");
    value = Strings.CS.replace(value, "${pagePath}", (pagePath));
    map.put(name, value);
  }

  private static void addLink(WidgetContext context, List<Map<String, String>> linkList, String name, String link,
      String icon, String container, String roleValue) {
    // Check for access to this menu item
    if (StringUtils.isNotBlank(roleValue) && !checkUserAccess(context, roleValue, null)) {
      return;
    }

    // Prepare the link
    Map<String, String> properties = new HashMap<>();
    addProperty(context, properties, "name", name);
    addProperty(context, properties, "link", link);
    addProperty(context, properties, "container", container);
    addProperty(context, properties, "icon", icon);
    //addProperty(context, properties, "icon-only", valueMap.get("icon-only"));
    linkList.add(properties);
  }
}
