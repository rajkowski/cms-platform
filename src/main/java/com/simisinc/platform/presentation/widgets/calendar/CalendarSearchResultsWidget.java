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

package com.simisinc.platform.presentation.widgets.calendar;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.domain.model.cms.CalendarEvent;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarEventRepository;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarEventSpecification;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.cms.SearchCriteria;

/**
 * Calendar Search Results Widget
 *
 * @author matt rajkowski
 * @created 8/28/19 11:31 AM
 */
public class CalendarSearchResultsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/calendar/calendar-search-results.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine the search criteria
    SearchCriteria searchCriteria = new SearchCriteria(context.getParameterMap());

    // Check the 'ofType' filter - only show resources when filter is 'calendar', 'all', or empty
    String isOfType = Objects.toString(searchCriteria.getOfType(), SearchCriteria.ALL);
    if (!SearchCriteria.ALL.equals(isOfType) && !SearchCriteria.CALENDAR.equals(isOfType)) {
      // User has selected a different content type filter (e.g., 'resources')
      return null;
    }

    // Check the 'showWhenOfType' preference
    String showWhenOfType = context.getPreferences().getOrDefault("showWhenOfType", SearchCriteria.ALL);
    if (!showWhenOfType.equals(isOfType)) {
      // Widget is configured to show a different content type than the current search criteria
      return null;
    }

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Determine the record paging
    int limit = Integer.parseInt(context.getPreferences().getOrDefault("limit", "3"));
    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("items", limit);
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);
    context.getRequest().setAttribute(RequestConstants.RECORD_PAGING, constraints);

    // Determine the search term
    String query = searchCriteria.getQuery();
    if (StringUtils.isBlank(query)) {
      return null;
    }

    // Determine the query date range (from today on... using the site's timezone start of day)
    ZoneId clientZoneId = ZoneId.of(LoadSitePropertyCommand.loadByName("site.timezone"));
    Instant instant = Instant.now();
    ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, clientZoneId);
    ZonedDateTime zdtStart = zdt.toLocalDate().atStartOfDay(clientZoneId);

    // Search the calendar events
    CalendarEventSpecification eventSpecification = new CalendarEventSpecification();
    eventSpecification.setSearchTerm(query);
    eventSpecification.setStartingDateRange(Timestamp.valueOf(zdtStart.toLocalDateTime()));
    List<CalendarEvent> calendarEventList = CalendarEventRepository.findAll(eventSpecification, constraints);

    // Determine if the widget is shown
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "true"));
    if (calendarEventList.isEmpty() && !showWhenEmpty) {
      return context;
    }

    context.getRequest().setAttribute("calendarEventList", calendarEventList);
    context.setJsp(JSP);
    return context;
  }
}
