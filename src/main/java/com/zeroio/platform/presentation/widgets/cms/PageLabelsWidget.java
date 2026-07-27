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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.LoadWebPageCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Displays labels/tags for the current webpage
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class PageLabelsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;

  protected static final Log LOG = LogFactory.getLog(PageLabelsWidget.class);

  static String JSP = "/cms/page-labels.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Load the current web page
    WebPage webPage = LoadWebPageCommand.loadByLink(context.getRequest().getPagePath());

    if (webPage == null || webPage.getTags() == null || webPage.getTags().length == 0) {
      return null;
    }

    // Set preferences
    context.getRequest().setAttribute("searchLink", context.getPreferences().getOrDefault("searchLink", "/search"));

    // Set the page information
    context.getRequest().setAttribute("webPage", webPage);

    context.setJsp(JSP);
    return context;
  }
}
