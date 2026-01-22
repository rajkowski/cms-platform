/*
 * Copyright 2025 Matt Rajkowski
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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Returns a single image's data for the visual image editor
 * Only returns images the user has permission to edit
 *
 * @author matt rajkowski
 * @created 1/21/26 9:15 PM
 */
public class ImageContentAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(ImageContentAjax.class);

  public WidgetContext execute(WidgetContext context) {

    LOG.debug("ImageContentAjax...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      LOG.debug("No permission to access image content");
      context.setJson("{}");
      return context;
    }

    // Get the image ID
    long imageId = context.getParameterAsLong("imageId", -1);
    if (imageId == -1) {
      LOG.debug("Image ID not provided");
      context.setJson("{\"error\":\"Image ID required\"}");
      return context;
    }

    // Retrieve the image
    Image image = ImageRepository.findById(imageId);
    if (image == null) {
      LOG.debug("Image not found for ID: " + imageId);
      context.setJson("{\"error\":\"Image not found\"}");
      return context;
    }

    // Build JSON response with image details
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"id\":").append(image.getId()).append(",");
    sb.append("\"filename\":\"").append(JsonCommand.toJson(image.getFilename())).append("\",");
    sb.append("\"url\":\"").append(JsonCommand.toJson(image.getUrl())).append("\",");
    sb.append("\"width\":").append(image.getWidth()).append(",");
    sb.append("\"height\":").append(image.getHeight()).append(",");
    sb.append("\"fileLength\":").append(image.getFileLength()).append(",");
    sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(image.getFileType()))).append("\",");
    
    // Format timestamps
    if (image.getCreated() != null) {
      sb.append("\"created\":\"").append(JsonCommand.toJson(image.getCreated().toString())).append("\",");
    } else {
      sb.append("\"created\":null,");
    }
    
    if (image.getProcessed() != null) {
      sb.append("\"processed\":\"").append(JsonCommand.toJson(image.getProcessed().toString())).append("\"");
    } else {
      sb.append("\"processed\":null");
    }
    
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
