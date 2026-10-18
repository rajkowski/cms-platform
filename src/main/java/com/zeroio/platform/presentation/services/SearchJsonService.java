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
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.simisinc.platform.infrastructure.persistence.cms.UnifiedSearchRepository;
import com.simisinc.platform.infrastructure.persistence.cms.UnifiedSearchSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.zeroio.platform.domain.model.Region;
import com.zeroio.platform.infrastructure.persistence.RegionRepository;

/**
 * Handles JSON/AJAX GET requests for /json/search endpoint
 * Returns ranked search results across all content types
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

      // Rank pages, posts, events, attachments and items together
      UnifiedSearchSpecification specification = new UnifiedSearchSpecification();
      specification.setSearchTerm(query);
      specification.setForUserId(context.getUserSession().getUserId());
      specification.setContentManager(context.hasRole("admin") || context.hasRole("content-manager"));
      specification.setDataManager(context.hasRole("admin") || context.hasRole("data-manager"));

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

      List<SearchResult> results = UnifiedSearchRepository.findAll(specification, constraints);

      // Build JSON response
      int count = results.size();
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"status\":\"ok\",");
      json.append("\"meta\":{\"type\":\"search\",\"count\":").append(count).append("},");
      json.append("\"data\":[");

      for (int i = 0; i < count; i++) {
        SearchResult result = results.get(i);
        if (i > 0) {
          json.append(",");
        }
        String lastModified = result.getModified() != null ? FormatDateCommand.formatMonthDayYear(result.getModified()) : null;
        String modifiedBy = result.getModifiedBy() > 0 ? UserCommand.name(result.getModifiedBy()) : null;
        String content = result.getHighlight();
        if (content == null) {
          content = result.getPageDescription();
        }

        json.append("{");
        json.append("\"type\":\"").append(JsonCommand.toJson(result.getTypeLabel())).append("\",");
        json.append("\"title\":\"").append(JsonCommand.toJson(result.getPageTitle())).append("\",");
        json.append("\"link\":\"").append(JsonCommand.toJson(result.getLink())).append("\",");
        json.append("\"lastModified\":").append(lastModified != null ? "\"" + JsonCommand.toJson(lastModified) + "\"" : "null")
            .append(",");
        json.append("\"modifiedBy\":").append(modifiedBy != null ? "\"" + JsonCommand.toJson(modifiedBy) + "\"" : "null")
            .append(",");
        json.append("\"content\":").append(content != null ? "\"" + JsonCommand.toJson(content) + "\"" : "null");
        if (result.getActionLink() != null && result.getActionLabel() != null) {
          json.append(",\"actionLink\":\"").append(JsonCommand.toJson(result.getActionLink())).append("\"");
          json.append(",\"actionLabel\":\"").append(JsonCommand.toJson(result.getActionLabel())).append("\"");
        }
        if (result.getCategory() != null) {
          json.append(",\"category\":\"").append(JsonCommand.toJson(result.getCategory())).append("\"");
        }
        if (result.getTags() != null && result.getTags().length > 0) {
          json.append(",\"tags\":[");
          for (int t = 0; t < result.getTags().length; t++) {
            if (t > 0) {
              json.append(",");
            }
            json.append("\"").append(JsonCommand.toJson(result.getTags()[t])).append("\"");
          }
          json.append("]");
        }
        json.append("}");
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
