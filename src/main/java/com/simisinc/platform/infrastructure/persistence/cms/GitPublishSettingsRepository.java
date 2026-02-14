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

import com.simisinc.platform.domain.model.cms.GitPublishSettings;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.SqlUtils;

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
    return (GitPublishSettings) DB.selectRecordFrom(
        TABLE_NAME,
        null,
        GitPublishSettingsRepository::buildRecord);
  }

  public static GitPublishSettings save(GitPublishSettings record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static GitPublishSettings add(GitPublishSettings record) {
    SqlUtils insertValues = new SqlUtils()
        .add("enabled", record.getEnabled())
        .add("git_provider", record.getGitProvider())
        .add("repository_url", record.getRepositoryUrl())
        .add("branch_name", record.getBranchName())
        .add("base_branch", record.getBaseBranch())
        .add("access_token", record.getAccessToken())
        .add("username", record.getUsername())
        .add("email", record.getEmail())
        .add("commit_message_template", record.getCommitMessageTemplate())
        .add("auto_create_pr", record.getAutoCreatePr())
        .add("pr_title_template", record.getPrTitleTemplate())
        .add("pr_description_template", record.getPrDescriptionTemplate())
        .add("target_directory", record.getTargetDirectory())
        .add("created_by", record.getCreatedBy(), -1)
        .add("modified_by", record.getModifiedBy(), -1);
    record.setId(DB.insertInto(TABLE_NAME, insertValues, PRIMARY_KEY));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static GitPublishSettings update(GitPublishSettings record) {
    SqlUtils updateValues = new SqlUtils()
        .add("enabled", record.getEnabled())
        .add("git_provider", record.getGitProvider())
        .add("repository_url", record.getRepositoryUrl())
        .add("branch_name", record.getBranchName())
        .add("base_branch", record.getBaseBranch())
        .add("access_token", record.getAccessToken())
        .add("username", record.getUsername())
        .add("email", record.getEmail())
        .add("commit_message_template", record.getCommitMessageTemplate())
        .add("auto_create_pr", record.getAutoCreatePr())
        .add("pr_title_template", record.getPrTitleTemplate())
        .add("pr_description_template", record.getPrDescriptionTemplate())
        .add("target_directory", record.getTargetDirectory())
        .add("modified", new Timestamp(System.currentTimeMillis()))
        .add("modified_by", record.getModifiedBy(), -1);
    SqlUtils where = new SqlUtils()
        .add("settings_id = ?", record.getId());
    if (DB.update(TABLE_NAME, updateValues, where)) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(GitPublishSettings record) {
    try (Connection connection = DB.getConnection()) {
      return DB.deleteFrom(connection, TABLE_NAME, PRIMARY_KEY, record.getId());
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
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
