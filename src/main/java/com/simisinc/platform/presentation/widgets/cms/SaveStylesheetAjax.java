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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Stylesheet;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.StylesheetRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Saves a web page's stylesheet/CSS from the visual page editor CSS tab
 * Only allows saving stylesheets for pages the user has permission to edit
 *
 * @author matt rajkowski
 * @created 1/10/26 10:00 AM
 */
public class SaveStylesheetAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908897L;
  private static Log LOG = LogFactory.getLog(SaveStylesheetAjax.class);

  public JsonServiceContext post(JsonServiceContext context) {

    LOG.debug("SaveStylesheetAjax...");

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + SaveStylesheetAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    // Determine the page link
    String webPageLink = context.getParameter("link");
    LOG.debug("Saving stylesheet for: " + webPageLink);
    if (StringUtils.isBlank(webPageLink)) {
      LOG.debug("Page link is empty");
      return context.writeError("Link is required");
    }

    // Retrieve the existing web page
    WebPage page = WebPageRepository.findByLink(webPageLink);
    if (page == null) {
      LOG.debug("Web page not found for link: " + webPageLink);
      return context.writeError("Page not found");
    }

    // Get the CSS content from the request
    String css = context.getParameter("css");
    LOG.debug("CSS content length: " + (css != null ? css.length() : 0));

    try {
      // Check if a stylesheet already exists for this page
      Stylesheet stylesheet = StylesheetRepository.findByWebPageId(page.getId());

      if (stylesheet == null) {
        // Create a new stylesheet
        stylesheet = new Stylesheet();
        stylesheet.setWebPageId(page.getId());
      }

      // Update the CSS content
      stylesheet.setCss(StringUtils.trimToNull(css));

      // Save the stylesheet
      Stylesheet savedStylesheet = StylesheetRepository.save(stylesheet);

      if (savedStylesheet != null) {
        // Build success response
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"message\":\"Stylesheet saved successfully\",");
        sb.append("\"id\":").append(savedStylesheet.getId()).append(",");
        sb.append("\"webPageId\":").append(page.getId()).append(",");
        sb.append("\"link\":\"").append(JsonCommand.toJson(page.getLink())).append("\"");
        sb.append("}");
        context.setJson(sb.toString());
        LOG.debug("Stylesheet saved successfully for page: " + webPageLink);
        return context;
      } else {
        // If CSS was blank and stylesheet was removed, that's still a success
        if (StringUtils.isBlank(css)) {
          StringBuilder sb = new StringBuilder();
          sb.append("{");
          sb.append("\"success\":true,");
          sb.append("\"message\":\"Stylesheet removed successfully\",");
          sb.append("\"id\":-1,");
          sb.append("\"webPageId\":").append(page.getId()).append(",");
          sb.append("\"link\":\"").append(JsonCommand.toJson(page.getLink())).append("\"");
          sb.append("}");
          context.setJson(sb.toString());
          LOG.debug("Stylesheet removed for page: " + webPageLink);
          return context;
        } else {
          return context.writeError("Failed to save stylesheet");
        }
      }
    } catch (Exception e) {
      LOG.error("Error saving stylesheet: " + e.getMessage(), e);
      return context.writeError("Error saving stylesheet: " + e.getMessage());
    }
  }
}
