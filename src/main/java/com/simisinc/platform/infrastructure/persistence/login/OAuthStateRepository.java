/*
 * Copyright 2025 Matt Rajkowski (https://github.com/rajkowski)
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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.login.OAuthState;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.SqlUtils;

/**
 * Persists and retrieves oauth state objects
 *
 * @author matt rajkowski
 * @created 4/3/2025 9:09 AM
 */
public class OAuthStateRepository {

  private static Log LOG = LogFactory.getLog(OAuthStateRepository.class);

  private static String TABLE_NAME = "oauth_state_values";
  private static String[] PRIMARY_KEY = new String[] { "state_id" };

  public static OAuthState findByStateIfValid(String state) {
    if (StringUtils.isBlank(state)) {
      return null;
    }
    return (OAuthState) DB.selectRecordFrom(
        TABLE_NAME,
        DB.WHERE("state = ?", state)
            .AND("created >= NOW() - INTERVAL '2 MINUTES'"),
        OAuthStateRepository::buildRecord);
  }

  public static OAuthState add(OAuthState record) {
    SqlUtils insertValues = new SqlUtils()
        .add("state", record.getState())
        .add("resource", record.getResource());
    record.setId(DB.insertInto(TABLE_NAME, insertValues, PRIMARY_KEY));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void deleteOldStateValues() {
    DB.deleteFrom(TABLE_NAME, DB.WHERE("created < NOW() - INTERVAL '5 MINUTES'"));
  }

  private static OAuthState buildRecord(ResultSet rs) {
    try {
      OAuthState record = new OAuthState();
      record.setId(rs.getLong("state_id"));
      record.setState(rs.getString("state"));
      record.setResource(rs.getString("resource"));
      record.setCreated(rs.getTimestamp("created"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
