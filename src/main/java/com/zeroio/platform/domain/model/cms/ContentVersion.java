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
package com.zeroio.platform.domain.model.cms;

import java.sql.Timestamp;

import com.simisinc.platform.domain.model.Entity;

/**
 * Represents a version of content
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ContentVersion extends Entity {

  private long id = -1L;
  private long contentId = -1L;
  private int versionNumber = -1;
  private String content = null;
  private long createdBy = -1L;
  private Timestamp created = null;
  private String notes = null;

  public ContentVersion() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getContentId() {
    return contentId;
  }

  public void setContentId(long contentId) {
    this.contentId = contentId;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(int versionNumber) {
    this.versionNumber = versionNumber;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
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

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
