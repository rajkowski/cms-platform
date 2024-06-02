/*
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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.items.CollectionTableColumnsCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.items.CategoryRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemSpecification;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Display a list of items using a specified layout
 *
 * @author matt rajkowski
 * @created 4/20/18 2:23 PM
 */
public class ItemsListWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/items/items-list.jsp";
  static String CARD_VIEW_JSP = "/items/items-card-view.jsp";
  static String CATEGORY_CARD_VIEW_JSP = "/items/items-category-card-view.jsp";
  static String TABLE_VIEW_JSP = "/items/items-table.jsp";
  static String JOBS_LIST_JSP = "/items/items-jobs-list.jsp";
  static String SEARCH_RESULTS_JSP = "/items/items-search-results-list.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine preferences
    String collectionUniqueId = context.getPreferences().get("collectionUniqueId");
    long categoryId = context.getParameterAsLong("categoryId");
    String categoryName = context.getPreferences().get("category");
    String nearbyItemUniqueId = context.getPreferences().get("nearbyItemUniqueId");
    boolean showMine = "true".equals(context.getPreferences().getOrDefault("showMine", "false"));
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "false"));

    // Determine the view
    String jsp = JSP;
    String view = context.getPreferences().get("view");
    if ("cards".equals(view)) {
      jsp = CARD_VIEW_JSP;
    } else if ("category-cards".equals(view)) {
      jsp = CATEGORY_CARD_VIEW_JSP;
    } else if ("table".equals(view)) {
      jsp = TABLE_VIEW_JSP;
    } else if ("jobs".equals(view)) {
      jsp = JOBS_LIST_JSP;
    }

    // @todo Consider using a cache for general users

    // Determine the collection
    Collection collection = LoadCollectionCommand.loadCollectionByUniqueIdForAuthorizedUser(collectionUniqueId, context.getUserId());
    if (collection == null) {
      LOG.warn("Set a collection or collectionUniqueId preference, or user does not have access");
      return null;
    }
    context.getRequest().setAttribute("collection", collection);

    // Determine the record paging
    int limit = Integer.parseInt(context.getPreferences().getOrDefault("limit", "20"));
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
    specification.setCollectionId(collection.getId());
    if (showMine) {
      specification.setForMemberWithUserId(context.getUserId());
    } else {
      specification.setForUserId(context.getUserId());
    }
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      specification.setApprovedOnly(true);
    }

    // Determine the category
    if (StringUtils.isNotBlank(categoryName)) {
      Category category = CategoryRepository.findByNameWithinCollection(categoryName, collection.getId());
      if (category != null && category.getCollectionId() == collection.getId()) {
        specification.setCategoryId(category.getId());
        context.getRequest().setAttribute("category", category);
      } else {
        return null;
      }
    } else if (categoryId > -1L) {
      Category category = CategoryRepository.findById(categoryId);
      if (category != null && category.getCollectionId() == collection.getId()) {
        specification.setCategoryId(category.getId());
        context.getRequest().setAttribute("category", category);
      } else {
        return null;
      }
    }

    // Check shared request values for search criteria
    String searchName = context.getSharedRequestValue("searchName");
    if (searchName == null) {
      searchName = context.getParameter("searchName");
    }
    if (StringUtils.isNotBlank(searchName)) {
      specification.setSearchName(searchName);
    }
    String searchLocation = context.getSharedRequestValue("searchLocation");
    if (searchLocation == null) {
      searchLocation = context.getParameter("searchLocation");
    }
    if (StringUtils.isNotBlank(searchLocation)) {
      specification.setSearchLocation(searchLocation);
      specification.setWithinMeters(48281);
    }
    if (searchName != null || searchLocation != null) {
      context.getRequest().setAttribute("isSearchResults", "true");
      context.getRequest().setAttribute("searchName", searchName);
      context.getRequest().setAttribute("searchLocation", searchLocation);
      if (!CARD_VIEW_JSP.equals(jsp)) {
        jsp = SEARCH_RESULTS_JSP;
      }
    }

    // Sort by nearby items
    if (StringUtils.isNotBlank(nearbyItemUniqueId)) {
      // Find items nearby based on the specified item
      Item item = ItemRepository.findByUniqueId(nearbyItemUniqueId);
      if (item == null || !item.hasGeoPoint()) {
        return null;
      }
      specification.setExcludeId(item.getId());
      specification.setNearItemId(item.getId());
      specification.setWithinMeters(48281);
    }

    // Query the data
    List<Item> itemList = ItemRepository.findAll(specification, constraints);
    if (itemList == null || itemList.isEmpty()) {
      if (!showWhenEmpty) {
        LOG.debug("Skipping, no items found for collection: " + collection.getUniqueId());
        return context;
      }
    }
    context.getRequest().setAttribute("itemList", itemList);

    if (TABLE_VIEW_JSP.equals(jsp)) {
      // Determine the columns to display
      Map<String, CustomField> tableColumnsList = CollectionTableColumnsCommand.createListFromSettings(collection,
          context.getPreferences().get("columns"));
      context.getRequest().setAttribute("tableColumnsList", tableColumnsList);
    }

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));
    context.getRequest().setAttribute("showPaging", context.getPreferences().getOrDefault("showPaging", "true"));
    context.getRequest().setAttribute("returnPage", context.getRequest().getRequestURI());

    // List view preferences
    context.getRequest().setAttribute("showLink", context.getPreferences().getOrDefault("showLink", "true"));
    context.getRequest().setAttribute("showImage", context.getPreferences().getOrDefault("showImage", "false"));
    context.getRequest().setAttribute("showIcon", context.getPreferences().getOrDefault("showIcon", "false"));
    context.getRequest().setAttribute("showSummary", context.getPreferences().getOrDefault("showSummary", "false"));
    context.getRequest().setAttribute("showCategory", context.getPreferences().getOrDefault("showCategory", "false"));
    context.getRequest().setAttribute("showCategoryIcon", context.getPreferences().getOrDefault("showCategoryIcon", "true"));
    context.getRequest().setAttribute("showAddress", context.getPreferences().getOrDefault("showAddress", "true"));
    context.getRequest().setAttribute("showKeywords", context.getPreferences().getOrDefault("showKeywords", "true"));
    context.getRequest().setAttribute("showUrl", context.getPreferences().getOrDefault("showUrl", "false"));
    context.getRequest().setAttribute("showBullets", context.getPreferences().getOrDefault("showBullets", "false"));
    context.getRequest().setAttribute("showActionLinks", context.getPreferences().getOrDefault("showActionLinks", "false"));
    context.getRequest().setAttribute("showLaunchLink", context.getPreferences().getOrDefault("showLaunchLink", "false"));
    context.getRequest().setAttribute("infoLabel", context.getPreferences().getOrDefault("infoLabel", "Get Info"));
    context.getRequest().setAttribute("launchLabel", context.getPreferences().getOrDefault("launchLabel", "Launch"));
    context.getRequest().setAttribute("useItemLink", context.getPreferences().getOrDefault("useItemLink", "false"));
    context.getRequest().setAttribute("useInfoLink", context.getPreferences().getOrDefault("useInfoLink", "true"));

    // Card size view preferences based on grid cells
    String smallGridCount = context.getPreferences().getOrDefault("smallGridCount", "6");
    context.getRequest().setAttribute("smallGridCount", smallGridCount);
    String mediumGridCount = context.getPreferences().getOrDefault("mediumGridCount", "4");
    context.getRequest().setAttribute("mediumGridCount", mediumGridCount);
    context.getRequest().setAttribute("largeGridCount", context.getPreferences().getOrDefault("largeGridCount", "3"));

    // Show the JSP
    context.setJsp(jsp);
    return context;
  }
}
