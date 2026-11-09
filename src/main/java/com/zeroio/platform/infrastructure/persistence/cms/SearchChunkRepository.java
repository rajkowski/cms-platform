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
package com.zeroio.platform.infrastructure.persistence.cms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;

/**
 * Stores text chunks and their embeddings for the unified_search sources
 *
 * @author matt rajkowski
 * @created 11/5/26 3:00 PM
 */
public class SearchChunkRepository {

  private static Log LOG = LogFactory.getLog(SearchChunkRepository.class);

  private static final String TABLE_NAME = "search_chunks";

  // unified_search result_type -> search_chunks source column
  public static final Map<String, String> SOURCE_COLUMNS = Map.of(
      "pages", "web_page_id",
      "posts", "post_id",
      "calendar", "event_id",
      "attachments", "file_id",
      "resources", "item_id",
      "resourceFiles", "item_file_id");

  public record Source(long id, String text) {
  }

  public record PendingChunk(long id, String text) {
  }

  /** Sources which have no chunks yet, or which changed after their chunks were made */
  public static List<Source> findSourcesToChunk(String resultType, int limit) throws SQLException {
    String column = SOURCE_COLUMNS.get(resultType);
    String sql = "SELECT u.result_id, u.text_content FROM unified_search u"
        + " WHERE u.result_type = ? AND btrim(COALESCE(u.text_content, '')) <> ''"
        + " AND NOT EXISTS (SELECT 1 FROM " + TABLE_NAME + " c WHERE c." + column + " = u.result_id AND c.modified >= u.modified)"
        + " ORDER BY u.result_id LIMIT ?";
    List<Source> sources = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sql)) {
      pst.setString(1, resultType);
      pst.setInt(2, limit);
      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          sources.add(new Source(rs.getLong("result_id"), rs.getString("text_content")));
        }
      }
    }
    return sources;
  }

  /** Unchanged chunks are only touched so they are not found again; otherwise the chunks (and embeddings) are replaced */
  public static void replaceChunks(String resultType, long sourceId, List<String> chunks, List<String> hashes) throws SQLException {
    String column = SOURCE_COLUMNS.get(resultType);
    try (Connection connection = DB.getConnection()) {
      connection.setAutoCommit(false);
      try {
        List<String> existingHashes = new ArrayList<>();
        try (PreparedStatement pst = connection
            .prepareStatement("SELECT content_hash FROM " + TABLE_NAME + " WHERE " + column + " = ? ORDER BY chunk_index")) {
          pst.setLong(1, sourceId);
          try (ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
              existingHashes.add(rs.getString("content_hash"));
            }
          }
        }
        if (existingHashes.equals(hashes)) {
          try (PreparedStatement pst = connection
              .prepareStatement("UPDATE " + TABLE_NAME + " SET modified = CURRENT_TIMESTAMP WHERE " + column + " = ?")) {
            pst.setLong(1, sourceId);
            pst.executeUpdate();
          }
        } else {
          try (PreparedStatement pst = connection.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE " + column + " = ?")) {
            pst.setLong(1, sourceId);
            pst.executeUpdate();
          }
          try (PreparedStatement pst = connection.prepareStatement(
              "INSERT INTO " + TABLE_NAME + " (" + column + ", chunk_index, chunk_text, content_hash) VALUES (?, ?, ?, ?)")) {
            for (int index = 0; index < chunks.size(); index++) {
              pst.setLong(1, sourceId);
              pst.setInt(2, index);
              pst.setString(3, chunks.get(index));
              pst.setString(4, hashes.get(index));
              pst.addBatch();
            }
            pst.executeBatch();
          }
        }
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    }
  }

  public static List<PendingChunk> findPendingEmbeddings(int limit) throws SQLException {
    List<PendingChunk> chunks = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection
            .prepareStatement("SELECT chunk_id, chunk_text FROM " + TABLE_NAME + " WHERE embedding IS NULL ORDER BY chunk_id LIMIT ?")) {
      pst.setInt(1, limit);
      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          chunks.add(new PendingChunk(rs.getLong("chunk_id"), rs.getString("chunk_text")));
        }
      }
    }
    return chunks;
  }

  public static void saveEmbeddings(List<PendingChunk> chunks, List<float[]> vectors, String modelName) throws SQLException {
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(
            "UPDATE " + TABLE_NAME + " SET embedding = ?::vector, embedding_model = ?, embedded = CURRENT_TIMESTAMP WHERE chunk_id = ?")) {
      for (int index = 0; index < chunks.size(); index++) {
        pst.setString(1, toVectorLiteral(vectors.get(index)));
        pst.setString(2, modelName);
        pst.setLong(3, chunks.get(index).id());
        pst.addBatch();
      }
      pst.executeBatch();
    } catch (SQLException e) {
      LOG.error("saveEmbeddings failed: " + e.getMessage());
      throw e;
    }
  }

  /** Nearest sources as "resultType:resultId" keys, closest first, using each source's best chunk */
  public static List<String> findNearestSources(float[] vector, String modelName, int chunkLimit, int sourceLimit, double maxDistance) throws SQLException {
    String literal = toVectorLiteral(vector);
    String sql = "SELECT CASE"
        + " WHEN web_page_id IS NOT NULL THEN 'pages:' || web_page_id"
        + " WHEN post_id IS NOT NULL THEN 'posts:' || post_id"
        + " WHEN event_id IS NOT NULL THEN 'calendar:' || event_id"
        + " WHEN file_id IS NOT NULL THEN 'attachments:' || file_id"
        + " WHEN item_id IS NOT NULL THEN 'resources:' || item_id"
        + " ELSE 'resourceFiles:' || item_file_id END AS source_key, MIN(distance) AS best"
        + " FROM (SELECT web_page_id, post_id, event_id, file_id, item_id, item_file_id, embedding <=> ?::vector AS distance FROM " + TABLE_NAME
        + " WHERE embedding IS NOT NULL AND embedding_model = ? ORDER BY embedding <=> ?::vector LIMIT ?) nearest"
        + " WHERE distance <= ? GROUP BY 1 ORDER BY best, 1 LIMIT ?";
    List<String> keys = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sql)) {
      pst.setString(1, literal);
      pst.setString(2, modelName);
      pst.setString(3, literal);
      pst.setInt(4, chunkLimit);
      pst.setDouble(5, maxDistance);
      pst.setInt(6, sourceLimit);
      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          keys.add(rs.getString("source_key"));
        }
      }
    }
    return keys;
  }

  private static String toVectorLiteral(float[] vector) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(vector[i]);
    }
    return sb.append(']').toString();
  }
}
