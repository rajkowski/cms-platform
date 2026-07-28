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
package com.zeroio.platform.application;

import javax.servlet.ServletContext;

import com.simisinc.platform.infrastructure.web.WebApp;
import com.simisinc.platform.presentation.controller.ContextConstants;

/**
 * Checks if the system is live and available
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class SystemLivenessCommand {

  public static boolean isSystemLive(ServletContext servletContext) {
    return isStartupSuccessful(servletContext) && isWebAppAvailable();
  }

  private static boolean isStartupSuccessful(ServletContext servletContext) {
    // Check if the startup was successful by looking for a specific attribute in the servlet context
    return "true".equals(servletContext.getAttribute(ContextConstants.STARTUP_SUCCESSFUL));
  }

  private static boolean isWebAppAvailable() {
    // Check if the web application is available
    return WebApp.isAvailable();
  }
}
