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

package com.simisinc.platform.presentation.widgets.editor;

import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Schedules a dataset for the visual data editor
 *
 * @author matt rajkowski
 * @created 01/25/26 5:00 PM
 */
public class DatasetScheduleAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908900L;
  private static Log LOG = LogFactory.getLog(DatasetScheduleAjax.class);

  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DatasetScheduleAjax...");

    // Check permissions
    if (!context.hasRole("admin") && !context.hasRole("data-manager")) {
      LOG.debug("No permission to schedule dataset");
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Stub implementation - to be completed
    context.setJson("{\"success\":true,\"message\":\"Dataset scheduling not yet implemented\"}");
    return context;
  }
}
