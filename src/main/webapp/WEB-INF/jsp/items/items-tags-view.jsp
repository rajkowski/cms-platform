<%--
  ~ Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="collection" uri="/WEB-INF/tlds/collection-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="collection" class="com.simisinc.platform.domain.model.items.Collection" scope="request"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="itemList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="showLink" class="java.lang.String" scope="request"/>
<jsp:useBean id="showCollectionIcon" class="java.lang.String" scope="request"/>
<c:if test="${!empty title}">
  <h4 class="margin-bottom-20"><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
</c:if>
<%@include file="../page_messages.jspf" %>
<c:forEach items="${itemList}" var="item">
  <%-- Determine the Icon --%>
  <c:set var="showIcon" value="false" />
  <c:set var="iconHtml" value="" />
  <c:set var="iconClass" value="" />
  <c:if test="${showCollectionIcon eq 'true'}">
    <c:set var="showIcon" value="true" />
    <c:if test="${!empty collection:icon(item.collectionId)}">
      <c:set var="iconClass" value="${font:fad()} fa-${collection:icon(item.collectionId)}" />
      <c:set var="iconHtml" value="<i class='${iconClass}'></i> " />
    </c:if>
  </c:if>
  <%-- Determine the link --%>
  <c:set var="linkStart" value="" />
  <c:set var="linkEnd" value="" />
  <c:if test="${showLink eq 'true' && !empty item.url}">
    <%-- Internal/External --%>
    <c:set var="isExternal" value="false" />
    <c:if test="${!empty item.url && (fn:startsWith(item.url, 'http://') || fn:startsWith(item.url, 'https://'))}">
      <c:set var="isExternal" value="true" />
    </c:if>
    <c:set var="linkStart" value="<a href='${item.url}' ${isExternal eq 'true' ? 'target=\"_blank\"' : ''}>"/>
    <c:set var="linkEnd" value="</a>" />
  </c:if>
  <%-- Output the tag --%>
  <span class="button tag">${linkStart}${iconHtml}<c:out value="${item.name}" />${linkEnd}</span>
</c:forEach>
