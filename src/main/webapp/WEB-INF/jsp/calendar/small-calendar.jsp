<%--
  ~ Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
  ~ Copyright 2022 SimIS Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<%@ page import="static com.zeroio.platform.ApplicationInfo.VERSION" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="calendarList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="calendarEventList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="calendarEvent" class="com.simisinc.platform.domain.model.cms.CalendarEvent" scope="request"/>
<jsp:useBean id="calendarUniqueId" class="java.lang.String" scope="request"/>
<jsp:useBean id="defaultView" class="java.lang.String" scope="request"/>
<jsp:useBean id="height" class="java.lang.String" scope="request"/>
<jsp:useBean id="showEvents" class="java.lang.String" scope="request"/>
<jsp:useBean id="showHolidays" class="java.lang.String" scope="request"/>
<jsp:useBean id="showMoodleEvents" class="java.lang.String" scope="request"/>
<jsp:useBean id="moodleBackgroundColor" class="java.lang.String" scope="request"/>
<jsp:useBean id="moodleTextColor" class="java.lang.String" scope="request"/>
<link rel="stylesheet" href="${ctx}/css/platform-calendar.css?v=<%= VERSION %>" />
<web:stylesheet package="fullcalendar" file="skeleton.css" />
<web:stylesheet package="fullcalendar" file="themes/forma/theme.css" />
<web:stylesheet package="fullcalendar" file="themes/forma/palettes/blue.css" />
<web:script package="moment" file="moment.min.js" />
<web:script package="fullcalendar" file="global.js" />
<web:script package="fullcalendar" file="themes/forma/global.js" />
<%-- Render the widget --%>
<div id="calendar-small"></div>
<div id="calendar-tooltip-small" class="tooltip top align-center under-reveal" style="display:none"></div>
<script>
  function showTooltip${widgetContext.uniqueId}(el, event) {
    let tooltip = $("#calendar-tooltip-small");
    tooltip.empty();
    $('<h5></h5>').text(event.title || '').appendTo(tooltip);
    if (event.allDay === undefined || !event.allDay) {
      $('<p></p>').text(moment(event.start).format('LT') + ' - ' + moment(event.end).format('LT')).appendTo(tooltip);
    }
    if (event.extendedProps.location) {
      let locationText = $('<p></p>');
      $('<i></i>').addClass('fa fa-map-marker').appendTo(locationText);
      locationText.append(document.createTextNode(' ' + event.extendedProps.location));
      locationText.appendTo(tooltip);
    }
    if (event.extendedProps.description || event.extendedProps.detailsUrl) {
      $('<p></p>').addClass('no-gap').text('(click for more details)').appendTo(tooltip);
    }
    let ttHeight = tooltip.outerHeight();
    let ttWidth = tooltip.outerWidth();

    <%-- Center and show it --%>
    let parentTop = Math.round($('#calendar-small').parent().offset().top);
    let parentLeft = Math.round($('#calendar-small').parent().offset().left);
    let calendarTop = $('#calendar-small').offset().top;
    let calendarLeft = $('#calendar-small').offset().left;
    let elTop = $(el).offset().top;
    let elLeft = Math.round($(el).offset().left);
    let tdTop = Math.round($(el).closest('.calendar-event').offset().top);
    let tdLeft = Math.round($(el).closest('.calendar-event').offset().left);
    let tdWidth = Math.round($(el).closest('.calendar-event').outerWidth());
    let top = Math.round(tdTop - ttHeight - 10);
    let left = tdLeft + (tdWidth/2) - (ttWidth/2);
    $('#calendar-tooltip-small').css({top: top, left: left});
    $('#calendar-tooltip-small').fadeIn(200);
  }

  <c:choose>
    <c:when test="${defaultView eq 'list'}">
      <c:set var="initialView" scope="request" value="listWeek" />
      <c:set var="optionOrder" scope="request" value="listWeek,dayGridMonth" />
    </c:when>
    <c:when test="${defaultView eq 'day'}">
      <c:set var="initialView" scope="request" value="timeGrid" />
      <c:set var="optionOrder" scope="request" value="timeGrid,dayGridMonth" />
    </c:when>
    <c:otherwise>
      <c:set var="initialView" scope="request" value="dayGridMonth" />
      <c:set var="optionOrder" scope="request" value="dayGridMonth,listWeek" />
    </c:otherwise>
  </c:choose>

  document.addEventListener('DOMContentLoaded', function() {
    let calendarEl = document.getElementById('calendar-small');
    let calendar = new FullCalendar.Calendar(calendarEl, {
      initialView: '${initialView}',
      <c:choose>
        <c:when test="${!empty height}">
          height: <c:out value="${height}" />,
        </c:when>
        <c:otherwise>
          height: 'auto',
        </c:otherwise>
      </c:choose>
      aspectRatio: 2,
      headerToolbar: {
        start: 'title',
        // center: '',
        end: 'today prev,next'
      },
      buttonText: {
        today:    'Today',
        month:    'Month',
        week:     'Week',
        day:      'Day',
        list:     'List',
        timeGrid: 'Day'
      },
      selectable: false,
      eventClick: function(info) {
        info.jsEvent.preventDefault(); 

        if (info.event.id <= 0) {
          return;
        }
        let detailsUrl = info.event.extendedProps.detailsUrl;
        if (detailsUrl && (detailsUrl.indexOf('http://') === 0 || detailsUrl.indexOf('https://') === 0)) {
          window.open(detailsUrl, '_blank');
        } else if (detailsUrl && detailsUrl.indexOf('/') === 0) {
          window.location.href='${ctx}' + detailsUrl + '?returnPage=${widgetContext.uri}';
        } else {
          window.location.href='${ctx}/calendar-event/' + info.event.extendedProps.uniqueId + '?returnPage=${widgetContext.uri}';
        }
      },
      eventMouseEnter: function(info) {
        if (info.view.type !== 'dayGridMonth') {
          return;
        }
        showTooltip${widgetContext.uniqueId}(info.el, info.event);
      },
      eventMouseLeave: function(info) {
        $('#calendar-tooltip-small').hide();
      },
      eventSources: [
        <c:if test="${showEvents eq 'true'}">
        {
          url: '/json/calendar?showEvents=true<c:if test="${!empty calendarUniqueId}">&calendarUniqueId=<c:out value="${calendarUniqueId}" /></c:if>',
          className: 'calendar-event',
          color: '#999999'
        },
        </c:if>
        <c:if test="${showHolidays eq 'true'}">
        {
          url: '/json/calendar?showHolidays=true',
          className: 'calendar-event',
          color: '#111111'
        },
        </c:if>
        <c:if test="${showMoodleEvents eq 'true'}">
        {
          url: '/json/calendar?showMoodleEvents=true',
          className: 'calendar-event',
          color: '${moodleBackgroundColor}'
        }
        </c:if>
      ]
    });
    calendar.render();
  });
</script>
