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
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.infrastructure.persistence.RoleRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * JSON service to list user roles (read-only) for the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-03-04
 */
public class CRMUserRolesJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908913L;
  private static Log LOG = LogFactory.getLog(CRMUserRolesJsonService.class);

  public WidgetContext execute(WidgetContext context) {

    if (!context.hasRole("admin")) {
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    List<Role> roles = RoleRepository.findAll();

    StringBuilder sb = new StringBuilder();
    sb.append("{\"roles\":[");
    if (roles != null) {
      boolean first = true;
      for (Role role : roles) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("{");
        sb.append("\"id\":").append(role.getId()).append(",");
        sb.append("\"code\":\"").append(JsonCommand.toJson(StringUtils.defaultString(role.getCode()))).append("\",");
        sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(role.getTitle()))).append("\",");
        sb.append("\"level\":").append(role.getLevel());
        sb.append("}");
      }
    }
    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }
}
