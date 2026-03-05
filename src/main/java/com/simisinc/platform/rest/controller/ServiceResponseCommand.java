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

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.database.DataConstraints;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * Common methods for service response
 *
 * @author matt rajkowski
 * @created 4/10/2022 8:51 AM
 */
public class ServiceResponseCommand implements Serializable {

  static final long serialVersionUID = 536435325324169646L;
  private static Log LOG = LogFactory.getLog(ServiceResponseCommand.class);

  public static void addMeta(ServiceResponse response, String type) {
    response.getMeta().put("type", type);
  }

  public static void addMeta(ServiceResponse response, String type, List recordList, DataConstraints constraints) {
    response.getMeta().put("type", type);
    response.getMeta().put("rows", recordList.size());
    if (constraints != null) {
      response.getMeta().put("pageIndex", constraints.getPageNumber());
      response.getMeta().put("totalPages", constraints.getMaxPageNumber());
      response.getMeta().put("totalItems", constraints.getTotalRecordCount());
    } else {
      response.getMeta().put("totalItems", recordList.size());
    }
  }

  public static String toJson(ServiceResponse result) {
    // Aspire to, but not quite there:
    // https://google.github.io/styleguide/jsoncstyleguide.xml
    try (Jsonb jsonb = JsonbBuilder.create()) {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      boolean hasValues = false;
      if (!result.getMeta().isEmpty()) {
        hasValues = true;
        String meta = jsonb.toJson(result.getMeta());
        sb.append("\"meta\": ").append(meta);
      }
      if (!result.getError().isEmpty()) {
        if (hasValues) {
          sb.append(",");
        } else {
          hasValues = true;
        }
        sb.append("\"error\": ")
            .append("{")
            .append("\"code\": ").append(result.getStatus()).append(",")
            .append("\"message\": \"").append(JsonCommand.toJson(result.getError().get("title"))).append("\"")
            .append("}");
      }
      if (result.getData() != null) {
        if (hasValues) {
          sb.append(",");
        } else {
          hasValues = true;
        }
        String data = jsonb.toJson(result.getData());
        sb.append("\"data\": ").append(data);
      }
      if (!result.getLinks().isEmpty()) {
        if (hasValues) {
          sb.append(",");
        }
        String links = jsonb.toJson(result.getLinks());
        sb.append("\"links\": ").append(links);
      }
      sb.append("}");
      return sb.toString();
    } catch (Exception e) {
      LOG.error("Could not serialize ServiceResponse to JSON: " + e.getMessage());
      return "{\"error\":{\"code\":500,\"message\":\"Serialization error\"}}";
    }
  }
}
