<%--
  ~ Copyright 2026 Matt Rajkowski
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
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="workspaceList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="ownerList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="databaseList" class="java.util.ArrayList" scope="request"/>
<c:if test="${!empty title}">
  <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
</c:if>
<%@include file="../page_messages.jspf" %>
<div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; gap: 1rem; flex-wrap: wrap;">
  <button class="button small primary radius workspace-add" data-open="workspaceFormReveal"><i class="fa fa-plus"></i> New Workspace</button>
</div>
<c:choose>
  <c:when test="${not empty workspaceList}">
    <div class="grid-x grid-margin-x small-up-1 medium-up-2 large-up-3">
      <c:forEach items="${workspaceList}" var="workspace">
        <div class="cell">
          <div class="card" style="height: 100%; border: 1px solid #dfe3e8; border-radius: 10px; overflow: hidden;">
            <div class="card-section" style="padding: 1rem;">
              <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 0.75rem; margin-bottom: 0.75rem;">
                <div>
                  <h5 style="margin: 0 0 0.2rem 0;"><c:out value="${workspace.name}" /></h5>
                  <c:if test="${!empty workspace.department}">
                    <small class="subheader"><c:out value="${workspace.department}" /></small>
                  </c:if>
                </div>
                <span style="white-space: nowrap;">
                  <c:choose>
                    <c:when test="${workspace.active}"><span class="label success">Active</span></c:when>
                    <c:otherwise><span class="label alert">Inactive</span></c:otherwise>
                  </c:choose>
                </span>
              </div>
              <div style="font-size: 0.9rem; line-height: 1.6; color: #4a4a4a;">
                <div><strong>URL:</strong>
                  <c:if test="${!empty workspace.siteUrl}">
                    <a href="${workspace.siteUrl}" target="_blank" rel="noopener noreferrer"><c:out value="${workspace.siteUrl}" /></a>
                  </c:if>
                  <c:if test="${empty workspace.siteUrl}"><span class="subheader">Not configured</span></c:if>
                </div>
                <div><strong>Domain:</strong> <c:out value="${empty workspace.canonicalDomain ? 'Not configured' : workspace.canonicalDomain}" /></div>
                <div><strong>Owner:</strong>
                  <c:if test="${!empty workspace.ownerName}">
                    <c:out value="${workspace.ownerName}" />
                  </c:if>
                  <c:if test="${empty workspace.ownerName}"><span class="subheader">Workspace administrator</span></c:if>
                </div>
                <c:if test="${!empty workspace.ownerEmail}">
                  <div><strong>Email:</strong> <c:out value="${workspace.ownerEmail}" /></div>
                </c:if>
              </div>
            </div>
            <div class="card-divider" style="padding: 0.75rem 1rem; display: flex; justify-content: space-between; align-items: center; gap: 0.5rem;">
              <small class="subheader">
                <c:if test="${!empty workspace.created}">
                  Created <fmt:formatDate pattern="yyyy-MM-dd" value="${workspace.created}" />
                </c:if>
              </small>
              <%-- <a href="${ctx}/admin/workspace-details?workspaceId=${workspace.id}" class="button small secondary no-gap">Details</a> --%>
              <a href="${ctx}/workspace-launch?workspace=${workspace.uniqueId}" class="button small secondary no-gap" target="_blank" rel="noopener noreferrer">Launch</a>
            </div>
          </div>
        </div>
      </c:forEach>
    </div>
  </c:when>
  <c:otherwise>
    <div class="callout secondary">
      No workspaces were found.
    </div>
  </c:otherwise>
</c:choose>
<div class="reveal small" id="workspaceFormReveal" data-reveal data-close-on-click="false" data-animation-in="slide-in-down fast">
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
  <h4 id="workspaceFormTitle">New Workspace</h4>
  <form id="workspaceForm" method="post" autocomplete="off">
    <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
    <input type="hidden" name="token" value="${userSession.formToken}"/>
    <input type="hidden" name="command" id="workspaceCommand" value="saveWorkspace" />
    <label>Name <span class="required">*</span>
      <input type="text" name="name" id="workspaceName" value="" required>
    </label>
    <label>Unique Id <span class="required">*</span>
      <input type="text" name="uniqueId" id="workspaceUniqueId" value="" required minlength="3" maxlength="63"
             pattern="[a-z0-9][a-z0-9\-]{1,61}[a-z0-9]" aria-describedby="workspaceUniqueIdHelp">
    </label>
    <p class="help-text" id="workspaceUniqueIdHelp">Used as the subdomain and folder name. Lowercase letters, numbers, and hyphens only.</p>
    <label>Description
      <textarea name="description" id="workspaceDescription" rows="3"></textarea>
    </label>
    <label>Department
      <input type="text" name="department" id="workspaceDepartment" value="">
    </label>
    <label>Owner
      <input type="text" id="workspaceOwnerSearch" placeholder="Search by name or email..." autocomplete="off">
      <input type="hidden" name="ownerId" id="workspaceOwnerId" value="">
    </label>
    <label>Pool / Database <span class="required">*</span>
      <select name="poolDatabaseId" id="workspacePoolDatabaseId" required>
        <option value="">Select a database...</option>
        <c:forEach items="${databaseList}" var="database">
          <option value="${database.id}"><c:out value="${database.label}" /></option>
        </c:forEach>
      </select>
    </label>
    <div class="button-container">
      <button type="submit" class="button radius expanded" id="workspaceSubmit">Create Workspace</button>
      <p id="workspaceProgress" class="text-center" style="display: none;"><i class="fa fa-spinner fa-spin"></i> Creating workspace, this might take several seconds to become available...</p>
    </div>
  </form>
</div>
<script>
  var workspaceOwners = [
    <c:forEach items="${ownerList}" var="owner" varStatus="status">
    ["<c:out value="${owner.fullName}" /> (<c:out value="${owner.email}" />)", "${owner.id}"]<c:if test="${!status.last}">,</c:if>
    </c:forEach>
  ];
  document.getElementById("workspaceForm").addEventListener("submit", function () {
    var submit = document.getElementById("workspaceSubmit");
    submit.disabled = true;
    submit.innerHTML = 'Please wait...';
    document.getElementById("workspaceProgress").style.display = "block";
  });
  var workspaceOwnerSearch = document.getElementById("workspaceOwnerSearch");
  var workspaceOwnerId = document.getElementById("workspaceOwnerId");
  workspaceOwnerSearch.addEventListener("input", function () {
    workspaceOwnerId.value = "";
  });
  new autoComplete({
    selector: '#workspaceOwnerSearch',
    minChars: 1,
    source: function (term, response) {
      var t = term.toLowerCase();
      response(workspaceOwners.filter(function (o) {
        return o[0].toLowerCase().indexOf(t) > -1;
      }));
    },
    renderItem: function (item, search) {
      search = search.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
      var re = new RegExp("(" + search.split(' ').join('|') + ")", "gi");
      var div = document.createElement('div');
      div.className = 'autocomplete-suggestion';
      div.setAttribute('data-name', item[0]);
      div.setAttribute('data-id', item[1]);
      div.setAttribute('data-val', search);
      div.textContent = item[0];
      div.innerHTML = div.innerHTML.replace(re, "<b>$1</b>");
      return div.outerHTML;
    },
    onSelect: function (e, term, item) {
      workspaceOwnerSearch.value = item.getAttribute('data-name');
      workspaceOwnerId.value = item.getAttribute('data-id');
    }
  });
</script>
