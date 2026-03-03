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

package com.simisinc.platform.infrastructure.persistence.analytics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.domain.model.analytics.PerformanceMetric;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.SqlUtils;

/**
 * Persists and retrieves performance metric records
 *
 * @author matt rajkowski
 * @created 02/27/26 09:00 AM
 */
public class PerformanceMetricRepository {

  private static Log LOG = LogFactory.getLog(PerformanceMetricRepository.class);

  private static final String TABLE_NAME = "performance_metrics";
  private static final String[] PRIMARY_KEY = new String[] { "metric_id" };

  public static PerformanceMetric save(PerformanceMetric record) {
    SqlUtils insertValues = new SqlUtils()
        .add("request_type", record.getRequestType(), 10)
        .add("status_code", record.getStatusCode())
        .add("duration_ms", record.getDurationMs());
    if (record.getMetricDate() != null) {
      insertValues.add("metric_date", record.getMetricDate());
    }
    record.setId(DB.insertInto(TABLE_NAME, insertValues, PRIMARY_KEY));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  /**
   * Query aggregate statistics (p50, p95, p99, avg, count) for a given
   * request type over the last {@code days} days.
   */
  public static ObjectNode findAggregatesForType(String requestType, int days) {
    String SQL = "SELECT " +
        "  percentile_cont(0.50) WITHIN GROUP (ORDER BY duration_ms)::bigint AS p50, " +
        "  percentile_cont(0.95) WITHIN GROUP (ORDER BY duration_ms)::bigint AS p95, " +
        "  percentile_cont(0.99) WITHIN GROUP (ORDER BY duration_ms)::bigint AS p99, " +
        "  ROUND(AVG(duration_ms))::bigint AS avg_ms, " +
        "  COUNT(*) AS total_count " +
        "FROM " + TABLE_NAME + " " +
        "WHERE request_type = ? " +
        "AND metric_date >= NOW() - (? || ' days')::interval";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL)) {
      int i = 0;
      pst.setString(++i, requestType);
      pst.setInt(++i, days);
      try (ResultSet rs = pst.executeQuery()) {
        if (rs.next()) {
          com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
          ObjectNode node = mapper.createObjectNode();
          node.put("p50", rs.getLong("p50"));
          node.put("p95", rs.getLong("p95"));
          node.put("p99", rs.getLong("p99"));
          node.put("avg", rs.getLong("avg_ms"));
          node.put("count", rs.getLong("total_count"));
          return node;
        }
      }
    } catch (Exception e) {
      LOG.error("findAggregatesForType error: " + e.getMessage());
    }
    return null;
  }

  /**
   * Query HTTP status code breakdown for all types over the last {@code days} days.
   */
  public static List<ObjectNode> findStatusCodeBreakdown(int days) {
    String SQL = "SELECT status_code, COUNT(*) AS error_count " +
        "FROM " + TABLE_NAME + " " +
        "WHERE metric_date >= NOW() - (? || ' days')::interval " +
        "AND status_code >= 400 " +
        "GROUP BY status_code " +
        "ORDER BY error_count DESC " +
        "LIMIT 20";
    List<ObjectNode> results = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL)) {
      pst.setInt(1, days);
      try (ResultSet rs = pst.executeQuery()) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        while (rs.next()) {
          ObjectNode node = mapper.createObjectNode();
          node.put("statusCode", rs.getInt("status_code"));
          node.put("count", rs.getLong("error_count"));
          results.add(node);
        }
      }
    } catch (Exception e) {
      LOG.error("findStatusCodeBreakdown error: " + e.getMessage());
    }
    return results;
  }

  /**
   * Delete records older than the specified number of days.
   */
  public static void deleteOlderThan(int days) {
    String SQL = "DELETE FROM " + TABLE_NAME + " WHERE metric_date < NOW() - (? || ' days')::interval";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL)) {
      pst.setInt(1, days);
      int deleted = pst.executeUpdate();
      if (deleted > 0) {
        LOG.debug("Deleted " + deleted + " performance metric records older than " + days + " days");
      }
    } catch (SQLException se) {
      LOG.error("deleteOlderThan error: " + se.getMessage());
    }
  }
}
