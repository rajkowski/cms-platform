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

package com.simisinc.platform.infrastructure.persistence.cms;

import java.sql.Timestamp;

import com.simisinc.platform.domain.model.Entity;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Properties for querying across pages, blog posts, events, attachments and items at once
 *
 * @author matt rajkowski
 * @created 10/15/26 8:00 AM
 */
public class UnifiedSearchSpecification extends Entity {

  private String searchTerm = null;
  // SearchCriteria type constants; null or empty means all types
  private String[] types = null;
  private String[] regionTags = null;
  private String[] filterTags = null;
  private Timestamp modifiedAfter = null;
  private Timestamp modifiedBefore = null;
  private long[] modifiedByUserIds = null;
  private long forUserId = DataConstants.UNDEFINED;
  // Can see unpublished pages and every attachment
  private boolean contentManager = false;
  // Can see unapproved items
  private boolean dataManager = false;

  public UnifiedSearchSpecification() {
  }

  public String getSearchTerm() {
    return searchTerm;
  }

  public void setSearchTerm(String searchTerm) {
    this.searchTerm = searchTerm;
  }

  public String[] getTypes() {
    return types;
  }

  public void setTypes(String[] types) {
    this.types = types;
  }

  public String[] getRegionTags() {
    return regionTags;
  }

  public void setRegionTags(String[] regionTags) {
    this.regionTags = regionTags;
  }

  public String[] getFilterTags() {
    return filterTags;
  }

  public void setFilterTags(String[] filterTags) {
    this.filterTags = filterTags;
  }

  public Timestamp getModifiedAfter() {
    return modifiedAfter;
  }

  public void setModifiedAfter(Timestamp modifiedAfter) {
    this.modifiedAfter = modifiedAfter;
  }

  public Timestamp getModifiedBefore() {
    return modifiedBefore;
  }

  public void setModifiedBefore(Timestamp modifiedBefore) {
    this.modifiedBefore = modifiedBefore;
  }

  public long[] getModifiedByUserIds() {
    return modifiedByUserIds;
  }

  public void setModifiedByUserIds(long[] modifiedByUserIds) {
    this.modifiedByUserIds = modifiedByUserIds;
  }

  public long getForUserId() {
    return forUserId;
  }

  public void setForUserId(long forUserId) {
    this.forUserId = forUserId;
  }

  public boolean isContentManager() {
    return contentManager;
  }

  public void setContentManager(boolean contentManager) {
    this.contentManager = contentManager;
  }

  public boolean isDataManager() {
    return dataManager;
  }

  public void setDataManager(boolean dataManager) {
    this.dataManager = dataManager;
  }
}
