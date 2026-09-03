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

package com.simisinc.platform.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.dashboard.StatisticsData;
import com.simisinc.platform.infrastructure.persistence.ecommerce.OrderRepository;
import com.simisinc.platform.infrastructure.persistence.login.UserGroupRepository;
import com.simisinc.platform.infrastructure.persistence.login.UserLoginRepository;
import com.simisinc.platform.infrastructure.persistence.login.UserRoleRepository;
import com.simisinc.platform.infrastructure.persistence.login.UserTokenRepository;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves user objects
 *
 * @author matt rajkowski
 * @created 4/8/18 4:33 PM
 */
public class UserRepository {

  private static Log LOG = LogFactory.getLog(UserRepository.class);

  private static String TABLE_NAME = "users";
  private static String[] PRIMARY_KEY = new String[] { "user_id" };

  private static DataResult<User> query(UserSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("users.*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("user_id = ?", specification.getId());
      }
      if (specification.getRoleId() > -1) {
        select.AND("EXISTS (SELECT 1 FROM user_roles WHERE user_id = users.user_id AND role_id = ?)", specification.getRoleId());
      }
      if (specification.getGroupId() > -1) {
        select.AND("EXISTS (SELECT 1 FROM user_groups WHERE user_id = users.user_id AND group_id = ?)", specification.getGroupId());
      }
      if (specification.getIsEnabled() != DataConstants.UNDEFINED) {
        select.AND("enabled = ?", specification.getIsEnabled() == DataConstants.TRUE);
      }
      if (specification.getIsVerified() != DataConstants.UNDEFINED) {
        if (specification.getIsVerified() == DataConstants.TRUE) {
          select.AND("validated IS NOT NULL");
        } else {
          select.AND("validated IS NULL");
        }
      }
      if (specification.getMatchesName() != null) {
        if (specification.getMatchesName().contains("@")) {
          select.AND("LOWER(email) = LOWER(?)", specification.getMatchesName().trim());
        } else {
          String likeValue = specification.getMatchesName().trim()
              .replace("!", "!!")
              .replace("%", "!%")
              .replace("_", "!_")
              .replace("[", "![");
          select.AND("LOWER(concat_ws(' ', first_name, last_name, nickname)) LIKE LOWER(?) ESCAPE '!'", "%" + likeValue + "%");
        }
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(UserRepository::buildRecord);
  }

  public static User findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return DB.SELECT("users.*")
        .FROM(TABLE_NAME)
        .WHERE("unique_id = ?", uniqueId)
        .returnRecord(UserRepository::buildRecord);
  }

  public static User findByUsername(String username) {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    return DB.SELECT("users.*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(username) = ?", username.toLowerCase())
        .returnRecord(UserRepository::buildRecord);
  }

  public static User findByUserId(long userId) {
    if (userId == -1) {
      return null;
    }
    return DB.SELECT("users.*")
        .FROM(TABLE_NAME)
        .WHERE("user_id = ?", userId)
        .returnRecord(UserRepository::buildRecord);
  }

  public static User findByAccountToken(String token) {
    if (StringUtils.isBlank(token)) {
      return null;
    }
    return DB.SELECT("users.*")
        .FROM(TABLE_NAME)
        .WHERE("account_token = ?", token)
        .returnRecord(UserRepository::buildRecord);
  }

  public static User findByEmailAddress(String email) {
    if (StringUtils.isBlank(email)) {
      return null;
    }
    return DB.SELECT("users.*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(email) = ?", email.toLowerCase())
        .returnRecord(UserRepository::buildRecord);
  }

  public static List<User> findAllByRole(Role role) {
    UserSpecification specification = new UserSpecification();
    specification.setRoleId(role.getId());
    specification.setIsEnabled(true);
    return findAll(specification, null);
  }

  public static List<User> findAll(UserSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("user_id desc");
    DataResult result = query(specification, constraints);
    return (List<User>) result.getRecords();
  }

  public static List<StatisticsData> findMonthlyUserRegistrations(int monthsLimit) {
    String SQL_QUERY = "SELECT DATE_TRUNC('month', month)::VARCHAR(10) AS date_column, COUNT(user_id) AS monthly_count " +
        "FROM (SELECT generate_series(NOW() - INTERVAL '" + monthsLimit + " months', NOW(), INTERVAL '1 month')::date) d(month) " +
        "LEFT JOIN users ON DATE_TRUNC('month', created) = DATE_TRUNC('month', month) " +
        "GROUP BY d.month " +
        "ORDER BY d.month";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("date_column"));
        data.setValue(String.valueOf(rs.getLong("monthly_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findDailyUserRegistrations(int daysToLimit) {
    String SQL_QUERY = "SELECT DATE_TRUNC('day', day)::VARCHAR(10) AS date_column, COUNT(user_id) AS daily_count " +
        "FROM (SELECT generate_series(NOW() - INTERVAL '" + daysToLimit + " days', NOW(), INTERVAL '1 day')::date) d(day) " +
        "LEFT JOIN users ON DATE_TRUNC('day', created) = DATE_TRUNC('day', day) " +
        "GROUP BY d.day " +
        "ORDER BY d.day";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("date_column"));
        data.setValue(String.valueOf(rs.getLong("daily_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static long countTotalUsers() {
    long count = -1;
    String SQL_QUERY = "SELECT COUNT(user_id) AS user_count " +
        "FROM users ";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      if (rs.next()) {
        count = rs.getLong("user_count");
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return count;
  }

  /**
   * Count users created within a date range
   */
  public static long countNewUsers(Timestamp startDate, Timestamp endDate) {
    long count = 0;
    String sqlQuery = "SELECT COUNT(user_id) AS new_user_count " +
        "FROM users " +
        "WHERE created >= ? " +
        "AND created < ?";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sqlQuery)) {
      pst.setTimestamp(1, startDate);
      pst.setTimestamp(2, endDate);
      ResultSet rs = pst.executeQuery();
      if (rs.next()) {
        count = rs.getLong("new_user_count");
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return count;
  }

  public static User save(User record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static User add(User record) {
    record.setAccountToken(UUID.randomUUID().toString());
    if (record.getEmail() != null) {
      record.setEmail(record.getEmail().trim().toLowerCase());
    }
    if (record.getUsername() != null) {
      record.setUsername(record.getUsername().trim().toLowerCase());
    }
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("first_name", StringUtils.trimToNull(record.getFirstName()))
        .FIELD("last_name", StringUtils.trimToNull(record.getLastName()))
        .FIELD("organization", StringUtils.trimToNull(record.getOrganization()))
        .FIELD("nickname", StringUtils.trimToNull(record.getNickname()))
        .FIELD("email", StringUtils.trimToNull(record.getEmail()))
        .FIELD("username", StringUtils.trimToNull(record.getUsername()))
        .FIELD("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("department", StringUtils.trimToNull(record.getDepartment()))
        .FIELD("timezone", StringUtils.trimToNull(record.getTimeZone()))
        .FIELD("city", StringUtils.trimToNull(record.getCity()))
        .FIELD("state", StringUtils.trimToNull(record.getState()))
        .FIELD("country", StringUtils.trimToNull(record.getCountry()))
        .FIELD("postal_code", StringUtils.trimToNull(record.getPostalCode()))
        .FIELD("password", record.getPassword())
        .FIELD("enabled", true)
        .FIELD("account_token", record.getAccountToken())
        .FIELD("created_by", record.getCreatedBy() == -1 ? null : record.getCreatedBy());
    if (record.getCreated() != null) {
      insert.FIELD("created", record.getCreated());
    }
    if (record.hasGeoPoint()) {
      insert.FIELD("latitude", record.getLatitude())
          .FIELD("longitude", record.getLongitude())
          .FIELD("geom", record.getLatitude(), record.getLongitude(), CastType.GEOM);
    }
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      record.setId(insert.execute(connection));
      UserGroupRepository.insertUserGroupList(connection, record);
      UserRoleRepository.insertUserRoleList(connection, record);
      transaction.commit();
      LOG.info("Inserted userId: " + record.getId() + " - " + record.getEmail());
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  private static User update(User record) {
    if (record.getEmail() != null) {
      record.setEmail(record.getEmail().trim().toLowerCase());
    }
    if (record.getUsername() != null) {
      record.setUsername(record.getUsername().trim().toLowerCase());
    }
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("first_name", StringUtils.trimToNull(record.getFirstName()))
        .SET("last_name", StringUtils.trimToNull(record.getLastName()))
        .SET("organization", StringUtils.trimToNull(record.getOrganization()))
        .SET("nickname", StringUtils.trimToNull(record.getNickname()))
        .SET("email", StringUtils.trimToNull(record.getEmail()))
        .SET("username", StringUtils.trimToNull(record.getUsername()))
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("department", StringUtils.trimToNull(record.getDepartment()))
        .SET("timezone", StringUtils.trimToNull(record.getTimeZone()))
        .SET("city", StringUtils.trimToNull(record.getCity()))
        .SET("state", StringUtils.trimToNull(record.getState()))
        .SET("country", StringUtils.trimToNull(record.getCountry()))
        .SET("postal_code", StringUtils.trimToNull(record.getPostalCode()))
        .SET("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (record.hasGeoPoint()) {
      update.SET("latitude", record.getLatitude())
          .SET("longitude", record.getLongitude())
          .SET("geom", record.getLatitude(), record.getLongitude(), CastType.GEOM);
    } else {
      update.SET("latitude", (Double) null)
          .SET("longitude", (Double) null)
          .SET("geom", (String) null);
    }
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      update.WHERE("user_id = ?", record.getId());
      if (update.execute(connection) != null) {
        UserGroupRepository.removeAll(connection, record);
        UserGroupRepository.insertUserGroupList(connection, record);
        UserRoleRepository.removeAll(connection, record);
        UserRoleRepository.insertUserRoleList(connection, record);
        transaction.commit();
        return record;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static User updateValidated(User record) {
    Timestamp occurred = new Timestamp(System.currentTimeMillis());
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("validated", occurred)
        .SET("account_token", (String) null)
        .SET("modified", occurred)
        .WHERE("user_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      OrderRepository.updateUserOrders(record);
      return record;
    }
    LOG.error("updateValidated failed!");
    return null;
  }

  public static User updatePassword(User record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("password", record.getPassword())
        .SET("account_token", (String) null)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("user_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      UserTokenRepository.removeAll(record.getId());
      return record;
    }
    LOG.error("updatePassword failed!");
    return null;
  }

  public static User createAccountToken(User record) {
    String newToken = UUID.randomUUID().toString();
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("account_token", newToken)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("user_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      record.setAccountToken(newToken);
      return record;
    }
    LOG.error("createAccountToken failed!");
    return null;
  }

  public static User suspendAccount(User record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("enabled", false)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("user_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("suspendAccount failed!");
    return null;
  }

  public static User restoreAccount(User record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("enabled", true)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("user_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("restoreAccount failed!");
    return null;
  }

  // Remove
  public static boolean remove(User record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      UserGroupRepository.removeAll(connection, record);
      UserRoleRepository.removeAll(connection, record);
      UserTokenRepository.removeAll(connection, record);
      UserLoginRepository.removeAll(connection, record);
      DB.DELETE().FROM(TABLE_NAME).WHERE("user_id = ?", record.getId()).execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The delete failed!");
    return false;
  }

  private static User buildRecord(ResultSet rs) {
    try {
      User record = new User();
      record.setId(rs.getLong("user_id"));
      record.setUniqueId(rs.getString("unique_id"));
      record.setFirstName(rs.getString("first_name"));
      record.setLastName(rs.getString("last_name"));
      record.setOrganization(rs.getString("organization"));
      record.setNickname(rs.getString("nickname"));
      record.setEmail(rs.getString("email"));
      record.setUsername(rs.getString("username"));
      record.setPassword(rs.getString("password"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      record.setAccountToken(rs.getString("account_token"));
      record.setValidated(rs.getTimestamp("validated"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setTitle(rs.getString("title"));
      record.setDepartment(rs.getString("department"));
      record.setTimeZone(rs.getString("timezone"));
      record.setCity(rs.getString("city"));
      record.setState(rs.getString("state"));
      record.setCountry(rs.getString("country"));
      record.setPostalCode(rs.getString("postal_code"));
      record.setLatitude(rs.getDouble("latitude"));
      record.setLongitude(rs.getDouble("longitude"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
