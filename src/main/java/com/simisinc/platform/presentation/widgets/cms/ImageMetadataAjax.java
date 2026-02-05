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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Saves image metadata (title, alt text, description) in the visual image editor
 *
 * @author matt rajkowski
 * @created 1/31/26 9:30 AM
 */
public class ImageMetadataAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(ImageMetadataAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("ImageMetadataAjax...");

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

    image.setTitle(StringUtils.trimToNull(context.getParameter("title")));
    image.setFilename(StringUtils.trimToNull(context.getParameter("filename")));
    image.setAltText(StringUtils.trimToNull(context.getParameter("altText")));
    image.setDescription(StringUtils.trimToNull(context.getParameter("description")));
    image.setModifiedBy(context.getUserId());

    Image saved = ImageRepository.save(image);
    if (saved != null) {
      context.setJson("{\"success\": true, \"message\": \"Image metadata saved successfully\"}");
    } else {
      context.setJson("{\"success\": false, \"message\": \"Failed to save image metadata\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
