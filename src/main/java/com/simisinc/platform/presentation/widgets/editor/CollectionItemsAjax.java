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
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.items.CollectionRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Returns items/records for a collection in the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:45 PM
 */
public class CollectionItemsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908904L;
  private static Log LOG = LogFactory.getLog(CollectionItemsAjax.class);

  // GET method
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("CollectionItemsAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to access collection items");
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

    // Get pagination parameters
    int limit = context.getParameterAsInt("limit", 20);
    int offset = context.getParameterAsInt("offset", 0);

    // Retrieve items for this collection
    ItemSpecification specification = new ItemSpecification();
    specification.setCollectionId(collection.getId());
    DataConstraints constraints = new DataConstraints(offset, limit);
    List<Item> itemList = ItemRepository.findAll(specification, constraints);
    
    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{\"items\":[");
    
    boolean first = true;
    for (Item item : itemList) {
      if (!first) {
        sb.append(",");
      }
      sb.append("{");
      sb.append("\"id\":").append(item.getId()).append(",");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(item.getUniqueId())).append("\",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(item.getName())).append("\",");
      sb.append("\"summary\":\"").append(JsonCommand.toJson(StringUtils.defaultString(item.getSummary()))).append("\"");
      sb.append("}");
      first = false;
    }
    
    sb.append("],");
    sb.append("\"count\":").append(itemList.size()).append(",");
    sb.append("\"offset\":").append(offset).append(",");
    sb.append("\"limit\":").append(limit);
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
