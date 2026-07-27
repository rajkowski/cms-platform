/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageSpecification;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.Region;
import com.zeroio.platform.domain.model.cms.SearchCriteria;
import com.zeroio.platform.infrastructure.persistence.RegionRepository;

/**
 * Returns search results for web pages
 *
 * @author matt rajkowski
 * @created 8/28/19 12:15 PM
 */
public class WebPageSearchResultsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/cms/web-page-search-results.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine the search criteria
    SearchCriteria searchCriteria = new SearchCriteria(context.getParameterMap());
    String location = context.getParameter("location");
    if (!searchCriteria.hasFilters() && StringUtils.isBlank(location)) {
      return null;
    }
    context.getRequest().setAttribute("searchCriteria", searchCriteria);

    // Check the 'ofType' filter - only show resources when filter is 'pages', 'all', or empty
    String isOfType = Objects.toString(searchCriteria.getOfType(), SearchCriteria.ALL);
    if (!SearchCriteria.ALL.equals(isOfType) && !SearchCriteria.PAGES.equals(isOfType)) {
      // User has selected a different content type filter (e.g., 'resources')
      return null;
    }

    // Check the 'showWhenOfType' preference
    String showWhenOfType = context.getPreferences().getOrDefault("showWhenOfType", SearchCriteria.ALL);
    if (!showWhenOfType.equals(isOfType)) {
      // Widget is configured to show a different content type than the current search criteria
      return null;
    }

    // View More takes over the 'ofType' for paging to use
    context.getRequest().setAttribute("viewMoreType", SearchCriteria.PAGES);

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Get widget preferences with defaults
    context.getRequest().setAttribute("showPaging", context.getPreferences().getOrDefault("showPaging", "false"));
    context.getRequest().setAttribute("showViewMoreLink",
        context.getPreferences().getOrDefault("showViewMoreLink", "false"));
    boolean useUserRegionPref = "true".equals(context.getPreferences().getOrDefault("useUserRegion", "false"));

    // Determine the record paging
    int limit = Integer.parseInt(context.getPreferences().getOrDefault("limit", "15"));
    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("items", limit);
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);
    context.getRequest().setAttribute(RequestConstants.RECORD_PAGING, constraints);

    // Spec out the conditions for the query
    WebPageSpecification specification = new WebPageSpecification();
    // Admins or content managers can see all pages, but regular users only see published pages that are searchable, and not drafts
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      // Limit the search to published pages that are enabled, searchable, and not drafts
      specification.setSearchable(true);
      specification.setDraft(false);
    }
    specification.setEnabled(true);
    specification.setHasRedirect(false);
    // Query term
    if (StringUtils.isNotBlank(searchCriteria.getQuery())) {
      specification.setSearchTerm(searchCriteria.getQuery());
    }
    if (searchCriteria.hasTags()) {
      specification.setFilterTags(searchCriteria.getTags());
    }

    // Check if we should include the user's region preferences as tags to filter by
    if (useUserRegionPref) {
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
    List<WebPage> webPageList = WebPageRepository.findAll(specification, constraints);
    LOG.debug("Found " + (webPageList != null ? webPageList.size() : 0) + " web pages matching filters");

    // Prepare the response
    Map<String, SearchResult> resultsMap = new LinkedHashMap<>();
    if (webPageList == null || webPageList.isEmpty()) {
      // No pages match the filters, return empty results
      LOG.debug("No web pages found matching filter criteria");
      return finishRequest(context, resultsMap);
    }

    // Return the results...
    for (WebPage webPage : webPageList) {
      String link = webPage.getLink();
      // Skip blank links
      if (StringUtils.isBlank(link)) {
        LOG.debug("Skipping web page with blank link - ID: " + webPage.getId() + ", Title: " + webPage.getTitle());
        continue;
      }
      addTheSearchResult(webPage, link, resultsMap);
    }
    context.getRequest().setAttribute("searchResultList", resultsMap.values());
    return finishRequest(context, resultsMap);
  }

  private void addTheSearchResult(WebPage webPage, String link, Map<String, SearchResult> resultsMap) {

    String htmlContent = HtmlCommand.toHtml(webPage.getHighlight());
    if (StringUtils.isBlank(htmlContent)) {
      LOG.debug("No highlight content for web page with link: " + link + " - skipping search result");
      return;
    }

    // Highlight is available, format it
    htmlContent = Strings.CS.replace(htmlContent, "${b}", "<strong>");
    htmlContent = Strings.CS.replace(htmlContent, "${/b}", "</strong>");
    SearchResult searchResult = new SearchResult();
    searchResult.setLink(link);
    if ("/".equals(link)) {
      // It's the home page
      searchResult.setPageTitle(LoadSitePropertyCommand.loadByName("site.name"));
    } else {
      searchResult.setPageTitle(webPage.getTitle());
    }
    if (StringUtils.isNotBlank(webPage.getDescription())) {
      searchResult.setPageDescription(webPage.getDescription());
    }
    searchResult.setHtmlExcerpt(htmlContent);
    searchResult.setTags(webPage.getTags());
    // Set modified date and modified by user
    searchResult.setModified(webPage.getModified());
    searchResult.setModifiedBy(webPage.getModifiedBy());
    resultsMap.put(link, searchResult);
  }

  private WidgetContext finishRequest(WidgetContext context, Map<String, SearchResult> resultsMap) {
    // Determine if the widget is shown
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "true"));
    if (resultsMap.isEmpty() && !showWhenEmpty) {
      LOG.debug("No results found and showWhenEmpty is false - not rendering widget");
      return context;
    }

    // Determine the view
    context.setJsp(JSP);
    return context;
  }
}
