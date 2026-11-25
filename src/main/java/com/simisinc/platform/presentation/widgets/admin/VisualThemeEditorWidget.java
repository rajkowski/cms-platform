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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.VisualWorkspaceEditorCommand;
import com.simisinc.platform.application.admin.VisualWorkspacePropertyCatalog;
import com.simisinc.platform.application.admin.VisualThemePresetCatalog;
import com.simisinc.platform.application.admin.ValidateSitePropertyCommand;
import com.simisinc.platform.application.cms.ThemeCommand;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.domain.model.cms.Theme;
import com.simisinc.platform.infrastructure.persistence.cms.ThemeRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.infrastructure.permission.Permission;

/**
 * Presents and updates the active workspace's visual theme colors.
 */
public class VisualThemeEditorWidget extends GenericWidget {

  private static final long serialVersionUID = 1L;
  private static final Log LOG = LogFactory.getLog(VisualThemeEditorWidget.class);
  private static final String ACCESS_PERMISSION = "cms.visual-workspace-editor.access";
  private static final String EDITOR_JSP = "/cms/visual-theme-editor.jsp";
  private static final Set<String> REQUEST_METADATA = Set.of("widget", "token", "action", "command", "themeSnapshotBaseName");
  private static final long MAX_THEME_FILE_SIZE = 1024 * 1024;

  @Override
  public WidgetContext execute(WidgetContext context) {
    if (!hasAccess(context)) {
      return context;
    }
    renderEditor(context, Map.of(), Map.of());
    return context;
  }

  @Override
  public WidgetContext post(WidgetContext context) {
    if (!hasAccess(context)) {
      return context;
    }

    String command = context.getParameter("command");
    if ("downloadThemeColors".equals(command)) {
      return downloadThemeColors(context);
    }
    if ("uploadThemeColors".equals(command)) {
      return uploadThemeColors(context);
    }

    Map<String, String> submittedValues = collectSubmittedValues(context);

    String snapshotName = sanitizeSnapshotName(context.getParameter("themeSnapshotBaseName"));
    if (StringUtils.isBlank(snapshotName)) {
      context.setErrorMessage("Enter a name for the saved theme.");
      renderEditor(context, submittedValues, Map.of());
      return context;
    }

    VisualWorkspaceEditorCommand.SaveResult result = VisualWorkspaceEditorCommand.saveValues(submittedValues, "theme.");
    if (result.isSuccessful()) {
      Theme snapshot = ThemeCommand.createSnapshotWithName(snapshotName);
      if (snapshot != null) {
        context.setSuccessMessage("Theme colors were saved and added to history.");
      } else {
        LOG.error("The saved visual theme could not be added to theme history for user " + context.getUserId());
        context.setSuccessMessage("Theme colors were saved.");
        context.setWarningMessage("The theme could not be added to history.");
      }
      renderEditor(context, Map.of(), Map.of());
    } else {
      LOG.warn("Visual theme editor values were not saved for user " + context.getUserId());
      context.setErrorMessage("Some colors could not be saved. Review the highlighted settings and try again.");
      renderEditor(context, submittedValues, result.getErrors());
    }
    return context;
  }

  private WidgetContext downloadThemeColors(WidgetContext context) {
    Map<String, String> submittedValues = collectSubmittedValues(context);
    List<SiteProperty> themeProperties = loadThemeProperties();
    Map<String, String> colors = new LinkedHashMap<>();
    Map<String, String> errors = new LinkedHashMap<>();
    for (SiteProperty property : themeProperties) {
      String value = submittedValues.getOrDefault(property.getName(), property.getValue());
      if (!ValidateSitePropertyCommand.isValid(property, value)
          || !value.matches("#[0-9a-fA-F]{6}")) {
        errors.put(property.getName(), "Enter a valid color before downloading.");
        continue;
      }
      colors.put(property.getName(), value);
    }
    if (!errors.isEmpty()) {
      context.setErrorMessage("Correct the invalid colors before downloading the theme.");
      renderEditor(context, submittedValues, errors);
      return context;
    }

    String themeName = sanitizeSnapshotName(context.getParameter("themeSnapshotBaseName"));
    if (StringUtils.isBlank(themeName)) {
      themeName = "Custom Theme";
    }
    try {
      byte[] contents = VisualThemePresetCatalog.createThemeFile(themeName, colors);
      context.getResponse().reset();
      context.getResponse().setContentType("application/json");
      context.getResponse().setCharacterEncoding("UTF-8");
      context.getResponse().setContentLength(contents.length);
      context.getResponse().setHeader("Content-Disposition",
          "attachment; filename=\"" + createThemeFilename(themeName) + "\"");
      context.getResponse().getOutputStream().write(contents);
      context.setHandledResponse(true);
    } catch (IOException exception) {
      LOG.error("Theme colors could not be downloaded for user " + context.getUserId(), exception);
      context.setErrorMessage("The theme colors could not be downloaded.");
      renderEditor(context, submittedValues, Map.of());
    }
    return context;
  }

  private WidgetContext uploadThemeColors(WidgetContext context) {
    try {
      Part part = context.getPart("file");
      if (part == null || StringUtils.isBlank(part.getSubmittedFileName())) {
        throw new IOException("Choose a theme colors file to upload");
      }
      String submittedFilename = part.getSubmittedFileName();
      if (!submittedFilename.toLowerCase(Locale.ROOT).endsWith(".json")) {
        throw new IOException("The uploaded theme colors file must be a JSON file");
      }
      if (part.getSize() <= 0 || part.getSize() > MAX_THEME_FILE_SIZE) {
        throw new IOException("The uploaded theme colors file must be between 1 byte and 1 MB");
      }
      Theme uploadedTheme = VisualThemePresetCatalog.loadThemeFile(part.getInputStream(), loadThemeProperties());
      Map<String, String> drafts = new LinkedHashMap<>();
      for (SiteProperty property : uploadedTheme.getSiteProperties()) {
        drafts.put(property.getName(), property.getValue());
      }
      context.getRequest().setAttribute("uploadedThemeName", uploadedTheme.getName());
      context.setSuccessMessage("Theme colors were uploaded. Review the preview and save to apply them.");
      renderEditor(context, drafts, Map.of());
    } catch (IOException | ServletException exception) {
      LOG.warn("Theme colors file was rejected for user " + context.getUserId() + ": " + exception.getMessage());
      context.setErrorMessage(StringUtils.defaultIfBlank(exception.getMessage(),
          "The theme colors file could not be uploaded"));
      renderEditor(context, Map.of(), Map.of());
    }
    return context;
  }

  private boolean hasAccess(WidgetContext context) {
    if (Permission.check(ACCESS_PERMISSION, context.getUserSession())) {
      return true;
    }
    LOG.debug("No permission to: " + VisualThemeEditorWidget.class.getSimpleName());
    return false;
  }

  private void renderEditor(WidgetContext context, Map<String, String> drafts, Map<String, String> errors) {
    List<SiteProperty> themeProperties = new ArrayList<>();
    Map<String, String> themePreviewTargets = new LinkedHashMap<>();

    for (SiteProperty property : VisualWorkspaceEditorCommand.loadEditableProperties()) {
      if (property.getName().startsWith("theme.")) {
        themeProperties.add(property);
        themePreviewTargets.put(property.getName(), VisualWorkspacePropertyCatalog.getPreviewTarget(property.getName()));
      }
    }

    context.getRequest().setAttribute("themePropertyList", themeProperties);
    context.getRequest().setAttribute("themePreviewTargets", themePreviewTargets);
    context.getRequest().setAttribute("includedThemeList", VisualThemePresetCatalog.loadIncludedThemes(themeProperties));
    List<Theme> savedThemes = ThemeRepository.findAll();
    List<Theme> themeHistoryList = new ArrayList<>();
    if (savedThemes != null) {
      themeHistoryList.addAll(savedThemes);
      Collections.reverse(themeHistoryList);
    }
    context.getRequest().setAttribute("themeHistoryList", themeHistoryList);
    context.getRequest().setAttribute("propertyDraftValues", drafts);
    context.getRequest().setAttribute("propertyErrors", errors);
    context.setJsp(EDITOR_JSP);
  }

  private List<SiteProperty> loadThemeProperties() {
    return VisualWorkspaceEditorCommand.loadEditableProperties().stream()
        .filter(property -> property.getName().startsWith("theme."))
        .toList();
  }

  private static Map<String, String> collectSubmittedValues(WidgetContext context) {
    Map<String, String> submittedValues = new LinkedHashMap<>();
    for (Map.Entry<String, String[]> entry : context.getParameterMap().entrySet()) {
      if (!REQUEST_METADATA.contains(entry.getKey())) {
        String[] values = entry.getValue();
        submittedValues.put(entry.getKey(), values == null || values.length == 0 ? "" : values[0]);
      }
    }
    return submittedValues;
  }

  private static String createThemeFilename(String themeName) {
    String filename = themeName.replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("^-+|-+$", "");
    return (StringUtils.isBlank(filename) ? "custom-theme" : filename) + "-colors.json";
  }

  private static String sanitizeSnapshotName(String requestedName) {
    String name = StringUtils.trimToEmpty(requestedName)
        .replaceAll("[\\p{Cntrl}]", " ")
        .trim();
    return StringUtils.abbreviate(name, 255);
  }
}
