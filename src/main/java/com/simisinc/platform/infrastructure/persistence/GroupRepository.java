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

package com.simisinc.platform.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.persistence.login.UserGroupRepository;

/**
 * Persists and retrieves group objects
 *
 * @author matt rajkowski
 * @created 4/24/18 8:40 AM
 */
public class GroupRepository {

  private static Log LOG = LogFactory.getLog(GroupRepository.class);

  private static String TABLE_NAME = "groups";
  private static String[] PRIMARY_KEY = new String[] { "group_id" };

  public static Group findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("groups.*")
        .FROM(TABLE_NAME)
        .WHERE("group_id = ?", id)
        .returnRecord(GroupRepository::buildRecord);
  }

  public static Group findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return DB.SELECT("groups.*")
        .FROM(TABLE_NAME)
        .WHERE("unique_id = ?", uniqueId)
        .returnRecord(GroupRepository::buildRecord);
  }

  public static Group findByOAuthPath(String oAuthPath) {
    if (StringUtils.isBlank(oAuthPath)) {
      return null;
    }
    return DB.SELECT("groups.*")
        .FROM(TABLE_NAME)
        .WHERE("oauth_path = ?", oAuthPath)
        .returnRecord(GroupRepository::buildRecord);
  }

  public static Group findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    return DB.SELECT("groups.*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(name) = ?", name.toLowerCase().trim())
        .returnRecord(GroupRepository::buildRecord);
  }

  public static List<Group> findAllByUserId(long userId) {
    if (userId == -1) {
      return null;
    }
    Select select = DB.SELECT("groups.*")
        .FROM(TABLE_NAME)
        .WHERE("EXISTS (SELECT 1 FROM user_groups WHERE group_id = groups.group_id AND user_id = ?)", userId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("group_id").setUseCount(false));
    return select.returnDataResult(GroupRepository::buildRecord).getRecords();
  }

  public static List<Group> findAll() {
    return DB.SELECT("groups.*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("name"))
        .returnDataResult(GroupRepository::buildRecord).getRecords();
  }

  public static Group save(Group record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static Group add(Group record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("description", StringUtils.trimToNull(record.getDescription()));
    if (StringUtils.isNotBlank(record.getOAuthPath())) {
      insert.FIELD("oauth_path", record.getOAuthPath());
    }
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static Group update(Group record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("description", StringUtils.trimToNull(record.getDescription()));
    if (StringUtils.isNotBlank(record.getOAuthPath())) {
      update.SET("oauth_path", record.getOAuthPath());
    } else {
      update.SET("oauth_path", (String) null);
    }
    update.WHERE("group_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Group record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      UserGroupRepository.remove(connection, record);
      DB.DELETE().FROM(TABLE_NAME).WHERE("group_id = ?", record.getId()).execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static PreparedStatement createPreparedStatementForUserCount(Connection connection, long groupId, int value)
      throws SQLException {
    String SQL_QUERY = "UPDATE groups " +
        "SET user_count = user_count + ? " +
        "WHERE group_id = ?";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setInt(++i, value);
    pst.setLong(++i, groupId);
    return pst;
  }

  public static boolean updateUserCount(long groupId, int value) {
    // Adjust the count
    try (Connection connection = DB.getConnection()) {
      return updateUserCount(connection, groupId, value);
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return false;

  }

  public static boolean updateUserCount(Connection connection, long groupId, int value) {
    // Adjust the count
    try (PreparedStatement pst = createPreparedStatementForUserCount(connection, groupId, value)) {
      return pst.execute();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return false;
  }

  private static PreparedStatement createPreparedStatementForRemoveUserCount(Connection connection, long userId) throws SQLException {
    String SQL_QUERY = "UPDATE groups " +
        "SET user_count = user_count - 1 " +
        "WHERE EXISTS (SELECT 1 FROM user_groups WHERE group_id = groups.group_id AND user_id = ?)";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setLong(++i, userId);
    return pst;
  }

  public static boolean removeUserCount(Connection connection, User user) {
    // Adjust the count
    try (PreparedStatement pst = createPreparedStatementForRemoveUserCount(connection, user.getId())) {
      return pst.execute();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return false;
  }

  private static Group buildRecord(ResultSet rs) {
    try {
      Group record = new Group();
      record.setId(rs.getLong("group_id"));
      record.setName(rs.getString("name"));
      record.setDescription(rs.getString("description"));
      record.setUserCount(rs.getLong("user_count"));
      record.setUniqueId(rs.getString("unique_id"));
      record.setOAuthPath(rs.getString("oauth_path"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
