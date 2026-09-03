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
import com.simisinc.platform.domain.model.items.CollectionRole;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.Member;
import com.simisinc.platform.domain.model.items.MemberRole;

/**
 * Properties for querying objects from the member role repository
 *
 * @author matt rajkowski
 * @created 8/24/18 9:53 AM
 */
public class MemberRoleRepository {

  private static Log LOG = LogFactory.getLog(MemberRoleRepository.class);

  private static String TABLE_NAME = "member_roles";
  private static String[] PRIMARY_KEY = new String[] { "member_role_id" };

  public static List<MemberRole> findAllByUserIdAndItemId(long userId, long itemId) {
    if (userId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("user_id = ?", userId)
        .WHERE("item_id = ?", itemId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("member_role_id"))
        .returnDataResult(MemberRoleRepository::buildRecord).getRecords();
  }

  public static List<MemberRole> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("member_role_id"))
        .returnDataResult(MemberRoleRepository::buildRecord).getRecords();
  }

  public static MemberRole add(MemberRole record) {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("member_id", record.getId())
        .FIELD("role_id", record.getItemRoleId())
        .FIELD("item_id", record.getItemId())
        .FIELD("user_id", record.getUserId())
        .FIELD("created_by", record.getCreatedBy())
        .execute();
    record.setId(generatedId);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static long insertMemberRoleList(Connection connection, Member member) throws SQLException {
    if (member.getRoleList() == null) {
      return 0;
    }
    long count = 0;
    for (CollectionRole collectionRole : member.getRoleList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("member_id", member.getId())
          .FIELD("role_id", collectionRole.getId())
          .FIELD("item_id", member.getItemId())
          .FIELD("user_id", member.getUserId())
          .FIELD("created_by", member.getCreatedBy())
          .execute(connection);
      ++count;
    }
    return count;
  }

  public static int removeAll(Connection connection, Member member) throws SQLException {
    // Delete the records
    return DB.DELETE().FROM(TABLE_NAME)
        .WHERE("member_id = ?", member.getId())
        .execute(connection).booleanValue() ? 1 : 0;
  }

  public static void removeAll(Connection connection, Item item) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", item.getId()).execute(connection);
  }

  private static MemberRole buildRecord(ResultSet rs) {
    try {
      MemberRole record = new MemberRole();
      record.setId(rs.getLong("member_role_id"));
      record.setMemberId(rs.getLong("member_id"));
      record.setItemRoleId(rs.getLong("role_id"));
      record.setItemId(rs.getLong("item_id"));
      record.setUserId(rs.getLong("user_id"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
