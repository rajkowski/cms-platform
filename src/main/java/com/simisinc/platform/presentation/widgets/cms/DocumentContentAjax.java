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

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Returns a single file's metadata for the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 10:20 AM
 */
public class DocumentContentAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(DocumentContentAjax.class);
  private static final String FILE_NOT_FOUND = "File not found";

  @Override
  public JsonServiceContext get(JsonServiceContext context) {

    LOG.debug("DocumentContentAjax...");

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + DocumentContentAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long fileId = context.getParameterAsLong("fileId", -1);
    String sourceType = context.getParameter("sourceType");
    if (fileId == -1) {
      return context.writeError("File ID required");
    }

    if ("collection".equalsIgnoreCase(sourceType)) {
      return getCollectionFile(context, fileId);
    }

    FileItem fileItem = FileItemRepository.findById(fileId);
    if (fileItem == null) {
      return context.writeError(FILE_NOT_FOUND);
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"id\":").append(fileItem.getId()).append(",");
    sb.append("\"folderId\":").append(fileItem.getFolderId()).append(",");
    sb.append("\"subFolderId\":").append(fileItem.getSubFolderId()).append(",");
    sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getTitle()))).append("\",");
    sb.append("\"filename\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getFilename()))).append("\",");
    sb.append("\"version\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getVersion()))).append("\",");
    sb.append("\"mimeType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getMimeType()))).append("\",");
    sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getFileType()))).append("\",");
    sb.append("\"fileLength\":").append(fileItem.getFileLength()).append(",");
    sb.append("\"url\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getUrl()))).append("\",");
    sb.append("\"versionCount\":").append(fileItem.getVersionCount()).append(",");
    sb.append("\"downloadCount\":").append(fileItem.getDownloadCount()).append(",");
    sb.append("\"summary\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getSummary()))).append("\",");
    sb.append("\"tags\":\"").append(JsonCommand.toJson(fileItem.getTags() != null ? StringUtils.join(fileItem.getTags(), ", ") : "")).append("\",");
    sb.append("\"created\":\"").append(fileItem.getCreated() != null ? JsonCommand.toJson(fileItem.getCreated().toString()) : "")
        .append("\",");
    sb.append("\"modified\":\"").append(fileItem.getModified() != null ? JsonCommand.toJson(fileItem.getModified().toString()) : "")
        .append("\"");
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }

  private JsonServiceContext getCollectionFile(JsonServiceContext context, long fileId) {
    ItemFileItem fileItem = ItemFileItemRepository.findById(fileId);
    if (fileItem == null) {
      return context.writeError(FILE_NOT_FOUND);
    }

    Item item = LoadItemCommand.loadItemById(fileItem.getItemId());
    if (item == null) {
      return context.writeError(FILE_NOT_FOUND);
    }

    Collection collection;
    if (context.hasRole("admin")) {
      collection = LoadCollectionCommand.loadCollectionById(item.getCollectionId());
    } else {
      collection = LoadCollectionCommand.loadCollectionByIdForAuthorizedUser(item.getCollectionId(), context.getUserId());
    }
    if (collection == null) {
      return context.writeError("Permission Denied");
    }

    String fileUrl = StringUtils.defaultString(fileItem.getUrl());
    String downloadUrl = "/show/" + item.getUniqueId() + "/assets/file/" + fileUrl;
    String viewUrl = "/show/" + item.getUniqueId() + "/assets/view/" + fileUrl;

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"id\":").append(fileItem.getId()).append(",");
    sb.append("\"sourceType\":\"collection\",");
    sb.append("\"collectionId\":").append(collection.getId()).append(",");
    sb.append("\"itemId\":").append(item.getId()).append(",");
    sb.append("\"itemUniqueId\":\"").append(JsonCommand.toJson(StringUtils.defaultString(item.getUniqueId()))).append("\",");
    sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getTitle()))).append("\",");
    sb.append("\"filename\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getFilename()))).append("\",");
    sb.append("\"version\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getVersion()))).append("\",");
    sb.append("\"mimeType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getMimeType()))).append("\",");
    sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getFileType()))).append("\",");
    sb.append("\"fileLength\":").append(fileItem.getFileLength()).append(",");
    sb.append("\"url\":\"").append(JsonCommand.toJson(fileUrl)).append("\",");
    sb.append("\"downloadUrl\":\"").append(JsonCommand.toJson(downloadUrl)).append("\",");
    sb.append("\"viewUrl\":\"").append(JsonCommand.toJson(viewUrl)).append("\",");
    sb.append("\"versionCount\":").append(fileItem.getVersionCount()).append(",");
    sb.append("\"downloadCount\":").append(fileItem.getDownloadCount()).append(",");
    sb.append("\"summary\":\"").append(JsonCommand.toJson(StringUtils.defaultString(fileItem.getSummary()))).append("\",");
    sb.append("\"tags\":\"").append(JsonCommand.toJson(fileItem.getTags() != null ? StringUtils.join(fileItem.getTags(), ", ") : "")).append("\",");
    sb.append("\"created\":\"").append(fileItem.getCreated() != null ? JsonCommand.toJson(fileItem.getCreated().toString()) : "")
        .append("\",");
    sb.append("\"modified\":\"").append(fileItem.getModified() != null ? JsonCommand.toJson(fileItem.getModified().toString()) : "")
        .append("\"");
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
