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
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns top assets analytics for a document repository in the visual document editor
 *
 * @author matt rajkowski
 * @created 2/19/26
 */
public class DocumentAnalyticsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908907L;
  private static Log LOG = LogFactory.getLog(DocumentAnalyticsAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DocumentAnalyticsAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"assets\":[]}");
      return context;
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    if (folderId == -1) {
      context.setJson("{\"assets\":[]}");
      return context;
    }

    // days parameter: 1, 7, 30, 90 (currently returns top by download_count from folder)
    int days = context.getParameterAsInt("days", 30);

    FileSpecification specification = new FileSpecification();
    specification.setFolderId(folderId);

    DataConstraints constraints = new DataConstraints(1, 20, "download_count", "DESC");

    List<FileItem> files = FileItemRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"folderId\":").append(folderId).append(",");
    sb.append("\"days\":").append(days).append(",");
    sb.append("\"assets\":[");

    boolean first = true;
    if (files != null) {
      for (FileItem file : files) {
        if (file.getDownloadCount() <= 0) {
          continue;
        }
        if (!first) {
          sb.append(",");
        }
        first = false;
        sb.append("{");
        sb.append("\"id\":").append(file.getId()).append(",");
        sb.append("\"title\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getTitle(), file.getFilename()))).append("\",");
        sb.append("\"filename\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getFilename()))).append("\",");
        sb.append("\"mimeType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(file.getMimeType()))).append("\",");
        sb.append("\"downloadCount\":").append(file.getDownloadCount());
        sb.append("}");
      }
    }

    sb.append("]}");
    context.setJson(sb.toString());
    return context;
  }
}
