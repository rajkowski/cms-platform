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

package com.simisinc.platform.presentation.widgets.editor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Visual Hierarchy Editor Widget
 *
 * @author matt rajkowski
 * @created 2/7/26 12:00 PM
 */
public class VisualHierarchyEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(VisualHierarchyEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-hierarchy-editor.jsp";

  public WidgetContext execute(WidgetContext context) {
    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + VisualHierarchyEditorWidget.class.getSimpleName());
      return context;
    }

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    // Specify the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    // Load initial context data for the editor
    // User permissions will be checked by the JSP and JavaScript modules
    context.getRequest().setAttribute("userId", context.getUserId());

    // Theme settings (for dark mode support)
    String theme = context.getPreferences().get("theme");
    if (StringUtils.isBlank(theme)) {
      theme = "light";
    }
    context.getRequest().setAttribute("theme", theme);

    LOG.debug("Visual Hierarchy Editor initialized for user: " + context.getUserId());

    return context;
  }
}
