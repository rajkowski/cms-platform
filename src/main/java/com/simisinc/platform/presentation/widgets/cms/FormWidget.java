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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sanctionco.jmail.JMail;
import com.simisinc.platform.application.RateLimitCommand;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.CaptchaCommand;
import com.simisinc.platform.application.cms.FormCommand;
import com.simisinc.platform.application.cms.FormFieldCommand;
import com.simisinc.platform.domain.events.cms.FormSubmittedEvent;
import com.simisinc.platform.domain.model.cms.FormData;
import com.simisinc.platform.domain.model.cms.FormDefinition;
import com.simisinc.platform.domain.model.cms.FormField;
import com.simisinc.platform.infrastructure.persistence.cms.FormDataRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FormDefinitionRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FormFieldRepository;
import com.simisinc.platform.infrastructure.workflow.WorkflowManager;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Form widget for handling form submissions and displaying success messages
 *
 * @author matt rajkowski
 * @created 6/1/18 11:11 AM
 */
public class FormWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(FormWidget.class);

  static String JSP = "/cms/form.jsp";
  static String SUCCESS_JSP = "/cms/form-success.jsp";
  static String RATE_LIMITED_JSP = "/cms/error-rate-limited.jsp";

  public WidgetContext execute(WidgetContext context) {

    // No need to show widget when rate limiting is triggered
    if (!RateLimitCommand.isIpAllowedRightNow(context.getRequest().getRemoteAddr(), false)) {
      context.setJsp(RATE_LIMITED_JSP);
      return context;
    }

    // Initialize the list of form fields, either from the database or preferences
    List<FormField> formFieldList = null;

    // Load the form definition and fields preference if no definition is found
    FormDefinition formDefinition = loadDefinition(context);
    if (formDefinition != null) {
      if (formDefinition.getEnabled()) {
        formFieldList = FormFieldRepository.findAllByFormDefinitionId(formDefinition.getId());
      }
    }

    // Check for preference entries list if no form definition is found
    PreferenceEntriesList fieldsEntriesList = null;
    if (formFieldList == null) {
      fieldsEntriesList = context.getPreferenceAsDataList("fields");
      if (fieldsEntriesList == null || fieldsEntriesList.isEmpty()) {
        return null;
      }
      formFieldList = FormFieldCommand.parseFieldContent(context.getPreferences().get("formUniqueId"), fieldsEntriesList);
    }

    if (formFieldList == null || formFieldList.isEmpty()) {
      return null;
    }

    // Check if the form was successfully submitted in a previous request
    String isSuccess = context.getSharedRequestValue(context.getUniqueId() + "formWidgetSuccess");
    if ("true".equals(isSuccess)) {
      context.getRequest().setAttribute("successTitle",
          formDefinition == null ? context.getPreferences().get("successTitle") : formDefinition.getSuccessTitle());
      context.getRequest().setAttribute("successMessage",
          formDefinition == null ? context.getPreferences().getOrDefault("successMessage", "Your information has been submitted.")
              : StringUtils.defaultIfBlank(formDefinition.getSuccessMessage(), "Your information has been submitted."));
      context.setJsp(SUCCESS_JSP);
      return context;
    }
    context.setJsp(JSP);

    // Preferences
    context.getRequest().setAttribute("buttonName",
        formDefinition == null ? context.getPreferences().getOrDefault("buttonName", "Submit")
            : StringUtils.defaultIfBlank(formDefinition.getButtonName(), "Submit"));

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title",
        formDefinition == null ? context.getPreferences().get("title") : formDefinition.getTitle());
    context.getRequest().setAttribute("subtitle",
        formDefinition == null ? context.getPreferences().get("subtitle") : formDefinition.getSubtitle());

    // Determine the captcha service
    boolean useCaptcha = formDefinition == null ? "true".equals(context.getPreferences().getOrDefault("useCaptcha", "false"))
        : formDefinition.getUseCaptcha();
    if (useCaptcha) {
      context.getRequest().setAttribute("useCaptcha", "true");
      context.getRequest().setAttribute("googleSiteKey", LoadSitePropertyCommand.loadByName("captcha.google.sitekey"));
    }

    // Previous post had error
    FormData formData = (FormData) context.getRequestObject();
    if (formData != null) {
      if (formData.getQueryParameters() != null) {
        context.getRequest().setAttribute("queryString", formData.getQueryParameters());
      }
      context.getRequest().setAttribute("formFieldList", formData.getFormFieldList());
      return context;
    }

    // Use the fields to determine the item properties to be shown
    if (formFieldList == null || formFieldList.isEmpty()) {
      LOG.warn("No fields were found");
      return null;
    }
    context.getRequest().setAttribute("formFieldList", formFieldList);

    // Check for any query parameters to save with the form data
    if (StringUtils.isNotBlank(context.getRequest().getQueryString())) {
      context.getRequest().setAttribute("queryString", context.getRequest().getQueryString());
    }

    // Show the JSP
    return context;
  }

  public WidgetContext post(WidgetContext context) {

    // Determine the fields
    String formUniqueId = null;
    List<FormField> formFieldList = null;
    boolean isValid = true;
    int blankValues = 0;

    // Load the form definition and fields preference if no definition is found
    FormDefinition formDefinition = loadDefinition(context);
    if (formDefinition != null) {
      if (formDefinition.getEnabled()) {
        formUniqueId = formDefinition.getUniqueId();
        formFieldList = FormFieldRepository.findAllByFormDefinitionId(formDefinition.getId());
      }
    }

    // Check for preference entries list if no form definition is found
    PreferenceEntriesList fieldsEntriesList = null;
    if (formFieldList == null) {
      fieldsEntriesList = context.getPreferenceAsDataList("fields");
      if (fieldsEntriesList == null || fieldsEntriesList.isEmpty()) {
        return null;
      }
      formUniqueId = context.getPreferences().get("formUniqueId");
      formFieldList = FormFieldCommand.parseFieldContent(formUniqueId, fieldsEntriesList);
    }

    // Determine the list of form fields based on the form definition or preference entries list
    if (formFieldList == null || formFieldList.isEmpty()) {
      return null;
    }

    for (FormField formField : formFieldList) {
      // Determine the user's value
      String parameterValue = context.getParameter(context.getUniqueId() + formField.getName());
      if (StringUtils.isBlank(parameterValue)) {
        // Check if the field is required
        if (formField.isRequired()) {
          isValid = false;
          context.setWarningMessage(formField.getLabel() + " is required");
        }
        ++blankValues;
        continue;
      }
      parameterValue = parameterValue.trim();
      if (formField.getListOfOptions() != null) {
        if (!formField.getListOfOptions().containsKey(parameterValue)) {
          isValid = false;
          context.setWarningMessage("Check " + formField.getLabel() + " and try again");
          continue;
        }
        formField.setUserValue(formField.getListOfOptions().get(parameterValue));
      } else {
        formField.setUserValue(parameterValue);
      }
      if ("email".equals(formField.getType())) {
        if (!JMail.isValid(parameterValue)) {
          isValid = false;
          context.setWarningMessage("Check the email address and try again");
        }
      }
      LOG.debug("Set userValue " + formField.getName() + "=" + formField.getUserValue());
    }
    if (isValid && blankValues == formFieldList.size()) {
      isValid = false;
      context.setWarningMessage("Check the form and try again");
    }

    // Validate the captcha
    boolean useCaptcha = formDefinition == null ? "true".equals(context.getPreferences().getOrDefault("useCaptcha", "false"))
        : formDefinition.getUseCaptcha();
    if (useCaptcha) {
      boolean captchaSuccess = CaptchaCommand.validateRequest(context);
      if (!captchaSuccess) {
        isValid = false;
        context.setWarningMessage("The form could not be validated");
      }
    }

    // If the user is not logged in, then limit the number of form posts here by IP
    if (!context.getUserSession().isLoggedIn()) {
      if (!RateLimitCommand.isIpAllowedRightNow(context.getRequest().getRemoteAddr(), true)) {
        context.setErrorMessage(RateLimitCommand.INVALID_ATTEMPTS);
        return context;
      }
    }

    // Prepare the object and handle any errors
    FormData formData = new FormData();
    formData.setFormUniqueId(formUniqueId);
    formData.setSessionId(context.getUserSession().getSessionId());
    if (context.getUserSession().isLoggedIn()) {
      formData.setCreatedBy(context.getUserId());
    }
    formData.setFormFieldList(formFieldList);
    formData.setIpAddress(context.getRequest().getRemoteAddr());
    formData.setUrl(context.getUrl() + context.getUri());
    if (context.getParameter("queryString") != null) {
      formData.setQueryParameters(context.getParameter("queryString"));
    }
    if (!isValid) {
      context.setRequestObject(formData);
      return context;
    }

    boolean checkForSpam = formDefinition == null ? Boolean.parseBoolean(context.getPreferences().getOrDefault("checkForSpam", "true"))
        : formDefinition.getCheckForSpam();
    if (checkForSpam) {
      FormCommand.checkNotificationRules(formData);
    }

    // Store in the database
    if (FormDataRepository.save(formData) == null) {
      context.setErrorMessage("The form was not saved... try again?");
      context.setRequestObject(formData);
      return context;
    }

    // Send an alert based on the preferences (or transform for another system)
    String emailAddresses = formDefinition == null ? context.getPreferences().get("emailTo") : formDefinition.getEmailTo();
    WorkflowManager.triggerWorkflowForEvent(new FormSubmittedEvent(formData, emailAddresses));

    // Redirect back so the message can be displayed
    context.addSharedRequestValue(context.getUniqueId() + "formWidgetSuccess", "true");

    return null;
  }

  private FormDefinition loadDefinition(WidgetContext context) {
    String uniqueId = context.getPreferences().get("formUniqueId");
    if (StringUtils.isNotBlank(uniqueId)) {
      return FormDefinitionRepository.findByUniqueId(uniqueId);
    }
    long id = NumberUtils.toLong(context.getPreferences().get("formId"), -1);
    return id > -1 ? FormDefinitionRepository.findById(id) : null;
  }
}
