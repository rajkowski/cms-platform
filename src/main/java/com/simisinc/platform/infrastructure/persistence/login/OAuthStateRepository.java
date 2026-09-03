/*
 * Copyright 2025-2026 Matt Rajkowski (https://github.com/rajkowski)
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

import com.github.rajkowski.database.DB;
import com.simisinc.platform.domain.model.login.OAuthState;

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
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("state = ?", state)
        .AND("created >= NOW() - INTERVAL '2 MINUTES'")
        .returnRecord(OAuthStateRepository::buildRecord);
  }

  public static OAuthState add(OAuthState record) {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("state", record.getState())
        .FIELD("resource", record.getResource())
        .FIELD("workspace_id", record.getWorkspaceId())
        .FIELD("destination_domain", record.getDestinationDomain())
        .execute();
    record.setId(generatedId);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static void deleteOldStateValues() {
    DB.DELETE().FROM(TABLE_NAME).WHERE("created < NOW() - INTERVAL '5 MINUTES'").execute();
  }

  private static OAuthState buildRecord(ResultSet rs) {
    try {
      OAuthState record = new OAuthState();
      record.setId(rs.getLong("state_id"));
      record.setState(rs.getString("state"));
      record.setResource(rs.getString("resource"));
      long workspaceId = rs.getLong("workspace_id");
      record.setWorkspaceId(rs.wasNull() ? null : workspaceId);
      record.setDestinationDomain(rs.getString("destination_domain"));
      record.setCreated(rs.getTimestamp("created"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
