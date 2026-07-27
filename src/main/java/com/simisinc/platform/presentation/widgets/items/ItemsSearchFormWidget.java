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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.infrastructure.persistence.items.CategoryRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.cms.SearchCriteria;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 5/23/18 2:49 PM
 */
public class ItemsSearchFormWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/items/items-search-form.jsp";
  static String EXPANDED_VIEW_JSP = "/items/items-search-expanded-form.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Determine the form settings
    context.getRequest().setAttribute("buttonName", context.getPreferences().getOrDefault("buttonName", "Search"));
    context.getRequest().setAttribute("useAutoComplete",
        context.getPreferences().getOrDefault("useAutoComplete", "true"));
    context.getRequest().setAttribute("useSearch", context.getPreferences().getOrDefault("useSearch", "true"));
    context.getRequest().setAttribute("useLocation", context.getPreferences().getOrDefault("useLocation", "true"));
    context.getRequest().setAttribute("useIcon", context.getPreferences().getOrDefault("useIcon", "false"));
    context.getRequest().setAttribute("showCategories",
        context.getPreferences().getOrDefault("showCategories", "true"));
    boolean basedOnItems = "true".equals(context.getPreferences().getOrDefault("basedOnItems", "false"));
    context.getRequest().setAttribute("showTags", context.getPreferences().getOrDefault("showTags", "false"));

    // Search values
    context.getRequest().setAttribute("searchName", context.getRequest().getParameter("searchName"));
    context.getRequest().setAttribute("searchLocation", context.getRequest().getParameter("searchLocation"));
    String searchTags = context.getRequest().getParameter("searchTags");
    context.getRequest().setAttribute("searchTags", searchTags);

    // Use the current collectionId to get the available categories to search on
    String collectionUniqueId = context.getPreferences().getOrDefault("collectionUniqueId",
        context.getCoreData().get("collectionUniqueId"));
    if (StringUtils.isBlank(collectionUniqueId)) {
      LOG.warn("Collection not specified");
      return context;
    }
    Collection collection = LoadCollectionCommand.loadCollectionByUniqueId(collectionUniqueId);
    if (collection != null) {
      context.getRequest().setAttribute("collection", collection);
      List<Category> categoryList = CategoryRepository.findAllByCollectionId(collection.getId(), basedOnItems);
      if (categoryList != null && !categoryList.isEmpty() && categoryList.size() <= 100) {
        context.getRequest().setAttribute("categoryList", categoryList);
      }
    }
    // Use the currently selected categoryId for the drop-down selection
    String categoryId = context.getRequest().getParameter("categoryId");
    if (categoryId != null) {
      context.getRequest().setAttribute("categoryId", categoryId);
    }

    // Prepare tags for form display
    if (searchTags != null) {
      String tagsValue = JsonCommand.toJsonArray(SearchCriteria.parseTags(searchTags));
      if (tagsValue == null) {
        tagsValue = "[]";
      }
      context.getRequest().setAttribute("tagsValue", tagsValue);
    } else {
      context.getRequest().setAttribute("tagsValue", "[]");
    }

    // Show the JSP
    String view = context.getPreferences().get("view");
    if ("expanded".equals(view)) {
      context.setJsp(EXPANDED_VIEW_JSP);
    } else {
      context.setJsp(JSP);
    }
    return context;
  }

  public WidgetContext post(WidgetContext context) {

    String name = context.getParameter("name");
    if (name != null) {
      name = name.trim();
    }
    context.addSharedRequestValue("searchName", name);

    String location = context.getParameter("location");
    if (location != null) {
      location = location.trim();
    }
    context.addSharedRequestValue("searchLocation", location);

    String categoryId = context.getParameter("categoryId" + context.getUniqueId());
    //    if (StringUtils.isNumeric(categoryId) && !"-1".equals(categoryId)) {
    //      context.addSharedRequestValue("categoryId", categoryId);
    //    }

    // Handle tags from JSON string using centralized JsonCommand
    String tagsValue = context.getParameter("tags");
    String[] tagsArray = null;
    String searchTags = null;
    if (StringUtils.isNotBlank(tagsValue)) {
      tagsArray = JsonCommand.fromJsonArray(tagsValue);
      if (tagsArray.length > 0) {
        searchTags = String.join(",", tagsArray);
        context.addSharedRequestValue("searchTags", searchTags);
      }
    }

    String redirectTo = context.getPreferences().get("redirectTo");
    if (StringUtils.isBlank(redirectTo)) {
      // Use the current collectionId to get the available categories to search on
      String collectionUniqueId = context.getPreferences().getOrDefault("collectionUniqueId",
          context.getCoreData().get("collectionUniqueId"));
      if (StringUtils.isNotBlank(collectionUniqueId)) {
        Collection collection = LoadCollectionCommand.loadCollectionByUniqueId(collectionUniqueId);
        if (collection != null) {
          redirectTo = collection.createListingsLink();
        }
      }
    }

    if (redirectTo != null) {
      boolean hasFirst = false;
      // Name
      if (StringUtils.isNotBlank(name)) {
        hasFirst = true;
        redirectTo += "?searchName=" + UrlCommand.encodeUri(name);
      }
      // Location
      if (StringUtils.isNotBlank(location)) {
        if (!hasFirst) {
          hasFirst = true;
          redirectTo += "?";
        } else {
          redirectTo += "&";
        }
        redirectTo += "searchLocation=" + UrlCommand.encodeUri(location);
      }
      // Category
      if (StringUtils.isNumeric(categoryId) && !"-1".equals(categoryId)) {
        if (!hasFirst) {
          hasFirst = true;
          redirectTo += "?";
        } else {
          redirectTo += "&";
        }
        redirectTo += "categoryId=" + UrlCommand.encodeUri(categoryId);
      }
      if (StringUtils.isNotBlank(searchTags)) {
        if (!hasFirst) {
          // hasFirst = true;
          redirectTo += "?";
        } else {
          redirectTo += "&";
        }
        redirectTo += "searchTags=" + UrlCommand.encodeUri(searchTags);
      }

      context.setRedirect(redirectTo);
    }
    return context;
  }
}
