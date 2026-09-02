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

package com.simisinc.platform.infrastructure.persistence.items;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Insert;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.CollectionGroup;
import com.simisinc.platform.domain.model.items.PrivacyType;

/**
 * Persists and retrieves collection group objects
 *
 * @author matt rajkowski
 * @created 7/19/18 9:29 AM
 */
public class CollectionGroupRepository {

  private static Log LOG = LogFactory.getLog(CollectionGroupRepository.class);

  private static String TABLE_NAME = "collection_groups";
  private static String[] PRIMARY_KEY = new String[] { "allowed_id" };

  public static List<CollectionGroup> findAllByCollectionId(long collectionId) {
    if (collectionId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id = ?", collectionId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("allowed_id").setUseCount(false))
        .returnDataResult(CollectionGroupRepository::buildRecord).getRecords();
  }

  public static List<CollectionGroup> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("allowed_id"))
        .returnDataResult(CollectionGroupRepository::buildRecord).getRecords();
  }

  public static CollectionGroup add(CollectionGroup record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("collection_id", record.getCollectionId())
        .FIELD("group_id", record.getGroupId())
        .FIELD("privacy_type", record.getPrivacyType())
        .FIELD("view_all", record.getPrivacyType() != PrivacyType.PRIVATE)
        .FIELD("add_permission", record.getAddPermission())
        .FIELD("edit_permission", record.getEditPermission())
        .FIELD("delete_permission", record.getDeletePermission());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void insertCollectionGroupList(Connection connection, Collection collection) throws SQLException {
    if (collection.getCollectionGroupList() == null) {
      return;
    }
    for (CollectionGroup allowedGroup : collection.getCollectionGroupList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("collection_id", collection.getId())
          .FIELD("group_id", allowedGroup.getGroupId())
          .FIELD("privacy_type", allowedGroup.getPrivacyType())
          .FIELD("view_all", allowedGroup.getPrivacyType() != PrivacyType.PRIVATE)
          .FIELD("add_permission", allowedGroup.getAddPermission())
          .FIELD("edit_permission", allowedGroup.getEditPermission())
          .FIELD("delete_permission", allowedGroup.getDeletePermission())
          .execute(connection);
    }
  }

  public static void removeAll(Connection connection, Collection collection) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", collection.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Group group) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("group_id = ?", group.getId()).execute(connection);
  }

  private static CollectionGroup buildRecord(ResultSet rs) {
    try {
      CollectionGroup record = new CollectionGroup();
      record.setId(rs.getLong("allowed_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setGroupId(rs.getLong("group_id"));
      record.setPrivacyType(rs.getInt("privacy_type"));
      record.setAddPermission(rs.getBoolean("add_permission"));
      record.setEditPermission(rs.getBoolean("edit_permission"));
      record.setDeletePermission(rs.getBoolean("delete_permission"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
