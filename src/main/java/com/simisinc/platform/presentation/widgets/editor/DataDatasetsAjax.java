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
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.infrastructure.persistence.datasets.DatasetRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Returns datasets for the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:00 PM
 */
public class DataDatasetsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908896L;
  private static Log LOG = LogFactory.getLog(DataDatasetsAjax.class);

  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DataDatasetsAjax...");

    // Check permissions: only allow admins and data editors
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to access datasets");
      context.setJson("{\"error\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Retrieve all datasets
    List<Dataset> datasetList = DatasetRepository.findAll();

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{\"datasets\":[");
    
    boolean first = true;
    for (Dataset dataset : datasetList) {
      if (!first) {
        sb.append(",");
      }
      sb.append("{");
      sb.append("\"id\":").append(dataset.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(dataset.getName())).append("\",");
      sb.append("\"recordCount\":").append(dataset.getRecordCount()).append(",");
      sb.append("\"syncEnabled\":").append(dataset.getSyncEnabled());
      sb.append("}");
      first = false;
    }
    
    sb.append("]}");

    context.setJson(sb.toString());
    return context;
  }
}
