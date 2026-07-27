/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 *
 * Adds an "UPLOAD NEW VERSION" action to the built-in Insert/Edit Image
 * dialog. Instead of inserting a brand new image reference, the selected
 * file replaces the image everywhere it is currently used (based on its
 * image web path/id), with a local preview shown until the change is saved
 * or published.
 *
 * Configure the upload endpoint via the `image_version_upload_url` editor
 * option, e.g.:
 *
 *   image_version_upload_url: '${ctx}/image-upload?widget=imageUpload1&token=${userSession.formToken}'
 *
 * @author matt rajkowski
 * @created 7/24/26
 */
(function () {
  'use strict';

  // These are intentionally shared across editor instances since only a
  // single content editor is normally present on a page at one time.
  var state = {
    uploadAsVersion: false,
    imageId: null,
    imageWebPath: null
  };
  var pendingUploads = [];
  var activeEditor = null;
  var toggleContainer = null;
  var fileInput = null;
  var dialogWatcherInterval = null;
  var uploadDialogPollInterval = null;

  tinymce.PluginManager.add('imageversion', function (editor) {

    editor.options.register('image_version_upload_url', {
      processor: 'string',
      default: '/image-upload'
    });

    function getUploadUrl() {
      return editor.options.get('image_version_upload_url');
    }

    // Users can upload a new version of an image, affecting the image used in multiple places
    function uploadVersionFileToServer(file, imageId, imageWebPath) {
      return new Promise(function (resolve, reject) {
        var xhr = new XMLHttpRequest();
        xhr.withCredentials = true;
        xhr.open('POST', getUploadUrl());

        xhr.onload = function () {
          if (xhr.status < 200 || xhr.status >= 300) {
            reject('HTTP Error: ' + xhr.status);
            return;
          }

          var json = null;
          try {
            json = JSON.parse(xhr.responseText);
          } catch (e) {
            reject('Invalid JSON response');
            return;
          }

          if (!json || typeof json.location !== 'string') {
            reject('Invalid upload response');
            return;
          }

          resolve(json.location);
        };

        xhr.onerror = function () {
          reject('Image upload failed');
        };

        var formData = new FormData();
        formData.append('file', file, file.name);
        if (imageId) {
          formData.append('imageId', imageId);
        }
        if (imageWebPath) {
          formData.append('imageWebPath', imageWebPath);
        }

        xhr.send(formData);
      });
    }

    function escapeRegExp(value) {
      return value
        .replace(/[.*+?^()|[\]\\]/g, '\\$&')
        .replace(/\$/g, '\\$&');
    }

    function replaceUrlInHtml(html, oldUrl, newUrl) {
      if (!html || !oldUrl) {
        return html;
      }
      return html.replace(new RegExp(escapeRegExp(oldUrl), 'g'), newUrl);
    }

    function replaceUrlsForImageReference(html, imageWebPath, finalUrl, fallbackUrl) {
      if (!html || !finalUrl) {
        return html;
      }

      if (imageWebPath) {
        var escapedWebPath = escapeRegExp(imageWebPath);
        var refPattern = new RegExp('(?:https?:\\/\\/[^"\\' + "'" + '\\s>]+)?\\/assets\\/img\\/' + escapedWebPath + '(?:-\\d+)?\\/[^"\\' + "'" + '\\s>]+(?:\\?v=\\d+)?', 'g');
        return html.replace(refPattern, finalUrl);
      }

      if (fallbackUrl) {
        return replaceUrlInHtml(html, fallbackUrl, finalUrl);
      }

      return html;
    }

    function buildStagedVersionUrl(originalUrl, imageWebPath, fileName) {
      if (!originalUrl || !fileName) {
        return originalUrl;
      }

      var encodedFileName = encodeURIComponent(fileName).replace(/%2F/g, '/');
      var baseRef = imageWebPath || (parseImageReferenceFromUrl(originalUrl) || {}).imageWebPath;
      if (baseRef) {
        return '/assets/img/' + baseRef + '/' + encodedFileName;
      }

      var noQuery = originalUrl.split('?')[0].split('#')[0];
      var lastSlash = noQuery.lastIndexOf('/');
      if (lastSlash === -1) {
        return originalUrl;
      }
      return noQuery.substring(0, lastSlash + 1) + encodedFileName;
    }

    function upsertPendingVersionUpload(upload) {
      if (upload.markerId) {
        var markerIndex = pendingUploads.findIndex(function (item) {
          return item.markerId === upload.markerId;
        });
        if (markerIndex !== -1) {
          var existingMarkerItem = pendingUploads[markerIndex];
          if (existingMarkerItem.previewUrl && existingMarkerItem.previewUrl !== upload.previewUrl) {
            URL.revokeObjectURL(existingMarkerItem.previewUrl);
          }
          pendingUploads[markerIndex] = upload;
          return;
        }
      }

      var existingIndex = pendingUploads.findIndex(function (item) {
        if (upload.imageWebPath && item.imageWebPath) {
          return item.imageWebPath === upload.imageWebPath;
        }
        return item.originalUrl === upload.originalUrl;
      });

      if (existingIndex !== -1) {
        var existingItem = pendingUploads[existingIndex];
        if (existingItem.previewUrl && existingItem.previewUrl !== upload.previewUrl) {
          URL.revokeObjectURL(existingItem.previewUrl);
        }
        pendingUploads[existingIndex] = upload;
        return;
      }

      pendingUploads.push(upload);
    }

    function flushPendingVersionUploads(isPublishAction) {
      if (!activeEditor || !pendingUploads.length) {
        return Promise.resolve();
      }

      var body = activeEditor.getBody();

      if (!isPublishAction) {
        for (var i = 0; i < pendingUploads.length; i += 1) {
          var pendingRollback = pendingUploads[i];
          if (body && pendingRollback.markerId) {
            var node = body.querySelector('img[data-pending-version-id="' + pendingRollback.markerId + '"]');
            if (node && pendingRollback.originalUrl) {
              node.setAttribute('src', pendingRollback.originalUrl);
              node.removeAttribute('data-pending-version-id');
            }
          }
          if (pendingRollback.previewUrl) {
            URL.revokeObjectURL(pendingRollback.previewUrl);
          }
        }
        activeEditor.save();
        pendingUploads.length = 0;
        state.uploadAsVersion = false;
        refreshUploadVersionActionButton();
        return Promise.resolve();
      }

      var html = activeEditor.getContent();

      return pendingUploads.reduce(function (chain, pending) {
        return chain.then(function () {
          return uploadVersionFileToServer(pending.file, pending.imageId, pending.imageWebPath).then(function (finalUrl) {
            var appliedToNode = false;
            if (body && pending.markerId) {
              var node = body.querySelector('img[data-pending-version-id="' + pending.markerId + '"]');
              if (node) {
                node.setAttribute('src', finalUrl);
                node.removeAttribute('data-pending-version-id');
                appliedToNode = true;
              }
            }

            if (!appliedToNode) {
              html = replaceUrlsForImageReference(html, pending.imageWebPath, finalUrl, pending.originalUrl);
            }

            if (pending.originalUrl) {
              html = replaceUrlInHtml(html, pending.originalUrl, finalUrl);
            }
            if (pending.stagedUrl) {
              html = replaceUrlInHtml(html, pending.stagedUrl, finalUrl);
            }
            if (pending.previewUrl) {
              html = replaceUrlInHtml(html, pending.previewUrl, finalUrl);
            }

            if (pending.previewUrl) {
              URL.revokeObjectURL(pending.previewUrl);
            }
          });
        });
      }, Promise.resolve()).then(function () {
        pendingUploads.length = 0;
        activeEditor.setContent(html);
        activeEditor.save();
        state.uploadAsVersion = false;
        refreshUploadVersionActionButton();
      });
    }

    function hideToggle() {
      if (toggleContainer) {
        toggleContainer.style.display = 'none';
      }
    }

    function getImageDialogSourceInput(dialogRoot) {
      if (!dialogRoot) {
        return null;
      }

      var sourceLabel = Array.from(dialogRoot.querySelectorAll('.tox-label')).find(function (label) {
        return (label.textContent || '').trim().toLowerCase() === 'source';
      });

      if (sourceLabel) {
        var sourceGroup = sourceLabel.closest('.tox-form__group');
        if (sourceGroup) {
          var sourceInputFromGroup = sourceGroup.querySelector('input.tox-textfield');
          if (sourceInputFromGroup) {
            return sourceInputFromGroup;
          }
        }
      }

      return dialogRoot.querySelector('input.tox-textfield');
    }

    function normalizeImageUrlForCompare(url) {
      if (!url) {
        return '';
      }
      var normalized = String(url).trim();
      if (normalized.indexOf(location.origin) === 0) {
        normalized = normalized.substring(location.origin.length);
      }
      var qIdx = normalized.indexOf('?');
      if (qIdx !== -1) {
        normalized = normalized.substring(0, qIdx);
      }
      var hIdx = normalized.indexOf('#');
      if (hIdx !== -1) {
        normalized = normalized.substring(0, hIdx);
      }
      return normalized;
    }

    function findEditorImageNodeForUrl(ed, imageUrl) {
      if (!ed || !imageUrl) {
        return null;
      }
      var body = ed.getBody();
      if (!body) {
        return null;
      }

      var target = normalizeImageUrlForCompare(imageUrl);
      var nodes = body.querySelectorAll('img');
      for (var i = 0; i < nodes.length; i += 1) {
        var node = nodes[i];
        var nodeSrc = normalizeImageUrlForCompare(node.getAttribute('src') || '');
        if (nodeSrc === target || (target && nodeSrc.endsWith(target))) {
          return node;
        }
      }
      return null;
    }

    function applyPendingPreviewToEditor(upload) {
      if (!activeEditor || !upload || !upload.previewUrl) {
        return;
      }

      var body = activeEditor.getBody();
      if (!body) {
        return;
      }

      var node = null;
      if (upload.markerId) {
        node = body.querySelector('img[data-pending-version-id="' + upload.markerId + '"]');
      }
      if (!node && upload.originalUrl) {
        node = findEditorImageNodeForUrl(activeEditor, upload.originalUrl);
      }
      if (!node && upload.stagedUrl) {
        node = findEditorImageNodeForUrl(activeEditor, upload.stagedUrl);
      }
      if (!node) {
        return;
      }

      if (!upload.markerId) {
        upload.markerId = 'pending-version-' + Date.now() + '-' + Math.floor(Math.random() * 100000);
      }
      node.setAttribute('data-pending-version-id', upload.markerId);
      node.setAttribute('src', upload.previewUrl);
      activeEditor.save();
    }

    function reapplyAllPendingPreviews() {
      for (var i = 0; i < pendingUploads.length; i += 1) {
        applyPendingPreviewToEditor(pendingUploads[i]);
      }
    }

    function ensureUploadVersionFileInput(dialogRoot) {
      if (!fileInput) {
        fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.accept = 'image/*';
        fileInput.style.display = 'none';
        document.body.appendChild(fileInput);
      }

      fileInput.onchange = function () {
        var file = fileInput.files && fileInput.files[0];
        if (!file) {
          state.uploadAsVersion = false;
          refreshUploadVersionActionButton();
          return;
        }

        state.uploadAsVersion = true;
        refreshUploadVersionActionButton();

        var sourceInput = getImageDialogSourceInput(dialogRoot);
        var originalUrl = sourceInput && sourceInput.value ? sourceInput.value : null;
        if (!originalUrl) {
          state.uploadAsVersion = false;
          refreshUploadVersionActionButton();
          fileInput.value = '';
          return;
        }

        var previewUrl = URL.createObjectURL(file);
        var markerId = null;
        if (activeEditor && activeEditor.selection) {
          var selectedNode = activeEditor.selection.getNode();
          if (selectedNode && selectedNode.nodeName === 'IMG') {
            markerId = selectedNode.getAttribute('data-pending-version-id');
          }
        }

        upsertPendingVersionUpload({
          file: file,
          imageId: state.imageId,
          imageWebPath: state.imageWebPath,
          originalUrl: originalUrl,
          stagedUrl: buildStagedVersionUrl(originalUrl, state.imageWebPath, file.name),
          previewUrl: previewUrl,
          markerId: markerId
        });

        var pendingUpload = pendingUploads.find(function (item) {
          if (markerId) {
            return item.markerId === markerId;
          }
          return item.imageWebPath === state.imageWebPath || item.originalUrl === originalUrl;
        });

        if (sourceInput && pendingUpload && pendingUpload.stagedUrl) {
          sourceInput.value = pendingUpload.stagedUrl;
          sourceInput.dispatchEvent(new Event('input', { bubbles: true }));
          sourceInput.dispatchEvent(new Event('change', { bubbles: true }));
        }

        if (pendingUpload) {
          applyPendingPreviewToEditor(pendingUpload);
        }

        state.uploadAsVersion = false;
        refreshUploadVersionActionButton();
        fileInput.value = '';
      };
    }

    function refreshUploadVersionActionButton() {
      var button = document.getElementById('content-editor-upload-new-version-btn');
      if (!button) {
        return;
      }
      var enabled = !!state.imageWebPath;
      button.disabled = !enabled;
      button.style.opacity = enabled ? '1' : '0.5';
      button.textContent = 'UPLOAD NEW VERSION';
    }

    function ensureToggle(ed, dialogRoot) {
      if (!toggleContainer) {
        var container = document.createElement('div');
        container.id = 'content-editor-upload-version-toggle';
        container.style.position = 'fixed';
        container.style.zIndex = '100100';
        container.style.background = '#ffffff';
        container.style.border = '1px solid #d9d9d9';
        container.style.borderRadius = '4px';
        container.style.padding = '0.25rem';
        container.style.boxShadow = '0 2px 6px rgba(0,0,0,0.12)';
        container.style.display = 'none';

        var button = document.createElement('button');
        button.type = 'button';
        button.id = 'content-editor-upload-new-version-btn';
        button.style.border = '1px solid #cfd3d8';
        button.style.borderRadius = '4px';
        button.style.background = '#ffffff';
        button.style.padding = '0.35rem 0.65rem';
        button.style.fontSize = '0.78rem';
        button.style.fontWeight = '600';
        button.style.letterSpacing = '0.02em';
        button.style.cursor = 'pointer';
        button.addEventListener('click', function () {
          var activeDialogRoot = getActiveImageDialogRoot() || dialogRoot;
          if (activeEditor) {
            updateImageUploadStateFromSource(activeEditor, activeDialogRoot);
          }
          if (!state.imageWebPath) {
            return;
          }
          state.uploadAsVersion = false;
          refreshUploadVersionActionButton();
          ensureUploadVersionFileInput(activeDialogRoot);
          fileInput.click();
        });

        button.textContent = 'UPLOAD NEW VERSION';
        container.appendChild(button);
        document.body.appendChild(container);
        toggleContainer = container;
      }

      if (!state.imageWebPath) {
        state.uploadAsVersion = false;
      }
      refreshUploadVersionActionButton();

      var rect = dialogRoot.getBoundingClientRect();
      toggleContainer.style.left = Math.max(10, rect.left + 12) + 'px';
      toggleContainer.style.top = Math.max(10, rect.bottom - 50) + 'px';
      toggleContainer.style.display = 'block';
    }

    function parseImageReferenceFromUrl(imageUrl) {
      if (!imageUrl || typeof imageUrl !== 'string') {
        return null;
      }

      var marker = '/assets/img/';
      var markerIndex = imageUrl.indexOf(marker);
      if (markerIndex === -1) {
        return null;
      }

      var pathPart = imageUrl.substring(markerIndex + marker.length);
      var queryIndex = pathPart.indexOf('?');
      if (queryIndex !== -1) {
        pathPart = pathPart.substring(0, queryIndex);
      }
      var hashIndex = pathPart.indexOf('#');
      if (hashIndex !== -1) {
        pathPart = pathPart.substring(0, hashIndex);
      }

      var pathSegments = pathPart.split('/');
      if (!pathSegments.length || !pathSegments[0]) {
        return null;
      }

      var firstSegment = pathSegments[0];
      var imageId = null;
      var imageWebPath = firstSegment;

      if (firstSegment.indexOf('confluence-') === 0) {
        var confluenceWithIdMatch = firstSegment.match(/^(confluence-\d+)-(\d+)$/);
        if (confluenceWithIdMatch) {
          imageWebPath = confluenceWithIdMatch[1];
          imageId = parseInt(confluenceWithIdMatch[2], 10);
        }
      } else {
        var dashedIdMatch = firstSegment.match(/^(.*)-(\d+)$/);
        if (dashedIdMatch) {
          imageWebPath = dashedIdMatch[1];
          imageId = parseInt(dashedIdMatch[2], 10);
        }
      }

      if (!imageWebPath) {
        return null;
      }

      return {
        imageId: Number.isNaN(imageId) ? null : imageId,
        imageWebPath: imageWebPath
      };
    }

    function updateImageUploadStateFromSource(ed, dialogRoot) {
      var sourceValue = '';

      if (dialogRoot) {
        var sourceInput = getImageDialogSourceInput(dialogRoot);
        if (sourceInput && sourceInput.value) {
          sourceValue = sourceInput.value;
        }
      }

      if (!sourceValue) {
        var selectedNode = ed.selection ? ed.selection.getNode() : null;
        if (selectedNode && selectedNode.nodeName === 'IMG') {
          sourceValue = selectedNode.getAttribute('src') || '';
        }
      }

      var parsedReference = parseImageReferenceFromUrl(sourceValue);
      state.imageId = parsedReference ? parsedReference.imageId : null;
      state.imageWebPath = parsedReference ? parsedReference.imageWebPath : null;
    }

    function getActiveImageDialogRoot() {
      var dialogs = document.querySelectorAll('.tox-dialog');
      if (!dialogs.length) {
        return null;
      }

      return Array.from(dialogs).find(function (dialog) {
        var label = (dialog.getAttribute('aria-label') || '').toLowerCase();
        var text = (dialog.textContent || '').toLowerCase();
        return label.indexOf('image') !== -1 || text.indexOf('insert/edit image') !== -1;
      }) || dialogs[dialogs.length - 1];
    }

    function ensureUploadVersionActionButton(ed) {
      activeEditor = ed;
      var dialogs = document.querySelectorAll('.tox-dialog');
      if (!dialogs.length) {
        hideToggle();
        return;
      }

      var dialogRoot = Array.from(dialogs).find(function (dialog) {
        var label = (dialog.getAttribute('aria-label') || '').toLowerCase();
        var text = (dialog.textContent || '').toLowerCase();
        return label.indexOf('image') !== -1 || text.indexOf('insert/edit image') !== -1;
      }) || dialogs[dialogs.length - 1];

      var dialogText = (dialogRoot.textContent || '').toLowerCase();
      if (dialogText.indexOf('insert/edit image') === -1) {
        hideToggle();
        return;
      }

      updateImageUploadStateFromSource(ed, dialogRoot);

      ensureToggle(ed, dialogRoot);

      var sourceInput = getImageDialogSourceInput(dialogRoot);
      if (sourceInput) {
        var handleSourceValueChange = function () {
          updateImageUploadStateFromSource(ed, dialogRoot);
          ensureToggle(ed, dialogRoot);
        };
        sourceInput.addEventListener('input', handleSourceValueChange);
        sourceInput.addEventListener('change', handleSourceValueChange);
      }
    }

    function startDialogWatcher(ed) {
      if (dialogWatcherInterval) {
        return;
      }
      dialogWatcherInterval = setInterval(function () {
        ensureUploadVersionActionButton(ed);
      }, 250);
    }

    // Handles images dropped/pasted directly into the editor (or uploaded via
    // the dialog's own Upload tab), tagging the request as a new version when
    // one was staged via the "UPLOAD NEW VERSION" button.
    function uploadImageFromEditor(blobInfo, progress) {
      return new Promise(function (resolve, reject) {
        var xhr = new XMLHttpRequest();
        xhr.withCredentials = true;
        xhr.open('POST', getUploadUrl());

        xhr.upload.onprogress = function (event) {
          progress(event.loaded / event.total * 100);
        };

        xhr.onload = function () {
          if (xhr.status < 200 || xhr.status >= 300) {
            reject('HTTP Error: ' + xhr.status);
            return;
          }

          var json = null;
          try {
            json = JSON.parse(xhr.responseText);
          } catch (e) {
            reject('Invalid JSON response');
            return;
          }

          if (!json || typeof json.location !== 'string') {
            reject('Invalid upload response');
            return;
          }

          state.uploadAsVersion = false;
          refreshUploadVersionActionButton();
          resolve(json.location);
        };

        xhr.onerror = function () {
          state.uploadAsVersion = false;
          refreshUploadVersionActionButton();
          reject('Image upload failed');
        };

        var formData = new FormData();
        formData.append('file', blobInfo.blob(), blobInfo.filename());

        if (state.uploadAsVersion && state.imageWebPath) {
          if (state.imageId) {
            formData.append('imageId', state.imageId);
          }
          formData.append('imageWebPath', state.imageWebPath);
        }

        xhr.send(formData);
      });
    }

    editor.on('init', function () {
      activeEditor = editor;
      startDialogWatcher(editor);
    });

    editor.on('OpenWindow', function () {
      if (uploadDialogPollInterval) {
        clearInterval(uploadDialogPollInterval);
      }
      setTimeout(function () {
        ensureUploadVersionActionButton(editor);
      }, 50);
      uploadDialogPollInterval = setInterval(function () {
        ensureUploadVersionActionButton(editor);
      }, 150);
      setTimeout(function () {
        if (uploadDialogPollInterval) {
          clearInterval(uploadDialogPollInterval);
          uploadDialogPollInterval = null;
        }
      }, 3000);
    });

    editor.on('CloseWindow', function () {
      state.uploadAsVersion = false;
      hideToggle();
      setTimeout(function () {
        reapplyAllPendingPreviews();
      }, 25);
      if (uploadDialogPollInterval) {
        clearInterval(uploadDialogPollInterval);
        uploadDialogPollInterval = null;
      }
    });

    editor.on('remove', function () {
      if (dialogWatcherInterval) {
        clearInterval(dialogWatcherInterval);
        dialogWatcherInterval = null;
      }
    });

    return {
      hasPendingUploads: function () {
        return pendingUploads.length > 0;
      },
      flushPendingUploads: function (isPublishAction) {
        return flushPendingVersionUploads(isPublishAction);
      },
      uploadImageFromEditor: function (blobInfo, progress) {
        return uploadImageFromEditor(blobInfo, progress);
      },
      getMetadata: function () {
        return {
          name: 'Image Version Upload Plugin',
          url: 'https://www.github.com/rajkowski/cms-platform'
        };
      }
    };
  });
})();
