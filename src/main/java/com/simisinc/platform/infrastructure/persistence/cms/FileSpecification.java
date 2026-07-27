/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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
 * Properties for querying objects from the file repository
 *
 * @author matt rajkowski
 * @created 12/12/18 2:07 PM
 */
public class FileSpecification extends Entity {

  private Long id = -1L;
  private long folderId = -1L;
  private long subFolderId = -1L;
  private String filename = null;
  private String barcode = null;
  private long createdBy = -1;
  private String[] fileType = null;
  private Long forUserId = -1L;
  private String matchesName = null;
  private String searchName = null;
  private String searchContent = null;
  private int withinLastDays = -1;
  private int inASubFolder = DataConstants.UNDEFINED;
  private String versionWebPath = null;
  private String[] regionTags = null;
  private String[] filterTags = null;
  private Timestamp modifiedAfter = null;
  private Timestamp modifiedBefore = null;
  private long[] modifiedByUserIds = null;

  public String getVersionWebPath() {
    return versionWebPath;
  }

  public void setVersionWebPath(String versionWebPath) {
    this.versionWebPath = versionWebPath;
  }

  public FileSpecification() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public long getFolderId() {
    return folderId;
  }

  public void setFolderId(long folderId) {
    this.folderId = folderId;
  }

  public long getSubFolderId() {
    return subFolderId;
  }

  public void setSubFolderId(long subFolderId) {
    this.subFolderId = subFolderId;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(long createdBy) {
    this.createdBy = createdBy;
  }

  public String[] getFileType() {
    return fileType;
  }

  public void setFileType(String[] value) {
    this.fileType = value;
  }

  public void setFileType(String value) {
    if (value != null && !value.isEmpty()) {
      String[] valueArray = value.split(",");
      for (int i = 0; i < valueArray.length; i++) {
        valueArray[i] = valueArray[i].trim();
      }
      this.fileType = valueArray;
    } else {
      this.fileType = null;
    }
  }

  public Long getForUserId() {
    return forUserId;
  }

  public void setForUserId(Long forUserId) {
    this.forUserId = forUserId;
  }

  public String getMatchesName() {
    return matchesName;
  }

  public void setMatchesName(String matchesName) {
    this.matchesName = matchesName;
  }

  public String getSearchName() {
    return searchName;
  }

  public void setSearchName(String searchName) {
    this.searchName = searchName;
  }

  public String getSearchContent() {
    return searchContent;
  }

  public void setSearchContent(String searchContent) {
    this.searchContent = searchContent;
  }

  public int getWithinLastDays() {
    return withinLastDays;
  }

  public void setWithinLastDays(int withinLastDays) {
    this.withinLastDays = withinLastDays;
  }

  public int getInASubFolder() {
    return inASubFolder;
  }

  public void setInASubFolder(int inASubFolder) {
    this.inASubFolder = inASubFolder;
  }

  public void setInASubFolder(boolean inASubFolder) {
    this.inASubFolder = (inASubFolder ? DataConstants.TRUE : DataConstants.FALSE);
  }

  public String[] getFilterTags() {
    return filterTags;
  }

  public void setFilterTags(String[] filterTags) {
    this.filterTags = filterTags;
  }

  public String[] getRegionTags() {
    return regionTags;
  }

  public void setRegionTags(String[] regionTags) {
    this.regionTags = regionTags;
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
}
