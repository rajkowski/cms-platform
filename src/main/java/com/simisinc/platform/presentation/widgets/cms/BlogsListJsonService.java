/*
 * Copyright 2026 Matt Rajkowski
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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Blog;
import com.simisinc.platform.infrastructure.persistence.cms.BlogRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Handles JSON/AJAX GET requests for /json/blogs/list endpoint
 * Returns a list of all blogs
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class BlogsListJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(BlogsListJsonService.class);

  /**
   * Handles GET requests for blog list
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public JsonServiceContext get(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + BlogsListJsonService.class.getSimpleName());
      return context.writeError("Permission denied");
    }

    try {
      // Load all blogs
      List<Blog> blogs = BlogRepository.findAll();

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("[");

      if (blogs != null && !blogs.isEmpty()) {
        boolean first = true;
        for (Blog blog : blogs) {
          if (!first) {
            json.append(",");
          }
          first = false;

          json.append("{");
          json.append("\"id\": ").append(blog.getId()).append(",");
          json.append("\"title\": \"").append(JsonCommand.toJson(blog.getName())).append("\",");
          json.append("\"description\": \"").append(JsonCommand.toJson(StringUtils.defaultString(blog.getDescription()))).append("\"");
          json.append("}");
        }
      }

      json.append("]");

      return context.writeOk(json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error loading blogs: " + e.getMessage(), e);
      return context.writeError(e.getMessage());
    }
  }

}
