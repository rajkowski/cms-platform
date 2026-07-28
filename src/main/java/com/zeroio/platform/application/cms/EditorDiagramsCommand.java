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
package com.zeroio.platform.application.cms;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Provides methods to convert diagram tokens to span tags and vice versa
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class EditorDiagramsCommand {

  private static final String DIAGRAM_SPAN_CLASS = "drawio-diagram-ref";

  public static String convertDiagramTokensToSpans(String contentHtml) {
    if (contentHtml == null || contentHtml.isEmpty()) {
      return contentHtml;
    }

    // Replace diagram tokens with span tags
    // String regex = "\\$\\{diagram:([^;\\}]+)(?:;([^\\}]+))?\\}";
    // return contentHtml.replaceAll(regex, "<span class=\"drawio-diagram-ref\" contenteditable=\"false\" data-webpath=\"$1\" data-label=\"$2\">diagram: $2</span>");
    if (!contentHtml.contains("${diagram:")) {
      return contentHtml;
    }
    int tokenIdx = 0;
    while ((tokenIdx = contentHtml.indexOf("${diagram:", tokenIdx)) != -1) {
      int tokenEnd = contentHtml.indexOf("}", tokenIdx);
      if (tokenEnd == -1)
        break;
      String tokenBody = contentHtml.substring(tokenIdx + 10, tokenEnd).trim();
      if (StringUtils.isBlank(tokenBody)) {
        tokenIdx = tokenEnd + 1;
        continue;
      }
      String webPath;
      String label = "";
      int semicolonIdx = tokenBody.indexOf(";");
      if (semicolonIdx > -1) {
        webPath = tokenBody.substring(0, semicolonIdx).trim();
        label = tokenBody.substring(semicolonIdx + 1).trim();
      } else {
        webPath = tokenBody;
      }
      if (StringUtils.isBlank(webPath)) {
        tokenIdx = tokenEnd + 1;
        continue;
      }
      String displayText = StringUtils.isNotBlank(label) ? label : webPath;
      String spanTag = "<span class=\"" + DIAGRAM_SPAN_CLASS + "\" contenteditable=\"false\" " +
          "data-webpath=\"" + StringEscapeUtils.escapeHtml4(webPath) + "\" " +
          "data-label=\"" + StringEscapeUtils.escapeHtml4(label) + "\" " +
          "style=\"background-color: #fff3cd; padding: 2px 6px; border-radius: 3px; " +
          "border: 1px solid #ffecb5; display: inline-block; font-family: monospace; font-size: 0.9em;\">" +
          "diagram: " + StringEscapeUtils.escapeHtml4(displayText) + "</span>";
      contentHtml = contentHtml.substring(0, tokenIdx) + spanTag + contentHtml.substring(tokenEnd + 1);
      tokenIdx = tokenIdx + spanTag.length();
    }
    return contentHtml;
  }

  public static String convertDiagramSpansToTokens(String contentHtml) {
    if (!contentHtml.contains(DIAGRAM_SPAN_CLASS)) {
      return contentHtml;
    }
    int spanIdx = 0;
    while ((spanIdx = contentHtml.indexOf(DIAGRAM_SPAN_CLASS, spanIdx)) != -1) {
      int spanStart = contentHtml.lastIndexOf("<span", spanIdx);
      if (spanStart == -1)
        break;
      int openingTagEnd = contentHtml.indexOf(">", spanIdx);
      if (openingTagEnd == -1)
        break;
      int spanEnd = contentHtml.indexOf("</span>", openingTagEnd);
      if (spanEnd == -1)
        break;
      String openingTag = contentHtml.substring(spanStart, openingTagEnd + 1);
      String webPath = extractAttribute(openingTag, "data-webpath");
      String label = extractAttribute(openingTag, "data-label");
      if (StringUtils.isBlank(webPath)) {
        spanIdx = openingTagEnd + 1;
        continue;
      }
      String token = StringUtils.isNotBlank(label)
          ? "${diagram:" + webPath + ";" + label + "}"
          : "${diagram:" + webPath + "}";
      contentHtml = contentHtml.substring(0, spanStart) + token + contentHtml.substring(spanEnd + 7);
      spanIdx = spanStart + token.length();
    }
    return contentHtml;
  }

  private static String extractAttribute(String html, String attributeName) {
    if (StringUtils.isBlank(html) || StringUtils.isBlank(attributeName)) {
      return null;
    }
    String attributeToken = attributeName + "=\"";
    int tokenStart = html.toLowerCase().indexOf(attributeToken.toLowerCase());
    if (tokenStart == -1) {
      return null;
    }
    int valueStart = tokenStart + attributeToken.length();
    int valueEnd = html.indexOf('"', valueStart);
    if (valueEnd == -1) {
      return null;
    }
    return StringEscapeUtils.unescapeHtml4(html.substring(valueStart, valueEnd));
  }
}
