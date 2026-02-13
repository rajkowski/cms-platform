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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.CalendarEvent;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarEventRepository;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarEventSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Handles JSON/AJAX GET requests for /json/calendars/events endpoint
 * Returns calendar events for selected calendars
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class CalendarsEventsJsonService extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(CalendarsEventsJsonService.class);

  /**
   * Handles GET requests for calendar events
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public WidgetContext execute(WidgetContext context) {

    // Check permissions - require admin or content-manager role
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      // Return empty array on permission denied for FullCalendar
      context.setJson("[]");
      return context;
    }

    try {
      // Get comma-separated calendar IDs
      String calendarIdsParam = context.getParameter("calendarIds");
      
      if (StringUtils.isBlank(calendarIdsParam)) {
        // Return empty array for FullCalendar
        context.setJson("[]");
        return context;
      }

      // Parse calendar IDs
      String[] calendarIdStrings = calendarIdsParam.split(",");
      long[] calendarIds = new long[calendarIdStrings.length];
      for (int i = 0; i < calendarIdStrings.length; i++) {
        try {
          calendarIds[i] = Long.parseLong(calendarIdStrings[i].trim());
        } catch (NumberFormatException e) {
          // Skip invalid IDs
        }
      }

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("[");

      boolean first = true;
      for (long calendarId : calendarIds) {
        if (calendarId <= 0) continue;
        
        // Load events for this calendar using CalendarEventSpecification
        CalendarEventSpecification spec = new CalendarEventSpecification();
        spec.setCalendarId(calendarId);
        List<CalendarEvent> events = CalendarEventRepository.findAll(spec, null);
        
        if (events != null && !events.isEmpty()) {
          for (CalendarEvent event : events) {
            if (!first) {
              json.append(",");
            }
            first = false;

            json.append("{");
            json.append("\"id\": ").append(event.getId()).append(",");
            json.append("\"title\": \"").append(JsonCommand.toJson(event.getTitle())).append("\",");
            json.append("\"calendarId\": ").append(calendarId).append(",");
            
            if (event.getStartDate() != null) {
              json.append("\"startDate\": \"").append(JsonCommand.toJson(event.getStartDate().toString())).append("\",");
            } else {
              json.append("\"startDate\": \"\",");
            }
            if (event.getEndDate() != null) {
              json.append("\"endDate\": \"").append(JsonCommand.toJson(event.getEndDate().toString())).append("\",");
            } else {
              json.append("\"endDate\": \"\",");
            }
            json.append("\"description\": \"").append(JsonCommand.toJson(StringUtils.defaultString(event.getSummary()))).append("\"");
            
            json.append("}");
          }
        }
      }

      json.append("]");

      // Return raw JSON array for FullCalendar compatibility
      context.setJson(json.toString());
      return context;

    } catch (Exception e) {
      LOG.error("Error loading calendar events: " + e.getMessage(), e);
      // Return empty array on error for FullCalendar
      context.setJson("[]");
      return context;
    }
  }

}
