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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.domain.model.cms.Theme;

class VisualWorkspacePropertyCatalogTest {

  @Test
  void allowsSafeSitePropertiesAndSupportedThemeColors() {
    Assertions.assertTrue(VisualWorkspacePropertyCatalog.isEditableProperty(property("site.name", "text")));
    Assertions.assertTrue(VisualWorkspacePropertyCatalog.isEditableProperty(property("site.logo", "image")));
    Assertions.assertTrue(VisualWorkspacePropertyCatalog.isEditableProperty(property("theme.body.backgroundColor", "color")));
    Assertions.assertFalse(VisualWorkspacePropertyCatalog.isEditableProperty(property("site.url", "url")));
    Assertions.assertFalse(VisualWorkspacePropertyCatalog.isEditableProperty(property("site.api", "boolean")));
    Assertions.assertFalse(VisualWorkspacePropertyCatalog.isEditableProperty(property("mail.apiKey", "text")));
  }

  @Test
  void mapsThemeColorsOnlyToSupportedPreviewTargets() {
    Assertions.assertEquals("body", VisualWorkspacePropertyCatalog.getPreviewTarget("theme.body.text.color"));
    Assertions.assertEquals("topbar", VisualWorkspacePropertyCatalog.getPreviewTarget("theme.topbar.dark.backgroundColor"));
    Assertions.assertEquals("button", VisualWorkspacePropertyCatalog.getPreviewTarget("theme.button.primary.backgroundColor"));
    Assertions.assertNull(VisualWorkspacePropertyCatalog.getPreviewTarget("theme.fonts.body"));
  }

  @Test
  void includesEveryLegacyAndRenderedThemeColorChoice() {
    String[] propertyNames = {
        "theme.body.text.color",
        "theme.body.link.color",
        "theme.body.backgroundColor",
        "theme.utilitybar.text.color",
        "theme.utilitybar.link.color",
        "theme.utilitybar.backgroundColor",
        "theme.topbar.text.color",
        "theme.topbar.backgroundColor",
        "theme.topbar.menu.text.color",
        "theme.topbar.menu.arrow.color",
        "theme.topbar.menu.text.hoverBackgroundColor",
        "theme.topbar.menu.hoverTextColor",
        "theme.topbar.menu.dropdown.backgroundColor",
        "theme.topbar.menu.dropdown.text.color",
        "theme.topbar.menu.activeBackgroundColor",
        "theme.topbar.menu.activeTextColor",
        "theme.topbar.dark.backgroundColor",
        "theme.topbar.dark.text.color",
        "theme.topbar.dark.menu.text.color",
        "theme.topbar.dark.menu.arrow.color",
        "theme.topbar.dark.menu.hoverTextColor",
        "theme.topbar.dark.menu.text.hoverBackgroundColor",
        "theme.topbar.dark.menu.activeTextColor",
        "theme.topbar.dark.menu.activeBackgroundColor",
        "theme.topbar.dark.menu.dropdown.backgroundColor",
        "theme.topbar.dark.menu.dropdown.text.color",
        "theme.button.text.color",
        "theme.button.default.text.color",
        "theme.button.default.backgroundColor",
        "theme.button.default.hoverBackgroundColor",
        "theme.button.primary.text.color",
        "theme.button.primary.backgroundColor",
        "theme.button.primary.hoverBackgroundColor",
        "theme.button.secondary.text.color",
        "theme.button.secondary.backgroundColor",
        "theme.button.secondary.hoverBackgroundColor",
        "theme.button.success.text.color",
        "theme.button.success.backgroundColor",
        "theme.button.success.hoverBackgroundColor",
        "theme.button.warning.text.color",
        "theme.button.warning.backgroundColor",
        "theme.button.warning.hoverBackgroundColor",
        "theme.button.alert.text.color",
        "theme.button.alert.backgroundColor",
        "theme.button.alert.hoverBackgroundColor",
        "theme.callout.backgroundColor",
        "theme.callout.text.color",
        "theme.callout.primary.backgroundColor",
        "theme.callout.primary.text.color",
        "theme.callout.secondary.backgroundColor",
        "theme.callout.secondary.text.color",
        "theme.callout.success.backgroundColor",
        "theme.callout.success.text.color",
        "theme.callout.warning.backgroundColor",
        "theme.callout.warning.text.color",
        "theme.callout.alert.backgroundColor",
        "theme.callout.alert.text.color",
        "theme.footer.backgroundColor",
        "theme.footer.text.color",
        "theme.footer.links.color"
    };

    for (String propertyName : propertyNames) {
      Assertions.assertTrue(VisualWorkspacePropertyCatalog.isEditableProperty(property(propertyName, "color")), propertyName);
      Assertions.assertNotNull(VisualWorkspacePropertyCatalog.getPreviewTarget(propertyName), propertyName);
    }
  }

  @Test
  void providesThirtyCompleteIncludedThemes() {
    List<SiteProperty> properties = List.of(
        property("theme.body.backgroundColor", "color"),
        property("theme.body.link.color", "color"),
        property("theme.utilitybar.backgroundColor", "color"),
        property("theme.topbar.dark.backgroundColor", "color"),
        property("theme.button.default.text.color", "color"),
        property("theme.button.primary.text.color", "color"),
        property("theme.button.primary.backgroundColor", "color"),
        property("theme.button.secondary.text.color", "color"),
        property("theme.button.success.text.color", "color"),
        property("theme.button.warning.text.color", "color"),
        property("theme.button.alert.text.color", "color"),
        property("theme.callout.alert.backgroundColor", "color"),
        property("theme.footer.links.color", "color"));

    List<Theme> themes = VisualThemePresetCatalog.loadIncludedThemes(properties);

    Assertions.assertEquals(30, themes.size());
    for (Theme theme : themes) {
      Assertions.assertEquals(properties.size(), theme.getSiteProperties().size());
      Assertions.assertTrue(theme.getSiteProperties().stream()
          .allMatch(themeProperty -> themeProperty.getValue().matches("#[0-9a-fA-F]{6}")));
      Assertions.assertEquals(
          propertyValue(theme, "theme.button.primary.backgroundColor"),
          propertyValue(theme, "theme.body.link.color"));
      Assertions.assertTrue(theme.getSiteProperties().stream()
          .filter(themeProperty -> themeProperty.getName().startsWith("theme.button."))
          .filter(themeProperty -> themeProperty.getName().endsWith(".text.color"))
          .allMatch(themeProperty -> "#ffffff".equals(themeProperty.getValue())));
    }
  }

  @Test
  void exportsAndImportsThemeColors() throws IOException {
    List<SiteProperty> properties = List.of(
        property("theme.body.backgroundColor", "color"),
        property("theme.body.link.color", "color"));

    byte[] contents = VisualThemePresetCatalog.createThemeFile("Ocean", Map.of(
        "theme.body.backgroundColor", "#F8FBFF",
        "theme.body.link.color", "#0077B6"));
    Theme imported = VisualThemePresetCatalog.loadThemeFile(
        new ByteArrayInputStream(contents), properties);

    Assertions.assertEquals("Ocean", imported.getName());
    Assertions.assertEquals("#f8fbff",
        propertyValue(imported, "theme.body.backgroundColor"));
    Assertions.assertEquals("#0077b6",
        propertyValue(imported, "theme.body.link.color"));
  }

  @Test
  void rejectsUnsupportedAndInvalidUploadedColors() {
    String unsupportedColor = """
        {
          "format": "cms-platform-theme-colors",
          "version": 1,
          "name": "Unsafe",
          "colors": {
            "site.url": "#ffffff"
          }
        }
        """;
    String invalidColor = """
        {
          "format": "cms-platform-theme-colors",
          "version": 1,
          "name": "Invalid",
          "colors": {
            "theme.body.backgroundColor": "white"
          }
        }
        """;
    List<SiteProperty> properties = List.of(property("theme.body.backgroundColor", "color"));

    Assertions.assertThrows(IOException.class, () -> VisualThemePresetCatalog.loadThemeFile(
        new ByteArrayInputStream(unsupportedColor.getBytes(StandardCharsets.UTF_8)), properties));
    Assertions.assertThrows(IOException.class, () -> VisualThemePresetCatalog.loadThemeFile(
        new ByteArrayInputStream(invalidColor.getBytes(StandardCharsets.UTF_8)), properties));
  }

  private static String propertyValue(Theme theme, String propertyName) {
    return theme.getSiteProperties().stream()
        .filter(property -> propertyName.equals(property.getName()))
        .map(SiteProperty::getValue)
        .findFirst()
        .orElse(null);
  }

  private static SiteProperty property(String name, String type) {
    SiteProperty property = new SiteProperty();
    property.setName(name);
    property.setType(type);
    return property;
  }
}
