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

package com.simisinc.platform.infrastructure.persistence.mailinglists;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.mailinglists.MailingList;

/**
 * Persists and retrieves mailing list objects
 *
 * @author matt rajkowski
 * @created 3/24/19 9:46 PM
 */
public class MailingListRepository {

  private static Log LOG = LogFactory.getLog(MailingListRepository.class);

  private static String TABLE_NAME = "mailing_lists";
  private static String[] PRIMARY_KEY = new String[] { "list_id" };

  public static List<MailingList> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("list_order, name"))
        .returnDataResult(MailingListRepository::buildRecord).getRecords();
  }

  public static List<MailingList> findOnlineLists() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("show_online = true")
        .AND("enabled = true")
        .WITH(new DataConstraints().setDefaultColumnToSortBy("list_order, name"))
        .returnDataResult(MailingListRepository::buildRecord).getRecords();
  }

  public static List<MailingList> findOnlineListsForEmail(long emailId) {
    if (emailId <= 0) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("show_online = true")
        .AND("enabled = true")
        .AND("EXISTS (SELECT 1 FROM mailing_list_members WHERE list_id = mailing_lists.list_id AND email_id = ? AND is_valid = true)",
            emailId)
        .returnDataResult(MailingListRepository::buildRecord).getRecords();
  }

  public static MailingList findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("list_id = ?", id)
        .returnRecord(MailingListRepository::buildRecord);
  }

  public static MailingList findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(name) = ?", name.toLowerCase().trim())
        .returnRecord(MailingListRepository::buildRecord);
  }

  public static long countTotalMembers() {
    long count = -1;
    String SQL_QUERY = "SELECT SUM(member_count) AS member_count " +
        "FROM mailing_lists ";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      if (rs.next()) {
        count = rs.getLong("member_count");
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return count;
  }

  public static MailingList save(MailingList record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static MailingList add(MailingList record) {
    long id = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("list_order", record.getOrder())
        .FIELD("name", record.getName().trim())
        .FIELD("title", record.getTitle().trim())
        .FIELD_UNLESS_NULL("description", record.getDescription())
        .FIELD("member_count", record.getMemberCount())
        .FIELD("created_by", record.getCreatedBy() != -1 ? record.getCreatedBy() : null)
        .FIELD("modified_by", record.getModifiedBy() != -1 ? record.getModifiedBy() : null)
        .FIELD("last_emailed", record.getLastEmailed())
        .FIELD("show_online", record.getShowOnline())
        .FIELD("enabled", record.getEnabled())
        .execute();
    record.setId(id);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static MailingList update(MailingList record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("list_order", record.getOrder())
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("show_online", record.getShowOnline())
        .SET("enabled", record.getEnabled())
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (update.WHERE("list_id = ?", record.getId()).execute()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(MailingList record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      MailingListMemberRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("list_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static MailingList buildRecord(ResultSet rs) {
    try {
      MailingList record = new MailingList();
      record.setId(rs.getLong("list_id"));
      record.setOrder(rs.getInt("list_order"));
      record.setName(rs.getString("name"));
      record.setTitle(rs.getString("title"));
      record.setDescription(rs.getString("description"));
      record.setMemberCount(rs.getInt("member_count"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setLastEmailed(rs.getTimestamp("last_emailed"));
      record.setShowOnline(rs.getBoolean("show_online"));
      record.setEnabled(rs.getBoolean("enabled"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
