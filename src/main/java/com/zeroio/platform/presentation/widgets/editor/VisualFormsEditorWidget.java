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
package com.zeroio.platform.presentation.widgets.editor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveFormDefinitionCommand;
import com.simisinc.platform.application.cms.SaveFormFieldCommand;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.domain.model.cms.FormDefinition;
import com.simisinc.platform.domain.model.cms.FormField;
import com.simisinc.platform.infrastructure.persistence.cms.FormDefinitionRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FormFieldRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.infrastructure.permission.Permission;

public class VisualFormsEditorWidget extends GenericWidget {

  static final long serialVersionUID = 1L;
  private static final String PATH = "/admin/visual-forms-editor";

  private boolean allowed(WidgetContext context) {
    return (context.hasRole("admin") || context.hasRole("content-manager"))
        && Permission.check("cms.visual-forms-editor.access", context.getUserSession());
  }

  public WidgetContext execute(WidgetContext context) {
    if (!allowed(context)) {
      return context;
    }
    String returnPage = UrlCommand.getValidReturnPage(context.getParameter("returnPage"));
    context.getRequest().setAttribute("returnPage", returnPage);
    context.getRequest().setAttribute("forms", FormDefinitionRepository.findAll());
    long formId = context.getParameterAsLong("formId");
    FormDefinition form = FormDefinitionRepository.findById(formId);
    Object requestObject = context.getRequestObject();
    if (requestObject instanceof FormDefinition) {
      context.getRequest().setAttribute("formValues", requestObject);
    }
    if (form != null) {
      context.getRequest().setAttribute("form", form);
      context.getRequest().setAttribute("fields", FormFieldRepository.findAllByFormDefinitionId(formId));
      FormField field = requestObject instanceof FormField ? (FormField) requestObject
          : FormFieldRepository.findById(context.getParameterAsLong("fieldId"));
      if (field != null && field.getFormDefinitionId() == formId) {
        context.getRequest().setAttribute("field", field);
        if (requestObject instanceof FormField) {
          context.getRequest().setAttribute("fieldOptions", context.getSharedRequestValue("formFieldOptions"));
        } else if (field.getListOfOptions() != null) {
          StringBuilder options = new StringBuilder();
          field.getListOfOptions().forEach((key, value) -> {
            if (options.length() > 0) {
              options.append('\n');
            }
            options.append(key).append('=').append(value);
          });
          context.getRequest().setAttribute("fieldOptions", options.toString());
        }
      }
    }
    context.setJsp("/cms/visual-forms-editor.jsp");
    return context;
  }

  public WidgetContext post(WidgetContext context) {
    if (!allowed(context)) {
      return context;
    }
    long formId = context.getParameterAsLong("formId");
    String action = context.getParameter("action");
    try {
      if ("saveForm".equals(action)) {
        FormDefinition form = new FormDefinition();
        form.setId(formId);
        form.setName(context.getParameter("name"));
        form.setTitle(context.getParameter("title"));
        form.setSubtitle(context.getParameter("subtitle"));
        form.setButtonName(context.getParameter("buttonName"));
        form.setSuccessTitle(context.getParameter("successTitle"));
        form.setSuccessMessage(context.getParameter("successMessage"));
        form.setEmailTo(context.getParameter("emailTo"));
        form.setUseCaptcha("true".equals(context.getParameter("useCaptcha")));
        form.setCheckForSpam("true".equals(context.getParameter("checkForSpam")));
        form.setEnabled("true".equals(context.getParameter("enabled")));
        form.setCreatedBy(context.getUserId());
        form.setModifiedBy(context.getUserId());
        context.setRequestObject(form);
        FormDefinition existing = FormDefinitionRepository.findById(formId);
        if (existing != null) {
          form.setShowPrivacyNotice(existing.getShowPrivacyNotice());
          form.setSendConfirmationToSubmitter(existing.getSendConfirmationToSubmitter());
          form.setConfirmationSubject(existing.getConfirmationSubject());
          form.setConfirmationMessage(existing.getConfirmationMessage());
        }
        FormDefinition saved = SaveFormDefinitionCommand.saveFormDefinition(form);
        if (saved == null) {
          throw new DataException("The form could not be saved");
        }
        formId = saved.getId();
      } else if ("saveField".equals(action)) {
        FormField field = new FormField();
        field.setId(context.getParameterAsLong("fieldId"));
        field.setFormDefinitionId(formId);
        field.setLabel(context.getParameter("label"));
        field.setName(context.getParameter("fieldName"));
        field.setType(context.getParameter("type"));
        field.setPlaceholder(context.getParameter("placeholder"));
        field.setDefaultValue(context.getParameter("defaultValue"));
        field.setRequired("true".equals(context.getParameter("required")));
        context.setRequestObject(field);
        context.addSharedRequestValue("formFieldOptions", StringUtils.defaultString(context.getParameter("options")));
        if (StringUtils.isNotBlank(field.getName()) && !field.getName().matches("[A-Za-z0-9_-]+")) {
          throw new DataException("Field names may contain only letters, numbers, underscores, and hyphens");
        }
        Map<String, String> options = new LinkedHashMap<>();
        String optionText = context.getParameter("options");
        if (StringUtils.isNotBlank(optionText)) {
          for (String line : optionText.split("\\r?\\n")) {
            String option = line.trim();
            if (StringUtils.isBlank(option)) {
              continue;
            }
            int separator = option.indexOf('=');
            String key = separator < 0 ? option : option.substring(0, separator).trim();
            String value = separator < 0 ? option : option.substring(separator + 1).trim();
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value) || option.contains(",")) {
              throw new DataException("Options must have a value and cannot contain commas");
            }
            if (options.put(key, value) != null) {
              throw new DataException("Option values must be unique");
            }
          }
        }
        if ("select".equals(field.getType()) && options.isEmpty()) {
          throw new DataException("A select field needs at least one option");
        }
        field.setListOfOptions("select".equals(field.getType()) ? options : null);
        if (SaveFormFieldCommand.saveField(field) == null) {
          throw new DataException("The field could not be saved");
        }
      } else if ("deleteField".equals(action)) {
        FormField field = FormFieldRepository.findById(context.getParameterAsLong("fieldId"));
        if (field == null || field.getFormDefinitionId() != formId || !FormFieldRepository.remove(field)) {
          throw new DataException("The field could not be deleted");
        }
      } else if ("deleteForm".equals(action)) {
        FormDefinition form = FormDefinitionRepository.findById(formId);
        if (form == null || !FormDefinitionRepository.remove(form)) {
          throw new DataException("The form could not be deleted");
        }
        formId = -1;
      } else {
        throw new DataException("Unknown action");
      }
      context.setRequestObject(null);
      context.setSuccessMessage("Changes saved");
      context.setRedirect(PATH + (formId > -1 ? "?formId=" + formId : ""));
    } catch (DataException exception) {
      context.setErrorMessage(exception.getMessage());
      context.setRedirect(PATH + (formId > -1 ? "?formId=" + formId : "?new=true"));
    }
    return context;
  }
}