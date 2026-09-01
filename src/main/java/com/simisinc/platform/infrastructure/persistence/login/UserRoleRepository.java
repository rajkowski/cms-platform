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
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.login.UserRole;

/**
 * Persists and retrieves user role objects
 *
 * @author matt rajkowski
 * @created 6/19/18 8:49 PM
 */
public class UserRoleRepository {

  private static Log LOG = LogFactory.getLog(UserRoleRepository.class);

  private static String TABLE_NAME = "user_roles";
  private static String[] PRIMARY_KEY = new String[] { "user_role_id" };

  public static List<UserRole> findAllByUserId(long userId) {
    if (userId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("user_id = ?", userId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("user_role_id").setUseCount(false))
        .returnDataResult(UserRoleRepository::buildRecord).getRecords();
  }

  public static List<UserRole> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("user_role_id"))
        .returnDataResult(UserRoleRepository::buildRecord).getRecords();
  }

  public static UserRole add(UserRole record) {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("user_id", record.getUserId())
        .FIELD("role_id", record.getRoleId())
        .execute();
    record.setId(generatedId);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void insertUserRoleList(Connection connection, User user) throws SQLException {
    if (user.getRoleList() == null) {
      return;
    }
    for (Role role : user.getRoleList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("user_id", user.getId())
          .FIELD("role_id", role.getId())
          .execute(connection);
    }
  }

  public static void removeAll(Connection connection, User user) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("user_id = ?", user.getId()).execute(connection);
  }

  private static UserRole buildRecord(ResultSet rs) {
    try {
      UserRole record = new UserRole();
      record.setId(rs.getLong("user_role_id"));
      record.setUserId(rs.getLong("user_id"));
      record.setRoleId(rs.getLong("role_id"));
      record.setCreated(rs.getTimestamp("created"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
