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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.WordUtils;

import com.simisinc.platform.application.cms.SaveWebPageCommand;
import com.simisinc.platform.application.cms.WebPageJsonToXMLCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Saves a web page's layout from the visual page editor
 * Only allows pages the user has permission to edit
 *
 * @author matt rajkowski
 * @created 12/21/25 10:00 AM
 */
public class SaveWebPageAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(SaveWebPageAjax.class);

  public WidgetContext post(WidgetContext context) {

    LOG.debug("SaveWebPageAjax...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      LOG.debug("No permission to save web page");
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Determine the page link
    String webPageLink = context.getParameter("webPageLink");
    LOG.debug("Saving page: " + webPageLink);
    if (StringUtils.isBlank(webPageLink)) {
      LOG.debug("Page link is empty");
      context.setJson("{\"success\":false,\"message\":\"Page link is required\"}");
      context.setSuccess(false);
      return context;
    }

    // Retrieve the web page
    WebPage page = WebPageRepository.findByLink(webPageLink);
    if (page == null) {
      // This is a new page
      LOG.debug("Creating new page for link: " + webPageLink);
      page = new WebPage();
      page.setLink(webPageLink);
      page.setCreatedBy(context.getUserId());
      // Make a page title
      String title = webPageLink.replace("-", " ");
      title = StringUtils.substringAfterLast(title, "/");
      title = WordUtils.capitalizeFully(title, ' ');
      page.setTitle(title);
    }
    page.setModifiedBy(context.getUserId());

    // Get the designer data (JSON format of the layout)
    String designerData = context.getParameter("designerData");
    if (StringUtils.isBlank(designerData)) {
      LOG.debug("No designer data provided");
      context.setJson("{\"success\":false,\"message\":\"Layout data is required\"}");
      context.setSuccess(false);
      return context;
    }

    // Convert the JSON layout data to XML format
    String pageXml;
    try {
      pageXml = WebPageJsonToXMLCommand.convertDesignerJsonToXml(designerData);
    } catch (Exception e) {
      LOG.error("Error converting JSON layout to XML: " + e.getMessage(), e);
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"success\":false,");
      sb.append("\"message\":\"").append(JsonCommand.toJson("Error processing layout: " + e.getMessage())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
      context.setSuccess(false);
      return context;
    }
    page.setPageXml(pageXml);

    try {
      // Save using the SaveWebPageCommand
      WebPage savedPage = SaveWebPageCommand.saveWebPage(page);

      if (savedPage != null) {
        // Build success response
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"message\":\"Page saved successfully\",");
        sb.append("\"id\":").append(savedPage.getId()).append(",");
        sb.append("\"link\":\"").append(JsonCommand.toJson(savedPage.getLink())).append("\"");
        sb.append("}");
        context.setJson(sb.toString());
        LOG.debug("Page saved successfully: " + webPageLink);
      } else {
        context.setJson("{\"success\":false,\"message\":\"Failed to save page\"}");
        context.setSuccess(false);
        LOG.debug("Save returned null for page: " + webPageLink);
      }
    } catch (Exception e) {
      LOG.error("Error saving page: " + e.getMessage(), e);
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"success\":false,");
      sb.append("\"message\":\"").append(JsonCommand.toJson(e.getMessage())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
      context.setSuccess(false);
    }

    return context;
  }

}
