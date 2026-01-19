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

package com.simisinc.platform.presentation.widgets.cms;

import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Returns a list of content items for the visual page editor
 *
 * @author matt rajkowski
 * @created 1/3/26 10:00 AM
 */
public class ContentListAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  public WidgetContext execute(WidgetContext context) {

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"contentList\":[]}");
      return context;
    }

    // Retrieve all content
    List<Content> contentList = ContentRepository.findAll();

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{\"contentList\":[");

    boolean first = true;
    for (Content content : contentList) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("{");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(content.getUniqueId())).append("\",");
      String contentSnippet = HtmlCommand.text(StringUtils.trimToNull(content.getContent()));
      contentSnippet = StringUtils.abbreviate(contentSnippet, 100);
      sb.append("\"content\":\"").append(JsonCommand.toJson(contentSnippet)).append("\"");
      sb.append("}");
    }
    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }
}
