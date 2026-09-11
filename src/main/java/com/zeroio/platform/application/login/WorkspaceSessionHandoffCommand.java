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

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zeroio.platform.domain.model.tenant.WorkspaceSessionHandoffToken;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceSessionHandoffTokenRepository;

/**
 * Issues and consumes short-lived, single-use tokens that carry an already-authenticated user's
 * identity across a tenant workspace domain boundary, without a new identity-provider round trip.
 *
 * @author matt rajkowski
 * @created 9/8/26
 */
public class WorkspaceSessionHandoffCommand {

  private static final Log LOG = LogFactory.getLog(WorkspaceSessionHandoffCommand.class);

  private WorkspaceSessionHandoffCommand() {
  }

  public static String issueToken(long mainTenantUserId, long workspaceId, String resource) {
    if (mainTenantUserId < 1 || workspaceId < 1) {
      LOG.warn("A mainTenantUserId and workspaceId are required to issue a handoff token");
      return null;
    }
    LOG.debug("Issuing handoff token for mainTenantUserId: " + mainTenantUserId + ", workspaceId: " + workspaceId + ", resource: " + resource);
    WorkspaceSessionHandoffToken handoffToken = new WorkspaceSessionHandoffToken();
    handoffToken.setToken(UUID.randomUUID().toString());
    handoffToken.setMainTenantUserId(mainTenantUserId);
    handoffToken.setWorkspaceId(workspaceId);
    handoffToken.setResource(StringUtils.isBlank(resource) ? "/" : resource);
    WorkspaceSessionHandoffToken saved = WorkspaceSessionHandoffTokenRepository.add(handoffToken);
    return saved != null ? saved.getToken() : null;
  }

  /**
   * Validates and permanently consumes the token (single use), regardless of outcome.
   */
  public static WorkspaceSessionHandoffToken consumeToken(String token) {
    // Must use the default tenant
    LOG.debug("Consuming handoff token: " + token);
    WorkspaceSessionHandoffToken handoffToken = WorkspaceSessionHandoffTokenRepository.findValidByToken(token);
    if (handoffToken == null) {
      return null;
    }
    WorkspaceSessionHandoffTokenRepository.markConsumed(handoffToken.getId());
    return handoffToken;
  }
}
