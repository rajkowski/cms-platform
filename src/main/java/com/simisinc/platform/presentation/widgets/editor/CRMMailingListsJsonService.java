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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.mailinglists.MailingList;
import com.simisinc.platform.infrastructure.persistence.mailinglists.MailingListRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to list all mailing lists with member counts
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMMailingListsJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908900L;
  private static Log LOG = LogFactory.getLog(CRMMailingListsJsonService.class);

  public JsonServiceContext get(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMMailingListsJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    List<MailingList> mailingLists = MailingListRepository.findAll();

    StringBuilder sb = new StringBuilder();
    sb.append("{\"mailingLists\":[");
    boolean first = true;
    for (MailingList list : mailingLists) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("{");
      sb.append("\"id\":").append(list.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(list.getName())).append("\",");
      sb.append("\"title\":\"").append(JsonCommand.toJson(list.getTitle() != null ? list.getTitle() : "")).append("\",");
      sb.append("\"description\":\"").append(JsonCommand.toJson(list.getDescription() != null ? list.getDescription() : ""))
          .append("\",");
      sb.append("\"memberCount\":").append(list.getMemberCount()).append(",");
      sb.append("\"enabled\":").append(list.getEnabled()).append(",");
      sb.append("\"showOnline\":").append(list.getShowOnline());
      sb.append("}");
    }
    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }
}
