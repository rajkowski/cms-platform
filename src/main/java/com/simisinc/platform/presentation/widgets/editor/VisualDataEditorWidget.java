/*
 * Copyright 2025 Matt Rajkowski (https://github.com/rajkowski)
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
 * Visual Editor Widget for data management with collections and datasets
 *
 * @author matt rajkowski
 * @created 01/25/26 4:50 PM
 */
public class VisualDataEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;
  protected static Log LOG = LogFactory.getLog(VisualDataEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-data-editor.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    // Specify the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    // Determine the view mode (collections or datasets)
    String viewMode = context.getParameter("view");
    if (StringUtils.isBlank(viewMode)) {
      viewMode = "collections";
    }
    context.getRequest().setAttribute("viewMode", viewMode);

    // The POST was triggered
    if (context.getRequestObject() != null) {
      context.getRequest().setAttribute("requestObject", context.getRequestObject());
      return context;
    }

    // Determine if editing an existing collection or dataset
    String uniqueId = context.getParameter("uniqueId");
    if (StringUtils.isNotBlank(uniqueId)) {
      LOG.debug("Loading data object for uniqueId: " + uniqueId);
      context.getRequest().setAttribute("uniqueId", uniqueId);
    }

    return context;
  }
}
