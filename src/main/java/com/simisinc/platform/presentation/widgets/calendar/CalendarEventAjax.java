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

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.CalendarEvent;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarEventRepository;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarEventSpecification;
import com.simisinc.platform.infrastructure.persistence.cms.CalendarRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns the specified event
 *
 * @author matt rajkowski
 * @created 11/27/18 8:55 AM
 */
public class CalendarEventAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  public WidgetContext execute(WidgetContext context) {

    long id = context.getParameterAsLong("id", -1);
    if (id == -1) {
      context.setJson("[]");
      return context;
    }

    // Access the event
    List<CalendarEvent> calendarEventList = null;
    CalendarEventSpecification specification = new CalendarEventSpecification();
    specification.setId(id);
    calendarEventList = CalendarEventRepository.findAll(specification, null);
    if (calendarEventList == null || calendarEventList.isEmpty()) {
      context.setJson("[]");
      return context;
    }

    String offset = "";
    ZoneId serverZoneId = ZoneId.systemDefault();
    if ("UTC".equals(serverZoneId.getId())) {
      offset = "+00:00";
    }

    // Determine the results to be shown
    StringBuilder sb = new StringBuilder();
    for (CalendarEvent calendarEvent : calendarEventList) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      // Prepare the calendar event object
      Map<String, Object> props = new HashMap<>();
      props.put("id", calendarEvent.getId());
      props.put("uniqueId", calendarEvent.getUniqueId());
      props.put("title", calendarEvent.getTitle());
      props.put("calendarId", calendarEvent.getCalendarId());
      if (calendarEvent.getAllDay()) {
        props.put("allDay", true);
      }
      // 2018-12-09T16:00:00+00:00
      String startDate = new SimpleDateFormat("yyyy-MM-dd").format(calendarEvent.getStartDate());
      String endDate = new SimpleDateFormat("yyyy-MM-dd").format(calendarEvent.getEndDate());
      String startDateHoursMinutes = new SimpleDateFormat("HH:mm").format(calendarEvent.getStartDate());
      String endDateHoursMinutes = new SimpleDateFormat("HH:mm").format(calendarEvent.getEndDate());
      props.put("start", startDate + "T" + startDateHoursMinutes + ":00" + offset);
      props.put("end", endDate + "T" + endDateHoursMinutes + ":00" + offset);
      if (calendarEvent.getDetailsUrl() != null) {
        props.put("detailsUrl", calendarEvent.getDetailsUrl());
      }
      if (calendarEvent.getSignUpUrl() != null) {
        props.put("signUpUrl", calendarEvent.getSignUpUrl());
      }
      if (StringUtils.isNotEmpty(calendarEvent.getSummary())) {
        props.put("description", calendarEvent.getSummary());
      }
      if (StringUtils.isNotEmpty(calendarEvent.getLocation())) {
        props.put("location", calendarEvent.getLocation());
      }
      String color = CalendarRepository.findById(calendarEvent.getCalendarId()).getColor();
      if (color != null) {
        props.put("color", color);
      }
      sb.append(JsonCommand.createJsonNode(props));
    }
    context.setJson(sb.toString());
    return context;
  }
}
