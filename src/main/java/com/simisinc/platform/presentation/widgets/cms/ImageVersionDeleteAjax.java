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

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Deletes an image version in the visual image editor
 *
 * @author matt rajkowski
 * @created 1/31/26 9:50 AM
 */
public class ImageVersionDeleteAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908898L;
  private static Log LOG = LogFactory.getLog(ImageVersionDeleteAjax.class);

  @Override
  public JsonServiceContext post(JsonServiceContext context) {

    LOG.debug("ImageVersionDeleteAjax...");

    // Restrict access to editors
    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + ImageVersionDeleteAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long versionId = context.getParameterAsLong("versionId", -1);
    if (versionId == -1) {
      return context.writeError("Version ID required");
    }

    ImageVersion version = ImageVersionRepository.findById(versionId);
    if (version == null) {
      return context.writeError("Version not found");
    }

    // Prevent deleting the current version
    if (version.getIsCurrent()) {
      return context.writeError("Cannot delete the current version");
    }

    try {
      ImageVersionRepository.remove(version);
      context.setJson("{\"success\": true, \"message\": \"Version deleted successfully\"}");
      return context;
    } catch (Exception e) {
      LOG.error("Error deleting version", e);
      return context.writeError("Failed to delete version: " + e.getMessage());
    }
  }
}
