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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.simisinc.platform.application.json.JsonCommand;
import com.zeroio.platform.domain.model.tenant.WorkspaceAccessGrant;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

public class WorkspaceAccessGrantRepository {

  private static final Log LOG = LogFactory.getLog(WorkspaceAccessGrantRepository.class);

  private WorkspaceAccessGrantRepository() {
  }

  public static boolean hasActiveAccess(long mainTenantUserId, long workspaceId) {
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT("workspace_id")
            .FROM("workspace_access_grants")
            .WHERE("user_id = ?", mainTenantUserId).AND("workspace_id = ?", workspaceId)
            .AND("active = ?", true)
            .returnRecord(resultSet -> {
              try {
                return resultSet.getLong("workspace_id") == workspaceId;
              } catch (SQLException e) {
                LOG.error("Unable to read workspace access grant", e);
                return false;
              }
            }) != null);
  }

  public static WorkspaceAccessGrant queryAccessGrant(long mainTenantUserId, long workspaceId) {
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT("*")
            .FROM("workspace_access_grants")
            .WHERE("user_id = ?", mainTenantUserId)
            .AND("workspace_id = ?", workspaceId)
            .AND("active = ?", true)
            .returnRecord(WorkspaceAccessGrantRepository::buildRecord));
  }

  private static WorkspaceAccessGrant buildRecord(ResultSet resultSet) {
    try {
      WorkspaceAccessGrant record = new WorkspaceAccessGrant();
      record.setWorkspaceId(resultSet.getLong("workspace_id"));
      record.setMainTenantUserId(resultSet.getLong("user_id"));
      record.setActive(resultSet.getBoolean("active"));
      record.setRoles(JsonCommand.fromJsonArray(resultSet.getString("roles")));
      record.setGroups(JsonCommand.fromJsonArray(resultSet.getString("groups")));
      return record;
    } catch (SQLException e) {
      LOG.error("Unable to read workspace record", e);
      return null;
    }
  }
}
