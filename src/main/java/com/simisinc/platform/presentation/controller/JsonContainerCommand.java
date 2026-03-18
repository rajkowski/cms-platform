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

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Dispatches a JSON service request directly to the registered service instance.
 *
 * This replaces WebContainerCommand.processWidgets() for the /json/* path so
 * that JsonServlet has no dependency on the page-rendering widget library,
 * web packages, or WebPageXmlLayoutCommand.
 *
 * Dispatch rules:
 * <ul>
 *   <li>Endpoint path is looked up directly in the serviceInstances map.</li>
 *   <li>HTTP method determines the service method: POST → post(),
 *       command=delete → delete(), action param → action() (falls back to
 *       execute() if not present), anything else → execute().</li>
 *   <li>POST / DELETE / action mutations require a valid form token; a missing
 *       or invalid token produces a 400 JSON error immediately.</li>
 *   <li>Supports both new {@code GenericJsonService} classes (take
 *       {@code JsonServiceContext}) and legacy {@code GenericWidget} classes
 *       (take {@code WidgetContext}). Since {@code JsonServiceContext extends
 *       WidgetContext}, the same context instance is passed to both.</li>
 * </ul>
 *
 * @author matt rajkowski
 * @created 3/5/26 12:00 PM
 */
public class JsonContainerCommand implements Serializable {

  private static final long serialVersionUID = 536435325324169647L;
  private static Log LOG = LogFactory.getLog(JsonContainerCommand.class);

  /**
   * Processes a JSON service request and writes the response.
   *
   * @param serviceInstance  the cached service object (GenericJsonService or GenericWidget subclass)
   * @param pageRequest      the parsed page request
   * @param request          the raw HTTP servlet request
   * @param response         the HTTP servlet response
   * @param userSession      the authenticated user session
   * @param controllerSession the controller session for token validation
   * @param applicationUrl   the servlet context root URL
   * @param endpoint         the matched endpoint path (e.g. "/json/content/get")
   * @return {@code true} if the request was handled (caller should not write further),
   *         {@code false} if the service produced no output
   * @throws Exception on unexpected I/O or reflection errors
   */
  public static boolean processService(Object serviceInstance, PageRequest pageRequest,
      HttpServletRequest request, HttpServletResponse response, UserSession userSession,
      ControllerSession controllerSession, URL applicationUrl, String endpoint) throws Exception {

    // ------------------------------------------------------------------
    // 1. Determine request type
    // ------------------------------------------------------------------
    boolean isPost = "post".equalsIgnoreCase(pageRequest.getMethod());
    boolean isDelete = "delete".equals(pageRequest.getParameter("command"));
    boolean isAction = pageRequest.getParameter("action") != null;
    boolean isMutation = isPost || isDelete || isAction;

    String methodName = "execute";
    if (isPost) {
      methodName = "post";
    } else if (isDelete) {
      methodName = "delete";
    } else if (isAction) {
      methodName = "action";
    }

    // ------------------------------------------------------------------
    // 2. Validate form token for all mutation requests (POST / DELETE / action)
    // ------------------------------------------------------------------
    if (isMutation) {
      String formToken = pageRequest.getParameter("token");
      if (StringUtils.isEmpty(formToken)) {
        LOG.error("DEVELOPER: A FORM TOKEN IS REQUIRED for endpoint: " + endpoint + " from " + pageRequest.getRemoteAddr());
        sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "A form token is required.");
        return true;
      }
      if (!userSession.getFormToken().equals(formToken)) {
        LOG.warn("Invalid form token for endpoint: " + endpoint + " from " + pageRequest.getRemoteAddr());
        controllerSession.clearAllWidgetData();
        sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Your session may have expired, please try again.");
        return true;
      }
    }

    // ------------------------------------------------------------------
    // 3. Build the service context (shared by both service types)
    // ------------------------------------------------------------------
    JsonServiceContext context = new JsonServiceContext(applicationUrl, pageRequest, request, response, userSession, endpoint);

    // ------------------------------------------------------------------
    // 4. Dispatch to the service
    // ------------------------------------------------------------------
    WidgetContext result = null;
    try {
      if (serviceInstance instanceof GenericJsonService) {
        // New-style service: get(JsonServiceContext)
        GenericJsonService service = (GenericJsonService) serviceInstance;
        if (isPost) {
          result = service.post(context);
        } else if (isDelete) {
          result = service.delete(context);
        } else {
          result = service.get(context);
        }
      } else {
        // Legacy service extending GenericWidget: methods take WidgetContext.
        // JsonServiceContext IS-A WidgetContext so it can be passed directly.
        try {
          Method method = serviceInstance.getClass().getMethod(methodName, WidgetContext.class);
          result = (WidgetContext) method.invoke(serviceInstance, new Object[] { context });
        } catch (NoSuchMethodException nsme) {
          // Certain methods (e.g. "action") may not be overridden; fall back to execute()
          LOG.debug(
              "Method '" + methodName + "' not found on " + serviceInstance.getClass().getName() + " — falling back to execute()");
          Method method = serviceInstance.getClass().getMethod("execute", WidgetContext.class);
          result = (WidgetContext) method.invoke(serviceInstance, new Object[] { context });
        }
      }
    } catch (NoSuchMethodException nm) {
      LOG.error("No execute() method found on service for endpoint: " + endpoint + " — " + nm.getMessage());
    } catch (Exception e) {
      LOG.error("Service execution error for endpoint: " + endpoint + " — " + e.getMessage(), e);
    }

    // ------------------------------------------------------------------
    // 5. Finalize session state
    // ------------------------------------------------------------------
    controllerSession.clearAllWidgetData();

    // ------------------------------------------------------------------
    // 6. Write the JSON response
    //    Some services write directly to the response (handledResponse).
    //    Most place the JSON string in the context via setJson().
    // ------------------------------------------------------------------
    if (context.handledResponse()) {
      LOG.debug("Service handled response directly for: " + endpoint);
      return true;
    }

    // Prefer json stored in the context; fall back to the returned result object
    String jsonOutput = context.hasJson() ? context.getJson() : (result != null && result.hasJson() ? result.getJson() : null);
    boolean success = context.hasJson() ? context.isSuccess() : (result != null ? result.isSuccess() : true);

    if (jsonOutput != null) {
      LOG.debug("Writing JSON response for: " + endpoint);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      if (!success) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
      response.setContentLength(jsonOutput.getBytes(StandardCharsets.UTF_8).length);
      try (PrintWriter out = response.getWriter()) {
        out.print(jsonOutput);
        out.flush();
      }
      return true;
    }

    LOG.warn("Service produced no JSON output for: " + endpoint);
    return false;
  }

  private static void sendJsonError(HttpServletResponse response, int status, String message) throws Exception {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().print("{\"error\":\"" + message + "\"}");
  }
}
