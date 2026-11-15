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

package com.simisinc.platform.application.cms;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sanctionco.jmail.JMail;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.FieldLengthCommand;
import com.simisinc.platform.domain.model.cms.FormDefinition;
import com.simisinc.platform.infrastructure.persistence.cms.FormDefinitionRepository;

/**
 * Validates and saves a database-backed form definition
 */
public class SaveFormDefinitionCommand {

  private static Log LOG = LogFactory.getLog(SaveFormDefinitionCommand.class);

  private static final int MAX_NAME_LENGTH = 255;
  private static final int MAX_TITLE_LENGTH = 255;
  private static final int MAX_BUTTON_NAME_LENGTH = 100;

  private static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyz1234567890";

  public static FormDefinition saveFormDefinition(FormDefinition formDefinitionBean) throws DataException {

    // Validate the required fields
    StringBuilder errorMessages = new StringBuilder();
    if (StringUtils.isBlank(formDefinitionBean.getName())) {
      errorMessages.append("A name is required");
    } else {
      FieldLengthCommand.appendIfTooLong(errorMessages, ", ", "A name",
          formDefinitionBean.getName(), MAX_NAME_LENGTH);
    }
    FieldLengthCommand.appendIfTooLong(errorMessages, ", ", "A title",
        formDefinitionBean.getTitle(), MAX_TITLE_LENGTH);
    FieldLengthCommand.appendIfTooLong(errorMessages, ", ", "A button name",
        formDefinitionBean.getButtonName(), MAX_BUTTON_NAME_LENGTH);
    if (formDefinitionBean.getModifiedBy() == -1) {
      if (errorMessages.length() > 0) {
        errorMessages.append(", ");
      }
      errorMessages.append("The user saving this form was not set");
    }
    // Validate the "Email submissions to" field if it is provided
    String emailToError = findInvalidEmailAddress(formDefinitionBean.getEmailTo());
    if (emailToError != null) {
      if (errorMessages.length() > 0) {
        errorMessages.append(", ");
      }
      errorMessages.append("'").append(emailToError).append("' is not a valid email address");
    }

    if (errorMessages.length() > 0) {
      throw new DataException("Please check the form and try again:\n" + errorMessages.toString());
    }

    // Transform the fields and store
    FormDefinition formDefinition;
    if (formDefinitionBean.getId() > -1) {
      LOG.debug("Saving an existing record... ");
      formDefinition = FormDefinitionRepository.findById(formDefinitionBean.getId());
      if (formDefinition == null) {
        throw new DataException("The existing record could not be found");
      }
    } else {
      LOG.debug("Saving a new record... ");
      formDefinition = new FormDefinition();
      formDefinition.setCreatedBy(formDefinitionBean.getCreatedBy());
    }
    // @note set the uniqueId before setting the name
    formDefinition.setUniqueId(generateUniqueId(formDefinition, formDefinitionBean));
    formDefinition.setName(formDefinitionBean.getName());
    formDefinition.setTitle(formDefinitionBean.getTitle());
    formDefinition.setSubtitle(formDefinitionBean.getSubtitle());
    formDefinition.setButtonName(formDefinitionBean.getButtonName());
    formDefinition.setSuccessTitle(formDefinitionBean.getSuccessTitle());
    formDefinition.setSuccessMessage(formDefinitionBean.getSuccessMessage());
    formDefinition.setEmailTo(formDefinitionBean.getEmailTo());
    formDefinition.setUseCaptcha(formDefinitionBean.getUseCaptcha());
    formDefinition.setCheckForSpam(formDefinitionBean.getCheckForSpam());
    formDefinition.setEnabled(formDefinitionBean.getEnabled());
    formDefinition.setShowPrivacyNotice(formDefinitionBean.getShowPrivacyNotice());
    formDefinition.setSendConfirmationToSubmitter(formDefinitionBean.getSendConfirmationToSubmitter());
    formDefinition.setConfirmationSubject(formDefinitionBean.getConfirmationSubject());
    formDefinition.setConfirmationMessage(formDefinitionBean.getConfirmationMessage());
    if (formDefinitionBean.getModifiedBy() > -1) {
      formDefinition.setModifiedBy(formDefinitionBean.getModifiedBy());
    } else {
      formDefinition.setModifiedBy(formDefinitionBean.getCreatedBy());
    }
    return FormDefinitionRepository.save(formDefinition);
  }

  /**
   * Returns the first entry that fails syntax check
   */
  public static String findInvalidEmailAddress(String emailTo) {
    if (StringUtils.isBlank(emailTo)) {
      return null;
    }
    for (String address : emailTo.split(",")) {
      String trimmed = address.trim();
      if (StringUtils.isBlank(trimmed)) {
        continue;
      }
      if (!JMail.isValid(trimmed)) {
        return trimmed;
      }
    }
    return null;
  }

  private static String generateUniqueId(FormDefinition previousItem, FormDefinition item) {

    if (StringUtils.isNotBlank(previousItem.getUniqueId())) {
      return previousItem.getUniqueId();
    }

    // Create a new one from the name
    StringBuilder sb = new StringBuilder();
    String name = item.getName().toLowerCase();
    final int len = name.length();
    for (int i = 0; i < len; i++) {
      char c = name.charAt(i);
      if (ALLOWED_CHARS.indexOf(c) > -1) {
        sb.append(c);
      } else if (c == ' ') {
        sb.append("-");
      }
    }

    // Find the next available unique instance
    int count = 1;
    String originalUniqueId = StringUtils.defaultIfBlank(sb.toString(), "form");
    String uniqueId = originalUniqueId;
    while (FormDefinitionRepository.findByUniqueId(uniqueId) != null) {
      ++count;
      uniqueId = originalUniqueId + "-" + count;
    }
    return uniqueId;
  }
}
