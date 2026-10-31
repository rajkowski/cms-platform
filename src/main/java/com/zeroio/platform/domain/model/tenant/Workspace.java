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
 * Workspace metadata for the tenant-aware CMS model.
 *
 * Purpose of this change: store the tenant host, canonical domain, active flag,
 * and ownership details used when the
 * app resolves a request to the correct workspace database. The tenant routing
 * logic depends on this data to ensure a
 * user is redirected to and authenticated in the correct workspace instead of
 * silently falling back to the default CMS
 * site.
 */
public class Workspace extends Entity {

  private long id = -1;
  private String uniqueId = null;
  private String name;
  private String siteUrl;
  private String canonicalDomain;
  private String fileRoot;
  private boolean active;
  private boolean showInCatalog = true;
  private String description;
  private String department;
  private long ownerId = -1;
  private String ownerName;
  private String ownerEmail;
  private Timestamp created;
  private Timestamp modified;
  private long createdBy = -1;
  private long modifiedBy = -1;

  // helper
  private long poolDatabaseId = -1;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getSiteUrl() {
    return siteUrl;
  }

  public void setSiteUrl(String siteUrl) {
    this.siteUrl = siteUrl;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCanonicalDomain() {
    return canonicalDomain;
  }

  public void setCanonicalDomain(String canonicalDomain) {
    this.canonicalDomain = canonicalDomain;
  }

  public String getFileRoot() {
    return fileRoot;
  }

  public void setFileRoot(String fileRoot) {
    this.fileRoot = fileRoot;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isShowInCatalog() {
    return showInCatalog;
  }

  public void setShowInCatalog(boolean showInCatalog) {
    this.showInCatalog = showInCatalog;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public long getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(long ownerId) {
    this.ownerId = ownerId;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public void setOwnerEmail(String ownerEmail) {
    this.ownerEmail = ownerEmail;
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

  public long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(long createdBy) {
    this.createdBy = createdBy;
  }

  public long getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(long modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  public long getPoolDatabaseId() {
    return poolDatabaseId;
  }

  public void setPoolDatabaseId(long poolDatabaseId) {
    this.poolDatabaseId = poolDatabaseId;
  }
}
