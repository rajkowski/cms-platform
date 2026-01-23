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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns file list data for a folder for the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 10:15 AM
 */
public class DocumentFileListAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(DocumentFileListAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DocumentFileListAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"files\":[],\"total\":0}");
      return context;
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    String searchTerm = context.getParameter("search");
    int limit = context.getParameterAsInt("limit", 25);
    int page = context.getParameterAsInt("page", 1);

    FileSpecification specification = new FileSpecification();
    specification.setFolderId(folderId);
    specification.setForUserId(context.getUserId());

    if (StringUtils.isNotBlank(searchTerm)) {
      specification.setMatchesName(searchTerm);
    }

    DataConstraints constraints = new DataConstraints();
    constraints.setDefaultColumnToSortBy("modified DESC");
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
      sb.append("\"filename\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getFilename()))).append("\",");
      sb.append("\"version\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getVersion()))).append("\",");
      sb.append("\"mimeType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getMimeType()))).append("\",");
      sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getFileType()))).append("\",");
      sb.append("\"fileLength\":").append(file.getFileLength()).append(",");
      sb.append("\"webPath\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getWebPath()))).append("\",");
      sb.append("\"downloadCount\":").append(file.getDownloadCount());
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
}
