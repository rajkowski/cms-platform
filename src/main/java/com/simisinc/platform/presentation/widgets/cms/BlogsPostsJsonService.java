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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.BlogPost;
import com.simisinc.platform.infrastructure.persistence.cms.BlogPostRepository;
import com.simisinc.platform.infrastructure.persistence.cms.BlogPostSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX GET requests for /json/blogs/posts endpoint
 * Returns blog posts for selected blogs
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class BlogsPostsJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(BlogsPostsJsonService.class);

  /**
   * Handles GET requests for blog posts
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext execute(WidgetContext context) {

    // Check permissions - require admin or content-manager role
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      // Return empty array on permission denied for FullCalendar
      context.setJson("[]");
      return context;
    }

    try {
      // Get comma-separated blog IDs
      String blogIdsParam = context.getParameter("blogIds");
      
      if (StringUtils.isBlank(blogIdsParam)) {
        // Return empty array for FullCalendar
        context.setJson("[]");
        return context;
      }

      // Parse blog IDs
      String[] blogIdStrings = blogIdsParam.split(",");
      long[] blogIds = new long[blogIdStrings.length];
      for (int i = 0; i < blogIdStrings.length; i++) {
        try {
          blogIds[i] = Long.parseLong(blogIdStrings[i].trim());
        } catch (NumberFormatException e) {
          // Skip invalid IDs
        }
      }

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("[");

      boolean first = true;
      for (long blogId : blogIds) {
        if (blogId <= 0) continue;
        
        // Load blog posts for this blog using BlogPostSpecification
        BlogPostSpecification spec = new BlogPostSpecification();
        spec.setBlogId(blogId);
        List<BlogPost> posts = BlogPostRepository.findAll(spec, null);
        
        if (posts != null && !posts.isEmpty()) {
          for (BlogPost post : posts) {
            if (!first) {
              json.append(",");
            }
            first = false;

            json.append("{");
            json.append("\"id\": ").append(post.getId()).append(",");
            json.append("\"title\": \"").append(JsonCommand.toJson(post.getTitle())).append("\",");
            json.append("\"blogId\": ").append(blogId).append(",");
            
            if (post.getPublished() != null) {
              json.append("\"publishDate\": \"").append(JsonCommand.toJson(post.getPublished().toString())).append("\",");
            } else if (post.getStartDate() != null) {
              json.append("\"publishDate\": \"").append(JsonCommand.toJson(post.getStartDate().toString())).append("\",");
            } else {
              json.append("\"publishDate\": \"\",");
            }
            if (post.getCreated() != null) {
              json.append("\"created\": \"").append(JsonCommand.toJson(post.getCreated().toString())).append("\",");
            }
            json.append("\"content\": \"").append(JsonCommand.toJson(StringUtils.defaultString(post.getBody()))).append("\"");
            
            json.append("}");
          }
        }
      }

      json.append("]");

      // Return raw JSON array for FullCalendar compatibility
      context.setJson(json.toString());
      return context;

    } catch (Exception e) {
      LOG.error("Error loading blog posts: " + e.getMessage(), e);
      // Return empty array on error for FullCalendar
      context.setJson("[]");
      return context;
    }
  }

}
