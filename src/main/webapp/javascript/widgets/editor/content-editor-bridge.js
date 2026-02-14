/**
 * Content Editor Bridge for Visual Content Editor
 * Handles HTML Editor integration, content editing, saving drafts, and publishing
 * 
 * @author matt rajkowski
 * @created 02/07/26 02:00 PM
 */

class ContentEditorBridge {
  constructor() {
    this.currentContentId = null;
    this.currentUniqueId = null;
    this.tinyMceEditor = null;
    this.autoSaveTimer = null;
    this.autoSaveInterval = 30000; // 30 seconds
    this.isDirty = false;
    this.isSaving = false;
    this.previewMode = 'content';
    this.lastPreviewLink = null;
  }

  /**
   * Initialize the content editor bridge
   */
  init() {
    this.setupEventListeners();
    this.initializeTinyMCE();
  }

  /**
   * Set up event listeners
   */
  setupEventListeners() {
    const saveDraftBtn = document.getElementById('save-draft-btn');
    const publishBtn = document.getElementById('publish-btn');

    if (saveDraftBtn) {
      saveDraftBtn.addEventListener('click', () => this.saveDraft());
    }

    if (publishBtn) {
      publishBtn.addEventListener('click', () => this.publish());
    }
  }

  /**
   * Initialize TinyMCE editor
   */
  initializeTinyMCE() {
    if (typeof tinymce === 'undefined') {
      console.error('TinyMCE not loaded');
      return;
    }

    tinymce.init({
      selector: '#content-html-editor',
      branding: false,
      width: '100%',
      height: '100%',
      resize: false,
      menubar: false,
      relative_urls: false,
      convert_urls: true,
      convert_unsafe_embeds: true,
      sandbox_iframes: true,
      browser_spellcheck: true,
      plugins: 'advlist autolink lists link image charmap preview anchor searchreplace visualblocks code insertdatetime media table wordcount',
      toolbar: 'link image media table | undo redo | blocks | bold italic backcolor | bullist numlist outdent indent hr | removeformat | visualblocks code',
      image_class_list: [
        { title: 'None', value: '' },
        { title: 'Image Left/Wrap Text Right', value: 'image-left' },
        { title: 'Image Right/Wrap Text left', value: 'image-right' },
        { title: 'Image Center On Line', value: 'image-center' }
      ],
      file_picker_types: 'file image media',
      // link_default_target: '_blank',
      file_picker_callback: function (callback, value, meta) {
        FileBrowser(value, meta.filetype, function (fileUrl) {
          callback(fileUrl);
        });
      },
      images_upload_url: '${ctx}/image-upload?widget=imageUpload1&token=${userSession.formToken}', // return { "location": "folder/sub-folder/new-location.png" }
      paste_data_images: true,
      automatic_uploads: true,
      setup: (editor) => {
        this.tinyMceEditor = editor;

        editor.on('change', () => {
          this.isDirty = true;
          this.startAutoSave();
        });

        editor.on('init', () => {
          // Initial setup complete
        });

        editor.on('SetContent', () => {
          // After content is set
        });
      }
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
        onMessage: function (dialogApi, details) {
          callback(details.content);
          instanceApi.close();
        }
      });
      return false;
    }
  }

  /**
   * Load content into editor
   */
  loadContent(contentId, uniqueId) {
    this.currentContentId = contentId;
    this.currentUniqueId = uniqueId;

    fetch(`/json/content/get?contentId=${contentId}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok' && data.data) {
          const content = data.data;

          // Update middle panel title with content info
          const iconEl = document.getElementById('middle-panel-icon');
          const titleTextEl = document.getElementById('middle-panel-title-text');
          if (iconEl) {
            iconEl.className = 'far fa-edit';
          }
          if (titleTextEl) {
            titleTextEl.textContent = `Edit: ${this.escapeHtml(content.unique_id)} (ID: ${contentId})`;
          }

          // Load content (prefer draft over published)
          const contentText = content.draft_content || content.content || '';

          if (this.tinyMceEditor) {
            this.tinyMceEditor.setContent(contentText);
          } else {
            document.getElementById('content-html-editor').value = contentText;
          }

          if (this.resizeEditor) {
            this.resizeEditor();
          }

          // Highlight directives
          this.highlightDirectives();

          // Show editor
          const editor = document.getElementById('content-editor');
          if (editor) {
            editor.style.display = 'block';
          }

          // Hide other views
          const sitemap = document.getElementById('sitemap-explorer');
          const library = document.getElementById('page-library-explorer');
          const calendar = document.getElementById('calendar-view');
          if (sitemap) sitemap.style.display = 'none';
          if (library) library.style.display = 'none';
          if (calendar) calendar.style.display = 'none';

          this.isDirty = false;
        } else {
          console.error('Failed to load content:', data.error);
          this.showError('Failed to load content');
        }
      })
      .catch(error => {
        console.error('Error loading content:', error);
        this.showError('Error loading content: ' + error.message);
      });

    this.previewMode = 'content';
    this.lastPreviewLink = null;
  }

  /**
   * Set preview mode
   */
  setPreviewMode(mode) {
    this.previewMode = mode;
  }

  /**
   * Get preview mode
   */
  getPreviewMode() {
    return this.previewMode;
  }

  /**
   * Show properties in the right panel
   */
  showProperties(properties) {
    const propsDiv = document.getElementById('properties-content');
    if (!propsDiv || !properties) return;

    const fields = Array.isArray(properties.fields) ? properties.fields : [];
    let html = '';
    html += `<h4>${this.escapeHtml(properties.type || 'Properties')}</h4>`;
    if (properties.title) {
      html += `<p style="margin: 4px 0 12px; color: var(--editor-text-muted);">${this.escapeHtml(properties.title)}</p>`;
    }
    if (fields.length > 0) {
      html += '<div class="property-fields">';
      fields.forEach(field => {
        const label = this.escapeHtml(field.label || '');
        const value = this.escapeHtml(String(field.value ?? ''));
        html += `<div class="property-row"><strong>${label}:</strong> <span>${value}</span></div>`;
      });
      html += '</div>';
    }

    propsDiv.innerHTML = html;
  }

  /**
   * Show a page preview in the preview iframe
   */
  showPagePreview(pageLink) {
    const iframe = document.getElementById('preview-iframe');
    if (!iframe) return;

    this.previewMode = 'page';

    if (!pageLink) {
      this.lastPreviewLink = null;
      this.showPreviewMessage('Select a page with a valid link to preview.');
      return;
    }

    this.lastPreviewLink = pageLink;
    iframe.src = pageLink;
  }

  /**
   * Refresh the current page preview
   */
  refreshPagePreview() {
    if (this.previewMode !== 'page' || !this.lastPreviewLink) return;

    const iframe = document.getElementById('preview-iframe');
    if (iframe) {
      iframe.src = this.lastPreviewLink;
    }
  }

  /**
   * Show a message in the preview iframe
   */
  showPreviewMessage(message) {
    const iframe = document.getElementById('preview-iframe');
    if (!iframe || !iframe.contentDocument) return;

    const doc = iframe.contentDocument || iframe.contentWindow.document;
    doc.open();
    doc.write(`<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body style="font-family: Helvetica, Arial, sans-serif; padding: 20px; color: #555;">
      <p>${this.escapeHtml(message)}</p>
    </body></html>`);
    doc.close();
  }

  /**
   * Save content as draft
   */
  saveDraft() {
    if (this.isSaving || !this.currentContentId) return;

    this.isSaving = true;
    this.showSaving();

    const content = this.getEditorContent();

    const params = new FormData();
    params.append('contentId', this.currentContentId);
    params.append('content', content);
    params.append('isDraft', 'true');
    params.append('token', window.getFormToken());

    fetch('/json/content/save-draft', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        this.isSaving = false;
        this.hideSaving();

        if (data.status === 'ok') {
          this.isDirty = false;
          this.showSuccess('Draft saved successfully');
          this.updateContentDraftBadge(true);
        } else {
          this.showError(data.error || 'Failed to save draft');
        }
      })
      .catch(error => {
        this.isSaving = false;
        this.hideSaving();
        console.error('Error saving draft:', error);
        this.showError('Error saving draft: ' + error.message);
      });
  }

  /**
   * Publish content
   */
  publish() {
    if (this.isSaving || !this.currentContentId) return;

    // Confirm action
    if (!confirm('Are you sure you want to publish this content? Any draft will be cleared.')) {
      return;
    }

    this.isSaving = true;
    this.showSaving();

    const content = this.getEditorContent();

    const params = new FormData();
    params.append('contentId', this.currentContentId);
    params.append('content', content);
    params.append('token', window.getFormToken());

    fetch('/json/content/publish', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        this.isSaving = false;
        this.hideSaving();

        if (data.status === 'ok') {
          this.isDirty = false;
          this.showSuccess('Content published successfully');
          this.updateContentDraftBadge(false);
        } else {
          this.showError(data.error || 'Failed to publish content');
        }
      })
      .catch(error => {
        this.isSaving = false;
        this.hideSaving();
        console.error('Error publishing content:', error);
        this.showError('Error publishing content: ' + error.message);
      });
  }

  /**
   * Start auto-save timer
   */
  startAutoSave() {
    if (this.autoSaveTimer) {
      clearTimeout(this.autoSaveTimer);
    }

    this.autoSaveTimer = setTimeout(() => {
      if (this.isDirty) {
        this.saveDraft();
      }
    }, this.autoSaveInterval);
  }

  /**
   * Get editor content
   */
  getEditorContent() {
    if (this.tinyMceEditor) {
      return this.tinyMceEditor.getContent();
    } else {
      return document.getElementById('content-html-editor').value;
    }
  }

  updateContentDraftBadge(hasDraft) {
    const listManager = window.visualContentEditor?.contentListManager;
    if (listManager && typeof listManager.setDraftStatus === 'function') {
      listManager.setDraftStatus(this.currentContentId, hasDraft);
    }
  }

  /**
   * Highlight content directives ${uniqueId:...}
   */
  highlightDirectives() {
    const directiveRegex = /\$\{uniqueId:([a-zA-Z0-9\-_]+)\}/g;
    const content = this.getEditorContent();
    const matches = [...content.matchAll(directiveRegex)];

    if (matches.length > 0) {
      // Show directive information panel
      const propsDiv = document.getElementById('properties-content');
      if (propsDiv) {
        let html = '<h4>Content Directives Found</h4><ul>';
        matches.forEach(match => {
          const uniqueId = match[1];
          html += `<li><code>${this.escapeHtml(match[0])}</code><br><small>References: ${this.escapeHtml(uniqueId)}</small></li>`;
        });
        html += '</ul>';
        propsDiv.innerHTML = html;
      }
    }
  }

  /**
   * Resolve content directives in preview
   */
  resolveContentDirectives(content) {
    return content; // Directives are resolved server-side
  }

  /**
   * Show saving indicator
   */
  showSaving() {
    const loadingIndicator = document.getElementById('loading-indicator');
    if (loadingIndicator) {
      loadingIndicator.style.display = 'inline-block';
    }
  }

  /**
   * Hide saving indicator
   */
  hideSaving() {
    const loadingIndicator = document.getElementById('loading-indicator');
    if (loadingIndicator) {
      loadingIndicator.style.display = 'none';
    }
  }

  /**
   * Show success message
   */
  showSuccess(message) {
    this.showNotification(message, 'success');
  }

  /**
   * Show error message
   */
  showError(message) {
    this.showNotification(message, 'error');
  }

  /**
   * Show notification
   */
  showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 15px 20px;
      background: ${type === 'success' ? '#28a745' : type === 'error' ? '#dc3545' : '#17a2b8'};
      color: white;
      border-radius: 4px;
      z-index: 10000;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    `;

    document.body.appendChild(notification);

    setTimeout(() => {
      notification.style.opacity = '0';
      notification.style.transition = 'opacity 0.3s ease';
      setTimeout(() => notification.remove(), 300);
    }, 3000);
  }

  /**
   * Escape HTML special characters
   */
  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}
