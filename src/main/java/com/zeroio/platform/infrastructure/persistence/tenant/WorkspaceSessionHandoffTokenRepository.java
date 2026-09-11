/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
package com.zeroio.platform.infrastructure.persistence.tenant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.zeroio.platform.domain.model.tenant.WorkspaceSessionHandoffToken;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

/**
 * Persists and retrieves workspace session handoff tokens
 *
 * @author matt rajkowski
 * @created 9/8/26
 */
public class WorkspaceSessionHandoffTokenRepository {

  private static final Log LOG = LogFactory.getLog(WorkspaceSessionHandoffTokenRepository.class);

  private static final String TABLE_NAME = "workspace_session_handoff_tokens";

  private WorkspaceSessionHandoffTokenRepository() {
  }

  public static WorkspaceSessionHandoffToken add(WorkspaceSessionHandoffToken tokenRecord) {
    long generatedId = WorkspaceContextManager.withoutWorkspace(
        () -> DB.INSERT()
            .INTO(TABLE_NAME)
            .FIELD("token", tokenRecord.getToken())
            .FIELD("user_id", tokenRecord.getMainTenantUserId())
            .FIELD("workspace_id", tokenRecord.getWorkspaceId())
            .FIELD("resource", tokenRecord.getResource())
            .execute());
    tokenRecord.setId(generatedId);
    if (tokenRecord.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return tokenRecord;
  }

  public static WorkspaceSessionHandoffToken findValidByToken(String token) {
    if (StringUtils.isBlank(token)) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT("*")
            .FROM(TABLE_NAME)
            .WHERE("token = ?", token)
            .AND("consumed_at IS NULL")
            .AND("created >= NOW() - INTERVAL '60' SECOND")
            .returnRecord(WorkspaceSessionHandoffTokenRepository::buildRecord));
  }

  public static void markConsumed(long id) {
    WorkspaceContextManager.withoutWorkspace(
        () -> DB.UPDATE(TABLE_NAME)
            .SET("consumed_at", new Timestamp(System.currentTimeMillis()))
            .WHERE("workspace_session_handoff_token_id = ?", id)
            .execute());
  }

  private static WorkspaceSessionHandoffToken buildRecord(ResultSet rs) {
    try {
      WorkspaceSessionHandoffToken tokenRecord = new WorkspaceSessionHandoffToken();
      tokenRecord.setId(rs.getLong("workspace_session_handoff_token_id"));
      tokenRecord.setToken(rs.getString("token"));
      tokenRecord.setMainTenantUserId(rs.getLong("user_id"));
      tokenRecord.setWorkspaceId(rs.getLong("workspace_id"));
      tokenRecord.setResource(rs.getString("resource"));
      tokenRecord.setCreated(rs.getTimestamp("created"));
      tokenRecord.setConsumedAt(rs.getTimestamp("consumed_at"));
      return tokenRecord;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
