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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.zeroio.platform.application.cms.WorkspaceResolutionCommand;
import com.zeroio.platform.application.login.WorkspaceAccessCommand;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

class WorkspaceTenantRepositoryTest {

  private DataSource dataSource;

  @BeforeEach
  void setUp() throws Exception {
    JdbcDataSource source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:tenant_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    source.setUser("sa");
    dataSource = source;
    DB.setDataSource(dataSource);
    DB.setTenantRegistry(new TenantRegistry());
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute(
          "CREATE TABLE workspaces (workspace_id BIGINT PRIMARY KEY, name VARCHAR(255), canonical_domain VARCHAR(255), file_root VARCHAR(1024), active BOOLEAN)");
      statement.execute(
          "CREATE TABLE workspace_domains (workspace_domain_id BIGINT PRIMARY KEY, workspace_id BIGINT, host_pattern VARCHAR(255), wildcard BOOLEAN, active BOOLEAN)");
      statement.execute("CREATE TABLE workspace_access_grants (workspace_id BIGINT, user_id BIGINT, active BOOLEAN)");
      statement.execute(
          "INSERT INTO workspaces VALUES (1, 'Exact', 'exact.example.com', '/var/cms/exact', TRUE), (2, 'Wildcard', 'wild.example.com', '/var/cms/wildcard', TRUE), (3, 'Inactive', 'inactive.example.com', '/var/cms/inactive', FALSE)");
      statement.execute(
          "INSERT INTO workspace_domains VALUES (1, 1, 'exact.example.com', FALSE, TRUE), (2, 2, '*.example.com', TRUE, TRUE), (3, 3, 'inactive.example.com', FALSE, TRUE)");
      statement.execute("INSERT INTO workspace_access_grants VALUES (1, 10, TRUE), (2, 10, TRUE)");
    }
  }

  @AfterEach
  void tearDown() {
    WorkspaceContextManager.clear();
    DB.setTenantRegistry(new TenantRegistry());
  }

  @Test
  void exactMappingWinsOverWildcardAndInactiveWorkspaceDoesNotResolve() {
    assertEquals(1, WorkspaceResolutionCommand.resolveWorkspace("exact.example.com").getId());
    assertEquals(2, WorkspaceResolutionCommand.resolveWorkspace("child.example.com").getId());
    assertNull(WorkspaceResolutionCommand.resolveWorkspace("inactive.example.com"));
  }

  @Test
  void accessLookupFiltersByActiveGrant() {
    Workspace workspace = WorkspaceRepository.findById(1);
    assertEquals(2, WorkspaceAccessCommand.findAuthorizedWorkspaces(10).size());
    assertFalse(WorkspaceAccessCommand.hasAccess(11, workspace));
  }

  @Test
  void workspaceRecordIncludesConfiguredFileRoot() {
    Workspace workspace = WorkspaceRepository.findById(1);

    assertEquals("/var/cms/exact", workspace.getFileRoot());
  }

  @Test
  void workspaceScopeActivatesTenantDatasourceAndCleansUp() {
    DB.getTenantRegistry().register("1", dataSource);
    WorkspaceContextManager.withWorkspace(1, "exact.example.com", "/var/cms/exact", () -> {
      assertEquals(1, WorkspaceContextManager.getCurrentContext().workspaceId());
      assertEquals("/var/cms/exact", WorkspaceContextManager.getCurrentContext().fileRoot());
      assertSame(dataSource, DB.getDataSource());
    });
    assertNull(WorkspaceContextManager.getCurrentContext());
    assertNull(DB.getTenantId());
  }
}
