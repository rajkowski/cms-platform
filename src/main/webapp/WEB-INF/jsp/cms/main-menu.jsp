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
<nav class="header-nav <c:out value='${menuClass}'/>" id="mainNav" data-header-nav aria-label="Main navigation">
  <ul class="nav-menu">
    <c:forEach items="${masterMenuTabList}" var="menuItem" varStatus="menuStatus">
      <c:if test="${not (omitFirstHomeItem and menuStatus.first and menuItem.link eq '/')}">
        <li class="nav-item${not empty menuItem.menuItemList ? ' has-dropdown' : ''}">
          <a href="<c:out value='${headerMenuLinks[menuItem.link]}'/>" class="nav-link${menuItem.active ? ' active' : ''}">
            <c:if test="${not empty menuItem.icon}"><i class="<c:out value='${menuItem.icon}'/>" aria-hidden="true"></i></c:if>
            <c:out value="${menuItem.name}" />
          </a>
          <c:if test="${not empty menuItem.menuItemList}">
            <button type="button" class="nav-dropdown-toggle" data-header-dropdown-toggle aria-label="Open submenu for <c:out value='${menuItem.name}'/>" aria-controls="mainNav-submenu-${menuStatus.index}" aria-expanded="false">
              <i class="<c:out value='${headerSubmenuIcon}'/> <c:out value='${submenuIconClass}'/>" aria-hidden="true"></i>
            </button>
            <div class="dropdown-menu" id="mainNav-submenu-${menuStatus.index}">
              <c:forEach items="${menuItem.menuItemList}" var="subItem">
                <a href="<c:out value='${headerMenuLinks[subItem.link]}'/>" class="dropdown-item${subItem.active ? ' active' : ''}">
                  <c:if test="${not empty subItem.icon}"><i class="<c:out value='${subItem.icon}'/>" aria-hidden="true"></i></c:if>
                  <c:out value="${subItem.name}" />
                </a>
              </c:forEach>
            </div>
          </c:if>
        </li>
      </c:if>
    </c:forEach>
  </ul>
</nav>