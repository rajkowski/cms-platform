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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.LoadContentListCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX GET requests for /json/content/list endpoint
 * Returns a paginated list of content blocks with search support
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class ContentListJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(ContentListJsonService.class);

  /**
   * Handles GET requests for content list
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext execute(WidgetContext context) {

    // Check permissions - require admin or content-manager role
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      return writeError(context, "Permission denied");
    }

    try {
      // Get pagination parameters
      int offset = context.getParameterAsInt("offset", 0);
      int limit = context.getParameterAsInt("limit", 50);
      String searchTerm = context.getParameter("search");
      String sortBy = context.getParameter("sortBy");

      // Load content list
      List<Content> contentList = LoadContentListCommand.loadContentList(searchTerm, offset, limit, sortBy);

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("[");

      if (contentList != null && !contentList.isEmpty()) {
        boolean first = true;
        for (Content content : contentList) {
          if (!first) {
            json.append(",");
          }
          first = false;

          json.append("{");
          json.append("\"id\": ").append(content.getId()).append(",");
          json.append("\"unique_id\": \"").append(JsonCommand.toJson(content.getUniqueId())).append("\",");
          
          // Include modified date if available
          if (content.getModified() != null) {
            json.append("\"modified\": \"").append(JsonCommand.toJson(content.getModified().toString())).append("\",");
          }
          
          // Indicate if draft exists
          boolean hasDraft = StringUtils.isNotBlank(content.getDraftContent());
          json.append("\"draft_content\": ").append(hasDraft).append(",");
          
          // Include highlight if available (from search)
          if (StringUtils.isNotBlank(content.getHighlight())) {
            json.append("\"highlight\": \"").append(JsonCommand.toJson(content.getHighlight())).append("\",");
          }
          
          // Include preview of content (first 160 chars)
          String preview = "";
          if (StringUtils.isNotBlank(content.getContent())) {
            preview = content.getContent().length() > 160 
                ? content.getContent().substring(0, 160) + "..." 
                : content.getContent();
            preview = preview.replaceAll("<[^>]*>", "");
          }
          json.append("\"content\": \"").append(JsonCommand.toJson(preview)).append("\"");
          
          json.append("}");
        }
        
        // Check if there are more results
        boolean hasMore = contentList.size() >= limit;
        json.append("]");
        StringBuilder meta = new StringBuilder();
        meta.append("{");
        meta.append("\"offset\": ").append(offset).append(",");
        meta.append("\"limit\": ").append(limit).append(",");
        meta.append("\"hasMore\": ").append(hasMore);
        meta.append("}");
        return writeOk(context, json.toString(), meta.toString());
      } else {
        json.append("]");
        StringBuilder meta = new StringBuilder();
        meta.append("{");
        meta.append("\"offset\": ").append(offset).append(",");
        meta.append("\"limit\": ").append(limit).append(",");
        meta.append("\"hasMore\": false");
        meta.append("}");
        return writeOk(context, json.toString(), meta.toString());
      }

    } catch (Exception e) {
      LOG.error("Error loading content list: " + e.getMessage(), e);
      return writeError(context, e.getMessage());
    }
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
