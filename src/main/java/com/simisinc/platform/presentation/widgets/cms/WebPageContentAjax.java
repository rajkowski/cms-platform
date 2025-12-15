/*
 * Copyright 2025 Matt Rajkowski
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
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns a web page's content for the visual page editor
 * Only returns pages the user has permission to edit
 *
 * @author matt rajkowski
 * @created 12/14/25 10:00 AM
 */
public class WebPageContentAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(WebPageContentAjax.class);

  public WidgetContext execute(WidgetContext context) {

    LOG.debug("WebPageContentAjax...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-editor")) {
      LOG.debug("No permission to access web page content");
      context.setJson("{}");
      return context;
    }

    // Determine the page link
    String link = context.getParameter("link");
    LOG.debug("Requested link: " + link);
    if (StringUtils.isBlank(link)) {
      LOG.debug("Link is empty");
      context.setJson("{}");
      return context;
    }

    // Retrieve the web page
    WebPage page = WebPageRepository.findByLink(link);
    if (page == null) {
      LOG.debug("Web page not found for link: " + link);
      context.setJson("{}");
      return context;
    }

    // Build JSON response with page content
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"id\":").append(page.getId()).append(",");
    sb.append("\"link\":\"").append(JsonCommand.toJson(page.getLink())).append("\",");
    String pageTitle = StringUtils.isNotBlank(page.getTitle()) ? page.getTitle() : page.getLink();
    sb.append("\"title\":\"").append(JsonCommand.toJson(pageTitle)).append("\",");
    
    // Include the page XML
    String pageXml = page.getPageXml();
    if (StringUtils.isNotBlank(pageXml)) {
      sb.append("\"pageXml\":\"").append(JsonCommand.toJson(pageXml)).append("\"");
    } else {
      sb.append("\"pageXml\":\"\"");
    }
    
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
