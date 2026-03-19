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

package com.simisinc.platform.presentation.controller;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.json.JsonCommand;

/**
 * A focused context for JSON-only service requests. Extends WidgetContext so
 * existing GenericWidget-based Ajax services continue to work unchanged, while
 * also serving as the dedicated context for new GenericJsonService classes.
 *
 * The constructor handles all setup (parameter map, user session, core data)
 * so the JsonContainerCommand does not need the page-rendering machinery of
 * WebContainerCommand.
 *
 * Convenience methods writeOk / writeError produce standard
 * {"status":"ok","data":...} / {"status":"error","error":"..."} envelopes.
 *
 * @author matt rajkowski
 * @created 3/5/26 12:00 PM
 */
public class JsonServiceContext extends WidgetContext {

  static final long serialVersionUID = -8484048371922100001L;

  /**
   * Build a fully-initialized context for a single JSON endpoint request.
   *
   * @param applicationUrl servlet-context root URL
   * @param pageRequest    the parsed page request
   * @param request        the raw HTTP servlet request (needed for parts, session)
   * @param response       the HTTP servlet response
   * @param userSession    the authenticated user session
   * @param endpoint       the matched endpoint path (e.g. "/json/content/get")
   */
  public JsonServiceContext(URL applicationUrl, PageRequest pageRequest, HttpServletRequest request,
      HttpServletResponse response, UserSession userSession, String endpoint) {
    // uniqueId and resourcePath are set to the endpoint; not used for rendering
    super(applicationUrl, pageRequest, request, response, endpoint, endpoint);

    // Attach user session (drives hasRole, getUserId, etc.)
    setUserSession(userSession);

    // coreData.userId is the convention used by WidgetContext.getUserId()
    Map<String, String> coreData = new HashMap<>();
    coreData.put("userId", String.valueOf(userSession.getUserId()));
    setCoreData(coreData);

    // Expose the full parameter map to getParameter* helpers
    setParameterMap(pageRequest.getParameterMap());
  }

  // -------------------------------------------------------------------------
  // JSON response convenience methods
  // -------------------------------------------------------------------------

  /**
   * Writes a {@code {"status":"ok","data":...}} JSON response.
   *
   * @param dataJson the serialized data object, or {@code null} for no data field
   * @return this context (fluent)
   */
  public JsonServiceContext writeOk(String dataJson) {
    StringBuilder json = new StringBuilder("{\"status\":\"ok\"");
    if (dataJson != null) {
      json.append(",\"data\":").append(dataJson);
    }
    json.append("}");
    setJson(json.toString());
    return this;
  }

  /**
   * Writes a {@code {"status":"ok","data":...,"meta":...}} JSON response.
   *
   * @param dataJson serialized data object, or {@code null}
   * @param metaJson serialized meta object (pagination etc.), or {@code null}
   * @return this context (fluent)
   */
  public JsonServiceContext writeOk(String dataJson, String metaJson) {
    StringBuilder json = new StringBuilder("{\"status\":\"ok\"");
    if (dataJson != null) {
      json.append(",\"data\":").append(dataJson);
    }
    if (metaJson != null) {
      json.append(",\"meta\":").append(metaJson);
    }
    json.append("}");
    setJson(json.toString());
    return this;
  }

  /**
   * Writes a {@code {"status":"error","error":"..."}} JSON response and marks
   * the context as unsuccessful (triggers HTTP 400 in the dispatcher).
   *
   * @param message human-readable error description
   * @return this context (fluent)
   */
  public JsonServiceContext writeError(String message) {
    setJson("{\"status\":\"error\",\"error\":\"" +
        JsonCommand.toJson(StringUtils.defaultString(message)) + "\"}");
    setSuccess(false);
    return this;
  }
}
