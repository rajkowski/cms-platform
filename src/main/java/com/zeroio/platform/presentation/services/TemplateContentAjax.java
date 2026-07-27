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

package com.zeroio.platform.presentation.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Returns the HTML content of a specific editor template
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class TemplateContentAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908895L;
  protected static Log LOG = LogFactory.getLog(TemplateContentAjax.class);

  /**
   * Validates filename to prevent path traversal attacks
   * @param fileName the filename to validate
   * @return true if the filename is valid
   */
  private boolean isValidFileName(String fileName) {
    // Check for path traversal characters and allow only safe characters
    return fileName != null && !fileName.isEmpty() &&
        !fileName.contains("..") &&
        !fileName.contains("/") &&
        !fileName.contains("\\") &&
        fileName.matches("[a-zA-Z0-9_. -]+");
  }

  public JsonServiceContext get(JsonServiceContext context) {

    // Check permissions - user must be able to edit content
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + TemplateContentAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    // Get the template file name parameter
    String fileName = context.getParameter("file");
    if (fileName == null || fileName.isEmpty()) {
      return context.writeError("Template file name is required");
    }

    // Security: Validate filename to prevent path traversal
    if (!isValidFileName(fileName)) {
      LOG.warn("Invalid template file name requested: " + fileName);
      return context.writeError("Invalid template file name");
    }

    // Construct the resource path and load using getResourceAsStream for robustness
    String resourcePath = "/WEB-INF/editor-templates/" + fileName;
    InputStream inputStream = context.getSession().getServletContext().getResourceAsStream(resourcePath);

    if (inputStream == null) {
      LOG.warn("Template file not found: " + resourcePath);
      return context.writeError("Template file not found");
    }

    // Read the template content
    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append("\n");
      }
    } catch (Exception e) {
      LOG.error("Error reading template file: " + resourcePath, e);
      return context.writeError("Error reading template file");
    }

    // Build JSON response with the template HTML content
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"fileName\":\"").append(JsonCommand.toJson(fileName)).append("\",");
    json.append("\"content\":\"").append(JsonCommand.toJson(content.toString())).append("\"");
    json.append("}");

    context.setJson(json.toString());
    return context;
  }
}
