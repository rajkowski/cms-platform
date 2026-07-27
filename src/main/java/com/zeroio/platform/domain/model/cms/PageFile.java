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

import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.domain.model.Entity;

/**
 * Represents a relationship between a web page and a file (web_page_files table)
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class PageFile extends Entity {

  private static final long serialVersionUID = 1L;

  private long id = -1L;
  private long webPageId = -1L;
  private long fileId = -1L;
  private Timestamp created = null;
  private long createdBy = -1L;

  // File details (joined from files table)
  private String filename = null;
  private String title = null;
  private String extension = null;
  private long fileLength = -1L;
  private String fileType = null;
  private String mimeType = null;
  private String webPath = null;
  private String version = null;
  private String summary = null;
  private Timestamp fileModified = null;
  private long fileModifiedBy = -1L;

  public PageFile() {
    // default constructor required for framework instantiation
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getWebPageId() {
    return webPageId;
  }

  public void setWebPageId(long webPageId) {
    this.webPageId = webPageId;
  }

  public long getFileId() {
    return fileId;
  }

  public void setFileId(long fileId) {
    this.fileId = fileId;
  }

  public Timestamp getCreated() {
    return created;
  }

  public void setCreated(Timestamp created) {
    this.created = created;
  }

  public long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(long createdBy) {
    this.createdBy = createdBy;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public long getFileLength() {
    return fileLength;
  }

  public void setFileLength(long fileLength) {
    this.fileLength = fileLength;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getWebPath() {
    return webPath;
  }

  public void setWebPath(String webPath) {
    this.webPath = webPath;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public Timestamp getFileModified() {
    return fileModified;
  }

  public void setFileModified(Timestamp fileModified) {
    this.fileModified = fileModified;
  }

  public long getFileModifiedBy() {
    return fileModifiedBy;
  }

  public void setFileModifiedBy(long fileModifiedBy) {
    this.fileModifiedBy = fileModifiedBy;
  }

  /**
   * Returns the base download URL for the file: webPath-fileId
   */
  public String getBaseUrl() {
    if (webPath == null || fileId == -1L) {
      return null;
    }
    return webPath + "-" + fileId;
  }

  public String getUrl() {
    return getBaseUrl() + "/" + UrlCommand.encodeUri(filename);
  }

  /**
   * Returns the display name: title if set, otherwise filename
   */
  public String getDisplayName() {
    if (title != null && !title.isEmpty()) {
      return title;
    }
    return filename;
  }
}
