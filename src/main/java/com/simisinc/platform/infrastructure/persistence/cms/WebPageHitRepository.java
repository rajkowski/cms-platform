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

package com.simisinc.platform.infrastructure.persistence.cms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.domain.model.cms.WebPageHit;
import com.simisinc.platform.domain.model.dashboard.StatisticsData;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.SqlUtils;
import com.simisinc.platform.infrastructure.database.SqlWhere;
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
    SqlUtils insertValues = new SqlUtils()
        .add("method", record.getMethod(), 6)
        .add("page_path", record.getPagePath(), 255)
        .add("web_page_id", record.getWebPageId(), -1)
        .add("ip_address", record.getIpAddress())
        .add("session_id", record.getSessionId())
        .add("is_logged_in", record.isLoggedIn());
    if (record.getHitDate() != null) {
      insertValues.add("hit_date", record.getHitDate());
    }
    record.setId(DB.insertInto(TABLE_NAME, insertValues, PRIMARY_KEY));
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

  private static PreparedStatement createPreparedStatementForDelete(Connection connection, WebPageHit record) throws SQLException {
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
    SqlWhere where = DB.WHERE()
        .AND("hit_date >= ?", startDate)
        .AND("hit_date < ?", endDate)
        .AND("page_path NOT LIKE ?", "/admin%")
        .AND("page_path NOT LIKE ?", "/assets%")
        .AND("page_path NOT LIKE ?", "/json%")
        .AND("page_path NOT LIKE ?", "%/*")
        .AND("NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE)");
    long webPageHitCount = DB.selectCountFrom(TABLE_NAME, where);

    long uniqueSessionCount = SessionRepository.countDistinctSessions(startDate, endDate);

    // INSERT or UPDATE
    SqlUtils insertValues = new SqlUtils()
        .add("snapshot_date", startDate)
        .add("date_value", startDateValue)
        .add("web_page_hits", webPageHitCount)
        .add("unique_sessions", uniqueSessionCount);

    String onConflict = "ON CONFLICT (date_value) " +
        "DO UPDATE SET " +
        "web_page_hits = EXCLUDED.web_page_hits, " +
        "unique_sessions = EXCLUDED.unique_sessions";

    DB.insertIntoWithConflict("web_page_hit_snapshots", insertValues, onConflict);
  }

  public static void deleteOldWebHits() {
    DB.deleteFrom(TABLE_NAME, DB.WHERE("hit_date < NOW() - INTERVAL '365 days'"));
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
    String SQL_QUERY = "SELECT DATE_TRUNC('month', month)::VARCHAR(10) AS date_column, SUM(unique_sessions) AS monthly_count " +
        "FROM (SELECT generate_series(NOW() - INTERVAL '" + monthsLimit + " months', NOW(), INTERVAL '1 month')::date) d(month) " +
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
            : (intervalType == 'm' ? "months" : (intervalType == 'w' ? "weeks" : (intervalType == 'h' ? "hours" : "days"))))
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
   * Find top pages with detailed metrics (views, unique users, avg time, bounce rate)
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
        ") " +
        "SELECT " +
        "  ps.page_path, " +
        "  ps.total_views AS view_count, " +
        "  ps.unique_sessions, " +
        "  COUNT(DISTINCT wph.ip_address) AS unique_users, " +
        "  ROUND(CAST(COALESCE(ps.avg_time_seconds, 0) AS NUMERIC), 1) AS avg_time_seconds, " +
        "  ROUND(CAST(CASE WHEN ps.session_count > 0 THEN (ps.bounce_count::float / ps.session_count) * 100 ELSE 0 END AS NUMERIC), 1) AS bounce_rate " +
        "FROM page_stats ps " +
        "LEFT JOIN web_page_hits wph ON ps.page_path = wph.page_path " +
        "  AND wph.hit_date > NOW() - INTERVAL '" + days + " days' " +
        "GROUP BY ps.page_path, ps.total_views, ps.unique_sessions, ps.avg_time_seconds, ps.session_count, ps.bounce_count " +
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
  public static List<ObjectNode> findTopAssets(int days, int recordLimit) {
    String sqlQuery = "SELECT " +
        "page_path, " +
        "COUNT(*) AS view_count, " +
        "COUNT(CASE WHEN method = 'GET' THEN 1 END) AS download_count " +
        "FROM web_page_hits " +
        "WHERE hit_date > NOW() - INTERVAL '" + days + " days' " +
        "AND (page_path LIKE '%.pdf' OR page_path LIKE '%.doc%' OR page_path LIKE '%.xls%' OR " +
        "     page_path LIKE '%.jpg' OR page_path LIKE '%.png' OR page_path LIKE '%.gif' OR " +
        "     page_path LIKE '%.zip' OR page_path LIKE '%.exe') " +
        "AND NOT EXISTS (SELECT 1 FROM sessions WHERE session_id = web_page_hits.session_id AND is_bot = TRUE) " +
        "GROUP BY page_path " +
        "ORDER BY view_count DESC " +
        "LIMIT " + recordLimit;

    List<ObjectNode> records = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sqlQuery);
        ResultSet rs = pst.executeQuery()) {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      while (rs.next()) {
        String assetPath = rs.getString("page_path");
        String assetType = getAssetType(assetPath);
        ObjectNode node = mapper.createObjectNode();
        node.put("assetPath", assetPath);
        node.put("assetName", assetPath.substring(assetPath.lastIndexOf("/") + 1));
        node.put("assetType", assetType);
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
   * Determine asset type from file extension
   */
  private static String getAssetType(String filePath) {
    if (filePath.toLowerCase().endsWith(".pdf")) return "PDF";
    if (filePath.toLowerCase().endsWith(".docx") || filePath.toLowerCase().endsWith(".doc")) return "Document";
    if (filePath.toLowerCase().endsWith(".xlsx") || filePath.toLowerCase().endsWith(".xls")) return "Spreadsheet";
    if (filePath.toLowerCase().endsWith(".pptx") || filePath.toLowerCase().endsWith(".ppt")) return "Presentation";
    if (filePath.toLowerCase().endsWith(".drawio") || filePath.toLowerCase().endsWith(".vsdx")) return "Diagram";
    if (filePath.toLowerCase().endsWith(".jpg") || filePath.toLowerCase().endsWith(".jpeg") || 
        filePath.toLowerCase().endsWith(".png") || filePath.toLowerCase().endsWith(".gif")) return "Image";
    if (filePath.toLowerCase().endsWith(".zip")) return "Archive";
    if (filePath.toLowerCase().endsWith(".exe")) return "Executable";
    return "File";
  }
}
