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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.CheckFolderPermissionCommand;
import com.simisinc.platform.application.cms.SaveFileCommand;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Adds a URL link record to a folder in the visual document editor
 *
 * @author matt rajkowski
 * @created 2/19/26
 */
public class DocumentAddUrlAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908901L;
  private static Log LOG = LogFactory.getLog(DocumentAddUrlAjax.class);

  @Override
  public WidgetContext post(WidgetContext context) {

    LOG.debug("DocumentAddUrlAjax...");

    long folderId = context.getParameterAsLong("folderId", -1);
    long subFolderId = context.getParameterAsLong("subFolderId", -1);

    if (!context.hasRole("admin") &&
        !CheckFolderPermissionCommand.userHasAddPermission(folderId, context.getUserId())) {
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    if (folderId == -1) {
      context.setJson("{\"success\":false,\"message\":\"Folder ID required\"}");
      context.setSuccess(false);
      return context;
    }

    String url = StringUtils.trimToNull(context.getParameter("url"));
    if (StringUtils.isBlank(url)) {
      context.setJson("{\"success\":false,\"message\":\"URL is required\"}");
      context.setSuccess(false);
      return context;
    }

    if (!UrlCommand.isUrlValid(url)) {
      context.setJson("{\"success\":false,\"message\":\"A valid URL is required (must start with http:// or https://)\"}");
      context.setSuccess(false);
      return context;
    }

    String title = StringUtils.trimToNull(context.getParameter("title"));
    if (StringUtils.isBlank(title)) {
      title = url;
    }

    try {
      FileItem fileItemBean = new FileItem();
      fileItemBean.setFolderId(folderId);
      if (subFolderId > 0) {
        fileItemBean.setSubFolderId(subFolderId);
      }
      fileItemBean.setFilename(url);
      fileItemBean.setTitle(title);
      fileItemBean.setVersion("1.0");
      fileItemBean.setFileType("URL");
      fileItemBean.setExtension("url");
      fileItemBean.setMimeType("text/uri-list");
      fileItemBean.setFileLength(0);
      fileItemBean.setFileServerPath(url);
      fileItemBean.setCreatedBy(context.getUserId());
      fileItemBean.setModifiedBy(context.getUserId());

      FileItem saved = SaveFileCommand.saveFile(fileItemBean);
      if (saved == null) {
        context.setJson("{\"success\":false,\"message\":\"Failed to save URL\"}");
        context.setSuccess(false);
        return context;
      }

      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"success\":true,");
      sb.append("\"id\":").append(saved.getId()).append(",");
      sb.append("\"title\":\"").append(JsonCommand.toJson(saved.getTitle())).append("\",");
      sb.append("\"filename\":\"").append(JsonCommand.toJson(saved.getFilename())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());

    } catch (DataException e) {
      LOG.warn("Error adding URL: " + e.getMessage());
      context.setJson("{\"success\":false,\"message\":\"" + JsonCommand.toJson(e.getMessage()) + "\"}");
      context.setSuccess(false);
    }

    return context;
  }
}
