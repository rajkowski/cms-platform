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

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.Part;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.thymeleaf.util.StringUtils;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveImageCommand;
import com.simisinc.platform.application.cms.ValidateImageCommand;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles multiple image uploads for the image editor
 *
 * @author matt rajkowski
 * @created 2/3/26 8:00 PM
 */
public class ImageUploadAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(ImageUploadAjax.class);

  /**
   * Handles multiple image uploads
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext post(WidgetContext context) {

    // /json/imageUpload
    LOG.debug("ImageUploadAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\": false, \"error\": \"You do not have permission to upload images.\"}");
      context.setSuccess(false);
      return context;
    }

    StringBuilder jsonResponse = new StringBuilder();
    jsonResponse.append("{");
    jsonResponse.append("\"success\": true,");
    jsonResponse.append("\"images\": [");

    try {
      // Get all file parts from the request
      Collection<Part> fileParts = context.getParts();
      List<Image> uploadedImages = new ArrayList<>();

      // Prepare server paths
      String serverSubPath = FileSystemCommand.generateFileServerSubPath("images");
      String serverRootPath = FileSystemCommand.getFileServerRootPathValue();
      String serverCompletePath = serverRootPath + serverSubPath;

      // Process each file part
      for (Part filePart : fileParts) {
        // Skip non-file parts (like token, widget)
        String submittedFilename = null;
        try {
          submittedFilename = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        } catch (Exception e) {
          // Not a file part, skip it
          continue;
        }

        if (submittedFilename == null || submittedFilename.isEmpty()) {
          continue;
        }

        // Handle TinyMCE clipboard filenames
        if (submittedFilename.startsWith("mceclip0")) {
          submittedFilename = StringUtils.replace(submittedFilename, "mceclip0", "clip");
        }

        String extension = FilenameUtils.getExtension(submittedFilename);
        String uniqueFilename = FileSystemCommand.generateUniqueFilename(context.getUserId());
        File tempFile = null;
        long fileLength = 0;

        try {
          tempFile = new File(serverCompletePath + uniqueFilename + "." + extension);
          fileLength = filePart.getSize();

          if (fileLength <= 0) {
            LOG.warn("Skipping empty file: " + submittedFilename);
            continue;
          }

          // Write the file
          filePart.write(serverCompletePath + uniqueFilename + "." + extension);

          // Populate the image bean
          Image imageBean = new Image();
          imageBean.setFilename(submittedFilename);
          imageBean.setFileLength(fileLength);
          imageBean.setFileServerPath(serverSubPath + uniqueFilename + "." + extension);
          imageBean.setCreatedBy(context.getUserId());

          // Validate the image
          ValidateImageCommand.checkFile(imageBean);

          // Determine if this is a new version or an update
          
          // Save the record
          Image image = SaveImageCommand.saveImage(imageBean);
          if (image == null) {
            LOG.warn("Failed to save image: " + submittedFilename);
            if (tempFile != null && tempFile.exists()) {
              tempFile.delete();
            }
            continue;
          }

          uploadedImages.add(image);
          LOG.debug("Uploaded image: " + image.getFilename() + " with ID: " + image.getId());

        } catch (DataException e) {
          LOG.warn("Failed to save image: " + submittedFilename + " - " + e.getMessage());
          // Clean up the file
          if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
          }
          // Continue with next file
        } catch (Exception e) {
          LOG.error("Error processing image: " + submittedFilename, e);
          // Clean up the file
          if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
          }
          // Continue with next file
        }
      }

      if (uploadedImages.isEmpty()) {
        throw new DataException("No valid image files were found in the request");
      }

      // Build JSON response
      boolean firstImage = true;
      for (Image image : uploadedImages) {
        if (!firstImage) {
          jsonResponse.append(",");
        }
        firstImage = false;

        jsonResponse.append("{");
        jsonResponse.append("\"id\": ").append(image.getId()).append(",");
        jsonResponse.append("\"filename\": \"").append(JsonCommand.toJson(image.getFilename())).append("\",");
        jsonResponse.append("\"location\": \"").append(JsonCommand.toJson("/assets/img/" + image.getUrl())).append("\",");
        jsonResponse.append("\"size\": ").append(image.getFileLength());
        jsonResponse.append("}");
      }

      jsonResponse.append("]");
      jsonResponse.append("}");

    } catch (DataException e) {
      LOG.warn("Error during image upload: " + e.getMessage());
      jsonResponse = new StringBuilder();
      jsonResponse.append("{");
      jsonResponse.append("\"success\": false,");
      jsonResponse.append("\"error\": \"").append(JsonCommand.toJson(e.getMessage())).append("\"");
      jsonResponse.append("}");
      context.setJson(jsonResponse.toString());
      context.setSuccess(false);
      return context;
    } catch (Exception e) {
      LOG.error("Unexpected error during image upload: " + e.getMessage(), e);
      jsonResponse = new StringBuilder();
      jsonResponse.append("{");
      jsonResponse.append("\"success\": false,");
      jsonResponse.append("\"error\": \"An unexpected error occurred during image upload\"");
      jsonResponse.append("}");
      context.setJson(jsonResponse.toString());
      context.setSuccess(false);
      return context;
    }

    context.setJson(jsonResponse.toString());
    return context;
  }
}
