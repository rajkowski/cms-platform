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

package com.simisinc.platform.infrastructure.scheduler.analytics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.jobs.annotations.Job;

import com.simisinc.platform.application.analytics.SavePerformanceMetricCommand;
import com.simisinc.platform.domain.model.analytics.PerformanceMetric;
import com.simisinc.platform.infrastructure.persistence.analytics.PerformanceMetricRepository;

/**
 * Drains the performance metric queue and persists records to the database
 *
 * @author matt rajkowski
 * @created 02/27/26 09:00 AM
 */
public class RecordPerformanceMetricJob {

  private static Log LOG = LogFactory.getLog(RecordPerformanceMetricJob.class);

  @Job(name = "Record performance metrics")
  public static void execute() {
    PerformanceMetric metric = null;
    int count = 0;
    while ((metric = SavePerformanceMetricCommand.getMetricFromQueue()) != null) {
      PerformanceMetricRepository.save(metric);
      ++count;
    }
    if (count > 0) {
      LOG.debug("Performance metrics processed: " + count);
    }
  }
}
