/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.domain.model.cms.WebSearch;
import com.simisinc.platform.infrastructure.persistence.cms.WebSearchRepository;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.cms.SearchCriteria;

/**
 * Displays what the user searched for
 *
 * @author matt rajkowski
 * @created 4/20/18 2:23 PM
 */
public class SearchInfoWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/cms/search-info.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine the search criteria
    SearchCriteria searchCriteria = new SearchCriteria(context.getParameterMap());
    context.getRequest().setAttribute("searchCriteria", searchCriteria);

    // Track the search terms
    if (StringUtils.isNotBlank(searchCriteria.getQuery()) &&
        (!context.getUserSession().isLoggedIn() || !(context.hasRole("admin") || context.hasRole("content-manager")))) {

      // Determine where the query came from
      String pagePath = (String) context.getRequest().getAttribute(RequestConstants.WEB_PAGE_PATH);

      WebSearch webSearch = new WebSearch();
      webSearch.setPagePath(pagePath);
      webSearch.setQuery(searchCriteria.getQuery());
      webSearch.setIpAddress(context.getRequest().getRemoteAddr());
      webSearch.setSessionId(context.getUserSession().getSessionId());
      webSearch.setIsLoggedIn(context.getUserSession().isLoggedIn());
      WebSearchRepository.save(webSearch);
    }

    // Show the JSP
    context.setJsp(JSP);
    return context;
  }
}
