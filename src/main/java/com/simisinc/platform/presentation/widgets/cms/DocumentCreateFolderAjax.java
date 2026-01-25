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

import com.simisinc.platform.application.cms.SaveFolderCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Creates a new folder for the visual document editor
 */
public class DocumentCreateFolderAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908897L;
  private static Log LOG = LogFactory.getLog(DocumentCreateFolderAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("DocumentCreateFolderAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    String name = context.getParameter("name");

    if (StringUtils.isBlank(name)) {
      context.setJson("{\"success\":false,\"message\":\"Folder name is required\"}");
      context.setSuccess(false);
      return context;
    }

    Folder folderBean = new Folder();
    folderBean.setName(name.trim());
    folderBean.setCreatedBy(context.getUserId());
    folderBean.setModifiedBy(context.getUserId());

    try {
      Folder saved = SaveFolderCommand.saveFolder(folderBean);
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"folderId\":").append(saved.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(saved.getName())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
    } catch (Exception e) {
      LOG.error("Unable to create folder", e);
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"success\":false,");
      sb.append("\"message\":\"").append(JsonCommand.toJson(e.getMessage())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
      context.setSuccess(false);
    }

    return context;
  }
}
