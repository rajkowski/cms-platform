<%--
  ~ Copyright 2026 Matt Rajkowski (https://www.github.com/rajkowski)
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
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<g:compress><link rel="stylesheet" href="${ctx}/css/visual-content-editor.css" /></g:compress>
<g:compress><link rel="stylesheet" href="${ctx}/css/visual-forms-editor.css" /></g:compress>
<div class="forms-editor">
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Home" /></a>
      <c:set var="editorName" value="Forms" />
      <c:set var="activeApp" value="forms" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>
    <div class="toolbar-section left"><a class="button tiny success no-gap radius" href="${ctx}/admin/visual-forms-editor?new=true"><i class="${font:far()} fa-plus"></i> New Form</a></div>
    <div class="titlebar-right"><%@include file="editor-site-exit.jspf" %></div>
  </div>
  <%@include file="../page_messages.jspf" %>
  <div class="forms-editor-layout">
    <aside class="forms-sidebar" aria-label="Forms">
      <h2>Forms</h2>
      <c:if test="${empty forms}"><p>No forms yet.</p></c:if>
      <ul>
        <c:forEach items="${forms}" var="item">
          <li><a class="${item.id == form.id ? 'selected' : ''}" href="${ctx}/admin/visual-forms-editor?formId=${item.id}"><c:out value="${item.name}"/><c:if test="${!item.enabled}"><span>Disabled</span></c:if></a></li>
        </c:forEach>
      </ul>
    </aside>
    <main class="forms-main">
      <c:choose>
        <c:when test="${empty form and param['new'] ne 'true'}">
          <div class="forms-empty"><h1>Forms</h1><p>Select a form or create a new one.</p><a class="button primary" href="${ctx}/admin/visual-forms-editor?new=true">Create Form</a></div>
        </c:when>
        <c:otherwise>
          <c:if test="${empty formValues}"><c:set var="formValues" value="${form}"/></c:if>
          <div class="forms-heading"><div><h1><c:choose><c:when test="${empty form}">New Form</c:when><c:otherwise><c:out value="${form.name}"/></c:otherwise></c:choose></h1><c:if test="${not empty form}"><span>Form ID: <code><c:out value="${form.uniqueId}"/></code></span></c:if></div></div>
          <form method="post" class="forms-properties">
            <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/><input type="hidden" name="token" value="${userSession.formToken}"/>
            <input type="hidden" name="action" value="saveForm"/><input type="hidden" name="formId" value="${empty form ? -1 : form.id}"/>
            <h2>Form Settings</h2>
            <div class="forms-grid">
              <label>Name <input name="name" value="<c:out value='${formValues.name}'/>" required maxlength="255"/></label>
              <label>Title <input name="title" value="<c:out value='${formValues.title}'/>" maxlength="255"/></label>
              <label class="wide">Subtitle <input name="subtitle" value="<c:out value='${formValues.subtitle}'/>"/></label>
              <label>Button label <input name="buttonName" value="<c:out value='${formValues.buttonName}'/>" placeholder="Submit" maxlength="100"/></label>
              <label>Email submissions to <input name="emailTo" value="<c:out value='${formValues.emailTo}'/>" placeholder="name@example.com"/></label>
              <label>Success title <input name="successTitle" value="<c:out value='${formValues.successTitle}'/>"/></label>
              <label>Success message <textarea name="successMessage" rows="2"><c:out value="${formValues.successMessage}"/></textarea></label>
            </div>
            <div class="forms-toggles">
              <label><input type="checkbox" name="enabled" value="true" ${empty formValues or formValues.enabled ? 'checked' : ''}/> Enabled</label>
              <label><input type="checkbox" name="useCaptcha" value="true" ${formValues.useCaptcha ? 'checked' : ''}/> Require CAPTCHA</label>
              <label><input type="checkbox" name="checkForSpam" value="true" ${empty formValues or formValues.checkForSpam ? 'checked' : ''}/> Check for spam</label>
            </div>
            <button type="submit" class="button primary"><i class="${font:far()} fa-floppy-disk"></i> Save Form</button>
          </form>
          <c:if test="${not empty form}">
            <section class="forms-fields">
              <h2>Fields</h2>
              <c:if test="${empty fields}"><p>No fields yet.</p></c:if>
              <ul class="forms-field-list">
                <c:forEach items="${fields}" var="item">
                  <li><div><strong><c:out value="${item.label}"/></strong><span><c:out value="${item.name}"/> &middot; <c:out value="${item.type}"/><c:if test="${item.required}"> &middot; Required</c:if></span></div>
                    <a href="${ctx}/admin/visual-forms-editor?formId=${form.id}&amp;fieldId=${item.id}#field-editor" class="button hollow tiny" title="Edit field" aria-label="Edit field"><i class="${font:far()} fa-pen"></i></a>
                    <form method="post" onsubmit="return confirm('Delete this field?')"><input type="hidden" name="widget" value="${widgetContext.uniqueId}"/><input type="hidden" name="token" value="${userSession.formToken}"/><input type="hidden" name="action" value="deleteField"/><input type="hidden" name="formId" value="${form.id}"/><input type="hidden" name="fieldId" value="${item.id}"/><button class="button hollow alert tiny" title="Delete field" aria-label="Delete field"><i class="${font:far()} fa-trash"></i></button></form>
                  </li>
                </c:forEach>
              </ul>
              <form method="post" id="field-editor" class="forms-field-editor">
                <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/><input type="hidden" name="token" value="${userSession.formToken}"/>
                <input type="hidden" name="action" value="saveField"/><input type="hidden" name="formId" value="${form.id}"/><input type="hidden" name="fieldId" value="${empty field ? -1 : field.id}"/>
                <h3>${empty field or field.id lt 0 ? 'Add Field' : 'Edit Field'}</h3>
                <div class="forms-grid">
                  <label>Label <input name="label" value="<c:out value='${field.label}'/>" maxlength="255" required/></label>
                  <label>Field name <input name="fieldName" value="<c:out value='${field.name}'/>" maxlength="255" placeholder="Generated from label"/></label>
                  <label>Type <select name="type"><option value="text" ${field.type == 'text' ? 'selected' : ''}>Text</option><option value="email" ${field.type == 'email' ? 'selected' : ''}>Email</option><option value="textarea" ${field.type == 'textarea' ? 'selected' : ''}>Paragraph</option><option value="select" ${field.type == 'select' ? 'selected' : ''}>Select</option><option value="checkbox" ${field.type == 'checkbox' ? 'selected' : ''}>Checkbox</option><option value="date" ${field.type == 'date' ? 'selected' : ''}>Date</option></select></label>
                  <label>Placeholder <input name="placeholder" value="<c:out value='${field.placeholder}'/>" maxlength="255"/></label>
                  <label>Default value <input name="defaultValue" value="<c:out value='${field.defaultValue}'/>" maxlength="255"/></label>
                  <label>Options (one per line, or value=label) <textarea name="options" rows="3"><c:out value="${fieldOptions}"/></textarea></label>
                </div>
                <label><input type="checkbox" name="required" value="true" ${field.required ? 'checked' : ''}/> Required</label>
                <div><button class="button primary"><i class="${font:far()} fa-floppy-disk"></i> ${empty field or field.id lt 0 ? 'Add Field' : 'Save Field'}</button> <c:if test="${not empty field}"><a class="button secondary" href="${ctx}/admin/visual-forms-editor?formId=${form.id}#field-editor">Cancel</a></c:if></div>
              </form>
            </section>
            <form method="post" class="forms-delete" onsubmit="return confirm('Delete this form and all its fields?')"><input type="hidden" name="widget" value="${widgetContext.uniqueId}"/><input type="hidden" name="token" value="${userSession.formToken}"/><input type="hidden" name="action" value="deleteForm"/><input type="hidden" name="formId" value="${form.id}"/><button class="button hollow alert"><i class="${font:far()} fa-trash"></i> Delete Form</button></form>
          </c:if>
        </c:otherwise>
      </c:choose>
    </main>
  </div>
</div>
<g:compress><script src="${ctx}/javascript/apps-menu-controller.js"></script></g:compress>
<script>setupEditorAppSwitcher();</script>
