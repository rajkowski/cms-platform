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
import com.simisinc.platform.application.cms.DeleteFileCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Deletes a file item in the visual document editor
 *
 * @author matt rajkowski
 * @created 2/19/26
 */
public class DocumentDeleteFileAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908906L;
  private static Log LOG = LogFactory.getLog(DocumentDeleteFileAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("DocumentDeleteFileAjax...");

    long fileId = context.getParameterAsLong("fileId", -1);
    if (fileId == -1) {
      context.setJson("{\"success\":false,\"message\":\"File ID required\"}");
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
        !CheckFolderPermissionCommand.userHasDeletePermission(fileItem.getFolderId(), context.getUserId())) {
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    try {
      boolean deleted = DeleteFileCommand.deleteFile(fileItem);
      if (deleted) {
        context.setJson("{\"success\":true,\"message\":\"File deleted\"}");
      } else {
        context.setJson("{\"success\":false,\"message\":\"Failed to delete file\"}");
        context.setSuccess(false);
      }
    } catch (DataException e) {
      LOG.warn("Error deleting file: " + e.getMessage());
      context.setJson("{\"success\":false,\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
