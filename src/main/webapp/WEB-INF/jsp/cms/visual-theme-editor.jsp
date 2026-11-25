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
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="property" uri="/WEB-INF/tlds/property-tags.tld" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<web:stylesheet package="spectrum" file="spectrum.css" />
<web:script package="spectrum" file="spectrum.js" />
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-workspace-editor.css" />
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
</g:compress>

<div id="visual-theme-editor-wrapper">
  <%@include file="../page_messages.jspf" %>
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Site home" /></a>
      <c:set var="editorName" value="Theme" />
      <c:set var="activeApp" value="theme" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>
    <div class="toolbar-section left">
      <label class="workspace-theme-selector">
        <span class="show-for-sr">Included theme</span>
        <select id="workspace-editor-included-theme" aria-label="Choose an included theme">
          <option value="">Included themes</option>
          <c:forEach items="${includedThemeList}" var="includedTheme" varStatus="themeStatus">
            <option value="included-${themeStatus.index}"><c:out value="${includedTheme.name}"/></option>
          </c:forEach>
        </select>
      </label>
      <label class="workspace-theme-selector">
        <span class="show-for-sr">Saved theme history</span>
        <select id="workspace-editor-theme-history" aria-label="Choose a saved theme from history">
          <option value=""><c:choose><c:when test="${empty themeHistoryList}">No saved history</c:when><c:otherwise>Theme history</c:otherwise></c:choose></option>
          <c:forEach items="${themeHistoryList}" var="savedTheme">
            <option value="history-${savedTheme.id}"><c:out value="${savedTheme.name}"/></option>
          </c:forEach>
        </select>
      </label>
      <button id="workspace-editor-discard" type="button" class="button tiny secondary no-gap radius" disabled>Discard</button>
      <button type="submit" form="workspace-editor-form" name="command" value="downloadThemeColors"
              class="button tiny secondary no-gap radius"><i class="${font:far()} fa-download"></i> Download</button>
      <form id="workspace-editor-theme-upload-form" method="post" enctype="multipart/form-data">
        <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
        <input type="hidden" name="token" value="${userSession.formToken}"/>
        <input type="hidden" name="command" value="uploadThemeColors"/>
        <label for="workspace-editor-theme-upload" class="button tiny secondary no-gap radius">
          <i class="${font:far()} fa-upload"></i> Upload
        </label>
        <input type="file" id="workspace-editor-theme-upload" name="file" accept="application/json,.json" class="show-for-sr"/>
      </form>
      <button id="workspace-editor-save" type="submit" form="workspace-editor-form" class="button tiny success no-gap radius" disabled><i class="${font:far()} fa-floppy-disk"></i> Save</button>
      <span id="workspace-editor-dirty-status" role="status" aria-live="polite">All changes saved</span>
    </div>
    <div class="titlebar-right">
      <%@include file="editor-site-exit.jspf" %>
    </div>
  </div>

  <div id="visual-theme-editor">
    <form id="workspace-editor-form" method="post">
      <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
      <input type="hidden" name="token" value="${userSession.formToken}"/>
      <input type="hidden" name="action" value="save"/>
      <input type="hidden" id="workspace-editor-theme-snapshot-name" name="themeSnapshotBaseName"
             value="<c:out value="${empty uploadedThemeName ? 'Custom Theme' : uploadedThemeName}"/>"/>
      <%@include file="visual-workspace-editor-preview.jspf" %>
    </form>
  </div>
</div>

<c:forEach items="${includedThemeList}" var="includedTheme" varStatus="themeStatus">
  <template id="workspace-theme-included-${themeStatus.index}" data-workspace-theme-values>
    <c:forEach items="${includedTheme.siteProperties}" var="themeProperty">
      <span data-theme-property-name="<c:out value="${themeProperty.name}"/>" data-theme-property-value="<c:out value="${themeProperty.value}"/>"></span>
    </c:forEach>
  </template>
</c:forEach>
<c:forEach items="${themeHistoryList}" var="savedTheme">
  <template id="workspace-theme-history-${savedTheme.id}" data-workspace-theme-values>
    <c:forEach items="${savedTheme.siteProperties}" var="themeProperty">
      <span data-theme-property-name="<c:out value="${themeProperty.name}"/>" data-theme-property-value="<c:out value="${themeProperty.value}"/>"></span>
    </c:forEach>
  </template>
</c:forEach>

<script src="${ctx}/javascript/widgets/editor/visual-workspace-editor-state.js"></script>
<script src="${ctx}/javascript/widgets/editor/visual-workspace-editor-preview.js"></script>
<script src="${ctx}/javascript/widgets/editor/visual-workspace-editor-properties.js"></script>
<script src="${ctx}/javascript/widgets/editor/visual-workspace-editor-actions.js"></script>
<script>
  document.getElementById("workspace-editor-theme-upload").addEventListener("change", function() {
    document.getElementById("workspace-editor-theme-upload-form").submit();
  });
</script>
<script>setupEditorAppSwitcher();</script>
