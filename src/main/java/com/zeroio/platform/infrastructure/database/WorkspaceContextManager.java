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
package com.zeroio.platform.infrastructure.database;

import com.github.rajkowski.database.DB;
import com.zeroio.platform.domain.model.tenant.WorkspaceContext;

public class WorkspaceContextManager {

  private static final ThreadLocal<WorkspaceContext> WORKSPACE_CONTEXT = new ThreadLocal<>();

  private WorkspaceContextManager() {
  }

  public static WorkspaceContext getCurrentContext() {
    return WORKSPACE_CONTEXT.get();
  }

  public static void activate(long workspaceId, String sourceHost) {
    activate(workspaceId, sourceHost, null);
  }

  public static void activate(long workspaceId, String sourceHost, String fileRoot) {
    if (workspaceId < 1) {
      throw new IllegalArgumentException("Workspace id must be greater than zero");
    }
    WORKSPACE_CONTEXT.set(new WorkspaceContext(workspaceId, sourceHost, fileRoot));
    DB.setTenant(String.valueOf(workspaceId));
  }

  public static void withWorkspace(long workspaceId, String sourceHost, Runnable runnable) {
    withWorkspace(workspaceId, sourceHost, null, runnable);
  }

  public static void withWorkspace(long workspaceId, String sourceHost, String fileRoot, Runnable runnable) {
    if (workspaceId < 1) {
      throw new IllegalArgumentException("Workspace id must be greater than zero");
    }
    WorkspaceContext previousContext = WORKSPACE_CONTEXT.get();
    try {
      WORKSPACE_CONTEXT.set(new WorkspaceContext(workspaceId, sourceHost, fileRoot));
      DB.withTenant(String.valueOf(workspaceId), runnable);
    } finally {
      if (previousContext == null) {
        WORKSPACE_CONTEXT.remove();
      } else {
        WORKSPACE_CONTEXT.set(previousContext);
      }
    }
  }

  public static void clear() {
    WORKSPACE_CONTEXT.remove();
    DB.clearTenant();
  }
}
