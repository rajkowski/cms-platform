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
import com.zeroio.platform.infrastructure.permission.Permission;

/**
 * Visual Scheduler Editor Widget for viewing and triggering scheduled tasks
 *
 * @author matt rajkowski
 * @created 11/07/26
 */
public class VisualSchedulerEditorWidget extends GenericWidget {

  static final long serialVersionUID = -7795228162914375528L;
  protected static Log LOG = LogFactory.getLog(VisualSchedulerEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-scheduler-editor.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {

    if (!Permission.check("cms.visual-scheduler-editor.access", context.getUserSession())) {
      LOG.debug("No permission to: " + VisualSchedulerEditorWidget.class.getSimpleName());
      return context;
    }

    context.setJsp(EDITOR_JSP);
    context.getRequest().setAttribute("viewMode", "tasks");

    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    String selectedId = context.getParameter("id");
    if (StringUtils.isNotBlank(selectedId)) {
      context.getRequest().setAttribute("selectedId", selectedId);
    }

    return context;
  }
}