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
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.simisinc.platform.infrastructure.persistence.cms.UnifiedSearchRepository;
import com.simisinc.platform.infrastructure.persistence.cms.UnifiedSearchSpecification;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.Region;
import com.zeroio.platform.domain.model.cms.SearchCriteria;
import com.zeroio.platform.infrastructure.persistence.RegionRepository;

/**
 * Returns the best ranked search results across pages, blog posts, events, attachments and items
 *
 * @author matt rajkowski
 * @created 10/15/26 8:00 AM
 */
public class UnifiedSearchResultsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;

  static String JSP = "/cms/web-page-search-results.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine the search criteria
    SearchCriteria searchCriteria = new SearchCriteria(context.getParameterMap());
    if (!searchCriteria.hasFilters()) {
      return null;
    }
    context.getRequest().setAttribute("searchCriteria", searchCriteria);

    // Check the 'ofType' filter - only show resources when filter is 'pages', 'all', or empty
    String isOfType = Objects.toString(searchCriteria.getOfType(), SearchCriteria.ALL);
    if (!SearchCriteria.ALL.equals(isOfType)) {
      // User has selected a different content type filter (e.g., 'resources')
      return null;
    }

    // Check the 'showWhenOfType' preference
    String showWhenOfType = context.getPreferences().getOrDefault("showWhenOfType", SearchCriteria.ALL);
    if (!showWhenOfType.equals(isOfType)) {
      // Widget is configured to show a different content type than the current search criteria
      return null;
    }

    // Standard request items
    context.getRequest().setAttribute("viewMoreType", SearchCriteria.ALL);
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));
    context.getRequest().setAttribute("showPaging", context.getPreferences().getOrDefault("showPaging", "false"));
    context.getRequest().setAttribute("showViewMoreLink",
        context.getPreferences().getOrDefault("showViewMoreLink", "false"));
    context.getRequest().setAttribute("returnPage", context.getRequest().getRequestURI());

    // Determine the record paging
    int limit = Integer.parseInt(context.getPreferences().getOrDefault("limit", "15"));
    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("items", limit);
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);
    context.getRequest().setAttribute(RequestConstants.RECORD_PAGING, constraints);

    // Spec out the conditions for the query
    UnifiedSearchSpecification specification = new UnifiedSearchSpecification();
    if (!SearchCriteria.ALL.equals(isOfType)) {
      specification.setTypes(new String[] { isOfType });
    } else {
      // Everything but Calendar since it is highlighted separately
      specification.setTypes(new String[] {
          SearchCriteria.PAGES, SearchCriteria.POSTS, SearchCriteria.ATTACHMENTS,
          SearchCriteria.RESOURCES, SearchCriteria.RESOURCE_FILES });
    }
    specification.setForUserId(context.getUserId());
    specification.setContentManager(context.hasRole("admin") || context.hasRole("content-manager"));
    specification.setDataManager(context.hasRole("admin") || context.hasRole("data-manager"));
    if (StringUtils.isNotBlank(searchCriteria.getQuery())) {
      specification.setSearchTerm(searchCriteria.getQuery());
    }
    if (searchCriteria.hasTags()) {
      specification.setFilterTags(searchCriteria.getTags());
    }
    if ("true".equals(context.getPreferences().getOrDefault("useUserRegion", "false"))) {
      String userRegionCode = context.getUserSession().getSelectedRegionCode();
      if (StringUtils.isNotBlank(userRegionCode)) {
        Region region = RegionRepository.findByCode(userRegionCode);
        if (region != null) {
          specification.setRegionTags(region.getValues());
        }
      }
    }
    if (searchCriteria.getFromDate() != null) {
      specification.setModifiedAfter(searchCriteria.getFromDate());
    }
    if (searchCriteria.getToDate() != null) {
      specification.setModifiedBefore(searchCriteria.getToDate());
    }
    if (searchCriteria.hasContributorFilter()) {
      specification.setModifiedByUserIds(searchCriteria.getContributorFilter());
    }

    // Query the data
    List<SearchResult> searchResultList = UnifiedSearchRepository.findAll(specification, constraints);
    LOG.debug("Found " + searchResultList.size() + " unified search results");

    // Determine if the widget is shown
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "true"));
    if (searchResultList.isEmpty() && !showWhenEmpty) {
      return context;
    }

    for (SearchResult searchResult : searchResultList) {
      searchResult.setHtmlExcerpt(toHtmlExcerpt(searchResult.getHighlight()));
    }
    context.getRequest().setAttribute("searchResultList", searchResultList);

    // Show the JSP
    context.setJsp(JSP);
    return context;
  }

  private static String toHtmlExcerpt(String highlight) {
    String htmlContent = HtmlCommand.toHtml(highlight);
    if (StringUtils.isBlank(htmlContent)) {
      return null;
    }
    htmlContent = Strings.CS.replace(htmlContent, "${b}", "<strong>");
    return Strings.CS.replace(htmlContent, "${/b}", "</strong>");
  }
}
