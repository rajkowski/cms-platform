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

package com.simisinc.platform.presentation.widgets.cms;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Visual Analytics Dashboard Widget for site usage and visitor analytics
 *
 * Provides a real-time and historical analytics surface for admins and editors
 * with KPI cards, trend charts, live sessions, content analytics, audience insights,
 * and technical metrics.
 *
 * @author matt rajkowski
 * @created 01/31/26 02:00 PM
 */
public class VisualAnalyticsDashboardWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(VisualAnalyticsDashboardWidget.class);

  static String DASHBOARD_JSP = "/cms/visual-analytics-dashboard.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {

    // Check if user has admin permissions to access analytics
    if (!context.getUserSession().hasRole("admin") && !context.getUserSession().hasRole("content-manager")) {
      context.setErrorMessage("You do not have permission to access the analytics dashboard");
      return context;
    }

    // Set the JSP
    context.setJsp(DASHBOARD_JSP);

    // Set initial configuration
    context.getRequest().setAttribute("allowedRoles", "admin,content-manager,community-manager");
    context.getRequest().setAttribute("defaultTimeRange", "7d"); // Last 7 days
    context.getRequest().setAttribute("liveEnabled", true);
    context.getRequest().setAttribute("technicalMetricsEnabled", true);

    return context;
  }
}
