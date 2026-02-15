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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns dataset details for the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:30 PM
 */
public class DatasetDetailsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908903L;
  private static Log LOG = LogFactory.getLog(DatasetDetailsAjax.class);

  // GET method
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DatasetDetailsAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to access dataset details");
      context.setJson("{\"error\":\"Permission denied\"}");
      return context;
    }

    // Get the dataset ID
    long datasetId = context.getParameterAsLong("id", -1);
    if (datasetId == -1) {
      context.setJson("{\"error\":\"Dataset id is required\"}");
      return context;
    }

    // Retrieve the dataset
    Dataset dataset = DatasetRepository.findById(datasetId);
    if (dataset == null) {
      context.setJson("{\"error\":\"Dataset not found\"}");
      return context;
    }

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"id\":").append(dataset.getId()).append(",");
    sb.append("\"name\":\"").append(JsonCommand.toJson(dataset.getName())).append("\",");
    sb.append("\"sourceUrl\":\"").append(JsonCommand.toJson(StringUtils.defaultString(dataset.getSourceUrl()))).append("\",");
    sb.append("\"sourceInfo\":\"").append(JsonCommand.toJson(StringUtils.defaultString(dataset.getSourceInfo()))).append("\",");
    sb.append("\"filename\":\"").append(JsonCommand.toJson(StringUtils.defaultString(dataset.getFilename()))).append("\",");
    sb.append("\"fileLength\":").append(dataset.getFileLength()).append(",");
    sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(dataset.getFileType()))).append("\",");
    sb.append("\"rowCount\":").append(dataset.getRowCount()).append(",");
    sb.append("\"columnCount\":").append(dataset.getColumnCount()).append(",");
    sb.append("\"recordCount\":").append(dataset.getRecordCount()).append(",");
    sb.append("\"rowsProcessed\":").append(dataset.getRowsProcessed()).append(",");
    sb.append("\"processStatus\":").append(dataset.getProcessStatus()).append(",");
    sb.append("\"syncEnabled\":").append(dataset.getSyncEnabled()).append(",");
    sb.append("\"syncStatus\":").append(dataset.getSyncStatus()).append(",");
    
    // Collection mapping
    if (StringUtils.isNotBlank(dataset.getCollectionUniqueId())) {
      sb.append("\"collectionUniqueId\":\"").append(JsonCommand.toJson(dataset.getCollectionUniqueId())).append("\",");
    }
    
    // Schedule info
    sb.append("\"scheduleEnabled\":").append(dataset.getScheduleEnabled()).append(",");
    if (StringUtils.isNotBlank(dataset.getScheduleFrequency())) {
      sb.append("\"scheduleFrequency\":\"").append(JsonCommand.toJson(dataset.getScheduleFrequency())).append("\",");
    }
    
    // Sync info
    sb.append("\"syncRecordCount\":").append(dataset.getSyncRecordCount()).append(",");
    sb.append("\"syncAddCount\":").append(dataset.getSyncAddCount()).append(",");
    sb.append("\"syncUpdateCount\":").append(dataset.getSyncUpdateCount()).append(",");
    sb.append("\"syncDeleteCount\":").append(dataset.getSyncDeleteCount()).append(",");
    
    // Request configuration
    if (StringUtils.isNotBlank(dataset.getRequestConfig())) {
      sb.append("\"requestConfig\":").append(dataset.getRequestConfig());
    } else {
      sb.append("\"requestConfig\":null");
    }
    
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
