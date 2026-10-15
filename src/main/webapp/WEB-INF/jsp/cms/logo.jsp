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
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<a href="<c:out value='${logoHomeLink}'/>" class="header-logo <c:out value='${logoClass}'/>">
  <c:choose>
    <c:when test="${not empty logoSrc}">
      <img class="logo" src="<c:out value='${logoSrc}'/>" alt="<c:out value='${siteTitle}'/>" style="<c:out value='${logoStyle}'/>" />
    </c:when>
    <c:when test="${not empty logoLightSrc}">
      <img class="logo-light" src="<c:out value='${logoLightSrc}'/>" alt="<c:out value='${siteTitle}'/>" style="<c:out value='${logoStyle}'/>" />
      <img class="logo-dark" src="<c:out value='${logoDarkSrc}'/>" alt="<c:out value='${siteTitle}'/>" style="<c:out value='${logoStyle}'/>" />
    </c:when>
    <c:otherwise>
      <i class="${font:fas()} fa-cube" aria-hidden="true"></i>
      <span><c:out value="${not empty text ? text : siteTitle}" /></span>
    </c:otherwise>
  </c:choose>
</a>