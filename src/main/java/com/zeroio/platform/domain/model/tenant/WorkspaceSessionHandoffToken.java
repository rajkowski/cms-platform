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
package com.zeroio.platform.domain.model.tenant;

import java.sql.Timestamp;

import com.simisinc.platform.domain.model.Entity;

/**
 * A short-lived, single-use token that hands an already-authenticated user's session off to a
 * specific tenant workspace domain, without a new identity-provider round trip.
 *
 * @author matt rajkowski
 * @created 9/8/26
 */
public class WorkspaceSessionHandoffToken extends Entity {

  private long id = -1L;
  private String token;
  private long mainTenantUserId;
  private long workspaceId;
  private String resource;
  private Timestamp created;
  private Timestamp consumedAt;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public long getMainTenantUserId() {
    return mainTenantUserId;
  }

  public void setMainTenantUserId(long mainTenantUserId) {
    this.mainTenantUserId = mainTenantUserId;
  }

  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public Timestamp getCreated() {
    return created;
  }

  public void setCreated(Timestamp created) {
    this.created = created;
  }

  public Timestamp getConsumedAt() {
    return consumedAt;
  }

  public void setConsumedAt(Timestamp consumedAt) {
    this.consumedAt = consumedAt;
  }
}
