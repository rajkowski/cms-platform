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
package com.simisinc.platform.infrastructure.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

class TenantAwareJobRunnerTest {

  @AfterEach
  void clearTenantContext() {
    WorkspaceContextManager.clear();
    DB.setTenantRegistry(new TenantRegistry());
  }

  @Test
  void continuesAfterFailureAndClearsContextForEachWorkspace() {
    DataSource dataSource = org.mockito.Mockito.mock(DataSource.class);
    TenantRegistry registry = new TenantRegistry();
    registry.register("1", dataSource);
    registry.register("2", dataSource);
    DB.setTenantRegistry(registry);
    List<TenantAwareJobRunner.Outcome> outcomes = TenantAwareJobRunner.run(List.of(workspace(1), workspace(2)), workspace -> {
      if (workspace.getId() == 1) {
        throw new IllegalStateException("expected failure");
      }
      assertEquals(2, WorkspaceContextManager.getCurrentContext().workspaceId());
    });

    assertEquals(2, outcomes.size());
    assertFalse(outcomes.get(0).completed());
    assertEquals(2, outcomes.get(1).workspaceId());
    assertNull(WorkspaceContextManager.getCurrentContext());
    assertNull(DB.getTenantId());
  }

  private static Workspace workspace(long id) {
    Workspace workspace = new Workspace();
    workspace.setId(id);
    workspace.setActive(true);
    return workspace;
  }
}
