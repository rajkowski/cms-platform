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
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns recent job history for a specific scheduled task in the visual workflow editor
 *
 * @author matt rajkowski
 * @created 02/27/26 9:00 AM
 */
public class WorkflowTaskHistoryAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908902L;
  private static Log LOG = LogFactory.getLog(WorkflowTaskHistoryAjax.class);

  private static final int HISTORY_LIMIT = 25;

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("WorkflowTaskHistoryAjax...");

    // Check permissions
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to access workflow task history");
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Require a job ID
    String jobId = context.getParameter("jobId");
    if (StringUtils.isBlank(jobId)) {
      context.setJson("{\"error\":\"jobId parameter is required\"}");
      context.setSuccess(false);
      return context;
    }

    // Retrieve recent succeeded and failed job history
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"jobId\":\"").append(JsonCommand.toJson(jobId)).append("\",");
    sb.append("\"history\":[");

    try {
      StorageProvider storageProvider = JobRunr.getBackgroundJobServer().getStorageProvider();

      boolean firstEntry = true;

      // Get recent succeeded jobs
      List<Job> succeededJobs = storageProvider.getJobList(StateName.SUCCEEDED, new AmountRequest("updatedAt:DESC", HISTORY_LIMIT));
      for (Job job : succeededJobs) {
        if (job.getRecurringJobId().filter(id -> id.equals(jobId)).isPresent()) {
          if (!firstEntry) {
            sb.append(",");
          }
          sb.append("{");
          sb.append("\"state\":\"SUCCEEDED\",");
          sb.append("\"updatedAt\":\"").append(JsonCommand.toJson(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : ""))
              .append("\"");
          sb.append("}");
          firstEntry = false;
        }
      }

      // Get recent failed jobs
      List<Job> failedJobs = storageProvider.getJobList(StateName.FAILED, new AmountRequest("updatedAt:DESC", HISTORY_LIMIT));
      for (Job job : failedJobs) {
        if (job.getRecurringJobId().filter(id -> id.equals(jobId)).isPresent()) {
          if (!firstEntry) {
            sb.append(",");
          }
          sb.append("{");
          sb.append("\"state\":\"FAILED\",");
          sb.append("\"updatedAt\":\"").append(JsonCommand.toJson(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : ""))
              .append("\"");
          sb.append("}");
          firstEntry = false;
        }
      }

    } catch (Exception e) {
      LOG.warn("Could not retrieve job history for " + jobId + ": " + e.getMessage());
    }

    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }
}
