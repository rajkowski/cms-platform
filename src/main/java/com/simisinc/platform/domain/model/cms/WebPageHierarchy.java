/*
 * Copyright 2026 Matt Rajkowski
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

package com.simisinc.platform.domain.model.cms;

import com.simisinc.platform.domain.model.Entity;

import java.sql.Timestamp;

/**
 * Represents a page hierarchy record for organizing pages in a tree structure
 *
 * @author matt rajkowski
 * @created 2/8/26 8:00 AM
 */
public class WebPageHierarchy extends Entity {

  private long webPageId = -1;
  private Long parentPageId = null;
  private int sortOrder = 0;
  private int depth = 0;
  private String path = null;
  private Timestamp created = null;
  private Timestamp modified = null;

  public WebPageHierarchy() {
    // Default constructor required for bean instantiation
  }

  public long getWebPageId() {
    return webPageId;
  }

  public void setWebPageId(long webPageId) {
    this.webPageId = webPageId;
  }

  public Long getParentPageId() {
    return parentPageId;
  }

  public void setParentPageId(Long parentPageId) {
    this.parentPageId = parentPageId;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
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
