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
import com.simisinc.platform.domain.model.mailinglists.Email;
import com.simisinc.platform.domain.model.mailinglists.MailingList;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.mailinglists.EmailRepository;
import com.simisinc.platform.infrastructure.persistence.mailinglists.EmailSpecification;
import com.simisinc.platform.infrastructure.persistence.mailinglists.MailingListRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * JSON service to list members of a mailing list
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMMailingListMembersJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908901L;
  private static Log LOG = LogFactory.getLog(CRMMailingListMembersJsonService.class);

  public WidgetContext execute(WidgetContext context) {

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    long mailingListId = context.getParameterAsLong("listId");
    if (mailingListId == -1) {
      context.setJson("{\"error\":\"listId is required\"}");
      context.setSuccess(false);
      return context;
    }

    MailingList mailingList = MailingListRepository.findById(mailingListId);
    if (mailingList == null) {
      context.setJson("{\"error\":\"Mailing list not found\"}");
      context.setSuccess(false);
      return context;
    }

    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("limit", 25);
    String searchTerm = context.getParameter("search");
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);

    EmailSpecification specification = new EmailSpecification();
    specification.setMailingListId(mailingListId);
    if (StringUtils.isNotBlank(searchTerm)) {
      specification.setMatchesEmail(searchTerm);
    }

    List<Email> emailList = EmailRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{\"members\":[");
    boolean first = true;
    for (Email email : emailList) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("{");
      sb.append("\"id\":").append(email.getId()).append(",");
      sb.append("\"email\":\"").append(JsonCommand.toJson(StringUtils.defaultString(email.getEmail()))).append("\",");
      sb.append("\"firstName\":\"").append(JsonCommand.toJson(StringUtils.defaultString(email.getFirstName()))).append("\",");
      sb.append("\"lastName\":\"").append(JsonCommand.toJson(StringUtils.defaultString(email.getLastName()))).append("\",");
      sb.append("\"organization\":\"").append(JsonCommand.toJson(StringUtils.defaultString(email.getOrganization()))).append("\",");
      sb.append("\"subscribed\":\"").append(email.getSubscribed() != null ? email.getSubscribed().toString() : "").append("\",");
      sb.append("\"unsubscribed\":").append(email.getUnsubscribed() != null).append(",");
      sb.append("\"valid\":").append(email.isValid());
      sb.append("}");
    }
    sb.append("],");
    sb.append("\"listId\":").append(mailingListId).append(",");
    sb.append("\"listName\":\"").append(JsonCommand.toJson(mailingList.getName())).append("\",");
    sb.append("\"page\":").append(page).append(",");
    sb.append("\"limit\":").append(itemsPerPage).append(",");
    sb.append("\"total\":").append(constraints.getTotalRecordCount());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
