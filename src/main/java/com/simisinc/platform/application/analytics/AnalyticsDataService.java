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

    // Get data from repositories for current period
    List<StatisticsData> dailySessions = WebPageHitRepository.findDailySessions(days);
    List<StatisticsData> dailyLogins = UserLoginRepository.findUniqueDailyLogins(days);
    List<StatisticsData> dailyHits = WebPageHitRepository.findDailyWebHits(days);

    // Calculate KPIs for current period
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

    // Get data for previous period to calculate trends
    // Query the previous equivalent period
    List<StatisticsData> prevDailySessions = WebPageHitRepository.findDailySessions(days * 2);
    List<StatisticsData> prevDailyLogins = UserLoginRepository.findUniqueDailyLogins(days * 2);
    List<StatisticsData> prevDailyHits = WebPageHitRepository.findDailyWebHits(days * 2);

    // Extract previous period data (older half of the data)
    long prevTotalSessions = 0;
    long prevTotalHits = 0;
    long prevTotalUsers = 0;
    
    if (prevDailySessions != null && prevDailySessions.size() > days) {
      prevTotalSessions = prevDailySessions.stream()
          .skip(Math.max(0, prevDailySessions.size() - days * 2))
          .limit(days)
          .mapToLong(d -> Long.parseLong(d.getValue()))
          .sum();
    }
    if (prevDailyHits != null && prevDailyHits.size() > days) {
      prevTotalHits = prevDailyHits.stream()
          .skip(Math.max(0, prevDailyHits.size() - days * 2))
          .limit(days)
          .mapToLong(d -> Long.parseLong(d.getValue()))
          .sum();
    }
    if (prevDailyLogins != null && prevDailyLogins.size() > days) {
      prevTotalUsers = prevDailyLogins.stream()
          .skip(Math.max(0, prevDailyLogins.size() - days * 2))
          .limit(days)
          .mapToLong(d -> Long.parseLong(d.getValue()))
          .sum();
    }

    double prevAvgSessionDuration = SessionRepository.findAverageSessionDuration(days * 2);

    // Calculate previous period bounce rate
    double prevBounceRate = SessionRepository.findBounceRate(days * 2) / 100.0;

    // Calculate previous period new users
    LocalDate prevStart = start.minusDays(days);
    LocalDate prevEnd = end.minusDays(days);
    long prevNewUsersCount = UserRepository.countNewUsers(
        Timestamp.valueOf(prevStart.atStartOfDay()),
        Timestamp.valueOf(prevEnd.plusDays(1).atStartOfDay())
    );

    // Calculate trend percentages
    double usersTrend = calculateTrendPercentage(prevTotalUsers, totalUsers);
    double sessionsTrend = calculateTrendPercentage(prevTotalSessions, totalSessions);
    double pageViewsTrend = calculateTrendPercentage(prevTotalHits, totalHits);
    double avgDurationTrend = calculateTrendPercentage(prevAvgSessionDuration, avgSessionDuration);
    double bounceRateTrend = calculateTrendPercentage(prevBounceRate, bounceRate);
    double newUsersTrend = calculateTrendPercentage(prevNewUsersCount, newUsersCount);

    response.put("success", true);
    response.put("rangeStart", rangeStart);
    response.put("rangeEnd", rangeEnd);
    response.put("timezone", ZoneId.systemDefault().getId());
    response.put("generatedAt", System.currentTimeMillis());

    // KPIs with trend values
    ObjectNode kpis = response.putObject("kpis");
    kpis.put("activeUsers", totalUsers);
    kpis.put("activeUsersTrend", Math.round(usersTrend * 10.0) / 10.0);
    kpis.put("sessions", totalSessions);
    kpis.put("sessionsTrend", Math.round(sessionsTrend * 10.0) / 10.0);
    kpis.put("pageViews", totalHits);
    kpis.put("pageViewsTrend", Math.round(pageViewsTrend * 10.0) / 10.0);
    kpis.put("avgSessionDuration", Math.round(avgSessionDuration));
    kpis.put("avgSessionDurationTrend", Math.round(avgDurationTrend * 10.0) / 10.0);
    kpis.put("bounceRate", String.format("%.1f%%", bounceRate * 100));
    kpis.put("bounceRateTrend", Math.round(bounceRateTrend * 10.0) / 10.0);
    kpis.put("newUsers", newUsersCount);
    kpis.put("newUsersTrend", Math.round(newUsersTrend * 10.0) / 10.0);

    // Trend series
    ArrayNode trendArray = MAPPER.valueToTree(dailySessions);
    response.putArray("trendSeries").addAll(trendArray);

    return response;
  }

  /**
   * Calculate trend percentage change between previous and current values
   * Formula: ((current - previous) / previous) * 100
   */
  private static double calculateTrendPercentage(double previous, double current) {
    if (previous == 0) {
      return current > 0 ? 100.0 : 0.0;
    }
    return ((current - previous) / previous) * 100.0;
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

    // Get visitor segmentation metrics
    long newVisitors = SessionRepository.findNewVisitors(days);
    long returningVisitors = SessionRepository.findReturningVisitors(days);
    long authenticatedUsers = UserLoginRepository.findUniqueAuthenticatedUsers(days);

    // Get devices and browsers data from sessions
    List<StatisticsData> devices = SessionRepository.findTopDevices(days);
    List<StatisticsData> browsers = SessionRepository.findTopBrowsers(days);
    
    // Get average session duration
    double avgSessionDuration = SessionRepository.findAverageSessionDuration(days);

    // Visitor segments
    ObjectNode segments = response.putObject("segments");
    segments.put("newVisitors", newVisitors);
    segments.put("returningVisitors", returningVisitors);
    segments.put("authenticatedUsers", authenticatedUsers);

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
   * Load bounce rate trend data
   */
  public static ObjectNode bounceRateTrend(String rangeStart, String rangeEnd) {
    ObjectNode response = MAPPER.createObjectNode();

    // Parse dates
    LocalDate start = LocalDate.parse(rangeStart);
    LocalDate end = LocalDate.parse(rangeEnd);
    int days = (int) ChronoUnit.DAYS.between(start, end) + 1;

    // Get daily bounce rate data
    List<StatisticsData> dailyBounceRate = SessionRepository.findDailyBounceRate(days);

    response.put("success", true);
    response.put("rangeStart", rangeStart);
    response.put("rangeEnd", rangeEnd);
    response.put("generatedAt", System.currentTimeMillis());

    // Daily bounce rate series
    ArrayNode trendArray = MAPPER.valueToTree(dailyBounceRate);
    response.putArray("series").addAll(trendArray);

    return response;
  }

  /**
   * Load new users trend data
   */
  public static ObjectNode newUsersTrend(String rangeStart, String rangeEnd) {
    ObjectNode response = MAPPER.createObjectNode();

    // Parse dates
    LocalDate start = LocalDate.parse(rangeStart);
    LocalDate end = LocalDate.parse(rangeEnd);
    int days = (int) ChronoUnit.DAYS.between(start, end) + 1;

    // Get daily new user registration data
    List<StatisticsData> dailyNewUsers = UserRepository.findDailyUserRegistrations(days);

    response.put("success", true);
    response.put("rangeStart", rangeStart);
    response.put("rangeEnd", rangeEnd);
    response.put("generatedAt", System.currentTimeMillis());

    // Daily new users series
    ArrayNode trendArray = MAPPER.valueToTree(dailyNewUsers);
    response.putArray("series").addAll(trendArray);

    return response;
  }
}
