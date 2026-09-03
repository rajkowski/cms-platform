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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceRepository;

/**
 * Tenant-aware job runner that executes background work for all active workspaces.
 */
public class TenantAwareJobRunner {

  private static final Log LOG = LogFactory.getLog(TenantAwareJobRunner.class);

  private TenantAwareJobRunner() {
  }

  public static List<Outcome> runAllActive(WorkspaceWork workspaceWork) {
    return run(WorkspaceRepository.findAllActive(), workspaceWork);
  }

  public static List<Outcome> run(List<Workspace> workspaces, WorkspaceWork workspaceWork) {
    List<Outcome> outcomes = new ArrayList<>();
    for (Workspace workspace : workspaces) {
      if (workspace == null || !workspace.isActive()) {
        continue;
      }
      try {
        WorkspaceContextManager.withWorkspace(workspace.getId(), workspace.getCanonicalDomain(), () -> workspaceWork.execute(workspace));
        outcomes.add(new Outcome(workspace.getId(), true, null));
        LOG.debug("Completed background work for workspace " + workspace.getId());
      } catch (Exception e) {
        outcomes.add(new Outcome(workspace.getId(), false, e.getMessage()));
        LOG.error("Background work failed for workspace " + workspace.getId(), e);
      } finally {
        WorkspaceContextManager.clear();
      }
    }
    return outcomes;
  }

  @FunctionalInterface
  public interface WorkspaceWork {
    void execute(Workspace workspace);
  }

  public record Outcome(long workspaceId, boolean completed, String failureSummary) {
  }
}
