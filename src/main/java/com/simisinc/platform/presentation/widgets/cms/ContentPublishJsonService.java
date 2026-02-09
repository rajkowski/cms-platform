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

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.PublishContentCommand;
import com.simisinc.platform.application.cms.SaveContentCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ContentSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX POST requests for /json/content/publish endpoint
 * Publishes content to live, optionally saving content first
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class ContentPublishJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(ContentPublishJsonService.class);

  /**
   * Handles POST requests to publish content
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext post(WidgetContext context) {

    // Check permissions - require admin or content-manager role
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      return writeError(context, "Permission denied");
    }

    try {
      // Get parameters
      long contentId = context.getParameterAsLong("contentId", -1);
      String uniqueId = context.getParameter("uniqueId");
      String contentHtml = context.getParameter("content");

      // If content is provided, save it first then publish
      // Otherwise, publish existing draft
      if (StringUtils.isNotBlank(contentHtml)) {
        // Create or load content bean
        Content contentBean = loadContentByIdOrUniqueId(contentId, uniqueId);
        if (contentBean == null) {
          if (StringUtils.isBlank(uniqueId)) {
            return writeError(context, "Content uniqueId is required");
          }
          contentBean = new Content();
          contentBean.setUniqueId(uniqueId);
          contentBean.setCreatedBy(context.getUserId());
        }
        
        contentBean.setContent(contentHtml);
        contentBean.setModifiedBy(context.getUserId());

        // Save as published (isDraft = false)
        Content savedContent = SaveContentCommand.saveContent(contentBean, false);

        if (savedContent == null) {
          return writeError(context, "Failed to publish content");
        }

        // Build success response
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\": ").append(savedContent.getId()).append(",");
        json.append("\"unique_id\": \"").append(JsonCommand.toJson(savedContent.getUniqueId())).append("\",");
        json.append("\"has_draft\": false");
        if (savedContent.getModified() != null) {
          json.append(",\"modified\": \"").append(JsonCommand.toJson(savedContent.getModified().toString())).append("\"");
        }
        json.append("}");

        return writeOk(context, json.toString(), null);

      } else {
        // Publish existing draft
        if (StringUtils.isBlank(uniqueId) && contentId > 0) {
          Content contentBean = loadContentByIdOrUniqueId(contentId, null);
          if (contentBean != null) {
            uniqueId = contentBean.getUniqueId();
          }
        }
        if (StringUtils.isBlank(uniqueId)) {
          return writeError(context, "Content uniqueId is required");
        }

        boolean published = PublishContentCommand.publishContent(uniqueId);

        if (!published) {
          return writeError(context, "Failed to publish content");
        }

        // Load the updated content
        Content content = ContentRepository.findByUniqueId(uniqueId);

        // Build success response
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\": ").append(content.getId()).append(",");
        json.append("\"unique_id\": \"").append(JsonCommand.toJson(content.getUniqueId())).append("\",");
        json.append("\"has_draft\": false");
        if (content.getModified() != null) {
          json.append(",\"modified\": \"").append(JsonCommand.toJson(content.getModified().toString())).append("\"");
        }
        json.append("}");

        return writeOk(context, json.toString(), null);
      }

    } catch (DataException e) {
      LOG.warn("Validation error publishing content: " + e.getMessage());
      return writeError(context, e.getMessage());
    } catch (Exception e) {
      LOG.error("Error publishing content: " + e.getMessage(), e);
      return writeError(context, "An unexpected error occurred while publishing content");
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
