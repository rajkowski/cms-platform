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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.infrastructure.persistence.UserSpecification;
import com.simisinc.platform.presentation.controller.DataConstants;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Handles JSON/AJAX GET requests for /json/users endpoint
 * Returns all enabled users for contributor dropdown
 * Available to all users (no authentication required)
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class UsersJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(UsersJsonService.class);

  /**
   * Handles GET requests for users
   *
   * @param context the JSON service context
   * @return context with JSON response
   */
  public JsonServiceContext get(JsonServiceContext context) {

    // @note Change users for specific uses, like finding Content Contributors

    // Permission is required
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      return context;
    }

    try {
      // Get only enabled users
      UserSpecification specification = new UserSpecification();
      specification.setIsEnabled(DataConstants.TRUE);

      DataConstraints constraints = new DataConstraints();
      constraints.setColumnToSortBy("first_name", "asc");

      List<User> users = UserRepository.findAll(specification, constraints);

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"status\":\"ok\",");
      json.append("\"meta\":{\"type\":\"users\",\"count\":").append(users != null ? users.size() : 0).append("},");
      json.append("\"data\":[");

      if (users != null && !users.isEmpty()) {
        for (int i = 0; i < users.size(); i++) {
          if (i > 0) {
            json.append(",");
          }
          User user = users.get(i);

          // Build display name (First Last or email if no name)
          String displayName = "";
          if (StringUtils.isNotBlank(user.getFirstName()) || StringUtils.isNotBlank(user.getLastName())) {
            displayName = StringUtils.trimToEmpty(user.getFirstName()) + " " + StringUtils.trimToEmpty(user.getLastName());
            displayName = displayName.trim();
          } else if (StringUtils.isNotBlank(user.getEmail())) {
            displayName = user.getEmail();
          }

          json.append("{");
          json.append("\"id\":").append(user.getId()).append(",");
          json.append("\"firstName\":\"").append(JsonCommand.toJson(user.getFirstName())).append("\",");
          json.append("\"lastName\":\"").append(JsonCommand.toJson(user.getLastName())).append("\",");
          json.append("\"email\":\"").append(JsonCommand.toJson(user.getEmail())).append("\",");
          json.append("\"displayName\":\"").append(JsonCommand.toJson(displayName)).append("\"");
          json.append("}");
        }
      }

      json.append("]}");

      // Set the response
      context.setJson(json.toString());
      return context;

    } catch (Exception e) {
      LOG.error("Error fetching users", e);
      context.setJson("{\"status\":\"error\",\"error\":{\"title\":\"Failed to fetch users\"}}");
      return context;
    }
  }
}
