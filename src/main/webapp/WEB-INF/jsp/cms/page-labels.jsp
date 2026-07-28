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
<jsp:useBean id="webPage" class="com.simisinc.platform.domain.model.cms.WebPage" scope="request"/>
<jsp:useBean id="searchLink" class="java.lang.String" scope="request"/>
<c:if test="${!empty webPage && !empty webPage.tags}">
  <div class="page-labels-widget">
    <div class="label-list">
      <c:forEach var="tag" items="${webPage.tags}">
        <span class="badge"><a href="${ctx}${searchLink}?label=${fn:escapeXml(tag)}"><c:out value="${tag}"/></a></span>
      </c:forEach>
    </div>
  </div>
</c:if>
