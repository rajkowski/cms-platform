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

  // Build a normalized control configuration for each supported header control type, keeping the main method focused.
  @Override
  public WidgetContext execute(WidgetContext context) {
    String type = context.getPreferences().get("type");
    ControlConfig config = buildControlConfig(context, type);
    if (config == null) {
      return context;
    }

    String icon = context.getPreferences().getOrDefault("icon", config.icon());
    if (!icon.contains(" ")) {
      icon = FontCommand.fas() + " " + icon;
    }

    context.getRequest().setAttribute("headerControlType", type);
    context.getRequest().setAttribute("headerControlLabel", context.getPreferences().getOrDefault("label", config.label()));
    context.getRequest().setAttribute("headerControlIcon", icon);
    if (config.askAiControlType() != null) {
      context.getRequest().setAttribute("headerControlAskAIControlType", config.askAiControlType());
    }
    if (config.link() != null) {
      context.getRequest().setAttribute("headerControlLink", config.link());
    }
    if (config.workspaceData() != null) {
      context.getRequest().setAttribute("headerControlDefaultWorkspaceUrl", config.workspaceData().defaultWorkspaceUrl());
      context.getRequest().setAttribute("headerControlWorkspaces", config.workspaceData().workspaces());
      context.getRequest().setAttribute("headerControlCurrentWorkspaceId", config.workspaceData().currentWorkspaceId());
      context.getRequest().setAttribute("headerControlWorkspaceLaunchBase", config.workspaceData().launchBase());
    }
    context.setJsp(JSP);
    return context;
  }

  // Resolve the runtime configuration for the requested control while preserving each original branch's validation rules.
  private static ControlConfig buildControlConfig(WidgetContext context, String type) {
    if (REGION.equals(type)) {
      return buildRegionConfig(context);
    }
    if (APPEARANCE.equals(type)) {
      return new ControlConfig("Toggle Light/Dark Mode", "fa-moon");
    }
    if (ASK_AI.equals(type)) {
      return buildAskAiConfig(context);
    }
    if (MOBILE_MENU.equals(type)) {
      return new ControlConfig("Toggle Menu", "fa-bars");
    }
    if (WORKSPACES.equals(type)) {
      return buildWorkspaceConfig(context);
    }
    throw new IllegalArgumentException("Header control type must be region, appearance, askAI, mobileMenu, or workspaces");
  }

  // Region controls are only shown when region data is available and the site feature is enabled.
  private static ControlConfig buildRegionConfig(WidgetContext context) {
    Object regions = context.getRequest().getAttribute(RequestConstants.REGION_LIST);
    if (!(regions instanceof List<?>) || ((List<?>) regions).isEmpty()) {
      return null;
    }
    if (!LoadSitePropertyCommand.loadByNameAsBoolean("site.regions.enabled")) {
      return null;
    }
    return new ControlConfig("Region", "fa-globe-americas");
  }

  // Ask AI controls require both the feature and the specific askAI panel to be enabled.
  private static ControlConfig buildAskAiConfig(WidgetContext context) {
    if (!LoadSitePropertyCommand.loadByNameAsBoolean("ai.enabled")) {
      return null;
    }
    if (!LoadSitePropertyCommand.loadByNameAsBoolean("ai.askai.enabled")) {
      return null;
    }

    String controlType = LoadSitePropertyCommand.loadByName("ai.askai.control");
    if (controlType == null || !"button".equals(controlType)) {
      controlType = "drawer";
    }
    return new ControlConfig(
        LoadSitePropertyCommand.loadByName("ai.askai.text"),
        LoadSitePropertyCommand.loadByName("ai.askai.icon"),
        controlType,
        LoadSitePropertyCommand.loadByName("ai.askai.link"));
  }

  // Workspace controls require a logged-in user and a valid list of accessible workspaces in the current context.
  private static ControlConfig buildWorkspaceConfig(WidgetContext context) {
    if (!context.getUserSession().isLoggedIn()) {
      return null;
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
      return null;
    }

    String defaultWorkspaceUrl = WorkspaceResolutionCommand.getDefaultWorkspaceUrl();
    String launchBase = workspaceContext != null && defaultWorkspaceUrl != null ? defaultWorkspaceUrl : "";

    String currentWorkspaceName = workspaceContext == null ? "Home" : "Current Workspace";
    if (workspaceContext != null) {
      for (Workspace workspace : workspaces) {
        if (workspace.getId() == workspaceContext.workspaceId()) {
          currentWorkspaceName = workspace.getName();
          break;
        }
      }
    }
    context.getRequest().setAttribute("headerControlCurrentWorkspaceName", currentWorkspaceName);

    return new ControlConfig(
        "Workspaces",
        "fa-cubes",
        new WorkspaceData(defaultWorkspaceUrl, workspaces, workspaceContext != null ? workspaceContext.workspaceId() : -1L, launchBase));
  }

  private record ControlConfig(String label, String icon, String askAiControlType, String link, WorkspaceData workspaceData) {
    private ControlConfig(String label, String icon) {
      this(label, icon, null, null, null);
    }

    private ControlConfig(String label, String icon, String askAiControlType, String link) {
      this(label, icon, askAiControlType, link, null);
    }

    private ControlConfig(String label, String icon, WorkspaceData workspaceData) {
      this(label, icon, null, null, workspaceData);
    }
  }

  private record WorkspaceData(String defaultWorkspaceUrl, List<Workspace> workspaces, long currentWorkspaceId, String launchBase) {
  }
}