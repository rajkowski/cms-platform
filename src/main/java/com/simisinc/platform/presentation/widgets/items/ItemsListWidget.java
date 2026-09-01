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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.items.CollectionTableColumnsCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.CategoryRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemSpecification;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.Region;
import com.zeroio.platform.domain.model.cms.SearchCriteria;
import com.zeroio.platform.infrastructure.persistence.RegionRepository;

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
  static String TAGS_VIEW_JSP = "/items/items-tags-view.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine preferences
    String collectionUniqueId = context.getPreferences().get("collectionUniqueId");
    String categoryName = context.getPreferences().get("category");
    String nearbyItemUniqueId = context.getPreferences().get("nearbyItemUniqueId");
    boolean showMine = "true".equals(context.getPreferences().getOrDefault("showMine", "false"));
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "false"));
    boolean useUserRegionPref = "true".equals(context.getPreferences().getOrDefault("useUserRegion", "false"));

    // Determine filter parameters
    long categoryId = context.getParameterAsLong("categoryId");

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
    } else if ("tags".equals(view)) {
      jsp = TAGS_VIEW_JSP;
    }

    // @todo Consider using a cache for general users

    // Determine the collection
    if (collectionUniqueId == null) {
      // Try to extract the collection unique ID from the path
      // Expects /path/collection-name
      collectionUniqueId = extractNameFromPath(context.getUri());
      if (collectionUniqueId != null) {
        LOG.debug("Collection unique ID extracted from path: " + collectionUniqueId);
      } else {
        LOG.warn("Set a collection or collectionUniqueId preference, or user does not have access");
        LOG.debug("Stopping - collection unique ID not found in preferences or path");
        return null;
      }
    }

    Collection collection = LoadCollectionCommand.loadCollectionByUniqueIdForAuthorizedUser(collectionUniqueId,
        context.getUserId());
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
    } else if (StringUtils.isNotBlank(sortBy)) {
      List<String> sortColumns = new ArrayList<>();
      for (String part : sortBy.split("\\|")) {
        part = part.trim();
        if (part.startsWith("custom.")) {
          String customSortField = part.substring("custom.".length()).trim();
          String sortDirection = null;
          if (customSortField.toLowerCase().endsWith(" asc") || customSortField.toLowerCase().endsWith(" desc")) {
            int directionStart = customSortField.lastIndexOf(' ');
            sortDirection = customSortField.substring(directionStart + 1).trim().toLowerCase();
            customSortField = customSortField.substring(0, directionStart).trim();
          }
          // Validate field name to prevent SQL injection: only allow alphanumeric, space, underscore, hyphen
          if (customSortField.matches("[A-Za-z0-9 _-]+")) {
            String customSortExpression = "LOWER((SELECT f->>'value' FROM jsonb_array_elements(items.field_values) AS f WHERE f->>'name' = '"
                + customSortField + "' LIMIT 1))";
            if (StringUtils.isNotBlank(sortDirection)) {
              customSortExpression += " " + sortDirection;
            }
            sortColumns.add(customSortExpression);
          } else {
            LOG.warn("sortBy custom field name contains invalid characters: " + customSortField);
          }
        }
      }
      if (!sortColumns.isEmpty()) {
        constraints.setColumnsToSortBy(sortColumns.toArray(new String[0]));
      }
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
    if (useUserRegionPref) {
      String userRegionCode = context.getUserSession().getSelectedRegionCode();
      if (StringUtils.isNotBlank(userRegionCode)) {
        Region region = RegionRepository.findByCode(userRegionCode);
        if (region != null) {
          specification.setRegionTags(region.getValues());
        }
      }
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

    // Apply custom field filters from widget preference
    // (e.g. custom.Timing=Prior to Start Date|custom.Who=John)
    // (e.g. custom.Code=${item.custom.Code})
    String filterPreference = context.getPreferences().get("filter");
    if (StringUtils.isNotBlank(filterPreference)) {
      LOG.debug("Applying custom field filters from widget preference: " + filterPreference);
      for (String part : filterPreference.split("\\|")) {
        part = part.trim();
        LOG.debug("Processing filter part: " + part);
        if (part.startsWith("custom.")) {
          String filterPart = part.substring("custom.".length());
          int eqIdx = filterPart.indexOf('=');
          if (eqIdx > 0) {
            String filterFieldName = filterPart.substring(0, eqIdx).trim();
            String filterFieldValue = filterPart.substring(eqIdx + 1).trim();
            if (StringUtils.isNotBlank(filterFieldName) && StringUtils.isNotBlank(filterFieldValue)) {
              // Handle the case where the filter value is a reference to another field (e.g., ${item.custom.Code})
              LOG.debug("Processing custom field filter: " + filterFieldName + " = " + filterFieldValue);
              if (filterFieldValue.startsWith("${item.custom.") && filterFieldValue.endsWith("}")) {
                String referencedFieldName = filterFieldValue.substring("${item.custom.".length(),
                    filterFieldValue.length() - 1);

                // Load the authorized item that this widget is embedded on
                Item currentItem = LoadItemCommand.loadItemByUniqueId(context.getCoreData().get("itemUniqueId"));
                if (currentItem == null) {
                  // If the current item is not found, we cannot apply the filter, so we return null to indicate that the widget should not render any items
                  return null;
                }

                // Retrieve the value of the referenced custom field from the current item in the request context
                CustomField referencedField = currentItem.getCustomField(referencedFieldName);
                if (referencedField == null) {
                  // If the referenced field is not found, we cannot apply the filter, so we return null to indicate that the widget should not render any items
                  return null;
                }
                String referencedFieldValue = referencedField.getValue();
                if (StringUtils.isBlank(referencedFieldValue)) {
                  // If the referenced field value is blank, we cannot apply the filter, so we return null to indicate that the widget should not render any items
                  return null;
                }
                LOG.debug("Adding custom field filter: " + filterFieldName + " = " + referencedFieldValue);
                specification.addCustomFieldFilter(filterFieldName, referencedFieldValue);
              } else {
                // Validate field name: only allow alphanumeric, space, underscore, hyphen
                LOG.debug("Adding custom field filter: " + filterFieldName + " = " + filterFieldValue);
                specification.addCustomFieldFilter(filterFieldName, filterFieldValue);
              }
            }
          }
        } else {
          // Check for "fieldname in (value1,value2,...)" syntax
          int inIdx = part.toLowerCase().indexOf(" in (");
          if (inIdx > 0 && part.endsWith(")")) {
            String fieldName = part.substring(0, inIdx).trim();
            String valuesStr = part.substring(inIdx + 5, part.length() - 1);
            List<String> values = parseInFilterValues(valuesStr);
            if ("tags".equals(fieldName) && !values.isEmpty()) {
              specification.addFieldInFilter(fieldName, values);
            }
          }
        }
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

    String searchTags = context.getSharedRequestValue("searchTags");
    if (searchTags == null) {
      searchTags = context.getParameter("searchTags");
    }
    if (StringUtils.isNotBlank(searchTags)) {
      specification.setFilterTags(SearchCriteria.parseTags(searchTags));
    }

    if (searchName != null || searchLocation != null || searchTags != null) {
      context.getRequest().setAttribute("isSearchResults", "true");
      context.getRequest().setAttribute("searchName", searchName);
      context.getRequest().setAttribute("searchLocation", searchLocation);
      context.getRequest().setAttribute("searchTags", searchTags);
      if (!CARD_VIEW_JSP.equals(jsp) && !TABLE_VIEW_JSP.equals(jsp)) {
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
    context.getRequest().setAttribute("showCategoryIcon",
        context.getPreferences().getOrDefault("showCategoryIcon", "true"));
    context.getRequest().setAttribute("showCollectionIcon",
        context.getPreferences().getOrDefault("showCollectionIcon", "true"));
    context.getRequest().setAttribute("showAddress", context.getPreferences().getOrDefault("showAddress", "true"));
    context.getRequest().setAttribute("showKeywords", context.getPreferences().getOrDefault("showKeywords", "true"));
    context.getRequest().setAttribute("showUrl", context.getPreferences().getOrDefault("showUrl", "false"));
    context.getRequest().setAttribute("showBullets", context.getPreferences().getOrDefault("showBullets", "false"));
    context.getRequest().setAttribute("showActionLinks",
        context.getPreferences().getOrDefault("showActionLinks", "false"));
    context.getRequest().setAttribute("showLaunchLink",
        context.getPreferences().getOrDefault("showLaunchLink", "false"));
    context.getRequest().setAttribute("useInfoLink", context.getPreferences().getOrDefault("useInfoLink", "true"));
    context.getRequest().setAttribute("infoLabel", context.getPreferences().getOrDefault("infoLabel", "Get Info"));
    context.getRequest().setAttribute("launchLabel", context.getPreferences().getOrDefault("launchLabel", "Launch"));
    context.getRequest().setAttribute("useItemLink", context.getPreferences().getOrDefault("useItemLink", "false"));

    // Other preferences
    context.getRequest().setAttribute("trimValue", context.getPreferences().getOrDefault("trimValue", "50"));
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

  /**
   * Extract collection name from page path
   * Expected format: /path/collection-name
   * 
   * @param pagePath the page path
   * @return the collection name or null if not found
   */
  private String extractNameFromPath(String pagePath) {
    if (StringUtils.isBlank(pagePath)) {
      return null;
    }

    String collectionName = pagePath.substring(pagePath.lastIndexOf("/") + 1);
    if (StringUtils.isBlank(collectionName)) {
      LOG.debug("Skipping - collection name is blank");
      return null;
    }

    if (collectionName.contains("?")) {
      collectionName = collectionName.substring(0, collectionName.indexOf("?"));
    }

    collectionName = java.net.URLDecoder.decode(collectionName, java.nio.charset.StandardCharsets.UTF_8);

    return collectionName;
  }

  /**
   * Parse comma-separated values from an IN filter expression's value list.
   * Strips surrounding double-quotes from each value.
   * Example input: {@code "global_metrics","region1_metrics","region2_metrics"}
   *
   * @param valuesStr the raw values string (content inside the parentheses)
   * @return list of parsed, trimmed values
   */
  static List<String> parseInFilterValues(String valuesStr) {
    List<String> result = new ArrayList<>();
    if (StringUtils.isBlank(valuesStr)) {
      return result;
    }
    for (String val : valuesStr.split(",")) {
      val = val.trim();
      if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
        val = val.substring(1, val.length() - 1);
      }
      if (!val.isEmpty()) {
        result.add(val);
      }
    }
    return result;
  }
}
