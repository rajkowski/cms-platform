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

import org.jobrunr.jobs.lambdas.JobRequest;

import com.github.rajkowski.database.DB;
import com.zeroio.platform.domain.model.tenant.WorkspaceContext;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

/**
 * Job request that captures and restores the tenant active at enqueue time.
 */
public abstract class TenantAwareJobRequest implements JobRequest {

  private long workspaceId = 0;
  private String sourceHost = null;
  private String fileRoot = null;
  private String tenantId = null;

  protected TenantAwareJobRequest() {
    WorkspaceContext workspaceContext = WorkspaceContextManager.getCurrentContext();
    if (workspaceContext != null) {
      workspaceId = workspaceContext.workspaceId();
      sourceHost = workspaceContext.sourceHost();
      fileRoot = workspaceContext.fileRoot();
    }
    tenantId = DB.getTenantId();
  }

  public void runInTenantContext(Runnable runnable) {
    try {
      if (workspaceId > 0) {
        WorkspaceContextManager.withWorkspace(workspaceId, sourceHost, fileRoot, runnable);
      } else if (tenantId != null && !tenantId.isEmpty()) {
        DB.withTenant(tenantId, runnable);
      } else {
        WorkspaceContextManager.withoutWorkspace(runnable);
      }
    } finally {
      WorkspaceContextManager.clear();
    }
  }

  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public String getSourceHost() {
    return sourceHost;
  }

  public void setSourceHost(String sourceHost) {
    this.sourceHost = sourceHost;
  }

  public String getFileRoot() {
    return fileRoot;
  }

  public void setFileRoot(String fileRoot) {
    this.fileRoot = fileRoot;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

}
