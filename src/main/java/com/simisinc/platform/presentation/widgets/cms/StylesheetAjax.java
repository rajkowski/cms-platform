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

package com.simisinc.platform.presentation.widgets.cms;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Stylesheet;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.StylesheetRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns a web page's stylesheet/CSS for the visual page editor CSS tab
 * Only returns stylesheets for pages the user has permission to edit
 *
 * @author matt rajkowski
 * @created 1/10/26 10:00 AM
 */
public class StylesheetAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908896L;
  private static Log LOG = LogFactory.getLog(StylesheetAjax.class);

  public WidgetContext execute(WidgetContext context) {

    LOG.debug("StylesheetAjax...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      LOG.debug("No permission to access stylesheet");
      context.setJson("{\"error\":\"Permission denied\"}");
      return context;
    }

    // Determine the page link
    String link = context.getParameter("link");
    LOG.debug("Requested link: " + link);
    if (StringUtils.isBlank(link)) {
      LOG.debug("Link is empty");
      context.setJson("{\"error\":\"Link is required\"}");
      return context;
    }

    // Retrieve the web page to get its ID
    WebPage page = WebPageRepository.findByLink(link);
    if (page == null) {
      LOG.debug("Web page not found for link: " + link);
      context.setJson("{\"error\":\"Page not found\"}");
      return context;
    }

    // Retrieve the stylesheet for this web page
    Stylesheet stylesheet = StylesheetRepository.findByWebPageId(page.getId());

    // Build JSON response with stylesheet info
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"webPageId\":").append(page.getId()).append(",");
    sb.append("\"link\":\"").append(JsonCommand.toJson(page.getLink())).append("\",");
    
    if (stylesheet != null) {
      sb.append("\"id\":").append(stylesheet.getId()).append(",");
      sb.append("\"css\":\"").append(JsonCommand.toJson(StringUtils.defaultString(stylesheet.getCss()))).append("\",");
      sb.append("\"hasStylesheet\":true");
    } else {
      sb.append("\"id\":-1,");
      sb.append("\"css\":\"\",");
      sb.append("\"hasStylesheet\":false");
    }
    
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
