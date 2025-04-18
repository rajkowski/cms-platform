/*
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

package com.simisinc.platform.infrastructure.scheduler.cms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.jobs.annotations.Job;

/**
 * Used for logging health information
 *
 * @author matt rajkowski
 * @created 3/26/2023 7:47 AM
 */
public class SystemHealthJob {

  private static Log LOG = LogFactory.getLog(SystemHealthJob.class);

  @Job(name = "System Health")
  public static void execute() {
    if (LOG.isDebugEnabled()) {
      LOG.info("Healthy");
    }
  }
}
