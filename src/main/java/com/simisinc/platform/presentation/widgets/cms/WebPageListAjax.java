/*
 * Copyright 2025 Matt Rajkowski
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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Returns a list of web pages for the visual page editor
 * Only returns pages the user has permission to edit
 *
 * @author matt rajkowski
 * @created 12/14/25 10:00 AM
 */
public class WebPageListAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  public WidgetContext execute(WidgetContext context) {

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-editor")) {
      context.setJson("[]");
      return context;
    }

    // Retrieve all enabled web pages
    WebPageSpecification specification = new WebPageSpecification();
    // specification.setEnabled(true);

    List<WebPage> pageList = WebPageRepository.findAll(specification, null);

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    for (WebPage page : pageList) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append("{");
      sb.append("\"id\":").append(page.getId()).append(",");
      sb.append("\"link\":\"").append(JsonCommand.toJson(page.getLink())).append("\",");
      String pageTitle = StringUtils.isNotBlank(page.getTitle()) ? page.getTitle() : page.getLink();
      sb.append("\"title\":\"").append(JsonCommand.toJson(pageTitle)).append("\"");
      sb.append("}");
    }

    context.setJson("[" + sb.toString() + "]");
    return context;
  }
}
