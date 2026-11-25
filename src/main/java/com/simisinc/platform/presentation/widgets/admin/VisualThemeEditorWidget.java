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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.VisualWorkspaceEditorCommand;
import com.simisinc.platform.application.admin.VisualWorkspacePropertyCatalog;
import com.simisinc.platform.application.admin.VisualThemePresetCatalog;
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
  private static final Set<String> REQUEST_METADATA = Set.of("widget", "token", "action", "themeSnapshotBaseName");

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

    Map<String, String> submittedValues = new LinkedHashMap<>();
    for (Map.Entry<String, String[]> entry : context.getParameterMap().entrySet()) {
      if (!REQUEST_METADATA.contains(entry.getKey())) {
        String[] values = entry.getValue();
        submittedValues.put(entry.getKey(), values == null || values.length == 0 ? "" : values[0]);
      }
    }

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

  private static String sanitizeSnapshotName(String requestedName) {
    String name = StringUtils.trimToEmpty(requestedName)
        .replaceAll("[\\p{Cntrl}]", " ")
        .trim();
    return StringUtils.abbreviate(name, 255);
  }
}
