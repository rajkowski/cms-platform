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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simisinc.platform.application.cms.SaveWebPageCommand;
import com.simisinc.platform.application.cms.WebPageXmlCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    if (!context.hasRole("admin") && !context.hasRole("content-editor")) {
      LOG.debug("No permission to save web page");
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      return context;
    }

    // Determine the page link
    String webPageLink = context.getParameter("webPageLink");
    LOG.debug("Saving page: " + webPageLink);
    if (StringUtils.isBlank(webPageLink)) {
      LOG.debug("Page link is empty");
      context.setJson("{\"success\":false,\"message\":\"Page link is required\"}");
      return context;
    }

    // Retrieve the web page
    WebPage page = WebPageRepository.findByLink(webPageLink);
    if (page == null) {
      LOG.debug("Web page not found for link: " + webPageLink);
      context.setJson("{\"success\":false,\"message\":\"Page not found\"}");
      return context;
    }

    // Get the designer data (JSON format of the layout)
    String designerData = context.getParameter("designerData");
    if (StringUtils.isBlank(designerData)) {
      LOG.debug("No designer data provided");
      context.setJson("{\"success\":false,\"message\":\"Layout data is required\"}");
      return context;
    }

    // Convert the JSON layout data to XML format
    String pageXml;
    try {
      pageXml = convertDesignerJsonToXml(designerData);
    } catch (Exception e) {
      LOG.error("Error converting JSON layout to XML: " + e.getMessage(), e);
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"success\":false,");
      sb.append("\"message\":\"").append(JsonCommand.toJson("Error processing layout: " + e.getMessage())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
      return context;
    }

    try {
      // Set the user who is making the change
      if (context.getUserSession() != null) {
        page.setModifiedBy(context.getUserSession().getUserId());
      }
      page.setPageXml(pageXml);

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
    }

    return context;
  }

  /**
   * Converts JSON layout structure to XML format
   *
   * @param designerJson the JSON layout structure
   * @return XML string for page layout
   */
  private String convertDesignerJsonToXml(String designerJson) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode pageNode = mapper.readTree(designerJson);

    StringBuilder xml = new StringBuilder();
    xml.append("<page>\n");

    if (pageNode.has("rows")) {
      for (JsonNode rowNode : pageNode.get("rows")) {
        String rowCssClass = rowNode.has("cssClass") ? rowNode.get("cssClass").asText() : "";
        xml.append("  <section");
        if (StringUtils.isNotBlank(rowCssClass)) {
          xml.append(" class=\"").append(WebPageXmlCommand.escapeXml(rowCssClass)).append("\"");
        }
        if (rowNode.has("hr") && rowNode.get("hr").asBoolean()) {
          xml.append(" hr=\"true\"");
        }
        xml.append(">\n");

        if (rowNode.has("columns")) {
          for (JsonNode columnNode : rowNode.get("columns")) {
            String colCssClass = columnNode.has("cssClass") ? columnNode.get("cssClass").asText() : "";
            xml.append("    <column");
            if (StringUtils.isNotBlank(colCssClass)) {
              xml.append(" class=\"").append(WebPageXmlCommand.escapeXml(colCssClass)).append("\"");
            }
            if (columnNode.has("hr") && columnNode.get("hr").asBoolean()) {
              xml.append(" hr=\"true\"");
            }
            xml.append(">\n");

            if (columnNode.has("widgets")) {
              for (JsonNode widgetNode : columnNode.get("widgets")) {
                xml.append(WebPageXmlCommand.widgetNodeToXml(widgetNode, "      "));
              }
            }
            xml.append("    </column>\n");
          }
        }
        xml.append("  </section>\n");
      }
    }
    xml.append("</page>");

    return xml.toString();
  }
}
