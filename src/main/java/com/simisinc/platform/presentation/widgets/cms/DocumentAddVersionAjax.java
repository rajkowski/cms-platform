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

import java.util.Collection;
import java.util.List;

import javax.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.CheckFolderPermissionCommand;
import com.simisinc.platform.application.cms.SaveFileCommand;
import com.simisinc.platform.application.cms.SaveFilePartCommand;
import com.simisinc.platform.application.cms.ValidateFileCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Uploads a new version of an existing file in the visual document editor
 *
 * @author matt rajkowski
 * @created 2/19/26
 */
public class DocumentAddVersionAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908904L;
  private static Log LOG = LogFactory.getLog(DocumentAddVersionAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("DocumentAddVersionAjax...");

    long fileId = context.getParameterAsLong("fileId", -1);
    if (fileId == -1) {
      context.setJson("{\"success\":false,\"message\":\"File ID required\"}");
      context.setSuccess(false);
      return context;
    }

    FileItem existingFile = FileItemRepository.findById(fileId);
    if (existingFile == null) {
      context.setJson("{\"success\":false,\"message\":\"File not found\"}");
      context.setSuccess(false);
      return context;
    }

    if (!context.hasRole("admin") &&
        !CheckFolderPermissionCommand.userHasEditPermission(existingFile.getFolderId(), context.getUserId())) {
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    String newVersion = StringUtils.trimToNull(context.getParameter("version"));

    try {
      Collection<Part> fileParts;
      try {
        fileParts = context.getParts();
      } catch (Exception e) {
        LOG.warn("Error reading file parts: " + e.getMessage());
        context.setJson("{\"success\":false,\"message\":\"Error reading uploaded file\"}");
        context.setSuccess(false);
        return context;
      }
      List<FileItem> uploadedFiles = SaveFilePartCommand.saveFiles(fileParts, context.getUserId());

      if (uploadedFiles.isEmpty()) {
        context.setJson("{\"success\":false,\"message\":\"No file was uploaded\"}");
        context.setSuccess(false);
        return context;
      }

      FileItem uploadedFile = uploadedFiles.get(0);
      // Copy identifying info from the uploaded file to become new version
      FileItem fileItemBean = new FileItem();
      fileItemBean.setId(existingFile.getId());
      fileItemBean.setFolderId(existingFile.getFolderId());
      fileItemBean.setSubFolderId(existingFile.getSubFolderId());
      fileItemBean.setCategoryId(existingFile.getCategoryId());
      fileItemBean.setFilename(existingFile.getFilename());
      fileItemBean.setTitle(existingFile.getTitle());
      fileItemBean.setSummary(existingFile.getSummary());
      fileItemBean.setVersion(StringUtils.defaultIfBlank(newVersion, "1.0"));
      fileItemBean.setExtension(uploadedFile.getExtension());
      fileItemBean.setFileServerPath(uploadedFile.getFileServerPath());
      fileItemBean.setFileLength(uploadedFile.getFileLength());
      fileItemBean.setFileType(uploadedFile.getFileType());
      fileItemBean.setMimeType(uploadedFile.getMimeType());
      fileItemBean.setFileHash(uploadedFile.getFileHash());
      fileItemBean.setWidth(uploadedFile.getWidth());
      fileItemBean.setHeight(uploadedFile.getHeight());
      fileItemBean.setCreatedBy(context.getUserId());
      fileItemBean.setModifiedBy(context.getUserId());

      // Validate
      ValidateFileCommand.checkFile(fileItemBean);

      // Save as new version
      FileItem saved = SaveFileCommand.saveNewVersionOfFile(fileItemBean);
      if (saved == null) {
        SaveFilePartCommand.cleanupFile(uploadedFile);
        context.setJson("{\"success\":false,\"message\":\"Failed to save new version\"}");
        context.setSuccess(false);
        return context;
      }

      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"success\":true,");
      sb.append("\"id\":").append(saved.getId()).append(",");
      sb.append("\"version\":\"").append(JsonCommand.toJson(StringUtils.defaultString(saved.getVersion()))).append("\"");
      sb.append("}");
      context.setJson(sb.toString());

    } catch (DataException e) {
      LOG.warn("Error adding version: " + e.getMessage());
      context.setJson("{\"success\":false,\"message\":\"" + JsonCommand.toJson(e.getMessage()) + "\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
