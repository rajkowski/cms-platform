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
package com.zeroio.platform.presentation.widgets.cms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;

import com.simisinc.platform.application.cms.ContentHtmlCommand;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.application.cms.LoadContentCommand;
import com.simisinc.platform.application.cms.LoadWebPageCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.presentation.controller.Column;
import com.simisinc.platform.presentation.controller.Page;
import com.simisinc.platform.presentation.controller.Section;
import com.simisinc.platform.presentation.controller.Widget;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Displays a dynamically generated Table of Contents from the page HTML headings
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class PageTableOfContentsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  protected static Log LOG = LogFactory.getLog(PageTableOfContentsWidget.class);

  static String JSP = "/cms/page-table-of-contents.jsp";

  public static class HeadingInfo {
    private int level;
    private String id;
    private String text;

    public HeadingInfo(int level, String id, String text) {
      this.level = level;
      this.id = id;
      this.text = text;
    }

    public int getLevel() {
      return level;
    }

    public String getId() {
      return id;
    }

    public String getText() {
      return text;
    }
  }

  public WidgetContext execute(WidgetContext context) {

    // Determine the page content to base the contents on
    StringBuilder pageHtmlBuilder = new StringBuilder();

    // If there's a preference for the page type and content, use it
    String itemUniqueId = context.getPreferences().get("itemUniqueId");
    if (!StringUtils.isBlank(itemUniqueId)) {
      Item item = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(itemUniqueId,
          context.getUserSession().getUserId());
      if (item != null) {
        String htmlContent = item.getDescription();
        if (StringUtils.isNotBlank(htmlContent)) {
          pageHtmlBuilder.append(htmlContent);
        }
      }
      if (pageHtmlBuilder.isEmpty()) {
        LOG.debug("No HTML content found in item with uniqueId: " + itemUniqueId);
        return null;
      }
    } else {
      // See if there is a web page associated with the current request path and use its content
      WebPage webPage = LoadWebPageCommand.loadByLink(context.getRequest().getPagePath());
      if (webPage == null) {
        LOG.debug("Page not found");
        return null;
      }
      // Retrieve all of the page HTML by processing the page sections in webPage.getPageXml()
      if (StringUtils.isNotBlank(webPage.getPageXml())) {
        extractContentHtmlFromPageXml(webPage, pageHtmlBuilder);
      }
      LOG.debug("Collected page HTML length: " + pageHtmlBuilder.length());
    }

    // Get widget preferences
    String title = context.getPreferences().getOrDefault("title", "Table of Contents");
    int minHeadingLevel = Integer.parseInt(context.getPreferences().getOrDefault("minHeadingLevel", "1"));
    int maxHeadingLevel = Integer.parseInt(context.getPreferences().getOrDefault("maxHeadingLevel", "3"));
    boolean showToTop = "true".equals(context.getPreferences().getOrDefault("showToTop", "true"));
    boolean showIfEmpty = "true".equals(context.getPreferences().getOrDefault("showIfEmpty", "false"));

    // Extract headings from the collected HTML
    List<HeadingInfo> headings = extractHeadings(pageHtmlBuilder.toString(), minHeadingLevel, maxHeadingLevel);

    LOG.debug("Found " + headings.size() + " headings between levels " + minHeadingLevel + " and " + maxHeadingLevel);

    // Check if we should hide when empty or just 1 entry
    if ((headings.isEmpty() || headings.size() == 1) && !showIfEmpty) {
      LOG.debug("No headings found and showIfEmpty is false");
      return null;
    }

    LOG.debug("Rendering Table of Contents with " + headings.size() + " headings");

    // Set attributes for JSP
    context.getRequest().setAttribute("title", title);
    context.getRequest().setAttribute("headings", headings);
    context.getRequest().setAttribute("minHeadingLevel", String.valueOf(minHeadingLevel));
    context.getRequest().setAttribute("maxHeadingLevel", String.valueOf(maxHeadingLevel));
    context.getRequest().setAttribute("showToTop", String.valueOf(showToTop));

    context.setJsp(JSP);
    return context;
  }

  /**
   * Extracts HTML content from content widgets in the page XML
   *
   * @param pageXml The page XML string
   * @param htmlBuilder StringBuilder to append HTML to
   */
  private void extractContentHtmlFromPageXml(WebPage webPage, StringBuilder htmlBuilder) {

    Page page = WebPageXmlLayoutCommand.retrievePageForRequest(webPage, webPage.getLink());
    if (page == null) {
      LOG.debug("Page not found for webPage: " + webPage.getLink());
      return;
    }

    try {
      for (Section section : page.getSections()) {
        for (Column column : section.getColumns()) {
          for (Widget widget : column.getWidgets()) {

            String widgetName = widget.getWidgetName();
            Map<String, String> prefs = widget.getPreferences();

            if (widgetName != null && widgetName.startsWith("content")) {
              String uniqueId = prefs.get("uniqueId");
              if (StringUtils.isBlank(uniqueId)) {
                uniqueId = prefs.get("contentUniqueId");
              }
              if (StringUtils.isNotBlank(uniqueId)) {
                Content content = LoadContentCommand.loadContentByUniqueId(uniqueId);
                // This content may have embedded content that needs to be processed
                String htmlContent = content != null ? content.getContent() : null;

                // Check the HTML for content unique id values and replace with the actual content
                htmlContent = ContentHtmlCommand.replaceContentUniqueIds(htmlContent);

                if (content != null && StringUtils.isNotBlank(htmlContent)) {
                  htmlBuilder.append(htmlContent).append("\n");
                }
              }
            } else if (widgetName != null && widgetName.equals("itemsList") && prefs.containsKey("title")) {
              // Also include widget titles in the TOC
              String title = prefs.get("title");
              if (StringUtils.isNotBlank(title)) {
                htmlBuilder.append("<h2>").append(HtmlCommand.toHtml(title)).append("</h2>\n");
              }
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Error extracting content from page", e);
    }
  }

  /**
   * Extracts heading information from HTML content
   *
   * @param html The HTML content to parse
   * @param minLevel Minimum heading level to include
   * @param maxLevel Maximum heading level to include
   * @return List of heading information
   */
  private List<HeadingInfo> extractHeadings(String html, int minLevel, int maxLevel) {
    List<HeadingInfo> headings = new ArrayList<>();

    if (StringUtils.isBlank(html)) {
      LOG.debug("No HTML content to parse for headings");
      return headings;
    }

    try {
      // Parse the HTML
      org.jsoup.nodes.Document doc = Jsoup.parse(html);

      // Track used IDs to ensure uniqueness
      Set<String> usedIds = new HashSet<>();

      // Select heading elements in document order so the TOC matches the rendered page.
      StringBuilder selector = new StringBuilder();
      for (int level = minLevel; level <= maxLevel; level++) {
        if (selector.length() > 0) {
          selector.append(", ");
        }
        selector.append("h").append(level);
      }

      for (org.jsoup.nodes.Element element : doc.select(selector.toString())) {
        String text = element.text();
        if (StringUtils.isBlank(text)) {
          continue;
        }

        String tagName = element.tagName();
        int level = Integer.parseInt(tagName.substring(1));

        // Get or generate ID
        String id = element.attr("id");
        if (StringUtils.isBlank(id)) {
          id = generateId(text, usedIds);
        } else {
          usedIds.add(id);
        }

        headings.add(new HeadingInfo(level, id, text));
      }

    } catch (Exception e) {
      LOG.error("Error extracting headings from HTML", e);
    }

    return headings;
  }

  /**
   * Generates a unique ID for a heading
   *
   * @param text The heading text
   * @param usedIds Set of already used IDs
   * @return A unique ID
   */
  private String generateId(String text, Set<String> usedIds) {
    String baseId = HtmlCommand.makeId(text);

    if (StringUtils.isBlank(baseId)) {
      baseId = "heading";
    }

    String id = baseId;
    int counter = 1;

    while (usedIds.contains(id)) {
      id = baseId + "-" + counter;
      counter++;
    }

    usedIds.add(id);
    return id;
  }
}
