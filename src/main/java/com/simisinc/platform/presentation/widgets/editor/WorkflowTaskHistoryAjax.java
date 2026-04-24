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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Returns recent job history for a specific scheduled task in the visual workflow editor
 *
 * @author matt rajkowski
 * @created 02/27/26 9:00 AM
 */
public class WorkflowTaskHistoryAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908902L;
  private static Log LOG = LogFactory.getLog(WorkflowTaskHistoryAjax.class);

  private static final int HISTORY_LIMIT = 25;

  @Override
  public JsonServiceContext get(JsonServiceContext context) {

    LOG.debug("WorkflowTaskHistoryAjax...");

    // Check permissions
    if (!context.hasRole("admin")) {
      LOG.debug("No permission to access workflow task history");
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Optional filter by recurring job ID (used by the right panel History tab)
    String jobId = context.getParameter("jobId");

    // Retrieve recent job history. When jobId is blank, return global history for live activity.
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"jobId\":\"").append(JsonCommand.toJson(jobId != null ? jobId : "")).append("\",");
    sb.append("\"history\":[");

    try {
      StorageProvider storageProvider = JobRunr.getBackgroundJobServer().getStorageProvider();
      RecurringJobsResult recurringJobs = storageProvider.getRecurringJobs();
      Map<String, String> recurringJobNames = new HashMap<>();

      if (recurringJobs != null && !recurringJobs.isEmpty()) {
        for (RecurringJob recurringJob : recurringJobs) {
          String recurringJobName = recurringJob.getJobName() != null ? recurringJob.getJobName() : recurringJob.getId();
          recurringJobNames.put(recurringJob.getId(), recurringJobName);
        }
      }

      boolean firstEntry = true;
      firstEntry = appendHistoryEntries(sb,
          storageProvider.getJobList(StateName.PROCESSING, new AmountRequest("updatedAt:DESC", HISTORY_LIMIT)),
          "PROCESSING", jobId, recurringJobNames, firstEntry);
      firstEntry = appendHistoryEntries(sb,
          storageProvider.getJobList(StateName.ENQUEUED, new AmountRequest("updatedAt:DESC", HISTORY_LIMIT)),
          "ENQUEUED", jobId, recurringJobNames, firstEntry);
      firstEntry = appendHistoryEntries(sb,
          storageProvider.getJobList(StateName.SUCCEEDED, new AmountRequest("updatedAt:DESC", HISTORY_LIMIT)),
          "SUCCEEDED", jobId, recurringJobNames, firstEntry);
      appendHistoryEntries(sb, storageProvider.getJobList(StateName.FAILED, new AmountRequest("updatedAt:DESC", HISTORY_LIMIT)),
          "FAILED", jobId, recurringJobNames, firstEntry);

    } catch (Exception e) {
      LOG.warn("Could not retrieve workflow task history: " + e.getMessage());
    }

    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }

  private boolean appendHistoryEntries(StringBuilder sb, List<Job> jobs, String state, String jobIdFilter,
      Map<String, String> recurringJobNames, boolean firstEntry) {

    if (jobs == null || jobs.isEmpty()) {
      return firstEntry;
    }

    for (Job job : jobs) {
      if (!job.getRecurringJobId().isPresent()) {
        continue;
      }

      String recurringJobId = job.getRecurringJobId().get();
      if (StringUtils.isNotBlank(jobIdFilter) && !jobIdFilter.equals(recurringJobId)) {
        continue;
      }

      if (!firstEntry) {
        sb.append(",");
      }

      sb.append("{");
      sb.append("\"state\":\"").append(state).append("\",");
      sb.append("\"jobId\":\"").append(JsonCommand.toJson(recurringJobId)).append("\",");
      sb.append("\"jobName\":\"").append(JsonCommand.toJson(recurringJobNames.getOrDefault(recurringJobId, recurringJobId)))
          .append("\",");
      sb.append("\"updatedAt\":\"").append(JsonCommand.toJson(job.getUpdatedAt() != null ? job.getUpdatedAt().toString() : ""))
          .append("\"");
      sb.append("}");
      firstEntry = false;
    }

    return firstEntry;
  }
}
