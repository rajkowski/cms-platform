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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.UserCommand;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.FormatDateCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.zeroio.platform.domain.model.Region;
import com.zeroio.platform.infrastructure.persistence.RegionRepository;

/**
 * Handles JSON/AJAX GET requests for /json/search endpoint
 * Returns search results based on query parameters
 *
 * @author matt rajkowski
 * @created 10/13/26 8:00 AM
 */
public class SearchJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(SearchJsonService.class);

  /**
   * Handles GET requests for search
   *
   * @param context the JSON service context
   * @return context with JSON response
   */
  public JsonServiceContext get(JsonServiceContext context) {
    try {

      // Retrieve the search query parameter from the request
      String query = context.getRequest().getParameter("query");

      // Create a specification for querying web pages based on the search term
      WebPageSpecification specification = new WebPageSpecification();
      if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
        // Limit the search to published pages that are enabled, searchable, and not drafts
        specification.setSearchable(true);
        specification.setDraft(false);
      }
      specification.setEnabled(true);
      specification.setHasRedirect(false);
      specification.setSearchTerm(query);

      // Limit to region
      if (LoadSitePropertyCommand.loadByNameAsBoolean("site.regions.enabled")) {
        String userRegionCode = context.getUserSession().getSelectedRegionCode();
        if (StringUtils.isNotBlank(userRegionCode)) {
          Region region = RegionRepository.findByCode(userRegionCode);
          if (region != null) {
            specification.setRegionTags(region.getValues());
          }
        }
      }

      // Set data constraints for the query (pagination)
      DataConstraints constraints = new DataConstraints(0, 20);

      // Execute the query to find all web pages matching the specification and constraints
      List<WebPage> results = WebPageRepository.findAll(specification, constraints);

      // Build JSON response
      int count = (results != null) ? results.size() : 0;
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"status\":\"ok\",");
      json.append("\"meta\":{\"type\":\"search\",\"count\":").append(count).append("},");
      json.append("\"data\":[");

      if (results != null && !results.isEmpty()) {
        for (int i = 0; i < results.size(); i++) {
          WebPage page = results.get(i);
          if (i > 0) {
            json.append(",");
          }
          String lastModified = page.getModified() != null ? FormatDateCommand.formatMonthDayYear(page.getModified()) : null;
          String modifiedBy = page.getModifiedBy() > 0 ? UserCommand.name(page.getModifiedBy()) : null;
          String content = page.getHighlight();
          if (content == null) {
            content = page.getDescription();
          }

          json.append("{");
          json.append("\"type\":\"").append(JsonCommand.toJson("webpage")).append("\",");
          json.append("\"title\":\"").append(JsonCommand.toJson(page.getTitle())).append("\",");
          json.append("\"link\":\"").append(JsonCommand.toJson(page.getLink())).append("\",");
          json.append("\"lastModified\":").append(lastModified != null ? "\"" + JsonCommand.toJson(lastModified) + "\"" : "null")
              .append(",");
          json.append("\"modifiedBy\":").append(modifiedBy != null ? "\"" + JsonCommand.toJson(modifiedBy) + "\"" : "null")
              .append(",");
          json.append("\"content\":").append(content != null ? "\"" + JsonCommand.toJson(content) + "\"" : "null");
          if (page.getTags() != null && page.getTags().length > 0) {
            json.append(",\"tags\":[");
            for (int t = 0; t < page.getTags().length; t++) {
              if (t > 0) {
                json.append(",");
              }
              json.append("\"").append(JsonCommand.toJson(page.getTags()[t])).append("\"");
            }
            json.append("]");
          }
          json.append("}");
        }
      }

      json.append("]}");

      // Set the response
      context.setJson(json.toString());
      return context;

    } catch (Exception e) {
      LOG.error("Error fetching results", e);
      context.setJson("{\"status\":\"error\",\"error\":{\"title\":\"Failed to fetch results\"}}");
      return context;
    }
  }
}
