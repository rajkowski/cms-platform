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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.application.analytics.AnalyticsDataService;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * AJAX endpoint for analytics content data
 *
 * @author matt rajkowski
 * @created 01/31/26 02:00 PM
 */
public class AnalyticsContentLoadAjax extends GenericWidget {
  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(AnalyticsContentLoadAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {
    String range = context.getParameter("range");
    String rangeStart = null;
    String rangeEnd = null;
    int days = 7; // default

    // Range can be "1d", "7d", "30d", "12m", or "2025-11-01,2025-12-01"
    if (range != null && range.contains(",")) {
      String[] parts = range.split(",");
      if (parts.length == 2) {
        rangeStart = parts[0].trim();
        rangeEnd = parts[1].trim();
      }
    } else {
      // Determine date range - default to 7 days
      rangeStart = LocalDate.now().minusDays(6).toString();
      rangeEnd = LocalDate.now().toString();

      if ("1d".equals(range)) {
        days = 1;
        rangeStart = LocalDate.now().toString();
        rangeEnd = LocalDate.now().toString();
      } else if ("30d".equals(range)) {
        days = 30;
        rangeStart = LocalDate.now().minusDays(29).toString();
        rangeEnd = LocalDate.now().toString();
      } else if ("12m".equals(range)) {
        days = 365;
        rangeStart = LocalDate.now().minusMonths(12).toString();
        rangeEnd = LocalDate.now().toString();
      }
    }

    // Calculate days if using custom date range
    if (rangeStart != null && rangeEnd != null && days == 7) {
      LocalDate start = LocalDate.parse(rangeStart);
      LocalDate end = LocalDate.parse(rangeEnd);
      days = (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    try {
      // Get data from service
      ObjectNode response = AnalyticsDataService.loadContent(rangeStart, rangeEnd, days);
      context.setJson(response.toString());
    } catch (Exception e) {
      LOG.error("Error loading content analytics", e);
      context.setJson("{\"success\": false, \"error\": \"Failed to load content analytics\"}");
      context.setSuccess(false);
    }
    return context;
  }
}
