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

package com.simisinc.platform.infrastructure.scheduler.cms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJobRequest;

import com.simisinc.platform.application.cms.RefreshWebPageTextCommand;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Async job for updating web page text indexes when content changes.
 * Triggered when content is saved or published to find dependent pages.
 *
 * @author matt rajkowski
 * @created 6/10/26 2:00 PM
 */
@NoArgsConstructor
public class RefreshWebPageTextIndexJob implements JobRequest {

  private static Log LOG = LogFactory.getLog(RefreshWebPageTextIndexJob.class);

  @Getter
  @Setter
  private String contentUniqueId = null;

  public RefreshWebPageTextIndexJob(String contentUniqueId) {
    this.contentUniqueId = contentUniqueId;
  }

  @Override
  public Class<RefreshWebPageTextIndexJobRequestHandler> getJobRequestHandler() {
    return RefreshWebPageTextIndexJobRequestHandler.class;
  }

  public static void enqueueForContent(String contentUniqueId) {
    if (contentUniqueId == null || contentUniqueId.isEmpty()) {
      return;
    }
    LOG.debug("Enqueuing RefreshWebPageTextIndexJob for contentUniqueId: " + contentUniqueId);
    BackgroundJobRequest.enqueue(new RefreshWebPageTextIndexJob(contentUniqueId));
  }

  public static class RefreshWebPageTextIndexJobRequestHandler implements JobRequestHandler<RefreshWebPageTextIndexJob> {

    @Override
    @Job(name = "Refresh web page text index for content", retries = 1)
    public void run(RefreshWebPageTextIndexJob jobRequest) {
      String contentUniqueId = jobRequest.getContentUniqueId();
      if (contentUniqueId == null || contentUniqueId.isEmpty()) {
        LOG.warn("Cannot refresh pages: contentUniqueId is empty");
        return;
      }

      try {
        LOG.debug("Starting async refresh of pages for content: " + contentUniqueId);

        RefreshWebPageTextCommand.RebuildStats stats = new RefreshWebPageTextCommand.RebuildStats();
        RefreshWebPageTextCommand.refreshPagesByContentUniqueId(contentUniqueId, stats);

        LOG.info("Async refresh complete for content '" + contentUniqueId + "': " + stats);

      } catch (Exception e) {
        LOG.error("Error refreshing pages for content: " + contentUniqueId, e);
        throw e;
      }
    }
  }

}
