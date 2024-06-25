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

import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jobrunr.jobs.annotations.Job;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JavaxServletWebApplication;

import com.simisinc.platform.application.cms.MakeStaticSiteCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
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
public class MakeStaticSiteJob {

  private static Log LOG = LogFactory.getLog(MakeStaticSiteJob.class);

  @Job(name = "Make a static site")
  public static void execute() {
    ServletContext servletContext = SchedulerManager.getServletContext();
    JavaxServletWebApplication application = JavaxServletWebApplication.buildApplication(servletContext);
    WebApplicationTemplateResolver templateResolver = new WebApplicationTemplateResolver(application);
    try {
      Map<String, String> widgetLibrary = WebPageXmlLayoutCommand.init(servletContext);
      PageTemplateEngine.startup(templateResolver, "/WEB-INF/html-templates/", widgetLibrary);
    } catch (Exception e) {
      LOG.error("Exiting, the PageTemplateEngine did not properly startup so web requests will not be allowed!", e);
    }

    Properties templateEngineProperties = new Properties();
    templateEngineProperties.setProperty("webAppPath", servletContext.getRealPath("/"));

    try {
      MakeStaticSiteCommand.execute(templateEngineProperties);
    } catch (Exception e) {
      LOG.error("Error occurred when making the static site", e);
    }
  }
}
