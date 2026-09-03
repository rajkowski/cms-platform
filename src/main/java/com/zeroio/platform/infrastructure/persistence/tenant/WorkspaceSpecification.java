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
package com.zeroio.platform.infrastructure.persistence.tenant;

public class WorkspaceSpecification {

  private long id = -1;
  private String canonicalDomain;
  private boolean activeOnly = true;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getCanonicalDomain() {
    return canonicalDomain;
  }

  public void setCanonicalDomain(String canonicalDomain) {
    this.canonicalDomain = canonicalDomain;
  }

  public boolean isActiveOnly() {
    return activeOnly;
  }

  public void setActiveOnly(boolean activeOnly) {
    this.activeOnly = activeOnly;
  }
}
