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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.infrastructure.persistence.GroupRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns all system user groups for folder permission setup in the visual document editor
 *
 * @author matt rajkowski
 * @created 2/19/26
 */
public class DocumentUserGroupsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908908L;
  private static Log LOG = LogFactory.getLog(DocumentUserGroupsAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DocumentUserGroupsAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"groups\":[]}");
      return context;
    }

    List<Group> allGroups = GroupRepository.findAll();

    StringBuilder sb = new StringBuilder();
    sb.append("{\"groups\":[");

    boolean first = true;
    if (allGroups != null) {
      for (Group group : allGroups) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("{");
        sb.append("\"id\":").append(group.getId()).append(",");
        sb.append("\"name\":\"").append(group.getName() != null ? group.getName().replace("\"", "\\\"") : "").append("\"");
        sb.append("}");
      }
    }

    sb.append("]}");
    context.setJson(sb.toString());
    return context;
  }
}
