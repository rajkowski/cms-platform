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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns a single file's metadata for the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 10:20 AM
 */
public class DocumentContentAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(DocumentContentAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DocumentContentAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{}");
      return context;
    }

    long fileId = context.getParameterAsLong("fileId", -1);
    if (fileId == -1) {
      context.setJson("{\"error\":\"fileId required\"}");
      return context;
    }

    FileItem fileItem = FileItemRepository.findById(fileId);
    if (fileItem == null) {
      context.setJson("{\"error\":\"File not found\"}");
      return context;
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
    sb.append("\"created\":\"").append(fileItem.getCreated() != null ? JsonCommand.toJson(fileItem.getCreated().toString()) : "").append("\",");
    sb.append("\"modified\":\"").append(fileItem.getModified() != null ? JsonCommand.toJson(fileItem.getModified().toString()) : "").append("\"");
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
