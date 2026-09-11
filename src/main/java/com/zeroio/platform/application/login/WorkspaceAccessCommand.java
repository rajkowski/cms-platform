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
package com.zeroio.platform.application.login;

import java.util.List;

import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.domain.model.tenant.WorkspaceAccessGrant;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceAccessGrantRepository;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceRepository;

public class WorkspaceAccessCommand {

  private WorkspaceAccessCommand() {
  }

  public static boolean hasAccess(long mainTenantUserId, Workspace workspace) {
    return mainTenantUserId > 0 && workspace != null && workspace.isActive() && WorkspaceAccessGrantRepository.hasActiveAccess(mainTenantUserId, workspace.getId());
  }

  public static boolean hasAccessToCanonicalDomain(long mainTenantUserId, Workspace workspace, String canonicalDomain) {
    return hasAccess(mainTenantUserId, workspace) && workspace.getCanonicalDomain() != null && workspace.getCanonicalDomain().equalsIgnoreCase(canonicalDomain);
  }

  public static List<Workspace> findAuthorizedWorkspaces(long mainTenantUserId) {
    return mainTenantUserId < 1 ? List.of() : WorkspaceRepository.findActiveWorkspacesByUserId(mainTenantUserId);
  }

  public static WorkspaceAccessGrant loadAccessGrant(long mainTenantUserId, Workspace workspace) {
    return WorkspaceAccessGrantRepository.queryAccessGrant(mainTenantUserId, workspace.getId());
  }
}
