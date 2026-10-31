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
 * A database in a tenant datasource pool.
 */
public class TenantDataSourceDatabase extends Entity {

  private long id = -1;
  private long poolId = -1;
  private String databaseName;
  private boolean active;
  private boolean available = true;
  private Timestamp created;
  private Timestamp modified;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getPoolId() {
    return poolId;
  }

  public void setPoolId(long poolId) {
    this.poolId = poolId;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  public Timestamp getCreated() {
    return created;
  }

  public void setCreated(Timestamp created) {
    this.created = created;
  }

  public Timestamp getModified() {
    return modified;
  }

  public void setModified(Timestamp modified) {
    this.modified = modified;
  }
}