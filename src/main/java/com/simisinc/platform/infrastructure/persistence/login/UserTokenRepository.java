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
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.login.UserToken;
import com.simisinc.platform.infrastructure.persistence.oauth.OAuthTokenRepository;

/**
 * Persists and retrieves user token objects
 *
 * @author matt rajkowski
 * @created 4/10/18 5:23 PM
 */
public class UserTokenRepository {

  private static Log LOG = LogFactory.getLog(UserTokenRepository.class);

  private static String TABLE_NAME = "user_tokens";
  private static String[] PRIMARY_KEY = new String[] { "token_id" };

  public static UserToken findByToken(String token) {
    if (StringUtils.isBlank(token)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("token = ?", token)
        .returnRecord(UserTokenRepository::buildRecord);
  }

  public static List<UserToken> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("token_id").setUseCount(false))
        .returnDataResult(UserTokenRepository::buildRecord).getRecords();
  }

  public static UserToken add(UserToken record) {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("user_id", record.getUserId())
        .FIELD("login_id", record.getLoginId())
        .FIELD("token", record.getToken())
        .FIELD("expires", record.getExpires())
        .execute();
    record.setId(generatedId);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void remove(UserToken userToken) {
    if (userToken == null || StringUtils.isBlank(userToken.getToken())) {
      return;
    }
    OAuthTokenRepository.remove(userToken);
    DB.DELETE().FROM(TABLE_NAME).WHERE("token_id = ?", userToken.getId()).execute();
  }

  public static void removeAll(long userId) {
    if (userId < 0) {
      return;
    }
    OAuthTokenRepository.removeAll(userId);
    DB.DELETE().FROM(TABLE_NAME).WHERE("user_id = ?", userId).execute();
  }

  public static int removeAll(Connection connection, User user) throws SQLException {
    OAuthTokenRepository.removeAll(connection, user);
    return DB.DELETE().FROM(TABLE_NAME).WHERE("user_id = ?", user.getId()).execute(connection) ? 1 : 0;
  }

  public static void extendTokenExpiration(String token, int seconds) {
    DB.UPDATE(TABLE_NAME)
        .SET("expires", new Timestamp(System.currentTimeMillis() + (seconds * 1000L)))
        .WHERE("token = ?", token)
        .execute();
  }

  public static void deleteOldTokens() {
    OAuthTokenRepository.deleteOldTokens();
    DB.DELETE().FROM(TABLE_NAME).WHERE("expires < NOW() - INTERVAL '1 day'").execute();
  }

  private static UserToken buildRecord(ResultSet rs) {
    try {
      UserToken record = new UserToken();
      record.setId(rs.getLong("token_id"));
      record.setUserId(rs.getLong("user_id"));
      record.setLoginId(rs.getLong("login_id"));
      record.setToken(rs.getString("token"));
      record.setExpires(rs.getTimestamp("expires"));
      record.setCreated(rs.getTimestamp("created"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
