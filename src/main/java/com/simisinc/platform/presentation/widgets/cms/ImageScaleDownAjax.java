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
import com.simisinc.platform.application.cms.ScaleDownImageCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Scales down an image proportionally and saves it as a new version
 *
 * @author matt rajkowski
 * @created 2/4/26 11:30 PM
 */
public class ImageScaleDownAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(ImageScaleDownAjax.class);

  public WidgetContext post(WidgetContext context) {

    LOG.debug("ImageScaleDownAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      LOG.warn("User does not have permission to scale images");
      context.setJson("{\"status\":\"error\",\"message\":\"Permission denied\"}");
      return context;
    }

    // Get the image ID
    long imageId = context.getParameterAsLong("imageId", -1);
    if (imageId == -1) {
      LOG.warn("Image ID not provided");
      context.setJson("{\"status\":\"error\",\"message\":\"Image ID is required\"}");
      return context;
    }

    // Get the scale percentage
    int scalePercentage = context.getParameterAsInt("scalePercentage", -1);
    if (scalePercentage == -1 || scalePercentage >= 100 || scalePercentage < 10) {
      LOG.warn("Invalid scale percentage: " + scalePercentage);
      context.setJson("{\"status\":\"error\",\"message\":\"Scale percentage must be between 10 and 99\"}");
      return context;
    }

    // Load the image
    Image image = ImageRepository.findById(imageId);
    if (image == null) {
      LOG.warn("Image not found: " + imageId);
      context.setJson("{\"status\":\"error\",\"message\":\"Image not found\"}");
      return context;
    }

    try {
      // Scale the image down
      Image scaledImage = ScaleDownImageCommand.scaleDownImage(image, scalePercentage);

      // Build success response with scaled image details
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"status\":\"success\",");
      sb.append("\"message\":\"Image scaled down successfully\",");
      sb.append("\"image\":{");
      sb.append("\"id\":").append(scaledImage.getId()).append(",");
      sb.append("\"width\":").append(scaledImage.getWidth()).append(",");
      sb.append("\"height\":").append(scaledImage.getHeight()).append(",");
      sb.append("\"fileLength\":").append(scaledImage.getFileLength()).append(",");
      sb.append("\"fileType\":\"").append(JsonCommand.toJson(scaledImage.getFileType())).append("\"");
      sb.append("}");
      sb.append("}");

      context.setJson(sb.toString());
      LOG.info("Image scaled down to " + scalePercentage + "% for image " + imageId);

    } catch (DataException e) {
      LOG.error("Error scaling image " + imageId, e);
      context.setJson("{\"status\":\"error\",\"message\":\"" + JsonCommand.toJson(e.getMessage()) + "\"}");
    }

    return context;
  }
}
