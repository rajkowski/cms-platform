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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.dashboard.StatisticsData;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHitRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns analytics for a specific file in the visual document editor, querying web_page_hits
 * by the file's asset URL path prefix for both /assets/file/ and /assets/view/ paths.
 *
 * @author matt rajkowski
 * @created 2/19/26
 */
public class DocumentFileAnalyticsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908911L;
  private static Log LOG = LogFactory.getLog(DocumentFileAnalyticsAjax.class);
  private static final String EMPTY_RESPONSE = "{\"totalHits\":0,\"recentHits\":[],\"monthlyData\":[]}";

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DocumentFileAnalyticsAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson(EMPTY_RESPONSE);
      return context;
    }

    long fileId = context.getParameterAsLong("fileId", -1);
    if (fileId == -1) {
      context.setJson(EMPTY_RESPONSE);
      return context;
    }

    int days = Math.min(Math.max(context.getParameterAsInt("days", 30), 1), 365);

    FileItem file = FileItemRepository.findById(fileId);
    if (file == null) {
      context.setJson(EMPTY_RESPONSE);
      return context;
    }

    String baseUrl = file.getBaseUrl();
    if (baseUrl == null) {
      context.setJson(EMPTY_RESPONSE);
      return context;
    }

    long totalHits = WebPageHitRepository.countFileHitsByPathPrefix(baseUrl, days);
    List<StatisticsData> dailyHits = WebPageHitRepository.findFileHitsByPathPrefix(baseUrl, days);
    List<StatisticsData> monthlyHits = WebPageHitRepository.findFileHitsByPathPrefix(baseUrl, 365);

    Map<String, Long> monthlyMap = buildMonthlyMap(monthlyHits);

    context.setJson(buildJson(totalHits, days, monthlyMap, dailyHits));
    return context;
  }

  private static Map<String, Long> buildMonthlyMap(List<StatisticsData> monthlyHits) {
    Map<String, Long> monthlyMap = new LinkedHashMap<>();
    LocalDate now = LocalDate.now();
    for (int i = 11; i >= 0; i--) {
      String key = now.minusMonths(i).withDayOfMonth(1).toString().substring(0, 7);
      monthlyMap.put(key, 0L);
    }
    if (monthlyHits == null) {
      return monthlyMap;
    }
    for (StatisticsData d : monthlyHits) {
      if (d.getLabel() != null && d.getLabel().length() >= 7) {
        String monthKey = d.getLabel().substring(0, 7);
        if (monthlyMap.containsKey(monthKey)) {
          monthlyMap.put(monthKey, monthlyMap.get(monthKey) + Long.parseLong(d.getValue()));
        }
      }
    }
    return monthlyMap;
  }

  private static String buildJson(long totalHits, int days, Map<String, Long> monthlyMap, List<StatisticsData> dailyHits) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"totalHits\":").append(totalHits).append(",");
    sb.append("\"days\":").append(days).append(",");
    appendMonthlyData(sb, monthlyMap);
    sb.append(",");
    appendRecentHits(sb, dailyHits);
    sb.append("}");
    return sb.toString();
  }

  private static void appendMonthlyData(StringBuilder sb, Map<String, Long> monthlyMap) {
    sb.append("\"monthlyData\":[");
    boolean first = true;
    for (Map.Entry<String, Long> entry : monthlyMap.entrySet()) {
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("{\"label\":\"").append(entry.getKey()).append("\",\"count\":").append(entry.getValue()).append("}");
    }
    sb.append("]");
  }

  private static void appendRecentHits(StringBuilder sb, List<StatisticsData> dailyHits) {
    sb.append("\"recentHits\":[");
    if (dailyHits == null) {
      sb.append("]");
      return;
    }
    boolean first = true;
    for (int i = dailyHits.size() - 1; i >= 0; i--) {
      StatisticsData d = dailyHits.get(i);
      long count = Long.parseLong(d.getValue());
      if (count <= 0) {
        continue;
      }
      if (!first) {
        sb.append(",");
      }
      first = false;
      sb.append("{\"date\":\"").append(JsonCommand.toJson(d.getLabel())).append("\",\"count\":").append(count).append("}");
    }
    sb.append("]");
  }
}
