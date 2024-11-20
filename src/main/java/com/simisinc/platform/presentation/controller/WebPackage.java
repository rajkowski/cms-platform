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
 * Used for dynamically adding script and stylesheet tags to the web page; Web packages are defined in dependencies.json
 *
 * @author matt rajkowski
 * @created 11/3/24 8:00 AM
 */
public class WebPackage implements Serializable {
  static final long serialVersionUID = -8484048371911908893L;

  private String name = null;
  private String version = null;

  public WebPackage(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

}
