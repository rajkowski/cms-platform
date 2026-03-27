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

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FormData;
import com.simisinc.platform.domain.model.cms.FormField;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.FormDataRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FormDataSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to list form submissions for a given form category
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMFormSubmissionsJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908897L;
  private static Log LOG = LogFactory.getLog(CRMFormSubmissionsJsonService.class);

  public JsonServiceContext get(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMFormSubmissionsJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    String formUniqueId = context.getParameter("formUniqueId");
    if (StringUtils.isBlank(formUniqueId)) {
      return context.writeError("Permission Denied");
    }

    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("limit", 25);
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);

    String statusFilter = context.getParameter("status");

    FormDataSpecification specification = new FormDataSpecification();
    specification.setFormUniqueId(formUniqueId);
    specification.setFlaggedAsSpam(false);
    if ("new".equals(statusFilter)) {
      specification.setClaimed(false);
      specification.setDismissed(false);
      specification.setProcessed(false);
    } else if ("claimed".equals(statusFilter)) {
      specification.setClaimed(true);
    } else if ("dismissed".equals(statusFilter)) {
      specification.setDismissed(true);
    } else if ("processed".equals(statusFilter)) {
      specification.setProcessed(true);
    }

    List<FormData> formDataList = FormDataRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{\"submissions\":[");
    boolean first = true;
    for (FormData formData : formDataList) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("{");
      sb.append("\"id\":").append(formData.getId()).append(",");
      sb.append("\"formUniqueId\":\"").append(JsonCommand.toJson(formData.getFormUniqueId())).append("\",");
      sb.append("\"created\":\"").append(formData.getCreated() != null ? formData.getCreated().toString() : "").append("\",");
      sb.append("\"claimed\":").append(formData.getClaimed() != null).append(",");
      sb.append("\"dismissed\":").append(formData.getDismissed() != null).append(",");
      sb.append("\"processed\":").append(formData.getProcessed() != null).append(",");
      // Include a summary - use first non-empty field value
      String summary = "";
      if (formData.getFormFieldList() != null) {
        for (FormField field : formData.getFormFieldList()) {
          if (StringUtils.isNotBlank(field.getUserValue())) {
            summary = field.getUserValue();
            if (summary.length() > 100) {
              summary = summary.substring(0, 100) + "...";
            }
            break;
          }
        }
      }
      sb.append("\"summary\":\"").append(JsonCommand.toJson(summary)).append("\"");
      sb.append("}");
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
