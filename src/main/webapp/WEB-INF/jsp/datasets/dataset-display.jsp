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
<%@ page import="static com.zeroio.platform.ApplicationInfo.VERSION" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="rows" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="metadata" class="java.util.LinkedHashMap" scope="request"/>

<c:if test="${!empty title}">
  <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
</c:if>

<c:if test="${showMetadata && !empty metadata}">
  <div class="dataset-metadata" style="margin-bottom: 15px; padding: 10px; background-color: #f5f5f5; border-radius: 4px;">
    <c:forEach items="${metadata}" var="item">
      <div style="display: inline-block; margin-right: 20px;">
        <strong><c:out value="${item.key}"/>:</strong> <c:out value="${item.value}"/>
      </div>
    </c:forEach>
  </div>
</c:if>

<c:if test="${!empty widgetContext.errorMessage}">
  <div class="alert alert-danger" role="alert">
    <c:out value="${widgetContext.errorMessage}"/>
  </div>
</c:if>

<c:if test="${empty rows}">
  <p>No data available.</p>
</c:if>

<c:if test="${!empty rows}">
  <div class="dataset-table-container" style="overflow-x: auto;">
    <table class="table table-striped table-hover">
      <thead>
        <tr>
          <c:forEach items="${displayColumnIndices}" var="columnIndex" varStatus="status">
            <th>
              <c:out value="${displayHeadings[status.index]}"/>
              <c:if test="${showSort}">
                <a href="#" style="margin-left: 5px;"><i class="fa fa-sort"></i></a>
              </c:if>
            </th>
          </c:forEach>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${rows}" var="row">
          <tr>
            <c:forEach items="${displayColumnIndices}" var="columnIndex">
              <td>
                <c:choose>
                  <c:when test="${columnIndex < fn:length(row)}">
                    <c:choose>
                      <c:when test="${convertColumnToLinkIndex == columnIndex && columnIsLinkIndex >= 0 && columnIsLinkIndex < fn:length(row) && !empty row[columnIsLinkIndex]}">
                        <a href="${fn:escapeXml(row[columnIsLinkIndex])}"><c:out value="${row[columnIndex]}"/></a>
                      </c:when>
                      <c:otherwise>
                        <c:out value="${row[columnIndex]}"/>
                      </c:otherwise>
                    </c:choose>
                  </c:when>
                  <c:otherwise>
                    &nbsp;
                  </c:otherwise>
                </c:choose>
              </td>
            </c:forEach>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </div>

  <c:if test="${showPaging && totalPages > 1}">
    <nav aria-label="Page navigation" style="margin-top: 20px;">
      <ul class="pagination">
        <c:if test="${pageNumber > 1}">
          <li class="page-item">
            <a class="page-link" href="?pageNumber=1">First</a>
          </li>
          <li class="page-item">
            <a class="page-link" href="?pageNumber=${pageNumber - 1}">Previous</a>
          </li>
        </c:if>

        <c:forEach begin="1" end="${totalPages}" var="pageNum">
          <c:choose>
            <c:when test="${pageNum == pageNumber}">
              <li class="page-item active">
                <span class="page-link">${pageNum}</span>
              </li>
            </c:when>
            <c:otherwise>
              <li class="page-item">
                <a class="page-link" href="?pageNumber=${pageNum}">${pageNum}</a>
              </li>
            </c:otherwise>
          </c:choose>
        </c:forEach>

        <c:if test="${pageNumber < totalPages}">
          <li class="page-item">
            <a class="page-link" href="?pageNumber=${pageNumber + 1}">Next</a>
          </li>
          <li class="page-item">
            <a class="page-link" href="?pageNumber=${totalPages}">Last</a>
          </li>
        </c:if>
      </ul>
    </nav>
    <p style="text-align: center; margin-top: 10px;">
      Showing <c:out value="${(pageNumber - 1) * recordsPerPage + 1}"/> to 
      <c:out value="${pageNumber * recordsPerPage > totalRecords ? totalRecords : pageNumber * recordsPerPage}"/> 
      of <c:out value="${totalRecords}"/> records
    </p>
  </c:if>
</c:if>
