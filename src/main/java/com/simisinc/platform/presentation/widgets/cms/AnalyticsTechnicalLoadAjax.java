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

package com.simisinc.platform.presentation.widgets.cms;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.application.analytics.AnalyticsDataService;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * AJAX endpoint for analytics technical data: system info and performance metrics
 *
 * @author matt rajkowski
 * @created 01/31/26 02:00 PM
 */
public class AnalyticsTechnicalLoadAjax extends GenericWidget {
  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(AnalyticsTechnicalLoadAjax.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  public WidgetContext execute(WidgetContext context) {
    try {
      // Determine days window from range parameter
      int days = 7;
      String range = context.getParameter("range");
      if (range != null && range.contains(",")) {
        // Custom date range
        String[] parts = range.split(",");
        if (parts.length == 2) {
          try {
            LocalDate start = LocalDate.parse(parts[0].trim());
            LocalDate end = LocalDate.parse(parts[1].trim());
            days = (int) ChronoUnit.DAYS.between(start, end) + 1;
          } catch (Exception ignored) {
            // Use default
          }
        }
      } else if ("1d".equals(range)) {
        days = 1;
      } else if ("30d".equals(range)) {
        days = 30;
      } else if ("12m".equals(range)) {
        days = 365;
      }

      // Build response combining system info and performance metrics
      ObjectNode systemInfo = AnalyticsDataService.loadSystemInfo();
      ObjectNode perfMetrics = AnalyticsDataService.loadTechnicalMetrics(days);

      ObjectNode response = MAPPER.createObjectNode();
      response.put("success", true);
      response.put("generatedAt", System.currentTimeMillis());
      response.set("systemInfo", systemInfo);
      response.set("performance", perfMetrics);

      context.setJson(response.toString());
    } catch (Exception e) {
      LOG.error("Error loading technical analytics", e);
      context.setSuccess(false);
    }
    return context;
  }
}

