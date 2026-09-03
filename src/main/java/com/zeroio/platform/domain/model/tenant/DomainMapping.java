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

import com.simisinc.platform.domain.model.Entity;

public class DomainMapping extends Entity {

  private long id = -1;
  private long workspaceId = -1;
  private String hostPattern;
  private boolean wildcard;
  private boolean active;

  public long getId() { return id; }
  public void setId(long id) { this.id = id; }
  public long getWorkspaceId() { return workspaceId; }
  public void setWorkspaceId(long workspaceId) { this.workspaceId = workspaceId; }
  public String getHostPattern() { return hostPattern; }
  public void setHostPattern(String hostPattern) { this.hostPattern = hostPattern; }
  public boolean isWildcard() { return wildcard; }
  public void setWildcard(boolean wildcard) { this.wildcard = wildcard; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
}
