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
package com.simisinc.platform.application.cms;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.simisinc.platform.presentation.controller.WebPackage;

/**
 * Builds a list of frontend web packages from web-packages.json
 *
 * @author matt rajkowski
 * @created 11/3/24 8:00 AM
 */
public class WebPackageCommand {

  private static Log LOG = LogFactory.getLog(WebPackageCommand.class);

  //* Read the web packages into a map */
  public static Map<String, WebPackage> init(URL resource) throws IOException {
    // A resource is required
    if (resource == null) {
      throw new IOException("Resource not found");
    }
    Map<String, WebPackage> webPackageMap = new LinkedHashMap<>();
    JsonNode json = JsonLoader.fromURL(resource);
    if (json == null) {
      throw new IOException("Invalid resource");
    }
    if (!json.has("dependencies")) {
      throw new IOException("Invalid json");
    }
    JsonNode dependencies = json.get("dependencies");
    if (dependencies == null) {
      throw new IOException("Dependencies not found");
    }
    Iterator<Entry<String, JsonNode>> fields = dependencies.fields();
    while (fields.hasNext()) {
      Entry<String, JsonNode> jsonField = fields.next();
      String name = jsonField.getKey();
      String version = jsonField.getValue().get("version").asText();
      webPackageMap.put(name, new WebPackage(name, version));
      LOG.debug("Found web package: " + name + "=" + version);
    }
    return webPackageMap;
  }
}
