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

package com.simisinc.platform.application.cms;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.presentation.controller.Column;
import com.simisinc.platform.presentation.controller.Page;
import com.simisinc.platform.presentation.controller.Section;
import com.simisinc.platform.presentation.controller.Widget;

/**
 * Generates compiled text from web page content for full-text search indexing.
 * Extracts page titles, Content widget references, and recursively resolves embedded uniqueId directives.
 *
 * @author matt rajkowski
 * @created 6/10/26 1:00 PM
 */
public class GenerateWebPageTextCommand {

  private static Log LOG = LogFactory.getLog(GenerateWebPageTextCommand.class);

  // Widget names that use content uniqueIds
  private static final Set<String> CONTENT_WIDGET_NAMES = Set.of(
      "content", "contentTabs",
      "contentCards", "contentAccordion", "contentSlider", "contentReveal",
      "contentGallery", "contentCarousel");

  // Pattern to match ${uniqueId:example-include}
  private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("\\$\\{uniqueId:([^}]+)\\}");

  // Maximum allowed size for page_text to prevent index bloat
  private static final int MAX_PAGE_TEXT_LENGTH = 50000;

  // Default max recursion depth for embedded references
  private static final int DEFAULT_MAX_RECURSION_DEPTH = 3;

  /**
   * Generate page text from page content for indexing
   *
   * @param webPage the web page to index
   * @param maxRecursionDepth max depth for resolving embedded uniqueIds (default 3)
   * @return compiled plain text suitable for full-text search, or null if no content
   */
  public static String generatePageText(WebPage webPage, int maxRecursionDepth) {
    if (webPage == null || StringUtils.isBlank(webPage.getPageXml())) {
      return null;
    }

    StringBuilder compiledText = new StringBuilder();
    Set<String> seenUniqueIds = new HashSet<>();

    try {
      // Parse page layout and extract content
      Page page = WebPageXmlLayoutCommand.retrievePageForRequest(webPage, webPage.getLink());
      if (page != null) {
        extractContentFromPage(page, compiledText, seenUniqueIds, maxRecursionDepth, 0);
      }

      // Normalize and return
      String result = compiledText.toString().trim();
      if (StringUtils.isBlank(result)) {
        return null;
      }

      // Apply size guardrail
      if (result.length() > MAX_PAGE_TEXT_LENGTH) {
        result = result.substring(0, MAX_PAGE_TEXT_LENGTH);
      }

      return result;

    } catch (Exception e) {
      LOG.warn("Error generating page text for page: " + webPage.getId(), e);
      return null;
    }
  }

  /**
   * Generate page text using default recursion depth
   */
  public static String generatePageText(WebPage webPage) {
    return generatePageText(webPage, DEFAULT_MAX_RECURSION_DEPTH);
  }

  /**
   * Extract content from page layout by walking sections/columns/widgets
   */
  private static void extractContentFromPage(Page page, StringBuilder compiledText,
      Set<String> seenUniqueIds, int maxRecursionDepth, int currentDepth) {

    try {
      for (Section section : page.getSections()) {
        for (Column column : section.getColumns()) {
          for (Widget widget : column.getWidgets()) {

            String widgetName = widget.getWidgetName();
            Map<String, String> prefs = widget.getPreferences();

            // Use page titles from widget preferences
            String title = prefs.get("title");
            if (StringUtils.isNotBlank(title)) {
              appendPlainText(title, compiledText);
            }

            // Extract content
            if (CONTENT_WIDGET_NAMES.contains(widgetName)) {
              String uniqueId = prefs.get("uniqueId");
              if (StringUtils.isBlank(uniqueId)) {
                uniqueId = prefs.get("contentUniqueId");
              }

              if (StringUtils.isNotBlank(uniqueId)) {
                extractContentByUniqueId(uniqueId, compiledText, seenUniqueIds, maxRecursionDepth, currentDepth);
              }

              // Also check for fallback HTML
              String fallbackHtml = prefs.get("html");
              if (StringUtils.isNotBlank(fallbackHtml)) {
                appendNormalizedText(fallbackHtml, compiledText);
                // Extract embedded directives from fallback HTML
                extractEmbeddedReferences(fallbackHtml, compiledText, seenUniqueIds, maxRecursionDepth,
                    currentDepth);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Error extracting content from page layout", e);
    }
  }

  /**
   * Resolve and extract text from a content record by uniqueId
   */
  private static void extractContentByUniqueId(String uniqueId, StringBuilder compiledText,
      Set<String> seenUniqueIds, int maxRecursionDepth, int currentDepth) {

    if (StringUtils.isBlank(uniqueId) || seenUniqueIds.contains(uniqueId)) {
      return; // Cycle protection
    }

    // Check recursion depth
    if (currentDepth >= maxRecursionDepth) {
      return;
    }

    seenUniqueIds.add(uniqueId);
    Content content = LoadContentCommand.loadContentByUniqueId(uniqueId);

    if (content != null) {
      // Use published content only (not draft)
      String contentHtml = content.getContent();
      if (StringUtils.isNotBlank(contentHtml)) {
        appendNormalizedText(contentHtml, compiledText);
        // Check for embedded references in the content HTML
        extractEmbeddedReferences(contentHtml, compiledText, seenUniqueIds, maxRecursionDepth,
            currentDepth + 1);
      }
    }
  }

  /**
   * Extract ${uniqueId:...} embedded references and recursively resolve them
   */
  private static void extractEmbeddedReferences(String html, StringBuilder compiledText,
      Set<String> seenUniqueIds, int maxRecursionDepth, int currentDepth) {

    if (StringUtils.isBlank(html)) {
      return;
    }

    Matcher matcher = DIRECTIVE_PATTERN.matcher(html);
    while (matcher.find()) {
      String embeddedId = matcher.group(1).trim();
      if (StringUtils.isNotBlank(embeddedId)) {
        extractContentByUniqueId(embeddedId, compiledText, seenUniqueIds, maxRecursionDepth,
            currentDepth);
      }
    }
  }

  /**
   * Append plain text to the compiled result
   */
  private static void appendPlainText(String plainText, StringBuilder compiledText) {
    if (StringUtils.isBlank(plainText)) {
      return;
    }

    if (!compiledText.isEmpty() && !compiledText.toString().endsWith(" ")) {
      compiledText.append(" ");
    }
    compiledText.append(plainText);
  }

  /**
   * Append HTML-normalized plain text to the compiled result
   */
  private static void appendNormalizedText(String html, StringBuilder compiledText) {
    if (StringUtils.isBlank(html)) {
      return;
    }

    String plainText = HtmlCommand.text(html);
    if (StringUtils.isNotBlank(plainText)) {
      appendPlainText(plainText, compiledText);
    }
  }

}
