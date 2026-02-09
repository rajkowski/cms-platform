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

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveContentCommand;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Visual Content Editor Widget for managing content blocks, pages, blogs, and calendars
 *
 * @author matt rajkowski
 * @created 2/7/26 12:00 PM
 */
public class VisualContentEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(VisualContentEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-content-editor.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    // Specify the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    // Load initial context data for the editor
    // User permissions will be checked by the JSP and JavaScript modules
    context.getRequest().setAttribute("userId", context.getUserId());

    // Theme settings (for dark mode support)
    String theme = context.getPreferences().get("theme");
    if (StringUtils.isBlank(theme)) {
      theme = "light";
    }
    context.getRequest().setAttribute("theme", theme);

    LOG.debug("Visual Content Editor initialized for user: " + context.getUserId());

    return context;
  }

/*
    public WidgetContext post(WidgetContext context) {

      // Parse form parameters
      String contentUniqueId = context.getParameter("contentUniqueId");
      String contentHtml = context.getParameter("content");
      String isDraftParam = context.getParameter("isDraft");
      String returnPage = context.getParameter("returnPage");

      // Validate required parameters
      if (StringUtils.isBlank(contentUniqueId)) {
        context.setErrorMessage("Content unique ID is required");
        context.setRequestObject(null);
        return context;
      }

      if (StringUtils.isBlank(contentHtml)) {
        context.setErrorMessage("Content is required");
        context.setRequestObject(null);
        return context;
      }

      // Determine if this is a draft save or publish
      boolean isDraft = "true".equalsIgnoreCase(isDraftParam);

      LOG.debug("Saving content: " + contentUniqueId + ", isDraft: " + isDraft);

      try {
        // Create content object for saving
        Content content = new Content();
        content.setUniqueId(contentUniqueId);
        content.setCreatedBy(context.getUserId());
        content.setModifiedBy(context.getUserId());

        // Set the content based on whether it's a draft or publish
        if (isDraft) {
          content.setDraftContent(contentHtml);
        } else {
          content.setContent(contentHtml);
        }

        // Save the content using SaveContentCommand
        Content savedContent = SaveContentCommand.saveContent(content, isDraft);
        if (savedContent == null) {
          throw new DataException("The content could not be saved");
        }

        // Set success message
        if (isDraft) {
          context.setSuccessMessage("Content saved as draft");
        } else {
          context.setSuccessMessage("Content published successfully");
        }

        // Determine the next page
        if (StringUtils.isNotBlank(returnPage)) {
          context.setRedirect(returnPage);
        } else {
          // Stay on the editor page
          context.setRedirect("/admin/visual-content-editor");
        }

        return context;

      } catch (DataException de) {
        LOG.error("DataException saving content", de);
        context.setErrorMessage(de.getMessage());
        context.setRequestObject(null);
        return context;
      } catch (Exception e) {
        LOG.error("Exception saving content", e);
        context.setErrorMessage("An error occurred while saving content. Please check the values and try again.");
        context.setRequestObject(null);
        return context;
      }
    }
 */
}
