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
import com.simisinc.platform.infrastructure.persistence.cms.FolderRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns folder details for the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 11:00 AM
 */
public class FolderDetailsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;
  private static Log LOG = LogFactory.getLog(FolderDetailsAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("FolderDetailsAjax...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\":false, \"error\": \"Access denied\"}");
      context.setSuccess(false);
      return context;
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    if (folderId == -1) {
      context.setJson("{\"success\":false, \"error\": \"Folder ID required\"}");
      context.setSuccess(false);
      return context;
    }

    // @todo check access

    Folder folder = FolderRepository.findById(folderId);
    if (folder == null) {
      context.setJson("{\"success\":false, \"error\": \"Folder not found\"}");
      context.setSuccess(false);
      return context;
    }

    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"id\":").append(folder.getId()).append(",");
    json.append("\"name\":\"").append(escapeJson(folder.getName())).append("\",");
    json.append("\"summary\":\"").append(escapeJson(folder.getSummary())).append("\",");
    json.append("\"enabled\":").append(folder.getEnabled()).append(",");
    json.append("\"fileCount\":").append(folder.getFileCount()).append(",");
    json.append("\"createdBy\":").append(folder.getCreatedBy()).append(",");
    json.append("\"modified\":\"").append(folder.getModified()).append("\"");
    json.append("}");

    context.setJson(json.toString());
    return context;
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }
}
