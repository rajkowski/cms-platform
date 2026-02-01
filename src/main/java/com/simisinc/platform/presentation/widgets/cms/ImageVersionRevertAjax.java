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

import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Reverts an image to a previous version in the visual image editor
 *
 * @author matt rajkowski
 * @created 1/31/26 9:45 AM
 */
public class ImageVersionRevertAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908897L;
  private static Log LOG = LogFactory.getLog(ImageVersionRevertAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("ImageVersionRevertAjax...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\": false, \"error\": \"Access denied\"}");
      context.setSuccess(false);
      return context;
    }

    long imageId = context.getParameterAsLong("imageId", -1);
    long versionId = context.getParameterAsLong("versionId", -1);

    if (imageId == -1 || versionId == -1) {
      context.setJson("{\"success\": false, \"error\": \"Image ID and Version ID required\"}");
      context.setSuccess(false);
      return context;
    }

    Image image = ImageRepository.findById(imageId);
    if (image == null) {
      context.setJson("{\"success\": false, \"error\": \"Image not found\"}");
      context.setSuccess(false);
      return context;
    }

    ImageVersion version = ImageVersionRepository.findById(versionId);
    if (version == null || version.getImageId() != imageId) {
      context.setJson("{\"success\": false, \"error\": \"Version not found\"}");
      context.setSuccess(false);
      return context;
    }

    try {
      // Mark all versions as not current
      ImageVersionRepository.markAsNotCurrent(imageId);

      // Mark this version as current
      version.setIsCurrent(true);
      ImageVersionRepository.save(version);

      // Update image with version details
      image.setFilename(version.getFilename());
      image.setFileServerPath(version.getFileServerPath());
      image.setFileLength(version.getFileLength());
      image.setFileType(version.getFileType());
      image.setWidth(version.getWidth());
      image.setHeight(version.getHeight());
      image.setVersionNumber(version.getVersionNumber());
      
      Image saved = ImageRepository.save(image);
      if (saved != null) {
        context.setJson("{\"success\": true, \"message\": \"Reverted to version " + version.getVersionNumber() + "\"}");
      } else {
        context.setJson("{\"success\": false, \"error\": \"Failed to update image\"}");
        context.setSuccess(false);
      }
    } catch (Exception e) {
      LOG.error("Error reverting version", e);
      context.setJson("{\"success\": false, \"error\": \"Failed to revert: " + e.getMessage() + "\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
