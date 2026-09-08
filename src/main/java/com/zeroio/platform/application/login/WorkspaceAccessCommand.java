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
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceAccessRepository;

public class WorkspaceAccessCommand {

  private WorkspaceAccessCommand() {
  }

  public static boolean hasAccess(long userId, Workspace workspace) {
    return userId > 0 && workspace != null && workspace.isActive() && WorkspaceAccessRepository.hasActiveAccess(userId, workspace.getId());
  }

  public static boolean hasAccessToCanonicalDomain(long userId, Workspace workspace, String canonicalDomain) {
    return hasAccess(userId, workspace) && workspace.getCanonicalDomain() != null && workspace.getCanonicalDomain().equalsIgnoreCase(canonicalDomain);
  }

  public static List<Workspace> findAuthorizedWorkspaces(long userId) {
    return userId < 1 ? List.of() : WorkspaceAccessRepository.findActiveWorkspacesByUserId(userId);
  }
}
