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

package com.simisinc.platform.infrastructure.persistence.mailinglists;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.mailinglists.Email;

/**
 * Persists and retrieves email objects
 *
 * @author matt rajkowski
 * @created 3/24/19 9:30 PM
 */
public class EmailRepository {

  private static Log LOG = LogFactory.getLog(EmailRepository.class);

  private static String TABLE_NAME = "emails";
  private static String[] PRIMARY_KEY = new String[] { "email_id" };

  private static DataResult<Email> query(EmailSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getMailingListId() > -1) {
        select.AND("EXISTS (SELECT 1 FROM mailing_list_members WHERE email_id = emails.email_id AND list_id = ?)",
            specification.getMailingListId());
      }
      if (StringUtils.isNotBlank(specification.getMatchesEmail())) {
        select.AND("LOWER(email) = LOWER(?)", specification.getMatchesEmail().trim());
      }
      if (StringUtils.isNotBlank(specification.getMatchesName())) {
        String likeValue = specification.getMatchesName().trim()
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
            .replace("[", "![");
        select.AND("LOWER(concat_ws(' ', first_name, last_name)) LIKE LOWER(?) ESCAPE '!'", "%" + likeValue + "%");
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(EmailRepository::buildRecord);
  }

  public static List<Email> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("email_id desc"))
        .returnDataResult(EmailRepository::buildRecord).getRecords();
  }

  public static Email findById(long emailId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("email_id = ?", emailId)
        .returnRecord(EmailRepository::buildRecord);
  }

  public static Email findByEmailAddress(String email) {
    if (StringUtils.isBlank(email)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(email) = ?", email.trim().toLowerCase())
        .returnRecord(EmailRepository::buildRecord);
  }

  public static List<Email> findAll(EmailSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("email_id desc");
    return query(specification, constraints).getRecords();
  }

  public static List<Email> findDailyUniqueLocations(int daysToLimit) {
    String SQL_QUERY = "SELECT DISTINCT continent, country, state, city, latitude, longitude " +
        "FROM emails " +
        "WHERE country IS NOT NULL " +
        "AND created > NOW() - INTERVAL '" + daysToLimit + " days' " +
        "AND latitude IS NOT NULL " +
        "ORDER BY continent, country, state, city, latitude, longitude";
    List<Email> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        Email data = new Email();
        data.setContinent(rs.getString("continent"));
        data.setCountry(rs.getString("country"));
        data.setState(rs.getString("state"));
        data.setCity(rs.getString("city"));
        data.setLatitude(rs.getDouble("latitude"));
        data.setLongitude(rs.getDouble("longitude"));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static Email add(Email record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("email", record.getEmail().trim().toLowerCase())
        .FIELD("first_name", record.getFirstName())
        .FIELD("last_name", record.getLastName())
        .FIELD("organization", record.getOrganization())
        .FIELD("source", record.getSource())
        .FIELD("ip_address", record.getIpAddress())
        .FIELD("session_id", record.getSessionId())
        .FIELD("user_agent", StringUtils.abbreviate(record.getUserAgent(), 500))
        .FIELD("referer", StringUtils.abbreviate(record.getReferer(), 250))
        .FIELD("continent", record.getContinent())
        .FIELD("country_iso", record.getCountryIso())
        .FIELD("country", record.getCountry())
        .FIELD("city", record.getCity())
        .FIELD("state_iso", record.getStateIso())
        .FIELD("state", record.getState())
        .FIELD("postal_code", record.getPostalCode())
        .FIELD("timezone", record.getTimezone())
        .FIELD("latitude", record.getLatitude())
        .FIELD("longitude", record.getLongitude())
        .FIELD("metro_code", record.getMetroCode() != -1 ? record.getMetroCode() : null)
        .FIELD("created_by", record.getCreatedBy() != -1 ? record.getCreatedBy() : null)
        .FIELD("modified_by", record.getModifiedBy() != -1 ? record.getModifiedBy() : null)
        .FIELD("last_emailed", record.getLastEmailed())
        .FIELD("subscribed", record.getSubscribed())
        .FIELD("unsubscribed", record.getUnsubscribed())
        .FIELD("unsubscribe_reason", record.getUnsubscribeReason())
        .FIELD("last_order", record.getLastOrder())
        .FIELD("number_of_orders", record.getNumberOfOrders())
        .FIELD("total_spent", record.getTotalSpent());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static Email update(Email record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified_by", record.getModifiedBy() != -1 ? record.getModifiedBy() : null)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("subscribed", record.getSubscribed())
        .SET("unsubscribed", record.getUnsubscribed());
    if (record.getFirstName() != null) {
      update.SET("first_name", record.getFirstName());
    }
    if (record.getLastName() != null) {
      update.SET("last_name", record.getLastName());
    }
    if (record.getOrganization() != null) {
      update.SET("organization", record.getOrganization());
    }
    update.WHERE("email = ?", record.getEmail().trim().toLowerCase());
    if (update.execute()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void markSynced(Email record) {
    if (record == null || record.getId() == -1) {
      return;
    }
    DB.UPDATE(TABLE_NAME)
        .SET("sync_date = CURRENT_TIMESTAMP")
        .WHERE("email_id = ?", record.getId())
        .execute();
  }

  public static void markNotSynced(Email record) {
    if (record == null || record.getId() == -1) {
      return;
    }
    DB.UPDATE(TABLE_NAME)
        .SET("sync_date", (Timestamp) null)
        .WHERE("email_id = ?", record.getId())
        .execute();
  }

  private static Email buildRecord(ResultSet rs) {
    try {
      Email record = new Email();
      record.setId(rs.getLong("email_id"));
      record.setEmail(rs.getString("email"));
      record.setFirstName(rs.getString("first_name"));
      record.setLastName(rs.getString("last_name"));
      record.setOrganization(rs.getString("organization"));
      record.setSource(rs.getString("source"));
      record.setIpAddress(rs.getString("ip_address"));
      record.setSessionId(rs.getString("session_id"));
      record.setUserAgent(rs.getString("user_agent"));
      record.setReferer(rs.getString("referer"));
      record.setContinent(rs.getString("continent"));
      record.setCountryIso(rs.getString("country_iso"));
      record.setCountry(rs.getString("country"));
      record.setCity(rs.getString("city"));
      record.setStateIso(rs.getString("state_iso"));
      record.setState(rs.getString("state"));
      record.setPostalCode(rs.getString("postal_code"));
      record.setTimezone(rs.getString("timezone"));
      record.setLatitude(rs.getDouble("latitude"));
      record.setLongitude(rs.getDouble("longitude"));
      record.setMetroCode(rs.getInt("metro_code"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(DB.getLong(rs, "modified_by", -1));
      record.setLastEmailed(rs.getTimestamp("last_emailed"));
      record.setSubscribed(rs.getTimestamp("subscribed"));
      record.setUnsubscribed(rs.getTimestamp("unsubscribed"));
      record.setUnsubscribeReason(rs.getString("unsubscribe_reason"));
      record.setLastOrder(rs.getTimestamp("last_order"));
      record.setNumberOfOrders(rs.getInt("number_of_orders"));
      record.setTotalSpent(rs.getBigDecimal("total_spent"));
      // @todo tags
      record.setSyncDate(rs.getTimestamp("sync_date"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
