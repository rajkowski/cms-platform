/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
import com.simisinc.platform.application.cms.CheckFolderPermissionCommand;
import com.simisinc.platform.application.cms.SaveFileCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FolderRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Moves a file to a different repo, parent folder, or subfolder in the visual document editor
 *
 * @author matt rajkowski
 * @created 2/19/26
 */
public class DocumentMoveFileAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908905L;
  private static Log LOG = LogFactory.getLog(DocumentMoveFileAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("DocumentMoveFileAjax...");

    long fileId = context.getParameterAsLong("fileId", -1);
    long targetFolderId = context.getParameterAsLong("targetFolderId", -1);
    long targetSubFolderId = context.getParameterAsLong("targetSubFolderId", -1);

    if (fileId == -1) {
      context.setJson("{\"success\":false,\"message\":\"File ID required\"}");
      context.setSuccess(false);
      return context;
    }

    if (targetFolderId == -1) {
      context.setJson("{\"success\":false,\"message\":\"Target folder ID required\"}");
      context.setSuccess(false);
      return context;
    }

    FileItem fileItem = FileItemRepository.findById(fileId);
    if (fileItem == null) {
      context.setJson("{\"success\":false,\"message\":\"File not found\"}");
      context.setSuccess(false);
      return context;
    }

    if (!context.hasRole("admin") &&
        !CheckFolderPermissionCommand.userHasEditPermission(fileItem.getFolderId(), context.getUserId())) {
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    Folder targetFolder = FolderRepository.findById(targetFolderId);
    if (targetFolder == null) {
      context.setJson("{\"success\":false,\"message\":\"Target folder not found\"}");
      context.setSuccess(false);
      return context;
    }

    // Update folder and subfolder
    fileItem.setFolderId(targetFolderId);
    if (targetSubFolderId > 0) {
      fileItem.setSubFolderId(targetSubFolderId);
    } else {
      fileItem.setSubFolderId(-1);
    }
    fileItem.setModifiedBy(context.getUserId());

    try {
      FileItem saved = SaveFileCommand.saveFile(fileItem);
      if (saved != null) {
        context.setJson("{\"success\":true,\"message\":\"File moved\"}");
      } else {
        context.setJson("{\"success\":false,\"message\":\"Failed to move file\"}");
        context.setSuccess(false);
      }
    } catch (DataException e) {
      LOG.warn("Error moving file: " + e.getMessage());
      context.setJson("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
