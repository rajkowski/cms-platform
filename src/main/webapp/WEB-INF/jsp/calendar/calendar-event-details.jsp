<%--
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
<%@ page import="static com.simisinc.platform.ApplicationInfo.VERSION" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="date" uri="/WEB-INF/tlds/date-functions.tld" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="sitePropertyMap" class="java.util.HashMap" scope="request"/>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="calendar" class="com.simisinc.platform.domain.model.cms.Calendar" scope="request"/>
<jsp:useBean id="calendarEvent" class="com.simisinc.platform.domain.model.cms.CalendarEvent" scope="request"/>
<%@include file="../page_messages.jspf" %>
<web:script package="add-to-calendar-button" file="atcb.js" async="true" defer="true" />
<div class="platform-calendar-details-container">
<c:if test="${!empty title}">
  <div class="platform-calendar-title text-center">
    <h3><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}"/></h3>
  </div>
</c:if>
  <%-- Date Formatting --%>
  <c:set var="startDateTime" scope="request"><fmt:formatDate pattern="MMMM d, yyyy" value="${calendarEvent.startDate}" /></c:set>
  <c:set var="endDateTime" scope="request"><fmt:formatDate pattern="MMMM d, yyyy" value="${calendarEvent.endDate}" /></c:set>
  <c:set var="startDate" scope="request"><fmt:formatDate pattern="MMMM d" value="${calendarEvent.startDate}" /></c:set>
  <c:set var="endDate" scope="request"><fmt:formatDate pattern="MMMM d" value="${calendarEvent.endDate}" /></c:set>
  <c:set var="startYear" scope="request"><fmt:formatDate pattern="yyyy" value="${calendarEvent.startDate}" /></c:set>
  <c:set var="endYear" scope="request"><fmt:formatDate pattern="yyyy" value="${calendarEvent.endDate}" /></c:set>
  <c:set var="startTime" scope="request"><fmt:formatDate pattern="h:mm a" value="${calendarEvent.startDate}" /></c:set>
  <c:set var="endTime" scope="request"><fmt:formatDate pattern="h:mm a" value="${calendarEvent.endDate}" /></c:set>
  <c:set var="thisMonth" scope="request"><fmt:formatDate pattern="MMMM yyyy" value="${calendarEvent.startDate}" /></c:set>
  <c:set var="thisDay" scope="request"><fmt:formatDate pattern="MMMM d, yyyy" value="${calendarEvent.startDate}" /></c:set>
  <%-- Show the month header--%>
  <div class="platform-calendar-month text-center"><h2><c:out value="${thisDay}" /></h2></div>
  <%-- Show the day --%>
  <div class="platform-calendar-month-separator">
    <span class="platform-calendar-month-separator-label"><c:out value="${thisDay}" /></span>
  </div>
  <div class="platform-calendar-event-block">
    <h1><c:out value="${calendarEvent.title}" /></h1>
    <c:choose>
      <c:when test="${calendarEvent.allDay}">
        <c:if test="${startDateTime ne endDateTime}">
          <p class="platform-calendar-event-date">
            <i class="fa fa-calendar-o fa-fw"></i>
            <fmt:formatDate pattern="MMMM d, yyyy" value="${calendarEvent.startDate}" />
            <c:if test="${startDateTime ne endDateTime}">
              -
              <fmt:formatDate pattern="MMMM d, yyyy" value="${calendarEvent.endDate}" />
            </c:if>
          </p>
        </c:if>
      </c:when>
      <c:otherwise>
        <p class="platform-calendar-event-date">
          <c:choose>
            <c:when test="${startDateTime eq endDateTime}">
              <i class="fa fa-clock-o fa-fw"></i>
              <fmt:formatDate pattern="h:mm a" value="${calendarEvent.startDate}" />
              <c:if test="${startTime ne endTime}">
                - <fmt:formatDate pattern="h:mm a" value="${calendarEvent.endDate}" />
              </c:if>
            </c:when>
            <c:otherwise>
              <i class="fa fa-calendar-o fa-fw"></i>
              <fmt:formatDate pattern="MMMM d, h:mm a" value="${calendarEvent.startDate}" />
              -
              <c:choose>
                <c:when test="${startYear ne endYear}">
                  <fmt:formatDate pattern="MMMM d, yyyy h:mm a" value="${calendarEvent.endDate}" />
                </c:when>
                <c:otherwise>
                  <fmt:formatDate pattern="MMMM d, h:mm a" value="${calendarEvent.endDate}" />
                </c:otherwise>
              </c:choose>
            </c:otherwise>
          </c:choose>
        </p>
      </c:otherwise>
    </c:choose>
    <c:if test="${!empty calendarEvent.location}">
      <p class="platform-calendar-event-location"><i class="fa fa-map-marker fa-fw"></i> <c:out value="${calendarEvent.location}" /></p>
    </c:if>
    <add-to-calendar-button
      name="<c:out value="${calendarEvent.title}" />"
      options="'Apple','Google','Microsoft365','Outlook.com'"
      <c:if test="${!empty calendarEvent.location}">
        location="<c:out value="${calendarEvent.location}" />"
      </c:if>
      startDate="<fmt:formatDate pattern="yyyy-MM-dd" value="${calendarEvent.startDate}" />"
      endDate="<fmt:formatDate pattern="yyyy-MM-dd" value="${calendarEvent.endDate}" />"
      <c:if test="${!calendarEvent.allDay}">
        startTime="<fmt:formatDate pattern="HH:mm" value="${calendarEvent.startDate}" />"
        endTime="<fmt:formatDate pattern="HH:mm" value="${calendarEvent.endDate}" />"
      </c:if>
      timeZone="<c:out value="${timezone}"/>"
      <c:if test="${!empty detailsForCalendarButton}">
        description="<c:out value="${detailsForCalendarButton}" />"
      </c:if>
      hideBranding="true"></add-to-calendar-button>
    <c:if test="${!empty calendarEvent.summary}">
      <p class="platform-calendar-event-summary"><c:out value="${calendarEvent.summary}" /></p>
    </c:if>
    <c:if test="${!empty calendarEvent.detailsUrl || !empty calendarEvent.signUpUrl}">
      <p class="platform-calendar-event-buttons">
        <i class="fa fa-fw"></i>
        <c:if test="${!empty calendarEvent.detailsUrl}">
          <c:choose>
            <c:when test="${fn:startsWith(calendarEvent.detailsUrl, 'http://') || fn:startsWith(calendarEvent.detailsUrl, 'https://')}">
              <a class="button primary" target="_blank" href="<c:out value="${calendarEvent.detailsUrl}" />">Learn More</a>
            </c:when>
            <c:otherwise>
              <a class="button primary" href="<c:out value="${ctx}${calendarEvent.detailsUrl}" />">View Details</a>
            </c:otherwise>
          </c:choose>
        </c:if>
        <c:if test="${!empty calendarEvent.signUpUrl}">
          <c:choose>
            <c:when test="${fn:startsWith(calendarEvent.signUpUrl, 'http://') || fn:startsWith(calendarEvent.signUpUrl, 'https://')}">
              <a class="button primary" target="_blank" href="<c:out value="${calendarEvent.signUpUrl}" />">Sign Up Page</a>
            </c:when>
            <c:otherwise>
              <a class="button primary" href="<c:out value="${ctx}${calendarEvent.signUpUrl}" />">Sign Up Page</a>
            </c:otherwise>
          </c:choose>
        </c:if>
      </p>
    </c:if>
    <c:choose>
      <c:when test="${!empty returnPage}">
        <p class="platform-calendar-event-return">
          <i class="fa fa-fw"></i> <a href="javascript:goBack('<c:out value="${returnPage}" />');"><i class="${font:fal()} fa-arrow-left"></i> Return to previous page</a>
        </p>
      </c:when>
      <c:otherwise>
        <p class="platform-calendar-event-return">
          <i class="fa fa-fw"></i> <a href="${ctx}/calendar"><i class="${font:fal()} fa-arrow-left"></i> View the calendar</a>
        </p>
      </c:otherwise>
    </c:choose>
  </div>
</div>
<script>
  function goBack() {
    window.history.back();
  }
</script>
