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

package com.simisinc.platform.application.admin;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.infrastructure.persistence.SitePropertyRepository;

class VisualWorkspaceEditorCommandTest {

  @Test
  void rejectsUrlTamperingWithoutSavingOtherSubmittedValues() {
    try (MockedStatic<SitePropertyRepository> repository = mockStatic(SitePropertyRepository.class)) {
      repository.when(() -> SitePropertyRepository.findAllByPrefix("site")).thenReturn(List.of(property("site.name", "text", "Original")));
      repository.when(() -> SitePropertyRepository.findAllByPrefix("theme"))
          .thenReturn(List.of(property("theme.body.backgroundColor", "color", "#ffffff")));

      VisualWorkspaceEditorCommand.SaveResult result = VisualWorkspaceEditorCommand.saveValues(
          Map.of("site.name", "Changed", "site.url", "https://attacker.example"));

      Assertions.assertFalse(result.isSuccessful());
      Assertions.assertTrue(result.getErrors().containsKey("site.url"));
      repository.verify(() -> SitePropertyRepository.saveAll(anyString(), anyList()), never());
    }
  }

  @Test
  void rejectsInvalidColorWithoutSavingSubmittedChanges() {
    try (MockedStatic<SitePropertyRepository> repository = mockStatic(SitePropertyRepository.class)) {
      repository.when(() -> SitePropertyRepository.findAllByPrefix("site"))
          .thenReturn(List.of(property("site.name", "text", "Original")));
      repository.when(() -> SitePropertyRepository.findAllByPrefix("theme"))
          .thenReturn(List.of(property("theme.body.backgroundColor", "color", "#ffffff")));

      VisualWorkspaceEditorCommand.SaveResult result = VisualWorkspaceEditorCommand.saveValues(
          Map.of("site.name", "Changed", "theme.body.backgroundColor", "not-a-color"));

      Assertions.assertFalse(result.isSuccessful());
      Assertions.assertTrue(result.getErrors().containsKey("theme.body.backgroundColor"));
      repository.verify(() -> SitePropertyRepository.saveAll(anyString(), anyList()), never());
    }
  }

  @Test
  void restrictsValuesToTheActiveEditor() {
    try (MockedStatic<SitePropertyRepository> repository = mockStatic(SitePropertyRepository.class)) {
      repository.when(() -> SitePropertyRepository.findAllByPrefix("site"))
          .thenReturn(List.of(property("site.name", "text", "Original")));
      repository.when(() -> SitePropertyRepository.findAllByPrefix("theme"))
          .thenReturn(List.of(property("theme.body.backgroundColor", "color", "#ffffff")));

      VisualWorkspaceEditorCommand.SaveResult themeResult = VisualWorkspaceEditorCommand.saveValues(
          Map.of("site.name", "Changed"), "theme.");
      VisualWorkspaceEditorCommand.SaveResult workspaceResult = VisualWorkspaceEditorCommand.saveValues(
          Map.of("theme.body.backgroundColor", "#000000"), "site.");

      Assertions.assertFalse(themeResult.isSuccessful());
      Assertions.assertTrue(themeResult.getErrors().containsKey("site.name"));
      Assertions.assertFalse(workspaceResult.isSuccessful());
      Assertions.assertTrue(workspaceResult.getErrors().containsKey("theme.body.backgroundColor"));
      repository.verify(() -> SitePropertyRepository.saveAll(anyString(), anyList()), never());
    }
  }

  @Test
  void ignoresUnchangedEmptyThemeColorsWhenSavingAPreset() {
    try (MockedStatic<SitePropertyRepository> repository = mockStatic(SitePropertyRepository.class)) {
      SiteProperty background = property("theme.body.backgroundColor", "color", "#ffffff");
      SiteProperty footer = property("theme.footer.backgroundColor", "color", "");
      repository.when(() -> SitePropertyRepository.findAllByPrefix("site")).thenReturn(List.of());
      repository.when(() -> SitePropertyRepository.findAllByPrefix("theme"))
          .thenReturn(List.of(background, footer));
      repository.when(() -> SitePropertyRepository.saveAll(anyString(), anyList())).thenReturn(true);

      VisualWorkspaceEditorCommand.SaveResult result = VisualWorkspaceEditorCommand.saveValues(
          Map.of(
              "theme.body.backgroundColor", "#000000",
              "theme.footer.backgroundColor", ""),
          "theme.");

      Assertions.assertTrue(result.isSuccessful());
      repository.verify(() -> SitePropertyRepository.saveAll("site,theme", List.of(background)));
    }
  }

  private static SiteProperty property(String name, String type, String value) {
    SiteProperty property = new SiteProperty();
    property.setId(1);
    property.setName(name);
    property.setLabel(name);
    property.setType(type);
    property.setValue(value);
    return property;
  }
}
