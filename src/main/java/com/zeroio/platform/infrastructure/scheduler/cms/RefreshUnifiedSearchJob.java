/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zeroio.platform.infrastructure.scheduler.cms;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.jobs.annotations.Job;

import com.simisinc.platform.infrastructure.distributedlock.LockManager;
import com.simisinc.platform.infrastructure.persistence.cms.UnifiedSearchRepository;
import com.simisinc.platform.infrastructure.scheduler.SchedulerManager;
import com.zeroio.platform.infrastructure.scheduler.TenantAwareJobRunner;

import lombok.NoArgsConstructor;

/**
 * Refreshes the materialized unified search index
 *
 * @author matt rajkowski
 * @created 11/10/26 9:00 AM
 */
@NoArgsConstructor
public class RefreshUnifiedSearchJob {

  private static Log LOG = LogFactory.getLog(RefreshUnifiedSearchJob.class);

  @Job(name = "Refresh unified search index")
  public static void execute() {
    TenantAwareJobRunner.runDefaultAndAllActive(RefreshUnifiedSearchJob::executeForTenant);
  }

  private static void executeForTenant() {
    String lock = LockManager.lock(SchedulerManager.REFRESH_UNIFIED_SEARCH_JOB, Duration.ofMinutes(10));
    if (lock == null) {
      return;
    }
    try {
      if (UnifiedSearchRepository.refreshIfNeeded()) {
        LOG.debug("Refreshed unified search");
      }
    } catch (Exception e) {
      LOG.warn("Error refreshing unified search: " + e.getMessage(), e);
    } finally {
      LockManager.unlock(SchedulerManager.REFRESH_UNIFIED_SEARCH_JOB, lock);
    }
  }
}
