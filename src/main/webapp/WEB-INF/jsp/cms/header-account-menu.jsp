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
<div class="user-menu" data-header-account>
  <button type="button" class="user-menu-trigger" title="<c:out value='${headerAccountTitle}'/>" aria-label="<c:out value='${headerAccountTitle}'/>" aria-controls="<c:out value='${headerAccountId}'/>" aria-expanded="false" data-header-account-trigger>
    <i class="${font:fas()} fa-user-circle" aria-hidden="true"></i><span><c:out value="${headerAccountName}" /></span><i class="${font:fas()} fa-chevron-down" aria-hidden="true"></i>
  </button>
  <div class="user-menu-dropdown" id="<c:out value='${headerAccountId}'/>">
    <c:forEach items="${headerAccountSections}" var="group">
      <div class="user-menu-section">
        <c:if test="${not empty group.label}"><div class="user-menu-section-title"><c:out value="${group.label}" /></div></c:if>
        <div class="${not empty group.label ? 'admin-apps-grid' : 'user-actions'}">
          <c:forEach items="${group.actions}" var="accountAction">
            <c:choose>
              <c:when test="${accountAction.action eq 'appearance'}">
                <button type="button" class="user-menu-item" data-header-control="appearance" title="<c:out value='${accountAction.name}'/>">
                  <i class="<c:out value='${accountAction.icon}'/>" data-header-theme-icon aria-hidden="true"></i><span class="user-menu-item-label"><c:out value="${accountAction.name}" /></span>
                </button>
              </c:when>
              <c:otherwise>
                <a href="<c:out value='${accountAction.link}'/>" class="${not empty group.label ? 'admin-app-item' : 'user-menu-item'}" title="<c:out value='${accountAction.name}'/>">
                  <i class="<c:out value='${accountAction.icon}'/>" aria-hidden="true"></i><span class="${not empty group.label ? 'admin-app-item-label' : 'user-menu-item-label'}"><c:out value="${accountAction.name}" /></span>
                </a>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </div>
      </div>
    </c:forEach>
  </div>
</div>