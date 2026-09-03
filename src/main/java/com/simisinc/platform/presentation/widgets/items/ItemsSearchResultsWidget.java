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

package com.simisinc.platform.presentation.widgets.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemSpecification;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.Region;
import com.zeroio.platform.domain.model.cms.SearchCriteria;
import com.zeroio.platform.infrastructure.persistence.RegionRepository;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 3/27/18 4:27 PM
 */
public class ItemsSearchResultsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/items/items-integrated-search-results-list.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine the search criteria
    SearchCriteria searchCriteria = new SearchCriteria(context.getParameterMap());
    String location = context.getParameter("location");
    if (!searchCriteria.hasFilters() && StringUtils.isBlank(location)) {
      return null;
    }
    context.getRequest().setAttribute("searchCriteria", searchCriteria);

    // Check the 'ofType' filter - only show resources when filter is 'resources', 'all', or empty
    String isOfType = Objects.toString(searchCriteria.getOfType(), SearchCriteria.ALL);
    if (!SearchCriteria.ALL.equals(isOfType) && !SearchCriteria.RESOURCES.equals(isOfType)) {
      // User has selected a different content type filter (e.g., 'pages')
      return null;
    }

    // Check the 'showWhenOfType' preference
    String showWhenOfType = context.getPreferences().getOrDefault("showWhenOfType", SearchCriteria.ALL);
    if (!showWhenOfType.equals(isOfType)) {
      // Widget is configured to show a different content type than the current search criteria
      return null;
    }

    // View More takes over the 'ofType' for paging to use
    context.getRequest().setAttribute("viewMoreType", SearchCriteria.RESOURCES);

    boolean useUserRegionPref = "true".equals(context.getPreferences().getOrDefault("useUserRegion", "false"));

    // Determine the record paging
    int limit = Integer.parseInt(context.getPreferences().getOrDefault("limit", "15"));
    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("items", limit);
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);
    String sortBy = context.getPreferences().get("sortBy");
    if ("new".equals(sortBy)) {
      constraints.setColumnToSortBy("created", "desc");
    }
    context.getRequest().setAttribute(RequestConstants.RECORD_PAGING, constraints);

    // Determine criteria
    ItemSpecification specification = new ItemSpecification();
    specification.setForUserId(context.getUserId());
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      specification.setApprovedOnly(true);
    }
    if (StringUtils.isNotBlank(searchCriteria.getQuery())) {
      specification.setSearchName(searchCriteria.getQuery());
    }
    if (StringUtils.isNotBlank(location)) {
      specification.setSearchLocation(location);
      specification.setWithinMeters(48281);
    }
    if (searchCriteria.hasTags()) {
      specification.setFilterTags(searchCriteria.getTags());
    }
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

    // Determine how the view will show the item's link
    boolean useItemLink = "true".equals(context.getPreferences().getOrDefault("useItemLink", "false"));

    // Query the data
    List<Item> itemList = ItemRepository.findAll(specification, constraints);
    if (itemList == null || itemList.isEmpty()) {
      boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "true"));
      if (!showWhenEmpty) {
        return context;
      }
    }
    context.getRequest().setAttribute("itemList", itemList);

    List<SearchResult> searchResultList = new ArrayList<>();
    for (Item item : itemList) {
      // Add the search result
      SearchResult searchResult = new SearchResult();
      searchResult.setPageTitle(item.getName());
      if (useItemLink && StringUtils.isNotBlank(item.getUrl())
          && (item.getUrl().startsWith("http://") || item.getUrl().startsWith("https://"))) {
        searchResult.setLink(item.getUrl());
      } else {
        searchResult.setLink(context.getContextPath() + "/show/" + item.getUniqueId());
      }
      if (StringUtils.isNotBlank(item.getSummary())) {
        searchResult.setPageDescription(item.getSummary());
      }
      searchResult.setTags(item.getTags());
      // Include an excerpt
      String htmlContent = HtmlCommand.toHtml(item.getHighlight());
      if (StringUtils.isNotBlank(htmlContent)) {
        htmlContent = Strings.CS.replace(htmlContent, "${b}", "<strong>");
        htmlContent = Strings.CS.replace(htmlContent, "${/b}", "</strong>");
        searchResult.setHtmlExcerpt(htmlContent);
      }
      searchResultList.add(searchResult);
    }
    context.getRequest().setAttribute("searchResultList", searchResultList);

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));
    context.getRequest().setAttribute("showPaging", context.getPreferences().getOrDefault("showPaging", "false"));
    context.getRequest().setAttribute("showViewMoreLink",
        context.getPreferences().getOrDefault("showViewMoreLink", "false"));
    context.getRequest().setAttribute("returnPage", context.getRequest().getRequestURI());

    // Show the JSP
    context.setJsp(JSP);
    return context;
  }
}
