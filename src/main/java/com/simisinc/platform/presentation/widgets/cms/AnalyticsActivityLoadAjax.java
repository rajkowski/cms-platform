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
import java.time.ZoneId;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.application.cms.MarkdownCommand;
import com.simisinc.platform.domain.model.xapi.XapiStatement;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.xapi.XapiStatementRepository;
import com.simisinc.platform.infrastructure.persistence.xapi.XapiStatementSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * AJAX endpoint for analytics activity stream data
 *
 * @author matt rajkowski
 * @created 02/27/26 08:00 PM
 */
public class AnalyticsActivityLoadAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908901L;
  private static Log LOG = LogFactory.getLog(AnalyticsActivityLoadAjax.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final int PAGE_SIZE = 50;

  @Override
  public WidgetContext execute(WidgetContext context) {
    try {
      String range = context.getParameter("range");

      // Determine start timestamp from range parameter
      LocalDate startDate = LocalDate.now().minusDays(6); // default: 7d
      LocalDate endDate = LocalDate.now();

      if (range != null && range.contains(",")) {
        // Custom date range: "2025-11-01,2025-12-01"
        String[] parts = range.split(",");
        if (parts.length == 2) {
          startDate = LocalDate.parse(parts[0].trim());
          endDate = LocalDate.parse(parts[1].trim());
        }
      } else if ("1d".equals(range)) {
        startDate = LocalDate.now();
      } else if ("30d".equals(range)) {
        startDate = LocalDate.now().minusDays(29);
      } else if ("12m".equals(range)) {
        startDate = LocalDate.now().minusMonths(12);
      }

      long minTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
      long maxTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

      // Query xAPI statement stream
      XapiStatementSpecification spec = new XapiStatementSpecification();

      DataConstraints constraints = new DataConstraints(1, PAGE_SIZE);
      constraints.setColumnToSortBy("occurred_at", "desc");

      List<XapiStatement> statements = XapiStatementRepository.findAll(spec, constraints);

      // Build response
      ObjectNode response = MAPPER.createObjectNode();
      response.put("success", true);
      response.put("generatedAt", System.currentTimeMillis());

      var activityArray = response.putArray("activities");
      if (statements != null) {
        for (XapiStatement statement : statements) {
          // Filter by timestamp range in memory
          if (statement.getOccurredAt() != null) {
            long ts = statement.getOccurredAt().getTime();
            if (ts < minTimestamp || ts >= maxTimestamp) {
              continue;
            }
          }
          var activityObj = activityArray.addObject();
          activityObj.put("id", statement.getId());
          activityObj.put("activityType", statement.getVerb() != null ? statement.getVerb() : "");
          activityObj.put("messageHtml", statement.getMessage() != null ? MarkdownCommand.html(statement.getMessageSnapshot()) : "");
          activityObj.put("timestamp", statement.getOccurredAt() != null ? statement.getOccurredAt().getTime() : 0L);
        }
      }

      context.setJson(response.toString());
    } catch (Exception e) {
      LOG.error("Error loading activity analytics", e);
      context.setJson("{\"success\": false, \"error\": \"Failed to load activity data\"}");
      context.setSuccess(false);
    }
    return context;
  }
}
