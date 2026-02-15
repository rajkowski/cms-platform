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

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.datasets.DatasetDownloadRemoteFileCommand;
import com.simisinc.platform.application.datasets.DatasetUploadFileCommand;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Imports a dataset for the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:00 PM
 */
public class DatasetImportAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908899L;
  private static Log LOG = LogFactory.getLog(DatasetImportAjax.class);

  public WidgetContext post(WidgetContext context) {

    LOG.debug("DatasetImportAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to import dataset");
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Check the form values
    String name = context.getParameter("name");
    String sourceUrl = context.getParameter("sourceUrl");
    String fileType = context.getParameter("fileType");
    String requestConfig = context.getParameter("requestConfig");

    if (StringUtils.isBlank(name)) {
      context.setJson("{\"success\":false,\"message\":\"Dataset name is required\"}");
      context.setSuccess(false);
      return context;
    }

    // Populate the fields
    Dataset datasetBean = new Dataset();
    datasetBean.setName(name);
    datasetBean.setCreatedBy(context.getUserId());
    datasetBean.setModifiedBy(context.getUserId());

    if (StringUtils.isNotBlank(sourceUrl)) {
      // Remote URL import
      datasetBean.setSourceUrl(sourceUrl.trim());
      if (StringUtils.isNotBlank(requestConfig)) {
        datasetBean.setRequestConfig(StringUtils.trimToNull(requestConfig));
      }
      if (StringUtils.isNotBlank(fileType)) {
        datasetBean.setFileType(fileType);
      } else {
        // Default to JSON if not specified
        datasetBean.setFileType("application/json");
      }

      // Download the remote file
      try {
        DatasetDownloadRemoteFileCommand.handleRemoteFileDownload(datasetBean, context.getUserId());
      } catch (DataException e) {
        LOG.error("Error downloading remote file: " + e.getMessage());
        context.setJson("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        context.setSuccess(false);
        return context;
      }
    } else {
      // File upload import
      if (StringUtils.isNotBlank(fileType)) {
        datasetBean.setFileType(fileType);
      }

      // Check for an uploaded file and validate
      if (!DatasetUploadFileCommand.handleUpload(context, datasetBean)) {
        LOG.error("File upload failed");
        context.setJson("{\"success\":false,\"message\":\"File upload failed\"}");
        context.setSuccess(false);
        return context;
      }
    }

    LOG.info("New dataset imported with id... " + datasetBean.getId());
    context.setJson("{\"success\":true,\"datasetId\":" + datasetBean.getId() + "}");
    return context;
  }

  private String escapeJson(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
