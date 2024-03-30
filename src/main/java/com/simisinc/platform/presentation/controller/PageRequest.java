/*
 * Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

/**
 * The page request encapsulates values from HttpRequest values and/or non-HttpRequest values
 *
 * @author matt rajkowski
 * @created 1/6/2024 8:50 PM
 */
public class PageRequest implements Serializable {

  private static final long serialVersionUID = 215434482513634196L;

  private String scheme = null;
  private String serverName = null;
  private int port = -1;
  private String contextPath = null;
  private String uri = null;
  private String pagePath = null;
  private String baseUrl = null;
  private String url = null;
  private String queryString = null;
  private boolean secure = false;
  private String remoteAddr = null;
  private String method = null;
  private Map<String, ArrayList<String>> headers = Collections.synchronizedMap(new CaseInsensitiveMap<String, ArrayList<String>>());
  private Map<String, String[]> parameterMap = null;
  private Map<String, Object> attributeMap = new ConcurrentHashMap<>();
  // private HttpServletRequest request = null;

  public PageRequest(HttpServletRequest request) {
    this.method = request.getMethod();
    this.scheme = request.getScheme();
    this.serverName = request.getServerName();
    this.port = request.getServerPort();
    this.contextPath = request.getServletContext().getContextPath();
    this.uri = request.getRequestURI();
    this.queryString = request.getQueryString();
    this.secure = request.isSecure();
    this.remoteAddr = request.getRemoteAddr();
    processPaths();
    // this.request = request;
    Enumeration<String> headers = request.getHeaderNames();
    if (headers != null) {
      while (headers.hasMoreElements()) {
        String header = headers.nextElement();
        this.setHeader(header, request.getHeaders(header));
      }
    }
    this.setParameterMap(request.getParameterMap());
  }

  public PageRequest(String method, String scheme, String serverName, int port, String contextPath, String uri,
      String queryString, boolean secure, String remoteAddr) {
    this.method = method;
    this.scheme = scheme;
    this.serverName = serverName;
    this.port = port;
    this.contextPath = contextPath;
    this.uri = uri;
    this.queryString = queryString;
    this.secure = secure;
    this.remoteAddr = remoteAddr;
    processPaths();
  }

  public String getScheme() {
    return scheme;
  }

  public String getServerName() {
    return serverName;
  }

  public int getPort() {
    return port;
  }

  public String getContextPath() {
    return contextPath;
  }

  public String getUri() {
    return uri;
  }

  public String getRequestURI() {
    return getUri();
  }

  public String getPagePath() {
    return pagePath;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getUrl() {
    return url;
  }

  public boolean isSecure() {
    return secure;
  }

  public String getRemoteAddr() {
    return remoteAddr;
  }

  public String getMethod() {
    return method;
  }

  public String getQueryString() {
    return queryString;
  }

  public void setHeader(String name, Enumeration<String> values) {
    ArrayList<String> existingValues = headers.get(name);
    if (existingValues == null) {
      existingValues = new ArrayList<String>();
      headers.put(name, existingValues);
    }
    while (values.hasMoreElements()) {
      existingValues.add(values.nextElement());
    }
  }

  public long getDateHeader(String name) {
    if (headers.containsKey(name) && headers.get(name).size() > 0) {
      try {
        return DateUtils.parseDate(headers.get(name).get(0)).getTime();
      } catch (Exception e) {
        // NumberFormatException: For input string: "Sat, 17 Feb 2024 01:26:41 GMT"
      }
    }
    return -1;
  }

  public String getHeader(String name) {
    if (headers.containsKey(name) && headers.get(name).size() > 0) {
      return headers.get(name).get(0);
    }
    return null;
  }

  public Enumeration<String> getHeaders(String name) {
    return Collections.enumeration(headers.get(name));
  }

  public Enumeration<String> getHeaderNames() {
    return Collections.enumeration(headers.keySet());
  }

  public int getIntHeader(String name) {
    if (headers.containsKey(name) && headers.get(name).size() > 0) {
      return Integer.parseInt(headers.get(name).get(0));
    }
    return -1;
  }

  public Map<String, String[]> getParameterMap() {
    return parameterMap;
  }

  public String getParameter(String name) {
    String[] values = parameterMap.get(name);
    if (values != null) {
      return values[0];
    }
    return null;
  }

  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(parameterMap.keySet());
  }

  public String[] getParameterValues(String name) {
    return parameterMap.get(name);
  }

  public void setParameterMap(Map<String, String[]> parameterMap) {
    this.parameterMap = parameterMap;
  }

  public void setAttribute(String name, Object o) {
    if (o == null) {
      return;
    }
    attributeMap.put(name, o);
  }

  public Object getAttribute(String name) {
    return attributeMap.get(name);
  }

  public void removeAttribute(String name) {
    attributeMap.remove(name);
  }

  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(attributeMap.keySet());
  }

  public Map<String, Object> getAttributes() {
    return attributeMap;
  }

  private void processPaths() {
    if (StringUtils.isBlank(uri)) {
      uri = "/";
    }
    pagePath = uri.substring(contextPath.length());
    baseUrl = scheme + "://" + serverName + (port != 80 && port != 443 ? ":" + port : "");
    url = baseUrl + contextPath;
  }
}
