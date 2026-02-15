/*
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.presentation.widgets.cms;

import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 12/17/18 4:14 PM
 */
public class FileBrowserWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  private static String ENHANCED_JSP = "/cms/enhanced-file-browser.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    if ("reveal".equals(context.getRequest().getParameter("view"))) {
      context.setEmbedded(true);
    }

    String inputId = context.getRequest().getParameter("inputId");
    context.getRequest().setAttribute("inputId", inputId);

    // Show the editor
    context.setEmbedded(true);
    context.setJsp(ENHANCED_JSP);
    return context;
  }
}
