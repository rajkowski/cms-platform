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
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageProvider;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Triggers a recurring job immediately from the visual workflow editor
 *
 * @author matt rajkowski
 * @created 04/23/26 8:30 PM
 */
public class WorkflowTaskTriggerAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908903L;
  private static Log LOG = LogFactory.getLog(WorkflowTaskTriggerAjax.class);

  @Override
  public JsonServiceContext post(JsonServiceContext context) {

    LOG.debug("WorkflowTaskTriggerAjax...");

    // Check permissions
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to trigger workflow task");
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Validate the token for write operation
    String token = context.getParameter("token");
    if (StringUtils.isBlank(token) || !token.equals(context.getUserSession().getFormToken())) {
      context.setJson("{\"error\":\"Invalid token\"}");
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

    try {
      StorageProvider storageProvider = JobRunr.getBackgroundJobServer().getStorageProvider();
      RecurringJobsResult recurringJobs = storageProvider.getRecurringJobs();

      RecurringJob recurringJob = null;
      if (recurringJobs != null && !recurringJobs.isEmpty()) {
        for (RecurringJob job : recurringJobs) {
          if (jobId.equals(job.getId())) {
            recurringJob = job;
            break;
          }
        }
      }

      if (recurringJob == null) {
        context.setJson("{\"error\":\"Recurring job not found\"}");
        context.setSuccess(false);
        return context;
      }

      Job enqueuedJob = recurringJob.toEnqueuedJob();
      storageProvider.save(enqueuedJob);

      context.setJson("{\"status\":\"ok\",\"jobId\":\"" + JsonCommand.toJson(jobId) + "\",\"queuedJobId\":\""
          + JsonCommand.toJson(enqueuedJob.getId() != null ? enqueuedJob.getId().toString() : "") + "\"}");
    } catch (Exception e) {
      LOG.warn("Could not trigger workflow task " + jobId + ": " + e.getMessage());
      context.setJson("{\"error\":\"Unable to trigger workflow task\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
