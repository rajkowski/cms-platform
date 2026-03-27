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
import com.simisinc.platform.infrastructure.persistence.cms.FolderGroupRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Deletes folder group permissions in the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 11:20 AM
 */
public class FolderGroupDeleteJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908897L;
  private static Log LOG = LogFactory.getLog(FolderGroupDeleteJsonService.class);

  @Override
  public JsonServiceContext post(JsonServiceContext context) {

    LOG.debug("FolderGroupDeleteAjax...");

    // Restrict access to editors
    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + FolderGroupDeleteJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long id = context.getParameterAsLong("id", -1);
    if (id == -1) {
      context.setJson("{\"success\": false, \"error\": \"Folder Group ID required\"}");
      context.setSuccess(false);
      return context;
    }

    if (FolderGroupRepository.remove(id)) {
      context.setJson("{\"success\": true, \"message\": \"Group access removed\"}");
    } else {
      context.setJson("{\"success\": false, \"message\": \"Failed to remove group access\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
