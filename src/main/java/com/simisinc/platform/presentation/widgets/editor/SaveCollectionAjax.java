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

package com.simisinc.platform.presentation.widgets.editor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.items.SaveCollectionCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.infrastructure.persistence.items.CollectionRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Saves a collection for the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:00 PM
 */
public class SaveCollectionAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908897L;
  private static Log LOG = LogFactory.getLog(SaveCollectionAjax.class);

  // POST method
  public WidgetContext post(WidgetContext context) {

    LOG.debug("SaveCollectionAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to save collection");
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Get the collection data from request
    long collectionId = context.getParameterAsLong("id", -1);
    String name = context.getParameter("name");
    String uniqueId = context.getParameter("uniqueId");
    String description = context.getParameter("description");
    boolean allowsGuests = "true".equalsIgnoreCase(context.getParameter("allowsGuests"));
    boolean showSearch = "true".equalsIgnoreCase(context.getParameter("showSearch"));
    boolean showListingsLink = "true".equalsIgnoreCase(context.getParameter("showListingsLink"));

    // Validate required fields
    if (StringUtils.isBlank(name)) {
      context.setJson("{\"success\":false,\"message\":\"Collection name is required\"}");
      context.setSuccess(false);
      return context;
    }

    try {
      Collection collection;
      if (collectionId > 0) {
        // Update existing collection
        collection = CollectionRepository.findById(collectionId);
        if (collection == null) {
          context.setJson("{\"success\":false,\"message\":\"Collection not found\"}");
          context.setSuccess(false);
          return context;
        }
      } else {
        // Create new collection
        collection = new Collection();
        collection.setCreatedBy(context.getUserId());
      }

      // Set the properties
      collection.setName(name);
      if (StringUtils.isNotBlank(uniqueId)) {
        collection.setUniqueId(uniqueId);
      }
      collection.setDescription(description);
      collection.setAllowsGuests(allowsGuests);
      collection.setShowSearch(showSearch);
      collection.setShowListingsLink(showListingsLink);

      // Save the collection
      Collection savedCollection = SaveCollectionCommand.saveCollection(collection);
      if (savedCollection == null) {
        throw new DataException("Collection could not be saved");
      }

      // Return success with collection details
      StringBuilder sb = new StringBuilder();
      sb.append("{\"success\":true,");
      sb.append("\"message\":\"Collection saved successfully\",");
      sb.append("\"collection\":{");
      sb.append("\"id\":").append(savedCollection.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(savedCollection.getName())).append("\",");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(savedCollection.getUniqueId())).append("\"");
      sb.append("}}");

      context.setJson(sb.toString());
      return context;

    } catch (DataException de) {
      LOG.error("DataException", de);
      context.setJson("{\"success\":false,\"message\":\"" + JsonCommand.toJson(de.getMessage()) + "\"}");
      context.setSuccess(false);
      return context;
    } catch (Exception e) {
      LOG.error("Exception", e);
      context.setJson("{\"success\":false,\"message\":\"An error occurred while saving the collection\"}");
      context.setSuccess(false);
      return context;
    }
  }
}
