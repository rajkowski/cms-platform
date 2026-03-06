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

package com.simisinc.platform.presentation.services;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.presentation.controller.JsonServiceContext;

/**
 * Base class for JSON-only services registered in json-services.xml.
 *
 * Like GenericWidget, instances are created once at startup and may be
 * called concurrently by multiple threads. Do NOT store request-scoped
 * state in instance fields; use only local variables and the supplied
 * JsonServiceContext.
 *
 * New services should extend this class instead of GenericWidget so they
 * are clearly separated from page-rendering widgets. Existing GenericWidget
 * subclasses continue to work because JsonContainerCommand supports both.
 *
 * Override only the methods you need (execute for GET, post for POST,
 * delete for DELETE). Unimplemented methods log an error and return the
 * context unchanged so callers can write a 404 or fallback response.
 *
 * @author matt rajkowski
 * @created 3/5/26 12:00 PM
 */
public class GenericJsonService implements Serializable, JsonService {

  static final long serialVersionUID = -8484048371922100002L;
  protected static Log LOG = LogFactory.getLog(GenericJsonService.class);

  public GenericJsonService() {
  }

  /**
   * Handles GET requests. Override to implement read-only data retrieval.
   *
   * @param context the JSON service context populated by JsonContainerCommand
   * @return context with JSON set via context.writeOk() / context.writeError()
   */
  public JsonServiceContext get(JsonServiceContext context) {
    LOG.error("MUST OVERRIDE THE DEFAULT GET METHOD IN: " + getClass().getName());
    return context;
  }

  /**
   * Handles POST requests (create / update). Override to implement mutations.
   * The form token is validated before this method is called.
   *
   * @param context the JSON service context
   * @return context with JSON set via context.writeOk() / context.writeError()
   */
  public JsonServiceContext post(JsonServiceContext context) {
    LOG.error("MUST OVERRIDE THE DEFAULT POST METHOD IN: " + getClass().getName());
    return context;
  }

  /**
   * Handles DELETE requests (command=delete). Override to implement deletions.
   * The form token is validated before this method is called.
   *
   * @param context the JSON service context
   * @return context with JSON set via context.writeOk() / context.writeError()
   */
  public JsonServiceContext delete(JsonServiceContext context) {
    LOG.error("MUST OVERRIDE THE DEFAULT DELETE METHOD IN: " + getClass().getName());
    return context;
  }
}
