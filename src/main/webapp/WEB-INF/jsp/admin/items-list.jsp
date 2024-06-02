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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="text" uri="/WEB-INF/tlds/text-functions.tld" %>
<%@ taglib prefix="category" uri="/WEB-INF/tlds/category-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="collection" class="com.simisinc.platform.domain.model.items.Collection" scope="request"/>
<jsp:useBean id="categoryMap" class="java.util.HashMap" scope="request"/>
<jsp:useBean id="itemList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="recordPaging" class="com.simisinc.platform.infrastructure.database.DataConstraints" scope="request"/>
<jsp:useBean id="tableColumnsList" class="java.util.LinkedHashMap" scope="request"/>
<style>
  .admin-item-list .item-image, .admin-item-list .item-icon {
      float: left;
  }
  .admin-item-list a {
      float: left;
      margin-top: 8px;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
      text-decoration: none;
      word-break: break-word;
      position: static!important;
      max-width: 85%;
  }
</style>
<c:if test="${!empty title}">
  <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
</c:if>
<%@include file="../page_messages.jspf" %>
<table class="unstriped admin-item-list">
  <thead>
  <tr>
    <c:forEach items="${tableColumnsList}" var="tableColumn" varStatus="status">
      <th><c:out value="${tableColumn.value.label}" /></th>
    </c:forEach>
  </tr>
  </thead>
  <tbody>
  <c:forEach items="${itemList}" var="item">
    <c:set var="category" scope="request" value="${categoryMap.get(item.categoryId)}"/>
    <tr>
      <c:forEach items="${tableColumnsList}" var="tableColumn" varStatus="status">
        <c:choose>
          <%-- Image --%>
          <c:when test="${tableColumn.value.name eq 'image'}">
            <td>
              <c:choose>
                <c:when test="${!empty item.imageUrl}">
                  <div class="item-image">
                    <img alt="item" src="<c:out value="${item.imageUrl}"/>" />
                  </div>
                </c:when>
                <c:when test="${!empty category.headerBgColor && !empty category.headerTextColor}">
                  <c:choose>
                    <c:when test="${!empty category.icon}">
                      <span class="item-icon padding-10 padding-width-10 margin-right-10" style="background-color:<c:out value="${category.headerBgColor}" />;color:<c:out value="${category.headerTextColor}" />">
                        <i class="${font:far()} fa-fw fa-<c:out value="${category.icon}" />"></i>
                      </span>
                    </c:when>
                    <c:otherwise>
                    <span class="item-icon padding-10 padding-width-10 margin-right-10" style="background-color:<c:out value="${category.headerBgColor}" />;color:<c:out value="${category.headerTextColor}" />">
                      <i class="${font:far()} fa-fw"></i>
                    </span>
                    </c:otherwise>
                  </c:choose>
                </c:when>
                <c:when test="${!empty collection.icon}">
                  <i class="item-icon ${font:fad()} fa-<c:out value="${collection.icon}" />"></i>
                </c:when>
                <c:otherwise>
                  
                </c:otherwise>
              </c:choose>
            </td>
          </c:when>
          <%-- Fields --%>
          <c:when test="${tableColumn.value.name eq 'name'}">
            <td>
              <a href="${ctx}/show/${item.uniqueId}"><c:out value="${text:trim(item.name, 30, true)}"/><c:if test="${empty item.approved}"> <span class="label warning">Needs approval</span></c:if></a>
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'uniqueId'}">
            <td>
              <a href="${ctx}/show/${item.uniqueId}" translate="no"><c:out value="${item.uniqueId}" /></a>
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'category'}">
            <td>
              <c:if test="${item.categoryId gt -1}">
                <c:out value="${category:name(item.categoryId)}" />
              </c:if>
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'summary'}">
            <td>
              <c:out value="${item.summary}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'description'}">
            <td>
              <c:out value="${item.description}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'textDescription'}">
            <td>
              <c:out value="${item.textDescription}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'keywords'}">
            <td>
              <c:out value="${item.keywords}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'geopoint'}">
            <td>
              <c:if test="${item.geocoded}">
                <c:out value="${item.latitude}" />, <c:out value="${item.longitude}" />
              </c:if>
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'latitude'}">
            <td>
              <c:out value="${item.latitude}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'longitude'}">
            <td>
              <c:out value="${item.longitude}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'location'}">
            <td>
              <c:out value="${item.location}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'street'}">
            <td>
              <c:out value="${item.street}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'addressLine2'}">
            <td>
              <c:out value="${item.addressLine2}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'addressLine3'}">
            <td>
              <c:out value="${item.addressLine3}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'city'}">
            <td>
              <c:out value="${item.city}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'state'}">
            <td>
              <c:out value="${item.state}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'postalCode'}">
            <td>
              <c:out value="${item.postalCode}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'country'}">
            <td>
              <c:out value="${item.country}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'county'}">
            <td>
              <c:out value="${item.county}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'phoneNumber'}">
            <td nowrap>
              <c:out value="${item.phoneNumber}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'email'}">
            <td>
              <c:out value="${item.email}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'cost'}">
            <td>
              <c:out value="${item.cost}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'startDate'}">
            <td>
              <c:out value="${item.startDate}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'endDate'}">
            <td>
              <c:out value="${item.endDate}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'expectedDate'}">
            <td>
              <c:out value="${item.expectedDate}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'expirationDate'}">
            <td>
              <c:out value="${item.expirationDate}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'url'}">
            <td>
              <c:if test="${!empty item.url}">
                <c:choose>
                  <c:when test="${fn:startsWith(item.url, 'http://') || fn:startsWith(item.url, 'https://')}">
                    <a href="${url:encode(item.url)}" target="_blank" rel="nofollow"><c:out value="${text:trim(item.url, 30, true)}"/></a>
                  </c:when>
                  <c:otherwise>
                    <c:out value="${text:trim(item.url, 30, true)}"/>
                  </c:otherwise>
                </c:choose>
              </c:if>
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'imageUrl'}">
            <td>
              <a target="_blank" href="${item.imageUrl}"><c:out value="${item.imageUrl}" /></a>
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'barcode'}">
            <td>
              <c:out value="${item.barcode}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'assignedTo'}">
            <td>
              <c:out value="${item.assignedTo}" />
            </td>
          </c:when>
          <c:when test="${tableColumn.value.name eq 'privacyType'}">
            <td>
              <c:out value="${item.privacyType}" />
            </td>
          </c:when>
          <c:when test="${fn:contains(item.customFieldList, tableColumn.value.name)}">
            <td>
              <c:out value="${item.customFieldList[tableColumn.value.name].value}" />
            </td>
          </c:when>
          <c:otherwise>
            <td>
              --
            </td>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </tr>
  </c:forEach>
  </tbody>
</table>
<c:if test="${empty itemList}">
  No records were found
</c:if>
<%-- Paging Control --%>
<c:set var="recordPagingParams" scope="request" value="collectionId=${collection.id}"/>
<%@include file="../paging_control.jspf" %>
