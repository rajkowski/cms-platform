/*
 * Copyright 2025 Matt Rajkowski (https://github.com/rajkowski)
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

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Commands for working with web page XML
 *
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */
public class WebPageXmlCommand {

  private static Log LOG = LogFactory.getLog(WebPageXmlCommand.class);

  public static String escapeXml(String text) {
    if (StringUtils.isBlank(text)) {
      return "";
    }
    return StringEscapeUtils.escapeXml11(text);
  }

  public static String widgetNodeToXml(JsonNode widgetNode, String indent) {
    StringBuilder xml = new StringBuilder();
    String widgetName = widgetNode.get("type").asText();
    xml.append(indent).append("<widget name=\"").append(escapeXml(widgetName)).append("\"");

    // Add class attribute if present
    if (widgetNode.has("cssClass")) {
      String cssClass = widgetNode.get("cssClass").asText();
      if (StringUtils.isNotBlank(cssClass)) {
        xml.append(" class=\"").append(escapeXml(cssClass)).append("\"");
      }
    }

    // Add hr attribute if present
    if (widgetNode.has("hr") && widgetNode.get("hr").asBoolean()) {
      xml.append(" hr=\"true\"");
    }

    // Add sticky attribute if present
    if (widgetNode.has("sticky") && widgetNode.get("sticky").asBoolean()) {
      xml.append(" sticky=\"true\"");
    }

    xml.append(">\n");

    if (widgetNode.has("properties")) {
      JsonNode propertiesNode = widgetNode.get("properties");
      Iterator<String> fieldNames = propertiesNode.fieldNames();
      while (fieldNames.hasNext()) {
        String key = fieldNames.next();
        JsonNode valueNode = propertiesNode.get(key);

        if (valueNode.isArray()) {
          LOG.debug("Processing array item for key: " + key);
          // Handle array properties (like links)
          xml.append(indent).append("  <").append(key).append(">\n");
          for (JsonNode item : valueNode) {
            if (item.isObject()) {

              // @todo pass in schema to determine item name

              // Generic based on schema
              String itemName = "undefined";
              if (key.equals("links")) {
                itemName = "link";
              } else if (key.equals("fields")) {
                itemName = "field";
              } else if (key.equals("tabs")) {
                itemName = "tab";
              } else if (key.endsWith("s")) {
                itemName = key.substring(0, key.length() - 1);
              }
              xml.append(indent).append("    <").append(itemName);

              Iterator<String> attributeNames = item.fieldNames();
              while (attributeNames.hasNext()) {
                String attributeName = attributeNames.next();
                xml.append(" ").append(attributeName).append("=\"").append(escapeXml(item.get(attributeName).asText())).append("\"");
              }
              xml.append(" />\n");
            }
          }
          xml.append(indent).append("  </").append(key).append(">\n");

        } else if (valueNode.isTextual()) {

          if ("html".equals(key)) {
            LOG.debug("Processing HTML content for key: " + key);
            xml.append(indent).append("  <").append(key).append("><![CDATA[").append(valueNode.asText()).append("]]></").append(key)
                .append(">\n");

          } else {

            String valueText = valueNode.asText();
            if (valueText.startsWith("<") && valueText.endsWith("/>") && valueText.contains("=\"")) {
              LOG.debug("Processing self-closing tag content for key: " + key);
              xml.append(indent).append("  <").append(key).append(">")
                  .append(valueText)
                  .append("</").append(key).append(">\n");
            } else {
              LOG.debug("Processing simple text content for key: " + key);
              xml.append(indent).append("  <").append(key).append(">")
                  .append(escapeXml(valueText))
                  .append("</").append(key).append(">\n");
            }
          }

        } else if (valueNode.isBoolean()) {
          LOG.debug("Processing boolean content for key: " + key);
          xml.append(indent).append("  <").append(key).append(">").append(valueNode.asBoolean()).append("</").append(key)
              .append(">\n");
        } else if (valueNode.isNumber()) {
          LOG.debug("Processing numeric content for key: " + key);
          xml.append(indent).append("  <").append(key).append(">").append(valueNode.asDouble()).append("</").append(key).append(">\n");
        }

      }
    }

    xml.append(indent).append("</widget>\n");
    if (LOG.isDebugEnabled()) {
      LOG.debug("Generated XML for widget " + widgetName + ":\n" + xml.toString());
    }
    return xml.toString();
  }
}
