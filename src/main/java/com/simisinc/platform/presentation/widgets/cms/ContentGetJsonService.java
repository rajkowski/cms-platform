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

import com.simisinc.platform.application.cms.TinyMceCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ContentSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX GET requests for /json/content/get endpoint
 * Returns a single content block by ID or uniqueId
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class ContentGetJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(ContentGetJsonService.class);

  /**
   * Handles GET requests to retrieve a single content block
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
      // Get content by uniqueId
      long contentId = context.getParameterAsLong("contentId", -1);
      String uniqueId = context.getParameter("uniqueId");
      
      Content content = loadContentByIdOrUniqueId(contentId, uniqueId);

      if (content == null) {
        return writeError(context, "Content not found");
      }

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"id\": ").append(content.getId()).append(",");
      json.append("\"unique_id\": \"").append(JsonCommand.toJson(content.getUniqueId())).append("\",");
      
      // Include published content, prepared for editor
      if (StringUtils.isNotBlank(content.getContent())) {
        String preparedContent = TinyMceCommand.prepareContentForEditor(content.getContent());
        json.append("\"content\": \"").append(JsonCommand.toJson(preparedContent)).append("\",");
      } else {
        json.append("\"content\": \"\",");
      }
      
      // Include draft content if exists, prepared for editor
      if (StringUtils.isNotBlank(content.getDraftContent())) {
        String preparedDraft = TinyMceCommand.prepareContentForEditor(content.getDraftContent());
        json.append("\"draft_content\": \"").append(JsonCommand.toJson(preparedDraft)).append("\",");
      } else {
        json.append("\"draft_content\": \"\",");
      }
      
      // Include metadata
      if (content.getCreated() != null) {
        json.append("\"created\": \"").append(JsonCommand.toJson(content.getCreated().toString())).append("\",");
      }
      if (content.getModified() != null) {
        json.append("\"modified\": \"").append(JsonCommand.toJson(content.getModified().toString())).append("\",");
      }
      json.append("\"created_by\": ").append(content.getCreatedBy()).append(",");
      json.append("\"modified_by\": ").append(content.getModifiedBy());
      json.append("}");

      return writeOk(context, json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error getting content: " + e.getMessage(), e);
      return writeError(context, e.getMessage());
    }
  }

  private Content loadContentByIdOrUniqueId(long contentId, String uniqueId) {
    if (contentId > 0) {
      ContentSpecification specification = new ContentSpecification();
      specification.setId(contentId);
      DataConstraints constraints = new DataConstraints(1, 1);
      List<Content> results = ContentRepository.findAll(specification, constraints);
      if (results != null && !results.isEmpty()) {
        return results.get(0);
      }
    }
    if (StringUtils.isNotBlank(uniqueId)) {
      return ContentRepository.findByUniqueId(uniqueId);
    }
    return null;
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
