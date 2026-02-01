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

package com.simisinc.platform.application.analytics;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.domain.model.dashboard.ActiveSessionData;
import com.simisinc.platform.domain.model.dashboard.StatisticsData;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHitRepository;
import com.simisinc.platform.infrastructure.persistence.login.UserLoginRepository;
import com.simisinc.platform.infrastructure.persistence.SessionRepository;
import com.simisinc.platform.infrastructure.persistence.UserRepository;

/**
 * Service for aggregating and providing analytics data
 *
 * @author matt rajkowski
 * @created 01/31/26 02:00 PM
 */
public class AnalyticsDataService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Load overview KPIs and trend data
   */
  public static ObjectNode loadOverview(String rangeStart, String rangeEnd) {
    ObjectNode response = MAPPER.createObjectNode();

    // Parse dates
    LocalDate start = LocalDate.parse(rangeStart);
    LocalDate end = LocalDate.parse(rangeEnd);

    // The queries will need to be updated to handle different date ranges
    
    // This method currently assumes daily data from based on date amounts
    int days = (int) ChronoUnit.DAYS.between(start, end) + 1;

    // Get data from repositories
    List<StatisticsData> dailySessions = WebPageHitRepository.findDailySessions(days);
    List<StatisticsData> dailyLogins = UserLoginRepository.findUniqueDailyLogins(days);
    List<StatisticsData> dailyHits = WebPageHitRepository.findDailyWebHits(days);

    // Calculate KPIs
    long totalSessions = dailySessions.stream().mapToLong(d -> Long.parseLong(d.getValue())).sum();
    long totalHits = dailyHits.stream().mapToLong(d -> Long.parseLong(d.getValue())).sum();
    long totalUsers = dailyLogins.stream().mapToLong(d -> Long.parseLong(d.getValue())).sum();

    // Calculate new users based on user creation dates within range
    long newUsersCount = UserRepository.countNewUsers(
        Timestamp.valueOf(start.atStartOfDay()),
        Timestamp.valueOf(end.plusDays(1).atStartOfDay())
    );

    // Calculate bounce rate based on single-page sessions
    double bounceRate = SessionRepository.findBounceRate(days) / 100.0;

    // Calculate avg session duration from session data
    double avgSessionDuration = SessionRepository.findAverageSessionDuration(days);

    response.put("success", true);
    response.put("rangeStart", rangeStart);
    response.put("rangeEnd", rangeEnd);
    response.put("timezone", ZoneId.systemDefault().getId());
    response.put("generatedAt", System.currentTimeMillis());

    // KPIs
    ObjectNode kpis = response.putObject("kpis");
    kpis.put("activeUsers", totalUsers);
    kpis.put("sessions", totalSessions);
    kpis.put("pageViews", totalHits);
    kpis.put("avgSessionDuration", Math.round(avgSessionDuration));
    kpis.put("bounceRate", String.format("%.1f%%", bounceRate * 100));
    kpis.put("newUsers", newUsersCount);

    // Trend series
    ArrayNode trendArray = MAPPER.valueToTree(dailySessions);
    response.putArray("trendSeries").addAll(trendArray);

    return response;
  }

  /**
   * Load live session data
   */
  public static ObjectNode loadLive(String filterPage, String filterDevice) {
    ObjectNode response = MAPPER.createObjectNode();

    response.put("success", true);
    response.put("generatedAt", System.currentTimeMillis());

    // Active sessions from the session store
    ArrayNode activeSessionsArray = response.putArray("activeSessions");
    List<ActiveSessionData> activeSessions = SessionRepository.findActiveSessions(20, 10);
    if (activeSessions != null) {
      for (ActiveSessionData session : activeSessions) {
        ObjectNode sessionObj = activeSessionsArray.addObject();
        sessionObj.put("sessionId", session.getSessionId());
        sessionObj.put("userType", session.getUserType());
        sessionObj.put("page", session.getPage());
        sessionObj.put("device", session.getDevice());
        sessionObj.put("location", session.getLocation());
        sessionObj.put("duration", session.getDuration());
      }
    }

    response.putArray("recentEvents");

    return response;
  }

  /**
   * Load content performance data
   */
  public static ObjectNode loadContent(String rangeStart, String rangeEnd, int daysToLimit) {
    ObjectNode response = MAPPER.createObjectNode();

    // Get top pages with metrics
    List<ObjectNode> topPages = WebPageHitRepository.findTopPagesWithMetrics(daysToLimit, 10);

    // Get top assets
    List<ObjectNode> topAssets = WebPageHitRepository.findTopAssets(daysToLimit, 10);

    response.put("success", true);
    response.put("rangeStart", rangeStart);
    response.put("rangeEnd", rangeEnd);
    response.put("generatedAt", System.currentTimeMillis());

    // Top pages
    ArrayNode topPagesArray = MAPPER.valueToTree(topPages);
    response.set("topPages", topPagesArray);

    // Top assets
    ArrayNode topAssetsArray = MAPPER.valueToTree(topAssets);
    response.set("topAssets", topAssetsArray);

    response.putArray("searchQueries");
    response.putArray("referrers");

    return response;
  }

  /**
   * Load audience segmentation data
   */
  public static ObjectNode loadAudience(String rangeStart, String rangeEnd) {
    ObjectNode response = MAPPER.createObjectNode();

    // Parse dates
    LocalDate start = LocalDate.parse(rangeStart);
    LocalDate end = LocalDate.parse(rangeEnd);
    int days = (int) ChronoUnit.DAYS.between(start, end) + 1;

    response.put("success", true);
    response.put("rangeStart", rangeStart);
    response.put("rangeEnd", rangeEnd);
    response.put("generatedAt", System.currentTimeMillis());

    // Get devices and browsers data from sessions
    List<StatisticsData> devices = SessionRepository.findTopDevices(days);
    List<StatisticsData> browsers = SessionRepository.findTopBrowsers(days);
    
    // Get average session duration
    double avgSessionDuration = SessionRepository.findAverageSessionDuration(days);

    // Devices
    ArrayNode devicesArray = MAPPER.valueToTree(devices);
    response.putArray("devices").addAll(devicesArray);

    // Browsers
    ArrayNode browsersArray = MAPPER.valueToTree(browsers);
    response.putArray("browsers").addAll(browsersArray);

    // Average session duration (in seconds)
    response.put("avgSessionDuration", Math.round(avgSessionDuration));

    return response;
  }

  /**
   * Load technical metrics
   */
  public static ObjectNode loadTechnical(String rangeStart, String rangeEnd) {
    ObjectNode response = MAPPER.createObjectNode();

    response.put("success", true);
    response.put("rangeStart", rangeStart);
    response.put("rangeEnd", rangeEnd);
    response.put("generatedAt", System.currentTimeMillis());

    // Performance metrics
    ObjectNode performance = response.putObject("performance");
    performance.put("p50", 450);
    performance.put("p95", 1200);
    performance.put("p99", 2500);
    performance.put("errorRate", 0.01);
    performance.put("cacheHitRate", 0.85);

    return response;
  }

  /**
   * Load available filter options
   */
  public static ObjectNode loadFilterOptions() {
    ObjectNode response = MAPPER.createObjectNode();

    response.put("success", true);

    // Available pages
    response.putArray("pages");

    // Available sections
    response.putArray("sections");

    // Available roles
    response.putArray("roles");

    // Available devices
    response.putArray("devices")
        .add("Desktop")
        .add("Mobile")
        .add("Tablet");

    // Available browsers
    response.putArray("browsers")
        .add("Chrome")
        .add("Safari")
        .add("Firefox")
        .add("Edge")
        .add("Opera")
        .add("Internet Explorer")
        .add("Other");

    // Available locations
    response.putArray("locations");

    return response;
  }
}
