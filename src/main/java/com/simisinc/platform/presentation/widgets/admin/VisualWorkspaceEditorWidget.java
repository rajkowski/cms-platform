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

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.jsp.jstl.core.Config;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.VisualWorkspaceEditorCommand;
import com.simisinc.platform.application.admin.VisualWorkspacePropertyCatalog;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.presentation.controller.SqlTimestampConverter;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.infrastructure.permission.Permission;

/**
 * Presents and updates the active workspace's visual theme and safe site properties.
 */
public class VisualWorkspaceEditorWidget extends GenericWidget {

  private static final long serialVersionUID = 1L;
  private static final Log LOG = LogFactory.getLog(VisualWorkspaceEditorWidget.class);
  private static final String ACCESS_PERMISSION = "cms.visual-workspace-editor.access";
  private static final String EDITOR_JSP = "/cms/visual-workspace-editor.jsp";
  private static final Set<String> REQUEST_METADATA = Set.of("widget", "token", "action", "returnPage");

  @Override
  public WidgetContext execute(WidgetContext context) {
    if (!hasAccess(context)) {
      return context;
    }
    renderEditor(context, null, Map.of());
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

    VisualWorkspaceEditorCommand.SaveResult result = VisualWorkspaceEditorCommand.saveValues(submittedValues, "site.");
    if (result.isSuccessful()) {
      updateTimeZone(context, submittedValues.get("site.timezone"));
      context.setSuccessMessage("Values were saved.");
      renderEditor(context, null, Map.of());
    } else {
      LOG.warn("Visual workspace editor values were not saved for user " + context.getUserId());
      context.setErrorMessage("Some values could not be saved. Review the highlighted settings and try again.");
      renderEditor(context, submittedValues, result.getErrors());
    }
    return context;
  }

  private void updateTimeZone(WidgetContext context, String timezone) {
    if (StringUtils.isBlank(timezone)) {
      return;
    }
    Config.set(context.getServletContext(), Config.FMT_TIME_ZONE, timezone);
    SqlTimestampConverter converter = (SqlTimestampConverter) ConvertUtils.lookup(Timestamp.class);
    converter.setTimeZone(TimeZone.getTimeZone(ZoneId.of(timezone)));
    ConvertUtils.register(converter, Timestamp.class);
  }

  private boolean hasAccess(WidgetContext context) {
    if (Permission.check(ACCESS_PERMISSION, context.getUserSession())) {
      return true;
    }
    LOG.debug("No permission to: " + VisualWorkspaceEditorWidget.class.getSimpleName());
    return false;
  }

  private void renderEditor(WidgetContext context, Map<String, String> drafts, Map<String, String> errors) {
    Map<String, List<SiteProperty>> sitePropertyGroups = new LinkedHashMap<>();
    Map<String, String> propertyDraftValues = drafts == null ? Map.of() : drafts;

    for (SiteProperty property : VisualWorkspaceEditorCommand.loadEditableProperties()) {
      if (property.getName().startsWith("site.")) {
        String category = VisualWorkspacePropertyCatalog.getCategory(property);
        sitePropertyGroups.computeIfAbsent(category, key -> new ArrayList<>()).add(property);
      }
    }

    context.getRequest().setAttribute("sitePropertyGroups", sitePropertyGroups);
    context.getRequest().setAttribute("propertyDraftValues", propertyDraftValues);
    context.getRequest().setAttribute("propertyErrors", errors);
    context.getRequest().setAttribute("editorReturnPage", "/admin");
    context.setJsp(EDITOR_JSP);
  }
}
