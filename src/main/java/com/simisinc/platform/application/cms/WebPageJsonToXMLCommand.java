/*
 * Copyright 2025-2026 Matt Rajkowski (https://github.com/rajkowski)
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
package com.simisinc.platform.application.cms;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebPageJsonToXMLCommand {

  private static final String ROLE_PRESENT = "rolePresent";

  /**
   * Converts JSON layout structure to XML format
   *
   * @param designerJson the JSON layout structure
   * @return XML string for page layout
   */
  public static String convertDesignerJsonToXml(String designerJson) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode pageNode = mapper.readTree(designerJson);

    StringBuilder xml = new StringBuilder();
    xml.append("<page");
    if (pageNode.has("role") && pageNode.get("role").isArray()) {
      StringBuilder roles = new StringBuilder();
      for (JsonNode roleNode : pageNode.get("role")) {
        if (roleNode.isTextual() && StringUtils.isNotBlank(roleNode.asText())) {
          if (roles.length() > 0) {
            roles.append(",");
          }
          roles.append(roleNode.asText());
        }
      }
      if (roles.length() > 0 || pageNode.path(ROLE_PRESENT).asBoolean()) {
        xml.append(" role=\"").append(WebPageXmlCommand.escapeXml(roles.toString())).append("\"");
      }
    }
    xml.append(">\n");

    if (pageNode.has("rows")) {
      for (JsonNode rowNode : pageNode.get("rows")) {
        String rowCssClass = rowNode.has("cssClass") ? rowNode.get("cssClass").asText() : "";
        xml.append("  <section");
        if (StringUtils.isNotBlank(rowCssClass)) {
          xml.append(" class=\"").append(WebPageXmlCommand.escapeXml(rowCssClass)).append("\"");
        }
        if (rowNode.has("role") && rowNode.get("role").isArray()) {
          StringBuilder roles = new StringBuilder();
          for (JsonNode roleNode : rowNode.get("role")) {
            if (roleNode.isTextual() && StringUtils.isNotBlank(roleNode.asText())) {
              if (roles.length() > 0) {
                roles.append(",");
              }
              roles.append(roleNode.asText());
            }
          }
          if (roles.length() > 0 || rowNode.path(ROLE_PRESENT).asBoolean()) {
            xml.append(" role=\"").append(WebPageXmlCommand.escapeXml(roles.toString())).append("\"");
          }
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
            if (columnNode.has("role") && columnNode.get("role").isArray()) {
              StringBuilder roles = new StringBuilder();
              for (JsonNode roleNode : columnNode.get("role")) {
                if (roleNode.isTextual() && StringUtils.isNotBlank(roleNode.asText())) {
                  if (roles.length() > 0) {
                    roles.append(",");
                  }
                  roles.append(roleNode.asText());
                }
              }
              if (roles.length() > 0 || columnNode.path(ROLE_PRESENT).asBoolean()) {
                xml.append(" role=\"").append(WebPageXmlCommand.escapeXml(roles.toString())).append("\"");
              }
            }
            if (columnNode.has("hr") && columnNode.get("hr").asBoolean()) {
              xml.append(" hr=\"true\"");
            }
            if (columnNode.has("sticky") && columnNode.get("sticky").asBoolean()) {
              xml.append(" sticky=\"true\"");
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
