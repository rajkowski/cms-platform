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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.cms.GenerateThumbnailCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Generates a thumbnail for an image
 *
 * @author matt rajkowski
 * @created 1/31/26 4:45 PM
 */
public class ImageThumbnailAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(ImageThumbnailAjax.class);

  public JsonServiceContext post(JsonServiceContext context) {

    LOG.debug("ImageThumbnailAjax...");

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + ImageThumbnailAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    // Get the image ID
    long imageId = context.getParameterAsLong("imageId", -1);
    if (imageId == -1) {
      LOG.warn("Image ID not provided");
      return context.writeError("Image ID required");
    }

    // Load the image
    Image image = ImageRepository.findById(imageId);
    if (image == null) {
      LOG.warn("Image not found: " + imageId);
      return context.writeError("Image not found");
    }

    try {
      // Generate the thumbnail
      Image updatedImage = GenerateThumbnailCommand.generateThumbnail(image);

      // Build success response with thumbnail details
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"status\":\"success\",");
      sb.append("\"message\":\"Thumbnail generated successfully\",");
      sb.append("\"thumbnail\":{");
      sb.append("\"width\":").append(updatedImage.getProcessedWidth()).append(",");
      sb.append("\"height\":").append(updatedImage.getProcessedHeight()).append(",");
      sb.append("\"fileLength\":").append(updatedImage.getProcessedFileLength()).append(",");
      sb.append("\"fileType\":\"").append(JsonCommand.toJson(updatedImage.getProcessedFileType())).append("\"");
      
      if (updatedImage.getThumbnailUrl() != null) {
        sb.append(",\"url\":\"").append(JsonCommand.toJson("/assets/img/" + updatedImage.getThumbnailUrl())).append("\"");
      }
      
      sb.append("}");
      sb.append("}");

      context.setJson(sb.toString());
      LOG.info("Thumbnail generated for image " + imageId);

    } catch (DataException e) {
      LOG.error("Error generating thumbnail for image " + imageId, e);
      return context.writeError("Failed to generate thumbnail: " + e.getMessage());
    }

    return context;
  }
}
