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

package com.simisinc.platform.rest.services.items;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.items.LoadCategoryCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemSpecification;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;
import com.simisinc.platform.rest.controller.ServiceResponseCommand;

/**
 * Returns a list of items for the given collection unique id
 *
 * @author matt rajkowski
 * @created 4/27/18 10:15 AM
 */
public class ItemListService extends GenericRestService {

  private static Log LOG = LogFactory.getLog(ItemListService.class);

  private static final String PARAM_QUERY = "query";
  private static final String PARAM_CATEGORY = "category";
  private static final String PARAM_TAGS = "tags";

  private static final String PARAM_FILTER = "filter";
  private static final String FILTER_BY_NAME = "name";
  private static final String FILTER_BY_UNIQUE_ID = "uniqueId";

  private static final String PARAM_EXPAND = "expand";
  private static final String PARAM_PAGE = "page";
  private static final String PARAM_SIZE = "size";

  private static final String EXPAND_DETAILS = "details";

  // GET /items/{collectionUniqueId}?category=${categoryUniqueId}&query=value&expand=details
  @Override
  public ServiceResponse get(ServiceContext context) {

    // Determine the collection
    String collectionUniqueId = context.getPathParam();
    Collection collection = LoadCollectionCommand.loadCollectionByUniqueId(collectionUniqueId);
    if (collection == null) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", "Collection was not found");
      return response;
    }

    // Validate access to the collection
    if (LoadCollectionCommand.loadCollectionByIdForAuthorizedUser(collection.getId(), context.getUserId()) == null) {
      LOG.warn("User does not have access to this collection");
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", "Collection was not found");
      return response;
    }

    // Check for a specific category
    String categoryUniqueId = context.getParameter(PARAM_CATEGORY);
    Category category = null;
    if (!StringUtils.isBlank(categoryUniqueId)) {
      category = LoadCategoryCommand.loadCategoryByUniqueIdWithinCollection(categoryUniqueId, collection.getId());
      if (category == null) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put("title", "Category was not found");
        return response;
      }
    }

    // Determine the constraints
    int pageNumber = context.getParameterAsInt(PARAM_PAGE, 1);
    int pageSize = context.getParameterAsInt(PARAM_SIZE, 20);
    if (pageNumber < 1 || pageSize < 1) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", "Required query params: page (>=1) and size (>=1)");
      return response;
    }

    // Prepare the specification
    ItemSpecification specification = new ItemSpecification();
    specification.setCollectionId(collection.getId());
    specification.setForUserId(context.getUserId());

    if (category != null) {
      specification.setCategoryId(category.getId());
    }

    // Check for tags
    String tagsValue = context.getParameter(PARAM_TAGS);
    if (StringUtils.isNotBlank(tagsValue)) {
      List<String> filterTagList = new ArrayList<>();
      String[] tagsArray = tagsValue.split(",");
      for (String tag : tagsArray) {
        filterTagList.add(tag.trim());
      }
      if (!filterTagList.isEmpty()) {
        specification.setFilterTags(filterTagList.toArray(new String[0]));
      }
    }

    // Check for a search query
    String query = context.getParameter(PARAM_QUERY);
    if (StringUtils.isNotBlank(query)) {
      specification.setSearchName(query);
    }

    // Determine any filters
    boolean validFilters = true;
    String[] filterValues = context.getParameterAsArray(PARAM_FILTER);
    if (filterValues != null) {
      for (String filterValue : filterValues) {
        // Handles an 'equals' filter in the form of "fieldName=fieldValue"
        String[] parts = filterValue.split("=");
        if (parts.length == 2) {
          String fieldName = parts[0].trim();
          String fieldValue = parts[1].trim();
          if (StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(fieldValue)) {
            if (fieldName.equals(FILTER_BY_NAME)) {
              // Name filter
              specification.setName(fieldValue);
            } else if (fieldName.equals(FILTER_BY_UNIQUE_ID)) {
              // Unique ID filter
              specification.setUniqueId(fieldValue);
            } else {
              LOG.debug("Unknown field filter: " + fieldName + "=" + fieldValue);
              validFilters = false;
            }
          }
        }
      }
    }
    if (!validFilters) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", "Invalid filter query param");
      return response;
    }

    // Retrieve the records
    DataConstraints constraints = new DataConstraints(pageNumber, pageSize);
    List<Item> itemList = ItemRepository.findAll(specification, constraints);

    // Prepare the response
    ServiceResponse response = new ServiceResponse(200);
    ServiceResponseCommand.addMeta(response, "item", itemList, constraints);

    // Return the ItemDetailResponse if requested
    boolean expandItemDetails = false;
    String[] expand = context.getParameterAsArray(PARAM_EXPAND);
    if (expand != null) {
      for (String expandValue : expand) {
        if (EXPAND_DETAILS.equals(expandValue)) {
          expandItemDetails = true;
        }
      }
    }

    // Determine the response type
    if (expandItemDetails) {
      // Set the fields to return
      List<ItemDetailsResponse> recordList = new ArrayList<>();
      for (Item item : itemList) {
        recordList.add(new ItemDetailsResponse(item, collection));
      }
      response.setData(recordList);
    } else {
      // Set the fields to return
      List<ItemResponse> recordList = new ArrayList<>();
      for (Item item : itemList) {
        recordList.add(new ItemResponse(item));
      }
      response.setData(recordList);
    }

    return response;
  }

}
