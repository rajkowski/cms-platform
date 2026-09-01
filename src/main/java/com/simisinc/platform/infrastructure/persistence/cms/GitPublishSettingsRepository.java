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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.cms.GitPublishSettings;

/**
 * Persists and retrieves Git publish settings objects
 *
 * @author matt rajkowski
 * @created 2/14/26 2:00 PM
 */
public class GitPublishSettingsRepository {

  private static Log LOG = LogFactory.getLog(GitPublishSettingsRepository.class);

  private static String TABLE_NAME = "git_publish_settings";
  private static String[] PRIMARY_KEY = new String[] { "settings_id" };

  public static GitPublishSettings findSettings() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .returnRecord(GitPublishSettingsRepository::buildRecord);
  }

  public static GitPublishSettings save(GitPublishSettings record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static GitPublishSettings add(GitPublishSettings record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("enabled", record.getEnabled())
        .FIELD("git_provider", record.getGitProvider())
        .FIELD("repository_url", record.getRepositoryUrl())
        .FIELD("branch_name", record.getBranchName())
        .FIELD("base_branch", record.getBaseBranch())
        .FIELD("access_token", record.getAccessToken())
        .FIELD("username", record.getUsername())
        .FIELD("email", record.getEmail())
        .FIELD("commit_message_template", record.getCommitMessageTemplate())
        .FIELD("auto_create_pr", record.getAutoCreatePr())
        .FIELD("pr_title_template", record.getPrTitleTemplate())
        .FIELD("pr_description_template", record.getPrDescriptionTemplate())
        .FIELD("target_directory", record.getTargetDirectory())
        .FIELD("created_by", record.getCreatedBy() == -1 ? null : record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static GitPublishSettings update(GitPublishSettings record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("enabled", record.getEnabled())
        .SET("git_provider", record.getGitProvider())
        .SET("repository_url", record.getRepositoryUrl())
        .SET("branch_name", record.getBranchName())
        .SET("base_branch", record.getBaseBranch())
        .SET("access_token", record.getAccessToken())
        .SET("username", record.getUsername())
        .SET("email", record.getEmail())
        .SET("commit_message_template", record.getCommitMessageTemplate())
        .SET("auto_create_pr", record.getAutoCreatePr())
        .SET("pr_title_template", record.getPrTitleTemplate())
        .SET("pr_description_template", record.getPrDescriptionTemplate())
        .SET("target_directory", record.getTargetDirectory())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy())
        .WHERE("settings_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(GitPublishSettings record) {
    return DB.DELETE().FROM(TABLE_NAME).WHERE("settings_id = ?", record.getId()).execute();
  }

  private static GitPublishSettings buildRecord(ResultSet rs) {
    try {
      GitPublishSettings record = new GitPublishSettings();
      record.setId(rs.getLong("settings_id"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setGitProvider(rs.getString("git_provider"));
      record.setRepositoryUrl(rs.getString("repository_url"));
      record.setBranchName(rs.getString("branch_name"));
      record.setBaseBranch(rs.getString("base_branch"));
      record.setAccessToken(rs.getString("access_token"));
      record.setUsername(rs.getString("username"));
      record.setEmail(rs.getString("email"));
      record.setCommitMessageTemplate(rs.getString("commit_message_template"));
      record.setAutoCreatePr(rs.getBoolean("auto_create_pr"));
      record.setPrTitleTemplate(rs.getString("pr_title_template"));
      record.setPrDescriptionTemplate(rs.getString("pr_description_template"));
      record.setTargetDirectory(rs.getString("target_directory"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setModifiedBy(DB.getLong(rs, "modified_by", -1));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
