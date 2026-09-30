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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.mailinglists.Email;
import com.simisinc.platform.domain.model.mailinglists.MailingList;

/**
 * Persists and retrieves mailing list member objects
 *
 * @author matt rajkowski
 * @created 3/25/19 9:10 PM
 */
public class MailingListMemberRepository {

  private static Log LOG = LogFactory.getLog(MailingListMemberRepository.class);

  private static String TABLE_NAME = "mailing_list_members";
  private static String JOIN = "LEFT JOIN emails ON (mailing_list_members.email_id = emails.email_id) " +
      "LEFT JOIN mailing_lists ON (mailing_list_members.list_id = mailing_lists.list_id)";
  private static String[] PRIMARY_KEY = new String[] { "member_id" };

  public static void addEmailToList(Email email, MailingList mailingList) {
    long memberId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("list_id", mailingList.getId())
        .FIELD("email_id", email.getId())
        .FIELD_UNLESS_MATCHES("created_by", email.getCreatedBy(), -1)
        .FIELD_UNLESS_MATCHES("modified_by", email.getModifiedBy(), -1)
        .execute();
    if (memberId > -1) {
      DB.UPDATE("mailing_lists")
          .SET("member_count = member_count + 1")
          .WHERE("list_id = ?", mailingList.getId())
          .execute();
    } else {
      DB.UPDATE(TABLE_NAME)
          .SET("unsubscribed", (Timestamp) null)
          .SET("modified", new Timestamp(System.currentTimeMillis()))
          .SET_UNLESS_MATCHES("modified_by", email.getModifiedBy(), -1)
          .SET("is_valid", true)
          .WHERE("list_id = ?", mailingList.getId())
          .AND("email_id = ?", email.getId())
          .execute();
    }
  }

  public static void remove(Email email, MailingList mailingList) {
    boolean removed = DB.DELETE().FROM(TABLE_NAME)
        .WHERE("email_id = ?", email.getId())
        .AND("list_id = ?", mailingList.getId())
        .execute();
    if (removed) {
      DB.UPDATE("mailing_lists")
          .SET("member_count = member_count - 1")
          .WHERE("list_id = ?", mailingList.getId())
          .execute();
    }
  }

  public static void removeAll(Connection connection, MailingList mailingList) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("list_id = ?", mailingList.getId()).execute(connection);
  }

  public static void unsubscribe(MailingList mailingList, Email email, User user) {
    DB.UPDATE(TABLE_NAME)
        .SET("unsubscribed", new Timestamp(System.currentTimeMillis()))
        .SET("unsubscribed_by", user.getId())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("modified_by", user.getId())
        .SET("is_valid", false)
        .WHERE("list_id = ?", mailingList.getId())
        .AND("email_id = ?", email.getId())
        .execute();
  }

  public static void export(MailingListMemberSpecification specification, DataConstraints constraints, File file) {
    Select select = DB.SELECT(
        "mailing_lists.name AS list",
        "email",
        "first_name",
        "last_name",
        "organization",
        "mailing_list_members.created AS subscribed",
        "mailing_list_members.unsubscribed AS unsubscribed",
        "emails.unsubscribed AS ref_unsubscribed",
        "is_valid")
        .FROM(TABLE_NAME)
        .LEFT_JOIN("emails").ON("mailing_list_members.email_id = emails.email_id")
        .LEFT_JOIN("mailing_lists").ON("mailing_list_members.list_id = mailing_lists.list_id");
    if (specification != null && specification.getMailingListId() > -1) {
      select.WHERE("mailing_list_members.list_id = ?", specification.getMailingListId());
    }
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("mailing_list_members.created");
    select.WITH(constraints);
    DB.writeCsv(select, file);
  }
}
