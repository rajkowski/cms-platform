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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Returns a list of available editor templates for the TinyMCE template plugin
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class TemplateListAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908894L;
  protected static Log LOG = LogFactory.getLog(TemplateListAjax.class);

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
    if (!PermissionEngine.checkAccess("cms.template.list", context.getUserSession())) {
      LOG.debug("No permission to: " + TemplateListAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    // Get the template directory using getResourcePaths for robustness
    Set<String> resourcePaths = context.getSession().getServletContext().getResourcePaths("/WEB-INF/editor-templates/");

    List<String> templateFiles = new ArrayList<>();

    if (resourcePaths != null) {
      for (String resourcePath : resourcePaths) {
        // Extract just the filename from the full path
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        // Only include HTML files with valid names
        if (fileName.endsWith(".html") && isValidFileName(fileName)) {
          templateFiles.add(fileName);
        }
      }
    }

    if (templateFiles.isEmpty()) {
      LOG.warn("No template files found in /WEB-INF/editor-templates/");
    }

    // Sort the templates alphabetically
    Collections.sort(templateFiles);

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{\"templates\":[");

    boolean first = true;
    for (String fileName : templateFiles) {
      if (!first) {
        sb.append(",");
      }
      first = false;

      // Extract a display name from the file name
      String displayName = fileName.replace(".html", "").replace("-", " ").replace("_", " ");
      // Capitalize first letter of each word
      String[] words = displayName.split(" ");
      StringBuilder displayNameBuilder = new StringBuilder();
      for (String word : words) {
        if (!word.isEmpty()) {
          if (displayNameBuilder.length() > 0) {
            displayNameBuilder.append(" ");
          }
          displayNameBuilder.append(word.substring(0, 1).toUpperCase());
          if (word.length() > 1) {
            displayNameBuilder.append(word.substring(1));
          }
        }
      }
      displayName = displayNameBuilder.toString();

      sb.append("{");
      sb.append("\"fileName\":\"").append(JsonCommand.toJson(fileName)).append("\",");
      sb.append("\"displayName\":\"").append(JsonCommand.toJson(displayName)).append("\"");
      sb.append("}");
    }
    sb.append("]}");

    LOG.debug("Returning " + templateFiles.size() + " templates");
    context.setJson(sb.toString());
    return context;
  }
}
