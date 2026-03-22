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

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Visual CRM Editor Widget - unified interface for forms, mailing lists, customers, orders, and product catalog
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class VisualCRMEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;
  protected static Log LOG = LogFactory.getLog(VisualCRMEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-crm-editor.jsp";

  public WidgetContext execute(WidgetContext context) {
    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + VisualCRMEditorWidget.class.getSimpleName());
      return context;
    }

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    // Specify the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    // Determine the active section: forms, mailinglists, customers, orders,
    //   product-categories, products, pricing-rules, sales-tax, shipping-rates
    String section = context.getParameter("section");
    if (StringUtils.isBlank(section)) {
      section = "forms";
    }
    context.getRequest().setAttribute("section", section);

    // The POST was triggered
    if (context.getRequestObject() != null) {
      context.getRequest().setAttribute("requestObject", context.getRequestObject());
      return context;
    }

    // Determine if a specific category/id is selected
    String selectedId = context.getParameter("selectedId");
    if (StringUtils.isNotBlank(selectedId)) {
      context.getRequest().setAttribute("selectedId", selectedId);
    }

    return context;
  }
}
