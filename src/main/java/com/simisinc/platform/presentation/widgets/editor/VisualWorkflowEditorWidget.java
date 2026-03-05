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

package com.simisinc.platform.presentation.widgets.editor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Visual Workflow Editor Widget for viewing scheduled tasks and event workflows
 *
 * @author matt rajkowski
 * @created 02/27/26 9:00 AM
 */
public class VisualWorkflowEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908899L;
  protected static Log LOG = LogFactory.getLog(VisualWorkflowEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-workflow-editor.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {

    // Check permissions
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to access workflow editor");
      return context;
    }

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    // Specify the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    // Determine the view mode (tasks or events)
    String viewMode = context.getParameter("view");
    if (StringUtils.isBlank(viewMode)) {
      viewMode = "tasks";
    }
    context.getRequest().setAttribute("viewMode", viewMode);

    // Determine if a specific item is selected
    String selectedId = context.getParameter("id");
    if (StringUtils.isNotBlank(selectedId)) {
      context.getRequest().setAttribute("selectedId", selectedId);
    }

    return context;
  }
}
