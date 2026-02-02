/*
 * Copyright 2026 Matt Rajkowski
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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.Part;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.CheckFolderPermissionCommand;
import com.simisinc.platform.application.cms.SaveFileCommand;
import com.simisinc.platform.application.cms.SaveFilePartCommand;
import com.simisinc.platform.application.cms.ValidateFileCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles multiple file uploads for the document editor
 *
 * @author matt rajkowski
 * @created 2/1/26 8:50 AM
 */
public class DocumentUploadAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(DocumentUploadAjax.class);

  /**
   * Handles file uploads
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext post(WidgetContext context) {

    LOG.debug("DocumentUploadAjax...");

    // Check permissions
    long folderId = context.getParameterAsLong("folderId", -1);
    long subFolderId = context.getParameterAsLong("subFolderId", -1);

    if (!context.hasRole("admin") && 
        !CheckFolderPermissionCommand.userHasAddPermission(folderId, context.getUserId())) {
      context.setJson("{\"success\": false, \"error\": \"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    StringBuilder jsonResponse = new StringBuilder();
    jsonResponse.append("{");
    jsonResponse.append("\"success\": true,");
    jsonResponse.append("\"files\": [");

    try {
      // Get all file parts from the request
      Collection<Part> fileParts = context.getParts();
      List<FileItem> uploadedFileItems = SaveFilePartCommand.saveFiles(fileParts, context.getUserId());

      if (uploadedFileItems.isEmpty()) {
        throw new DataException("No files were found in the request");
      }

      // Process and save each file
      boolean firstFile = true;
      for (FileItem fileItemBean : uploadedFileItems) {
        try {
          // Set folder and subfolder
          fileItemBean.setFolderId(folderId);
          fileItemBean.setSubFolderId(subFolderId);
          fileItemBean.setVersion("1.0");

          // Validate the file
          ValidateFileCommand.checkFile(fileItemBean);

          // Save it
          FileItem fileItem = SaveFileCommand.saveFile(fileItemBean);
          if (fileItem == null) {
            LOG.warn("Failed to save file: " + fileItemBean.getFilename());
            continue;
          }

          if (!firstFile) {
            jsonResponse.append(",");
          }
          firstFile = false;

          // Add file to response
          jsonResponse.append("{");
          jsonResponse.append("\"id\": ").append(fileItem.getId()).append(",");
          jsonResponse.append("\"filename\": \"").append(JsonCommand.toJson(fileItem.getFilename())).append("\",");
          jsonResponse.append("\"location\": \"").append(JsonCommand.toJson("/assets/file/" + fileItem.getUrl())).append("\",");
          jsonResponse.append("\"size\": ").append(fileItem.getFileLength());
          jsonResponse.append("}");

          LOG.debug("Uploaded file: " + fileItem.getFilename() + " with ID: " + fileItem.getId());

        } catch (DataException e) {
          LOG.warn("Failed to save file: " + e.getMessage());
          // Clean up the file
          SaveFilePartCommand.cleanupFile(fileItemBean);
          // Continue with next file
        }
      }

      jsonResponse.append("]");
      jsonResponse.append("}");

    } catch (DataException e) {
      LOG.warn("Error during file upload: " + e.getMessage());
      jsonResponse = new StringBuilder();
      jsonResponse.append("{");
      jsonResponse.append("\"success\": false,");
      jsonResponse.append("\"error\": \"").append(JsonCommand.toJson(e.getMessage())).append("\"");
      jsonResponse.append("}");
      context.setJson(jsonResponse.toString());
      context.setSuccess(false);
      return context;
    } catch (Exception e) {
      LOG.error("Unexpected error during file upload: " + e.getMessage(), e);
      jsonResponse = new StringBuilder();
      jsonResponse.append("{");
      jsonResponse.append("\"success\": false,");
      jsonResponse.append("\"error\": \"An unexpected error occurred during file upload\"");
      jsonResponse.append("}");
      context.setJson(jsonResponse.toString());
      context.setSuccess(false);
      return context;
    }

    context.setJson(jsonResponse.toString());
    return context;
  }
}
