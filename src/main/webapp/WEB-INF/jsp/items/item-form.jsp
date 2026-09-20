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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="collection" class="com.simisinc.platform.domain.model.items.Collection" scope="request"/>
<jsp:useBean id="item" class="com.simisinc.platform.domain.model.items.Item" scope="request"/>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/platform-editor.css" />
</g:compress>
<web:script package="hugerte" file="hugerte.min.js" />
<script>
  hugerte.init({
    selector: '.html-field',
    branding: false,
    width: '100%',
    height: 300,
    resize: true,
    menubar: false,
    relative_urls: false,
    convert_urls: true,
    convert_unsafe_embeds: true,
    sandbox_iframes: true,
    content_css: [
      '${ctx}/css/${font:fontawesome()}/css/all.min.css',
      '${ctx}/css/${font:fontawesome()}/css/v4-shims.min.css',
      '${ctx}/css/platform.css?v=${VERSION}'
      <c:if test="${!empty includeGlobalStylesheet}">,'${ctx}/css/custom/stylesheet.css?v=${includeGlobalStylesheetLastModified}'</c:if>
      <c:if test="${!empty includeStylesheet}">,'${ctx}/css/custom/stylesheet${includeStylesheet}.css?v=${includeStylesheetLastModified}'</c:if>
    ],
    browser_spellcheck: true,
    noneditable_class: 'mceNonEditable',
    plugins: 'advlist autolink lists link image charmap preview anchor searchreplace visualblocks code insertdatetime media table wordcount contentblock diagram templates fullscreen',
    toolbar: 
    [
      'link image media diagram table fontawesome | contentblock templatesMenu | visualblocks  code | undo redo | fullscreen',
      'blocks | bold italic backcolor | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | hr | anchor | removeformat'
    ],
    toolbar_mode: 'wrap',
    external_plugins: {
       "contentblock": "${ctx}/javascript/tinymce-plugins/contentblock/plugin.js?v=${VERSION}",
       "diagram": "${ctx}/javascript/tinymce-plugins/diagram/plugin.js?v=${VERSION}",
       "templates": "${ctx}/javascript/tinymce-plugins/templates/plugin.js?v=${VERSION}"
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
    file_picker_callback: function (callback, value, meta) {
      FileBrowser(value, meta.filetype, function (fileUrl) {
        callback(fileUrl);
      });
    },
    images_upload_url: '${ctx}/image-upload?widget=imageUpload1&token=${userSession.formToken}',
    image_uploadtab: true,
    paste_data_images: true,
    automatic_uploads: true
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
    const instanceApi = hugerte.activeEditor.windowManager.openUrl({
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
<%-- Handle item image uploads --%>
<script>
  function SavePhoto(e) {
    var file = e.files[0]; // similar to: document.getElementById("file").files[0]
    var formData = new FormData();
    formData.append("file", file);
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if (this.readyState === 4) {
        if (this.status === 200) {
          var fileData = JSON.parse(this.responseText);
          document.getElementById("imageUrl").value = fileData.location;
          document.getElementById("imageUrlPreview").src = fileData.location;
        } else {
          document.getElementById("imageFile").value = "";
          alert('There was an error with the file. Make sure to use a .jpg or .png');
        }
      }
    };
    xhr.open("POST", '${ctx}/image-upload?widget=imageUpload1&token=${userSession.formToken}');
    xhr.send(formData);
  }
</script>
<form method="post" autocomplete="off">
  <%-- Required by controller --%>
  <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <%-- Form values --%>
  <input type="hidden" name="id" value="${item.id}"/>
  <input type="hidden" name="returnPage" value="${returnPage}"/>
  <%-- Title and Message block --%>
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>
  <%-- Form Content --%>
  <label>Name
    <input type="text" placeholder="Give it a name..." name="name" value="<c:out value="${item.name}"/>">
  </label>
  <label>Summary
    <textarea placeholder="Write an optional summary..." id="summary" name="summary" class="html-field" style="height:180px"><c:out value="${item.summary}"/></textarea>
  </label>
  <div class="button-container">
    <input type="submit" class="button radius success expanded" value="Save"/>
  </div>
</form>