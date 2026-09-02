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

package com.simisinc.platform.infrastructure.persistence.login;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.login.UserGroup;
import com.simisinc.platform.infrastructure.persistence.GroupRepository;

/**
 * Persists and retrieves user group objects
 *
 * @author matt rajkowski
 * @created 6/19/18 8:36 PM
 */
public class UserGroupRepository {

  private static Log LOG = LogFactory.getLog(UserGroupRepository.class);

  private static String TABLE_NAME = "user_groups";
  private static String[] PRIMARY_KEY = new String[] { "user_group_id" };

  public static List<UserGroup> findAllByUserId(long userId) {
    if (userId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("user_id = ?", userId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("user_group_id").setUseCount(false))
        .returnDataResult(UserGroupRepository::buildRecord).getRecords();
  }

  public static List<UserGroup> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("user_group_id"))
        .returnDataResult(UserGroupRepository::buildRecord).getRecords();
  }

  public static UserGroup add(UserGroup record) {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("user_id", record.getUserId())
        .FIELD("group_id", record.getGroupId())
        .execute();
    record.setId(generatedId);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    // Update the group count
    GroupRepository.updateUserCount(record.getGroupId(), 1);
    return record;
  }

  public static long insertUserGroupList(Connection connection, User user) throws SQLException {
    if (user.getGroupList() == null) {
      return 0;
    }
    long count = 0;
    for (Group group : user.getGroupList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("user_id", user.getId())
          .FIELD("group_id", group.getId())
          .execute(connection);
      GroupRepository.updateUserCount(connection, group.getId(), 1);
      ++count;
    }
    return count;
  }

  public static int removeAll(Connection connection, User user) throws SQLException {
    GroupRepository.removeUserCount(connection, user);
    return DB.DELETE().FROM(TABLE_NAME).WHERE("user_id = ?", user.getId()).execute(connection).booleanValue() ? 1 : 0;
  }

  public static void remove(Connection connection, Group group) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("group_id = ?", group.getId()).execute(connection);
  }

  private static UserGroup buildRecord(ResultSet rs) {
    try {
      UserGroup record = new UserGroup();
      record.setId(rs.getLong("user_group_id"));
      record.setUserId(rs.getLong("user_id"));
      record.setGroupId(rs.getLong("group_id"));
      record.setCreated(rs.getTimestamp("created"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
