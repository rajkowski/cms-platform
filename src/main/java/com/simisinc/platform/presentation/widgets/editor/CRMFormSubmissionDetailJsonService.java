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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FormData;
import com.simisinc.platform.domain.model.cms.FormField;
import com.simisinc.platform.infrastructure.persistence.cms.FormDataRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * JSON service to return the full detail of a single form submission
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMFormSubmissionDetailJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908898L;
  private static Log LOG = LogFactory.getLog(CRMFormSubmissionDetailJsonService.class);

  public WidgetContext execute(WidgetContext context) {

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    long submissionId = context.getParameterAsLong("id");
    if (submissionId == -1) {
      context.setJson("{\"error\":\"id is required\"}");
      context.setSuccess(false);
      return context;
    }

    FormData formData = FormDataRepository.findById(submissionId);
    if (formData == null) {
      context.setJson("{\"error\":\"Submission not found\"}");
      context.setSuccess(false);
      return context;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"id\":").append(formData.getId()).append(",");
    sb.append("\"formUniqueId\":\"").append(JsonCommand.toJson(formData.getFormUniqueId())).append("\",");
    sb.append("\"ipAddress\":\"").append(JsonCommand.toJson(StringUtils.defaultString(formData.getIpAddress()))).append("\",");
    sb.append("\"url\":\"").append(JsonCommand.toJson(StringUtils.defaultString(formData.getUrl()))).append("\",");
    sb.append("\"created\":\"").append(formData.getCreated() != null ? formData.getCreated().toString() : "").append("\",");
    sb.append("\"claimed\":").append(formData.getClaimed() != null).append(",");
    sb.append("\"claimedDate\":").append(formData.getClaimed() != null ? "\"" + formData.getClaimed().toString() + "\"" : "null").append(",");
    sb.append("\"dismissed\":").append(formData.getDismissed() != null).append(",");
    sb.append("\"dismissedDate\":").append(formData.getDismissed() != null ? "\"" + formData.getDismissed().toString() + "\"" : "null").append(",");
    sb.append("\"processed\":").append(formData.getProcessed() != null).append(",");
    sb.append("\"processedDate\":").append(formData.getProcessed() != null ? "\"" + formData.getProcessed().toString() + "\"" : "null").append(",");
    sb.append("\"flaggedAsSpam\":").append(formData.getFlaggedAsSpam()).append(",");

    // Build field array
    sb.append("\"fields\":[");
    if (formData.getFormFieldList() != null) {
      boolean firstField = true;
      for (FormField field : formData.getFormFieldList()) {
        if (!firstField) {
          sb.append(",");
        }
        firstField = false;
        sb.append("{");
        sb.append("\"label\":\"").append(JsonCommand.toJson(StringUtils.defaultString(field.getLabel()))).append("\",");
        sb.append("\"name\":\"").append(JsonCommand.toJson(StringUtils.defaultString(field.getName()))).append("\",");
        sb.append("\"value\":\"").append(JsonCommand.toJson(StringUtils.defaultString(field.getUserValue()))).append("\"");
        sb.append("}");
      }
    }
    sb.append("]");
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
