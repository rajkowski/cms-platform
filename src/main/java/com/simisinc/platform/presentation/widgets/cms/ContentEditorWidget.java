/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.DateCommand;
import com.simisinc.platform.application.cms.LoadContentCommand;
import com.simisinc.platform.application.cms.LoadStylesheetCommand;
import com.simisinc.platform.application.cms.LoadWebPageCommand;
import com.simisinc.platform.application.cms.SaveContentCommand;
import com.simisinc.platform.application.cms.HtmlEditorCommand;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.domain.events.cms.WebPageUpdatedEvent;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.Stylesheet;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.workflow.WorkflowManager;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.events.cms.WebPageDraftContentEditedEvent;

/**
 * Content editor widget
 *
 * @author matt rajkowski
 * @created 4/17/18 8:09 PM
 */
public class ContentEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/cms/content-editor.jsp";
  static String CODE_EDITOR_JSP = "/cms/content-code-editor.jsp";

  public WidgetContext execute(WidgetContext context) {

    // This is a demo capability
    String codeContent = context.getPreferences().get("codeContent");
    if (codeContent != null) {
      context.getRequest().setAttribute("codeContent", codeContent);

      String returnPage = context.getPreferences().get("returnPage");
      context.getRequest().setAttribute("returnPage", returnPage);

      context.setJsp(CODE_EDITOR_JSP);
      return context;
    }

    // Determine the page being edited
    String uniqueId = context.getParameter("uniqueId");
    if (StringUtils.isEmpty(uniqueId)) {
      return context;
    }
    Content content = ContentRepository.findByUniqueId(uniqueId);
    if (content == null) {
      content = new Content();
      content.setUniqueId(uniqueId);
    }
    context.getRequest().setAttribute("content", content);

    // Determine the HTML
    String contentHtml = content.getContent();
    if (content.getDraftContent() != null) {
      context.getRequest().setAttribute("isDraft", "true");
      contentHtml = content.getDraftContent();
    }

    // Set Icons to Span for HTML editor
    if (contentHtml != null) {
      // Handle conventions used in HTML Editor for editing
      contentHtml = HtmlEditorCommand.prepareContentForEditor(contentHtml);
    }
    context.getRequest().setAttribute("contentHtml", contentHtml);

    // Determine the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    // Mirror live page stylesheet loading so editor preview matches site rendering.
    Stylesheet globalStylesheet = LoadStylesheetCommand.loadStylesheetByWebPageId(-1L);
    if (globalStylesheet != null) {
      context.getRequest().setAttribute("includeGlobalStylesheet", "true");
      context.getRequest().setAttribute("includeGlobalStylesheetLastModified",
          globalStylesheet.getModified().getTime());
    }
    if (StringUtils.isNotBlank(returnPage)) {
      WebPage returnWebPage = LoadWebPageCommand.loadByLink(returnPage);
      if (returnWebPage != null) {
        Stylesheet pageStylesheet = LoadStylesheetCommand.loadStylesheetByWebPageId(returnWebPage.getId());
        if (pageStylesheet != null) {
          context.getRequest().setAttribute("includeStylesheet", pageStylesheet.getWebPageId());
          context.getRequest().setAttribute("includeStylesheetLastModified", pageStylesheet.getModified().getTime());
        }
      }
    }

    // Show the editor
    context.setJsp(JSP);
    return context;
  }

  public WidgetContext post(WidgetContext context) {

    // Determine the content's uniqueId
    String uniqueId = context.getParameter("uniqueId");
    if (StringUtils.isEmpty(uniqueId)) {
      context.setErrorMessage("Content id must be specified");
      return context;
    }

    // Check for editor content
    String contentHtml = context.getParameter("content");
    if (contentHtml == null) {
      LOG.error("DEVELOPER: Content parameter was not found");
      context.setErrorMessage("A system error occurred");
      return context;
    }

    // Determine the page to return to
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    if (StringUtils.isEmpty(returnPage)) {
      returnPage = "/";
    }
    context.setRedirect(returnPage);

    // Determine if this is a draft being removed
    String saveAction = context.getParameter("save");
    if ("Remove this Draft".equalsIgnoreCase(saveAction)) {
      removeDraft(uniqueId);
      return context;
    }

    // Determine if the content is immediately published or saved as draft
    boolean publish = true;
    if ("Save as Draft".equalsIgnoreCase(saveAction)) {
      publish = false;
      LOG.debug("Saving as draft...");
    }
    try {
      // Before saving, check when the existing content was last modified
      Content existingContent = LoadContentCommand.loadContentByUniqueId(uniqueId);
      boolean doContentNotification = (existingContent == null
          || DateCommand.isHoursOld(existingContent.getModified(), 3)
          || context.getUserId() != existingContent.getModifiedBy());

      boolean contentWasDraft = (existingContent != null && StringUtils.isNotBlank(existingContent.getDraftContent()));

      // Save the content
      Content content = SaveContentCommand.saveSafeContent(uniqueId, contentHtml, context.getUserId(), publish);
      if (content == null) {
        LOG.warn("Content record was not saved!");
        context.setErrorMessage("An error occurred");
        return context;
      }

      // Load the associated web page and trigger events
      WebPage webPage = LoadWebPageCommand.loadByLink(returnPage);
      if (webPage == null) {
        return context;
      }

      // Triggers include:
      // - WebPageUpdatedEvent: when content is published and the page is updated by a different user or was just updated in the last 3 hours.
      // - WebPageDraftContentEditedEvent: when content is saved as a draft and the draft is edited by a different user or was just updated in the last 3 hours.

      // Determine if the content was published on the page
      if (publish) {
        // Check when the page was last modified
        boolean doWebPageNotification = DateCommand.isHoursOld(webPage.getModified(), 3)
            || contentWasDraft
            || context.getUserId() != webPage.getModifiedBy();

        // Mark the page as modified to update the modified date and user, which is used for triggering events and cache invalidation
        WebPageRepository.markAsModifiedAndFindable(webPage, context.getUserId());

        // Record the web page was updated
        if (doWebPageNotification) {
          WorkflowManager.triggerWorkflowForEvent(new WebPageUpdatedEvent(webPage, context.getUserId()));
        }
      } else {
        // This is a draft edit, trigger draft event workflow
        if (doContentNotification) {
          WorkflowManager.triggerWorkflowForEvent(new WebPageDraftContentEditedEvent(webPage, context.getUserId()));
        }
      }
    } catch (DataException e) {
      LOG.error("DEVELOPER: Content parameter was not found");
      context.setErrorMessage("A system error occurred");
    }
    return context;
  }

  private void removeDraft(String uniqueId) {
    Content content = LoadContentCommand.loadContentByUniqueId(uniqueId);
    ContentRepository.removeDraft(content);
  }
}
