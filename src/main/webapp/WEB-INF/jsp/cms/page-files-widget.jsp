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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="user" uri="/WEB-INF/tlds/user-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>

<div class="page-files-widget">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
  </c:if>

  <style>
    .page-files-widget-actions {
      margin-bottom: 1rem;
      padding-top: 0.75rem;
    }

    .page-files-widget .page-files-widget-actions > button.button.small,
    .page-files-widget .page-files-widget-actions > button.button.small:hover,
    .page-files-widget .page-files-widget-actions > button.button.small:focus,
    .page-files-widget .page-files-widget-actions > button.button.small:active,
    .page-files-widget .page-files-widget-actions > button.button.small:visited,
    .page-files-widget .page-files-widget-actions > button.button.small.is-active,
    .page-files-widget .page-files-widget-actions > button.button.small.is-open,
    .page-files-widget .page-files-widget-actions > button.button.small[aria-expanded='true'] {
      background: #1779ba !important;
      background-color: #1779ba !important;
      border-color: #1779ba !important;
      color: #ffffff !important;
      box-shadow: none !important;
      outline: none !important;
      background-image: none !important;
    }

    #page-files-upload-modal {
      border: 0;
      border-radius: 14px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
      padding: 1.5rem;
      max-width: 34rem;
      width: calc(100% - 1.5rem);
    }

    #page-files-upload-modal h3 {
      margin-bottom: 1rem;
      font-weight: 700;
    }

    #page-files-upload-modal label {
      display: block;
      font-weight: 600;
      color: #2f3437;
      margin-bottom: 0.35rem;
    }

    #page-files-upload-input {
      display: block;
      width: 100%;
      padding: 0.75rem 0.9rem;
      border: 1px dashed #b8c2cc;
      border-radius: 10px;
      background: #f8fafc;
      color: #334155;
      cursor: pointer;
      transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
    }

    #page-files-upload-input:hover,
    #page-files-upload-input:focus {
      border-color: #1779ba;
      background: #ffffff;
      box-shadow: 0 0 0 3px rgba(23, 121, 186, 0.12);
      outline: none;
    }

    #page-files-upload-input::file-selector-button {
      margin-right: 0.9rem;
      padding: 0.55rem 0.9rem;
      border: 0;
      border-radius: 8px;
      background: #1779ba;
      color: #ffffff;
      font-weight: 600;
      cursor: pointer;
      transition: background-color 0.15s ease;
    }

    #page-files-upload-input::file-selector-button:hover {
      background: #12679f;
    }

    #page-files-upload-status {
      margin-top: 1rem;
    }

    #page-files-upload-status .callout {
      margin-bottom: 0;
      border-radius: 10px;
    }

    #page-files-attach-existing-modal {
      border: 0;
      border-radius: 14px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
      padding: 1.5rem;
      max-width: 44rem;
      width: calc(100% - 1.5rem);
    }

    #page-files-attach-existing-modal h3 {
      margin-bottom: 1rem;
      font-weight: 700;
    }

    #page-files-attach-search {
      display: block;
      width: 100%;
      margin-bottom: 1rem;
      padding: 0.75rem 0.9rem;
      border: 1px solid #cbd5e1;
      border-radius: 10px;
      background: #ffffff;
      color: #334155;
    }

    .page-files-attach-list {
      max-height: 20rem;
      overflow-y: auto;
      border: 1px solid #e2e8f0;
      border-radius: 10px;
      padding: 0.5rem;
      background: #f8fafc;
      margin-bottom: 1rem;
      list-style: none;
    }

    .page-files-attach-list li {
      margin: 0;
      padding: 0;
    }

    .page-files-attach-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.75rem 0.9rem;
      border-radius: 8px;
      background: #ffffff;
      border: 1px solid transparent;
      margin-bottom: 0.5rem;
      cursor: pointer;
      transition: border-color 0.15s ease, background-color 0.15s ease;
    }

    .page-files-attach-item:hover {
      border-color: #1779ba;
      background: #f0f9ff;
    }

    .page-files-attach-meta {
      min-width: 0;
      flex: 1;
    }

    .page-files-attach-name {
      display: block;
      font-weight: 600;
      color: #0f172a;
    }

    .page-files-attach-detail {
      display: block;
      color: #64748b;
      font-size: 0.875rem;
    }

    .page-files-attach-empty {
      padding: 1rem;
      text-align: center;
      color: #64748b;
    }

    #page-files-attach-status {
      margin-top: 0.75rem;
    }

    #page-files-attach-status .callout {
      margin-bottom: 0;
      border-radius: 10px;
    }
  </style>

  <c:set var="canUpload" value="${userSession.hasRole('admin') || userSession.hasRole('content-manager')}" />
  <c:set var="canRemove" value="${userSession.hasRole('admin') || userSession.hasRole('content-manager')}" />
  <c:set var="canDelete" value="${userSession.hasRole('admin')}" />
  <c:set var="deletePageFileUrl" value="${widgetContext.uri}?command=delete&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=" />
  <c:set var="removePageFileUrl" value="${widgetContext.uri}?command=remove&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=" />

  <c:if test="${canUpload}">
    <div class="page-files-widget-actions">
      <button type="button" class="button small secondary" data-open="page-files-attach-existing-modal">Attach an Existing File</button>
      <button type="button" class="button small" data-open="page-files-upload-modal">Upload New File</button>
    </div>

    <div class="reveal" id="page-files-attach-existing-modal" data-reveal data-close-on-click="false">
      <h3>Attach an Existing File</h3>
      <input type="search" id="page-files-attach-search" placeholder="Search existing files..." aria-label="Search existing files" />
      <div class="page-files-attach-list" id="page-files-attach-list">
        <div class="page-files-attach-empty">Loading files...</div>
      </div>
      <div id="page-files-attach-status"></div>
      <div class="grid-x grid-padding-x" style="margin-top: 1rem;">
        <div class="cell">
          <button type="button" class="button secondary" data-close>Close</button>
        </div>
      </div>
      <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
      </button>
    </div>

    <div class="reveal" id="page-files-upload-modal" data-reveal data-close-on-click="false">
      <h3>Upload a File</h3>
      <form id="page-files-upload-form" method="post" enctype="multipart/form-data">
        <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
        <input type="hidden" name="token" value="${userSession.formToken}"/>
        <div class="grid-x grid-padding-x">
          <div class="cell">
            <label for="page-files-upload-input">Select File</label>
            <input type="file" name="file" id="page-files-upload-input" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.csv,.png,.jpg,.jpeg,.gif,.mp4,.mov,.zip" multiple required />
          </div>
          <div class="cell" style="margin-top: 1rem;">
            <button type="submit" class="button" id="page-files-upload-submit">Upload</button>
            <button type="button" class="button secondary" data-close>Cancel</button>
          </div>
        </div>
        <div id="page-files-upload-status"></div>
      </form>
      <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
      </button>
    </div>

    <script>
      (function() {
        var form = document.getElementById('page-files-upload-form');
        var statusDiv = document.getElementById('page-files-upload-status');
        var submitBtn = document.getElementById('page-files-upload-submit');
        var canRemove = ${canRemove};
        var canDelete = ${canDelete};
        var deletePageFileUrl = '${widgetContext.uri}?command=delete&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=';
        var removePageFileUrl = '${widgetContext.uri}?command=remove&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=';
        var attachSearch = document.getElementById('page-files-attach-search');
        var attachList = document.getElementById('page-files-attach-list');
        var attachStatus = document.getElementById('page-files-attach-status');
        var attachFilesCache = [];

        function renderAttachItems(files, searchTerm) {
          if (!attachList) {
            return;
          }

          attachList.innerHTML = '';

          if (!files || files.length === 0) {
            attachList.innerHTML = '<div class="page-files-attach-empty">' + (searchTerm ? 'No files found matching your search.' : 'No files found.') + '</div>';
            return;
          }

          var fragment = document.createDocumentFragment();
          files.forEach(function(file) {
            var item = document.createElement('div');
            item.className = 'page-files-attach-item';
            item.setAttribute('data-file-id', file.id || '');
            var title = file.title || file.filename || 'Untitled file';
            var filename = file.filename || '';
            var detailParts = [];
            if (filename) {
              detailParts.push(filename);
            }
            if (file.fileType) {
              detailParts.push(file.fileType);
            }
            var searchableText = [title, filename, file.fileType || '', file.mimeType || ''].join(' ').toLowerCase();
            item.setAttribute('data-file-name', searchableText);
            var detailText = detailParts.map(function(part) {
              return escapeHtml(part);
            }).join(' &bull; ');
            item.innerHTML =
              '<div class="page-files-attach-meta">' +
                '<span class="page-files-attach-name">' + escapeHtml(title) + '</span>' +
                '<span class="page-files-attach-detail">' + detailText + '</span>' +
              '</div>' +
              '<span class="button small">Attach</span>';
            fragment.appendChild(item);
          });

          attachList.appendChild(fragment);
          attachList.scrollTop = 0;
        }

        function filterAttachFiles(searchTerm) {
          var term = (searchTerm || '').trim();
          if (!attachFilesCache || attachFilesCache.length === 0) {
            renderAttachItems([], term);
            return;
          }

          var filteredFiles = !term ? attachFilesCache : attachFilesCache.filter(function(file) {
            var haystack = [file.title || '', file.filename || '', file.fileType || '', file.mimeType || ''].join(' ').toLowerCase();
            return haystack.indexOf(term.toLowerCase()) !== -1;
          });

          renderAttachItems(filteredFiles, term);
        }

        function loadAttachFiles() {
          if (!attachList) {
            return;
          }

          attachList.innerHTML = '<div class="page-files-attach-empty">Loading files...</div>';

          var url = new URL('${ctx}/json/documentFileList', window.location.origin);
          url.searchParams.set('page', '1');
          url.searchParams.set('limit', '1000');

          fetch(url.toString(), {
            credentials: 'same-origin'
          })
          .then(function(response) {
            if (!response.ok) {
              throw new Error('HTTP ' + response.status);
            }
            return response.json();
          })
          .then(function(payload) {
            attachFilesCache = payload.files || [];
            filterAttachFiles(attachSearch ? attachSearch.value : '');
          })
          .catch(function(error) {
            console.error('[page-files] unable to load existing files', error);
            if (attachList) {
              attachList.innerHTML = '<div class="page-files-attach-empty">Unable to load files right now.</div>';
            }
          });
        }

        function removeFileFromList(pageFileId) {
          var viewMode = '${viewMode}';
          if (viewMode === 'table') {
            var rows = document.querySelectorAll('.page-files-table tbody tr');
            rows.forEach(function(row) {
              var links = row.querySelectorAll('a[href*="fileId=' + pageFileId + '"]');
              if (links.length > 0) {
                row.remove();
              }
            });
            var tbody = document.querySelector('.page-files-table tbody');
            if (tbody && tbody.children.length === 0) {
              var noFilesRow = document.createElement('tr');
              noFilesRow.className = 'no-files-message';
              noFilesRow.innerHTML = '<td colspan="4" style="text-align: center; color: #999; padding: 2rem;">There are no files attached to this page.</td>';
              tbody.appendChild(noFilesRow);
            }
          } else {
            var items = document.querySelectorAll('.page-files-list .page-file-item');
            items.forEach(function(item) {
              var links = item.querySelectorAll('a[href*="fileId=' + pageFileId + '"]');
              if (links.length > 0) {
                item.remove();
              }
            });
            var list = document.querySelector('.page-files-list');
            if (list && list.children.length === 0) {
              var noFilesMsg = document.createElement('li');
              noFilesMsg.className = 'no-files-message';
              noFilesMsg.style.cssText = 'text-align: center; color: #999; padding: 2rem; list-style: none;';
              noFilesMsg.textContent = 'There are no files attached to this page.';
              list.appendChild(noFilesMsg);
            }
          }
        }

        function handleRemoveFile(pageFileId, event) {
          if (event) {
            event.preventDefault();
          }
          if (!confirm('Remove this attachment from this page?\n\nThe file will remain in the system and any other pages using it will not be affected.')) {
            return false;
          }
          
          fetch(removePageFileUrl + pageFileId, {
            method: 'POST',
            headers: {
              'X-Requested-With': 'XMLHttpRequest'
            }
          })
          .then(function(response) {
            if (response.ok) {
              removeFileFromList(pageFileId);
            } else {
              alert('Failed to remove attachment. Please try again.');
            }
          })
          .catch(function(error) {
            alert('Error removing attachment: ' + error.message);
          });
          
          return false;
        }

        function handleDeleteFile(pageFileId, event) {
          if (event) {
            event.preventDefault();
          }
          if (!confirm('Are you sure you want to permanently DELETE this file?\n\nThis will remove the file from ALL pages and delete it from the system.\n\nThis action CANNOT be undone.')) {
            return false;
          }
          
          fetch(deletePageFileUrl + pageFileId, {
            method: 'POST',
            headers: {
              'X-Requested-With': 'XMLHttpRequest'
            }
          })
          .then(function(response) {
            if (response.ok) {
              removeFileFromList(pageFileId);
            } else {
              alert('Failed to delete file. Please try again.');
            }
          })
          .catch(function(error) {
            alert('Error deleting file: ' + error.message);
          });
          
          return false;
        }

        document.addEventListener('click', function(e) {
          var addVersionLink = e.target.closest('.add-version-link');
          if (addVersionLink) {
            e.preventDefault();
            triggerUpload(addVersionLink.getAttribute('data-file-id'));
            return;
          }

          var removeLink = e.target.closest('a[href*="command=remove"]');
          if (removeLink && removeLink.href.includes('fileId=')) {
            e.preventDefault();
            var fileId = removeLink.href.match(/fileId=(\d+)/)[1];
            handleRemoveFile(fileId, e);
            return;
          }

          var deleteLink = e.target.closest('a[href*="command=delete"]');
          if (deleteLink && deleteLink.href.includes('fileId=')) {
            e.preventDefault();
            var fileId = deleteLink.href.match(/fileId=(\d+)/)[1];
            handleDeleteFile(fileId, e);
            return;
          }
        });

        function attachExistingFile(fileId) {
          if (!fileId) {
            console.error('[page-files] no fileId provided');
            return;
          }
          if (attachSearch) {
            attachSearch.value = '';
          }
          if (attachStatus) {
            attachStatus.innerHTML = '<div class="callout primary">Attaching file...</div>';
          }

          var formData = new FormData();
          formData.append('command', 'attach-existing');
          formData.append('fileId', fileId);
          formData.append('widget', '${widgetContext.uniqueId}');
          formData.append('token', '${userSession.formToken}');

          fetch('${widgetContext.uri}', {
            method: 'POST',
            body: formData,
            headers: {
              'X-Requested-With': 'XMLHttpRequest'
            }
          })
          .then(function(response) {
            return response.text().then(function(text) {
              return { status: response.status, ok: response.ok, text: text };
            });
          })
          .then(function(result) {
            if (!result.ok) {
              throw new Error('Attach failed (status ' + result.status + '): ' + result.text.substring(0, 200));
            }

            var data;
            try {
              data = JSON.parse(result.text);
            } catch (e) {
              throw new Error('Server returned invalid response. Expected JSON, got: ' + result.text.substring(0, 100));
            }

            if (data.success === false || data.error) {
              if (attachStatus) {
                attachStatus.innerHTML = '<div class="callout alert">' + (data.error || 'Could not attach file') + '</div>';
              }
              return;
            }

            if (data.alreadyAttached) {
              if (attachStatus) {
                attachStatus.innerHTML = '<div class="callout warning">This file is already attached to this page.</div>';
              }
              setTimeout(function() {
                if (window.jQuery && window.jQuery.fn && window.jQuery.fn.foundation) {
                  window.jQuery('#page-files-attach-existing-modal').foundation('close');
                }
                if (attachStatus) {
                  attachStatus.innerHTML = '';
                }
              }, 1500);
              return;
            }

            if (data.file) {
              addFileToList(data.file);
            }

            if (attachStatus) {
              attachStatus.innerHTML = '<div class="callout success">File attached successfully.</div>';
            }

            setTimeout(function() {
              if (window.jQuery && window.jQuery.fn && window.jQuery.fn.foundation) {
                window.jQuery('#page-files-attach-existing-modal').foundation('close');
              }
              if (attachStatus) {
                attachStatus.innerHTML = '';
              }
            }, 500);
          })
          .catch(function(error) {
            console.error('[page-files] attach error caught:', error);
            if (attachStatus) {
              attachStatus.innerHTML = '<div class="callout alert">Error: ' + error.message + '</div>';
            }
          });
        }
        
        if (attachSearch && attachList) {
          attachSearch.addEventListener('input', function() {
            filterAttachFiles(attachSearch.value);
          });

          attachList.addEventListener('click', function(e) {
            var item = e.target.closest('.page-files-attach-item');
            if (!item) {
              return;
            }
            e.preventDefault();
            var fileId = item.getAttribute('data-file-id');
            attachExistingFile(fileId);
          });
        } else {
          console.error('[page-files] Could not set up event listeners - attachSearch:', !!attachSearch, 'attachList:', !!attachList);
        }

        function handleAttachModalOpen() {
          if (attachSearch) {
            attachSearch.value = '';
          }
          loadAttachFiles();
          if (attachStatus) {
            attachStatus.innerHTML = '';
          }
        }

        var attachModal = document.getElementById('page-files-attach-existing-modal');
        var attachOpenButton = document.querySelector('[data-open="page-files-attach-existing-modal"]');

        if (attachOpenButton) {
          attachOpenButton.addEventListener('click', handleAttachModalOpen);
        }

        if (attachModal) {
          attachModal.addEventListener('open.zf.reveal', handleAttachModalOpen);
          attachModal.addEventListener('opened.zf.reveal', handleAttachModalOpen);
          if (window.jQuery && window.jQuery.fn && window.jQuery.fn.foundation) {
            window.jQuery(attachModal).on('open.zf.reveal opened.zf.reveal', handleAttachModalOpen);
          }
        }

        if (form) {
          form.addEventListener('submit', function(e) {
            e.preventDefault();

            var fileInput = document.getElementById('page-files-upload-input');
            if (!fileInput.files || fileInput.files.length === 0) {
              statusDiv.innerHTML = '<div class="callout alert">Please select a file to upload.</div>';
              return;
            }

            statusDiv.innerHTML = '<div class="callout primary">Uploading...</div>';
            submitBtn.disabled = true;

            var formData = new FormData(form);
            var uploadUrl = '${widgetContext.uri}?widget=${widgetContext.uniqueId}';

            fetch(uploadUrl, {
              method: 'POST',
              body: formData,
              headers: {
                'X-Requested-With': 'XMLHttpRequest'
              }
            })
            .then(function(response) {
              return response.text().then(function(text) {
                return { status: response.status, ok: response.ok, text: text };
              });
            })
            .then(function(result) {
              if (!result.ok) {
                throw new Error('Upload failed (status ' + result.status + '): ' + result.text.substring(0, 200));
              }

              var data;
              try {
                data = JSON.parse(result.text);
              } catch (e) {
                console.error('Response is not JSON:', result.text.substring(0, 500));
                throw new Error('Server returned invalid response. Expected JSON, got: ' + result.text.substring(0, 100));
              }

              if (data.success === false || data.error) {
                var errorMsg = data.error || 'Upload failed';
                console.error('Upload error:', errorMsg);
                statusDiv.innerHTML = '<div class="callout alert">' + errorMsg + '</div>';
                submitBtn.disabled = false;
              } else if (data.success === true && data.file) {
                addFileToList(data.file);
                statusDiv.innerHTML = '<div class="callout success">File uploaded successfully!</div>';

                setTimeout(function() {
                  var modal = document.getElementById('page-files-upload-modal');
                  if (modal && window.Foundation) {
                    jQuery(modal).foundation('close');
                  }
                  form.reset();
                  statusDiv.innerHTML = '';
                  submitBtn.disabled = false;
                }, 1000);
              } else {
                statusDiv.innerHTML = '<div class="callout success">File uploaded successfully!</div>';
                setTimeout(function() {
                  window.location.reload();
                }, 1000);
              }
            })
            .catch(function(error) {
              console.error('Upload error:', error);
              statusDiv.innerHTML = '<div class="callout alert">Error: ' + error.message + '</div>';
              submitBtn.disabled = false;
            });
          });
        }

        function addFileToList(file) {
          var viewMode = '${viewMode}';
          var ctx = '${ctx}';

          var fileDate = new Date(parseInt(file.fileModified) || Date.now());
          var dateStr = fileDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
          var displayName = file.title || file.filename;
          var extension = file.extension ? '.' + file.extension : '';
          var deleteId = file.pageFileId || file.id;
          var deleteUrl = canDelete && deleteId ? (deletePageFileUrl + deleteId) : '';
          var removeUrl = canRemove && deleteId ? (removePageFileUrl + deleteId) : '';
          var removeLink = '';
          var deleteLink = '';
          var tableRemoveLink = '';
          var tableDeleteLink = '';
         
          if (canRemove && removeUrl) {
            if (viewMode === 'table') {
              tableRemoveLink =
                ' <span class="page-file-separator">|</span> ' +
                '<a class="link-action" title="Remove attachment from this page only" href="' + removeUrl + '">' +
                  '<i class="fa fa-unlink"></i>' +
                '</a>';
            } else {
              removeLink =
                '<span class="page-file-separator">|</span>' +
                '<a class="link-action" title="Remove attachment from this page only" href="' + removeUrl + '">' +
                  '<i class="fa fa-unlink"></i> Remove' +
                '</a>';
            }
          }

          if (canDelete && deleteUrl) {
            if (viewMode === 'table') {
              tableDeleteLink =
                ' <span class="page-file-separator">|</span> ' +
                '<a class="link-action remove-link alert" title="Delete file permanently from system" href="' + deleteUrl + '">' +
                  '<i class="fa fa-trash"></i>' +
                '</a>';
            } else {
              deleteLink =
                '<span class="page-file-separator">|</span>' +
                '<a class="link-action remove-link alert" title="Delete file permanently from system" href="' + deleteUrl + '">' +
                  '<i class="fa fa-trash"></i> Delete' +
                '</a>';
            }
          }

          if (viewMode === 'table') {
            var tbody = document.querySelector('.page-files-table tbody');
            if (tbody) {
              var noFilesMsg = tbody.querySelector('.no-files-message');
              if (noFilesMsg) {
                noFilesMsg.remove();
              }

              var row = document.createElement('tr');
              row.innerHTML = 
                '<td>' +
                  '<a href="' + ctx + file.viewUrl + '" class="page-file-link">' +
                    '<span class="page-file-name">' + escapeHtml(displayName) + '</span>' +
                    (extension ? extension : '') +
                  '</a>' +
                '</td>' +
                '<td>' + dateStr + '</td>' +
                '<td>' + escapeHtml(file.fileModifiedBy || file.createdBy || '') + '</td>' +
                '<td>' +
                  '<a href="' + ctx + file.viewUrl + '" class="page-file-link" title="View" aria-label="View" target="_blank" rel="noopener noreferrer">' +
                    '<i class="fa fa-eye"></i>' +
                  '</a>' +
                  ' <span class="page-file-separator">|</span> ' +
                  '<a href="' + ctx + file.downloadUrl + '" class="page-file-link" title="Download" aria-label="Download" download>' +
                    '<i class="fa fa-download"></i>' +
                  '</a>' +
                  ' <span class="page-file-separator">|</span> ' +
                  '<a class="link-action add-version-link" title="Add version" aria-label="Add version" href="javascript:void(0)" data-file-id="' + deleteId + '">' +
                    '<i class="fa fa-plus"></i>' +
                  '</a>' +
                  tableRemoveLink +
                  tableDeleteLink +
                '</td>';
              tbody.appendChild(row);
            }
          } else {
            var list = document.querySelector('.page-files-list');
            if (list) {
              var noFilesMsg = list.querySelector('.no-files-message');
              if (noFilesMsg) {
                noFilesMsg.remove();
              }

              var li = document.createElement('li');
              li.className = 'page-file-item';
              li.innerHTML = 
                '<div class="page-file-row">' +
                  '<a href="' + ctx + file.viewUrl + '" class="page-file-link">' +
                    '<span class="page-file-name">' + escapeHtml(displayName) + '</span>' +
                    (extension ? '<span class="page-file-ext">' + extension + '</span>' : '') +
                  '</a>' +
                  '<span class="page-file-separator">|</span>' +
                  '<span class="page-file-actions">' +
                    '<a href="' + ctx + file.viewUrl + '" class="page-file-link" title="View" aria-label="View" target="_blank" rel="noopener noreferrer">' +
                      '<i class="fa fa-eye"></i> View' +
                    '</a>' +
                    '<span class="page-file-separator">|</span>' +
                    '<a href="' + ctx + file.downloadUrl + '" class="page-file-link" title="Download" aria-label="Download" download>' +
                      '<i class="fa fa-download"></i> Download' +
                    '</a>' +
                    '<span class="page-file-separator">|</span>' +
                    '<a class="link-action add-version-link" title="Add version" aria-label="Add version" href="javascript:void(0)" data-file-id="' + deleteId + '">' +
                      '<i class="fa fa-plus"></i>Add version' +
                    '</a>' +
                    removeLink +
                    deleteLink +
                  '</span>' +
                '</div>';
              list.appendChild(li);
            }
          }
        }

        function replaceVersionInList(file) {
          var pageFileId = file.web_page_file_id || file.id;
          if (!pageFileId) {
            addFileToList(file);
            return;
          }

          removeFileFromList(pageFileId);
          addFileToList(file);
        }

        function escapeHtml(text) {
          var map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
          };
          return String(text || '').replace(/[&<>"']/g, function(m) { return map[m]; });
        }

        function triggerUpload(fileId) {
          var input = document.getElementById('fileInput-' + fileId);

          if (!input) {
            alert('Input not found');
            return;
          }

          input.value = '';
          input.click();
        }

        function uploadVersion(fileId, input) {
          if (!input.files || input.files.length === 0) {
            return;
          }

          var file = input.files[0];
          var formData = new FormData();
          formData.append('file', file);
          formData.append('web_page_file_id', fileId);
          formData.append('pageId', '${widgetContext.uniqueId}');
          formData.append('token', '${userSession.formToken}');

          var xhr = new XMLHttpRequest();
          xhr.open('POST', '/json/webPageFileVersionUpload', true);

          xhr.onload = function() {
            var data;
            try {
              data = JSON.parse(xhr.responseText || '{}');
            } catch (error) {
              data = { error: 'Invalid server response' };
            }

            if (data.error) {
              if (statusDiv) {
                statusDiv.innerHTML = '<div class="callout alert">' + escapeHtml(data.error) + '</div>';
              }
              return;
            }
            if (data && data.file_id) {
              replaceVersionInList(data);
            }

            if (statusDiv) {
              statusDiv.innerHTML = '<div class="callout success">File uploaded successfully!</div>';
            }

            setTimeout(function() {
              if (form) {
                form.reset();
              }
              if (statusDiv) {
                statusDiv.innerHTML = '';
              }
              if (submitBtn) {
                submitBtn.disabled = false;
              }
            }, 800);
          };

          xhr.onerror = function() {
            if (statusDiv) {
              statusDiv.innerHTML = '<div class="callout alert">Upload failed.</div>';
            }
          };

          xhr.send(formData);
        }

        window.triggerUpload = triggerUpload;
        window.uploadVersion = uploadVersion;
      })();
    </script>
  </c:if>

  <c:choose>
    <c:when test="${viewMode eq 'table'}">
      <div class="table-responsive">
        <table class="hover stack page-files-table">
          <thead>
            <tr>
              <th>File</th>
              <th>Modified</th>
              <th>Modified By</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <c:choose>
              <c:when test="${empty fileList}">
                <tr class="no-files-message">
                  <td colspan="4" style="text-align: center; color: #999; padding: 2rem;">
                    There are no files attached to this page.
                  </td>
                </tr>
              </c:when>
              <c:otherwise>
                <c:forEach var="pageFile" items="${fileList}">
                  <tr>
                    <td>
                      <a href="${ctx}/assets/view/${pageFile.url}" class="page-file-link">
                        <span class="page-file-name"><c:out value="${pageFile.displayName}"/></span><c:if test="${!empty pageFile.extension}">.<c:out value="${pageFile.extension}"/></c:if>
                      </a>
                    </td>
                    <td><fmt:formatDate value="${pageFile.fileModified}" pattern="MMM dd, yyyy"/></td>
                    <td><c:out value="${user:name(pageFile.fileModifiedBy)}"/></td>
                    <td>
                      <a href="${ctx}/assets/view/${pageFile.url}" class="page-file-link" title="View" aria-label="View" target="_blank" rel="noopener noreferrer">
                        <i class="fa fa-eye"></i>
                      </a>
                      <span class="page-file-separator">|</span>
                      <a href="${ctx}/assets/file/${pageFile.url}" class="page-file-link" title="Download" aria-label="Download" download>
                        <i class="fa fa-download"></i>
                      </a>
                      <span class="page-file-separator">|</span>
                      <a class="link-action add-version-link" title="Add version" aria-label="Add version" href="javascript:void(0)" data-file-id="${pageFile.id}">
                        <i class="fa fa-plus"></i>
                      </a>
                      <c:if test="${canRemove}">
                        <span class="page-file-separator">|</span>
                        <a class="link-action" title="Remove attachment from this page only" href="${widgetContext.uri}?command=remove&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=${pageFile.id}">
                          <i class="fa fa-unlink"></i>
                        </a>
                      </c:if>
                      <c:if test="${canDelete}">
                        <span class="page-file-separator">|</span>
                        <a class="link-action remove-link alert" title="Delete file permanently from system" href="${widgetContext.uri}?command=delete&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=${pageFile.id}">
                          <i class="fa fa-trash"></i>
                        </a>
                      </c:if>
                    </td>
                  </tr>
                  <%-- Hidden form for version upload --%>
                  <form id="uploadForm-${pageFile.id}" style="display:none;">
                    <input type="file" id="fileInput-${pageFile.id}" style="display:none" onchange="uploadVersion('${pageFile.id}', this)" />
                  </form>
                </c:forEach>
              </c:otherwise>
            </c:choose>
          </tbody>
        </table>
      </div>
    </c:when>
    <c:otherwise>
      <ul class="page-files-list">
        <c:choose>
          <c:when test="${empty fileList}">
            <li class="no-files-message" style="text-align: center; color: #999; padding: 2rem; list-style: none;">
              There are no files attached to this page.
            </li>
          </c:when>
          <c:otherwise>
            <c:forEach var="pageFile" items="${fileList}">
              <li class="page-file-item">
                <div class="page-file-row">
                  <a href="${ctx}/assets/view/${pageFile.url}" class="page-file-link">
                    <span class="page-file-name"><c:out value="${pageFile.displayName}"/></span>
                    <c:if test="${!empty pageFile.extension}">
                      <span class="page-file-ext">.${fn:escapeXml(pageFile.extension)}</span>
                    </c:if>
                  </a>
                  <span class="page-file-separator">|</span>
                  <span class="page-file-actions">
                    <a href="${ctx}/assets/view/${pageFile.url}" class="page-file-link" title="View" aria-label="View" target="_blank" rel="noopener noreferrer">
                      <i class="fa fa-eye"></i> View
                    </a>
                    <span class="page-file-separator">|</span>
                    <a href="${ctx}/assets/file/${pageFile.url}" class="page-file-link" title="Download" aria-label="Download" download>
                      <i class="fa fa-download"></i> Download
                    </a>
                    <span class="page-file-separator">|</span>
                    <a class="link-action add-version-link" title="Add version" aria-label="Add version" href="javascript:void(0)" data-file-id="${pageFile.id}">
                      <i class="fa fa-plus"></i>Add version
                    </a>
                    <c:if test="${canRemove}">
                      <span class="page-file-separator">|</span>
                      <a class="link-action" title="Remove attachment from this page only" href="${widgetContext.uri}?command=remove&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=${pageFile.id}">
                        <i class="fa fa-unlink"></i> Remove
                      </a>
                    </c:if>
                    <c:if test="${canDelete}">
                      <span class="page-file-separator">|</span>
                      <a class="link-action remove-link alert" title="Delete file permanently from system" href="${widgetContext.uri}?command=delete&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&fileId=${pageFile.id}">
                        <i class="fa fa-trash"></i> Delete
                      </a>
                    </c:if>
                  </span>
                </div>
                <c:if test="${!empty pageFile.summary}">
                  <span class="page-file-summary"><c:out value="${pageFile.summary}"/></span>
                </c:if>
                <%-- Hidden form for version upload --%>
                <form id="uploadForm-${pageFile.id}" style="display:none;">
                  <input type="file" id="fileInput-${pageFile.id}" style="display:none" onchange="uploadVersion('${pageFile.id}', this)" />
                </form>
              </li>
            </c:forEach>
          </c:otherwise>
        </c:choose>
      </ul>
    </c:otherwise>
  </c:choose>
</div>
