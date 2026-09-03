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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.CollectionRole;
import com.simisinc.platform.domain.model.items.Member;

/**
 * Persists and retrieves collection role objects
 *
 * @author matt rajkowski
 * @created 8/24/18 10:24 AM
 */
public class CollectionRoleRepository {

  private static Log LOG = LogFactory.getLog(CollectionRoleRepository.class);

  private static String TABLE_NAME = "lookup_collection_role";
  private static String[] PRIMARY_KEY = new String[] { "role_id" };

  public static CollectionRole findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("role_id = ?", id)
        .returnRecord(CollectionRoleRepository::buildRecord);
  }

  public static CollectionRole findByCode(String code) {
    if (StringUtils.isBlank(code)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("code = ?", code)
        .returnRecord(CollectionRoleRepository::buildRecord);
  }

  public static List<CollectionRole> findAllAvailableForCollectionId(long collectionId) {
    if (collectionId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id IS NULL OR collection_id = ?", collectionId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("level,role_id").setUseCount(false))
        .returnDataResult(CollectionRoleRepository::buildRecord).getRecords();
  }

  public static List<CollectionRole> findAllByMember(Member member) {
    if (member.getItemId() == -1 || member.getUserId() == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("EXISTS (SELECT 1 FROM member_roles WHERE role_id = lookup_collection_role.role_id AND item_id = ? AND user_id = ?)",
            member.getItemId(), member.getUserId())
        .WITH(new DataConstraints().setDefaultColumnToSortBy("role_id").setUseCount(false))
        .returnDataResult(CollectionRoleRepository::buildRecord).getRecords();
  }

  public static List<CollectionRole> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("title"))
        .returnDataResult(CollectionRoleRepository::buildRecord).getRecords();
  }

  public static CollectionRole save(CollectionRole record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static CollectionRole add(CollectionRole record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("code", StringUtils.trimToNull(record.getCode()));
    if (record.getCollectionId() > -1) {
      insert.FIELD("collection_id", record.getCollectionId());
    }
    insert.FIELD("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("archived", record.getArchived());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static CollectionRole update(CollectionRole record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("code", StringUtils.trimToNull(record.getCode()))
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("archived", record.getArchived())
        .WHERE("role_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void remove(CollectionRole record) {
    DB.DELETE().FROM(TABLE_NAME).WHERE("role_id = ?", record.getId()).execute();
  }

  public static void remove(Collection record) {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", record.getId()).execute();
  }

  public static void removeAll(Connection connection, Collection collection) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", collection.getId()).execute(connection);
  }

  private static CollectionRole buildRecord(ResultSet rs) {
    try {
      CollectionRole record = new CollectionRole();
      record.setId(rs.getLong("role_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setCode(rs.getString("code"));
      record.setTitle(rs.getString("title"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
