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
<c:choose>
  <%-- Mobile Menu Toggle --%>
  <c:when test="${headerControlType eq 'mobileMenu'}">
    <button type="button" class="mobile-menu-toggle" data-header-control="mobileMenu" aria-controls="mainNav" aria-expanded="false" aria-label="<c:out value='${headerControlLabel}'/>">
      <i class="<c:out value='${headerControlIcon}'/>" aria-hidden="true"></i>
    </button>
  </c:when>
  <%-- Appearance Light/Dark --%>
  <c:when test="${headerControlType eq 'appearance'}">
    <button type="button" class="theme-toggle" data-header-control="appearance" title="<c:out value='${headerControlLabel}'/>" aria-label="<c:out value='${headerControlLabel}'/>">
      <i class="<c:out value='${headerControlIcon}'/>" data-header-theme-icon aria-hidden="true"></i>
    </button>
  </c:when>
  <%-- Region --%>
  <c:when test="${headerControlType eq 'region'}">
    <button type="button" class="header-btn region-filter-trigger" title="<c:out value='${headerControlLabel}'/>" aria-controls="regionModal" data-header-control="region">
      <i class="<c:out value='${headerControlIcon}'/>" aria-hidden="true"></i><span class="btn-text"><c:out value="${headerControlLabel}" /></span>
    </button>
    <div id="regionModal" class="region-modal" role="dialog" aria-modal="true" aria-hidden="true" aria-labelledby="regionModalTitle" tabindex="-1">
      <div class="region-modal-content">
        <div class="region-modal-header">
          <h3 id="regionModalTitle"><i class="${font:fas()} fa-globe-americas" aria-hidden="true"></i> To get started, select your region / market:</h3>
          <button type="button" class="region-modal-close" data-header-control="closeRegion" aria-label="Close"><i class="${font:fas()} fa-times" aria-hidden="true"></i></button>
        </div>
        <div class="region-modal-body"><form id="regionFilterForm" autocomplete="off"><div class="region-options" id="regionOptionsContainer"></div></form></div>
      </div>
    </div>
  </c:when>
  <%-- Ask AI Button with an HTML Link --%>
  <c:when test="${headerControlType eq 'askAI' && headerControlAskAIControlType eq 'button'}">
    <a href="<c:out value='${headerControlLink}'/>" class="header-btn ask-ai-trigger" data-header-control="askAI" title="<c:out value='${headerControlLabel}'/>">
      <i class="<c:out value='${headerControlIcon}'/>" aria-hidden="true"></i>
      <span class="btn-text"><c:out value="${headerControlLabel}" /></span>
    </a>
  </c:when>
  <%-- Ask AI Drawer --%>
  <c:when test="${headerControlType eq 'askAI'}">
    <button type="button" class="header-btn  ask-ai-trigger" data-header-control="askAI" title="<c:out value='${headerControlLabel}'/>" aria-controls="askAIDrawer" aria-expanded="false">
      <i class="<c:out value='${headerControlIcon}'/>" aria-hidden="true"></i>
      <span class="btn-text"><c:out value="${headerControlLabel}" /></span>
    </button>
    <div id="askAIDrawer" class="ask-ai-drawer" role="dialog" aria-modal="true" aria-hidden="true" aria-labelledby="askAIDrawerTitle" tabindex="-1">
      <div class="ask-ai-drawer-header">
        <h3 id="askAIDrawerTitle"><i class="${font:fas()} fa-brain" aria-hidden="true"></i> Ask AI Assistant</h3>
        <button type="button" class="ask-ai-close" data-header-control="closeAskAI" aria-label="Close">
          <i class="${font:fas()} fa-times" aria-hidden="true"></i>
        </button>
      </div>
      <div class="ask-ai-drawer-content">
        <div class="ask-ai-messages" id="askAIMessages">
          <div class="ask-ai-message ask-ai-message-system">
            <div class="ask-ai-message-icon"><i class="${font:fas()} fa-info-circle" aria-hidden="true"></i></div>
            <div class="ask-ai-message-text">
              <strong>AI Assistant Not Connected</strong>
              <p>The AI assistant service is currently not configured. Please contact your administrator to enable this feature.</p>
            </div>
          </div>
        </div>
        <div class="ask-ai-input-container">
          <form class="ask-ai-form" data-ask-ai-form>
            <textarea id="askAIInput" class="ask-ai-input" placeholder="Type your question here..." rows="3" disabled></textarea>
            <button type="submit" class="ask-ai-send" disabled title="AI assistant not connected">
              <i class="${font:fas()} fa-paper-plane" aria-hidden="true"></i>
            </button>
          </form>
        </div>
      </div>
    </div>
  </c:when>
  <%-- Workspaces --%>
  <c:when test="${headerControlType eq 'workspaces'}">
    <div class="user-menu" data-header-workspaces>
      <button type="button" class="header-btn" data-header-control="workspaces" title="<c:out value='${headerControlLabel}'/>" aria-haspopup="true" aria-expanded="false">
        <i class="<c:out value='${headerControlIcon}'/>" aria-hidden="true"></i>
        <span class="btn-text"><c:out value="${headerControlLabel}" /></span>
      </button>
      <div class="user-menu-dropdown header-menu-dropdown">
        <div class="user-menu-section">
          <a class="user-menu-item" href="<c:out value='${headerControlDefaultWorkspaceUrl}'/>" title="Go to Home">
            <i class="${font:fas()} fa-home" aria-hidden="true"></i>
            <span class="user-menu-item-content"><span class="user-menu-item-label">Home</span></span>
          </a>
        </div>
        <div class="user-menu-section">
          <div class="user-menu-section-title"><c:out value="${headerControlLabel}" /></div>
          <c:forEach items="${headerControlWorkspaces}" var="workspace">
            <c:set var="isCurrentWorkspace" value="${workspace.id eq headerControlCurrentWorkspaceId}" />
            <a class="user-menu-item" href="<c:out value='${headerControlWorkspaceLaunchBase}'/>/workspace-launch?workspaceId=${workspace.id}"<c:if test="${isCurrentWorkspace}"> aria-current="true"</c:if>>
              <i class="${font:fas()} ${isCurrentWorkspace ? 'fa-check' : 'fa-cube'}" aria-hidden="true"></i>
              <span class="user-menu-item-content"><span class="user-menu-item-label"><c:out value="${workspace.name}" /></span></span>
              <%-- <c:if test="${isCurrentWorkspace}"><span class="user-menu-item-desc">Current</span></c:if> --%>
            </a>
          </c:forEach>
        </div>
      </div>
    </div>
  </c:when>
  <c:otherwise>
    <%-- Unknown Header Control Type --%>
  </c:otherwise>
</c:choose>