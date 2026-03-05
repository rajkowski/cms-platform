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

import com.simisinc.platform.domain.model.cms.FormData;
import com.simisinc.platform.infrastructure.persistence.cms.FormDataRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * JSON service to update the status of a form submission: claim, dismiss, or process
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMFormSubmissionUpdateJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908899L;
  private static Log LOG = LogFactory.getLog(CRMFormSubmissionUpdateJsonService.class);

  public WidgetContext execute(WidgetContext context) {

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    long submissionId = context.getParameterAsLong("id");
    String action = context.getParameter("action");

    if (submissionId == -1 || StringUtils.isBlank(action)) {
      context.setJson("{\"error\":\"id and action are required\"}");
      context.setSuccess(false);
      return context;
    }

    FormData formData = FormDataRepository.findById(submissionId);
    if (formData == null) {
      context.setJson("{\"error\":\"Submission not found\"}");
      context.setSuccess(false);
      return context;
    }

    long userId = context.getUserId();
    boolean success = false;

    switch (action) {
      case "claim":
        success = FormDataRepository.tryToMarkAsClaimed(formData, userId);
        break;
      case "dismiss":
        success = FormDataRepository.markAsArchived(formData, userId);
        break;
      case "process":
        success = FormDataRepository.markAsProcessed(formData, userId);
        break;
      default:
        context.setJson("{\"error\":\"Unknown action: " + action + "\"}");
        context.setSuccess(false);
        return context;
    }

    if (!success) {
      context.setJson("{\"error\":\"Action could not be completed\"}");
      context.setSuccess(false);
      return context;
    }

    context.setJson("{\"status\":\"ok\",\"id\":" + submissionId + ",\"action\":\"" + action + "\"}");
    return context;
  }
}
