/*
 * Copyright 2024 Matt Rajkowski (https://www.github.com/rajkowski)
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

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JavaxServletWebApplication;

import com.simisinc.platform.application.cms.MakeStaticSiteCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.infrastructure.distributedlock.LockManager;
import com.simisinc.platform.infrastructure.scheduler.SchedulerManager;
import com.simisinc.platform.presentation.controller.PageTemplateEngine;

import lombok.NoArgsConstructor;

/**
 * Makes a static site by outputting all content to the static-site path
 *
 * @author matt rajkowski
 * @created 6/22/2024 3:06 PM
 */
@NoArgsConstructor
public class MakeStaticSiteJob implements JobRequest {

  private static Log LOG = LogFactory.getLog(MakeStaticSiteJob.class);

  @Override
  public Class<MakeStaticSiteJobRequestHandler> getJobRequestHandler() {
    return MakeStaticSiteJobRequestHandler.class;
  }

  public static class MakeStaticSiteJobRequestHandler implements JobRequestHandler<MakeStaticSiteJob> {
    @Override
    @Job(name = "Make a static site")
    public void run(MakeStaticSiteJob jobRequest) {
      jobContext().saveMetadata("name", "Make Static Site");
      execute();
    }
  }

  public static void execute() {
    // Distributed lock
    String lock = LockManager.lock(SchedulerManager.STATIC_SITE_GENERATOR_JOB, Duration.ofMinutes(5));
    if (lock == null) {
      return;
    }

    try {
      // Prepare the template engine
      ServletContext servletContext = SchedulerManager.getServletContext();
      JavaxServletWebApplication application = JavaxServletWebApplication.buildApplication(servletContext);
      WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(application);
      Map<String, String> widgetLibrary = WebPageXmlLayoutCommand.init(servletContext);
      URL webPackageFile = servletContext.getResource("/WEB-INF/dependencies.json");

      PageTemplateEngine.startup(templateResolver, "/WEB-INF/html-templates/", widgetLibrary, webPackageFile);
      Properties templateEngineProperties = new Properties();
      templateEngineProperties.setProperty("webAppPath", servletContext.getRealPath("/"));

      // Export the static site
      String filePath = MakeStaticSiteCommand.execute(templateEngineProperties);
      if (filePath != null) {
        LOG.info("Static site generated at: " + filePath);
      } else {
        LOG.error("Static site generation failed.");
      }
    } catch (Exception e) {
      LOG.error("Error occurred when making the static site", e);
    } finally {
      LockManager.unlock(SchedulerManager.STATIC_SITE_GENERATOR_JOB, lock);
    }
  }
}
