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
package com.zeroio.platform.presentation.widgets.cms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.FontCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.presentation.controller.WebComponentCommand;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.cms.ResolveHeaderLinkCommand;

public class HeaderAccountMenuWidget extends GenericWidget {

  private static final long serialVersionUID = 1L;

  private static final Set<String> ADMIN_ROLE = Set.of("admin");
  private static final Set<String> ALL_MANAGERS_ROLES = Set.of("admin", "community-manager", "content-manager", "data-manager");
  private static final Set<String> CONTENT_ROLES = Set.of("admin", "content-manager");
  private static final Set<String> COMMUNITY_ROLES = Set.of("admin", "community-manager");

  private static final Map<String, Set<String>> PERMISSIONS = Map.ofEntries(
      Map.entry("pageInfo", CONTENT_ROLES),
      Map.entry("pageAnalytics", CONTENT_ROLES),
      Map.entry("pageDesign", CONTENT_ROLES),
      Map.entry("files", CONTENT_ROLES),
      Map.entry("images", CONTENT_ROLES),
      Map.entry("hierarchy", CONTENT_ROLES),
      Map.entry("siteAnalytics", CONTENT_ROLES),
      Map.entry("instructions", ALL_MANAGERS_ROLES),
      Map.entry("users", COMMUNITY_ROLES),
      Map.entry("announcement", ADMIN_ROLE),
      Map.entry("siteCss", ADMIN_ROLE),
      Map.entry("setup", ADMIN_ROLE),
      Map.entry("appearance", Set.of()),
      Map.entry("account", Set.of()),
      Map.entry("logout", Set.of()),
      Map.entry("link", Set.of()));

  public static boolean supportsAction(String action) {
    return PERMISSIONS.containsKey(action);
  }

  @Override
  public WidgetContext execute(WidgetContext context) {
    if (!context.getUserSession().isLoggedIn()) {
      return context;
    }
    User user = context.getUserSession().getUser();
    if (user == null) {
      return context;
    }
    List<Map<String, Object>> sections = new ArrayList<>();
    Map<String, String> sectionLabels = new HashMap<>();
    String previousSection = null;
    List<Map<String, String>> actions = null;
    for (Map<String, String> entry : context.getPreferenceAsDataList("links")) {
      String enabled = entry.get("enabled");
      if (enabled != null && !"true".equals(enabled.trim()) && !"false".equals(enabled.trim())) {
        LOG.warn("Invalid account action enabled value; expected true or false; using true");
      }
      if (enabled != null && "false".equals(enabled.trim())) {
        continue;
      }
      if (StringUtils.isBlank(entry.get("name"))) {
        throw new IllegalArgumentException("Header account actions require a nonempty accessible label");
      }
      String action = entry.getOrDefault("action", "link");
      Set<String> ceiling = PERMISSIONS.get(action);
      if (ceiling == null) {
        throw new IllegalArgumentException("Unknown header account action: " + action);
      }
      if (!ceiling.isEmpty() && ceiling.stream().noneMatch(context::hasRole)) {
        continue;
      }
      if (!WebComponentCommand.allowsUser(values(entry.get("role")), values(entry.get("group")), context.getUserSession())) {
        continue;
      }
      boolean rulesAllowed = true;
      for (String rule : values(entry.get("rule"))) {
        rulesAllowed &= LoadSitePropertyCommand.loadByNameAsBoolean(rule);
      }
      if (!rulesAllowed) {
        continue;
      }
      Map<String, String> normalized = new HashMap<>(entry);
      normalized.put("action", action);
      if ("appearance".equals(action)) {
        if (StringUtils.isNotBlank(entry.get("link"))) {
          throw new IllegalArgumentException("The appearance action does not accept a destination");
        }
      } else {
        String destination = destination(context, action, entry.get("link"));
        if (destination == null) {
          LOG.warn("Missing current-page context for header action: " + action);
          continue;
        }
        normalized.put("link", ResolveHeaderLinkCommand.resolveLink(destination, context.getContextPath()));
      }
      String icon = entry.get("icon");
      if (StringUtils.isNotBlank(icon) && !icon.contains(" ")) {
        normalized.put("icon", FontCommand.fas() + " " + icon);
      }
      String section = entry.getOrDefault("section", "user");
      String label = entry.getOrDefault("sectionLabel", "");
      String knownLabel = sectionLabels.putIfAbsent(section, label);
      if (knownLabel != null && !knownLabel.equals(label)) {
        throw new IllegalArgumentException("Header section labels must agree: " + section);
      }
      if (!section.equals(previousSection)) {
        actions = new ArrayList<>();
        Map<String, Object> group = new HashMap<>();
        group.put("label", label);
        group.put("actions", actions);
        sections.add(group);
        previousSection = section;
      }
      if (actions != null) {
        actions.add(normalized);
      }
    }
    if (sections.isEmpty()) {
      return context;
    }
    context.getRequest().setAttribute("headerAccountName", user.getFullName());
    context.getRequest().setAttribute("headerAccountTitle", context.getPreferences().getOrDefault("title", "User Menu"));
    context.getRequest().setAttribute("headerAccountId", context.getUniqueId() + "-account-menu");
    context.getRequest().setAttribute("headerAccountSections", sections);
    context.setJsp("/cms/header-account-menu.jsp");
    return context;
  }

  private static String[] values(String value) {
    return StringUtils.isBlank(value) ? new String[0] : value.trim().split("\\s*,\\s*");
  }

  private static String destination(WidgetContext context, String action, String override) {
    String pagePath = context.getRequest().getPagePath();
    Object page = context.getRequest().getAttribute("webPage");
    String masterLink = page instanceof WebPage ? ((WebPage) page).getLink() : null;
    String destination;
    String parameter = null;
    String value = null;
    switch (action) {
      case "pageInfo":
        destination = "/admin/web-page";
        parameter = "webPage";
        value = pagePath;
        break;
      case "pageAnalytics":
        destination = "/page-analytics";
        parameter = "webPage";
        value = StringUtils.isNotBlank(pagePath) ? pagePath : masterLink;
        break;
      case "pageDesign":
        destination = "/admin/visual-page-editor";
        parameter = "webPage";
        value = masterLink;
        break;
      case "files":
        destination = "/admin/visual-document-editor";
        parameter = "returnPage";
        value = pagePath;
        break;
      case "images":
        destination = "/admin/visual-image-editor";
        parameter = "returnPage";
        value = pagePath;
        break;
      case "hierarchy":
        destination = "/admin/visual-hierarchy-editor";
        parameter = "webPage";
        value = pagePath;
        break;
      case "instructions":
        destination = "/site-instructions";
        break;
      case "siteAnalytics":
        destination = "/admin/visual-analytics-dashboard";
        break;
      case "users":
        destination = "/admin/users";
        break;
      case "announcement":
        destination = "/admin/site-header-properties";
        break;
      case "siteCss":
        destination = "/admin/css-editor";
        parameter = "returnPage";
        value = pagePath;
        break;
      case "setup":
        destination = "/admin";
        break;
      case "account":
        destination = "/account";
        break;
      case "logout":
        destination = "/logout";
        break;
      default:
        destination = override;
        break;
    }
    if (StringUtils.isNotBlank(override)) {
      destination = override;
    }
    if (parameter != null) {
      if (StringUtils.isBlank(value)) {
        return null;
      }
      destination = ResolveHeaderLinkCommand.bindParameter(destination, parameter, value);
    }
    return destination;
  }
}