/*
 * Copyright 2026 Matt Rajkowski
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.LoadUserCommand;
import com.simisinc.platform.application.cms.LoadContentCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.Column;
import com.simisinc.platform.presentation.controller.Page;
import com.simisinc.platform.presentation.controller.Section;
import com.simisinc.platform.presentation.controller.Widget;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns the content blocks referenced by a page's XML layout
 * Parses widget preferences for uniqueId values and embedded ${uniqueId:...} directives
 *
 * @author matt rajkowski
 * @created 2/14/26 10:00 AM
 */
public class PageContentBlocksJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(PageContentBlocksJsonService.class);

  // Widget names that use content uniqueIds
  private static final Set<String> CONTENT_WIDGET_NAMES = Set.of(
      "content", "contentHeadline", "contentParagraph", "contentTabs",
      "contentCards", "contentAccordion", "contentSlider", "contentReveal",
      "contentGallery", "contentCarousel");

  // Pattern to match ${uniqueId:example-include}
  private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("\\$\\{uniqueId:([^}]+)\\}");

  public WidgetContext execute(WidgetContext context) {

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("content-manager") && !context.hasRole("content-editor")) {
      return writeError(context, "Permission denied");
    }

    try {
      String link = context.getParameter("link");
      if (StringUtils.isBlank(link)) {
        return writeError(context, "Page link is required");
      }

      // Load the web page
      WebPage webPage = WebPageRepository.findByLink(link);
      if (webPage == null) {
        return writeError(context, "Page not found");
      }

      // Collect unique content IDs from the page layout
      Set<String> uniqueIds = new LinkedHashSet<>();
      extractContentUniqueIds(webPage, link, uniqueIds);

      // Load content records and build response
      List<Content> contentBlocks = new ArrayList<>();
      for (String uniqueId : uniqueIds) {
        Content content = LoadContentCommand.loadContentByUniqueId(uniqueId);
        if (content != null) {
          contentBlocks.add(content);
          // Also check for embedded ${uniqueId:...} directives (1 level deep)
          extractEmbeddedDirectives(content.getContent(), uniqueIds);
        }
      }

      // Re-load any newly discovered content from embedded directives
      List<Content> allContentBlocks = new ArrayList<>();
      for (String uniqueId : uniqueIds) {
        Content content = LoadContentCommand.loadContentByUniqueId(uniqueId);
        if (content != null) {
          allContentBlocks.add(content);
        }
      }

      // Build JSON response
      StringBuilder dataJson = new StringBuilder();
      dataJson.append("[");
      boolean first = true;
      for (Content content : allContentBlocks) {
        if (!first) {
          dataJson.append(",");
        }
        first = false;
        appendContentJson(dataJson, content);
      }
      dataJson.append("]");

      return writeOk(context, dataJson.toString(), null);

    } catch (Exception e) {
      LOG.error("Error loading page content blocks: " + e.getMessage(), e);
      return writeError(context, "Error loading content blocks");
    }
  }

  /**
   * Extract content uniqueIds from the page's XML layout by traversing the widget hierarchy
   */
  private void extractContentUniqueIds(WebPage webPage, String link, Set<String> uniqueIds) {
    try {
      // Use the page layout command to get the parsed Page object
      Page page = WebPageXmlLayoutCommand.retrievePageForRequest(webPage, link);
      if (page == null) {
        LOG.debug("No page layout found for: " + link);
        return;
      }

      // Traverse sections -> columns -> widgets
      for (Section section : page.getSections()) {
        for (Column column : section.getColumns()) {
          for (Widget widget : column.getWidgets()) {
            String widgetName = widget.getWidgetName();
            if (CONTENT_WIDGET_NAMES.contains(widgetName)) {
              Map<String, String> prefs = widget.getPreferences();
              // Check for uniqueId preference
              String uniqueId = prefs.get("uniqueId");
              if (StringUtils.isBlank(uniqueId)) {
                uniqueId = prefs.get("contentUniqueId");
              }
              if (StringUtils.isNotBlank(uniqueId)) {
                uniqueIds.add(uniqueId);
              }
              // Check for embedded directives in html preference
              String html = prefs.get("html");
              if (StringUtils.isNotBlank(html)) {
                extractEmbeddedDirectives(html, uniqueIds);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.warn("Error parsing page XML for content blocks: " + e.getMessage());
    }
  }

  /**
   * Extract uniqueId references from ${uniqueId:...} directives in content HTML
   */
  private void extractEmbeddedDirectives(String html, Set<String> uniqueIds) {
    if (StringUtils.isBlank(html)) {
      return;
    }
    Matcher matcher = DIRECTIVE_PATTERN.matcher(html);
    while (matcher.find()) {
      String embeddedId = matcher.group(1).trim();
      if (StringUtils.isNotBlank(embeddedId)) {
        uniqueIds.add(embeddedId);
      }
    }
  }

  /**
   * Append a content block as JSON
   */
  private void appendContentJson(StringBuilder json, Content content) {
    json.append("{");
    json.append("\"id\":").append(content.getId()).append(",");
    json.append("\"uniqueId\":\"").append(JsonCommand.toJson(content.getUniqueId())).append("\",");

    String snippet = StringUtils.truncate(content.getContentAsText(), 120);
    json.append("\"snippet\":\"").append(JsonCommand.toJson(snippet)).append("\",");

    // Has draft?
    json.append("\"hasDraft\":").append(StringUtils.isNotBlank(content.getDraftContent())).append(",");

    // Modified date/time
    if (content.getModified() != null) {
      json.append("\"modified\":\"").append(JsonCommand.toJson(content.getModified().toString())).append("\",");
    } else if (content.getCreated() != null) {
      json.append("\"modified\":\"").append(JsonCommand.toJson(content.getCreated().toString())).append("\",");
    } else {
      json.append("\"modified\":\"\",");
    }

    // Modified by user
    String modifiedByName = "";
    long modifiedById = content.getModifiedBy() > 0 ? content.getModifiedBy() : content.getCreatedBy();
    if (modifiedById > 0) {
      User user = LoadUserCommand.loadUser(modifiedById);
      if (user != null) {
        modifiedByName = StringUtils.defaultString(user.getFullName(), user.getEmail());
      }
    }
    json.append("\"modifiedBy\":\"").append(JsonCommand.toJson(modifiedByName)).append("\"");

    json.append("}");
  }

  private WidgetContext writeOk(WidgetContext context, String dataJson, String metaJson) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"status\":\"ok\"");
    if (dataJson != null) {
      json.append(",\"data\":").append(dataJson);
    }
    if (metaJson != null) {
      json.append(",\"meta\":").append(metaJson);
    }
    json.append("}");
    context.setJson(json.toString());
    return context;
  }

  private WidgetContext writeError(WidgetContext context, String message) {
    context.setJson("{\"status\":\"error\",\"error\":\"" + JsonCommand.toJson(StringUtils.defaultString(message)) + "\"}");
    context.setSuccess(false);
    return context;
  }
}
