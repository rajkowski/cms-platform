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
<%@ page import="static com.zeroio.platform.ApplicationInfo.VERSION" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="collection" class="com.simisinc.platform.domain.model.items.Collection" scope="request"/>
<jsp:useBean id="item" class="com.simisinc.platform.domain.model.items.Item" scope="request"/>
<jsp:useBean id="categoryList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="cancelUrl" class="java.lang.String" scope="request"/>
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
<c:set var="showAll" value="${empty allowedFields}"/>
<form method="post" autocomplete="off">
  <%-- Required by controller --%>
  <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <%-- Form values --%>
  <input type="hidden" name="id" value="${item.id}"/>
  <c:if test="${!empty returnPage}">
    <input type="hidden" name="returnPage" value="${returnPage}"/>
  </c:if>
  <%-- Title and Message block --%>
  <h2><em><c:out value="${collection.name}" /></em></h2>
  <c:if test="${!empty title}">
    <p><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}"/></p>
  </c:if>
  <c:if test="${item.id ne -1}">
    <c:if test="${userSession.hasRole('admin') || userSession.hasRole('data-manager')}">
      <div><a class="button radius secondary small no-gap" data-open="itemVersionHistoryReveal${widgetContext.uniqueId}"><i class="fas fa-rotate-left"></i> View History</a></div>
    </c:if>
  </c:if>
  <%@include file="../page_messages.jspf" %>
  <%-- Form Content --%>
  <%--
  These input types create a text field:
  text, date, datetime, datetime-local, email, month, number, password, search, tel, time, url, and week. --%>
  <c:if test="${showAll or allowedFields.contains('name') or allowedFields.contains('summary') or allowedFields.contains('description') or allowedFields.contains('tags')}">
    <h3 class="margin-top-30">Details</h3>
    <div class="grid-container">
      <div class="grid-x grid-padding-x">
        <div class="small-12 cell">
          <c:if test="${showAll or allowedFields.contains('name')}">
          <label class="margin-top-20">${not empty fieldLabels['name'] ? fieldLabels['name'] : 'Name'}
            <input type="text" placeholder="Give it a name..." name="name" value="<c:out value="${item.name}"/>">
          </label>
          </c:if>
          <c:if test="${showAll or allowedFields.contains('summary')}">
          <label class="margin-top-20">
            ${not empty fieldLabels['summary'] ? fieldLabels['summary'] : 'Summary'}
            <textarea placeholder="optional description" name="summary" style="height:180px"><c:out value="${item.summary}"/></textarea>
          </label>
          </c:if>
          <c:if test="${showAll or allowedFields.contains('description')}">
          <p>
            <label class="margin-top-20">
              ${not empty fieldLabels['description'] ? fieldLabels['description'] : 'Description'}
              <textarea id="description" name="description" class="html-field"><c:out value="${item.description}"/></textarea>
            </label>
          </p>
          </c:if>
          <c:if test="${showAll or allowedFields.contains('tags')}">
          <label class="margin-top-20">${not empty fieldLabels['tags'] ? fieldLabels['tags'] : 'Tags'}
            <small>Type a tag and press Tab or Enter, or select from suggestions</small>
            <input type="hidden" id="tags-hidden" name="tags" value="<c:out value='${fn:join(item.tags, ",")}'/>" />
            <div id="tag-input-container" class="tag-input-container">
              <div id="tag-chips"></div>
              <input type="text" id="tag-input"
                    placeholder="Add tags..."
                    autocomplete="off"/>
              <div id="tag-dropdown" class="tag-dropdown" style="display:none;"></div>
            </div>
          </label>
          </c:if>
        </div>
      </div>
    </div>
  </c:if>

  <c:if test="${(!empty categoryList) and (showAll or allowedFields.contains('categoryId'))}">
    <h3 class="margin-top-40 margin-bottom-20">Categories</h3>
    <div class="grid-container">
      <div class="grid-x grid-padding-x">
        <div class="small-12 cell">
          <span class="input-group-label">Primary Category</span>
          <select class="input-group-field" id="categoryId" name="categoryId">
            <option value="">Make a selection...</option>
            <c:forEach items="${categoryList}" var="category">
              <option value="${category.id}"<c:if test="${item.categoryId eq category.id}"> selected</c:if>><c:out value="${category.name}" /></option>
            </c:forEach>
          </select>
        </div>
      </div>
    </div>
    <div class="grid-container margin-top-20">
      <div class="grid-x grid-padding-x">
        <div class="small-12 cell">
          <div class="input-container">
            <span class="input-group-label">Additional Categories</span>
            <c:forEach items="${categoryList}" var="category">
              <%--                <c:if test="${fn:contains(item.categoryIdList, category.id)}"> checked</c:if>--%>
              <c:set var="contains" value="false" />
              <c:forEach var="thisCategoryId" items="${item.categoryIdList}">
                <c:if test="${thisCategoryId eq category.id}">
                  <c:set var="contains" value="true" />
                </c:if>
              </c:forEach>
              <input id="categoryId${category.id}" type="checkbox" name="categoryId${category.id}" value="${category.id}"<c:if test="${contains eq 'true'}"> checked</c:if> /><label for="categoryId${category.id}"><c:out value="${category.name}" /></label>
            </c:forEach>
          </div>
        </div>
      </div>
    </div>
  </c:if>

  <c:if test="${showAll or allowedFields.contains('url') or allowedFields.contains('urlText') or allowedFields.contains('imageUrl') or allowedFields.contains('keywords')}">
    <h3 class="margin-top-40">Reference</h3>
    <c:if test="${showAll or allowedFields.contains('url') or allowedFields.contains('urlText')}">
      <div class="grid-container">
        <div class="grid-x grid-padding-x">
          <div class="small-12 medium-8 cell">
            <label>${not empty fieldLabels['url'] ? fieldLabels['url'] : 'URL'}
              <div class="input-group">
                <span class="input-group-label"><i class="fa fa-link"></i></span>
                <input class="input-group-field" type="text" placeholder="https://" name="url" value="<c:out value="${item.url}"/>">
              </div>
            </label>
          </div>
          <div class="small-6 medium-4 cell">
            <label>${not empty fieldLabels['urlText'] ? fieldLabels['urlText'] : 'Link Text'}
              <input type="text" placeholder="link text" name="urlText" value="<c:out value="${item.urlText}"/>">
            </label>
          </div>
        </div>
      </div>
    </c:if>
    <c:if test="${showAll or allowedFields.contains('imageUrl')}">
      <div class="grid-container">
        <div class="grid-x grid-padding-x">
          <div class="small-12 cell">
            <label>${not empty fieldLabels['imageUrl'] ? fieldLabels['imageUrl'] : 'Image URL'}
              <div class="input-group">
                <span class="input-group-label"><i class="fa fa-link"></i></span>
                <input class="input-group-field" type="text" placeholder="https://" id="imageUrl" name="imageUrl" value="<c:out value="${item.imageUrl}"/>">
                <span class="input-group-label" style="padding: 0;"><a class="button small primary expanded no-gap" data-open="imageBrowserReveal">Browse Images</a></span>
              </div>
              <label for="imageFile" class="button">Upload Image File...</label>
              <input type="file" id="imageFile" class="show-for-sr" onchange="SavePhoto(this)">
            </label>
          </div>
          <div class="small-4 cell">
            <img id="imageUrlPreview" alt="preview image" src="<c:out value="${item.imageUrl}"/>" style="max-height: 150px; max-width: 150px"/>
          </div>
        </div>
      </div>
    </c:if>
    <c:if test="${showAll or allowedFields.contains('keywords')}">
      <div class="grid-container">
        <div class="grid-x grid-padding-x">
          <div class="small-12 cell">
            <label>${not empty fieldLabels['keywords'] ? fieldLabels['keywords'] : 'Keywords'}
              <div class="input-group">
                <span class="input-group-label"><i class="fa fa-key"></i></span>
                <input class="input-group-field" type="text" placeholder="comma-separated keywords" name="keywords" value="<c:out value="${item.keywords}"/>">
              </div>
            </label>
          </div>
        </div>
      </div>
    </c:if>
  </c:if>

  <c:if test="${showAll or allowedFields.contains('location') or allowedFields.contains('street') or allowedFields.contains('addressLine2') or allowedFields.contains('addressLine3') or allowedFields.contains('city') or allowedFields.contains('state') or allowedFields.contains('postalCode') or allowedFields.contains('country')}">
    <h3 class="margin-top-40">Location</h3>
    <div class="grid-container">
      <div class="grid-x grid-padding-x">
        <div class="small-12 cell">
          <label>${not empty fieldLabels['location'] ? fieldLabels['location'] : 'Location Name (optional)'}
            <input type="text" placeholder="name of location" name="location" value="<c:out value="${item.location}"/>">
          </label>
          <label>${not empty fieldLabels['street'] ? fieldLabels['street'] : 'Street Address'}
            <input type="text" placeholder="number and street" name="street" value="<c:out value="${item.street}"/>">
          </label>
          <label>${not empty fieldLabels['addressLine2'] ? fieldLabels['addressLine2'] : 'Street Address Line 2'}
            <input type="text" placeholder="suite or unit number" name="addressLine2" value="<c:out value="${item.addressLine2}"/>">
          </label>
          <label>${not empty fieldLabels['addressLine3'] ? fieldLabels['addressLine3'] : 'Street Address Line 3'}
            <input type="text" name="addressLine3" value="<c:out value="${item.addressLine3}"/>">
          </label>
        </div>
      </div>
      <div class="grid-x grid-padding-x">
        <div class="medium-6 cell">
          <label>${not empty fieldLabels['city'] ? fieldLabels['city'] : 'City'}
            <input type="text" placeholder="city" name="city" value="<c:out value="${item.city}"/>">
          </label>
        </div>
        <div class="medium-6 cell">
          <label>${not empty fieldLabels['state'] ? fieldLabels['state'] : 'State'}
            <select id="state" name="state">
              <option value=""></option>
              <option value="AL"<c:if test="${item.state eq 'AL'}"> selected</c:if>>Alabama (AL)</option>
              <option value="AK"<c:if test="${item.state eq 'AK'}"> selected</c:if>>Alaska (AK)</option>
              <option value="AZ"<c:if test="${item.state eq 'AZ'}"> selected</c:if>>Arizona (AZ)</option>
              <option value="AR"<c:if test="${item.state eq 'AR'}"> selected</c:if>>Arkansas (AR)</option>
              <option value="CA"<c:if test="${item.state eq 'CA'}"> selected</c:if>>California (CA)</option>
              <option value="CO"<c:if test="${item.state eq 'CO'}"> selected</c:if>>Colorado (CO)</option>
              <option value="CT"<c:if test="${item.state eq 'CT'}"> selected</c:if>>Connecticut (CT)</option>
              <option value="DE"<c:if test="${item.state eq 'DE'}"> selected</c:if>>Delaware (DE)</option>
              <option value="DC"<c:if test="${item.state eq 'DC'}"> selected</c:if>>District Of Columbia (DC)</option>
              <option value="FL"<c:if test="${item.state eq 'FL'}"> selected</c:if>>Florida (FL)</option>
              <option value="GA"<c:if test="${item.state eq 'GA'}"> selected</c:if>>Georgia (GA)</option>
              <option value="HI"<c:if test="${item.state eq 'HI'}"> selected</c:if>>Hawaii (HI)</option>
              <option value="ID"<c:if test="${item.state eq 'ID'}"> selected</c:if>>Idaho (ID)</option>
              <option value="IL"<c:if test="${item.state eq 'IL'}"> selected</c:if>>Illinois (IL)</option>
              <option value="IN"<c:if test="${item.state eq 'IN'}"> selected</c:if>>Indiana (IN)</option>
              <option value="IA"<c:if test="${item.state eq 'IA'}"> selected</c:if>>Iowa (IA)</option>
              <option value="KS"<c:if test="${item.state eq 'KS'}"> selected</c:if>>Kansas (KS)</option>
              <option value="KY"<c:if test="${item.state eq 'KY'}"> selected</c:if>>Kentucky (KY)</option>
              <option value="LA"<c:if test="${item.state eq 'LA'}"> selected</c:if>>Louisiana (LA)</option>
              <option value="ME"<c:if test="${item.state eq 'ME'}"> selected</c:if>>Maine (ME)</option>
              <option value="MD"<c:if test="${item.state eq 'MD'}"> selected</c:if>>Maryland (MD)</option>
              <option value="MA"<c:if test="${item.state eq 'MA'}"> selected</c:if>>Massachusetts (MA)</option>
              <option value="MI"<c:if test="${item.state eq 'MI'}"> selected</c:if>>Michigan (MI)</option>
              <option value="MN"<c:if test="${item.state eq 'MN'}"> selected</c:if>>Minnesota (MN)</option>
              <option value="MS"<c:if test="${item.state eq 'MS'}"> selected</c:if>>Mississippi (MS)</option>
              <option value="MO"<c:if test="${item.state eq 'MO'}"> selected</c:if>>Missouri (MO)</option>
              <option value="MT"<c:if test="${item.state eq 'MT'}"> selected</c:if>>Montana (MT)</option>
              <option value="NE"<c:if test="${item.state eq 'NE'}"> selected</c:if>>Nebraska (NE)</option>
              <option value="NV"<c:if test="${item.state eq 'NV'}"> selected</c:if>>Nevada (NV)</option>
              <option value="NH"<c:if test="${item.state eq 'NH'}"> selected</c:if>>New Hampshire (NH)</option>
              <option value="NJ"<c:if test="${item.state eq 'NJ'}"> selected</c:if>>New Jersey (NJ)</option>
              <option value="NM"<c:if test="${item.state eq 'NM'}"> selected</c:if>>New Mexico (NM)</option>
              <option value="NY"<c:if test="${item.state eq 'NY'}"> selected</c:if>>New York (NY)</option>
              <option value="NC"<c:if test="${item.state eq 'NC'}"> selected</c:if>>North Carolina (NC)</option>
              <option value="ND"<c:if test="${item.state eq 'ND'}"> selected</c:if>>North Dakota (ND)</option>
              <option value="OH"<c:if test="${item.state eq 'OH'}"> selected</c:if>>Ohio (OH)</option>
              <option value="OK"<c:if test="${item.state eq 'OK'}"> selected</c:if>>Oklahoma (OK)</option>
              <option value="OR"<c:if test="${item.state eq 'OR'}"> selected</c:if>>Oregon (OR)</option>
              <option value="PA"<c:if test="${item.state eq 'PA'}"> selected</c:if>>Pennsylvania (PA)</option>
              <option value="RI"<c:if test="${item.state eq 'RI'}"> selected</c:if>>Rhode Island (RI)</option>
              <option value="SC"<c:if test="${item.state eq 'SC'}"> selected</c:if>>South Carolina (SC)</option>
              <option value="SD"<c:if test="${item.state eq 'SD'}"> selected</c:if>>South Dakota (SD)</option>
              <option value="TN"<c:if test="${item.state eq 'TN'}"> selected</c:if>>Tennessee (TN)</option>
              <option value="TX"<c:if test="${item.state eq 'TX'}"> selected</c:if>>Texas (TX)</option>
              <option value="UT"<c:if test="${item.state eq 'UT'}"> selected</c:if>>Utah (UT)</option>
              <option value="VT"<c:if test="${item.state eq 'VT'}"> selected</c:if>>Vermont (VT)</option>
              <option value="VA"<c:if test="${item.state eq 'VA'}"> selected</c:if>>Virginia (VA)</option>
              <option value="WA"<c:if test="${item.state eq 'WA'}"> selected</c:if>>Washington (WA)</option>
              <option value="WV"<c:if test="${item.state eq 'WV'}"> selected</c:if>>West Virginia (WV)</option>
              <option value="WI"<c:if test="${item.state eq 'WI'}"> selected</c:if>>Wisconsin (WI)</option>
              <option value="WY"<c:if test="${item.state eq 'WY'}"> selected</c:if>>Wyoming (WY)</option>
              <option value="AS"<c:if test="${item.state eq 'AS'}"> selected</c:if>>American Samoa (AS)</option>
              <option value="GU"<c:if test="${item.state eq 'GU'}"> selected</c:if>>Guam (GU)</option>
              <option value="MP"<c:if test="${item.state eq 'MP'}"> selected</c:if>>Northern Mariana Islands (MP)</option>
              <option value="PR"<c:if test="${item.state eq 'PR'}"> selected</c:if>>Puerto Rico (PR)</option>
              <option value="UM"<c:if test="${item.state eq 'UM'}"> selected</c:if>>United States Minor Outlying Islands (UM)</option>
              <option value="VI"<c:if test="${item.state eq 'VI'}"> selected</c:if>>Virgin Islands (VI)</option>
              <option value="AA"<c:if test="${item.state eq 'AA'}"> selected</c:if>>Armed Forces Americas (AA)</option>
              <option value="AP"<c:if test="${item.state eq 'AP'}"> selected</c:if>>Armed Forces Pacific (AP)</option>
              <option value="AE"<c:if test="${item.state eq 'AE'}"> selected</c:if>>Armed Forces Others (AE)</option>
            </select>
          </label>
        </div>
      </div>
      <div class="grid-x grid-padding-x">
        <div class="small-6 cell">
          <label>${not empty fieldLabels['postalCode'] ? fieldLabels['postalCode'] : 'Postal Code'}
            <input type="text" placeholder="postal code" name="postalCode" value="<c:out value="${item.postalCode}"/>">
          </label>
        </div>
        <div class="small-6 cell">
          <label>${not empty fieldLabels['country'] ? fieldLabels['country'] : 'Country'}
            <select id="country" name="country">
              <option value=""></option>
              <option value="United States"<c:if test="${item.country eq 'United States'}"> selected</c:if>>United States</option>
            </select>
          </label>
        </div>
      </div>
    </div>
  </c:if>

  <c:if test="${showAll or allowedFields.contains('county') or allowedFields.contains('latitude') or allowedFields.contains('longitude')}">
    <h3 class="margin-top-40">GIS</h3>
    <div class="grid-container">
      <div class="grid-x grid-padding-x">
        <c:if test="${showAll or allowedFields.contains('county')}">
          <div class="small-12 cell">
            <label>${not empty fieldLabels['county'] ? fieldLabels['county'] : 'County'}
              <input type="text" placeholder="county" name="county" value="<c:out value="${item.county}"/>">
            </label>
          </div>
        </c:if>
        <c:if test="${showAll or allowedFields.contains('latitude') or allowedFields.contains('longitude')}">
          <div class="medium-6 cell">
            <label>${not empty fieldLabels['latitude'] ? fieldLabels['latitude'] : 'Latitude'}
              <input type="text" placeholder="decimal format" name="latitude" value="<c:if test="${item.latitude ne 0}"><c:out value="${item.latitude}"/></c:if>">
            </label>
          </div>
          <div class="medium-6 cell">
            <label>${not empty fieldLabels['longitude'] ? fieldLabels['longitude'] : 'Longitude'}
              <input type="text" placeholder="decimal format" name="longitude" value="<c:if test="${item.longitude ne 0}"><c:out value="${item.longitude}"/></c:if>">
            </label>
          </div>
        </c:if>
      </div>
    </div>
  </c:if>

  <c:if test="${showAll or allowedFields.contains('phoneNumber') or allowedFields.contains('barcode')}">
    <h3 class="margin-top-40">Data</h3>
    <c:if test="${showAll or allowedFields.contains('phoneNumber')}">
      <div class="grid-container">
        <div class="grid-x grid-padding-x">
          <div class="small-12 cell">
            <label>${not empty fieldLabels['phoneNumber'] ? fieldLabels['phoneNumber'] : 'Phone Number'}
              <div class="input-group">
                <span class="input-group-label"><i class="fa fa-phone"></i></span>
                <input class="input-group-field" type="tel" placeholder="(xxx) xxx-xxxx" name="phoneNumber" value="<c:out value="${item.phoneNumber}"/>">
              </div>
            </label>
          </div>
        </div>
      </div>
    </c:if>
    <c:if test="${showAll or allowedFields.contains('barcode')}">
      <div class="grid-container">
        <div class="grid-x grid-padding-x">
          <div class="small-12 cell">
            <label>${not empty fieldLabels['barcode'] ? fieldLabels['barcode'] : 'Barcode'}
              <div class="input-group">
                <span class="input-group-label"><i class="fa fa-barcode"></i></span>
                <input class="input-group-field" type="text" placeholder="barcode value" name="barcode" value="<c:out value="${item.barcode}"/>">
              </div>
            </label>
          </div>
        </div>
      </div>
    </c:if>
  </c:if>

  <c:if test="${showAll or allowedFields.contains('cost')}">
    <h3 class="margin-top-40">Value</h3>
    <div class="grid-container">
      <div class="grid-x grid-padding-x">
        <div class="small-12 cell">
          <label>${not empty fieldLabels['cost'] ? fieldLabels['cost'] : 'Amount'}
            <div class="input-group">
              <span class="input-group-label"><i class="fa fa-dollar"></i></span>
              <input class="input-group-field" type="text" name="cost" value="<c:if test="${item.cost ne 0}"><c:out value="${item.cost}"/></c:if>">
            </div>
          </label>
        </div>
      </div>
    </div>
  </c:if>

  <c:if test="${showAll or allowedFields.contains('expectedDate') or allowedFields.contains('expirationDate') or allowedFields.contains('startDate') or allowedFields.contains('endDate')}">
    <h3 class="margin-top-40">Dates</h3>
    <div class="grid-container">
      <div class="grid-x grid-padding-x">

        <div class="medium-6 cell">
          <label>${not empty fieldLabels['expectedDate'] ? fieldLabels['expectedDate'] : 'Expected Start Date'}
            <div class="input-group">
              <span class="input-group-label"><i class="fa fa-calendar"></i></span>
              <input class="input-group-field" type="text" placeholder="mm-dd-yyyy time" id="expectedDate" name="expectedDate" value="<fmt:formatDate pattern="MM-dd-yyyy HH:mm" value="${item.expectedDate}" />">
            </div>
          </label>
          <script>
            $(function(){
              $('#expectedDate').fdatepicker({
                format: 'mm-dd-yyyy hh:ii',
                disableDblClickSelection: true,
                pickTime: true
              });
            });
          </script>
        </div>

        <div class="medium-6 cell">
          <label>${not empty fieldLabels['expirationDate'] ? fieldLabels['expirationDate'] : 'Expiration Date'}
            <div class="input-group">
              <span class="input-group-label"><i class="fa fa-calendar"></i></span>
              <input class="input-group-field" type="text" placeholder="mm-dd-yyyy time" id="expirationDate" name="expirationDate" value="<fmt:formatDate pattern="MM-dd-yyyy HH:mm" value="${item.expirationDate}" />">
            </div>
          </label>
          <script>
            $(function(){
              $('#expirationDate').fdatepicker({
                format: 'mm-dd-yyyy hh:ii',
                disableDblClickSelection: true,
                pickTime: true
              });
            });
          </script>
        </div>

      </div>
      <div class="grid-x grid-padding-x">

        <div class="medium-6 cell">
          <label>${not empty fieldLabels['startDate'] ? fieldLabels['startDate'] : 'Actual Start Date'}
            <div class="input-group">
              <span class="input-group-label"><i class="fa fa-calendar"></i></span>
              <input class="input-group-field" type="text" placeholder="mm-dd-yyyy time" id="startDate" name="startDate" value="<fmt:formatDate pattern="MM-dd-yyyy HH:mm" value="${item.startDate}" />">
            </div>
          </label>
          <script>
            $(function(){
              $('#startDate').fdatepicker({
                format: 'mm-dd-yyyy hh:ii',
                disableDblClickSelection: true,
                pickTime: true
              });
            });
          </script>
        </div>

        <div class="medium-6 cell">
          <label>${not empty fieldLabels['endDate'] ? fieldLabels['endDate'] : 'Actual End Date'}
            <div class="input-group">
              <span class="input-group-label"><i class="fa fa-calendar"></i></span>
              <input class="input-group-field" type="text" placeholder="mm-dd-yyyy time" id="endDate" name="endDate" value="<fmt:formatDate pattern="MM-dd-yyyy HH:mm" value="${item.endDate}" />">
            </div>
          </label>
          <script>
            $(function(){
              // yyyy-MM-dd HH:mm:ss.fffffffff
              $('#endDate').fdatepicker({
                format: 'mm-dd-yyyy hh:ii',
                disableDblClickSelection: true,
                pickTime: true
              });
            });
          </script>
        </div>

      </div>
    </div>

  </c:if>

  <%--
  <label>Assigned To
    <input type="text" placeholder="user name" name="assignedTo" value="<c:out value="${item.assignedTo}"/>">
  </label>
  --%>

  <c:if test="${!empty customFieldList}">
    <h3 class="margin-top-40">Additional Fields</h3>
    <div class="grid-container">
      <div class="grid-x grid-padding-x">
        <div class="small-12 cell">
        <c:forEach items="${customFieldList.values()}" var="formField" varStatus="status">
          <label class="margin-top-20"><c:out value="${formField.label}"/><c:if test="${formField.required}"> <span class="required">*</span></c:if>
            <c:choose>
              <c:when test="${formField.type eq 'multi-select list'}">
                <c:set var="normalizedValue" value="${fn:replace(formField.value, ', ', ',')}"/>
                <c:set var="delimitedValue" value=",${normalizedValue},"/>
                <div class="input-container">
                  <c:forEach items="${formField.listOfOptions}" var="option">
                    <c:set var="searchValue" value=",${option.value},"/>
                    <span class="text-no-wrap no-gap-all"><input type="checkbox"
                      id="${widgetContext.uniqueId}<c:out value="${formField.name}"/>-<c:out value="${option.key}"/>"
                      name="${widgetContext.uniqueId}<c:out value="${formField.name}"/>"
                      value="<c:out value="${option.key}"/>"
                      <c:if test="${fn:contains(delimitedValue, searchValue)}">checked</c:if>/><label class="no-gap" for="${widgetContext.uniqueId}<c:out value="${formField.name}"/>-<c:out value="${option.key}"/>"><c:out value="${option.value}"/></label></span>
                  </c:forEach>
                </div>
              </c:when>
              <c:when test="${!empty formField.listOfOptions}">
                <select id="${widgetContext.uniqueId}<c:out value="${formField.name}"/>" name="${widgetContext.uniqueId}<c:out value="${formField.name}"/>">
                  <option value="">&lt; Please Choose &gt;</option>
                  <c:forEach items="${formField.listOfOptions}" var="option">
                    <c:choose>
                      <c:when test="${option.value eq formField.value}">
                        <option value="${option.key}" selected><c:out value="${option.value}" /></option>
                      </c:when>
                      <c:otherwise>
                        <option value="${option.key}"><c:out value="${option.value}" /></option>
                      </c:otherwise>
                    </c:choose>
                  </c:forEach>
                </select>
              </c:when>
              <c:when test="${formField.type eq 'html'}">
                <textarea id="${widgetContext.uniqueId}<c:out value="${formField.name}"/>" class="html-field" name="${widgetContext.uniqueId}<c:out value="${formField.name}"/>"
                  <c:if test="${!empty formField.placeholder}"> placeholder="<c:out value="${formField.placeholder}" />"</c:if>
                  <c:if test="${formField.required}">required</c:if>><c:if test="${!empty formField.value}"><c:out value="${formField.value}" /></c:if></textarea>
              </c:when>
              <c:when test="${formField.type eq 'textarea'}">
                <textarea id="${widgetContext.uniqueId}<c:out value="${formField.name}"/>" name="${widgetContext.uniqueId}<c:out value="${formField.name}"/>" style="height:120px"
                  <c:if test="${!empty formField.placeholder}"> placeholder="<c:out value="${formField.placeholder}" />"</c:if>
                  <c:if test="${formField.required}">required</c:if>><c:if test="${!empty formField.value}"><c:out value="${formField.value}" /></c:if></textarea>
              </c:when>
              <c:otherwise>
                <input type="text"
                  id="${widgetContext.uniqueId}<c:out value="${formField.name}"/>" name="${widgetContext.uniqueId}<c:out value="${formField.name}"/>"
                  <c:if test="${!empty formField.placeholder}"> placeholder="<c:out value="${formField.placeholder}" />"</c:if>
                  <c:if test="${!empty formField.value}">value="<c:out value="${formField.value}" />"</c:if>
                  <c:if test="${formField.required}">required</c:if>>
              </c:otherwise>
            </c:choose>
          </label>
        </c:forEach>
        </div>
      </div>
    </div>
  </c:if>
  <div class="button-container gap">
    <input type="submit" class="button radius success" value="Save"/>
    <c:if test="${!empty cancelUrl}"><span class="button-gap"><a class="button radius secondary" href="${ctx}${cancelUrl}">Cancel</a></span></c:if>
  </div>
</form>
<div class="reveal large" id="itemVersionHistoryReveal${widgetContext.uniqueId}" data-reveal>
  <h3>Version History</h3>
  <div id="itemVersionHistoryList${widgetContext.uniqueId}"><p>Loading...</p></div>
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
</div>
<div class="reveal large" id="imageBrowserReveal" data-reveal data-animation-in="slide-in-down fast">
  <h3>Loading...</h3>
</div>
<script>
  function escapeItemVersionHtml(input) {
    if (input === null || input === undefined) {
      return '';
    }
    return String(input)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
  }

  function loadItemVersions${widgetContext.uniqueId}() {
    var target = $('#itemVersionHistoryList${widgetContext.uniqueId}');
    target.html('<p>Loading...</p>');

    $.ajax({
      url: '${ctx}/json/itemVersions?itemId=${item.id}',
      cache: false,
      dataType: 'json'
    }).done(function (versions) {
      var versionList = [];
      if (Array.isArray(versions)) {
        versionList = versions;
      } else if (versions && Array.isArray(versions.data)) {
        versionList = versions.data;
      }

      if (versionList.length === 0) {
        target.html('<p>No previous versions were found.</p>');
        return;
      }

      var html = '<table class="stack"><thead><tr><th>By</th><th>Date</th><th>Actions</th></tr></thead><tbody>';
      versionList.forEach(function (version) {
        var payload = '';
        if (version.versionData) {
          try {
            payload = JSON.stringify(JSON.parse(version.versionData), null, 2);
          } catch (e) {
            payload = version.versionData;
          }
        }
        html += '<tr>'
            + '<td>' + escapeItemVersionHtml(version.createdByName || '') + '</td>'
            + '<td>' + escapeItemVersionHtml(version.created || '') + '</td>'
            + '<td>'
          + '<a class="button tiny secondary margin-right-5 js-preview-item-version" href="#" data-preview-id="previewItemVersion${widgetContext.uniqueId}_' + version.versionId + '">Preview</a>'
            + '<a class="button tiny alert" onclick="restoreItemVersion${widgetContext.uniqueId}(' + version.versionId + ')">Restore</a>'
            + '</td>'
            + '</tr>'
            + '<tr><td colspan="3"><div class="callout secondary hide" id="previewItemVersion${widgetContext.uniqueId}_' + version.versionId + '"><pre style="white-space: pre-wrap; max-height: 320px; overflow: auto;">' + escapeItemVersionHtml(payload) + '</pre></div></td></tr>';
      });
      html += '</tbody></table>';
      target.html(html);
    }).fail(function (xhr) {
      var message = xhr && xhr.responseText ? xhr.responseText : 'Unable to load version history';
      target.html('<div class="callout alert">' + escapeItemVersionHtml(message) + '</div>');
    });
  }

  function restoreItemVersion${widgetContext.uniqueId}(versionId) {
    if (!confirm('Restore this version? The current item state will be saved as a new version before restore.')) {
      return;
    }

    $.ajax({
      type: 'POST',
      url: '${ctx}/json/itemVersionRestore',
      dataType: 'json',
      data: {
        itemId: '${item.id}',
        versionId: versionId,
        token: '${userSession.formToken}'
      }
    }).done(function () {
      window.location.reload();
    }).fail(function (xhr) {
      var message = xhr && xhr.responseText ? xhr.responseText : 'Unable to restore version';
      alert(message);
    });
  }

  (function() {
    const tagInput = document.getElementById('tag-input');
    const tagChips = document.getElementById('tag-chips');
    const tagsHidden = document.getElementById('tags-hidden');
    const dropdown = document.getElementById('tag-dropdown');

    if (!tagInput || !tagsHidden) return;

    let tags = [];
    let allTags = [];
    let filteredTags = [];
    let activeIndex = -1;

    // LOAD EXISTING TAGS
    const value = tagsHidden.value.trim();
    if (value) {
      tags = value.split(',').map(t => t.trim()).filter(Boolean);
    }

    // FETCH TAGS
    fetch('${ctx}/json/tags')
      .then(res => res.json())
      .then(data => {
        allTags = Array.isArray(data) ? data : (data.data || []);
      })
      .catch(() => allTags = []);

    function renderTags() {
      tagChips.innerHTML = '';

      tags.forEach((tag, index) => {
        const chip = document.createElement('div');
        chip.className = 'tag-chip';

        const text = document.createElement('span');
        text.textContent = tag;

        const remove = document.createElement('span');
        remove.className = 'remove-tag';
        remove.innerHTML = '&times;';
        remove.dataset.index = index;

        chip.appendChild(text);
        chip.appendChild(remove);
        tagChips.appendChild(chip);
      });

      tagsHidden.value = tags.join(',');
    }

    function addTag(value) {
      const t = value.trim();

      if (!t) return;

      const exists = tags.some(x => x.toLowerCase() === t.toLowerCase());
      if (!exists) {
        tags.push(t);
        renderTags();
      }

      tagInput.value = '';
      hideDropdown();

      setTimeout(() => tagInput.focus(), 0);
    }

    function removeTag(index) {
      tags.splice(index, 1);
      renderTags();
    }

    function showDropdown(list) {
      dropdown.innerHTML = '';

      if (!list.length) {
        dropdown.style.display = 'none';
        return;
      }

      list.forEach((tag, i) => {
        const div = document.createElement('div');
        div.className = 'tag-option';
        div.textContent = tag;

        // highlight already selected
        if (tags.some(x => x.toLowerCase() === tag.toLowerCase())) {
          div.classList.add('selected');
        }

        // FIX CLICK (use mousedown instead of click)
        div.addEventListener('mousedown', (e) => {
          e.preventDefault();   // prevent blur
          addTag(tag);
        });

        dropdown.appendChild(div);
      });

      dropdown.style.display = 'block';
    }

    function hideDropdown() {
      dropdown.style.display = 'none';
    }

    function filterTags(query) {
      const q = query.toLowerCase();
      filteredTags = allTags.filter(t => t.toLowerCase().includes(q));
      showDropdown(filteredTags);
    }

    // INPUT EVENTS
    tagInput.addEventListener('input', function() {
      filterTags(this.value);
    });

    tagInput.addEventListener('focus', function() {
      filterTags(this.value);
    });

    // KEYBOARD
    tagInput.addEventListener('keydown', function(e) {

      if (e.key === 'Enter') {
        e.preventDefault();
        addTag(tagInput.value);
        return;
      }

      if (e.key === 'Tab') {
        if (tagInput.value.trim()) {
          e.preventDefault();
          addTag(tagInput.value);
        }
        return;
      }

      if (e.key === 'Backspace' && !tagInput.value && tags.length) {
        tags.pop();
        renderTags();
      }
    });

    document.addEventListener('click', function(e) {
      if (!e.target.closest('#tag-input-container')) {
        hideDropdown();
      }
    });

    tagChips.addEventListener('click', function(e) {
      if (e.target.classList.contains('remove-tag')) {
        removeTag(parseInt(e.target.dataset.index));
      }
    });

    renderTags();

  })();

  $('#itemVersionHistoryReveal${widgetContext.uniqueId}').on('open.zf.reveal', function () {
    loadItemVersions${widgetContext.uniqueId}();
  });

  $('#itemVersionHistoryList${widgetContext.uniqueId}').on('click', '.js-preview-item-version', function (e) {
    e.preventDefault();
    var previewId = $(this).attr('data-preview-id');
    $('[id="' + previewId + '"]').toggleClass('hide');
  });

  $('#imageBrowserReveal').on('open.zf.reveal', function () {
    $('#imageBrowserReveal').html("<h3>Loading...</h3>");
    $.ajax({
      url: '${ctx}/image-browser?inputId=imageUrl&view=reveal',
      cache: false,
      dataType: 'html'
    }).done(function (content) {
      setTimeout(function () {
        $('#imageBrowserReveal').html(content);
        $('#imageBrowserReveal').trigger('resizeme.zf.trigger');
      }, 1000);
    });
  })
</script>