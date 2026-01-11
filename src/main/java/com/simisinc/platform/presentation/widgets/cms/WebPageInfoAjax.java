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
import com.simisinc.platform.domain.model.cms.SitemapChangeFrequencyOptions;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Returns a web page's metadata/info for the visual page editor Info tab
 * Only returns pages the user has permission to edit
 *
 * @author matt rajkowski
 * @created 1/10/26 10:00 AM
 */
public class WebPageInfoAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(WebPageInfoAjax.class);

  public WidgetContext execute(WidgetContext context) {

    LOG.debug("WebPageInfoAjax...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-editor")) {
      LOG.debug("No permission to access web page info");
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

    // Retrieve the web page
    WebPage page = WebPageRepository.findByLink(link);
    if (page == null) {
      LOG.debug("Web page not found for link: " + link);
      context.setJson("{\"error\":\"Page not found\"}");
      return context;
    }

    // Build JSON response with page info
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"id\":").append(page.getId()).append(",");
    sb.append("\"link\":\"").append(JsonCommand.toJson(page.getLink())).append("\",");
    sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(page.getTitle()))).append("\",");
    sb.append("\"keywords\":\"").append(JsonCommand.toJson(StringUtils.defaultString(page.getKeywords()))).append("\",");
    sb.append("\"description\":\"").append(JsonCommand.toJson(StringUtils.defaultString(page.getDescription()))).append("\",");
    sb.append("\"imageUrl\":\"").append(JsonCommand.toJson(StringUtils.defaultString(page.getImageUrl()))).append("\",");
    sb.append("\"draft\":").append(page.getDraft()).append(",");
    sb.append("\"searchable\":").append(page.isSearchable()).append(",");
    sb.append("\"showInSitemap\":").append(page.isShowInSitemap()).append(",");
    sb.append("\"sitemapPriority\":").append(page.getSitemapPriority() != null ? page.getSitemapPriority().toString() : "0.5").append(",");
    sb.append("\"sitemapChangeFrequency\":\"").append(JsonCommand.toJson(StringUtils.defaultString(page.getSitemapChangeFrequency()))).append("\",");
    
    // Include sitemap change frequency options for the dropdown
    sb.append("\"sitemapChangeFrequencyOptions\":{");
    boolean first = true;
    for (Map.Entry<String, String> entry : SitemapChangeFrequencyOptions.map.entrySet()) {
      if (!first) {
        sb.append(",");
      }
      sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
      first = false;
    }
    sb.append("}");
    
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
