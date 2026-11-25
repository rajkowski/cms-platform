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
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-workspace-editor.css" />
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
</g:compress>

<div id="visual-workspace-editor-wrapper">
  <%@include file="../page_messages.jspf" %>
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Site home" /></a>
      <c:set var="editorName" value="Workspace" />
      <c:set var="activeApp" value="workspace" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>
    <div class="toolbar-section left">
      <span id="workspace-editor-dirty-status" role="status" aria-live="polite">All changes saved</span>
      <button id="workspace-editor-discard" type="button" class="button tiny secondary no-gap radius" disabled>Discard</button>
      <button id="workspace-editor-save" type="submit" form="workspace-editor-form" class="button tiny success no-gap radius" disabled><i class="${font:far()} fa-floppy-disk"></i> Save</button>
    </div>
    <div class="titlebar-right">
      <%@include file="editor-site-exit.jspf" %>
    </div>
  </div>

  <div id="visual-workspace-editor">
    <form id="workspace-editor-form" method="post">
      <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
      <input type="hidden" name="token" value="${userSession.formToken}"/>
      <input type="hidden" name="action" value="save"/>
      <%@include file="visual-workspace-editor-properties.jspf" %>
    </form>
  </div>
</div>

<script src="${ctx}/javascript/widgets/editor/visual-workspace-editor-state.js"></script>
<script src="${ctx}/javascript/widgets/editor/visual-workspace-editor-properties.js"></script>
<script src="${ctx}/javascript/widgets/editor/visual-workspace-editor-actions.js"></script>
<script>setupEditorAppSwitcher();</script>
