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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.SaveGroupCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.infrastructure.persistence.GroupRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to save (create/update) a user group from the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-03-04
 */
public class CRMSaveUserGroupAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908912L;
  private static Log LOG = LogFactory.getLog(CRMSaveUserGroupAjax.class);

  public JsonServiceContext post(JsonServiceContext context) {

    if (!context.hasRole("admin")) {
      context.setJson("{\"status\":\"error\",\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    long id = context.getParameterAsLong("id", -1L);
    String name = StringUtils.trimToNull(context.getParameter("name"));
    String uniqueId = StringUtils.trimToNull(context.getParameter("uniqueId"));
    String description = StringUtils.trimToNull(context.getParameter("description"));

    if (StringUtils.isBlank(name)) {
      context.setJson("{\"status\":\"error\",\"error\":\"A group name is required\"}");
      context.setSuccess(false);
      return context;
    }

    try {
      Group groupBean;
      if (id > -1) {
        groupBean = GroupRepository.findById(id);
        if (groupBean == null) {
          context.setJson("{\"status\":\"error\",\"error\":\"Group not found\"}");
          context.setSuccess(false);
          return context;
        }
      } else {
        groupBean = new Group();
      }
      groupBean.setName(name);
      groupBean.setUniqueId(uniqueId);
      groupBean.setDescription(description);

      Group savedGroup = SaveGroupCommand.saveGroup(groupBean);
      if (savedGroup == null) {
        context.setJson("{\"status\":\"error\",\"error\":\"Could not save the group\"}");
        context.setSuccess(false);
        return context;
      }

      StringBuilder sb = new StringBuilder();
      sb.append("{\"status\":\"ok\",\"group\":{");
      sb.append("\"id\":").append(savedGroup.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(StringUtils.defaultString(savedGroup.getName()))).append("\",");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(StringUtils.defaultString(savedGroup.getUniqueId()))).append("\",");
      sb.append("\"description\":\"").append(JsonCommand.toJson(StringUtils.defaultString(savedGroup.getDescription()))).append("\",");
      sb.append("\"userCount\":").append(savedGroup.getUserCount());
      sb.append("}}");
      context.setJson(sb.toString());

    } catch (DataException de) {
      context.setJson("{\"status\":\"error\",\"error\":\"" + JsonCommand.toJson(de.getMessage()) + "\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
