/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

/**
 * Base class for REST service endpoints; returns 405 Method Not Allowed by default for unimplemented verbs
 *
 * @author matt rajkowski
 * @created 3/6/26 7:00 AM
 */
public abstract class GenericRestService implements RestService {

  @Override
  public ServiceResponse get(ServiceContext context) {
    return new ServiceResponse(405);
  }

  @Override
  public ServiceResponse post(ServiceContext context) {
    return new ServiceResponse(405);
  }

  @Override
  public ServiceResponse put(ServiceContext context) {
    return new ServiceResponse(405);
  }

  @Override
  public ServiceResponse delete(ServiceContext context) {
    return new ServiceResponse(405);
  }

}
