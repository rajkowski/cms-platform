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
import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.cms.SaveContentCommand;
import com.simisinc.platform.application.cms.TinyMceCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ContentSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Handles JSON/AJAX POST requests for /json/content/save-draft endpoint
 * Saves content as a draft without publishing
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class ContentSaveDraftJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(ContentSaveDraftJsonService.class);

  /**
   * Handles POST requests to save content as draft
   *
   * @param context the JSON service context
   * @return context with JSON response
   */
  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + ContentSaveDraftJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    try {
      // Get parameters
      long contentId = context.getParameterAsLong("contentId", -1);
      String uniqueId = context.getParameter("uniqueId");
      String contentHtml = StringUtils.defaultString(context.getParameter("content"));

      if (StringUtils.isBlank(contentHtml)) {
        contentHtml = "<p></p>";
      }

      // Update content from editor (convert content block spans to text)
      contentHtml = TinyMceCommand.updateContentFromEditor(contentHtml);

      // Create or load content bean
      Content contentBean = loadContentByIdOrUniqueId(contentId, uniqueId);
      if (contentBean == null) {
        if (StringUtils.isBlank(uniqueId)) {
          return context.writeError("Content uniqueId is required");
        }
        contentBean = new Content();
        contentBean.setUniqueId(uniqueId);
        contentBean.setCreatedBy(context.getUserId());
      }

      contentBean.setDraftContent(contentHtml);
      contentBean.setModifiedBy(context.getUserId());

      // Save as draft
      Content savedContent = SaveContentCommand.saveContent(contentBean, true);

      if (savedContent == null) {
        return context.writeError("Failed to save draft");
      }

      // Build success response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"id\": ").append(savedContent.getId()).append(",");
      json.append("\"unique_id\": \"").append(JsonCommand.toJson(savedContent.getUniqueId())).append("\",");
      json.append("\"has_draft\": true");
      if (savedContent.getModified() != null) {
        json.append(",\"modified\": \"").append(JsonCommand.toJson(savedContent.getModified().toString())).append("\"");
      }
      json.append("}");

      return context.writeOk(json.toString(), null);

    } catch (DataException e) {
      LOG.warn("Validation error saving draft: " + e.getMessage());
      return context.writeError(e.getMessage());
    } catch (Exception e) {
      LOG.error("Error saving draft: " + e.getMessage(), e);
      return context.writeError("An unexpected error occurred while saving draft");
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

}
