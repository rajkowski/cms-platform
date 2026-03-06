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

package com.simisinc.platform.presentation.widgets.editor;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.infrastructure.persistence.UserSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to list and search users for the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-03-04
 */
public class CRMUsersJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908910L;
  private static Log LOG = LogFactory.getLog(CRMUsersJsonService.class);

  public JsonServiceContext get(JsonServiceContext context) {

    if (!context.hasRole("admin")) {
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("limit", 25);
    String searchTerm = context.getParameter("search");
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);

    UserSpecification specification = new UserSpecification();
    if (StringUtils.isNotBlank(searchTerm)) {
      specification.setMatchesName(searchTerm.trim());
    }

    List<User> users = UserRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{\"users\":[");
    if (users != null) {
      boolean first = true;
      for (User user : users) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("{");
        sb.append("\"id\":").append(user.getId()).append(",");
        sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getUniqueId()))).append("\",");
        sb.append("\"firstName\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getFirstName()))).append("\",");
        sb.append("\"lastName\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getLastName()))).append("\",");
        sb.append("\"email\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getEmail()))).append("\",");
        sb.append("\"username\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getUsername()))).append("\",");
        sb.append("\"organization\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getOrganization()))).append("\",");
        sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getTitle()))).append("\",");
        sb.append("\"city\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getCity()))).append("\",");
        sb.append("\"state\":\"").append(JsonCommand.toJson(StringUtils.defaultString(user.getState()))).append("\",");
        sb.append("\"enabled\":").append(user.isEnabled()).append(",");
        sb.append("\"validated\":").append(user.getValidated() != null ? "true" : "false").append(",");
        sb.append("\"created\":\"").append(user.getCreated() != null ? user.getCreated().toString() : "").append("\"");
        sb.append("}");
      }
    }
    sb.append("],");
    sb.append("\"page\":").append(page).append(",");
    sb.append("\"limit\":").append(itemsPerPage).append(",");
    sb.append("\"total\":").append(constraints.getTotalRecordCount());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
