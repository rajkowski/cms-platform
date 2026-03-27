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
import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.application.mailinglists.SaveMailingListCommand;
import com.simisinc.platform.domain.model.mailinglists.MailingList;
import com.simisinc.platform.infrastructure.persistence.mailinglists.MailingListRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to save (create/update) a mailing list from the CRM editor
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMSaveMailingListAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908880L;
  private static Log LOG = LogFactory.getLog(CRMSaveMailingListAjax.class);

  public JsonServiceContext post(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMSaveMailingListAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long id = context.getParameterAsLong("id", -1L);
    String name = StringUtils.trimToNull(context.getParameter("name"));
    String title = StringUtils.trimToNull(context.getParameter("title"));
    String description = StringUtils.trimToNull(context.getParameter("description"));
    boolean showOnline = "true".equalsIgnoreCase(context.getParameter("showOnline"));

    if (StringUtils.isBlank(name)) {
      return context.writeError("A name is required");
    }

    try {
      MailingList mailingListBean;
      if (id > -1) {
        mailingListBean = MailingListRepository.findById(id);
        if (mailingListBean == null) {
          return context.writeError("Mailing list not found");
        }
      } else {
        mailingListBean = new MailingList();
        mailingListBean.setEnabled(true);
      }

      mailingListBean.setName(name);
      mailingListBean.setTitle(title != null ? title : name);
      mailingListBean.setDescription(description);
      mailingListBean.setShowOnline(showOnline);

      MailingList saved = SaveMailingListCommand.saveMailingList(mailingListBean);
      if (saved == null) {
        throw new DataException("Mailing list could not be saved");
      }

      StringBuilder sb = new StringBuilder();
      sb.append("{\"success\":true,");
      sb.append("\"message\":\"Mailing list saved\",");
      sb.append("\"mailingList\":{");
      sb.append("\"id\":").append(saved.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(saved.getName())).append("\",");
      sb.append("\"title\":\"").append(JsonCommand.toJson(saved.getTitle() != null ? saved.getTitle() : "")).append("\"");
      sb.append("}}");

      context.setJson(sb.toString());
      return context;

    } catch (DataException de) {
      LOG.error("DataException", de);
      return context.writeError("" + de.getMessage());
    } catch (Exception e) {
      LOG.error("Exception", e);
      return context.writeError("An error occurred");
    }
  }
}
