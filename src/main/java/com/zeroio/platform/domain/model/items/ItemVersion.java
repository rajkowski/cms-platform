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
package com.zeroio.platform.domain.model.items;

import java.sql.Timestamp;

import com.simisinc.platform.domain.model.Entity;

/**
 * A historical snapshot of an item record saved before an update
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ItemVersion extends Entity {

  private Long id = -1L;
  private long itemId = -1L;
  private long collectionId = -1L;
  private String uniqueId = null;
  private String name = null;
  private long createdBy = -1L;
  private Timestamp created = null;
  private String versionData = null;

  public ItemVersion() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public long getItemId() {
    return itemId;
  }

  public void setItemId(long itemId) {
    this.itemId = itemId;
  }

  public long getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(long collectionId) {
    this.collectionId = collectionId;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(long createdBy) {
    this.createdBy = createdBy;
  }

  public Timestamp getCreated() {
    return created;
  }

  public void setCreated(Timestamp created) {
    this.created = created;
  }

  public String getVersionData() {
    return versionData;
  }

  public void setVersionData(String versionData) {
    this.versionData = versionData;
  }
}
