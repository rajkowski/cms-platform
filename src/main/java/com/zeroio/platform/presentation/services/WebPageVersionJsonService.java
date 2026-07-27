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

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.simisinc.platform.presentation.widgets.cms.SaveWebPageAjax;
import com.zeroio.platform.domain.model.cms.WebPageVersion;
import com.zeroio.platform.infrastructure.persistence.cms.WebPageVersionRepository;

/**
 * JSON service for retrieving web page XML version history
 * Endpoint: /json/webpage/versions?webPageId=123
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class WebPageVersionJsonService extends GenericJsonService {

  private static Log LOG = LogFactory.getLog(WebPageVersionJsonService.class);

  @Override
  public JsonServiceContext get(JsonServiceContext context) {

    if (!PermissionEngine.checkAccess(SaveWebPageAjax.class.getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + WebPageVersionJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long webPageId = context.getParameterAsLong("webPageId", -1L);
    if (webPageId <= 0) {
      LOG.warn("Invalid web page ID received: " + webPageId);
      return context.writeError("Invalid web page ID");
    }

    try {
      List<WebPageVersion> versions = WebPageVersionRepository.findAllByWebPageId(webPageId);
      if (versions == null || versions.isEmpty()) {
        return context.writeOk("[]");
      }

      Set<Long> userIds = new HashSet<>();
      for (WebPageVersion version : versions) {
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

      StringBuilder json = new StringBuilder();
      json.append("[");

      boolean first = true;
      for (WebPageVersion version : versions) {
        if (!first) {
          json.append(",");
        }
        first = false;

        String createdByName = "";
        User user = userMap.get(version.getCreatedBy());
        if (user != null) {
          createdByName = StringUtils.defaultString(user.getFullName());
        }

        json.append("{");
        json.append("\"versionId\":").append(version.getId()).append(",");
        json.append("\"webPageId\":").append(version.getWebPageId()).append(",");
        json.append("\"pageXml\":\"").append(JsonCommand.toJson(StringUtils.defaultString(version.getPageXml()))).append("\",");
        json.append("\"createdBy\":").append(version.getCreatedBy()).append(",");
        json.append("\"createdByName\":\"").append(JsonCommand.toJson(createdByName)).append("\",");
        json.append("\"created\":\"").append(version.getCreated() != null ? version.getCreated().toString() : "").append("\",");
        json.append("\"notes\":\"").append(JsonCommand.toJson(StringUtils.defaultString(version.getNotes()))).append("\"");
        json.append("}");
      }

      json.append("]");
      return context.writeOk(json.toString());
    } catch (Exception e) {
      LOG.error("Error getting web page versions: " + e.getMessage(), e);
      return context.writeError("Error loading version history: " + e.getMessage());
    }
  }
}