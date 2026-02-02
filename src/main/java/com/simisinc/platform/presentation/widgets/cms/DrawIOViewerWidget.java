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

import com.simisinc.platform.application.cms.LoadFileCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Displays a Draw.io diagram from a .drawio FileItem using the Draw.io viewer
 *
 * @author matt rajkowski
 * @created 1/31/26 12:30 PM
 */
public class DrawIOViewerWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908896L;

  static String JSP = "/cms/drawio-viewer.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {

    // GET uri /assets/drawio/20180503171549-5/something.drawio

    // Use the request uri
    String resourceValue = context.getUri().substring(context.getResourcePath().length() + 1);
    if (resourceValue.contains("/")) {
      resourceValue = resourceValue.substring(0, resourceValue.indexOf("/"));
    }
    LOG.debug("Using resource value: " + resourceValue);
    int dashIdx = resourceValue.lastIndexOf("-");
    if (dashIdx == -1) {
      return null;
    }

    // Determine the file id and web path
    String webPath = resourceValue.substring(0, dashIdx);
    String fileIdValue = resourceValue.substring(dashIdx + 1);
    long fileId = Long.parseLong(fileIdValue);
    if (fileId <= 0) {
      return null;
    }

    // Determine the file and access permissions
    FileItem record;
    if (context.hasRole("admin")) {
      // The file can be downloaded
      record = LoadFileCommand.loadItemById(fileId);
    } else {
      // User must have view access in the folder's user group
      record = LoadFileCommand.loadLatestFileByIdForAuthorizedUser(webPath, fileId, context.getUserId());
    }
    if (record == null) {
      LOG.warn("File record does not exist or no access: " + fileId);
      return null;
    }

    // Set the file path
    context.getRequest().setAttribute("filePath", record.getUrl());

    // https://www.drawio.com/doc/faq/embed-html-options

    // Highlight color for hyperlinks
    context.getRequest().setAttribute("highlight", context.getPreferences().getOrDefault("highlight", "#0000ff"));
    // Control the looks and behavior of the toolbar (zoom lightbox layers pages)
    context.getRequest().setAttribute("toolbar", context.getPreferences().getOrDefault("toolbar", "zoom layers"));
    // Control the position of the toolbar (top or bottom)
    context.getRequest().setAttribute("toolbarPosition", context.getPreferences().getOrDefault("toolbar-position", "top"));
    // Do not hide the toolbar when not hovering over diagrams
    context.getRequest().setAttribute("toolbarNohide", context.getPreferences().getOrDefault("toolbar-nohide", "true"));
    // Display the lightbox / fullscreen button
    context.getRequest().setAttribute("lightbox", context.getPreferences().getOrDefault("lightbox", "false"));
    // Editable
    context.getRequest().setAttribute("editable", context.getPreferences().getOrDefault("editable", "false"));
    // Zoom
    context.getRequest().setAttribute("zoom", context.getPreferences().getOrDefault("zoom", "1"));
    // Disables collapse/expand
    context.getRequest().setAttribute("nav", context.getPreferences().getOrDefault("nav", "false"));
    context.getRequest().setAttribute("resize", context.getPreferences().getOrDefault("resize", "true"));
    // Enable opening the editor for diagrams
    context.getRequest().setAttribute("edit", context.getPreferences().getOrDefault("edit", "false"));

    context.setJsp(JSP);
    return context;
  }
}
