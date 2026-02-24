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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileVersion;
import com.simisinc.platform.infrastructure.persistence.cms.FileVersionRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileVersionSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns version history for a file item in the visual document editor
 *
 * @author matt rajkowski
 * @created 2/19/26
 */
public class DocumentFileVersionsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908903L;
  private static Log LOG = LogFactory.getLog(DocumentFileVersionsAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DocumentFileVersionsAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"versions\":[]}");
      return context;
    }

    long fileId = context.getParameterAsLong("fileId", -1);
    if (fileId == -1) {
      context.setJson("{\"versions\":[]}");
      return context;
    }

    FileVersionSpecification specification = new FileVersionSpecification();
    specification.setFileId(fileId);
    List<FileVersion> versions = FileVersionRepository.findAll(specification, null);

    StringBuilder sb = new StringBuilder();
    sb.append("{\"versions\":[");

    boolean first = true;
    if (versions != null) {
      for (FileVersion v : versions) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("{");
        sb.append("\"id\":").append(v.getId()).append(",");
        sb.append("\"fileId\":").append(v.getFileId()).append(",");
        sb.append("\"filename\":\"").append(JsonCommand.toJson(StringUtils.defaultString(v.getFilename()))).append("\",");
        sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(v.getTitle()))).append("\",");
        sb.append("\"version\":\"").append(JsonCommand.toJson(StringUtils.defaultString(v.getVersion()))).append("\",");
        sb.append("\"mimeType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(v.getMimeType()))).append("\",");
        sb.append("\"fileLength\":").append(v.getFileLength()).append(",");
        sb.append("\"url\":\"").append(JsonCommand.toJson(StringUtils.defaultString(v.getUrl()))).append("\",");
        sb.append("\"downloadCount\":").append(v.getDownloadCount()).append(",");
        sb.append("\"created\":\"").append(v.getCreated() != null ? JsonCommand.toJson(v.getCreated().toString()) : "").append("\"");
        sb.append("}");
      }
    }

    sb.append("]}");
    context.setJson(sb.toString());
    return context;
  }
}
