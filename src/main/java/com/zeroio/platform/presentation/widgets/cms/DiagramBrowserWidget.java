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

package com.zeroio.platform.presentation.widgets.cms;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Widget to display the diagram browser for TinyMCE editor
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class DiagramBrowserWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;

  private static Log LOG = LogFactory.getLog(DiagramBrowserWidget.class);

  private static String JSP = "/cms/diagram-browser.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Load all files and filter for diagram files (files with .drawio extension)
    FileSpecification spec = new FileSpecification();
    List<FileItem> allFiles = FileItemRepository.findAll(spec, null);

    // Filter for .drawio files
    List<FileItem> diagramList = new java.util.ArrayList<>();
    if (allFiles != null) {
      for (FileItem file : allFiles) {
        if (file.getFilename() != null && file.getFilename().endsWith(".drawio")) {
          diagramList.add(file);
        }
      }
    }
    context.getRequest().setAttribute("diagramList", diagramList);

    if ("reveal".equals(context.getRequest().getParameter("view"))) {
      context.setEmbedded(true);
    }

    String inputId = context.getRequest().getParameter("inputId");
    context.getRequest().setAttribute("inputId", inputId);

    // Show the browser
    context.setEmbedded(true);
    context.setJsp(JSP);
    return context;
  }
}
