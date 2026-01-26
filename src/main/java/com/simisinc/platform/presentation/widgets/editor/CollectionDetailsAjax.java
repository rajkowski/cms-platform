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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.infrastructure.persistence.items.CollectionRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns collection details for the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:30 PM
 */
public class CollectionDetailsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908902L;
  private static Log LOG = LogFactory.getLog(CollectionDetailsAjax.class);

  // GET method
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("CollectionDetailsAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to access collection details");
      context.setJson("{\"error\":\"Permission denied\"}");
      return context;
    }

    // Get the collection unique ID
    String uniqueId = context.getParameter("uniqueId");
    if (StringUtils.isBlank(uniqueId)) {
      context.setJson("{\"error\":\"Collection uniqueId is required\"}");
      return context;
    }

    // Retrieve the collection
    Collection collection = CollectionRepository.findByUniqueId(uniqueId);
    if (collection == null) {
      context.setJson("{\"error\":\"Collection not found\"}");
      return context;
    }

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"id\":").append(collection.getId()).append(",");
    sb.append("\"name\":\"").append(JsonCommand.toJson(collection.getName())).append("\",");
    sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(collection.getUniqueId())).append("\",");
    sb.append("\"description\":\"").append(JsonCommand.toJson(StringUtils.defaultString(collection.getDescription()))).append("\",");
    sb.append("\"allowsGuests\":").append(collection.getAllowsGuests()).append(",");
    sb.append("\"guestPrivacyType\":").append(collection.getGuestPrivacyType()).append(",");
    sb.append("\"hasAllowedGroups\":").append(collection.doAllowedGroupsCheck()).append(",");
    sb.append("\"itemCount\":").append(collection.getItemCount()).append(",");
    sb.append("\"categoryCount\":").append(collection.getCategoryCount()).append(",");
    sb.append("\"showSearch\":").append(collection.getShowSearch()).append(",");
    sb.append("\"showListingsLink\":").append(collection.getShowListingsLink()).append(",");
    
    // Theme colors
    if (StringUtils.isNotBlank(collection.getHeaderTextColor())) {
      sb.append("\"headerTextColor\":\"").append(JsonCommand.toJson(collection.getHeaderTextColor())).append("\",");
    }
    if (StringUtils.isNotBlank(collection.getHeaderBgColor())) {
      sb.append("\"headerBgColor\":\"").append(JsonCommand.toJson(collection.getHeaderBgColor())).append("\",");
    }
    if (StringUtils.isNotBlank(collection.getIcon())) {
      sb.append("\"icon\":\"").append(JsonCommand.toJson(collection.getIcon())).append("\",");
    }
    if (StringUtils.isNotBlank(collection.getImageUrl())) {
      sb.append("\"imageUrl\":\"").append(JsonCommand.toJson(collection.getImageUrl())).append("\",");
    }
    
    // Remove trailing comma if exists
    String json = sb.toString();
    if (json.endsWith(",")) {
      json = json.substring(0, json.length() - 1);
    }
    json += "}";

    context.setJson(json);
    return context;
  }
}
