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

package com.simisinc.platform.presentation.widgets.admin;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Part;

import org.junit.jupiter.api.Test;

import com.simisinc.platform.application.cms.ThemeCommand;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.domain.model.cms.Theme;
import com.simisinc.platform.infrastructure.persistence.SitePropertyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ThemeRepository;
import com.simisinc.platform.presentation.controller.PageRequest;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.zeroio.platform.infrastructure.permission.Permission;

class VisualThemeEditorWidgetTest {

  @Test
  void requiresAThemeNameBeforeSavingColors() {
    WidgetContext context = mock(WidgetContext.class);
    PageRequest request = mock(PageRequest.class);
    SiteProperty property = property("theme.body.backgroundColor", "#ffffff");
    when(context.getParameterMap()).thenReturn(Map.of(
        "theme.body.backgroundColor", new String[] { "#000000" },
        "themeSnapshotBaseName", new String[] { " " }));
    when(context.getParameter("themeSnapshotBaseName")).thenReturn(" ");
    when(context.getRequest()).thenReturn(request);

    try (var permissions = mockStatic(Permission.class);
        var siteProperties = mockStatic(SitePropertyRepository.class);
        var themes = mockStatic(ThemeRepository.class)) {
      permissions.when(() -> Permission.check(anyString(), org.mockito.ArgumentMatchers.isNull())).thenReturn(true);
      siteProperties.when(() -> SitePropertyRepository.findAllByPrefix("site")).thenReturn(new ArrayList<>());
      siteProperties.when(() -> SitePropertyRepository.findAllByPrefix("theme")).thenReturn(List.of(property));
      themes.when(() -> ThemeRepository.findAll()).thenReturn(new ArrayList<>());

      new VisualThemeEditorWidget().post(context);

      siteProperties.verify(() -> SitePropertyRepository.saveAll(anyString(),
          org.mockito.ArgumentMatchers.anyList()), never());
      verify(context).setErrorMessage("Enter a name for the saved theme.");
    }
  }

  @Test
  void savesAThemeSnapshotAfterSavingThemeColors() {
    WidgetContext context = mock(WidgetContext.class);
    PageRequest request = mock(PageRequest.class);
    SiteProperty property = property("theme.body.backgroundColor", "#ffffff");
    when(context.getParameterMap()).thenReturn(Map.of(
        "theme.body.backgroundColor", new String[] { "#000000" },
        "themeSnapshotBaseName", new String[] { "Midnight" }));
    when(context.getParameter("themeSnapshotBaseName")).thenReturn("Midnight");
    when(context.getRequest()).thenReturn(request);

    try (var permissions = mockStatic(Permission.class);
        var siteProperties = mockStatic(SitePropertyRepository.class);
        var themes = mockStatic(ThemeRepository.class);
        var themeCommand = mockStatic(ThemeCommand.class)) {
      permissions.when(() -> Permission.check(anyString(), org.mockito.ArgumentMatchers.isNull())).thenReturn(true);
      siteProperties.when(() -> SitePropertyRepository.findAllByPrefix("site")).thenReturn(new ArrayList<>());
      siteProperties.when(() -> SitePropertyRepository.findAllByPrefix("theme")).thenReturn(List.of(property));
      siteProperties.when(() -> SitePropertyRepository.saveAll(anyString(),
          org.mockito.ArgumentMatchers.anyList())).thenReturn(true);
      themes.when(() -> ThemeRepository.findAll()).thenReturn(new ArrayList<>());
      themeCommand.when(() -> ThemeCommand.createSnapshotWithName("Midnight"))
          .thenReturn(new Theme("Midnight"));

      new VisualThemeEditorWidget().post(context);

      themeCommand.verify(() -> ThemeCommand.createSnapshotWithName("Midnight"));
      verify(context).setSuccessMessage("Theme colors were saved and added to history.");
    }
  }

  @Test
  void uploadsThemeColorsAsUnsavedDrafts() throws Exception {
    WidgetContext context = mock(WidgetContext.class);
    PageRequest request = mock(PageRequest.class);
    Part part = mock(Part.class);
    SiteProperty property = property("theme.body.backgroundColor", "#ffffff");
    String file = """
        {
          "format": "cms-platform-theme-colors",
          "version": 1,
          "name": "Ocean",
          "colors": {
            "theme.body.backgroundColor": "#0077B6"
          }
        }
        """;
    when(context.getParameter("command")).thenReturn("uploadThemeColors");
    when(context.getPart("file")).thenReturn(part);
    when(context.getRequest()).thenReturn(request);
    when(part.getSubmittedFileName()).thenReturn("ocean.json");
    when(part.getSize()).thenReturn((long) file.length());
    when(part.getInputStream()).thenReturn(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)));

    try (var permissions = mockStatic(Permission.class);
        var siteProperties = mockStatic(SitePropertyRepository.class);
        var themes = mockStatic(ThemeRepository.class)) {
      permissions.when(() -> Permission.check(anyString(), org.mockito.ArgumentMatchers.isNull())).thenReturn(true);
      siteProperties.when(() -> SitePropertyRepository.findAllByPrefix("site")).thenReturn(new ArrayList<>());
      siteProperties.when(() -> SitePropertyRepository.findAllByPrefix("theme")).thenReturn(List.of(property));
      themes.when(() -> ThemeRepository.findAll()).thenReturn(new ArrayList<>());

      new VisualThemeEditorWidget().post(context);

      siteProperties.verify(() -> SitePropertyRepository.saveAll(anyString(),
          org.mockito.ArgumentMatchers.anyList()), never());
      verify(request).setAttribute("uploadedThemeName", "Ocean");
      verify(request).setAttribute("propertyDraftValues",
          Map.of("theme.body.backgroundColor", "#0077b6"));
      verify(context).setSuccessMessage("Theme colors were uploaded. Review the preview and save to apply them.");
    }
  }

  private static SiteProperty property(String name, String value) {
    SiteProperty property = new SiteProperty();
    property.setId(1);
    property.setName(name);
    property.setLabel(name);
    property.setType("color");
    property.setValue(value);
    return property;
  }
}
