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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageProvider;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Returns scheduled task information for the visual workflow editor
 *
 * @author matt rajkowski
 * @created 02/27/26 9:00 AM
 */
public class WorkflowTasksAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908900L;
  private static Log LOG = LogFactory.getLog(WorkflowTasksAjax.class);

  @Override
  public JsonServiceContext get(JsonServiceContext context) {

    LOG.debug("WorkflowTasksAjax...");

    // Check permissions
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to access workflow tasks");
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Get job stats and recurring jobs from the scheduler
    long totalJobs = 0;
    long enqueuedCount = 0;
    long succeededCount = 0;
    long failedCount = 0;
    RecurringJobsResult recurringJobs = null;

    try {
      StorageProvider storageProvider = JobRunr.getBackgroundJobServer().getStorageProvider();
      JobStats stats = storageProvider.getJobStats();
      totalJobs = stats.getRecurringJobs();
      enqueuedCount = stats.getEnqueued() != null ? stats.getEnqueued() : 0;
      succeededCount = stats.getSucceeded() != null ? stats.getSucceeded() : 0;
      failedCount = stats.getFailed() != null ? stats.getFailed() : 0;
      recurringJobs = storageProvider.getRecurringJobs();
    } catch (Exception e) {
      LOG.warn("Could not retrieve job stats from scheduler: " + e.getMessage());
    }

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"stats\":{");
    sb.append("\"total\":").append(totalJobs).append(",");
    sb.append("\"enqueued\":").append(enqueuedCount).append(",");
    sb.append("\"succeeded\":").append(succeededCount).append(",");
    sb.append("\"failed\":").append(failedCount);
    sb.append("},");
    sb.append("\"jobs\":[");

    if (recurringJobs != null && !recurringJobs.isEmpty()) {
      boolean first = true;
      for (RecurringJob job : recurringJobs) {
        if (!first) {
          sb.append(",");
        }
        sb.append("{");
        sb.append("\"id\":\"").append(JsonCommand.toJson(job.getId())).append("\",");
        String jobName = job.getJobName() != null ? job.getJobName() : job.getId();
        sb.append("\"name\":\"").append(JsonCommand.toJson(jobName)).append("\",");
        String schedule = job.getScheduleExpression() != null ? job.getScheduleExpression() : "";
        sb.append("\"schedule\":\"").append(JsonCommand.toJson(schedule)).append("\",");
        String nextRun = job.getNextRun() != null ? job.getNextRun().toString() : "";
        sb.append("\"nextRun\":\"").append(JsonCommand.toJson(nextRun)).append("\"");
        sb.append("}");
        first = false;
      }
    }

    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }
}
