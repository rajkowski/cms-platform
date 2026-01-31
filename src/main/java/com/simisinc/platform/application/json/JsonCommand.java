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

package com.simisinc.platform.application.json;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Functinos for working with JSON data
 *
 * @author matt rajkowski
 * @created 9/9/19 10:47 AM
 */
public class JsonCommand {

  private static Log LOG = LogFactory.getLog(JsonCommand.class);

  public static String toJson(String value) {
    if (StringUtils.isBlank(value)) {
      return "";
    }
    // return value.replaceAll("\"", "\\"").trim();
    return StringEscapeUtils.escapeJson(value);
  }

  /* Create a JsonNode object from a string */
  public static JsonNode fromString(String jsonString) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(jsonString);
  }

  /* Create a JsonNode object from a URL's resource contents */
  public static JsonNode fromURL(URL resource) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(resource);
  }

  /* Create a JsonNode object from a file's contents */
  public static JsonNode fromFile(File file) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(file);
  }

  /* Create JSON string from nested String/Object pairs */
  public static StringBuilder createJsonNode(Map<String, Object> pairs) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    boolean isFirst = true;
    for (String name : pairs.keySet()) {
      // Make sure the value exists
      Object value = pairs.get(name);
      if (value == null) {
        continue;
      }
      // @note Consider empty values
      //      if (value instanceof String) {
      //        if (StringUtils.isBlank(String.valueOf(value))) {
      //          continue;
      //        }
      //      }
      // Append the name
      if (!isFirst) {
        sb.append(", ");
      } else {
        isFirst = false;
      }
      sb.append("\"").append(name).append("\":");
      // Determine how the value is appended
      if (value instanceof List) {
        sb.append("[");
        boolean isFirstItem = true;
        for (Object item : (List) value) {
          if (!isFirstItem) {
            sb.append(", ");
          } else {
            isFirstItem = false;
          }
          // Handle different types in the list
          if (item instanceof Map) {
            sb.append(createJsonNode((Map) item));
          } else if (item instanceof String) {
            sb.append("\"").append(toJson((String) item)).append("\"");
          } else if (item instanceof Boolean) {
            sb.append((boolean) item ? "true" : "false");
          } else if (item instanceof List) {
            // Recursively handle nested lists
            sb.append(createJsonNode(Map.of("temp", item)).toString().replaceFirst("\\{\"temp\":", "").replaceFirst("\\}$", ""));
          } else if (item != null) {
            sb.append(item);
          } else {
            sb.append("null");
          }
        }
        sb.append("]");
      } else if (value instanceof String) {
        sb.append("\"").append(toJson((String) value)).append("\"");
      } else if (value instanceof Boolean) {
        sb.append((boolean) value ? "true" : "false");
      } else if (value instanceof Map) {
        sb.append(createJsonNode((Map) value));
      } else if (value.getClass().isArray()) {
        // Handle arrays by converting them to JSON arrays
        sb.append("[");
        Object[] arrayValue = (Object[]) value;
        boolean isFirstItem = true;
        for (Object item : arrayValue) {
          if (!isFirstItem) {
            sb.append(", ");
          } else {
            isFirstItem = false;
          }
          // Handle different types in the array
          if (item instanceof Map) {
            sb.append(createJsonNode((Map) item));
          } else if (item instanceof String) {
            sb.append("\"").append(toJson((String) item)).append("\"");
          } else if (item instanceof Boolean) {
            sb.append((boolean) item ? "true" : "false");
          } else if (item != null) {
            sb.append(item);
          } else {
            sb.append("null");
          }
        }
        sb.append("]");
      } else {
        sb.append(value);
      }
    }
    sb.append("}");
    return sb;
  }
}
