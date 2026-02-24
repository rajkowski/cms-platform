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
import com.simisinc.platform.domain.model.cms.FolderGroup;
import com.simisinc.platform.infrastructure.persistence.cms.FolderGroupRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FolderRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Saves folder group permissions in the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 11:15 AM
 */
public class FolderGroupSaveJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908896L;
  private static Log LOG = LogFactory.getLog(FolderGroupSaveJsonService.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("FolderGroupSaveAjax...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\": false, \"error\": \"Access denied\"}");
      context.setSuccess(false);
      return context;
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    long groupId = context.getParameterAsLong("groupId", -1);
    int privacyType = context.getParameterAsInt("privacyType", 0);
    boolean addPermission = "true".equals(context.getParameter("addPermission"));
    boolean editPermission = "true".equals(context.getParameter("editPermission"));
    boolean deletePermission = "true".equals(context.getParameter("deletePermission"));

    if (folderId == -1 || groupId == -1) {
      context.setJson("{\"success\":false, \"error\": \"Folder ID and Group ID required\"}");
      context.setSuccess(false);
      return context;
    }

    Folder folder = FolderRepository.findById(folderId);
    if (folder == null) {
      context.setJson("{\"success\":false, \"error\": \"Folder not found\"}");
      context.setSuccess(false);
      return context;
    }

    long id = context.getParameterAsLong("id", -1);
    FolderGroup folderGroup;

    if (id > 0) {
      // Update existing
      folderGroup = new FolderGroup();
      folderGroup.setId(id);
      folderGroup.setFolderId(folderId);
      folderGroup.setGroupId(groupId);
      folderGroup.setPrivacyType(privacyType);
      folderGroup.setAddPermission(addPermission);
      folderGroup.setEditPermission(editPermission);
      folderGroup.setDeletePermission(deletePermission);

      if (FolderGroupRepository.update(folderGroup)) {
        context.setJson("{\"success\":true, \"message\": \"Group access updated\"}");
      } else {
        context.setJson("{\"success\":false, \"message\": \"Failed to update group access\"}");
        context.setSuccess(false);
      }
    } else {

      // Create new
      folderGroup = new FolderGroup();
      folderGroup.setFolderId(folderId);
      folderGroup.setGroupId(groupId);
      folderGroup.setPrivacyType(privacyType);
      folderGroup.setAddPermission(addPermission);
      folderGroup.setEditPermission(editPermission);
      folderGroup.setDeletePermission(deletePermission);

      FolderGroup saved = FolderGroupRepository.add(folderGroup);
      if (saved != null && saved.getId() > 0) {
        context.setJson("{\"success\":true, \"message\": \"Group access added\", \"id\": " + saved.getId() + "}");
      } else {
        context.setJson("{\"success\":false, \"message\": \"Failed to add group access\"}");
        context.setSuccess(false);
      }
    }

    return context;
  }
}
