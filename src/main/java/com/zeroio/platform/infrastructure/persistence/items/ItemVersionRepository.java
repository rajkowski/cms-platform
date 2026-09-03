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
package com.zeroio.platform.infrastructure.persistence.items;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.items.Item;
import com.zeroio.platform.domain.model.items.ItemVersion;

/**
 * Persists and retrieves item version objects
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ItemVersionRepository {

  private static Log LOG = LogFactory.getLog(ItemVersionRepository.class);

  private static String TABLE_NAME = "item_versions";
  private static String[] PRIMARY_KEY = new String[] { "item_version_id" };

  private static DataResult<ItemVersion> query(ItemVersionSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("item_version_id = ?", specification.getId());
      }
      if (specification.getItemId() > -1) {
        select.AND("item_id = ?", specification.getItemId());
      }
      if (specification.getCollectionId() > -1) {
        select.AND("collection_id = ?", specification.getCollectionId());
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(ItemVersionRepository::buildRecord);
  }

  public static ItemVersion findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_version_id = ?", id)
        .returnRecord(ItemVersionRepository::buildRecord);
  }

  public static List<ItemVersion> findAll(ItemVersionSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("created DESC");
    return query(specification, constraints).getRecords();
  }

  /**
   * Saves the current state of the item row as a version snapshot using a
   * database-native INSERT-SELECT with to_jsonb so the full row is captured.
   *
   * @param item the item whose current DB row should be snapshotted
   * @return the created ItemVersion or null on failure
   */
  public static ItemVersion saveVersion(Item item) {
    String sql = "INSERT INTO item_versions (item_id, collection_id, unique_id, name, created_by, version_data) " +
        "SELECT item_id, collection_id, unique_id, name, created_by, to_jsonb(items.*) - 'tsv' " +
        "FROM items WHERE item_id = ?";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sql, PRIMARY_KEY)) {
      pst.setLong(1, item.getId());
      if (pst.executeUpdate() > 0) {
        ResultSet generatedKeys = pst.getGeneratedKeys();
        if (generatedKeys.next()) {
          long versionId = generatedKeys.getLong(1);
          return findById(versionId);
        }
      }
    } catch (SQLException se) {
      LOG.error("saveVersion SQLException: " + se.getMessage());
    }
    return null;
  }

  public static void removeAll(Connection connection, Item item) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", item.getId()).execute(connection);
  }

  private static ItemVersion buildRecord(ResultSet rs) {
    try {
      ItemVersion record = new ItemVersion();
      record.setId(rs.getLong("item_version_id"));
      record.setItemId(rs.getLong("item_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setUniqueId(rs.getString("unique_id"));
      record.setName(rs.getString("name"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setVersionData(rs.getString("version_data"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
