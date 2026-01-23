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

package com.simisinc.platform.presentation.widgets.editor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Visual Document Editor widget shell for managing folders and files
 *
 * @author matt rajkowski
 * @created 1/22/26 10:05 AM
 */
public class VisualDocumentEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(VisualDocumentEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-document-editor.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    // Specify the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    // Optional: pre-select a folder or file
    String folderIdValue = context.getParameter("folderId");
    if (StringUtils.isNotBlank(folderIdValue)) {
      try {
        context.getRequest().setAttribute("selectedFolderId", Long.parseLong(folderIdValue));
      } catch (NumberFormatException nfe) {
        LOG.warn("Invalid folderId parameter: " + folderIdValue);
      }
    }

    String fileIdValue = context.getParameter("fileId");
    if (StringUtils.isNotBlank(fileIdValue)) {
      try {
        context.getRequest().setAttribute("selectedFileId", Long.parseLong(fileIdValue));
      } catch (NumberFormatException nfe) {
        LOG.warn("Invalid fileId parameter: " + fileIdValue);
      }
    }

    return context;
  }
}
