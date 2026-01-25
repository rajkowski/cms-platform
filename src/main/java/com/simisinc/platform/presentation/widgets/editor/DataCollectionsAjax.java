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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Returns collections for the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:00 PM
 */
public class DataCollectionsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(DataCollectionsAjax.class);

  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DataCollectionsAjax...");

    // Check permissions: only allow admins and data editors
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to access collections");
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Retrieve all collections
    List<Collection> collectionList = CollectionRepository.findAll();

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{\"collections\":[");
    
    boolean first = true;
    for (Collection collection : collectionList) {
      if (!first) {
        sb.append(",");
      }
      sb.append("{");
      sb.append("\"id\":").append(collection.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(collection.getName())).append("\",");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(collection.getUniqueId())).append("\",");
      sb.append("\"description\":\"").append(JsonCommand.toJson(collection.getDescription() != null ? collection.getDescription() : "")).append("\",");
      sb.append("\"itemCount\":").append(collection.getItemCount());
      sb.append("}");
      first = false;
    }
    
    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }
}
