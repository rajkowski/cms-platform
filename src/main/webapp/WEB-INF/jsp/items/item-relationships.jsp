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
<%@ taglib prefix="item" uri="/WEB-INF/tlds/item-functions.tld" %>
<%@ taglib prefix="collection" uri="/WEB-INF/tlds/collection-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="itemRelationshipList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="recordPaging" class="com.simisinc.platform.infrastructure.database.DataConstraints" scope="request"/>
<jsp:useBean id="showRelatedCollectionName" class="java.lang.String" scope="request"/>
<jsp:useBean id="showRemoveRelationshipButton" class="java.lang.String" scope="request"/>
<c:if test="${showRemoveRelationshipButton eq 'true'}">
  <script>
    function removeRelationship${widgetContext.uniqueId}(relatedItemId) {
      if (!confirm("Are you sure you want to remove the relationship?")) {
        return;
      }
      window.location.href = '${widgetContext.uri}?action=removeRelationship&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&relatedItemId=' + relatedItemId;
    }
  </script>
</c:if>
<c:if test="${!empty title}">
  <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}"/></h4>
</c:if>
<%@include file="../page_messages.jspf" %>
<c:if test="${empty itemRelationshipList}">
  <p class="subheader">
    No relationships were found
  </p>
</c:if>
<c:if test="${!empty itemRelationshipList}">
  <ul>
    <c:forEach items="${itemRelationshipList}" var="itemRelationship">
      <c:choose>
        <c:when test="${item.id eq itemRelationship.itemId}">
          <c:set var="relatedItem" scope="request" value="${item:itemById(itemRelationship.relatedItemId)}"/>
        </c:when>
        <c:otherwise>
          <c:set var="relatedItem" scope="request" value="${item:itemById(itemRelationship.itemId)}"/>
        </c:otherwise>
      </c:choose>
      <li>
        <c:choose>
          <c:when test="${!empty relatedItem.imageUrl}">
            <div class="item-image">
              <img alt="item image" src="<c:out value="${relatedItem.imageUrl}"/>" />
            </div>
          </c:when>
          <c:when test="${!empty collection:icon(relatedItem.collectionId)}">
            <i class="${font:fad()} fa-<c:out value="${collection:icon(relatedItem.collectionId)}" />"></i>
          </c:when>
        </c:choose>
        <c:choose>
          <c:when test="${item:hasViewPermission(relatedItem, userSession.user)}">
            <a href="${ctx}/show/${relatedItem.uniqueId}"><c:out value="${relatedItem.name}"/></a>
          </c:when>
          <c:otherwise>
            <c:out value="${relatedItem.name}"/>
          </c:otherwise>
        </c:choose>
        <c:if test="${showRelatedCollectionName eq 'true'}">
          / <c:out value="${collection:name(relatedItem.collectionId)}"/>
        </c:if>
        <c:if test="${!empty relatedItem.city}">
          <small class="subheader"><c:out value="${relatedItem.city}"/></small>
        </c:if>
        <c:if test="${showRemoveRelationshipButton eq 'true'}">
          <a href="javascript:removeRelationship${widgetContext.uniqueId}(${relatedItem.id})" class="button radius tiny warning no-gap">Remove</a>
        </c:if>
      </li>
    </c:forEach>
  </ul>
</c:if>
