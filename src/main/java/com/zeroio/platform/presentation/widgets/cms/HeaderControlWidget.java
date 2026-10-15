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

import java.util.List;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.FontCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.cms.WorkspaceResolutionCommand;
import com.zeroio.platform.application.login.WorkspaceAccessCommand;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.domain.model.tenant.WorkspaceContext;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

public class HeaderControlWidget extends GenericWidget {

  private static final long serialVersionUID = 1L;

  public static final String MOBILE_MENU = "mobileMenu";
  public static final String APPEARANCE = "appearance";
  public static final String REGION = "region";
  public static final String ASK_AI = "askAI";
  public static final String WORKSPACES = "workspaces";

  private static final String JSP = "/cms/header-control.jsp";

  public static boolean supportsType(String type) {
    return REGION.equals(type) || APPEARANCE.equals(type) || ASK_AI.equals(type) || MOBILE_MENU.equals(type) || WORKSPACES.equals(type);
  }

  @Override
  public WidgetContext execute(WidgetContext context) {
    String type = context.getPreferences().get("type");
    String defaultLabel;
    String defaultIcon;
    if (REGION.equals(type)) {
      Object regions = context.getRequest().getAttribute(RequestConstants.REGION_LIST);
      if (!(regions instanceof List<?>) || ((List<?>) regions).isEmpty()) {
        return context;
      }
      if (!LoadSitePropertyCommand.loadByNameAsBoolean("site.regions.enabled")) {
        return context;
      }
      defaultLabel = "Region";
      defaultIcon = "fa-globe-americas";
    } else if (APPEARANCE.equals(type)) {
      defaultLabel = "Toggle Light/Dark Mode";
      defaultIcon = "fa-moon";
    } else if (ASK_AI.equals(type)) {
      if (!LoadSitePropertyCommand.loadByNameAsBoolean("ai.enabled")) {
        return context;
      }
      if (!LoadSitePropertyCommand.loadByNameAsBoolean("ai.askai.enabled")) {
        return context;
      }
      defaultLabel = LoadSitePropertyCommand.loadByName("ai.askai.text");
      defaultIcon = LoadSitePropertyCommand.loadByName("ai.askai.icon");
      String controlType = LoadSitePropertyCommand.loadByName("ai.askai.control");
      if (controlType == null || !"button".equals(controlType)) {
        controlType = "drawer";
      }
      context.getRequest().setAttribute("headerControlAskAIControlType", controlType);
      context.getRequest().setAttribute("headerControlLink", LoadSitePropertyCommand.loadByName("ai.askai.link"));
    } else if (MOBILE_MENU.equals(type)) {
      defaultLabel = "Toggle Menu";
      defaultIcon = "fa-bars";
    } else if (WORKSPACES.equals(type)) {
      if (!context.getUserSession().isLoggedIn()) {
        return context;
      }
      User sessionUser = context.getUserSession().getUser();
      WorkspaceContext workspaceContext = WorkspaceContextManager.getCurrentContext();
      long sessionUserId = context.getUserSession().getUserId();
      // Grants live in the default tenant; a workspace user is matched by email
      List<Workspace> workspaces = WorkspaceContextManager.withoutWorkspace(() -> {
        long mainUserId = sessionUserId;
        if (workspaceContext != null) {
          User mainUser = sessionUser != null && sessionUser.getEmail() != null ? UserRepository.findByEmailAddress(sessionUser.getEmail()) : null;
          mainUserId = mainUser != null ? mainUser.getId() : -1L;
        }
        return WorkspaceAccessCommand.findAuthorizedWorkspaces(mainUserId);
      });
      if (workspaces == null || workspaces.isEmpty()) {
        return context;
      }
      String defaultWorkspaceUrl = WorkspaceResolutionCommand.getDefaultWorkspaceUrl();
      String launchBase = "";
      if (workspaceContext != null) {
        launchBase = defaultWorkspaceUrl != null ? defaultWorkspaceUrl : "";
      }
      context.getRequest().setAttribute("headerControlDefaultWorkspaceUrl", defaultWorkspaceUrl);
      context.getRequest().setAttribute("headerControlWorkspaces", workspaces);
      context.getRequest().setAttribute("headerControlCurrentWorkspaceId", workspaceContext != null ? workspaceContext.workspaceId() : -1L);
      context.getRequest().setAttribute("headerControlWorkspaceLaunchBase", launchBase);
      defaultLabel = "Workspaces";
      defaultIcon = "fa-cubes";
    } else {
      throw new IllegalArgumentException("Header control type must be region, appearance, askAI, mobileMenu, or workspaces");
    }
    String icon = context.getPreferences().getOrDefault("icon", defaultIcon);
    if (!icon.contains(" ")) {
      icon = FontCommand.fas() + " " + icon;
    }
    context.getRequest().setAttribute("headerControlType", type);
    context.getRequest().setAttribute("headerControlLabel", context.getPreferences().getOrDefault("label", defaultLabel));
    context.getRequest().setAttribute("headerControlIcon", icon);
    context.setJsp(JSP);
    return context;
  }
}