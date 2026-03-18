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

package com.simisinc.platform.rest.controller;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.analytics.SavePerformanceMetricCommand;
import com.simisinc.platform.application.cms.SaveWebPageHitCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.App;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.presentation.controller.ContextConstants;
import com.simisinc.platform.presentation.controller.RequestConstants;

/**
 * Handles all web api requests
 *
 * @author matt rajkowski
 * @created 7/17/18 1:51 PM
 */
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 30, // 30MB
    maxRequestSize = 1024 * 1024 * 50) // 50MB
public class RestServlet extends HttpServlet {

  private static Log LOG = LogFactory.getLog(RestServlet.class);

  // Services Cache
  private Map<String, Object> serviceInstances = new ConcurrentHashMap<String, Object>();

  @Override
  public void init(ServletConfig config) throws ServletException {

    LOG.info("RestServlet starting up...");
    String startupSuccessful = (String) config.getServletContext().getAttribute(ContextConstants.STARTUP_SUCCESSFUL);
    if (!"true".equals(startupSuccessful)) {
      throw new ServletException("Startup failed due to previous error");
    }

    // Find the available services
    LOG.info("Loading the services library...");
    XMLServiceLoader xmlServiceLoader = new XMLServiceLoader();
    Set<String> restServiceResourcePath = config.getServletContext().getResourcePaths("/WEB-INF/rest-services");
    for (String resource : restServiceResourcePath) {
      try {
        URL restServiceUrl = config.getServletContext().getResource(resource);
        xmlServiceLoader.addFile(restServiceUrl);
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }

    // Instantiate the services
    LOG.info("Instantiating the services...");
    for (Map<String, String> service : xmlServiceLoader.getServiceLibrary()) {
      String endpoint = service.get("endpoint");
      try {
        String serviceClass = service.get("serviceClass");
        Object classRef = Class.forName(serviceClass).getDeclaredConstructor().newInstance();
        serviceInstances.put(endpoint, classRef);
        LOG.info("Added service class: " + endpoint + " = " + serviceClass);
      } catch (Exception e) {
        LOG.error("Class not found for '" + endpoint + "': " + e.getMessage());
      }
    }
    LOG.info("Services loaded: " + serviceInstances.size());
  }

  @Override
  public void destroy() {

  }

  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) {

    long startRequestTime = System.currentTimeMillis();

    LOG.debug("Service processor...");

    // Determine request values
    String requestMethod = request.getMethod().toLowerCase();
    String contextPath = request.getServletContext().getContextPath();
    String requestURI = request.getRequestURI();
    String endpoint = requestURI.substring(contextPath.length() + "/api/".length());
    String pathParam = null;
    String pathParam2 = null;
    if (LOG.isDebugEnabled()) {
      LOG.debug("method: " + requestMethod);
      LOG.debug("contextPath: " + contextPath);
      LOG.debug("requestURI: " + requestURI);
      LOG.debug("endpoint: " + endpoint);
    }

    // Prep the response
    String siteUrl = LoadSitePropertyCommand.loadByName("site.url");
    if (StringUtils.isNotBlank(request.getHeader("Origin")) && StringUtils.isNotBlank(siteUrl)) {
      response.addHeader("Access-Control-Allow-Origin", siteUrl);
    }
    response.setContentType("application/json");
    try {
      response.setCharacterEncoding("UTF-8");
      request.setCharacterEncoding("UTF-8");
    } catch (Exception e) {
      LOG.warn("Unsupported encoding UTF-8: " + e.getMessage());
    }

    // Determine the resource
    try {
      // Get the cached class reference for processing
      Object classRef = serviceInstances.get(endpoint);
      String pathEndpoint = null;
      if (classRef == null) {
        LOG.debug("Could not find endpoint: " + endpoint);
        if (endpoint.contains("/")) {
          // Try as a pathParam
          pathEndpoint = endpoint.substring(0, endpoint.indexOf("/"));
          pathParam = endpoint.substring(endpoint.indexOf("/") + 1);
          if (pathParam.contains("/")) {
            pathParam2 = pathParam.substring(pathParam.indexOf("/") + 1);
            pathParam = pathParam.substring(0, pathParam.indexOf("/"));
          }
          classRef = serviceInstances.get(pathEndpoint);
        }
      }
      if (classRef == null) {
        LOG.error("Class not found for service: " + endpoint);
        sendError(response, SC_NOT_FOUND, "Endpoint not found");
        return;
      }
      if (LOG.isDebugEnabled()) {
        if (pathEndpoint != null) {
          LOG.debug("pathEndpoint: " + pathEndpoint);
          LOG.debug("pathParam: " + pathParam);
          LOG.debug("pathParam2: " + pathParam2);
        }
      }

      // REST endpoint hits
      SaveWebPageHitCommand.saveHit(request.getRemoteAddr(), request.getMethod(),
          "/api/" + (pathEndpoint != null ? pathEndpoint : endpoint),
          (User) request.getAttribute(RequestConstants.REST_USER));

      // Setup the context for this service processor
      ServiceContext serviceContext = new ServiceContext(request, response);
      serviceContext.setPathParam(pathParam);
      serviceContext.setPathParam2(pathParam2);
      serviceContext.setParameterMap(request.getParameterMap());
      serviceContext.setApp((App) request.getAttribute(RequestConstants.REST_APP));
      serviceContext.setUser((User) request.getAttribute(RequestConstants.REST_USER));

      // Execute the service
      ServiceResponse result = null;
      try {
        LOG.debug("-----------------------------------------------------------------------");
        if (pathEndpoint != null) {
          LOG.debug("Executing service: " + pathEndpoint);
        } else {
          LOG.debug("Executing service: " + endpoint);
        }
        RestService service = (RestService) classRef;
        if ("post".equals(requestMethod)) {
          result = service.post(serviceContext);
        } else if ("put".equals(requestMethod)) {
          result = service.put(serviceContext);
        } else if ("delete".equals(requestMethod)) {
          result = service.delete(serviceContext);
        } else {
          result = service.get(serviceContext);
        }
      } catch (Exception e) {
        LOG.error("Exception. MESSAGE = " + e.getMessage(), e);
      }
      if (result == null) {
        LOG.debug("Returning an error...");
        sendError(response, SC_NOT_FOUND, "Service error occurred");
        return;
      }
      if (result.getStatus() != 200) {
        LOG.debug("Setting result: " + result.getStatus());
        response.setStatus(result.getStatus());
        result.getError().put("status", String.valueOf(result.getStatus()));
      }

      LOG.debug("Returning JSON...");
      String json = ServiceResponseCommand.toJson(result);

      long endRequestTime = System.currentTimeMillis();
      long totalTime = endRequestTime - startRequestTime;
      LOG.debug("REST total time: " + totalTime + "ms");
      SavePerformanceMetricCommand.queueMetric("api", response.getStatus(), totalTime);

      response.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);
      try (PrintWriter out = response.getWriter()) {
        out.print(json);
        out.flush();
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Response sent: " + json);
      }
    } catch (Exception e) {
      LOG.error("Could not render: " + e.getMessage());
      LOG.error(e);
      try {
        sendError(response, 500, "Error occurred");
      } catch (Exception io) {
        // no connection
      }
    }
  }

  public static void sendError(HttpServletResponse response, int code, String errorMessage) throws IOException {
    response.setStatus(code);
    try (PrintWriter out = response.getWriter()) {
      out.print("{\n" +
          "      \"error\": {\n" +
          "        \"code\": " + code + ",\n" +
          "        \"message\": \"" + JsonCommand.toJson(errorMessage) + "\"\n" +
          "      }\n" +
          "    }");
      out.flush();
    }
  }
}
