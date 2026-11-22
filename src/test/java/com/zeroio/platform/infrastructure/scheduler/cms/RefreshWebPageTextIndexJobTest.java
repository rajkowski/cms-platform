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
package com.zeroio.platform.infrastructure.scheduler.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.simisinc.platform.application.cms.RefreshWebPageTextCommand;
import com.zeroio.platform.domain.model.tenant.WorkspaceContext;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

class RefreshWebPageTextIndexJobTest {

  @AfterEach
  void clearTenantContext() {
    WorkspaceContextManager.clear();
    DB.setTenantRegistry(new TenantRegistry());
  }

  @Test
  void capturesAndRestoresOriginatingWorkspaceContext() {
    DataSource dataSource = mock(DataSource.class);
    TenantRegistry registry = new TenantRegistry();
    registry.register("42", dataSource);
    DB.setTenantRegistry(registry);

    RefreshWebPageTextIndexJob request = null;
    WorkspaceContextManager.activate(42, "tenant.example.com", "/var/cms/tenant-42");
    try {
      request = new RefreshWebPageTextIndexJob("content-1");
    } finally {
      WorkspaceContextManager.clear();
    }

    assertEquals(42, request.getWorkspaceId());
    assertEquals("tenant.example.com", request.getSourceHost());
    assertEquals("/var/cms/tenant-42", request.getFileRoot());

    try (MockedStatic<RefreshWebPageTextCommand> refreshCommand = mockStatic(RefreshWebPageTextCommand.class)) {
      refreshCommand.when(() -> RefreshWebPageTextCommand.refreshPagesByContentUniqueId(eq("content-1"),
          any(RefreshWebPageTextCommand.RebuildStats.class))).thenAnswer(invocation -> {
            WorkspaceContext context = WorkspaceContextManager.getCurrentContext();
            assertEquals(42, context.workspaceId());
            assertEquals("tenant.example.com", context.sourceHost());
            assertEquals("/var/cms/tenant-42", context.fileRoot());
            assertEquals("42", DB.getTenantId());
            return 0;
          });

      new RefreshWebPageTextIndexJob.RefreshWebPageTextIndexJobRequestHandler().run(request);
    }

    assertNull(WorkspaceContextManager.getCurrentContext());
    assertNull(DB.getTenantId());
  }

  @Test
  void targetsDefaultWorkspaceWhenCreatedWithoutWorkspaceContext() {
    RefreshWebPageTextIndexJob request = new RefreshWebPageTextIndexJob("default-content");

    assertEquals(0, request.getWorkspaceId());
    assertNull(request.getSourceHost());
    assertNull(request.getFileRoot());

    try (MockedStatic<RefreshWebPageTextCommand> refreshCommand = mockStatic(RefreshWebPageTextCommand.class)) {
      refreshCommand.when(() -> RefreshWebPageTextCommand.refreshPagesByContentUniqueId(eq("default-content"),
          any(RefreshWebPageTextCommand.RebuildStats.class))).thenAnswer(invocation -> {
            assertNull(WorkspaceContextManager.getCurrentContext());
            assertNull(DB.getTenantId());
            return 0;
          });

      new RefreshWebPageTextIndexJob.RefreshWebPageTextIndexJobRequestHandler().run(request);
    }

    assertNull(WorkspaceContextManager.getCurrentContext());
    assertNull(DB.getTenantId());
  }
}
