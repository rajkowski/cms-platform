/*
 * Copyright 2025 Matt Rajkowski (https://github.com/rajkowski)
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
 * Visual Image Editor Widget for viewing, editing, and managing images
 *
 * @author matt rajkowski
 * @created 1/21/26 8:45 PM
 */
public class VisualImageEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(VisualImageEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-image-editor.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    // Specify the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    // Optional: Load a specific image if imageId is provided
    String imageIdValue = context.getParameter("imageId");
    if (StringUtils.isNotBlank(imageIdValue)) {
      try {
        long imageId = Long.parseLong(imageIdValue);
        context.getRequest().setAttribute("selectedImageId", imageId);
      } catch (NumberFormatException nfe) {
        LOG.warn("Invalid imageId parameter: " + imageIdValue);
      }
    }

    return context;
  }
}
