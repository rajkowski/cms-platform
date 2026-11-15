/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2026 SimIS Inc. (https://www.simiscms.com)
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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.cms.FormDefinition;

/**
 * Persists and retrieves database-backed form configurations
 */
public class FormDefinitionRepository {

  private static Log LOG = LogFactory.getLog(FormDefinitionRepository.class);

  private static final String TABLE_NAME = "form_definitions";

  public static List<FormDefinition> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .ORDER_BY("name")
        .returnDataResult(FormDefinitionRepository::buildRecord)
        .getRecords();
  }

  public static FormDefinition findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("form_definition_id = ?", id)
        .returnRecord(FormDefinitionRepository::buildRecord);
  }

  public static FormDefinition findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("unique_id = ?", uniqueId.trim())
        .returnRecord(FormDefinitionRepository::buildRecord);
  }

  public static FormDefinition save(FormDefinition record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static FormDefinition add(FormDefinition record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD_UNLESS_NULL("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD_UNLESS_NULL("subtitle", StringUtils.trimToNull(record.getSubtitle()))
        .FIELD_UNLESS_NULL("button_name", StringUtils.trimToNull(record.getButtonName()))
        .FIELD_UNLESS_NULL("success_title", StringUtils.trimToNull(record.getSuccessTitle()))
        .FIELD_UNLESS_NULL("success_message", StringUtils.trimToNull(record.getSuccessMessage()))
        .FIELD_UNLESS_NULL("email_to", StringUtils.trimToNull(record.getEmailTo()))
        .FIELD("use_captcha", record.getUseCaptcha())
        .FIELD("check_for_spam", record.getCheckForSpam())
        .FIELD("enabled", record.getEnabled())
        .FIELD("show_privacy_notice", record.getShowPrivacyNotice())
        .FIELD("send_confirmation_to_submitter", record.getSendConfirmationToSubmitter())
        .FIELD_UNLESS_NULL("notification_subject", StringUtils.trimToNull(record.getNotificationSubject()))
        .FIELD_UNLESS_NULL("confirmation_subject", StringUtils.trimToNull(record.getConfirmationSubject()))
        .FIELD_UNLESS_NULL("confirmation_message", StringUtils.trimToNull(record.getConfirmationMessage()))
        .FIELD_UNLESS_NULL("created_by", record.getCreatedBy() == -1 ? null : record.getCreatedBy())
        .FIELD_UNLESS_NULL("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static FormDefinition update(FormDefinition record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("subtitle", StringUtils.trimToNull(record.getSubtitle()))
        .SET("button_name", StringUtils.trimToNull(record.getButtonName()))
        .SET("success_title", StringUtils.trimToNull(record.getSuccessTitle()))
        .SET("success_message", StringUtils.trimToNull(record.getSuccessMessage()))
        .SET("email_to", StringUtils.trimToNull(record.getEmailTo()))
        .SET("use_captcha", record.getUseCaptcha())
        .SET("check_for_spam", record.getCheckForSpam())
        .SET("enabled", record.getEnabled())
        .SET("show_privacy_notice", record.getShowPrivacyNotice())
        .SET("send_confirmation_to_submitter", record.getSendConfirmationToSubmitter())
        .SET("notification_subject", StringUtils.trimToNull(record.getNotificationSubject()))
        .SET("confirmation_subject", StringUtils.trimToNull(record.getConfirmationSubject()))
        .SET("confirmation_message", StringUtils.trimToNull(record.getConfirmationMessage()))
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (record.getModifiedBy() != -1) {
      update.SET("modified_by", record.getModifiedBy());
    }
    if (update.WHERE("form_definition_id = ?", record.getId()).execute()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  /**
   * Deletes a form definition and, in the same transaction, every field that belongs to it
   */
  public static boolean remove(FormDefinition record) {
    try {
      try (Connection connection = DB.getConnection();
          AutoStartTransaction a = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {
        // Delete the references
        FormFieldRepository.removeAll(connection, record);
        // Delete the record
        DB.DELETE().FROM(TABLE_NAME).WHERE("form_definition_id = ?", record.getId()).execute(connection);
        // Finish transaction
        transaction.commit();
        return true;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static FormDefinition buildRecord(ResultSet rs) {
    try {
      FormDefinition record = new FormDefinition();
      record.setId(rs.getLong("form_definition_id"));
      record.setUniqueId(rs.getString("unique_id"));
      record.setName(rs.getString("name"));
      record.setTitle(rs.getString("title"));
      record.setSubtitle(rs.getString("subtitle"));
      record.setButtonName(rs.getString("button_name"));
      record.setSuccessTitle(rs.getString("success_title"));
      record.setSuccessMessage(rs.getString("success_message"));
      record.setEmailTo(rs.getString("email_to"));
      record.setUseCaptcha(rs.getBoolean("use_captcha"));
      record.setCheckForSpam(rs.getBoolean("check_for_spam"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setShowPrivacyNotice(rs.getBoolean("show_privacy_notice"));
      record.setSendConfirmationToSubmitter(rs.getBoolean("send_confirmation_to_submitter"));
      record.setNotificationSubject(rs.getString("notification_subject"));
      record.setConfirmationSubject(rs.getString("confirmation_subject"));
      record.setConfirmationMessage(rs.getString("confirmation_message"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
