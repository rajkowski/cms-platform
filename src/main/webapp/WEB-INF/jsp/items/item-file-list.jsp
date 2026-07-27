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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="html" uri="/WEB-INF/tlds/html-functions.tld" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="text" uri="/WEB-INF/tlds/text-functions.tld" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="date" uri="/WEB-INF/tlds/date-functions.tld" %>
<%@ taglib prefix="number" uri="/WEB-INF/tlds/number-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="item" class="com.simisinc.platform.domain.model.items.Item" scope="request"/>
<jsp:useBean id="fileItemList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="useViewer" class="java.lang.String" scope="request"/>
<jsp:useBean id="showLinks" class="java.lang.String" scope="request"/>
<jsp:useBean id="canEdit" class="java.lang.String" scope="request"/>
<jsp:useBean id="canDelete" class="java.lang.String" scope="request"/>
<jsp:useBean id="emptyMessage" class="java.lang.String" scope="request"/>
<style>
  .file-item-row {
    padding: 8px 0;
    border-bottom: 1px solid #eee;
  }

  .file-main {
    font-weight: 500;
  }

  .file-size {
    color: #888;
    font-size: 12px;
    margin-left: 6px;
  }

  .file-actions {
    margin-top: 4px;
  }

  .link-action {
    text-decoration: none;
  }

  .link-action:hover {
    text-decoration: underline;
  }

  .separator {
    margin: 0 6px;
    color: #999;
  }

  .remove-link {
    color: #d13438;
  }
</style>
<c:if test="${!empty title}">
  <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
</c:if>
<c:if test="${empty fileItemList}">
  <c:out value="${emptyMessage}" />
</c:if>

<iframe name="uploadFrame" style="display:none;"></iframe>

<c:if test="${!empty fileItemList}">
  <ul>
    <c:forEach items="${fileItemList}" var="file">
      <li class="file-item-row">

        <%-- Line 1: File name + size --%>
        <div class="file-main">
          <span class="file-name">
            <c:out value="${file.filename}" />
            <c:if test="${file.fileLength gt 0}">
              <span class="file-size">
                (<c:out value="${number:suffix(file.fileLength)}"/>)
              </span>
            </c:if>
          </span>

          <%-- Status --%>
          <c:choose>
            <c:when test="${!date:isHoursOld(file.created, 1)}">
              <span class="badge new">new</span>
            </c:when>
            <c:when test="${!date:isDaysOld(file.modified, 1)}">
              <span class="badge updated">updated</span>
            </c:when>
          </c:choose>
        </div>

        <%-- Line 2: Actions --%>
        <div class="file-actions">

          <c:choose>

            <c:when test="${fn:toLowerCase(file.fileType) eq 'url'}">
              <a target="_blank" href="${ctx}/show/${item.uniqueId}/assets/view/${file.url}" class="link-action">Open</a>
            </c:when>

            <c:when test="${fn:toLowerCase(file.fileType) eq 'video'}">
              <a target="_blank" href="${ctx}/show/${item.uniqueId}/assets/view/${file.url}" class="link-action">View</a>
            </c:when>

            <c:when test="${useViewer eq 'true' && fn:toLowerCase(file.fileType) eq 'pdf'}">
              <a target="_blank" href="${ctx}/show/${item.uniqueId}/assets/view/${file.url}" class="link-action">View</a>
            </c:when>

            <c:otherwise>
              <a href="${ctx}/show/${item.uniqueId}/assets/file/${file.url}" class="link-action">Download</a>
            </c:otherwise>

          </c:choose>

          <%-- Add Version --%>
          <c:if test="${canEdit eq 'true'}">
            <span class="separator">|</span>
            <a href="javascript:void(0)"
              class="link-action"
              onclick="triggerUpload('${file.id}')">Add Version</a>
          </c:if>

          <%-- Remove --%>
          <c:if test="${canDelete eq 'true'}">
            <span class="separator">|</span>
            <a class="link-action remove-link"
                        onclick="return confirm('Are you sure you want to permanently delete this file? This action cannot be undone.');"
              href="${widgetContext.uri}?command=delete&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=${file.id}">
              Delete
            </a>
          </c:if>

        </div>

        <%-- Tags --%>
        <c:if test="${!empty file.tags}">
          <div class="file-tags">
            <small><c:out value="${fn:join(file.tags, ', ')}" /></small>
          </div>
        </c:if>
        <form id="uploadForm-${file.id}"
            action="${widgetContext.uri}?widget=${widgetContext.uniqueId}&fileId=${file.id}"
            method="post"
            enctype="multipart/form-data"
            target="uploadFrame"
            style="display:none;">          
          
            <input type="file"
              id="fileInput-${file.id}"
              style="display:none"
              onchange="uploadVersion('${file.id}', this)" />

        </form>
      </li>
    </c:forEach>
  </ul>
</c:if>

<div id="upload-modal" class="reveal" data-reveal>
  <h3>Uploading File</h3>

  <div id="upload-progress">
    <p id="upload-status">Preparing upload...</p>
    <div class="progress">
      <div class="progress-meter" id="upload-progress-bar" style="width: 0%"></div>
    </div>
  </div>

  <div id="upload-success" style="display:none;">
    <div class="callout success">
      <p>File uploaded successfully!</p>
    </div>
  </div>

  <div id="upload-error" style="display:none;">
    <div class="callout alert">
      <p><span id="upload-error-message">Upload failed</span></p>
    </div>
  </div>

  <button class="close-button" data-close>&times;</button>
</div>

<script>
  let uploadModal;

  document.addEventListener("DOMContentLoaded", function () {
    const modalEl = document.getElementById("upload-modal");

    if (modalEl) {
      uploadModal = new Foundation.Reveal($(modalEl));
    } else {
      console.error("upload-modal not found in DOM");
    }
  });

  function triggerUpload(fileId) {
    const input = document.getElementById("fileInput-" + fileId);

    if (!input) {
      alert("Input not found");
      return;
    }

    input.value = "";
    input.click();
  }

  function formatFileSize(bytes) {
    if (!bytes) return "";

    bytes = Number(bytes);

    if (bytes < 1024) return bytes + " B";

    let kb = bytes / 1024;
    if (kb < 1024) return Math.round(kb) + " KB";

    let mb = kb / 1024;
    return mb.toFixed(1) + " MB";
  }

  async function uploadVersion(fileId, input) {
    try {
      if (!input.files || input.files.length === 0) return;

      openUploadModal(); // OPEN MODAL

      const file = input.files[0];

      const formData = new FormData();
      formData.append("file", file);
      formData.append("fileId", fileId);
      formData.append("itemId", "${item.uniqueId}");
      formData.append("token", "${userSession.formToken}");

      const xhr = new XMLHttpRequest();

      xhr.open("POST", "/json/itemFileVersionUpload", true);

      // Progress tracking

      xhr.upload.onprogress = function (e) {
        if (!e.lengthComputable) return;

        const bar = document.getElementById("upload-progress-bar");

        if (bar) {
          const percent = (e.loaded / e.total) * 100;
          bar.style.width = percent + "%";
        }
      };


      xhr.onload = function () {
        const data = JSON.parse(xhr.responseText);

        if (data.error) {
          document.getElementById("upload-progress").style.display = "none";
          document.getElementById("upload-error").style.display = "block";
          document.getElementById("upload-error-message").innerText = data.error;
          return;
        }

        // SUCCESS UI
        const progress = document.getElementById("upload-progress");
        const success = document.getElementById("upload-success");

        if (progress) progress.style.display = "none";
        if (success) success.style.display = "block";


        const row = input.closest("li");

        // 1. keep your working logic
        const link = row.querySelector("a[href*='/assets/file/']");
        if (link && data.url) {
          link.href = data.url;
        }

        // 2. OPTIONAL update filename (SAFE)
        if (data.filename) {
          const nameEl = row.querySelector(".file-name");
          if (nameEl) {
            nameEl.childNodes[0].nodeValue = data.filename + " ";
          }
        }

        if (data.size) {
          const sizeEl = row.querySelector(".file-size");

          if (sizeEl) {
            try {
              // extract numeric part safely
              const numericSize = parseInt(data.size, 10);

              if (!isNaN(numericSize)) {
                sizeEl.innerText = "(" + formatFileSize(numericSize) + ")";
              } else {
                sizeEl.innerText = "(" + data.size + ")";
              }

            } catch (e) {
              console.error("Size format failed:", e);
              sizeEl.innerText = "(" + data.size + ")";
            }
          }
        }



        // 4. SAFE badge update
        const existingBadge = row.querySelector(".badge.updated");
        if (!existingBadge) {
          const badge = document.createElement("span");
          badge.className = "badge updated";
          badge.innerText = "updated";
          row.querySelector(".file-main").appendChild(badge);
        }

        // 5. highlight effect
        row.style.background = "#e6ffed";
        setTimeout(() => {
          row.style.background = "";
        }, 1500);

        // CLOSE MODAL
        closeUploadModal();
      };


      xhr.onerror = function () {
        const progress = document.getElementById("upload-progress");
        const error = document.getElementById("upload-error");

        if (progress) progress.style.display = "none";
        if (error) error.style.display = "block";
      };


      xhr.send(formData);
    } catch (err) {
      console.error("Upload failed:", err);
    }
  }



  function openUploadModal() {

    const progress = document.getElementById("upload-progress");
    const success = document.getElementById("upload-success");
    const error = document.getElementById("upload-error");
    const status = document.getElementById("upload-status");
    const bar = document.getElementById("upload-progress-bar");

    // Safely update only if exists
    if (progress) progress.style.display = "block";
    if (success) success.style.display = "none";
    if (error) error.style.display = "none";

    if (status) status.innerText = "Uploading...";
    if (bar) bar.style.width = "0%";

    if (uploadModal) {
      uploadModal.open();
    } else {
      console.warn("Modal not initialized");
    }
  }


  function closeUploadModal() {
    setTimeout(() => {

      if (uploadModal) {
        uploadModal.close();
      }

      // Clean overlay only (not modal DOM)
      document.body.classList.remove("is-reveal-open");

      document.querySelectorAll(".reveal-overlay").forEach(el => el.remove());

    }, 800);
  }

</script>


