/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.infrastructure.scheduler.login;

import java.time.Duration;

import org.jobrunr.jobs.annotations.Job;

import com.simisinc.platform.infrastructure.distributedlock.LockManager;
import com.simisinc.platform.infrastructure.persistence.login.UserTokenRepository;
import com.simisinc.platform.infrastructure.scheduler.SchedulerManager;
import com.simisinc.platform.infrastructure.scheduler.TenantAwareJobRunner;

/**
 * Deletes expired tokens
 *
 * @author matt rajkowski
 * @created 4/26/22 10:39 PM
 */
public class UserTokensCleanupJob {

  @Job(name = "Delete expired tokens")
  public static void execute() {
    // Distributed lock
    String lock = LockManager.lock(SchedulerManager.USER_TOKENS_CLEANUP_JOB, Duration.ofHours(1));
    if (lock == null) {
      return;
    }

    TenantAwareJobRunner.runAllActive(workspace -> UserTokenRepository.deleteOldTokens());
  }
}
