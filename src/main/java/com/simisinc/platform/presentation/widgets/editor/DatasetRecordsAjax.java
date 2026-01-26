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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.datasets.DatasetFileCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.infrastructure.persistence.datasets.DatasetRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns records for a dataset in the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:58 PM
 */
public class DatasetRecordsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908905L;
  private static Log LOG = LogFactory.getLog(DatasetRecordsAjax.class);

  // GET method
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DatasetRecordsAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to access dataset records");
      context.setJson("{\"error\":\"Permission denied\"}");
      return context;
    }

    // Get the dataset ID
    long datasetId = context.getParameterAsLong("datasetId", -1);
    if (datasetId == -1) {
      context.setJson("{\"error\":\"Dataset ID is required\"}");
      return context;
    }

    // Retrieve the dataset
    Dataset dataset = DatasetRepository.findById(datasetId);
    if (dataset == null) {
      context.setJson("{\"error\":\"Dataset not found\"}");
      return context;
    }

    // Get pagination parameters
    int limit = context.getParameterAsInt("limit", 20);
    int offset = context.getParameterAsInt("offset", 0);

    // Load dataset records from file or storage
    try {
      List<String[]> rows = DatasetFileCommand.loadRows(dataset, offset, limit, false);

      int totalCount = dataset.getRowCount();

      // Build JSON response with records
      StringBuilder sb = new StringBuilder();
      sb.append("{\"records\":[");
      boolean first = true;
      if (rows != null) {
        for (String[] record : rows) {
          if (!first)
            sb.append(",");
          sb.append("{");
          boolean firstField = true;
          for (int i = 0; i < record.length; i++) {
            if (!firstField) {
              sb.append(",");
            }
            sb.append("\"").append(JsonCommand.toJson(dataset.getColumnNamesList().get(i))).append("\":");
            sb.append("\"").append(JsonCommand.toJson(record[i])).append("\"");
            firstField = false;
          }
          sb.append("}");
          first = false;
        }
      }
      sb.append("],");
      sb.append("\"count\":").append(rows != null ? rows.size() : 0).append(",");
      sb.append("\"offset\":").append(offset).append(",");
      sb.append("\"limit\":").append(limit).append(",");
      sb.append("\"dataset\":{");
      sb.append("\"name\":\"").append(JsonCommand.toJson(dataset.getName())).append("\",");
      sb.append("\"rowCount\":").append(totalCount).append(",");
      sb.append("\"columnCount\":").append(dataset.getColumnCount());
      sb.append("}");
      sb.append("}");

      context.setJson(sb.toString());
    } catch (Exception e) {
      LOG.error("Error loading dataset records: " + e.getMessage());
      context.setJson("{\"error\":\"Could not load dataset records\"}");
      context.setSuccess(false);
    }
    return context;
  }
}
