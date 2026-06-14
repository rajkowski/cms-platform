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

import java.time.Duration;
import java.util.List;

import org.jobrunr.jobs.annotations.Job;

import com.simisinc.platform.application.cms.RefreshWebPageTextCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.distributedlock.LockManager;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.scheduler.SchedulerManager;

import lombok.NoArgsConstructor;

/**
 * Async job for updating all web page text indexes.
 *
 * @author matt rajkowski
 * @created 6/13/26 5:00 PM
 */
@NoArgsConstructor
public class RefreshAllWebPageTextIndexesJob {

  @Job(name = "Refresh all web page text indexes for content")
  public static void execute() {
    // Distributed lock
    String lock = LockManager.lock(SchedulerManager.REFRESH_ALL_WEB_PAGE_TEXT_INDEXES_JOB, Duration.ofMinutes(10));
    if (lock == null) {
      return;
    }
    // Page through all web pages and update text indexes for those that depend on the content
    List<WebPage> allPages = WebPageRepository.findAll();
    allPages.forEach(page -> RefreshWebPageTextCommand.refreshPageText(page));
    // Finish up
    LockManager.unlock(SchedulerManager.REFRESH_ALL_WEB_PAGE_TEXT_INDEXES_JOB, lock);
  }
}
