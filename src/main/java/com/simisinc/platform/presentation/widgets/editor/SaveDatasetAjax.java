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

import com.simisinc.platform.application.datasets.SaveDatasetCommand;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.infrastructure.persistence.datasets.DatasetRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Saves a dataset for the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:00 PM
 */
public class SaveDatasetAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908898L;
  private static Log LOG = LogFactory.getLog(SaveDatasetAjax.class);

  public WidgetContext post(WidgetContext context) {

    LOG.debug("SaveDatasetAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to save dataset");
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Get the dataset ID
    long datasetId = context.getParameterAsLong("id", -1);
    if (datasetId == -1) {
      context.setJson("{\"success\":false,\"message\":\"Dataset id is required\"}");
      context.setSuccess(false);
      return context;
    }

    // Retrieve the dataset
    Dataset dataset = DatasetRepository.findById(datasetId);
    if (dataset == null) {
      context.setJson("{\"success\":false,\"message\":\"Dataset not found\"}");
      context.setSuccess(false);
      return context;
    }

    // Update fields from parameters
    String name = context.getParameter("name");
    if (StringUtils.isNotBlank(name)) {
      dataset.setName(name);
    }

    String sourceUrl = context.getParameter("sourceUrl");
    if (sourceUrl != null) {
      dataset.setSourceUrl(StringUtils.trimToNull(sourceUrl));
    }

    String sourceInfo = context.getParameter("sourceInfo");
    if (sourceInfo != null) {
      dataset.setSourceInfo(StringUtils.trimToNull(sourceInfo));
    }

    String requestConfig = context.getParameter("requestConfig");
    if (requestConfig != null) {
      dataset.setRequestConfig(StringUtils.trimToNull(requestConfig));
    }

    String syncEnabled = context.getParameter("syncEnabled");
    if (syncEnabled != null) {
      dataset.setSyncEnabled("true".equalsIgnoreCase(syncEnabled));
    }

    String scheduleEnabled = context.getParameter("scheduleEnabled");
    if (scheduleEnabled != null) {
      dataset.setScheduleEnabled("true".equalsIgnoreCase(scheduleEnabled));
    }

    // Set the user who is modifying the dataset
    dataset.setModifiedBy(context.getUserId());

    // Save the dataset
    try {
      Dataset savedDataset = SaveDatasetCommand.saveDataset(dataset);
      if (savedDataset == null) {
        context.setJson("{\"success\":false,\"message\":\"Failed to save dataset\"}");
        context.setSuccess(false);
        return context;
      }
      context.setJson("{\"success\":true,\"message\":\"Dataset saved successfully\",\"id\":" + savedDataset.getId() + "}");
      return context;
    } catch (Exception e) {
      LOG.error("Error saving dataset", e);
      context.setJson("{\"success\":false,\"message\":\"Error: " + e.getMessage() + "\"}");
      context.setSuccess(false);
      return context;
    }
  }
}
