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

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.jobs.states.SucceededState;
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
  private static final String SORT_BY_UPDATED_AT_DESC = "updatedAt:DESC";

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
          storageProvider.getJobList(StateName.PROCESSING, new AmountRequest(SORT_BY_UPDATED_AT_DESC, HISTORY_LIMIT)),
          "PROCESSING", jobId, recurringJobNames, firstEntry);
      firstEntry = appendHistoryEntries(sb,
          storageProvider.getJobList(StateName.ENQUEUED, new AmountRequest(SORT_BY_UPDATED_AT_DESC, HISTORY_LIMIT)),
          "ENQUEUED", jobId, recurringJobNames, firstEntry);
      firstEntry = appendHistoryEntries(sb,
          storageProvider.getJobList(StateName.SUCCEEDED, new AmountRequest(SORT_BY_UPDATED_AT_DESC, HISTORY_LIMIT)),
          "SUCCEEDED", jobId, recurringJobNames, firstEntry);
      appendHistoryEntries(sb, storageProvider.getJobList(StateName.FAILED, new AmountRequest(SORT_BY_UPDATED_AT_DESC, HISTORY_LIMIT)),
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
      Optional<String> recurringJobId = job.getRecurringJobId();
      if (recurringJobId.isEmpty()) {
        continue;
      }

      if (shouldIncludeJob(jobIdFilter, recurringJobId.get())) {
        firstEntry = appendHistoryEntry(sb, state, recurringJobId.get(), recurringJobNames, job, firstEntry);
      }
    }

    return firstEntry;
  }

  private boolean shouldIncludeJob(String jobIdFilter, String recurringJobId) {
    return StringUtils.isBlank(jobIdFilter) || jobIdFilter.equals(recurringJobId);
  }

  private boolean appendHistoryEntry(StringBuilder sb, String state, String recurringJobId,
      Map<String, String> recurringJobNames, Job job, boolean firstEntry) {

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

    Optional<Long> processDurationMs = getProcessDurationMs(job, state);
    if (processDurationMs.isPresent()) {
      sb.append(",\"processDurationMs\":").append(processDurationMs.get());
    }

    sb.append("}");
    return false;
  }

  private Optional<Long> getProcessDurationMs(Job job, String state) {
    if ("SUCCEEDED".equals(state)) {
      return job.getLastJobStateOfType(SucceededState.class)
          .map(SucceededState::getProcessDuration)
          .map(Duration::toMillis)
          .filter(ms -> ms >= 0);
    }

    if (!"FAILED".equals(state)) {
      return Optional.empty();
    }

    Optional<ProcessingState> processingState = job.getLastJobStateOfType(ProcessingState.class);
    Optional<FailedState> failedState = job.getLastJobStateOfType(FailedState.class);
    if (processingState.isEmpty() || failedState.isEmpty()) {
      return Optional.empty();
    }

    Instant processingStart = processingState.get().getCreatedAt();
    Instant failedAt = failedState.get().getUpdatedAt();
    if (processingStart == null || failedAt == null || failedAt.isBefore(processingStart)) {
      return Optional.empty();
    }

    return Optional.of(Duration.between(processingStart, failedAt).toMillis());
  }
}
