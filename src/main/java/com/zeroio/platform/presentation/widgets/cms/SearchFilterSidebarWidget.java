/*
 * Copyright 2026 Matt Rajkowski
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
import com.zeroio.platform.domain.model.Region;
import com.zeroio.platform.infrastructure.persistence.RegionRepository;

/**
 * Displays search filters in a sidebar
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class SearchFilterSidebarWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908894L;

  static String JSP = "/cms/search-filter-sidebar.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Get the filter options from the widget preferences
    boolean useRegions = Boolean.parseBoolean(context.getPreferences().getOrDefault("useRegions", "false"));
    context.getRequest().setAttribute("useRegions", useRegions ? "true" : "false");
    context.getRequest().setAttribute("useTags", context.getPreferences().getOrDefault("useTags", "false"));
    context.getRequest().setAttribute("useTypes", context.getPreferences().getOrDefault("useTypes", "false"));
    context.getRequest().setAttribute("useLastModified", context.getPreferences().getOrDefault("useLastModified", "false"));
    context.getRequest().setAttribute("useContributors", context.getPreferences().getOrDefault("useContributors", "false"));

    // Retrieve custom filter options from the widget preferences
    if (useRegions) {
      // Display the user's selected region name
      String selectedRegionCode = context.getUserSession().getSelectedRegionCode();
      Region region = RegionRepository.findByCode(selectedRegionCode);
      if (region != null) {
        context.getRequest().setAttribute("region", region);
      }
    }

    // Make filter parameters available to the JSP
    context.getRequest().setAttribute("query", context.getParameter("query"));
    context.getRequest().setAttribute("labelFilter", context.getParameter("label"));
    context.getRequest().setAttribute("modifiedAfter", context.getParameter("modifiedAfter", ""));
    context.getRequest().setAttribute("modifiedBefore", context.getParameter("modifiedBefore", ""));
    context.getRequest().setAttribute("dateFilterType", context.getParameter("dateFilterType"));
    context.getRequest().setAttribute("contributorFilter", context.getParameter("contributorFilter"));
    context.getRequest().setAttribute("ofType", context.getParameter("ofType"));

    // Show the JSP
    context.setJsp(JSP);
    return context;
  }
}
