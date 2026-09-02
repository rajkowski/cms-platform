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
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Insert;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.Member;

/**
 * Persists and retrieves member objects
 *
 * @author matt rajkowski
 * @created 8/24/18 8:36 AM
 */
public class MemberRepository {

  private static Log LOG = LogFactory.getLog(MemberRepository.class);

  private static String TABLE_NAME = "members";
  private static String[] PRIMARY_KEY = new String[] { "member_id" };

  public static Member findById(long memberId) {
    if (memberId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("member_id = ?", memberId)
        .returnRecord(MemberRepository::buildRecord);
  }

  public static List<Member> findAllForUserId(long userId) {
    if (userId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("user_id = ?", userId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("member_id"))
        .returnDataResult(MemberRepository::buildRecord).getRecords();
  }

  public static List<Member> findAllForItemId(long itemId) {
    if (itemId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", itemId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("member_id"))
        .returnDataResult(MemberRepository::buildRecord).getRecords();
  }

  public static boolean isApprovedMember(long itemId, long userId) {
    if (itemId == -1 || userId < 1) {
      return false;
    }
    return DB.SELECT("COUNT(*)")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", itemId)
        .AND("user_id = ?", userId)
        .AND("approved IS NOT NULL")
        .returnValue(Long.class) > 0;
  }

  public static Member save(Member record) {
    if (record.getId() > -1) {
      LOG.error("Member entity cannot be updated directly");
      return null;
    }
    return add(record);
  }

  public static Member add(Member record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("user_id", record.getUserId())
        .FIELD("item_id", record.getItemId())
        .FIELD("collection_id", record.getCollectionId())
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("requested", record.getRequested())
        .FIELD("archived", record.getApproved())
        .FIELD("last_viewed", record.getApproved());
    if (record.getApprovedBy() > -1) {
      insert.FIELD("archived_by", record.getApprovedBy());
      insert.FIELD("approved_by", record.getApprovedBy());
      if (record.getApproved() != null) {
        insert.FIELD("approved", record.getApproved());
      } else {
        insert.FIELD("approved", new Timestamp(System.currentTimeMillis()));
      }
    }
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      record.setId(insert.execute(connection));
      // Manage the roles
      MemberRoleRepository.insertMemberRoleList(connection, record);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  public static Member remove(Member record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Manage the roles
      MemberRoleRepository.removeAll(connection, record);
      // Remove the record
      remove(connection, record);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static void remove(Connection connection, Member member) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("member_id = ?", member.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Item item) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", item.getId()).execute(connection);
  }

  private static Member buildRecord(ResultSet rs) {
    try {
      Member record = new Member();
      record.setId(rs.getLong("member_id"));
      record.setUserId(rs.getLong("user_id"));
      record.setItemId(rs.getLong("item_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setRequested(rs.getTimestamp("requested"));
      record.setApprovedBy(rs.getLong("approved_by"));
      record.setApproved(rs.getTimestamp("approved"));
      record.setArchivedBy(rs.getLong("archived_by"));
      record.setArchived(rs.getTimestamp("archived"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
