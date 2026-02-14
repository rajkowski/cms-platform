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

package com.simisinc.platform.domain.model.cms;

import java.sql.Timestamp;

import com.simisinc.platform.domain.model.Entity;

/**
 * Git publish settings for static site export
 *
 * @author matt rajkowski
 * @created 2/14/26 2:00 PM
 */
public class GitPublishSettings extends Entity {

  private Long id = -1L;

  private boolean enabled = false;
  private String gitProvider = null;
  private String repositoryUrl = null;
  private String branchName = "main";
  private String baseBranch = "main";
  private String accessToken = null;
  private String username = null;
  private String email = null;
  private String commitMessageTemplate = "Static site update: ${timestamp}";
  private boolean autoCreatePr = true;
  private String prTitleTemplate = "Static site update: ${timestamp}";
  private String prDescriptionTemplate = "Automated static site export";
  private String targetDirectory = "/";

  private Timestamp created = null;
  private Timestamp modified = null;
  private long createdBy = -1L;
  private long modifiedBy = -1L;

  public GitPublishSettings() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getGitProvider() {
    return gitProvider;
  }

  public void setGitProvider(String gitProvider) {
    this.gitProvider = gitProvider;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public void setRepositoryUrl(String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public String getBaseBranch() {
    return baseBranch;
  }

  public void setBaseBranch(String baseBranch) {
    this.baseBranch = baseBranch;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCommitMessageTemplate() {
    return commitMessageTemplate;
  }

  public void setCommitMessageTemplate(String commitMessageTemplate) {
    this.commitMessageTemplate = commitMessageTemplate;
  }

  public boolean getAutoCreatePr() {
    return autoCreatePr;
  }

  public void setAutoCreatePr(boolean autoCreatePr) {
    this.autoCreatePr = autoCreatePr;
  }

  public String getPrTitleTemplate() {
    return prTitleTemplate;
  }

  public void setPrTitleTemplate(String prTitleTemplate) {
    this.prTitleTemplate = prTitleTemplate;
  }

  public String getPrDescriptionTemplate() {
    return prDescriptionTemplate;
  }

  public void setPrDescriptionTemplate(String prDescriptionTemplate) {
    this.prDescriptionTemplate = prDescriptionTemplate;
  }

  public String getTargetDirectory() {
    return targetDirectory;
  }

  public void setTargetDirectory(String targetDirectory) {
    this.targetDirectory = targetDirectory;
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
}
