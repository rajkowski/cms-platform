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
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.CollectionRelationship;

/**
 * Persists and retrieves collection relationship objects
 *
 * @author matt rajkowski
 * @created 7/26/18 1:09 PM
 */
public class CollectionRelationshipRepository {

  private static Log LOG = LogFactory.getLog(CollectionRelationshipRepository.class);

  private static String TABLE_NAME = "collection_relationships";
  private static String[] PRIMARY_KEY = new String[] { "relationship_id" };

  public static List<CollectionRelationship> findAllParentsByCollectionId(long collectionId) {
    if (collectionId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("related_collection_id = ?", collectionId)
        .AND("collection_id != related_collection_id")
        .WITH(new DataConstraints().setDefaultColumnToSortBy("relationship_id"))
        .returnDataResult(CollectionRelationshipRepository::buildRecord).getRecords();
  }

  public static List<CollectionRelationship> findAllSelfByCollectionId(long collectionId) {
    if (collectionId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id = ?", collectionId)
        .AND("related_collection_id = ?", collectionId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("relationship_id"))
        .returnDataResult(CollectionRelationshipRepository::buildRecord).getRecords();
  }

  public static List<CollectionRelationship> findAllChildrenByCollectionId(long collectionId) {
    if (collectionId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id = ?", collectionId)
        .AND("collection_id != related_collection_id")
        .WITH(new DataConstraints().setDefaultColumnToSortBy("relationship_id"))
        .returnDataResult(CollectionRelationshipRepository::buildRecord).getRecords();
  }

  public static CollectionRelationship findById(long relationshipId) {
    if (relationshipId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("relationship_id = ?", relationshipId)
        .returnRecord(CollectionRelationshipRepository::buildRecord);
  }

  public static CollectionRelationship save(CollectionRelationship record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static CollectionRelationship add(CollectionRelationship record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("collection_id", record.getCollectionId())
        .FIELD("related_collection_id", record.getRelatedCollectionId())
        .FIELD("is_active", record.getIsActive())
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static CollectionRelationship update(CollectionRelationship record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("is_active", record.getIsActive())
        .SET("modified_by", record.getModifiedBy())
        .WHERE("relationship_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void remove(CollectionRelationship collectionRelationship) {
    DB.DELETE().FROM(TABLE_NAME).WHERE("relationship_id = ?", collectionRelationship.getId()).execute();
  }

  public static void remove(Connection connection, CollectionRelationship collectionRelationship) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("relationship_id = ?", collectionRelationship.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Collection collection) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", collection.getId()).execute(connection);
    DB.DELETE().FROM(TABLE_NAME).WHERE("related_collection_id = ?", collection.getId()).execute(connection);
  }

  private static CollectionRelationship buildRecord(ResultSet rs) {
    try {
      CollectionRelationship record = new CollectionRelationship();
      record.setId(rs.getLong("relationship_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setRelatedCollectionId(rs.getLong("related_collection_id"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setIsActive(rs.getBoolean("is_active"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
