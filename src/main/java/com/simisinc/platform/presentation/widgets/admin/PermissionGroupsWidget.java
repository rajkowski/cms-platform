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
package com.simisinc.platform.presentation.widgets.admin;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Read-only admin view of loaded Cedar permission groups and their component members.
 *
 * @author matt rajkowski
 * @created 3/6/26 8:00 AM
 */
public class PermissionGroupsWidget extends GenericWidget {

  static final long serialVersionUID = 7812340985623401287L;
  static String JSP = "/admin/permission-groups.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to: " + PermissionGroupsWidget.class.getSimpleName());
      return context;
    }
    context.getRequest().setAttribute("permissionGroupList", PermissionEngine.getAllGroups());
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));
    context.setJsp(JSP);
    return context;
  }
}
