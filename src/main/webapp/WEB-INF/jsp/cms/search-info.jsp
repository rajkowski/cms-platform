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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="text" uri="/WEB-INF/tlds/text-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="searchCriteria" class="com.zeroio.platform.domain.model.cms.SearchCriteria" scope="request"/>
<c:choose>
  <c:when test="${!empty searchCriteria.query || searchCriteria.hasFilters}">
    <p>
      You searched    
      <c:if test="${!empty searchCriteria.query}">
        for <strong><c:out value="${searchCriteria.query}" /></strong>...
      </c:if>
      <c:if test="${!empty searchCriteria.tags}">
        <c:forEach items="${searchCriteria.tags}" var="label" varStatus="status">
          <strong><c:out value="${fn:trim(label)}" /></strong><c:if test="${!status.last}">, </c:if>
        </c:forEach>
        <c:if test="${fn:length(searchCriteria.tags) == 1}">tag</c:if>
        <c:if test="${fn:length(searchCriteria.tags) > 1}">tags</c:if>
      </c:if>
      <c:if test="${!empty searchCriteria.ofType && searchCriteria.ofType != 'all'}">
        in <strong><c:out value="${searchCriteria.ofType}" /></strong>...
      </c:if>
      <c:if test="${searchCriteria.hasDateFilter}"><strong>by date</strong>...</c:if>
    </p>
  </c:when>
  <c:otherwise>
    <p>A search term was not provided</p>
    <p>No results were found</p>
  </c:otherwise>
</c:choose>