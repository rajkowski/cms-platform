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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Iterator;

/**
 * Commands for working with web page XML
 *
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */
public class WebPageXmlCommand {

  public static String escapeXml(String text) {
    if (StringUtils.isBlank(text)) {
      return "";
    }
    return StringEscapeUtils.escapeXml11(text);
  }

  public static String widgetNodeToXml(JsonNode widgetNode, String indent) {
    StringBuilder xml = new StringBuilder();
    String widgetName = widgetNode.get("type").asText();
    xml.append(indent).append("<widget name=\"").append(escapeXml(widgetName)).append("\">\n");

    if (widgetNode.has("properties")) {
      JsonNode propertiesNode = widgetNode.get("properties");
      Iterator<String> fieldNames = propertiesNode.fieldNames();
      while (fieldNames.hasNext()) {
        String key = fieldNames.next();
        JsonNode valueNode = propertiesNode.get(key);

        if (valueNode.isArray()) {
          // Handle array properties (like links)
          xml.append(indent).append("  <").append(key).append(">\n");
          for (JsonNode item : valueNode) {
            if (item.isObject()) {
              xml.append(indent).append("    <link");
              Iterator<String> attributeNames = item.fieldNames();
              while(attributeNames.hasNext()) {
                String attributeName = attributeNames.next();
                xml.append(" ").append(attributeName).append("=\"").append(escapeXml(item.get(attributeName).asText())).append("\"");
              }
              xml.append(" />\n");
            }
          }
          xml.append(indent).append("  </").append(key).append(">\n");
        } else if (valueNode.isTextual()) {
          if ("html".equals(key)) {
            xml.append(indent).append("  <").append(key).append("><![CDATA[").append(valueNode.asText()).append("]]></").append(key).append(">\n");
          } else {
            xml.append(indent).append("  <").append(key).append(">").append(escapeXml(valueNode.asText())).append("</").append(key).append(">\n");
          }
        } else if (valueNode.isBoolean()) {
          xml.append(indent).append("  <").append(key).append(">").append(valueNode.asBoolean()).append("</").append(key).append(">\n");
        } else if (valueNode.isNumber()) {
          xml.append(indent).append("  <").append(key).append(">").append(valueNode.asDouble()).append("</").append(key).append(">\n");
        }
      }
    }

    xml.append(indent).append("</widget>\n");
    return xml.toString();
  }
}
