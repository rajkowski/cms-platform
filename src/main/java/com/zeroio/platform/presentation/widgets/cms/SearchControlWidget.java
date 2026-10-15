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

package com.zeroio.platform.presentation.widgets.cms;

import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * A control for searching the site
 *
 * @author matt rajkowski
 * @created 10/11/2026 10:11 PM
 */
public class SearchControlWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/cms/search-control.jsp";

  public WidgetContext execute(WidgetContext context) {
    context.getRequest().setAttribute("placeholder", context.getPreferences().getOrDefault("placeholder", "Search..."));
    context.getRequest().setAttribute("searchMinChars", validateInteger(context, "minChars", 3, 1, 100));
    context.getRequest().setAttribute("searchDebounceMs", validateInteger(context, "debounceMs", 300, 0, 5000));
    context.getRequest().setAttribute("searchInstanceId", context.getUniqueId());
    context.setJsp(JSP);
    return context;
  }

  private static int validateInteger(WidgetContext context, String preference, int defaultValue, int minimum, int maximum) {
    String value = context.getPreferences().get(preference);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      int parsedValue = Integer.parseInt(value.trim());
      if (parsedValue >= minimum && parsedValue <= maximum) {
        return parsedValue;
      }
    } catch (NumberFormatException exception) {
      LOG.debug("Invalid numeric search preference: " + preference);
    }
    LOG.warn("Invalid search " + preference + "; expected " + minimum + "-" + maximum + "; using " + defaultValue);
    return defaultValue;
  }
}
