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

package com.simisinc.platform.application;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.CustomField;

/**
 * Encodes and decodes json fields
 *
 * @author matt rajkowski
 * @created 6/1/2024 6:35PM
 */
public class CollectionTableColumnsJSONCommand {

  private static Log LOG = LogFactory.getLog(CollectionTableColumnsJSONCommand.class);

  public static String createJSONString(Map<String, CustomField> tableColumnList) {
    if (tableColumnList == null || tableColumnList.isEmpty()) {
      LOG.debug("No fields found");
      return null;
    }

    StringBuilder sb = new StringBuilder();
    int count = 0;
    for (Map.Entry<String, CustomField> set : tableColumnList.entrySet()) {
      CustomField customField = set.getValue();
      if (count > 0) {
        sb.append(",");
      }
      ++count;
      boolean entryAdded = false;
      sb.append("{");
      if (StringUtils.isNotBlank(customField.getLabel())) {
        if (entryAdded) {
          sb.append(",");
        }
        sb.append("\"").append("label").append("\"").append(":").append("\"")
            .append(JsonCommand.toJson(customField.getLabel())).append("\"");
        entryAdded = true;
      }
      if (StringUtils.isNotBlank(customField.getName())) {
        if (entryAdded) {
          sb.append(",");
        }
        sb.append("\"").append("name").append("\"").append(":").append("\"")
            .append(JsonCommand.toJson(customField.getName())).append("\"");
      }
      sb.append("}");
    }
    if (sb.length() == 0) {
      LOG.debug("No values found");
      return null;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Using: " + "[" + sb.toString() + "]");
    }
    return "[" + sb.toString() + "]";
  }

  public static Map<String, CustomField> populateFromJSONString(String jsonValue) throws SQLException {
    // Convert JSON string back into values
    if (StringUtils.isBlank(jsonValue)) {
      return null;
    }
    try {
      JsonNode config = JsonCommand.fromString(jsonValue);
      if (!config.isArray()) {
        LOG.error("populateFromJSONString value is not an array");
        return null;
      }

      // Determine the values
      Map<String, CustomField> customFieldList = new LinkedHashMap<>();
      Iterator<JsonNode> fields = config.elements();
      while (fields.hasNext()) {
        JsonNode node = fields.next();
        CustomField customField = new CustomField();
        if (node.has("name")) {
          customField.setName(node.get("name").asText());
        }
        if (node.has("label")) {
          customField.setLabel(node.get("label").asText());
        } else {
          // Use the name if not found
          customField.setLabel(customField.getName());
        }
        customFieldList.put(customField.getName(), customField);
      }
      // Store in the record
      return customFieldList;
    } catch (Exception e) {
      throw new SQLException("Could not convert from JSON", e.getMessage());
    }
  }
}
