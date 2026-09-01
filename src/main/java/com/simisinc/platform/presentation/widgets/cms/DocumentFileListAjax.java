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

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileSpecification;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.controller.UserSession;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.zeroio.platform.infrastructure.permission.Permission;

/**
 * Returns file list data for a folder for the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 10:15 AM
 */
public class DocumentFileListAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(DocumentFileListAjax.class);

  @Override
  public JsonServiceContext get(JsonServiceContext context) {

    LOG.debug("DocumentFileListAjax...");

    // Check permissions
    if (!Permission.check("cms.document.file-list", context.getUserSession())) {
      LOG.debug("No permission to: " + DocumentFileListAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    long subFolderId = context.getParameterAsLong("subFolderId", -1);
    long collectionId = context.getParameterAsLong("collectionId", -1);
    String searchTerm = context.getParameter("search");
    int limit = context.getParameterAsInt("limit", -1);
    int page = context.getParameterAsInt("page", 1);

    if (collectionId > -1) {
      return listCollectionFiles(context, collectionId, searchTerm, limit, page);
    }

    FileSpecification specification = new FileSpecification();
    specification.setFolderId(folderId);
    specification.setSubFolderId(subFolderId);
    long userId = context.getUserId();
    if (userId > -1) {
      // Determine role which can see all document repositories
      if (!context.hasRole("admin")) {
        specification.setForUserId(userId);
      }
    } else {
      specification.setForUserId((long) UserSession.GUEST_ID);
    }

    if (StringUtils.isNotBlank(searchTerm)) {
      specification.setMatchesName(searchTerm);
    }

    DataConstraints constraints = new DataConstraints();
    // by name
    constraints.setColumnToSortBy("filename");
    constraints.setPageNumber(page);
    constraints.setPageSize(limit);

    List<FileItem> files = FileItemRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"files\": [");

    boolean first = true;
    for (FileItem file : files) {
      if (!first) {
        sb.append(",");
      }
      first = false;

      sb.append("{");
      sb.append("\"id\":").append(file.getId()).append(",");
      sb.append("\"folderId\":").append(file.getFolderId()).append(",");
      sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getTitle()))).append("\",");
      sb.append("\"filename\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getFilename())))
          .append("\",");
      sb.append("\"version\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getVersion())))
          .append("\",");
      sb.append("\"mimeType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getMimeType())))
          .append("\",");
      sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getFileType())))
          .append("\",");
      sb.append("\"fileLength\":").append(file.getFileLength()).append(",");
      sb.append("\"url\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getUrl()))).append("\",");
      sb.append("\"downloadCount\":").append(file.getDownloadCount()).append(",");
      sb.append("\"error\":")
          .append(isMissingOnServer(file.getFileType(), file.getMimeType(), file.getFileServerPath())).append(",");
      sb.append("\"modified\":\"")
          .append(file.getModified() != null ? JsonCommand.toJson(file.getModified().toString()) : "")
          .append("\"");
      sb.append("}");
    }

    sb.append("],");
    sb.append("\"page\":").append(page).append(",");
    sb.append("\"limit\":").append(limit).append(",");
    sb.append("\"total\":").append(files.size());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }

  private JsonServiceContext listCollectionFiles(JsonServiceContext context, long collectionId, String searchTerm,
      int limit, int page) {
    Collection collection;
    if (context.hasRole("admin")) {
      collection = LoadCollectionCommand.loadCollectionById(collectionId);
    } else {
      collection = LoadCollectionCommand.loadCollectionByIdForAuthorizedUser(collectionId, context.getUserId());
    }
    if (collection == null) {
      return context.writeError("Collection not found or permission denied");
    }

    ItemFileSpecification specification = new ItemFileSpecification();
    specification.setCollectionId(collectionId);
    if (StringUtils.isNotBlank(searchTerm)) {
      specification.setMatchesName(searchTerm);
    }

    DataConstraints constraints = new DataConstraints();
    constraints.setColumnToSortBy("filename");
    constraints.setPageNumber(page);
    constraints.setPageSize(limit);

    List<ItemFileItem> files = ItemFileItemRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"files\": [");

    boolean first = true;
    for (ItemFileItem file : files) {
      Item item = LoadItemCommand.loadItemById(file.getItemId());
      if (item == null) {
        continue;
      }

      if (!first) {
        sb.append(",");
      }
      first = false;

      String fileUrl = StringUtils.defaultString(file.getUrl());
      String downloadUrl = "/show/" + item.getUniqueId() + "/assets/file/" + fileUrl;
      String viewUrl = "/show/" + item.getUniqueId() + "/assets/view/" + fileUrl;

      sb.append("{");
      sb.append("\"id\":").append(file.getId()).append(",");
      sb.append("\"collectionId\":").append(collection.getId()).append(",");
      sb.append("\"itemId\":").append(file.getItemId()).append(",");
      sb.append("\"itemUniqueId\":\"").append(JsonCommand.toJson(StringUtils.defaultString(item.getUniqueId())))
          .append("\",");
      sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getTitle()))).append("\",");
      sb.append("\"filename\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getFilename())))
          .append("\",");
      sb.append("\"version\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getVersion())))
          .append("\",");
      sb.append("\"mimeType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getMimeType())))
          .append("\",");
      sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getFileType())))
          .append("\",");
      sb.append("\"fileLength\":").append(file.getFileLength()).append(",");
      sb.append("\"url\":\"").append(JsonCommand.toJson(fileUrl)).append("\",");
      sb.append("\"downloadUrl\":\"").append(JsonCommand.toJson(downloadUrl)).append("\",");
      sb.append("\"viewUrl\":\"").append(JsonCommand.toJson(viewUrl)).append("\",");
      sb.append("\"downloadCount\":").append(file.getDownloadCount()).append(",");
      sb.append("\"error\":")
          .append(isMissingOnServer(file.getFileType(), file.getMimeType(), file.getFileServerPath())).append(",");
      sb.append("\"sourceType\":\"collection\",");
      sb.append("\"modified\":\"")
          .append(file.getModified() != null ? JsonCommand.toJson(file.getModified().toString()) : "")
          .append("\"");
      sb.append("}");
    }

    sb.append("],");
    sb.append("\"page\":").append(page).append(",");
    sb.append("\"limit\":").append(limit).append(",");
    sb.append("\"total\":").append(files.size());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }

  private boolean isMissingOnServer(String fileType, String mimeType, String fileServerPath) {
    if (StringUtils.isBlank(fileServerPath)) {
      return false;
    }
    if ("url".equalsIgnoreCase(StringUtils.defaultString(fileType))
        || "text/uri-list".equalsIgnoreCase(StringUtils.defaultString(mimeType))) {
      return false;
    }
    File serverFile = FileSystemCommand.getFileServerRootPath(fileServerPath);
    return serverFile != null && !serverFile.isFile();
  }
}
