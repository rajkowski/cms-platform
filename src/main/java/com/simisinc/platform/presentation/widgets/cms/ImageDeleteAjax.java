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

import com.simisinc.platform.application.cms.DeleteImageCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Deletes an image in the visual image editor
 *
 * @author matt rajkowski
 * @created 1/31/26 9:35 AM
 */
public class ImageDeleteAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(ImageDeleteAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("ImageDeleteAjax...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\": false, \"error\": \"Access denied\"}");
      context.setSuccess(false);
      return context;
    }

    long imageId = context.getParameterAsLong("id", -1);
    if (imageId == -1) {
      context.setJson("{\"success\": false, \"error\": \"Image 'ID' required\"}");
      context.setSuccess(false);
      return context;
    }

    Image image = ImageRepository.findById(imageId);
    if (image == null) {
      context.setJson("{\"success\": false, \"error\": \"Image not found\"}");
      context.setSuccess(false);
      return context;
    }

    try {
      // Use the command to delete the file from the fileLibrary, versions, and database record
      DeleteImageCommand.deleteImage(image);
      context.setJson("{\"success\": true, \"message\": \"Image deleted successfully\"}");
    } catch (Exception e) {
      LOG.error("Error deleting image", e);
      context.setJson("{\"success\": false, \"error\": \"Failed to delete image: " + e.getMessage() + "\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
