<%--
  ~ Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
  ~ Copyright 2022 SimIS Inc.
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
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="content" class="com.simisinc.platform.domain.model.cms.Content" scope="request"/>
<jsp:useBean id="isDraft" class="java.lang.String" scope="request"/>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/platform-editor.css" />
</g:compress>
<web:script package="tinymce" file="tinymce.min.js" />
<script>
  $(window).on('resize', function () {
    setTimeout(function () {
      var container = document.getElementsByClassName("tox-tinymce")[0];
      var rect = container.getBoundingClientRect(),
        scrollLeft = window.pageXOffset || document.documentElement.scrollLeft,
        scrollTop = window.pageYOffset || document.documentElement.scrollTop;
      var newHeight = $(window).height() - Math.round(rect.top + scrollTop);
      $('.tox-tinymce').height(newHeight);
    }, 100);
  });

  tinymce.init({
    selector: '.html-field',
    branding: false,
    width: '100%',
    height: '100%',
    resize: false,
    setup: function (ed) {
      ed.on('init', function(args) {
        $(window).trigger('resize');
      });
    },
    menubar: false,
    relative_urls : false,
    convert_urls : true,
    convert_unsafe_embeds: true, <%-- CVE-2024-29203 fix --%>
    sandbox_iframes: true, <%-- CVE-2024-29203 fix --%>
    content_css: [
      '${ctx}/css/${font:fontawesome()}/css/all.min.css',
      '${ctx}/css/${font:fontawesome()}/css/v4-shims.min.css',
      '${ctx}/css/platform.css?v=${includeGlobalStylesheetLastModified}'
      <c:if test="${!empty includeGlobalStylesheet}">,'${ctx}/css/custom/stylesheet.css?v=${includeGlobalStylesheetLastModified}'</c:if>
      <c:if test="${!empty includeStylesheet}">,'${ctx}/css/custom/stylesheet${includeStylesheet}.css?v=${includeStylesheetLastModified}'</c:if>
    ],
    content_style: "body.platform-content { overflow: auto !important; }",
    body_class: 'web-content platform-content',
    noneditable_class: 'tinymce-noedit',
    browser_spellcheck: true,
    plugins: 'advlist autolink lists link image charmap preview anchor searchreplace visualblocks code media table wordcount fontawesome contentblock diagram versionhistory cards notes panels templates imageversion',
    contextmenu: 'notesContext link',
    contextmenu_never_use_native: true,
    toolbar: 
      [
        'link image media diagram table fontawesome | notesMenu cardsMenu panelsMenu | contentblock templatesMenu | visualblocks  code | versionhistory',
        'blocks | bold italic backcolor | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | hr | anchor | removeformat | undo redo'
      ],
    toolbar_mode: 'wrap',
    external_plugins: {
        "contentblock": "${ctx}/javascript/tinymce-plugins/contentblock/plugin.js?v=${includeGlobalStylesheetLastModified}",
        "fontawesome": "${ctx}/javascript/tinymce-plugins/fontawesome/plugin.min.js?v=${includeGlobalStylesheetLastModified}",
        "diagram": "${ctx}/javascript/tinymce-plugins/diagram/plugin.js?v=${includeGlobalStylesheetLastModified}",
        "versionhistory": "${ctx}/javascript/tinymce-plugins/versionhistory/plugin.js?v=${includeGlobalStylesheetLastModified}",
        "cards": "${ctx}/javascript/tinymce-plugins/cards/plugin.js?v=${includeGlobalStylesheetLastModified}",
        "notes": "${ctx}/javascript/tinymce-plugins/notes/plugin.js?v=${includeGlobalStylesheetLastModified}",
        "panels": "${ctx}/javascript/tinymce-plugins/panels/plugin.js?v=${includeGlobalStylesheetLastModified}",
        "templates": "${ctx}/javascript/tinymce-plugins/templates/plugin.js?v=${includeGlobalStylesheetLastModified}",
        "imageversion": "${ctx}/javascript/tinymce-plugins/imageversion/plugin.js?v=${includeGlobalStylesheetLastModified}"
    },
    image_class_list: [
      {title: 'None', value: ''},
      {title: 'Image Left/Wrap Text Right', value: 'image-left'},
      {title: 'Image Right/Wrap Text left', value: 'image-right'},
      {title: 'Image Center On Line', value: 'image-center'}
    ],
    link_class_list: [
      {title: 'None', value: ''},
      {title: 'Button', value: 'button'},
      {title: 'Button Primary', value: 'button primary'},
      {title: 'Button Primary Radius', value: 'button primary radius'},
      {title: 'Button Primary Round', value: 'button primary round'},
      {title: 'Button Secondary', value: 'button secondary'},
      {title: 'Button Secondary Radius', value: 'button secondary radius'},
      {title: 'Button Secondary Round', value: 'button secondary round'},
      {title: 'Button Box', value: 'button box'},
      {title: 'Button Box Radius', value: 'button box radius'},
      {title: 'Button Box Round', value: 'button box round'},
      {title: 'Call to Action', value: 'button call-to-action'}
    ],
    extended_valid_elements: 'span[*]',
    file_picker_types: 'file image media',
    // link_default_target: '_blank',
    file_picker_callback: function (callback, value, meta) {
        FileBrowser(value, meta.filetype, function (fileUrl) {
            callback(fileUrl);
        });
    },
    images_upload_handler: function (blobInfo, progress) {
      return tinymce.activeEditor.plugins.imageversion.uploadImageFromEditor(blobInfo, progress);
    },
    image_version_upload_url: '${ctx}/image-upload?widget=imageUpload1&token=${userSession.formToken}',
    automatic_uploads: true
    // paste_word_valid_elements: "p,a,b,strong,i,em,h1,h2,h3,h4,h5,ol,ul,li"
    // paste_retain_style_properties: "color"
    // paste_as_text: true
  });

  $(function () {
    const form = document.querySelector('form[method="post"]');
    if (!form) {
      return;
    }

    let processingSubmit = false;
    form.addEventListener('submit', async function (event) {
      if (processingSubmit) {
        return;
      }

      const submitter = event.submitter;
      const actionValue = submitter && submitter.name === 'save' ? submitter.value : '';
      const isPublishAction = actionValue === 'Publish Immediately';

      const imageVersionPlugin = tinymce.activeEditor && tinymce.activeEditor.plugins.imageversion;
      if (!imageVersionPlugin || !imageVersionPlugin.hasPendingUploads()) {
        return;
      }

      event.preventDefault();
      processingSubmit = true;
      try {
        await imageVersionPlugin.flushPendingUploads(isPublishAction);
        if (submitter && submitter.name) {
          const hidden = document.createElement('input');
          hidden.type = 'hidden';
          hidden.name = submitter.name;
          hidden.value = submitter.value;
          form.appendChild(hidden);
        }
        form.submit();
      } catch (e) {
        processingSubmit = false;
        alert('Unable to process pending image version uploads. Please try again.');
      }
    });
  });

  function FileBrowser(value, type, callback) {
    // type will be: file, image, media
    var cmsType = 'image';
    if (type === 'media') {
      cmsType = 'video';
    } else if (type === 'file') {
      cmsType = 'file';
    }
    var cmsURL = '${ctx}/' + cmsType + '-browser';
    const instanceApi = tinyMCE.activeEditor.windowManager.openUrl({
        title: 'Browser',
        url: cmsURL,
        width: 850,
        height: 650,
        onMessage: function(dialogApi, details) {
            callback(details.content);
            instanceApi.close();
        }
    });
    return false;
  }
</script>
<%@include file="../page_messages.jspf" %>
<div id="content-editor-container" class="panel-container">
<form method="post">
  <%-- Required by controller --%>
  <input type="hidden" name="widget" value="${widgetContext.uniqueId}" />
  <input type="hidden" name="token" value="${userSession.formToken}" />
  <%-- Form Content --%>
  <input type="hidden" name="uniqueId" value="${content.uniqueId}" />
  <input type="hidden" name="returnPage" value="${returnPage}" />

  <div id="middle-panel-header" class="panel-header">
    <div class="titlebar-left">
      <a href="/" class="header-logo">
        <c:choose>
          <c:when test="${!empty sitePropertyMap['site.logo']}">
            <img class="logo-light" src="${sitePropertyMap['site.logo']}" alt="<c:out value="${sitePropertyMap['site.name']}"/>" />
            <c:choose>
              <c:when test="${!empty sitePropertyMap['site.logo.white']}">
                <img class="logo-dark" src="${sitePropertyMap['site.logo.white']}" alt="<c:out value="${sitePropertyMap['site.name']}"/>" />
              </c:when>
              <c:when test="${!empty sitePropertyMap['site.logo.mixed']}">
                <img class="logo-dark" src="${sitePropertyMap['site.logo.mixed']}" alt="<c:out value="${sitePropertyMap['site.name']}"/>" />
              </c:when>
              <c:otherwise>
                <img class="logo-dark" src="${sitePropertyMap['site.logo']}" alt="<c:out value="${sitePropertyMap['site.name']}"/>" />
              </c:otherwise>
            </c:choose>
          </c:when>
          <c:otherwise>
            <i class="fas fa-cube"></i>
            <span><c:out value="${sitePropertyMap['site.name']}"/></span>
          </c:otherwise>
        </c:choose>
      </a>
    </div>
    <h3 id="middle-panel-title">
      <i id="middle-panel-icon" class="far"></i>
      <span id="middle-panel-title-text"><c:out value="${content.uniqueId}" />
        <c:if test="${isDraft eq 'true'}">
          <span class="label warning">Draft</span>
        </c:if>
      </span>
    </h3>
    <div id="middle-panel-tools">
      <div id="content-tools" class="panel-tools">
        <a href="${returnPage}" class="button tiny radius secondary no-gap">Cancel</a>
        <c:choose>
          <c:when test="${content.id eq -1}">
            <input type="submit" class="button tiny radius primary no-gap" name="save" value="Save" />
          </c:when>
          <c:otherwise>
            <c:if test="${isDraft eq 'true'}">
              <input type="submit" class="button tiny radius alert no-gap" name="save" value="Remove this Draft" />
            </c:if>
            <input type="submit" class="button tiny radius warning no-gap" name="save" value="Save as Draft" />
            <input type="submit" class="button tiny radius success no-gap" name="save" value="Publish Immediately" />
          </c:otherwise>
        </c:choose>
      </div>    
    </div>
  </div>
  <div id="content-editor">
    <textarea name="content" class="html-field" data-content-id="${content.id}" data-unique-id="${content.uniqueId}"><c:out value="${contentHtml}"/><p>&nbsp;</p></textarea>
  </div>
</form>
</div>
