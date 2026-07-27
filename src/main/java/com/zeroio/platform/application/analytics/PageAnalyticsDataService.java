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
package com.zeroio.platform.application.analytics;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.domain.model.dashboard.StatisticsData;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHitRepository;

/**
 * Provides analytics data for a specific page, including daily views and member visits
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class PageAnalyticsDataService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static ObjectNode loadPageAnalytics(String pagePath, int days) {
    return loadPageAnalytics(pagePath, null, null, days);
  }

  public static ObjectNode loadPageAnalytics(String pagePath, LocalDate fromDate, LocalDate toDate) {
    return loadPageAnalytics(pagePath, fromDate, toDate, 7);
  }

  public static ObjectNode loadPageAnalytics(String pagePath, LocalDate fromDate, LocalDate toDate, int days) {
    ObjectNode response = MAPPER.createObjectNode();
    String resolvedPagePath = resolvePagePath(pagePath);
    List<StatisticsData> dailyViews;
    List<ObjectNode> members;

    if (fromDate != null && toDate != null) {
      dailyViews = WebPageHitRepository.findDailyWebHitsForPage(resolvedPagePath, fromDate, toDate);
      members = WebPageHitRepository.findAuthenticatedUserVisitsForPage(resolvedPagePath, fromDate, toDate, 100);
      response.put("fromDate", fromDate.toString());
      response.put("toDate", toDate.toString());
    } else {
      dailyViews = WebPageHitRepository.findDailyWebHitsForPage(resolvedPagePath, days);
      members = WebPageHitRepository.findAuthenticatedUserVisitsForPage(resolvedPagePath, days, 100);
      response.put("days", days);
    }

    if (dailyViews == null) {
      dailyViews = List.of();
    }
    if (members == null) {
      members = List.of();
    }

    long totalViews = 0;
    for (StatisticsData item : dailyViews) {
      totalViews += Long.parseLong(item.getValue());
    }

    response.put("success", true);
    response.put("pagePath", resolvedPagePath);
    response.put("requestedPagePath", pagePath);
    response.put("totalViews", totalViews);
    response.put("memberCount", members.size());
    response.put("anonymousVisits", 0);

    ArrayNode trendArray = response.putArray("trend");
    for (StatisticsData item : dailyViews) {
      ObjectNode point = trendArray.addObject();
      point.put("label", item.getLabel());
      point.put("views", Long.parseLong(item.getValue()));
    }

    ArrayNode membersArray = response.putArray("members");
    for (ObjectNode member : members) {
      membersArray.add(member);
    }
    return response;
  }

  private static String resolvePagePath(String pagePath) {
    String normalizedPagePath = StringUtils.trimToNull(pagePath);
    if (normalizedPagePath == null) {
      return null;
    }
    normalizedPagePath = normalizedPagePath.trim().toLowerCase();
    int queryIndex = normalizedPagePath.indexOf('?');
    if (queryIndex > -1) {
      normalizedPagePath = normalizedPagePath.substring(0, queryIndex);
    }
    int daysIndex = normalizedPagePath.indexOf("&days=");
    if (daysIndex > -1) {
      normalizedPagePath = normalizedPagePath.substring(0, daysIndex);
    }
    int fromDateIndex = normalizedPagePath.indexOf("&fromDate=");
    if (fromDateIndex > -1) {
      normalizedPagePath = normalizedPagePath.substring(0, fromDateIndex);
    }
    int toDateIndex = normalizedPagePath.indexOf("&toDate=");
    if (toDateIndex > -1) {
      normalizedPagePath = normalizedPagePath.substring(0, toDateIndex);
    }
    if (normalizedPagePath.endsWith("/")) {
      normalizedPagePath = normalizedPagePath.substring(0, normalizedPagePath.length() - 1);
    }
    return normalizedPagePath;
  }
}