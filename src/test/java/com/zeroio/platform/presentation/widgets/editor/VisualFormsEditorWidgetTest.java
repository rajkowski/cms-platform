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

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.application.cms.SaveFormDefinitionCommand;
import com.simisinc.platform.application.cms.SaveFormFieldCommand;
import com.simisinc.platform.domain.model.cms.FormDefinition;
import com.simisinc.platform.infrastructure.persistence.cms.FormDefinitionRepository;
import com.zeroio.platform.infrastructure.permission.Permission;

class VisualFormsEditorWidgetTest extends WidgetBase {

  @Test
  void unauthorizedPostCannotModifyForms() {
    addQueryParameter(widgetContext, "action", "deleteForm");
    addQueryParameter(widgetContext, "formId", "42");
    try (MockedStatic<FormDefinitionRepository> forms = mockStatic(FormDefinitionRepository.class)) {
      new VisualFormsEditorWidget().post(widgetContext);
      forms.verifyNoInteractions();
    }
  }

  @Test
  void contentManagerCanOpenEditor() {
    setRoles(widgetContext, CONTENT_MANAGER);
    addQueryParameter(widgetContext, "returnPage", "/tenant-home");
    try (MockedStatic<Permission> permissions = mockStatic(Permission.class);
        MockedStatic<FormDefinitionRepository> forms = mockStatic(FormDefinitionRepository.class)) {
      permissions.when(() -> Permission.check("cms.visual-forms-editor.access", widgetContext.getUserSession())).thenReturn(true);
      new VisualFormsEditorWidget().execute(widgetContext);
      Assertions.assertEquals("/cms/visual-forms-editor.jsp", widgetContext.getJsp());
      Assertions.assertEquals("/tenant-home", widgetContext.getRequest().getAttribute("returnPage"));
      forms.verify(FormDefinitionRepository::findAll);
    }
  }

  @Test
  void externalReturnPageIsRejected() {
    setRoles(widgetContext, CONTENT_MANAGER);
    addQueryParameter(widgetContext, "returnPage", "https://example.com/");
    try (MockedStatic<Permission> permissions = mockStatic(Permission.class);
        MockedStatic<FormDefinitionRepository> forms = mockStatic(FormDefinitionRepository.class)) {
      permissions.when(() -> Permission.check("cms.visual-forms-editor.access", widgetContext.getUserSession())).thenReturn(true);
      new VisualFormsEditorWidget().execute(widgetContext);
      Assertions.assertNull(widgetContext.getRequest().getAttribute("returnPage"));
    }
  }

  @Test
  void contentManagerCanCreateForm() {
    setRoles(widgetContext, CONTENT_MANAGER);
    addQueryParameter(widgetContext, "action", "saveForm");
    addQueryParameter(widgetContext, "name", "Contact Us");
    addQueryParameter(widgetContext, "enabled", "true");
    FormDefinition saved = new FormDefinition();
    saved.setId(42L);
    try (MockedStatic<Permission> permissions = mockStatic(Permission.class);
        MockedStatic<FormDefinitionRepository> forms = mockStatic(FormDefinitionRepository.class);
        MockedStatic<SaveFormDefinitionCommand> save = mockStatic(SaveFormDefinitionCommand.class)) {
      permissions.when(() -> Permission.check("cms.visual-forms-editor.access", widgetContext.getUserSession())).thenReturn(true);
      save.when(() -> SaveFormDefinitionCommand.saveFormDefinition(org.mockito.ArgumentMatchers.any())).thenReturn(saved);
      new VisualFormsEditorWidget().post(widgetContext);
      Assertions.assertEquals("/admin/visual-forms-editor?formId=42", widgetContext.getRedirect());
      save.verify(() -> SaveFormDefinitionCommand
          .saveFormDefinition(org.mockito.ArgumentMatchers.argThat(form -> "Contact Us".equals(form.getName()) && form.getEnabled())));
    }
  }

  @Test
  void invalidFieldRetainsFormAndOptions() {
    setRoles(widgetContext, CONTENT_MANAGER);
    addQueryParameter(widgetContext, "action", "saveField");
    addQueryParameter(widgetContext, "formId", "42");
    addQueryParameter(widgetContext, "label", "Choice");
    addQueryParameter(widgetContext, "fieldName", "invalid name");
    addQueryParameter(widgetContext, "options", "a=Alpha");
    try (MockedStatic<Permission> permissions = mockStatic(Permission.class)) {
      permissions.when(() -> Permission.check("cms.visual-forms-editor.access", widgetContext.getUserSession())).thenReturn(true);
      new VisualFormsEditorWidget().post(widgetContext);
      Assertions.assertEquals("/admin/visual-forms-editor?formId=42", widgetContext.getRedirect());
      Assertions.assertNotNull(widgetContext.getRequestObject());
      Assertions.assertEquals("a=Alpha", widgetContext.getSharedRequestValue("formFieldOptions"));
      Assertions.assertNotNull(widgetContext.getErrorMessage());
    }
  }

  @Test
  void contentManagerCanCreateSelectField() {
    setRoles(widgetContext, CONTENT_MANAGER);
    addQueryParameter(widgetContext, "action", "saveField");
    addQueryParameter(widgetContext, "formId", "42");
    addQueryParameter(widgetContext, "label", "Reason");
    addQueryParameter(widgetContext, "type", "select");
    addQueryParameter(widgetContext, "options", "help=Help\nother=Other");
    try (MockedStatic<Permission> permissions = mockStatic(Permission.class);
        MockedStatic<SaveFormFieldCommand> save = mockStatic(SaveFormFieldCommand.class)) {
      permissions.when(() -> Permission.check("cms.visual-forms-editor.access", widgetContext.getUserSession())).thenReturn(true);
      save.when(() -> SaveFormFieldCommand.saveField(org.mockito.ArgumentMatchers.any()))
          .thenAnswer(invocation -> invocation.getArgument(0));
      new VisualFormsEditorWidget().post(widgetContext);
      Assertions.assertEquals("/admin/visual-forms-editor?formId=42", widgetContext.getRedirect());
      save.verify(() -> SaveFormFieldCommand.saveField(org.mockito.ArgumentMatchers
          .argThat(field -> field.getFormDefinitionId() == 42 && "Help".equals(field.getListOfOptions().get("help")))));
    }
  }

  @Test
  void renamingFormPreservesIdentifier() throws Exception {
    FormDefinition existing = new FormDefinition();
    existing.setId(42L);
    existing.setName("Contact Us");
    existing.setUniqueId("contact-us");
    FormDefinition update = new FormDefinition();
    update.setId(42L);
    update.setName("Contact Our Team");
    update.setModifiedBy(1);
    try (MockedStatic<FormDefinitionRepository> forms = mockStatic(FormDefinitionRepository.class)) {
      forms.when(() -> FormDefinitionRepository.findById(42)).thenReturn(existing);
      forms.when(() -> FormDefinitionRepository.save(org.mockito.ArgumentMatchers.any()))
          .thenAnswer(invocation -> invocation.getArgument(0));
      Assertions.assertEquals("contact-us", SaveFormDefinitionCommand.saveFormDefinition(update).getUniqueId());
    }
  }
}