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

import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Deletes an image version in the visual image editor
 *
 * @author matt rajkowski
 * @created 1/31/26 9:50 AM
 */
public class ImageVersionDeleteAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908898L;
  private static Log LOG = LogFactory.getLog(ImageVersionDeleteAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("ImageVersionDeleteAjax...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\": false, \"error\": \"Access denied\"}");
      context.setSuccess(false);
      return context;
    }

    long versionId = context.getParameterAsLong("versionId", -1);
    if (versionId == -1) {
      context.setJson("{\"success\": false, \"error\": \"Version 'ID' required\"}");
      context.setSuccess(false);
      return context;
    }

    ImageVersion version = ImageVersionRepository.findById(versionId);
    if (version == null) {
      context.setJson("{\"success\": false, \"error\": \"Version not found\"}");
      context.setSuccess(false);
      return context;
    }

    // Prevent deleting the current version
    if (version.getIsCurrent()) {
      context.setJson("{\"success\": false, \"error\": \"Cannot delete the current version\"}");
      context.setSuccess(false);
      return context;
    }

    try {
      ImageVersionRepository.remove(version);
      context.setJson("{\"success\": true, \"message\": \"Version deleted successfully\"}");
    } catch (Exception e) {
      LOG.error("Error deleting version", e);
      context.setJson("{\"success\": false, \"error\": \"Failed to delete version: " + e.getMessage() + "\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
