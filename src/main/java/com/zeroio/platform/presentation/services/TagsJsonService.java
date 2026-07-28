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
package com.zeroio.platform.presentation.services;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Handles JSON/AJAX GET requests for /json/tags endpoint
 * Returns all distinct tags from published web pages
 * Available to all users (no authentication required)
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class TagsJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(TagsJsonService.class);

  /**
   * Handles GET requests for tags
   *
   * @param context the JSON service context
   * @return context with JSON response
   */
  public JsonServiceContext get(JsonServiceContext context) {
    try {
      // Get all distinct tags from published pages
      List<String> tags = WebPageRepository.findAllDistinctTags();

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"status\":\"ok\",");
      json.append("\"meta\":{\"type\":\"tags\",\"count\":").append(tags.size()).append("},");
      json.append("\"data\":[");

      if (tags != null && !tags.isEmpty()) {
        for (int i = 0; i < tags.size(); i++) {
          if (i > 0) {
            json.append(",");
          }
          json.append("\"").append(JsonCommand.toJson(tags.get(i))).append("\"");
        }
      }

      json.append("]}");

      // Set the response
      context.setJson(json.toString());
      return context;

    } catch (Exception e) {
      LOG.error("Error fetching tags", e);
      context.setJson("{\"status\":\"error\",\"error\":{\"title\":\"Failed to fetch tags\"}}");
      return context;
    }
  }
}
