/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

package com.simisinc.platform.infrastructure.persistence.cms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.cms.WebPageHit;
import com.simisinc.platform.domain.model.dashboard.StatisticsData;
import com.simisinc.platform.infrastructure.persistence.SessionRepository;

/**
 * Persists and retrieves web page hit objects
 *
 * @author matt rajkowski
 * @created 5/21/18 1:54 PM
 */
public class WebPageHitRepository {

  private static Log LOG = LogFactory.getLog(WebPageHitRepository.class);

  private static String TABLE_NAME = "web_page_hits";
  private static String[] PRIMARY_KEY = new String[] { "hit_id" };

  public static WebPageHit save(WebPageHit record) {
    return add(record);
  }

  private static WebPageHit add(WebPageHit record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("method", StringUtils.truncate(record.getMethod(), 6))
        .FIELD("page_path", StringUtils.truncate(record.getPagePath(), 255))
        .FIELD("web_page_id", record.getWebPageId() == -1 ? null : record.getWebPageId())
        .FIELD("ip_address", record.getIpAddress())
        .FIELD("session_id", record.getSessionId())
        .FIELD("is_logged_in", record.isLoggedIn());
    if (record.getHitDate() != null) {
      insert.FIELD("hit_date", record.getHitDate());
    }
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static boolean remove(WebPageHit record) {
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = createPreparedStatementForDelete(connection, record)) {
      pst.execute();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The delete failed!");
    return false;
  }

  private static PreparedStatement createPreparedStatementForDelete(Connection connection, WebPageHit record)
      throws SQLException {
    String SQL_QUERY = "DELETE FROM web_page_hits " +
        "WHERE hit_id = ?";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setLong(++i, record.getId());
    return pst;
  }

  public static void createSnapshot(Timestamp startDate, Timestamp endDate) {

    String startDateValue = new SimpleDateFormat("yyyy-MM-dd").format(startDate);

    // Query the data, skip some things
    Select select = DB.SELECT("COUNT(*)").FROM(TABLE_NAME)
        .WHERE("hit_date >= ?", startDate)
        .AND("hit_date < ?", endDate)
        .AND("page_path NOT LIKE ?", "/admin%")
        .AND("page_path NOT LIKE ?", "/assets%")
        .AND("page_path NOT LIKE ?", "/json%")
        .AND("page_path NOT LIKE ?", "%/*")
        .AND("NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE)");
    long webPageHitCount = select.returnCount();

    long uniqueSessionCount = SessionRepository.countDistinctSessions(startDate, endDate);

    // INSERT or UPDATE
    Insert insert = DB.INSERT().INTO("web_page_hit_snapshots")
        .FIELD("snapshot_date", startDate)
        .FIELD("date_value", startDateValue)
        .FIELD("web_page_hits", webPageHitCount)
        .FIELD("unique_sessions", uniqueSessionCount)
        .ON_CONFLICT("date_value")
        .DO_UPDATE()
        .SET("web_page_hits = EXCLUDED.web_page_hits")
        .SET("unique_sessions = EXCLUDED.unique_sessions");
    insert.execute();
  }

  public static void deleteOldWebHits() {
    DB.DELETE().FROM(TABLE_NAME).WHERE("hit_date < NOW() - INTERVAL '365 days'").execute();
  }

  public static List<StatisticsData> findDailyWebHits(int daysToLimit) {
    String SQL_QUERY = "SELECT date_value, web_page_hits " +
        "FROM web_page_hit_snapshots " +
        "WHERE snapshot_date > NOW() - INTERVAL '" + daysToLimit + " days' " +
        "ORDER BY snapshot_date";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("date_value"));
        data.setValue(String.valueOf(rs.getLong("web_page_hits")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findDailySessions(int daysToLimit) {
    String SQL_QUERY = "SELECT date_value, unique_sessions " +
        "FROM web_page_hit_snapshots " +
        "WHERE snapshot_date > NOW() - INTERVAL '" + daysToLimit + " days' " +
        "ORDER BY snapshot_date";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("date_value"));
        data.setValue(String.valueOf(rs.getLong("unique_sessions")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findMonthlySessions(int monthsLimit) {
    String SQL_QUERY = "SELECT DATE_TRUNC('month', month)::VARCHAR(10) AS date_column, SUM(unique_sessions) AS monthly_count "
        +
        "FROM (SELECT generate_series(NOW() - INTERVAL '" + monthsLimit
        + " months', NOW(), INTERVAL '1 month')::date) d(month) " +
        "LEFT JOIN web_page_hit_snapshots ON DATE_TRUNC('month', snapshot_date) = DATE_TRUNC('month', month) " +
        "GROUP BY d.month " +
        "ORDER BY d.month";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("date_column"));
        data.setValue(String.valueOf(rs.getLong("monthly_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findTopWebPages(int daysToLimit, int recordLimit) {
    String SQL_QUERY = "SELECT link, count(link) AS link_count " +
        "FROM web_pages " +
        "LEFT JOIN web_page_hits wph ON (wph.web_page_id = web_pages.web_page_id) " +
        "WHERE hit_date > NOW() - INTERVAL '" + daysToLimit + " days' " +
        "AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE) " +
        "GROUP BY link " +
        "ORDER BY link_count desc " +
        "LIMIT " + recordLimit;
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("link"));
        data.setValue(String.valueOf(rs.getLong("link_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findTopPaths(int value, char intervalType, int recordLimit) {
    String SQL_QUERY = "SELECT page_path, count(page_path) AS path_count " +
        "FROM web_page_hits " +
        "WHERE hit_date > NOW() - INTERVAL '" + value + " " +
        (intervalType == 'y' ? "years"
            : (intervalType == 'm' ? "months"
                : (intervalType == 'w' ? "weeks" : (intervalType == 'h' ? "hours" : "days"))))
        +
        "' " +
        "AND page_path NOT LIKE '/admin%' " +
        "AND page_path NOT LIKE '/assets/%' " +
        "AND page_path NOT LIKE '/json/%' " +
        "AND page_path NOT LIKE '%/*' " +
        "AND page_path <> '/content-editor' " +
        "AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE) " +
        "GROUP BY page_path " +
        "ORDER BY path_count desc " +
        "LIMIT " + recordLimit;
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("page_path"));
        data.setValue(String.valueOf(rs.getLong("path_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  /**
   * Find top pages with detailed metrics (views, sessions, users, avg time, bounce rate)
   */
  public static List<ObjectNode> findTopPagesWithMetrics(int days, int recordLimit) {
    String sqlQuery = "WITH page_sessions AS ( " +
        "  SELECT page_path, session_id, " +
        "    MIN(hit_date) AS first_hit, " +
        "    MAX(hit_date) AS last_hit, " +
        "    COUNT(*) AS hit_count " +
        "  FROM web_page_hits " +
        "  WHERE hit_date > NOW() - INTERVAL '" + days + " days' " +
        "  AND page_path NOT LIKE '/admin%' " +
        "  AND page_path NOT LIKE '/assets/%' " +
        "  AND page_path NOT LIKE '/json/%' " +
        "  AND page_path <> '/content-editor' " +
        "  AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE) " +
        "  GROUP BY page_path, session_id " +
        "), " +
        "page_stats AS ( " +
        "  SELECT " +
        "    page_path, " +
        "    COUNT(*) AS session_count, " +
        "    SUM(hit_count) AS total_views, " +
        "    COUNT(DISTINCT session_id) AS unique_sessions, " +
        "    AVG(EXTRACT(EPOCH FROM (last_hit - first_hit))) AS avg_time_seconds, " +
        "    SUM(CASE WHEN hit_count = 1 THEN 1 ELSE 0 END) AS bounce_count " +
        "  FROM page_sessions " +
        "  GROUP BY page_path " +
        "), " +
        "user_visits AS ( " +
        "  SELECT wph.page_path, COUNT(DISTINCT ul.user_id) AS unique_system_users " +
        "  FROM web_page_hits wph " +
        "  JOIN user_logins ul ON ul.session_id = wph.session_id " +
        "  WHERE wph.hit_date > NOW() - INTERVAL '" + days + " days' " +
        "  AND wph.is_logged_in = TRUE " +
        "  AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = wph.session_id AND is_bot = TRUE) " +
        "  GROUP BY wph.page_path " +
        ") " +
        "SELECT " +
        "  ps.page_path, " +
        "  ps.total_views AS view_count, " +
        "  ps.unique_sessions AS unique_users, " +
        "  COALESCE(uv.unique_system_users, 0) AS unique_system_users, " +
        "  ROUND(CAST(COALESCE(ps.avg_time_seconds, 0) AS NUMERIC), 1) AS avg_time_seconds, " +
        "  ROUND(CAST(CASE WHEN ps.session_count > 0 THEN (ps.bounce_count::float / ps.session_count) * 100 ELSE 0 END AS NUMERIC), 1) AS bounce_rate "
        +
        "FROM page_stats ps " +
        "LEFT JOIN user_visits uv ON ps.page_path = uv.page_path " +
        "ORDER BY ps.total_views DESC " +
        "LIMIT " + recordLimit;

    List<ObjectNode> records = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sqlQuery);
        ResultSet rs = pst.executeQuery()) {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      while (rs.next()) {
        ObjectNode node = mapper.createObjectNode();
        node.put("pagePath", rs.getString("page_path"));
        node.put("views", rs.getLong("view_count"));
        node.put("uniqueUsers", rs.getLong("unique_users"));
        node.put("uniqueSystemUsers", rs.getLong("unique_system_users"));
        node.put("avgTime", rs.getDouble("avg_time_seconds"));
        node.put("bounceRate", rs.getDouble("bounce_rate"));
        records.add(node);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  /**
   * Find top assets with metrics (downloads, views)
   */
  public static List<ObjectNode> findTopAssets(int days, int recordLimit, String assetType) {
    StringBuilder sqlQuery = new StringBuilder();
    sqlQuery.append("SELECT ");
    sqlQuery.append("page_path, ");
    sqlQuery.append("COUNT(*) AS view_count, ");
    sqlQuery.append("COUNT(CASE WHEN method = 'GET' THEN 1 END) AS download_count ");
    sqlQuery.append("FROM web_page_hits ");
    sqlQuery.append("WHERE hit_date > NOW() - INTERVAL '").append(days).append(" days' ");
    sqlQuery.append("AND (page_path LIKE '%.pdf' OR page_path LIKE '%.doc%' OR page_path LIKE '%.xls%' OR ");
    sqlQuery.append("     page_path LIKE '%.jpg' OR page_path LIKE '%.png' OR page_path LIKE '%.gif' OR ");
    sqlQuery.append("     page_path LIKE '%.zip' OR page_path LIKE '%.exe' OR page_path LIKE '%.ppt%' OR ");
    sqlQuery.append("     page_path LIKE '%.drawio' OR page_path LIKE '%.vsdx') ");
    sqlQuery.append(
        "AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE) ");

    // Add asset type filter if specified
    if (assetType != null && !assetType.trim().isEmpty()) {
      sqlQuery.append("AND (");
      switch (assetType) {
        case "PDF":
          sqlQuery.append("page_path LIKE '%.pdf'");
          break;
        case "Document":
          sqlQuery.append("page_path LIKE '%.doc' OR page_path LIKE '%.docx'");
          break;
        case "Spreadsheet":
          sqlQuery.append("page_path LIKE '%.xls' OR page_path LIKE '%.xlsx'");
          break;
        case "Presentation":
          sqlQuery.append("page_path LIKE '%.ppt' OR page_path LIKE '%.pptx'");
          break;
        case "Diagram":
          sqlQuery.append("page_path LIKE '%.drawio' OR page_path LIKE '%.vsdx'");
          break;
        case "Image":
          sqlQuery.append(
              "page_path LIKE '%.jpg' OR page_path LIKE '%.jpeg' OR page_path LIKE '%.png' OR page_path LIKE '%.gif'");
          break;
        case "Archive":
          sqlQuery.append("page_path LIKE '%.zip'");
          break;
        case "Executable":
          sqlQuery.append("page_path LIKE '%.exe'");
          break;
        case "File":
          // File is a catch-all for unknown types, so we don't filter further
          sqlQuery.append("1=1");
          break;
        default:
          sqlQuery.append("1=1"); // No additional filtering for unknown types
      }
      sqlQuery.append(") ");
    }

    sqlQuery.append("GROUP BY page_path ");
    sqlQuery.append("ORDER BY view_count DESC ");
    sqlQuery.append("LIMIT ").append(recordLimit);

    List<ObjectNode> records = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sqlQuery.toString());
        ResultSet rs = pst.executeQuery()) {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      while (rs.next()) {
        String assetPath = rs.getString("page_path");
        String assetFileType = getAssetType(assetPath);
        ObjectNode node = mapper.createObjectNode();
        node.put("assetPath", assetPath);
        node.put("assetName", assetPath.substring(assetPath.lastIndexOf("/") + 1));
        node.put("assetType", assetFileType);
        node.put("downloads", rs.getLong("download_count"));
        node.put("views", rs.getLong("view_count"));
        records.add(node);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  /**
   * Find daily hit counts for a specific file asset by its base URL prefix,
   * returning one entry per day within the given number of days.
   * Searches both /assets/file/ and /assets/view/ paths.
   */
  public static List<StatisticsData> findFileHitsByPathPrefix(String baseUrl, int daysToLimit) {
    // Generate a series of dates and left-join hit counts
    String sqlQuery = "SELECT d.date_value, COALESCE(SUM(h.hit_count), 0) AS day_count " +
        "FROM ( " +
        "  SELECT TO_CHAR(day::date, 'YYYY-MM-DD') AS date_value " +
        "  FROM generate_series( " +
        "    NOW() - INTERVAL '" + daysToLimit + " days', " +
        "    NOW(), " +
        "    INTERVAL '1 day' " +
        "  ) AS day " +
        ") d " +
        "LEFT JOIN ( " +
        "  SELECT TO_CHAR(hit_date::date, 'YYYY-MM-DD') AS day, COUNT(*) AS hit_count " +
        "  FROM web_page_hits " +
        "  WHERE (page_path LIKE ? OR page_path LIKE ?) " +
        "  AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE) " +
        "  GROUP BY TO_CHAR(hit_date::date, 'YYYY-MM-DD') " +
        ") h ON d.date_value = h.day " +
        "GROUP BY d.date_value " +
        "ORDER BY d.date_value";
    String filePrefix = "/assets/file/" + baseUrl + "%";
    String viewPrefix = "/assets/view/" + baseUrl + "%";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sqlQuery)) {
      pst.setString(1, filePrefix);
      pst.setString(2, viewPrefix);
      try (ResultSet rs = pst.executeQuery()) {
        records = new ArrayList<>();
        while (rs.next()) {
          StatisticsData data = new StatisticsData();
          data.setLabel(rs.getString("date_value"));
          data.setValue(String.valueOf(rs.getLong("day_count")));
          records.add(data);
        }
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  /**
   * Count total hits for a specific file asset by its base URL prefix.
   * Searches both /assets/file/ and /assets/view/ paths.
   */
  public static long countFileHitsByPathPrefix(String baseUrl, int daysToLimit) {
    String sqlQuery = "SELECT COUNT(*) AS hit_count " +
        "FROM web_page_hits " +
        "WHERE (page_path LIKE ? OR page_path LIKE ?) " +
        "AND hit_date > NOW() - INTERVAL '" + daysToLimit + " days' " +
        "AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE)";
    String filePrefix = "/assets/file/" + baseUrl + "%";
    String viewPrefix = "/assets/view/" + baseUrl + "%";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sqlQuery)) {
      pst.setString(1, filePrefix);
      pst.setString(2, viewPrefix);
      try (ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("hit_count");
        }
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return 0;
  }

  /**
   * Determine asset type from file extension
   */
  private static String getAssetType(String filePath) {
    if (filePath.toLowerCase().endsWith(".pdf"))
      return "PDF";
    if (filePath.toLowerCase().endsWith(".docx") || filePath.toLowerCase().endsWith(".doc"))
      return "Document";
    if (filePath.toLowerCase().endsWith(".xlsx") || filePath.toLowerCase().endsWith(".xls"))
      return "Spreadsheet";
    if (filePath.toLowerCase().endsWith(".pptx") || filePath.toLowerCase().endsWith(".ppt"))
      return "Presentation";
    if (filePath.toLowerCase().endsWith(".drawio") || filePath.toLowerCase().endsWith(".vsdx"))
      return "Diagram";
    if (filePath.toLowerCase().endsWith(".jpg") || filePath.toLowerCase().endsWith(".jpeg") ||
        filePath.toLowerCase().endsWith(".png") || filePath.toLowerCase().endsWith(".gif"))
      return "Image";
    if (filePath.toLowerCase().endsWith(".zip"))
      return "Archive";
    if (filePath.toLowerCase().endsWith(".exe"))
      return "Executable";
    return "File";
  }

  public static List<StatisticsData> findDailyWebHitsForPage(String pagePath, int daysToLimit) {
    String searchedPagePath = normalizePagePath(pagePath);
    String SQL_QUERY = "SELECT to_char(hit_date::date, 'YYYY-MM-DD') AS day, count(*) AS hit_count " +
        "FROM web_page_hits " +
        "WHERE page_path = ? " +
        "AND hit_date >= NOW() - INTERVAL '" + daysToLimit + " days' " +
        "AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE) " +
        "GROUP BY page_path, hit_date::date " +
        "ORDER BY day";
    List<StatisticsData> records = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY)) {
      pst.setString(1, searchedPagePath);
      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          StatisticsData data = new StatisticsData();
          data.setLabel(rs.getString("day"));
          data.setValue(String.valueOf(rs.getLong("hit_count")));
          records.add(data);
        }
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findDailyWebHitsForPage(String pagePath, java.time.LocalDate fromDate,
      java.time.LocalDate toDate) {
    String searchedPagePath = normalizePagePath(pagePath);
    java.time.LocalDate startDate = fromDate != null ? fromDate : toDate;
    java.time.LocalDate endDate = toDate != null ? toDate : fromDate;
    if (startDate == null || endDate == null) {
      return null;
    }
    String SQL_QUERY = "SELECT to_char(hit_date::date, 'YYYY-MM-DD') AS day, count(*) AS hit_count " +
        "FROM web_page_hits " +
        "WHERE hit_date >= ? " +
        "AND hit_date < ? " +
        "AND page_path = ? " +
        "AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE) " +
        "GROUP BY page_path, hit_date::date " +
        "ORDER BY day";
    List<StatisticsData> records = new ArrayList<>();
    Map<String, Long> hitCounts = new LinkedHashMap<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY)) {
      pst.setTimestamp(1, java.sql.Timestamp.valueOf(startDate.atStartOfDay()));
      pst.setTimestamp(2, java.sql.Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
      pst.setString(3, searchedPagePath);
      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          hitCounts.put(rs.getString("day"), rs.getLong("hit_count"));
        }
      }
      for (java.time.LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
        String day = cursor.toString();
        StatisticsData data = new StatisticsData();
        data.setLabel(day);
        data.setValue(String.valueOf(hitCounts.getOrDefault(day, 0L)));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<ObjectNode> findAuthenticatedUserVisitsForPage(String pagePath, int days) {
    java.time.LocalDate endDate = java.time.LocalDate.now();
    java.time.LocalDate startDate = endDate.minusDays(Math.max(0, days - 1));
    return findAuthenticatedUserVisitsForPageRange(pagePath, startDate, endDate);
  }

  public static List<ObjectNode> findAuthenticatedUserVisitsForPage(String pagePath, java.time.LocalDate fromDate,
      java.time.LocalDate toDate) {
    java.time.LocalDate startDate = fromDate != null ? fromDate : toDate;
    java.time.LocalDate endDate = toDate != null ? toDate : fromDate;
    if (startDate == null || endDate == null) {
      return null;
    }
    return findAuthenticatedUserVisitsForPageRange(pagePath, startDate, endDate);
  }

  private static List<ObjectNode> findAuthenticatedUserVisitsForPageRange(String pagePath,
      java.time.LocalDate startDate, java.time.LocalDate endDate) {
    String normalizedPagePath = normalizePagePath(pagePath);
    if (normalizedPagePath == null) {
      return null;
    }
    String SQL_QUERY = "SELECT * FROM (" +
        "  SELECT page_visits.page_path, " +
        "    ul.user_id, " +
        "    u.first_name, " +
        "    u.last_name, " +
        "    u.email, " +
        "    u.username, " +
        "    COUNT(DISTINCT page_visits.session_id) AS session_count, " +
        "    SUM(page_visits.visit_count) AS visit_count, " +
        "    MIN(page_visits.first_hit) AS first_hit, " +
        "    MAX(page_visits.last_hit) AS last_hit " +
        "  FROM ( " +
        "    SELECT page_path, session_id, " +
        "      MIN(hit_date) AS first_hit, " +
        "      MAX(hit_date) AS last_hit, " +
        "      COUNT(*) AS visit_count " +
        "    FROM web_page_hits " +
        "    WHERE hit_date >= ? " +
        "      AND hit_date < ? " +
        "      AND page_path = ? " +
        "      AND is_logged_in = TRUE " +
        "      AND session_id IS NOT NULL " +
        "      AND NOT EXISTS (SELECT 1 FROM sessions s WHERE s.session_id = web_page_hits.session_id AND s.is_bot = TRUE) "
        +
        "    GROUP BY page_path, session_id " +
        "  ) page_visits " +
        "  JOIN user_logins ul ON ul.session_id = page_visits.session_id " +
        "  JOIN users u ON u.user_id = ul.user_id " +
        "  GROUP BY page_visits.page_path, ul.user_id, u.first_name, u.last_name, u.email, u.username " +
        ") member_visits " +
        "ORDER BY last_hit DESC";
    List<ObjectNode> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY)) {
      pst.setTimestamp(1, java.sql.Timestamp.valueOf(startDate.atStartOfDay()));
      pst.setTimestamp(2, java.sql.Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
      pst.setString(3, normalizedPagePath);
      try (ResultSet rs = pst.executeQuery()) {
        records = new ArrayList<>();
        while (rs.next()) {
          ObjectNode node = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
          node.put("pagePath", rs.getString("page_path"));
          node.put("userId", rs.getLong("user_id"));
          node.put("fullName", StringUtils.trimToNull(rs.getString("first_name") + " " + rs.getString("last_name")));
          node.put("email", rs.getString("email"));
          node.put("username", rs.getString("username"));
          node.put("sessionCount", rs.getLong("session_count"));
          node.put("visitCount", rs.getLong("visit_count"));
          node.put("firstVisit", rs.getTimestamp("first_hit") != null ? rs.getTimestamp("first_hit").toString() : null);
          node.put("lastVisit", rs.getTimestamp("last_hit") != null ? rs.getTimestamp("last_hit").toString() : null);
          records.add(node);
        }
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  private static String normalizePagePath(String pagePath) {
    String normalizedPagePath = StringUtils.trimToNull(pagePath);
    if (normalizedPagePath == null) {
      return null;
    }
    int queryIndex = normalizedPagePath.indexOf('?');
    if (queryIndex > -1) {
      normalizedPagePath = normalizedPagePath.substring(0, queryIndex);
    }
    if (normalizedPagePath.endsWith("/") && normalizedPagePath.length() > 1) {
      normalizedPagePath = normalizedPagePath.substring(0, normalizedPagePath.length() - 1);
    }
    return normalizedPagePath;
  }
}
