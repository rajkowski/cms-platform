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
<p>
  <c:out value="${sitePropertyMap['site.header.line1']}" />
  <c:if test="${not empty announcementLink}">
    <a style="white-space: nowrap" href="<c:out value='${announcementLink}'/>"><c:out value="${announcementLinkLabel}" /></a>
  </c:if>
  <c:if test="${showAnnouncementEdit}">
    <a class="hollow button tiny secondary" style="padding:2px; margin:0" href="${ctx}/admin/site-header-properties" title="Edit announcement" aria-label="Edit announcement">
      <i class="${font:fas()} fa-edit" aria-hidden="true"></i>
    </a>
  </c:if>
</p>