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

package com.simisinc.platform.infrastructure.database.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.simisinc.platform.application.cms.MakeContentUniqueIdCommand;
import com.zeroio.platform.domain.model.tenant.Workspace;

/**
 * Updates existing workspace records to have a unique id
 *
 * @author matt rajkowski
 * @created 10/15/26 8:00 AM
 */
public class V20261015_1001__workspace_unique_id extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Connection connection = context.getConnection();
    List<Workspace> workspaceList = new ArrayList<>();

    {
      // Get a list of records to update
      try (Statement st = connection.createStatement()) {
        ResultSet rs = st.executeQuery("SELECT workspace_id, name FROM workspaces");
        while (rs.next()) {
          Workspace workspace = new Workspace();
          workspace.setId(rs.getLong("workspace_id"));
          workspace.setName(rs.getString("name"));
          workspaceList.add(workspace);
        }
        rs.close();
      }
    }

    // Update the unique id
    for (Workspace workspace : workspaceList) {
      // Create a uniqueId
      String name = workspace.getName();
      String value = MakeContentUniqueIdCommand.parseToValidValue(name);
      String uniqueId = generateUniqueId(connection, value);
      updateUniqueId(connection, workspace.getId(), uniqueId);
    }
  }

  private String generateUniqueId(Connection connection, String baseValue) throws Exception {
    String uniqueId = baseValue;
    try (PreparedStatement pst = connection.prepareStatement("SELECT * FROM workspaces WHERE workspace_unique_id = ?")) {
      int count = 1;
      while (!testUniqueId(pst, uniqueId)) {
        ++count;
        uniqueId = baseValue + "-" + count;
      }
    }
    return uniqueId;
  }

  private boolean testUniqueId(PreparedStatement pst, String uniqueId) throws Exception {
    boolean result = true;
    pst.setString(1, uniqueId);
    ResultSet rs = pst.executeQuery();
    if (rs.next()) {
      result = false;
    }
    rs.close();
    return result;
  }

  private void updateUniqueId(Connection connection, long workspaceId, String uniqueId) throws Exception {
    try (PreparedStatement pst = connection.prepareStatement("UPDATE workspaces SET workspace_unique_id = ? WHERE workspace_id = ?")) {
      int i = 0;
      pst.setString(++i, uniqueId);
      pst.setLong(++i, workspaceId);
      pst.execute();
    }
  }
}
