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
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemRelationship;

/**
 * Persists and retrieves item relationship objects
 *
 * @author matt rajkowski
 * @created 7/27/18 4:54 PM
 */
public class ItemRelationshipRepository {

  private static Log LOG = LogFactory.getLog(ItemRelationshipRepository.class);

  private static String TABLE_NAME = "item_relationships";
  private static String[] PRIMARY_KEY = new String[] { "relationship_id" };

  public static ItemRelationship findById(long relationshipId) {
    if (relationshipId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("relationship_id = ?", relationshipId)
        .returnRecord(ItemRelationshipRepository::buildRecord);
  }

  public static List<ItemRelationship> findRelatedItemsForItemId(long itemId) {
    if (itemId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", itemId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("relationship_id").setUseCount(false))
        .returnDataResult(ItemRelationshipRepository::buildRecord).getRecords();
  }

  public static List<ItemRelationship> findRelatedItemsForItemIdInCollection(Item item, Collection collection) {
    if (item == null || collection == null) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("((item_id = ? AND related_collection_id = ?) OR (related_item_id = ? AND collection_id = ?))",
            item.getId(), collection.getId(), item.getId(), collection.getId())
        .WITH(new DataConstraints().setDefaultColumnToSortBy("relationship_id"))
        .returnDataResult(ItemRelationshipRepository::buildRecord).getRecords();
  }

  public static boolean isAuthorizedForUser(Item item, Collection relatedCollection, long userId) {
    if (item == null || relatedCollection == null || userId < 1) {
      return false;
    }
    return DB.SELECT().COUNT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", item.getId())
        .AND("related_collection_id = ?", relatedCollection.getId())
        .AND(
            "EXISTS (SELECT 1 FROM members WHERE members.item_id = item_relationships.related_item_id AND members.user_id = ? AND members.approved IS NOT NULL AND archived IS NULL)",
            userId)
        .returnCount() > 0;
  }

  public static boolean isAuthorizedForUser(Item item, Collection relatedCollection, long userId,
      long collectionRoleId) {
    if (item == null || relatedCollection == null || userId < 1) {
      return false;
    }
    return DB.SELECT().COUNT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", item.getId())
        .AND("related_collection_id = ?", relatedCollection.getId())
        .AND(
            "EXISTS (SELECT 1 FROM members WHERE members.item_id = item_relationships.related_item_id AND members.user_id = ? AND members.approved IS NOT NULL AND archived IS NULL "
                + "AND EXISTS (SELECT 1 FROM member_roles WHERE members.member_id = member_roles.member_id AND role_id = ?))",
            userId, collectionRoleId)
        .returnCount() > 0;
  }

  public static ItemRelationship save(ItemRelationship record) {
    return save(record, true);
  }

  public static ItemRelationship save(ItemRelationship record, boolean saveReciprocalRelationships) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record, saveReciprocalRelationships);
  }

  public static ItemRelationship add(ItemRelationship record, boolean saveReciprocalRelationships) {
    // Save First Relationship record
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("item_id", record.getItemId())
        .FIELD("collection_id", record.getCollectionId())
        .FIELD("related_item_id", record.getRelatedItemId())
        .FIELD("related_collection_id", record.getRelatedCollectionId())
        .FIELD("is_active", record.getIsActive())
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("start_date", record.getStartDate())
        .FIELD("end_date", record.getEndDate());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }

    // Save Second Relationship record
    if (saveReciprocalRelationships) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("item_id", record.getRelatedItemId())
          .FIELD("collection_id", record.getRelatedCollectionId())
          .FIELD("related_item_id", record.getItemId())
          .FIELD("related_collection_id", record.getCollectionId())
          .FIELD("is_active", record.getIsActive())
          .FIELD("created_by", record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy())
          .FIELD("start_date", record.getStartDate())
          .FIELD("end_date", record.getEndDate())
          .execute();
    }

    return record;
  }

  private static ItemRelationship update(ItemRelationship record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("is_active", record.getIsActive())
        .SET("modified_by", record.getModifiedBy());
    if (update.WHERE("relationship_id = ?", record.getId()).execute()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void remove(ItemRelationship itemRelationship) {
    DB.DELETE().FROM(TABLE_NAME).WHERE("relationship_id = ?", itemRelationship.getId()).execute();
  }

  public static void remove(Connection connection, ItemRelationship itemRelationship) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("relationship_id = ?", itemRelationship.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Item item) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", item.getId()).execute(connection);
    DB.DELETE().FROM(TABLE_NAME).WHERE("related_item_id = ?", item.getId()).execute(connection);
  }

  public static void removeRelationship(Item item, Item relatedItem) {
    DB.DELETE().FROM(TABLE_NAME)
        .WHERE("((item_id = ? AND related_item_id = ?) OR (item_id = ? AND related_item_id = ?))",
            item.getId(), relatedItem.getId(), relatedItem.getId(), item.getId())
        .execute();
  }

  private static ItemRelationship buildRecord(ResultSet rs) {
    try {
      ItemRelationship record = new ItemRelationship();
      record.setId(rs.getLong("relationship_id"));
      record.setItemId(rs.getLong("item_id"));
      record.setRelatedItemId(rs.getLong("related_item_id"));
      record.setRelatedCollectionId(rs.getLong("related_collection_id"));
      record.setRelationshipTypeId(rs.getLong("relationship_type"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setIsActive(rs.getBoolean("is_active"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setStartDate(rs.getTimestamp("start_date"));
      record.setEndDate(rs.getTimestamp("end_date"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
