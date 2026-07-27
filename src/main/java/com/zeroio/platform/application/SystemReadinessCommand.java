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

import java.io.File;

import javax.servlet.ServletContext;

import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.infrastructure.database.ConnectionPool;
import com.simisinc.platform.presentation.controller.ContextConstants;

/**
 * Checks if the system is ready for use, including database and file storage availability
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class SystemReadinessCommand {

  public static boolean isSystemReady(ServletContext servletContext) {
    return isStartupSuccessful(servletContext) && isDatabaseReady() && isFileStorageAvailable();
  }

  private static boolean isStartupSuccessful(ServletContext servletContext) {
    // Check if the startup was successful by looking for a specific attribute in the servlet context
    return "true".equals(servletContext.getAttribute(ContextConstants.STARTUP_SUCCESSFUL));
  }

  private static boolean isDatabaseReady() {
    // Database check
    return ConnectionPool.isLive();
  }

  private static boolean isFileStorageAvailable() {
    // Check if the file system is ready and writable
    File fileStorageDirectory = FileSystemCommand.getFileServerRootPath();
    return fileStorageDirectory != null && fileStorageDirectory.isDirectory() && fileStorageDirectory.canWrite();
  }

}
