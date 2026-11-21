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
<c:set var="hasAdminActions" value="false" />
<c:forEach items="${headerAccountSections}" var="group">
  <c:if test="${group.name eq 'page' or group.name eq 'content' or group.name eq 'admin'}"><c:set var="hasAdminActions" value="true" /></c:if>
</c:forEach>
<div class="header-account-controls">
  <c:if test="${hasAdminActions}">
    <div class="user-menu header-admin-menu" data-header-admin>
      <button type="button" class="header-btn header-admin-menu-trigger" title="Site tools" aria-label="Open site tools" aria-controls="<c:out value='${headerAccountId}'/>-admin" aria-haspopup="true" aria-expanded="false" data-header-admin-trigger>
        <i class="${font:fas()} fa-th-large" aria-hidden="true"></i><span class="btn-text">Site Tools</span><i class="${font:fas()} fa-chevron-down header-menu-chevron" aria-hidden="true"></i>
      </button>
      <div class="user-menu-dropdown admin-menu-dropdown" id="<c:out value='${headerAccountId}'/>-admin">
        <div class="admin-menu-heading">
          <span class="admin-menu-heading-title">Site Tools</span>
          <span class="admin-menu-heading-description">Manage this site</span>
        </div>
        <c:forEach items="${headerAccountSections}" var="group">
          <c:if test="${group.name eq 'page' or group.name eq 'content' or group.name eq 'admin'}">
            <div class="user-menu-section">
              <c:if test="${not empty group.label}"><div class="user-menu-section-title"><c:out value="${group.label}" /></div></c:if>
              <div class="admin-apps-grid">
                <c:forEach items="${group.actions}" var="accountAction">
                  <a href="<c:out value='${accountAction.link}'/>" class="admin-app-item" title="<c:out value='${accountAction.name}'/>">
                    <i class="<c:out value='${accountAction.icon}'/>" aria-hidden="true"></i><span class="admin-app-item-label"><c:out value="${accountAction.name}" /></span>
                  </a>
                </c:forEach>
              </div>
            </div>
          </c:if>
        </c:forEach>
      </div>
    </div>
  </c:if>
  <div class="user-menu account-menu" data-header-account>
    <button type="button" class="user-menu-trigger user-account-trigger" title="<c:out value='${headerAccountTitle}'/> for <c:out value='${headerAccountName}'/>" aria-label="<c:out value='${headerAccountTitle}'/> for <c:out value='${headerAccountName}'/>" aria-controls="<c:out value='${headerAccountId}'/>" aria-haspopup="true" aria-expanded="false" data-header-account-trigger>
      <span class="user-avatar"><i class="${font:fas()} fa-user" aria-hidden="true"></i></span><span class="user-account-name"><c:out value="${headerAccountName}" /></span><i class="${font:fas()} fa-chevron-down header-menu-chevron" aria-hidden="true"></i>
    </button>
    <div class="user-menu-dropdown account-menu-dropdown" id="<c:out value='${headerAccountId}'/>">
      <div class="account-menu-heading">
        <span class="user-avatar user-avatar-large"><i class="${font:fas()} fa-user" aria-hidden="true"></i></span>
        <span class="account-menu-identity"><span class="account-menu-name"><c:out value="${headerAccountName}" /></span><span class="account-menu-caption">Signed in</span></span>
      </div>
      <c:forEach items="${headerAccountSections}" var="group">
        <c:if test="${group.name ne 'page' and group.name ne 'content' and group.name ne 'admin'}">
          <div class="user-menu-section user-actions">
            <c:forEach items="${group.actions}" var="accountAction">
              <c:choose>
                <c:when test="${accountAction.action eq 'appearance'}">
                  <button type="button" class="user-menu-item" data-header-control="appearance" title="<c:out value='${accountAction.name}'/>">
                    <i class="<c:out value='${accountAction.icon}'/>" data-header-theme-icon aria-hidden="true"></i><span class="user-menu-item-label"><c:out value="${accountAction.name}" /></span>
                  </button>
                </c:when>
                <c:otherwise>
                  <a href="<c:out value='${accountAction.link}'/>" class="user-menu-item" title="<c:out value='${accountAction.name}'/>">
                    <i class="<c:out value='${accountAction.icon}'/>" aria-hidden="true"></i><span class="user-menu-item-label"><c:out value="${accountAction.name}" /></span>
                  </a>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </div>
        </c:if>
      </c:forEach>
    </div>
  </div>
</div>