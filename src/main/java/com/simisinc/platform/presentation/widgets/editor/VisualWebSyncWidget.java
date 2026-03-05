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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Visual Web Sync widget for managing static site generation and Git publishing
 *
 * @author matt rajkowski
 * @created 2/25/26 9:00 AM
 */
public class VisualWebSyncWidget extends GenericWidget {

  static final long serialVersionUID = -3948271637912347651L;
  protected static Log LOG = LogFactory.getLog(VisualWebSyncWidget.class);

  static String EDITOR_JSP = "/cms/visual-web-sync.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Only allow admins
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to: " + VisualWebSyncWidget.class.getSimpleName());
      return context;
    }

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    return context;
  }
}
