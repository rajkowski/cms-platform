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
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/tlds/html-functions.tld" %>
<%@ taglib prefix="text" uri="/WEB-INF/tlds/text-functions.tld" %>
<%@ taglib prefix="category" uri="/WEB-INF/tlds/category-functions.tld" %>
<%@ taglib prefix="collection" uri="/WEB-INF/tlds/collection-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="collection" class="com.simisinc.platform.domain.model.items.Collection" scope="request"/>
<jsp:useBean id="searchResultList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="itemList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="recordPaging" class="com.simisinc.platform.infrastructure.database.DataConstraints" scope="request"/>
<jsp:useBean id="viewMoreType" class="java.lang.String" scope="request"/>
<c:if test="${!empty title}">
  <h4 class="margin-bottom-20"><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
</c:if>
<c:if test="${empty searchResultList}">
  <p>No results were found.</p>
</c:if>
<c:forEach items="${searchResultList}" var="searchResult" varStatus="status">
  <c:set var="item" scope="request" value="${itemList[status.index]}"/>
  <div class="platform-content-search-result margin-top-10">
    <h5>
      <c:choose>
        <c:when test="${fn:startsWith(searchResult.link, 'http://') || fn:startsWith(searchResult.link, 'https://')}">
          <a target="_blank" href="${searchResult.link}"><c:out value="${searchResult.pageTitle}"/></a>
        </c:when>
        <c:otherwise>
          <a href="${searchResult.link}"><c:out value="${searchResult.pageTitle}"/></a>
        </c:otherwise>
      </c:choose>
      <c:if test="${!empty item.city}"><small class="subheader"><c:out value="${item.city}" /></small></c:if>
      <c:if test="${item.collectionId gt 0 && item.collectionId ne collection.id}">
        <span class="label" style="${collection:headerColorCSS(item.collectionId)}"><c:out value="${collection:name(item.collectionId)}" /></span>
      </c:if>
      <c:if test="${item.categoryId gt 0}">
        <span class="label" style="${category:headerColorCSS(item.categoryId)}"><c:out value="${category:name(item.categoryId)}" /></span>
      </c:if>
    </h5>
    <c:choose>
      <c:when test="${!empty searchResult.htmlExcerpt}">
        <p>${searchResult.htmlExcerpt}</p>
      </c:when>
      <c:when test="${!empty searchResult.pageDescription}">
        <p><c:out value="${searchResult.pageDescription}" /></p>
      </c:when>
    </c:choose>
    <c:if test="${!empty searchResult.tags and fn:length(searchResult.tags) > 0}">
      <div class="margin-top-5" style="margin-bottom: 15px;">
        <c:forEach items="${searchResult.tags}" var="tag" varStatus="tagStatus">
          <span class="badge"><a href="?label=${fn:escapeXml(tag)}"><c:out value="${tag}"/></a></span>
        </c:forEach>
      </div>
    </c:if>
  </div>
</c:forEach>
<%-- Paging Control and View More --%>
<c:if test="${!empty searchCriteria && recordPaging.maxPageNumber gt 1}">
  <c:choose>
    <c:when test="${showViewMoreLink eq 'true'}">
      <a href="?${searchCriteria.uri}&ofType=${viewMoreType}" class="button expanded">View More</a>
    </c:when>
    <c:when test="${showPaging eq 'true'}">
      <div class="margin-top-20 margin-bottom-20">
        <c:set var="recordPagingParams" scope="request">${searchCriteria.uri}</c:set>
        <%@include file="../paging_control.jspf" %>
      </div>
    </c:when>
  </c:choose>
</c:if>
