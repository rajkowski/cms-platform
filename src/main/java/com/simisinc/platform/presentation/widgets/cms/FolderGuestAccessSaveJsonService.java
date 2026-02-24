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

import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.infrastructure.persistence.cms.FolderRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Saves folder guest access settings in the visual document editor
 *
 * @author matt rajkowski
 * @created 2/23/26 7:00 PM
 */
public class FolderGuestAccessSaveJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(FolderGuestAccessSaveJsonService.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("FolderGuestAccessSaveJsonService...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\": false, \"error\": \"Access denied\"}");
      context.setSuccess(false);
      return context;
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    if (folderId == -1) {
      context.setJson("{\"success\": false, \"error\": \"Folder ID required\"}");
      context.setSuccess(false);
      return context;
    }

    Folder folder = FolderRepository.findById(folderId);
    if (folder == null) {
      context.setJson("{\"success\": false, \"error\": \"Folder not found\"}");
      context.setSuccess(false);
      return context;
    }

    int guestPrivacyType = context.getParameterAsInt("guestPrivacyType", -1);

    if (FolderRepository.updateGuestAccess(folderId, guestPrivacyType)) {
      context.setJson("{\"success\": true, \"message\": \"Guest access saved\"}");
    } else {
      context.setJson("{\"success\": false, \"message\": \"Failed to save guest access\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
