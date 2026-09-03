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
import com.zeroio.platform.domain.model.tenant.Workspace;

public class WorkspaceRepository {

  private static final Log LOG = LogFactory.getLog(WorkspaceRepository.class);
  private static final String TABLE_NAME = "workspaces";

  private WorkspaceRepository() {}

  public static Workspace findById(long workspaceId) {
    if (workspaceId < 1) {
      return null;
    }
    return DB.SELECT("*").FROM(TABLE_NAME).WHERE("workspace_id = ?", workspaceId).returnRecord(WorkspaceRepository::buildRecord);
  }

  public static Workspace findActiveByCanonicalDomain(String canonicalDomain) {
    if (canonicalDomain == null || canonicalDomain.isBlank()) {
      return null;
    }
    return DB.SELECT("*").FROM(TABLE_NAME).WHERE("canonical_domain = ?", canonicalDomain).AND("active = ?", true)
        .returnRecord(WorkspaceRepository::buildRecord);
  }

  public static List<Workspace> findAllActive() {
    DataResult<Workspace> result = DB.SELECT("*").FROM(TABLE_NAME).WHERE("active = ?", true).ORDER_BY("workspace_id")
        .returnDataResult(WorkspaceRepository::buildRecord);
    return result.getRecords();
  }

  private static Workspace buildRecord(ResultSet resultSet) {
    try {
      Workspace workspace = new Workspace();
      workspace.setId(resultSet.getLong("workspace_id"));
      workspace.setName(resultSet.getString("name"));
      workspace.setCanonicalDomain(resultSet.getString("canonical_domain"));
      workspace.setFileRoot(resultSet.getString("file_root"));
      workspace.setActive(resultSet.getBoolean("active"));
      return workspace;
    } catch (SQLException e) {
      LOG.error("Unable to build workspace record", e);
      return null;
    }
  }
}
