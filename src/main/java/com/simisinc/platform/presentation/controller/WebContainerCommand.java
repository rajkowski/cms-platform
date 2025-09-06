/*
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

package com.simisinc.platform.presentation.controller;

import static com.simisinc.platform.presentation.controller.RequestConstants.ERROR_MESSAGE_TEXT;
import static com.simisinc.platform.presentation.controller.RequestConstants.MESSAGE_TEXT;
import static com.simisinc.platform.presentation.controller.RequestConstants.SUCCESS_MESSAGE_TEXT;
import static com.simisinc.platform.presentation.controller.RequestConstants.WARNING_MESSAGE_TEXT;
import static com.simisinc.platform.presentation.controller.RequestConstants.WEB_PACKAGE_LIST;

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.thymeleaf.context.Context;

import com.simisinc.platform.ApplicationInfo;
import com.simisinc.platform.application.LoadUserCommand;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.presentation.widgets.cms.WebContainerContext;

/**
 * Process for executing the widgets on a web page
 *
 * @author matt rajkowski
 * @created 2/7/2021 2:40 PM
 */
public class WebContainerCommand implements Serializable {

  private static final long serialVersionUID = 536435325324169646L;
  private static Log LOG = LogFactory.getLog(WebContainerCommand.class);

  private static final String REQUEST_SHARED_VALUE_MAP = "REQUEST_SHARED_VALUE_MAP";
  private static final String MESSAGE = "MESSAGE";
  private static final String SUCCESS_MESSAGE = "SUCCESS_MESSAGE";
  private static final String WARNING_MESSAGE = "WARNING_MESSAGE";
  private static final String ERROR_MESSAGE = "ERROR_MESSAGE";
  private static final String REQUEST_OBJECT = "REQUEST_OBJECT";

  public static PageResponse processWidgets(WebContainerContext webContainerContext, List<Section> sections,
      ContainerRenderInfo containerRenderInfo, Map<String, String> coreData,
      UserSession userSession, Map<String, String> themePropertyMap, HttpServletRequest httpRequest)
      throws Exception {

    LOG.debug("Processing container... " + containerRenderInfo.getName() + ": " + sections.size());

    PageRequest pageRequest = webContainerContext.getPageRequest();
    HttpServletResponse response = webContainerContext.getResponse();
    ControllerSession controllerSession = webContainerContext.getControllerSession();

    PageResponse pageResponse = new PageResponse();

    // Check the controller session for shared widget data
    Map<String, String> sharedWidgetValueMap = null;
    if (controllerSession.hasWidgetData(REQUEST_SHARED_VALUE_MAP)) {
      sharedWidgetValueMap = (HashMap) controllerSession.getWidgetData(REQUEST_SHARED_VALUE_MAP);
    }

    int widgetCount = 0;
    //if (!isPost && !isDelete && !isAction) {
    // Cycle the form token
    // @todo actually, keep a list and expire old ones eventually
    //userSession.renewFormToken();
    //}

    // Process the page (read-only)
    for (Section section : sections) {

      // Check the user's role and groups
      if (!WebComponentCommand.allowsUser(section, userSession)) {
        LOG.debug("SECTION NOT ALLOWED: " +
            (!section.getRoles().isEmpty() ? "[roles=" + section.getRoles().toString() + "]" + " " : "") +
            (!section.getGroups().isEmpty() ? "[groups=" + section.getGroups().toString() + "]" + " " : "") +
            pageRequest.getRemoteAddr());
        continue;
      }

      // Process the section
      boolean sectionAdded = false;
      SectionRenderInfo sectionRenderInfo = new SectionRenderInfo(section);
      LOG.debug("  Columns: " + section.getColumns().size());
      for (Column column : section.getColumns()) {

        // Check the user's role and groups
        if (!WebComponentCommand.allowsUser(column, userSession)) {
          LOG.debug("COLUMN NOT ALLOWED: " +
              (!column.getRoles().isEmpty() ? "[roles=" + column.getRoles().toString() + "]" + " " : "") +
              (!column.getGroups().isEmpty() ? "[groups=" + column.getGroups().toString() + "]" + " " : "") +
              pageRequest.getRemoteAddr());
          continue;
        }

        // Process the column
        boolean columnAdded = false;
        ColumnRenderInfo columnRenderInfo = new ColumnRenderInfo(column);
        LOG.debug("  Widgets: " + column.getWidgets().size());
        for (Widget widget : column.getWidgets()) {

          // Reset the request attributes for each widget
          Enumeration<?> pageRequestAttributeNames = pageRequest.getAttributeNames();
          while (pageRequestAttributeNames.hasMoreElements()) {
            String name = (String) pageRequestAttributeNames.nextElement();
            if (!name.startsWith("controller") && !name.startsWith("master") && !name.startsWith("request")) {
              // Page request attributes are set by widgets for both JSPs and Templates
              pageRequest.removeAttribute(name);
            }
          }

          // HTTP request attributes are set by this container for JSPs specifically
          // and JSPs can also set attributes separately
          if (httpRequest != null) {
            Enumeration<?> httpRequestAttributeNames = httpRequest.getAttributeNames();
            while (httpRequestAttributeNames.hasMoreElements()) {
              String name = (String) httpRequestAttributeNames.nextElement();
              if (!name.startsWith("controller") && !name.startsWith("master") && !name.startsWith("request")) {
                httpRequest.removeAttribute(name);
              }
            }
          }

          // Check the user's role and groups
          if (!WebComponentCommand.allowsUser(widget, userSession)) {
            LOG.debug("WIDGET NOT ALLOWED: " + widget.getWidgetName() + " " +
                (!widget.getRoles().isEmpty() ? "[roles=" + widget.getRoles().toString() + "]" + " " : "") +
                (!widget.getGroups().isEmpty() ? "[groups=" + widget.getGroups().toString() + "]" + " " : "") +
                pageRequest.getRemoteAddr());
            continue;
          }

          // Each widget has a unique id on the page for forms, Javascript, etc.
          ++widgetCount;
          String thisWidgetUniqueId = widget.getWidgetName() + widgetCount;

          // On a POST/DELETE, only execute the action widget
          if (webContainerContext.isTargeted()) {
            if (!thisWidgetUniqueId.equals(containerRenderInfo.getTargetWidget())) {
              continue;
            }
            // Validate the token and fail immediately
            String formToken = pageRequest.getParameter("token");
            if (!userSession.getFormToken().equals(formToken)) {
              controllerSession.clearAllWidgetData();
              controllerSession.addWidgetData(thisWidgetUniqueId, MESSAGE,
                  "Your session may have expired before submitting the form, please try again");
              String siteUrl = LoadSitePropertyCommand.loadByName("site.url");
              response.sendRedirect(siteUrl + pageRequest.getContextPath() + containerRenderInfo.getName());
              pageResponse.setHandled(true);
              return pageResponse;
            }
          }

          WidgetContext widgetContext = new WidgetContext(webContainerContext.getApplicationURL(), pageRequest, httpRequest, response,
              thisWidgetUniqueId,
              containerRenderInfo.getName());
          widgetContext.setParameterMap(pageRequest.getParameterMap());
          widgetContext.setCoreData(coreData);
          widgetContext.setUserSession(userSession);
          widgetContext.setSharedRequestValueMap(sharedWidgetValueMap);

          // Allow the widget to use the properties
          pageRequest.setAttribute("themePropertyMap", themePropertyMap);

          // Get a copy of the preferences and translate any variables
          Map<String, String> preferences = new HashMap<>();

          for (String preference : widget.getPreferences().keySet()) {
            String value = widget.getPreferences().get(preference);
            // check for dynamic preferences
            while (value.contains("${ctx}")) {
              value = StringUtils.replace(value, "${ctx}", pageRequest.getContextPath());
            }
            if (value.contains("${platform.")) {
              value = StringUtils.replace(value, "${platform.name}",
                  StringEscapeUtils.escapeXml11(ApplicationInfo.PRODUCT_NAME));
              value = StringUtils.replace(value, "${platform.url}", ApplicationInfo.PRODUCT_URL);
              value = StringUtils.replace(value, "${platform.version}", ApplicationInfo.VERSION);
            }
            if (value.contains("${webPage.")) {
              if (webContainerContext.getWebPage() != null) {
                value = StringUtils.replace(value, "${webPage.link}", webContainerContext.getWebPage().getLink());
              } else {
                value = StringUtils.replace(value, "${webPage.link}", pageRequest.getPagePath());
              }
              if (widgetContext.getUri().contains("/")) {
                String webPageUniqueId = widgetContext.getUri().substring(widgetContext.getUri().lastIndexOf("/") + 1);
                value = StringUtils.replace(value, "${webPage.uniqueId}", webPageUniqueId);
              }
            }
            if (value.contains("${collection.") && coreData.containsKey("collectionUniqueId")) {
              Collection collection = LoadCollectionCommand
                  .loadCollectionByUniqueId(coreData.get("collectionUniqueId"));
              value = replaceVariable(value, "collection.uniqueId", collection, "uniqueId");
              value = replaceVariable(value, "collection.name", collection, "name");
              value = replaceVariable(value, "collection.link", collection, "listingsLink");
              value = replaceVariable(value, "collection.listingsLink", collection, "listingsLink");
            }
            if (value.contains("${item.") && coreData.containsKey("itemUniqueId")) {
              Item item = LoadItemCommand.loadItemByUniqueId(coreData.get("itemUniqueId"));
              Collection collection = LoadCollectionCommand.loadCollectionById(item.getCollectionId());
              value = replaceVariable(value, "item.uniqueId", item, "uniqueId");
              value = replaceVariable(value, "item.name", item, "name");
              value = replaceVariable(value, "item.summary", item, "summary");
              value = replaceVariable(value, "item.collectionUniqueId", collection, "uniqueId");
              value = replaceVariable(value, "item.collection.name", collection, "name");
              value = replaceVariable(value, "item.collection.link", collection, "listingsLink");
              value = replaceVariable(value, "item.collection.listingsLink", collection, "listingsLink");
              value = replaceVariable(value, "item.latitude", item, "latitude");
              value = replaceVariable(value, "item.longitude", item, "longitude");
              value = replaceVariable(value, "item.city", item, "city");
              value = replaceVariable(value, "item.state", item, "state");
              value = replaceVariable(value, "item.postalCode", item, "postalCode");
            }
            if (value.contains("${user.") && userSession.isLoggedIn()) {
              User thisUser = LoadUserCommand.loadUser(userSession.getUserId());
              value = replaceVariable(value, "user.id", thisUser, "id");
              value = replaceVariable(value, "user.email", thisUser, "email");
              value = replaceVariable(value, "user.firstName", thisUser, "firstName");
              value = replaceVariable(value, "user.lastName", thisUser, "lastName");
              value = replaceVariable(value, "user.fullName", thisUser, "fullName");
            }
            while (value.contains("${request.")) {
              value = replaceVariableWithParameterValue(pageRequest, value);
            }
            preferences.put(preference, value);
          }
          widgetContext.setPreferences(preferences);

          // Check the controller session for widget data
          Object requestObject = controllerSession.getWidgetData(thisWidgetUniqueId, REQUEST_OBJECT);
          if (requestObject != null) {
            LOG.debug("Found a request object: " + requestObject.getClass().getName());
            widgetContext.setRequestObject(requestObject);
            controllerSession.clearWidgetData(thisWidgetUniqueId, REQUEST_OBJECT);
          }
          // Check for display messages (from GET or POST)
          String widgetMessage = (String) controllerSession.getWidgetData(thisWidgetUniqueId, MESSAGE);
          if (widgetMessage != null) {
            widgetContext.setMessage(widgetMessage);
            pageRequest.setAttribute(MESSAGE_TEXT, widgetMessage);
            controllerSession.clearWidgetData(thisWidgetUniqueId, MESSAGE);
          }
          String widgetSuccessMessage = (String) controllerSession.getWidgetData(thisWidgetUniqueId, SUCCESS_MESSAGE);
          if (widgetSuccessMessage != null) {
            widgetContext.setSuccessMessage(widgetSuccessMessage);
            pageRequest.setAttribute(SUCCESS_MESSAGE_TEXT, widgetSuccessMessage);
            controllerSession.clearWidgetData(thisWidgetUniqueId, SUCCESS_MESSAGE);
          }
          String widgetWarningMessage = (String) controllerSession.getWidgetData(thisWidgetUniqueId, WARNING_MESSAGE);
          if (widgetWarningMessage != null) {
            widgetContext.setWarningMessage(widgetWarningMessage);
            pageRequest.setAttribute(WARNING_MESSAGE_TEXT, widgetWarningMessage);
            controllerSession.clearWidgetData(thisWidgetUniqueId, WARNING_MESSAGE);
          }
          String widgetErrorMessage = (String) controllerSession.getWidgetData(thisWidgetUniqueId, ERROR_MESSAGE);
          if (widgetErrorMessage != null) {
            widgetContext.setErrorMessage(widgetErrorMessage);
            pageRequest.setAttribute(ERROR_MESSAGE_TEXT, widgetErrorMessage);
            controllerSession.clearWidgetData(thisWidgetUniqueId, ERROR_MESSAGE);
          }

          // Make this widget context and resources available in the request during processing
          pageRequest.setAttribute(RequestConstants.CONTEXT_PATH, pageRequest.getContextPath());
          pageRequest.setAttribute(RequestConstants.WIDGET_CONTEXT, widgetContext);
          pageRequest.setAttribute("webPage", webContainerContext.getWebPage());
          pageRequest.setAttribute(RequestConstants.WEB_PAGE_PATH, pageRequest.getPagePath());

          // Get the cached class reference for processing
          Object classRef = webContainerContext.getWidgetInstances().get(widget.getWidgetName());
          if (classRef == null) {
            LOG.error("Class not found for widget: " + widget.getWidgetName());
            continue;
          }

          // Execute the widget
          WidgetContext result = null;
          try {
            LOG.debug("-----------------------------------------------------------------------");
            LOG.trace("Getting method...");
            String methodName = "execute";
            if (webContainerContext.isPost()) {
              methodName = "post";
            } else if (webContainerContext.isDelete()) {
              methodName = "delete";
            } else if (webContainerContext.isAction()) {
              methodName = "action";
            }
            LOG.debug("Executing widget: " + widget.getWidgetName() + "." + methodName + " [" + thisWidgetUniqueId + "]");
            Method method = classRef.getClass().getMethod(methodName, widgetContext.getClass());
            result = (WidgetContext) method.invoke(classRef, new Object[] { widgetContext });
            // Check for an alternate widget and execute it, log this
            if (result != null && result.hasWidgetName()) {
              LOG.trace("Widget requesting a different widget to execute: " + result.getWidgetName());
              classRef = webContainerContext.getWidgetInstances().get(result.getWidgetName());
              if (classRef == null) {
                LOG.error("Class not found for widget: " + result.getWidgetName());
                continue;
              }
              LOG.debug("Executing widget: " + result.getWidgetName() + "." + methodName + " [" + thisWidgetUniqueId + "]");
              method = classRef.getClass().getMethod(methodName, widgetContext.getClass());
              result = (WidgetContext) method.invoke(classRef, new Object[] { widgetContext });
            }
          } catch (NoSuchMethodException nm) {
            LOG.error("No Such Method Exception for method execute. MESSAGE = " + nm.getMessage(), nm);
          } catch (IllegalAccessException ia) {
            LOG.error("Illegal Access Exception. MESSAGE = " + ia.getMessage(), ia);
          } catch (Exception e) {
            LOG.error("Exception. MESSAGE = " + e.getMessage(), e);
            if (webContainerContext.isPost()) {
              widgetContext.setErrorMessage("The form could not be validated, please try again");
            }
          }

          // The container may have updated the page's render info
          if (containerRenderInfo instanceof PageRenderInfo) {
            PageRenderInfo pageRenderInfo = (PageRenderInfo) containerRenderInfo;
            if (StringUtils.isNotBlank(widgetContext.getPageTitle())) {
              if (webContainerContext.getWebPage() != null) {
                pageRenderInfo.setTitle(
                    widgetContext.getPageTitle() +
                        (StringUtils.isNotBlank(webContainerContext.getWebPage().getTitle())
                            ? " - " + webContainerContext.getWebPage().getTitle()
                            : ""));
              } else if (webContainerContext.getPage() != null) {
                pageRenderInfo.setTitle(
                    widgetContext.getPageTitle() +
                        (StringUtils.isNotBlank(webContainerContext.getPage().getTitle())
                            ? " - " + webContainerContext.getPage().getTitle()
                            : ""));
              }
            }
            if (StringUtils.isNotBlank(widgetContext.getPageDescription())) {
              pageRenderInfo.setDescription(widgetContext.getPageDescription());
            }
            if (StringUtils.isNotBlank(widgetContext.getPageKeywords())) {
              pageRenderInfo.setKeywords(widgetContext.getPageKeywords());
            }
          }

          // Expect JSON first and return early
          if (widgetContext.hasJson()) {
            LOG.debug("Returning JSON...");
            controllerSession.clearAllWidgetData();
            response.setContentType("application/json");
            response.setContentLength(widgetContext.getJson().length());
            PrintWriter out = response.getWriter();
            out.print(widgetContext.getJson());
            out.flush();
            pageResponse.setHandled(true);
            return pageResponse;
          }

          // A widget can handle the response, so exit
          if (widgetContext.handledResponse()) {
            LOG.debug("Widget handled response...");
            controllerSession.clearAllWidgetData();
            pageResponse.setHandled(true);
            return pageResponse;
          }

          // See if the widget issued a redirect
          if (!webContainerContext.isTargeted()) {
            if (widgetContext.hasRedirect()) {
              controllerSession.clearAllWidgetData();
              String siteUrl = LoadSitePropertyCommand.loadByName("site.url");
              response.sendRedirect(siteUrl + pageRequest.getContextPath() + widgetContext.getRedirect());
              pageResponse.setHandled(true);
              return pageResponse;
            }
          }

          // Handle POST and DELETE response
          if (webContainerContext.isTargeted()) {
            // Determine the next page after the action
            String actionRedirect = widgetContext.getRedirect();
            if (actionRedirect != null && pageRequest.getContextPath().length() > 0) {
              if (actionRedirect.startsWith(pageRequest.getContextPath())) {
                actionRedirect = actionRedirect.substring(pageRequest.getContextPath().length());
              }
            }
            if (actionRedirect == null) {
              LOG.debug("Action pagePath: " + pageRequest.getPagePath());
              actionRedirect = containerRenderInfo.getName();
              if (webContainerContext.getPage().checkForItemUniqueId()) {
                // The XML indicates that there is an item
                if (webContainerContext.getPage().getItemUniqueId().contains("*")) {
                  // Substitute the item's unique id
                  actionRedirect = StringUtils.replace(webContainerContext.getPage().getItemUniqueId(), "*",
                      widgetContext.getCoreData().get("itemUniqueId"));
                }
              } else if (webContainerContext.getPage().checkForCollectionUniqueId()) {
                // The XML indicates that there is a collection
                if (webContainerContext.getPage().getCollectionUniqueId().contains("*")) {
                  // Substitute the collection's uniqueId (/uri/*)
                  actionRedirect = StringUtils.replace(webContainerContext.getPage().getCollectionUniqueId(), "*",
                      widgetContext.getCoreData().get("collectionUniqueId"));
                } else if ("?collectionId".equals(webContainerContext.getPage().getCollectionUniqueId())) {
                  // Use the collection's id (/admin/collection-form{?collectionId})
                  if (widgetContext.getCoreData().containsKey("collectionId")) {
                    actionRedirect += webContainerContext.getPage().getCollectionUniqueId() + "="
                        + widgetContext.getCoreData().get("collectionId");
                  }
                } else {
                  // Use the collection's uniqueId
                  if (widgetContext.getCoreData().containsKey("collectionUniqueId")) {
                    LOG.warn("This actionRedirect was called and is in use, so remove this comment");
                    actionRedirect += webContainerContext.getPage().getCollectionUniqueId() + "="
                        + widgetContext.getCoreData().get("collectionUniqueId");
                  }
                }
              }
            }
            LOG.debug("Sending an action redirect to: " + actionRedirect);
            if (widgetContext.getMessage() != null) {
              controllerSession.addWidgetData(widgetContext.getUniqueId(), MESSAGE, widgetContext.getMessage());
            }
            // Since this is an action, use the session to provide data for the next request
            if (widgetContext.getSuccessMessage() != null) {
              controllerSession.addWidgetData(widgetContext.getUniqueId(), SUCCESS_MESSAGE,
                  widgetContext.getSuccessMessage());
            }
            if (widgetContext.getWarningMessage() != null) {
              controllerSession.addWidgetData(widgetContext.getUniqueId(), WARNING_MESSAGE,
                  widgetContext.getWarningMessage());
            }
            if (widgetContext.getErrorMessage() != null) {
              controllerSession.addWidgetData(widgetContext.getUniqueId(), ERROR_MESSAGE,
                  widgetContext.getErrorMessage());
            }
            if (widgetContext.getRequestObject() != null) {
              LOG.debug("Adding a request object: " + widgetContext.getRequestObject().getClass().getName());
              controllerSession.addWidgetData(widgetContext.getUniqueId(), REQUEST_OBJECT,
                  widgetContext.getRequestObject());
            }
            if (widgetContext.getSharedRequestValueMap() != null) {
              sharedWidgetValueMap = widgetContext.getSharedRequestValueMap();
              controllerSession.addWidgetData(REQUEST_SHARED_VALUE_MAP, sharedWidgetValueMap);
            }
            String siteUrl = LoadSitePropertyCommand.loadByName("site.url");
            response.sendRedirect(siteUrl + pageRequest.getContextPath() + actionRedirect);
            LOG.debug("-----------------------------------------------------------------------");
            pageResponse.setHandled(true);
            return pageResponse;
          }

          // If the method was a success, check for content
          String widgetContent = null;
          if (result != null) {
            // For the time being, render from the HTML template for non-HTTP requests only (CLI, Jobs, etc)
            if (httpRequest == null && widgetContext.hasTemplate()) {
              // Render the HTML Template content
              String template = widgetContext.getTemplate();
              if (template.startsWith("/")) {
                template = template.substring(1);
              }
              LOG.debug("Using Template: /WEB-INF/html-templates/" + template);
              Context ctx = new Context();
              ctx.setVariables(widgetContext.getRequest().getAttributes());
              widgetContent = PageTemplateEngine.useTemplateEngine().process(template, ctx).trim();
            } else if (widgetContext.hasJsp()) {
              // Render the widget's JSP
              if (httpRequest != null) {
                RequestDispatcher requestDispatcher = httpRequest.getRequestDispatcher("/WEB-INF/jsp" + widgetContext.getJsp());
                if (requestDispatcher == null) {
                  // Register an error and skip the output
                  LOG.error("JSP NOT FOUND: " + "/WEB-INF/jsp" + widgetContext.getJsp());
                  continue;
                }
                // Share the web packages
                httpRequest.setAttribute(WEB_PACKAGE_LIST, webContainerContext.getWebPackageList());
                // Map the pageRequest attributes to the http request for JSPs
                for (String attribute : pageRequest.getAttributes().keySet()) {
                  httpRequest.setAttribute(attribute, pageRequest.getAttribute(attribute));
                }
                // Render the JSP content
                WidgetResponseWrapper responseWrapper = new WidgetResponseWrapper(response);
                requestDispatcher.include(httpRequest, responseWrapper);
                widgetContent = responseWrapper.getOutputAndClose();
              }
            } else if (widgetContext.hasHtml()) {
              // Use the widget's generated HTML
              widgetContent = widgetContext.getHtml();
            }
          }

          // If there's content, then turn on the output
          if (widgetContent != null && widgetContent.length() > 0) {
            // The widget asked to be included without the main css/scripts/footer
            if (widgetContext.isEmbedded()) {
              webContainerContext.setEmbedded(true);
            }
            WidgetRenderInfo widgetRenderInfo = new WidgetRenderInfo(widget, widgetContent);
            if (!sectionAdded) {
              sectionAdded = true;
              containerRenderInfo.setHasWidgets(true);
              sectionRenderInfo.setHasWidgets(true);
              containerRenderInfo.addSection(sectionRenderInfo);
            }
            if (!columnAdded) {
              columnAdded = true;
              sectionRenderInfo.addColumn(columnRenderInfo);
            }
            columnRenderInfo.addWidget(widgetRenderInfo);
          }
        }
      }
    }
    return pageResponse;
  }

  // replaceVariable("collection.uniqueId", value, collection, "uniqueId")
  public static String replaceVariable(String content, String term, Object bean, String property) {

    if (bean == null) {
      return content;
    }

    // See if the term exists in the content
    if (!content.contains("${" + term + "}") && !content.contains("${" + term + ":")) {
      return content;
    }

    // Retrieve the bean's value
    String replacementValue = null;
    try {
      replacementValue = BeanUtils.getProperty(bean, property);
    } catch (Exception e) {
      LOG.debug("Value not found for property: " + property);
    }
    if (StringUtils.isBlank(replacementValue)) {
      LOG.debug("The bean did not have a value for the term: " + term + ", using blank value");
      replacementValue = "";
    }

    // See if there is a requested encoding
    int startIdx = content.indexOf("${" + term + "}");
    int splitIdx = -1;
    if (startIdx == -1) {
      startIdx = content.indexOf("${" + term + ":");
      splitIdx = content.indexOf(":", startIdx);
    }
    if (startIdx == -1) {
      return content;
    }
    int endIdx = content.indexOf("}", startIdx);

    String searchString = content.substring(startIdx, endIdx + 1);
    if (splitIdx == -1) {
      return StringUtils.replace(content, searchString, replacementValue);
    }

    // Use the encoding
    String encoding = content.substring(splitIdx + 1, endIdx);
    if ("html".equals(encoding)) {
      replacementValue = StringEscapeUtils.escapeXml11(replacementValue);
    } else if ("toHtml".equals(encoding)) {
      replacementValue = HtmlCommand.textToHtml(replacementValue);
    } else if ("json".equals(encoding)) {
      replacementValue = StringEscapeUtils.escapeJson(replacementValue);
    } else if ("sql".equals(encoding)) {
      replacementValue = StringUtils.replace(replacementValue, "'", "''");
    }
    return StringUtils.replace(content, searchString, replacementValue);
  }

  public static String replaceVariableWithParameterValue(PageRequest request, String content) {
    // Determine the search string
    int idxStart = content.indexOf("${request.");
    int idxEnd = content.indexOf("}", idxStart);
    if (idxEnd == -1) {
      LOG.warn("variable not enclosed between {}");
      idxEnd = content.length() - 1;
    }
    String searchString = content.substring(idxStart, idxEnd + 1);
    // Determine the parameter and parameter value
    String requestParam = content.substring(idxStart + "${request.".length(), idxEnd);
    String encoding = null;
    if (requestParam.contains(":")) {
      int splitIdx = requestParam.indexOf(":");
      encoding = requestParam.substring(splitIdx + 1);
      requestParam = requestParam.substring(0, splitIdx);
    }
    String replacementValue = request.getParameter(requestParam);
    if (StringUtils.isBlank(replacementValue)) {
      LOG.debug("Parameter value not found for: " + requestParam);
      replacementValue = "";
    }

    // Use the encoding
    if ("html".equals(encoding)) {
      replacementValue = StringEscapeUtils.escapeXml11(replacementValue);
    } else if ("toHtml".equals(encoding)) {
      replacementValue = HtmlCommand.textToHtml(replacementValue);
    } else if ("json".equals(encoding)) {
      replacementValue = StringEscapeUtils.escapeJson(replacementValue);
    } else if ("sql".equals(encoding)) {
      replacementValue = StringUtils.replace(replacementValue, "'", "''");
    }
    return StringUtils.replace(content, searchString, replacementValue);
  }
}
