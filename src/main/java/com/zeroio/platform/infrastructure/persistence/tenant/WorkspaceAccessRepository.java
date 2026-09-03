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
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.zeroio.platform.domain.model.tenant.Workspace;

public class WorkspaceAccessRepository {

  private static final Log LOG = LogFactory.getLog(WorkspaceAccessRepository.class);

  private WorkspaceAccessRepository() {}

  public static boolean hasActiveAccess(long userId, long workspaceId) {
    return DB.SELECT("workspace_id").FROM("workspace_access_grants").WHERE("user_id = ?", userId).AND("workspace_id = ?", workspaceId)
        .AND("active = ?", true).returnRecord(resultSet -> {
          try {
            return resultSet.getLong("workspace_id") == workspaceId;
          } catch (SQLException e) {
            LOG.error("Unable to read workspace access grant", e);
            return false;
          }
        }) != null;
  }

  public static List<Workspace> findActiveWorkspacesByUserId(long userId) {
    Select select = DB.SELECT("w.*").FROM("workspaces").AS("w").JOIN("workspace_access_grants g").ON("w.workspace_id = g.workspace_id")
        .WHERE("g.user_id = ?", userId).AND("g.active = ?", true).AND("w.active = ?", true).ORDER_BY("w.name");
    DataResult<Workspace> result = select.returnDataResult(WorkspaceAccessRepository::buildWorkspace);
    return result.getRecords();
  }

  private static Workspace buildWorkspace(ResultSet resultSet) {
    try {
      Workspace workspace = new Workspace();
      workspace.setId(resultSet.getLong("workspace_id"));
      workspace.setName(resultSet.getString("name"));
      workspace.setCanonicalDomain(resultSet.getString("canonical_domain"));
      workspace.setActive(resultSet.getBoolean("active"));
      return workspace;
    } catch (SQLException e) {
      LOG.error("Unable to build authorized workspace record", e);
      return null;
    }
  }
}
