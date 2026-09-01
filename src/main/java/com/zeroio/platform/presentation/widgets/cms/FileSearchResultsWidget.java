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

package com.zeroio.platform.presentation.widgets.cms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileSpecification;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.Region;
import com.zeroio.platform.domain.model.cms.SearchCriteria;
import com.zeroio.platform.infrastructure.persistence.RegionRepository;

/**
 * Returns search results for file attachments
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class FileSearchResultsWidget extends GenericWidget {

  private static Log LOG = LogFactory.getLog(FileSearchResultsWidget.class);

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/cms/web-page-search-results.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine the search criteria
    SearchCriteria searchCriteria = new SearchCriteria(context.getParameterMap());
    if (!searchCriteria.hasFilters()) {
      return null;
    }
    context.getRequest().setAttribute("searchCriteria", searchCriteria);

    // Check the 'ofType' filter - only show attachments when filter is 'attachments', 'all', or empty
    String isOfType = Objects.toString(searchCriteria.getOfType(), SearchCriteria.ALL);
    if (!SearchCriteria.ALL.equals(isOfType) && !SearchCriteria.ATTACHMENTS.equals(isOfType)) {
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
    context.getRequest().setAttribute("viewMoreType", SearchCriteria.ATTACHMENTS);

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    boolean useUserRegionPref = "true".equals(context.getPreferences().getOrDefault("useUserRegion", "false"));

    // Determine the record paging
    int limit = Integer.parseInt(context.getPreferences().getOrDefault("limit", "15"));
    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("items", limit);
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);
    context.getRequest().setAttribute(RequestConstants.RECORD_PAGING, constraints);

    // Determine criteria
    FileSpecification specification = new FileSpecification();
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      specification.setForUserId(context.getUserId());
    }
    if (StringUtils.isNotBlank(searchCriteria.getQuery())) {
      specification.setSearchName(searchCriteria.getQuery());
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

    // Query the data
    List<FileItem> fileItemList = FileItemRepository.findAll(specification, constraints);
    LOG.debug("Initial search returned " + (fileItemList != null ? fileItemList.size() : 0) + " files");

    // Fallback to a direct title match for terms that might not be tokenized as expected by tsquery
    if ((fileItemList == null || fileItemList.isEmpty()) && StringUtils.isNotBlank(searchCriteria.getQuery())) {
      // Try a direct match on the name
      String query = searchCriteria.getQuery();
      specification.setMatchesName(query);
      specification.setSearchName(null);
      fileItemList = FileItemRepository.findAll(specification, constraints);
      if ((fileItemList == null || fileItemList.isEmpty()) && query.contains(".")) {
        // If the query contains a dot, try matching on the base name without the extension
        String baseName = StringUtils.substringBefore(query, ".");
        if (StringUtils.isNotBlank(baseName)) {
          specification.setMatchesName(baseName);
          fileItemList = FileItemRepository.findAll(specification, constraints);
        }
      }
    }

    List<SearchResult> searchResultList = new ArrayList<>();
    Map<Long, SearchResult> deduplicatedResults = new LinkedHashMap<>();
    if (fileItemList != null) {
      for (FileItem fileItem : fileItemList) {
        if (fileItem == null || deduplicatedResults.containsKey(fileItem.getId())) {
          continue;
        }
        SearchResult searchResult = new SearchResult();
        searchResult.setPageTitle(StringUtils.defaultIfBlank(fileItem.getTitle(), fileItem.getFilename()));
        searchResult.setPageDescription(fileItem.getSummary());
        boolean isDiagram = "drawio".equalsIgnoreCase(fileItem.getExtension())
            || "diagram".equalsIgnoreCase(fileItem.getFileType());
        boolean isViewable = isDiagram || isInlineViewable(fileItem);
        if (isDiagram) {
          searchResult.setLink("/assets/drawio/" + fileItem.getUrl());
          searchResult.setTitleLinkEnabled(true);
          searchResult.setActionLabel("View attachment");
          searchResult.setActionLink("/assets/drawio/" + fileItem.getUrl());
        } else if (isViewable) {
          searchResult.setLink("/assets/view/" + fileItem.getUrl());
          searchResult.setTitleLinkEnabled(true);
          searchResult.setActionLabel("View attachment");
          searchResult.setActionLink("/assets/view/" + fileItem.getUrl());
        } else {
          searchResult.setLink("/assets/file/" + fileItem.getUrl());
          searchResult.setTitleLinkEnabled(false);
          searchResult.setActionLabel("Download attachment");
          searchResult.setActionLink("/assets/file/" + fileItem.getUrl());
        }
        searchResult.setTags(fileItem.getTags());
        searchResult.setModified(fileItem.getModified());
        searchResult.setModifiedBy(fileItem.getModifiedBy());
        deduplicatedResults.put(fileItem.getId(), searchResult);
      }
    }
    searchResultList.addAll(deduplicatedResults.values());
    LOG.debug("Final searchResultList size: " + searchResultList.size());

    // Determine if the widget is shown
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "true"));
    if (searchResultList.isEmpty() && !showWhenEmpty) {
      LOG.debug("No results and showWhenEmpty=false, returning context without JSP");
      return context;
    }

    LOG.debug("Setting searchResultList with " + searchResultList.size() + " results and rendering JSP");
    context.getRequest().setAttribute("searchResultList", searchResultList);
    context.getRequest().setAttribute("showPaging", context.getPreferences().getOrDefault("showPaging", "false"));
    context.getRequest().setAttribute("showViewMoreLink",
        context.getPreferences().getOrDefault("showViewMoreLink", "false"));
    context.getRequest().setAttribute("returnPage", context.getRequest().getRequestURI());

    // Show the JSP
    context.setJsp(JSP);
    return context;
  }

  private boolean isInlineViewable(FileItem fileItem) {
    if (fileItem == null) {
      return false;
    }
    String mimeType = StringUtils.defaultString(fileItem.getMimeType());
    if (mimeType.startsWith("image/") || mimeType.startsWith("video/")
        || "application/pdf".equalsIgnoreCase(mimeType)) {
      return true;
    }
    String fileType = StringUtils.defaultString(fileItem.getFileType());
    return "image".equalsIgnoreCase(fileType) || "video".equalsIgnoreCase(fileType) || "pdf".equalsIgnoreCase(fileType);
  }
}