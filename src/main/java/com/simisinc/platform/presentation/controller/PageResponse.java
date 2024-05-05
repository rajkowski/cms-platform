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

/**
 * Information about a page response
 *
 * @author matt rajkowski
 * @created 1/21/2024 8:50 PM
 */
public class PageResponse implements Serializable {

  private static final long serialVersionUID = 215434482513634196L;

  private int responseCode = -1;
  private boolean isHandled = false;

  public PageResponse() {
  }

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public boolean isHandled() {
    return isHandled;
  }

  public void setHandled(boolean wasHandled) {
    this.isHandled = wasHandled;
  }

}
