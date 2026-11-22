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
package com.zeroio.platform.infrastructure.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.simisinc.platform.domain.events.Event;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.infrastructure.scheduler.WorkflowEngineJob;
import com.simisinc.platform.infrastructure.scheduler.admin.ProcessDatasetJob;
import com.simisinc.platform.infrastructure.scheduler.ecommerce.OrderManagementEmailJob;
import com.simisinc.platform.infrastructure.scheduler.mailinglists.ProcessEmailSubscriptionJob;
import com.zeroio.platform.domain.model.tenant.WorkspaceContext;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;
import com.zeroio.platform.infrastructure.scheduler.cms.RefreshWebPageTextIndexJob;
import com.zeroio.platform.infrastructure.scheduler.staticsite.MakeStaticSiteJob;

class TenantAwareJobRequestTest {

  @AfterEach
  void clearTenantContext() {
    WorkspaceContextManager.clear();
    DB.setTenantRegistry(new TenantRegistry());
  }

  @Test
  void capturesWorkspaceForEveryEnqueuedJobRequestType() {
    TenantRegistry registry = new TenantRegistry();
    registry.register("42", mock(DataSource.class));
    DB.setTenantRegistry(registry);
    WorkspaceContextManager.activate(42, "tenant.example.com", "/var/cms/tenant-42");

    List<TenantAwareJobRequest> requests = List.of(
        new RefreshWebPageTextIndexJob("content-1"),
        new MakeStaticSiteJob(),
        new ProcessDatasetJob(new Dataset()),
        new ProcessEmailSubscriptionJob(null, null, "subscribed"),
        new OrderManagementEmailJob(),
        new WorkflowEngineJob(mock(Event.class)));

    for (TenantAwareJobRequest request : requests) {
      assertEquals(42, request.getWorkspaceId());
      assertEquals("tenant.example.com", request.getSourceHost());
      assertEquals("/var/cms/tenant-42", request.getFileRoot());
      assertEquals("42", request.getTenantId());
    }
  }

  @Test
  void restoresCapturedTenantAndClearsItAfterExecution() {
    TenantRegistry registry = new TenantRegistry();
    registry.register("42", mock(DataSource.class));
    DB.setTenantRegistry(registry);

    WorkspaceContextManager.activate(42, "tenant.example.com", "/var/cms/tenant-42");
    TenantAwareJobRequest request = new MakeStaticSiteJob();
    WorkspaceContextManager.clear();

    request.runInTenantContext(() -> {
      WorkspaceContext context = WorkspaceContextManager.getCurrentContext();
      assertEquals(42, context.workspaceId());
      assertEquals("42", DB.getTenantId());
    });

    assertNull(WorkspaceContextManager.getCurrentContext());
    assertNull(DB.getTenantId());
  }
}
