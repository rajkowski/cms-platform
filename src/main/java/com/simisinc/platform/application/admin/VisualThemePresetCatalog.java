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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.domain.model.cms.Theme;

/**
 * Provides built-in color combinations for the visual theme editor.
 */
public class VisualThemePresetCatalog {

  public static final String FILE_FORMAT = "cms-platform-theme-colors";
  public static final int FILE_VERSION = 1;
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  private static final List<Palette> PALETTES = List.of(
      new Palette("Classic Blue", "#ffffff", "#1f2937", "#1779ba", "#126195", "#d7ecfa", "#353535", "#ffffff"),
      new Palette("Ocean", "#f8fbff", "#17324d", "#0077b6", "#005f8f", "#d9f0ff", "#123b5d", "#ffffff"),
      new Palette("Forest", "#fbfdf9", "#1f3527", "#2f855a", "#246b47", "#def4e7", "#234d35", "#ffffff"),
      new Palette("Violet", "#fcfaff", "#2f2348", "#7c3aed", "#6428ca", "#eee3ff", "#3f246b", "#ffffff"),
      new Palette("Sunset", "#fffaf5", "#44291b", "#e76f51", "#c95539", "#ffe4d6", "#61362a", "#ffffff"),
      new Palette("Rose", "#fff9fb", "#40232d", "#d53f70", "#b72f5c", "#fce0ea", "#5c253c", "#ffffff"),
      new Palette("Teal", "#f6fffd", "#173b3a", "#0f8b8d", "#0a6f70", "#d8f4f2", "#174f50", "#ffffff"),
      new Palette("Slate", "#f8fafc", "#1e293b", "#475569", "#334155", "#e2e8f0", "#293548", "#ffffff"),
      new Palette("Amber", "#fffcf5", "#422f12", "#d97706", "#b45309", "#fff0c2", "#5a3b12", "#ffffff"),
      new Palette("Midnight", "#111827", "#f3f4f6", "#60a5fa", "#3b82f6", "#1e3a5f", "#090f1c", "#e5e7eb"),
      new Palette("Cobalt", "#f7f9ff", "#1b2745", "#2563eb", "#1d4ed8", "#dbeafe", "#1e3a8a", "#ffffff"),
      new Palette("Mint", "#f7fffb", "#183d34", "#10a37f", "#087f63", "#d6f8eb", "#176b58", "#ffffff"),
      new Palette("Coral", "#fff9f7", "#4a2721", "#f05d5e", "#d74445", "#ffe1dc", "#71312b", "#ffffff"),
      new Palette("Lavender", "#fcfaff", "#35264b", "#8b5cf6", "#7041d6", "#eee7ff", "#503080", "#ffffff"),
      new Palette("Crimson", "#fff8f8", "#451d25", "#c81e3a", "#a5172f", "#ffe0e6", "#6d1b2d", "#ffffff"),
      new Palette("Citrus", "#fffef5", "#3d3b17", "#7c9a18", "#627b0e", "#eff5c8", "#4f6117", "#ffffff"),
      new Palette("Sand", "#fdfbf7", "#41382d", "#b7793d", "#945e2d", "#f3e6d5", "#5c4939", "#ffffff"),
      new Palette("Espresso", "#fbf7f3", "#35271f", "#8b5e3c", "#6f452a", "#eadccf", "#4a3428", "#ffffff"),
      new Palette("Arctic", "#f7fbfc", "#18343d", "#36a5c5", "#23839e", "#d9f2f8", "#225968", "#ffffff"),
      new Palette("Orchid", "#fdf8ff", "#41233f", "#b23aa7", "#8e2c85", "#f5dcf2", "#68245f", "#ffffff"),
      new Palette("Neon Grid", "#07111f", "#d9f4ff", "#00e5ff", "#00a9c4", "#0a3446", "#081827", "#e6fbff"),
      new Palette("Quantum Chrome", "#f3f7fa", "#18242f", "#6d5dfc", "#5141d9", "#e5e1ff", "#242d3d", "#ffffff"),
      new Palette("Blacklight", "#090711", "#f2e9ff", "#c026d3", "#8b1f9b", "#31143b", "#140a22", "#f8e8ff"),
      new Palette("Bubblegum Reactor", "#fff0f8", "#4f1738", "#ff4fb3", "#d92b8c", "#ffd2e9", "#681b49", "#ffffff"),
      new Palette("Pickle Signal", "#fbffe7", "#293b12", "#84cc16", "#5f970c", "#e9f7bd", "#36520f", "#ffffff"),
      new Palette("Cosmic Pudding", "#140d26", "#fff1ff", "#b44dff", "#8c2cce", "#40205c", "#24123b", "#fff5ff"),
      new Palette("Electric Koi", "#fff8ef", "#3f2418", "#ff6b00", "#cf4f00", "#ffe0bd", "#6b321a", "#ffffff"),
      new Palette("Moon Cheese", "#20272c", "#fffbe0", "#ffd84d", "#d5ac18", "#564d25", "#10171b", "#fffbe0"),
      new Palette("Disco Cactus", "#f7fff4", "#20351c", "#39d353", "#249a3a", "#d3f9d8", "#28562a", "#ffffff"),
      new Palette("Plasma Sherbet", "#fff5ee", "#4a243a", "#ff5c8a", "#c52e5b", "#ffcddd", "#632344", "#ffffff"));

  public static List<Theme> loadIncludedThemes(List<SiteProperty> properties) {
    List<Theme> themes = new ArrayList<>();
    for (Palette palette : PALETTES) {
      Theme theme = new Theme(palette.name);
      List<SiteProperty> themeProperties = new ArrayList<>();
      for (SiteProperty property : properties) {
        SiteProperty presetProperty = new SiteProperty();
        presetProperty.setName(property.getName());
        presetProperty.setValue(resolveColor(property.getName(), palette));
        themeProperties.add(presetProperty);
      }
      theme.setSiteProperties(themeProperties);
      themes.add(theme);
    }
    return themes;
  }

  public static byte[] createThemeFile(String name, Map<String, String> colors) throws IOException {
    if (colors == null || colors.isEmpty()) {
      throw new IOException("The theme file does not contain any colors");
    }
    ObjectNode root = MAPPER.createObjectNode();
    root.put("format", FILE_FORMAT);
    root.put("version", FILE_VERSION);
    root.put("name", StringUtils.trimToEmpty(name));
    ObjectNode colorsNode = root.putObject("colors");
    for (Map.Entry<String, String> color : colors.entrySet()) {
      if (!isThemeColor(color.getValue())) {
        throw new IOException("The theme contains an invalid color for " + color.getKey());
      }
      colorsNode.put(color.getKey(), color.getValue());
    }
    return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
  }

  public static Theme loadThemeFile(InputStream input, List<SiteProperty> editableProperties) throws IOException {
    JsonNode root = MAPPER.readTree(input);
    if (root == null || !root.isObject()) {
      throw new IOException("The uploaded theme file is not a valid JSON object");
    }
    if (!FILE_FORMAT.equals(root.path("format").asText()) || root.path("version").asInt() != FILE_VERSION) {
      throw new IOException("The uploaded file is not a supported theme colors file");
    }

    JsonNode colorsNode = root.get("colors");
    if (colorsNode == null || !colorsNode.isObject() || colorsNode.isEmpty()) {
      throw new IOException("The uploaded theme file does not contain any colors");
    }

    Map<String, SiteProperty> editableByName = new LinkedHashMap<>();
    for (SiteProperty property : editableProperties) {
      editableByName.put(property.getName(), property);
    }

    List<SiteProperty> colors = new ArrayList<>();
    java.util.Iterator<Map.Entry<String, JsonNode>> fields = colorsNode.properties().iterator();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String propertyName = field.getKey();
      String color = field.getValue().isTextual() ? field.getValue().textValue() : null;
      SiteProperty editableProperty = editableByName.get(propertyName);
      if (editableProperty == null) {
        throw new IOException("The uploaded theme contains an unsupported color: " + propertyName);
      }
      if (color == null || !isThemeColor(color)) {
        throw new IOException("The uploaded theme contains an invalid color for " + propertyName);
      }
      SiteProperty property = new SiteProperty();
      property.setName(propertyName);
      property.setValue(color.toLowerCase(Locale.ROOT));
      colors.add(property);
    }

    if (colors.isEmpty()) {
      throw new IOException("The uploaded theme file does not contain any colors");
    }
    String requestedName = StringUtils.trimToNull(
        StringUtils.trimToEmpty(root.path("name").asText(null)).replaceAll("[\\p{Cntrl}]", " "));
    Theme theme = new Theme(requestedName == null ? "Uploaded Theme" : StringUtils.abbreviate(requestedName, 255));
    theme.setSiteProperties(colors);
    return theme;
  }

  private static boolean isThemeColor(String value) {
    return value != null && value.matches("#[0-9a-fA-F]{6}");
  }

  private static String resolveColor(String propertyName, Palette palette) {
    String name = propertyName.toLowerCase(Locale.ROOT);
    if (name.equals("theme.body.backgroundcolor")) {
      return palette.bodyBackground;
    }
    if (name.equals("theme.body.text.color")) {
      return palette.bodyText;
    }
    if (name.equals("theme.body.link.color")) {
      return palette.accent;
    }
    if (name.startsWith("theme.utilitybar.")) {
      return name.endsWith("backgroundcolor") ? palette.accent : "#ffffff";
    }
    if (name.startsWith("theme.topbar.dark.")) {
      return resolveNavigationColor(name.substring("theme.topbar.dark.".length()), "#0b1220", "#edf2f7", "#253247");
    }
    if (name.startsWith("theme.topbar.")) {
      return resolveNavigationColor(name.substring("theme.topbar.".length()), palette.navigationBackground,
          palette.navigationText, palette.accentHover);
    }
    if (name.startsWith("theme.button.")) {
      if (name.equals("theme.button.text.color")) {
        return "#ffffff";
      }
      if (name.endsWith(".text.color")) {
        return "#ffffff";
      }
      if (name.contains(".primary.") || name.contains(".default.")) {
        return name.contains(".hover") ? palette.accentHover : palette.accent;
      }
      if (name.contains(".secondary.")) {
        return name.contains(".hover") ? "#475569" : "#64748b";
      }
      if (name.contains(".success.")) {
        return name.contains(".hover") ? "#24834a" : "#34a55f";
      }
      if (name.contains(".warning.")) {
        return name.contains(".hover") ? "#b76e00" : "#e89b00";
      }
      if (name.contains(".alert.")) {
        return name.contains(".hover") ? "#9f3128" : "#c44736";
      }
    }
    if (name.startsWith("theme.callout.")) {
      if (name.endsWith(".text.color") || name.equals("theme.callout.text.color")) {
        return "#17202a";
      }
      if (name.contains(".primary.")) {
        return palette.accentSoft;
      }
      if (name.contains(".secondary.")) {
        return "#edf1f5";
      }
      if (name.contains(".success.")) {
        return "#dcf7e7";
      }
      if (name.contains(".warning.")) {
        return "#fff0c7";
      }
      if (name.contains(".alert.")) {
        return "#f9dfdc";
      }
      return palette.accentSoft;
    }
    if (name.startsWith("theme.pagination.")) {
      if (name.endsWith(".text.color")) {
        return name.contains(".current.") ? "#ffffff" : palette.accent;
      }
      if (name.contains(".current.")) {
        return palette.accent;
      }
      return palette.accentSoft;
    }
    if (name.startsWith("theme.label.") || name.startsWith("theme.badge.")) {
      if (name.endsWith(".text.color")) {
        return name.contains(".badge.") && name.equals("theme.badge.text.color") ? "#000000" : "#ffffff";
      }
      return resolveSemanticBackgroundColor(name, palette);
    }
    if (name.startsWith("theme.progress.")) {
      if (name.endsWith(".text.color")) {
        return "#ffffff";
      }
      if (name.equals("theme.progress.backgroundcolor")) {
        return "#e6e6e6";
      }
      return resolveSemanticBackgroundColor(name, palette);
    }
    if (name.startsWith("theme.footer.")) {
      return name.endsWith("links.color") ? palette.accentSoft
          : name.endsWith("text.color") ? palette.navigationText : palette.navigationBackground;
    }
    return palette.accent;
  }

  private static String resolveSemanticBackgroundColor(String name, Palette palette) {
    if (name.contains(".secondary.")) {
      return "#64748b";
    }
    if (name.contains(".success.")) {
      return "#34a55f";
    }
    if (name.contains(".warning.")) {
      return "#e89b00";
    }
    if (name.contains(".alert.")) {
      return "#c44736";
    }
    if (name.startsWith("theme.badge.") && !name.contains(".primary.")) {
      return "#dfe1e6";
    }
    return palette.accent;
  }

  private static String resolveNavigationColor(String name, String background, String text, String hover) {
    if (name.equals("backgroundcolor") || name.equals("menu.dropdown.backgroundcolor")) {
      return background;
    }
    if (name.contains("hoverbackgroundcolor") || name.contains("activebackgroundcolor")) {
      return hover;
    }
    return text;
  }

  private static class Palette {
    private final String name;
    private final String bodyBackground;
    private final String bodyText;
    private final String accent;
    private final String accentHover;
    private final String accentSoft;
    private final String navigationBackground;
    private final String navigationText;

    private Palette(String name, String bodyBackground, String bodyText, String accent, String accentHover,
        String accentSoft, String navigationBackground, String navigationText) {
      this.name = name;
      this.bodyBackground = bodyBackground;
      this.bodyText = bodyText;
      this.accent = accent;
      this.accentHover = accentHover;
      this.accentSoft = accentSoft;
      this.navigationBackground = navigationBackground;
      this.navigationText = navigationText;
    }
  }
}
