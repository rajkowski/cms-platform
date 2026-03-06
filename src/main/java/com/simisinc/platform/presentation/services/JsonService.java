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

import com.simisinc.platform.presentation.controller.JsonServiceContext;

/**
 * Contract for JSON services registered in json-services.xml.
 *
 * Implementations handle HTTP verbs by overriding the appropriate method.
 * The form token is validated by the framework before post() and delete()
 * are invoked.
 *
 * @author matt rajkowski
 * @created 3/6/26 6:00 AM
 */
public interface JsonService {

  /**
   * Handles GET requests.
   *
   * @param context the JSON service context populated by JsonContainerCommand
   * @return context with JSON set via context.writeOk() / context.writeError()
   */
  JsonServiceContext get(JsonServiceContext context);

  /**
   * Handles POST requests (create / update).
   *
   * @param context the JSON service context
   * @return context with JSON set via context.writeOk() / context.writeError()
   */
  JsonServiceContext post(JsonServiceContext context);

  /**
   * Handles DELETE requests (command=delete).
   *
   * @param context the JSON service context
   * @return context with JSON set via context.writeOk() / context.writeError()
   */
  JsonServiceContext delete(JsonServiceContext context);
}
