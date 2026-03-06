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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.analytics.SavePerformanceMetricCommand;

/**
 * Handles /json/* AJAX service requests.
 *
 * <p>This servlet is fully independent of PageServlet: it loads its own
 * service registry from WEB-INF/json-services/ XML files, instantiates each
 * service class once, and dispatches incoming requests directly via
 * {@link JsonContainerCommand} — no widget library, no web packages, and no
 * page-rendering infrastructure required.</p>
 *
 * <p>Supported HTTP verbs: GET, POST, HEAD. Special mutations are signalled
 * with {@code command=delete} (DELETE) or an {@code action} parameter.</p>
 *
 * @author matt rajkowski
 * @created 2/21/26 8:00 AM
 */
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 100, // 100MB
    maxRequestSize = 1024 * 1024 * 100) // 100MB
public class JsonServlet extends HttpServlet {

  private static Log LOG = LogFactory.getLog(JsonServlet.class);

  // JSON Service instance cache: endpoint path → service instance
  private Map<String, Object> serviceInstances = new HashMap<>();

  @Override
  public void init(ServletConfig config) throws ServletException {

    LOG.info("JsonServlet starting up...");
    String startupSuccessful = (String) config.getServletContext().getAttribute(ContextConstants.STARTUP_SUCCESSFUL);
    if (!"true".equals(startupSuccessful)) {
      throw new ServletException("Startup failed due to previous error");
    }

    // Load and instantiate JSON services from WEB-INF/json-services/
    LOG.info("Loading JSON services...");
    XMLJSONServiceLoader xmlJsonServiceLoader = new XMLJSONServiceLoader();
    xmlJsonServiceLoader.addDirectory(config.getServletContext(), "json-services");
    for (Map.Entry<String, String> entry : xmlJsonServiceLoader.getServiceLibrary().entrySet()) {
      String endpoint = entry.getKey();
      String serviceClass = entry.getValue();
      try {
        Object classRef = Class.forName(serviceClass).getDeclaredConstructor().newInstance();
        serviceInstances.put(endpoint, classRef);
        LOG.debug("Registered JSON service: " + endpoint + " → " + serviceClass);
      } catch (Exception e) {
        LOG.error(
            "Could not instantiate JSON service class '" + serviceClass + "' for endpoint '" + endpoint + "': " + e.getMessage());
      }
    }
    LOG.info("JSON services loaded: " + serviceInstances.size());
  }

  @Override
  public void destroy() {
  }

  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) {

    long startRequestTime = System.currentTimeMillis();

    LOG.trace("JsonServlet processing request...");

    // Set content type and encoding up front
    response.setContentType("application/json");
    try {
      response.setCharacterEncoding("UTF-8");
      request.setCharacterEncoding("UTF-8");
    } catch (Exception e) {
      LOG.warn("Unsupported encoding UTF-8: " + e.getMessage());
    }
    response.setHeader("X-Content-Type-Options", "nosniff");

    // Capture the endpoint so it is available in the finally block for metrics
    String endpoint = null;

    try {
      // Parse the incoming request into a lightweight PageRequest
      PageRequest pageRequest = new PageRequest(request);
      endpoint = pageRequest.getPagePath();
      LOG.debug("JSON request: " + endpoint);

      // Retrieve the controller session (created by WebRequestFilter)
      ControllerSession controllerSession = (ControllerSession) request.getSession()
          .getAttribute(SessionConstants.CONTROLLER);

      // A valid user session must exist (created by WebRequestFilter)
      UserSession userSession = (UserSession) request.getSession().getAttribute(SessionConstants.USER);
      if (userSession == null) {
        LOG.debug("No user session for JSON request — rejected");
        sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
        return;
      }

      // Prevent caching of JSON responses
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
      response.setHeader("Expires", "-1");

      // Look up the registered service for this endpoint path
      Object serviceInstance = serviceInstances.get(endpoint);
      if (serviceInstance == null) {
        LOG.warn("JSON SERVICE NOT FOUND: " + endpoint + " from " + pageRequest.getRemoteAddr());
        sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
        return;
      }

      // Obtain the servlet context root URL for resource resolution
      URL applicationUrl = request.getServletContext().getResource("/");

      // Dispatch: token validation, method detection, invocation, and response writing
      boolean handled = JsonContainerCommand.processService(serviceInstance, pageRequest, request, response,
          userSession, controllerSession, applicationUrl, endpoint);

      if (!handled) {
        LOG.warn("JSON SERVICE NOT HANDLED: " + endpoint);
        sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
      }

    } catch (Exception e) {
      LOG.error("JsonServlet error for endpoint '" + endpoint + "': " + e.getMessage(), e);
      try {
        sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
      } catch (Exception ignored) {
        // suppress secondary write error
      }
    }

    long endRequestTime = System.currentTimeMillis();
    SavePerformanceMetricCommand.queueMetric("json", response.getStatus(), endRequestTime - startRequestTime);
    LOG.debug("-----------------------------------------------------------------------");
  }

  private void sendJsonError(HttpServletResponse response, int status, String message) throws Exception {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().print("{\"error\":\"" + message + "\"}");
  }
}
