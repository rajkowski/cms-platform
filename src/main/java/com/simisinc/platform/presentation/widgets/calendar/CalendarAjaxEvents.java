/*
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
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Calendar;
import com.simisinc.platform.domain.model.cms.CalendarEvent;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarEventRepository;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarEventSpecification;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarRepository;

/**
 * Retrieves calendars and provides a JSON response
 *
 * @author matt rajkowski
 * @created 1/22/19 12:12 PM
 */
public class CalendarAjaxEvents {

  private static Log LOG = LogFactory.getLog(CalendarAjaxEvents.class);

  protected static void addCalendarEvents(long userId, String calendarUniqueId, Date startDate, Date endDate, StringBuilder sb) {

    // Determine which calendar(s) to show
    List<Calendar> calendarList = CalendarRepository.findAll();
    long calendarId = -1L;
    if (StringUtils.isNotBlank(calendarUniqueId)) {
      for (Calendar calendar : calendarList) {
        if (calendar.getUniqueId().equals(calendarUniqueId)) {
          calendarId = calendar.getId();
          break;
        }
      }
    }

    // Load the events
    CalendarEventSpecification specification = new CalendarEventSpecification();
    if (calendarId > -1) {
      specification.setCalendarId(calendarId);
    }
    specification.setStartingDateRange(new Timestamp(startDate.getTime()));
    specification.setEndingDateRange(new Timestamp(endDate.getTime()));
    List<CalendarEvent> calendarEventList = CalendarEventRepository.findAll(specification, null);
    LOG.debug("Calendar events: " + startDate + " - " + endDate + " (" + calendarEventList.size() + ")");

    String offset = "";
    ZoneId serverZoneId = ZoneId.systemDefault();
    if ("UTC".equals(serverZoneId.getId())) {
      offset = "+00:00";
    }

    // Determine the results to be shown
    if (!calendarEventList.isEmpty()) {
      for (CalendarEvent calendarEvent : calendarEventList) {
        if (sb.length() > 0) {
          sb.append(",");
        }
        String color = getColor(calendarList, calendarEvent);
        sb.append(calendarEventToJson(calendarEvent, offset, color));
      }
    }
  }

  private static String calendarEventToJson(CalendarEvent calendarEvent, String offset, String color) {
    // Prepare the calendar event object
    Map<String, Object> props = new HashMap<>();
    props.put("id", calendarEvent.getId());
    props.put("title", calendarEvent.getTitle());
    if (calendarEvent.getAllDay()) {
      props.put("allDay", true);
    }
    // 2018-12-09T16:00:00+00:00
    String startDate = new SimpleDateFormat("yyyy-MM-dd").format(calendarEvent.getStartDate());
    String endDate = new SimpleDateFormat("yyyy-MM-dd").format(calendarEvent.getEndDate());
    String startDateHoursMinutes = new SimpleDateFormat("HH:mm").format(calendarEvent.getStartDate());
    String endDateHoursMinutes = new SimpleDateFormat("HH:mm").format(calendarEvent.getEndDate());
    if (calendarEvent.getAllDay()) {
      // Requied for viewing to extend to the end of the day, otherwise the view uses the day before
      props.put("start", startDate);
      props.put("end", endDate + "T24:00");
    } else {
      props.put("start", startDate + "T" + startDateHoursMinutes + ":00" + offset);
      props.put("end", endDate + "T" + endDateHoursMinutes + ":00" + offset);
    }

    // Add extended properties
    Map<String, Object> extendedProps = new HashMap<>();
    extendedProps.put("uniqueId", calendarEvent.getUniqueId());
    extendedProps.put("calendarId", calendarEvent.getCalendarId());
    if (calendarEvent.getDetailsUrl() != null) {
      extendedProps.put("detailsUrl", calendarEvent.getDetailsUrl());
    }
    if (calendarEvent.getSignUpUrl() != null) {
      extendedProps.put("signUpUrl", calendarEvent.getSignUpUrl());
    }
    if (StringUtils.isNotEmpty(calendarEvent.getSummary())) {
      extendedProps.put("description", calendarEvent.getSummary());
    }
    if (StringUtils.isNotEmpty(calendarEvent.getLocation())) {
      extendedProps.put("location", calendarEvent.getLocation());
    }
    if (color != null) {
      extendedProps.put("color", color);
    }
    props.put("extendedProps", extendedProps);
    return JsonCommand.createJsonNode(props).toString();
  }

  public static String getColor(List<Calendar> calendarList, CalendarEvent calendarEvent) {
    for (Calendar calendar : calendarList) {
      if (calendarEvent.getCalendarId().equals(calendar.getId())) {
        return calendar.getColor();
      }
    }
    return null;
  }
}
