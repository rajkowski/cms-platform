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

package com.simisinc.platform.presentation.widgets.cms;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.presentation.controller.ControllerSession;
import com.simisinc.platform.presentation.controller.Page;
import com.simisinc.platform.presentation.controller.PageRequest;

/**
 * A context for processing the widgets
 *
 * @author matt rajkowski
 * @created 2/7/2021 6:02 PM
 */
public class WebContainerContext implements Serializable {

  private static final long serialVersionUID = 215434482513634196L;
  private static final int METHOD_GET = 0;
  private static final int METHOD_POST = 1;
  private static final int METHOD_DELETE = 2;
  private static final int METHOD_ACTION = 3;

  private URL applicationUrl = null;
  private PageRequest pageRequest = null;
  private HttpServletRequest request = null;
  private HttpServletResponse response = null;

  private int method = METHOD_GET;
  private ControllerSession controllerSession = null;
  private Map<String, Object> widgetInstances = null;
  private WebPage webPage = null;
  private Page page = null;

  private boolean embedded = false;

  public WebContainerContext(URL applicationUrl, PageRequest pageRequest, HttpServletRequest request,
      ControllerSession controllerSession, Map<String, Object> widgetInstances, WebPage webPage, Page page) {

    this.applicationUrl = applicationUrl;
    this.pageRequest = pageRequest;
    this.request = request;
    this.controllerSession = controllerSession;
    this.widgetInstances = widgetInstances;
    this.webPage = webPage;
    this.page = page;

    if ("post".equalsIgnoreCase(pageRequest.getMethod())) {
      method = METHOD_POST;
    } else if ("delete".equals(pageRequest.getParameter("command"))) {
      method = METHOD_DELETE;
    } else if (pageRequest.getParameter("action") != null) {
      method = METHOD_ACTION;
    }
  }

  public WebContainerContext(URL applicationUrl, PageRequest pageRequest, HttpServletRequest request,
      HttpServletResponse response, ControllerSession controllerSession, Map<String, Object> widgetInstances,
      WebPage webPage, Page page) {
    this.applicationUrl = applicationUrl;
    this.pageRequest = pageRequest;
    this.request = request;
    this.response = response;
    this.controllerSession = controllerSession;
    this.widgetInstances = widgetInstances;
    this.webPage = webPage;
    this.page = page;

    if ("post".equalsIgnoreCase(pageRequest.getMethod())) {
      method = METHOD_POST;
    } else if ("delete".equals(pageRequest.getParameter("command"))) {
      method = METHOD_DELETE;
    } else if (pageRequest.getParameter("action") != null) {
      method = METHOD_ACTION;
    }
  }

  public PageRequest getPageRequest() {
    return pageRequest;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  public HttpServletResponse getResponse() {
    return response;
  }

  public ControllerSession getControllerSession() {
    return controllerSession;
  }

  public Map<String, Object> getWidgetInstances() {
    return widgetInstances;
  }

  public WebPage getWebPage() {
    return webPage;
  }

  public Page getPage() {
    return page;
  }

  public boolean isEmbedded() {
    return embedded;
  }

  public void setEmbedded(boolean embedded) {
    this.embedded = embedded;
  }

  public boolean isTargeted() {
    return method != METHOD_GET;
  }

  public boolean isPost() {
    return method == METHOD_POST;
  }

  public boolean isDelete() {
    return method == METHOD_DELETE;
  }

  public boolean isAction() {
    return method == METHOD_ACTION;
  }

  public URL getApplicationURL() {
    return applicationUrl;
  }
}
