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
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.infrastructure.persistence.GroupRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * JSON service to list user groups for the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-03-04
 */
public class CRMUserGroupsJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908911L;
  private static Log LOG = LogFactory.getLog(CRMUserGroupsJsonService.class);

  public WidgetContext execute(WidgetContext context) {

    if (!context.hasRole("admin")) {
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    List<Group> groups = GroupRepository.findAll();

    StringBuilder sb = new StringBuilder();
    sb.append("{\"groups\":[");
    if (groups != null) {
      boolean first = true;
      for (Group group : groups) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("{");
        sb.append("\"id\":").append(group.getId()).append(",");
        sb.append("\"name\":\"").append(JsonCommand.toJson(StringUtils.defaultString(group.getName()))).append("\",");
        sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(StringUtils.defaultString(group.getUniqueId()))).append("\",");
        sb.append("\"description\":\"").append(JsonCommand.toJson(StringUtils.defaultString(group.getDescription()))).append("\",");
        sb.append("\"oAuthPath\":\"").append(JsonCommand.toJson(StringUtils.defaultString(group.getOAuthPath()))).append("\",");
        sb.append("\"userCount\":").append(group.getUserCount());
        sb.append("}");
      }
    }
    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }
}
