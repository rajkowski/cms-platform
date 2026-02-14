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

import com.simisinc.platform.application.cms.LoadContentListCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Widget to display the content browser for TinyMCE editor
 *
 * @author matt rajkowski
 * @created 2/14/26 10:00 PM
 */
public class ContentBrowserWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  private static Log LOG = LogFactory.getLog(ContentBrowserWidget.class);

  private static String JSP = "/cms/content-browser.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Load all content blocks for the browser
    List<Content> contentList = LoadContentListCommand.loadContentList(null, 0, 100, "alphabetical");
    context.getRequest().setAttribute("contentList", contentList);

    if ("reveal".equals(context.getRequest().getParameter("view"))) {
      context.setEmbedded(true);
    }

    String inputId = context.getRequest().getParameter("inputId");
    context.getRequest().setAttribute("inputId", inputId);

    // Show the editor
    context.setEmbedded(true);
    context.setJsp(JSP);
    return context;
  }
}
