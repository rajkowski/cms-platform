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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * AJAX endpoint for analytics filter options
 *
 * @author matt rajkowski
 * @created 01/31/26 02:00 PM
 */
public class AnalyticsFiltersOptionsAjax extends GenericWidget {
  static final long serialVersionUID = -8484048371911908896L;
  private static Log LOG = LogFactory.getLog(AnalyticsFiltersOptionsAjax.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public WidgetContext execute(WidgetContext context) {
    try {
      // Build response
      ObjectNode response = MAPPER.createObjectNode();
      response.put("success", true);
      response.put("generatedAt", System.currentTimeMillis());

      // Get available pages for filter options
      List<WebPage> pages = WebPageRepository.findAll();
      var pagesArray = response.putArray("pages");
      if (pages != null) {
        for (WebPage page : pages) {
          var pageObj = pagesArray.addObject();
          pageObj.put("id", page.getId());
          pageObj.put("title", page.getTitle());
          pageObj.put("path", page.getLink());
        }
      }

      // Device options (static)
      var devicesArray = response.putArray("devices");
      devicesArray.add("desktop");
      devicesArray.add("mobile");
      devicesArray.add("tablet");

      // Location options (static - common regions)
      var locationsArray = response.putArray("locations");
      locationsArray.add("North America");
      locationsArray.add("Europe");
      locationsArray.add("Asia");
      locationsArray.add("South America");
      locationsArray.add("Africa");
      locationsArray.add("Oceania");

      // Browser options (static)
      var browsersArray = response.putArray("browsers");
      browsersArray.add("Chrome");
      browsersArray.add("Edge");
      browsersArray.add("Firefox");
      browsersArray.add("Safari");
      browsersArray.add("Other");

      context.setJson(response.toString());
    } catch (Exception e) {
      LOG.error("Error loading analytics filter options", e);
      context.setJson("{\"success\": false, \"error\": \"Error loading filter options\"}");
      context.setSuccess(false);
    }
    return context;
  }
}
