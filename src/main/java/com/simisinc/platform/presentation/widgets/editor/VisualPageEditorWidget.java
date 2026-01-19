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
import org.apache.commons.text.WordUtils;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveWebPageCommand;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.application.cms.WebPageJsonToXMLCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Visual Editor Widget for web page design with drag-and-drop interface
 *
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */
public class VisualPageEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(VisualPageEditorWidget.class);

  static String EDITOR_JSP = "/cms/visual-page-editor.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Set the JSP
    context.setJsp(EDITOR_JSP);

    // Specify the return page
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);

    WebPage webPage = null;

    // The POST was triggered
    if (context.getRequestObject() != null) {
      // Determine the reason...
      webPage = (WebPage) context.getRequestObject();
      context.getRequest().setAttribute("webPage", webPage);
      return context;
    }

    // webPage must be specified, even if it doesn't exist
    String webPageLinkValue = context.getParameter("webPage");
    if (webPage == null) {
      // Determine the page being edited
      if (StringUtils.isBlank(webPageLinkValue)) {
        context.setErrorMessage("A web page is required");
        return context;
      }
      LOG.debug("Loading web page for link: " + webPageLinkValue);
      webPage = WebPageRepository.findByLink(webPageLinkValue);
      if (webPage == null) {
        webPage = new WebPage();
        webPage.setLink(webPageLinkValue);
      }
      context.getRequest().setAttribute("webPage", webPage);
    }

    // Populate the canvas with existing layout if available
    if (StringUtils.isNotBlank(webPage.getPageXml())) {
      // Parse the XML and prepare for the editor
      try {
        // The editor will use the XML to render the visual layout
        context.getRequest().setAttribute("hasExistingLayout", true);
      } catch (Exception e) {
        LOG.error("Error parsing existing page XML: " + e.getMessage(), e);
        context.setWarningMessage("Could not load existing layout. Starting with blank canvas.");
      }
    }

    return context;
  }

  public WidgetContext post(WidgetContext context) {

    // Populate the fields
    String webPageLinkValue = context.getParameter("webPageLink");
    String returnPage = context.getParameter("returnPage");

    // Load the web page being designed
    LOG.debug("Loading web page for link: " + webPageLinkValue);
    WebPage webPage = WebPageRepository.findByLink(webPageLinkValue);
    if (webPage == null) {
      webPage = new WebPage();
      webPage.setLink(webPageLinkValue);
    }

    // Convert the designer JSON to XML
    String designerJson = context.getParameter("designerData");
    if (StringUtils.isBlank(designerJson)) {
      context.setWarningMessage("No design data was submitted");
      context.setRequestObject(webPage);
      return context;
    }

    LOG.debug("Designer JSON: " + designerJson);

    try {
      // Convert the JSON structure to XML format
      String pageXml = WebPageJsonToXMLCommand.convertDesignerJsonToXml(designerJson);
      webPage.setPageXml(pageXml);
      LOG.debug("Converted to XML: " + pageXml);

      // Make a suggested page title
      String title = webPageLinkValue.replace("-", " ");
      title = StringUtils.substringAfterLast(title, "/");
      title = WordUtils.capitalizeFully(title, ' ');
      webPage.setTitle(title);

      // Save the web page
      webPage.setCreatedBy(context.getUserId());
      webPage.setModifiedBy(context.getUserId());

      WebPage savedWebPage = SaveWebPageCommand.saveWebPage(webPage);
      if (savedWebPage == null) {
        throw new DataException("The web page could not be saved");
      }

      // Determine the next page
      if (StringUtils.isNotBlank(returnPage)) {
        context.setSuccessMessage("Web page was saved");
        context.setRedirect(returnPage);
      } else {
        context.setRedirect(savedWebPage.getLink());
      }
      return context;

    } catch (DataException de) {
      LOG.error("DataException", de);
      context.setErrorMessage(de.getMessage());
      context.setRequestObject(webPage);
      return context;
    } catch (Exception e) {
      LOG.error("Exception", e);
      context.setErrorMessage("An error occurred. Please check the values and try again.");
      context.setRequestObject(webPage);
      return context;
    }
  }
}
