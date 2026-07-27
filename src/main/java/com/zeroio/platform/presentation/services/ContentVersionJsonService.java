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

package com.zeroio.platform.presentation.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.zeroio.platform.domain.model.cms.ContentVersion;
import com.zeroio.platform.infrastructure.persistence.cms.ContentVersionRepository;

/**
 * JSON service for retrieving content version history
 * Endpoint: /json/content/versions?contentId=123
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ContentVersionJsonService extends GenericJsonService {

  private static Log LOG = LogFactory.getLog(ContentVersionJsonService.class);

  @Override
  public JsonServiceContext get(JsonServiceContext context) {

    // Permission is required
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      return context;
    }

    // Get content ID from query parameter
    long contentId = context.getParameterAsLong("contentId", -1L);

    if (contentId <= 0) {
      LOG.warn("Invalid content ID received: " + contentId);
      return context.writeError("Invalid content ID");
    }

    try {
      // Get version history
      List<ContentVersion> versions = ContentVersionRepository.findAllByContentId(contentId);

      if (versions == null || versions.isEmpty()) {
        return context.writeOk("[]");
      }

      // Batch load all users to avoid N+1 query problem
      Set<Long> userIds = new HashSet<>();
      for (ContentVersion version : versions) {
        if (version.getCreatedBy() > 0) {
          userIds.add(version.getCreatedBy());
        }
      }
      Map<Long, User> userMap = new HashMap<>();
      for (Long userId : userIds) {
        User user = UserRepository.findByUserId(userId);
        if (user != null) {
          userMap.put(userId, user);
        }
      }

      // Build JSON array
      StringBuilder json = new StringBuilder();
      json.append("[");

      boolean first = true;
      for (ContentVersion version : versions) {
        if (!first) {
          json.append(",");
        }
        first = false;

        // Get user from map (already loaded)
        String createdByName = "";
        User user = userMap.get(version.getCreatedBy());
        if (user != null) {
          createdByName = StringUtils.defaultString(user.getFullName());
        }

        json.append("{");
        json.append("\"versionId\":").append(version.getId()).append(",");
        json.append("\"versionNumber\":").append(version.getVersionNumber()).append(",");
        json.append("\"contentId\":").append(version.getContentId()).append(",");
        json.append("\"content\":\"").append(JsonCommand.toJson(StringUtils.defaultString(version.getContent()))).append("\",");
        json.append("\"createdBy\":").append(version.getCreatedBy()).append(",");
        json.append("\"createdByName\":\"").append(JsonCommand.toJson(createdByName)).append("\",");
        json.append("\"created\":\"").append(version.getCreated() != null ? version.getCreated().toString() : "").append("\",");
        json.append("\"notes\":\"").append(JsonCommand.toJson(StringUtils.defaultString(version.getNotes()))).append("\"");
        json.append("}");
      }

      json.append("]");

      return context.writeOk(json.toString());

    } catch (Exception e) {
      LOG.error("Error getting versions: " + e.getMessage(), e);
      return context.writeError("Error loading version history: " + e.getMessage());
    }
  }
}
