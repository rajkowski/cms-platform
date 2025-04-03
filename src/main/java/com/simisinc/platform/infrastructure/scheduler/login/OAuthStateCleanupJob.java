/*
 * Copyright 2025 Matt Rajkowski (https://github.com/rajkowski)
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
import com.simisinc.platform.infrastructure.persistence.login.OAuthStateRepository;
import com.simisinc.platform.infrastructure.scheduler.SchedulerManager;

/**
 * Deletes expired oauth values
 *
 * @author matt rajkowski
 * @created 4/3/2025 9:22 AM
 */
public class OAuthStateCleanupJob {

  @Job(name = "Delete expired oauth values")
  public static void execute() {
    // Distributed lock
    String lock = LockManager.lock(SchedulerManager.OAUTH_STATE_CLEANUP_JOB, Duration.ofMinutes(5));
    if (lock == null) {
      return;
    }

    OAuthStateRepository.deleteOldStateValues();
  }
}
