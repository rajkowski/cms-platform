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

import com.simisinc.platform.application.cms.SaveSubFolderCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.SubFolder;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates a new subfolder for the visual document editor
 */
public class DocumentCreateSubfolderAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908896L;
  private static Log LOG = LogFactory.getLog(DocumentCreateSubfolderAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("DocumentCreateSubfolderAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"status\":0,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    String name = context.getParameter("name");

    if (folderId <= 0 || StringUtils.isBlank(name)) {
      context.setJson("{\"status\":0,\"message\":\"Folder and name are required\"}");
      context.setSuccess(false);
      return context;
    }

    SubFolder subFolderBean = new SubFolder();
    subFolderBean.setFolderId(folderId);
    subFolderBean.setName(name.trim());
    subFolderBean.setCreatedBy(context.getUserId());
    subFolderBean.setModifiedBy(context.getUserId());

    try {
      SubFolder saved = SaveSubFolderCommand.saveSubFolder(subFolderBean);
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"status\":1,");
      sb.append("\"subfolderId\":").append(saved.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(saved.getName())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
      context.setSuccess(true);
    } catch (Exception e) {
      LOG.error("Unable to create subfolder", e);
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"status\":0,");
      sb.append("\"message\":\"").append(JsonCommand.toJson(e.getMessage())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
      context.setSuccess(false);
    }

    return context;
  }
}
