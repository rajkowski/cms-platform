/**
 * Page Content Blocks Manager for Visual Content Editor
 * Handles loading and displaying content blocks referenced by a selected page
 * in the right panel preview tab
 *
 * @author matt rajkowski
 * @created 02/14/26 10:00 AM
 */

class PageContentBlocksManager {
  constructor(editorBridge) {
    this.editorBridge = editorBridge;
    this.contentBlocks = [];
    this.currentPageLink = null;
    this.isLoading = false;
  }

  /**
   * Initialize the content blocks manager
   */
  init() {
    this.setupModalListeners();
  }

  /**
   * Load content blocks for a given page link
   */
  loadContentBlocks(pageLink) {
    if (!pageLink || this.isLoading) return;

    this.currentPageLink = pageLink;
    this.isLoading = true;
    this.showLoading();

    fetch(`/json/pages/content-blocks?link=${encodeURIComponent(pageLink)}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        this.isLoading = false;
        if (data.status === 'ok' && Array.isArray(data.data)) {
          this.contentBlocks = data.data;
          this.renderContentBlocks();
        } else {
          this.showEmpty();
        }
      })
      .catch(error => {
        this.isLoading = false;
        console.error('Error loading content blocks:', error);
        this.showError('Error loading content blocks');
      });
  }

  /**
   * Show loading state
   */
  showLoading() {
    const container = document.getElementById('page-content-blocks-list');
    if (!container) return;
    container.innerHTML = '<div class="content-blocks-status">Loading content blocks...</div>';
  }

  /**
   * Show empty state
   */
  showEmpty() {
    const container = document.getElementById('page-content-blocks-list');
    if (!container) return;
    container.innerHTML = '<div class="content-blocks-status">No content blocks found for this page</div>';
  }

  /**
   * Show error state
   */
  showError(message) {
    const container = document.getElementById('page-content-blocks-list');
    if (!container) return;
    container.innerHTML = `<div class="content-blocks-status content-blocks-error">${this.escapeHtml(message)}</div>`;
  }

  /**
   * Clear the content blocks list
   */
  clear() {
    this.contentBlocks = [];
    this.currentPageLink = null;
    const container = document.getElementById('page-content-blocks-list');
    if (container) {
      container.innerHTML = '<div class="content-blocks-status">Select a page to view its content blocks</div>';
    }
  }

  /**
   * Render the content blocks list
   */
  renderContentBlocks() {
    const container = document.getElementById('page-content-blocks-list');
    if (!container) return;

    if (this.contentBlocks.length === 0) {
      this.showEmpty();
      return;
    }

    container.innerHTML = '';

    this.contentBlocks.forEach(block => {
      const item = document.createElement('div');
      item.className = 'content-block-item';
      item.dataset.contentId = block.id;
      item.dataset.uniqueId = block.uniqueId;

      const hasDraftBadge = block.hasDraft ? '<span class="content-block-draft-badge">Draft</span>' : '';

      item.innerHTML = `
        <div class="content-block-header">
          <span class="content-block-unique-id">${this.escapeHtml(block.uniqueId)}</span>
          ${hasDraftBadge}
        </div>
        <div class="content-block-snippet">${this.escapeHtml(block.snippet || 'Empty content')}</div>
        <div class="content-block-meta">
          ${block.modified ? this.formatDate(block.modified) : ''}${block.modifiedBy ? ' by ' + this.escapeHtml(block.modifiedBy) : ''}
        </div>
      `;

      item.addEventListener('click', () => {
        this.openContentEditor(block.id, block.uniqueId);
      });

      container.appendChild(item);
    });
  }

  /**
   * Open the floating content editor modal for a content block
   */
  openContentEditor(contentId, uniqueId) {
    const modal = document.getElementById('content-block-editor-modal');
    if (!modal) return;

    // Show modal
    modal.style.display = 'block';

    const titleEl = document.getElementById('content-block-editor-title');
    if (titleEl) {
      titleEl.textContent = `Edit: ${uniqueId}`;
    }

    // Store current content info
    modal.dataset.contentId = contentId;
    modal.dataset.uniqueId = uniqueId;

    // Load content into the modal editor
    this.loadContentIntoModal(contentId);
  }

  /**
   * Load content into the modal TinyMCE editor
   */
  loadContentIntoModal(contentId) {
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
          const contentText = content.draft_content || content.content || '';

          // Initialize or set content in modal editor
          const editor = tinymce.get('content-block-html-editor');
          if (editor) {
            editor.setContent(contentText);
          } else {
            this.initModalTinyMCE(contentText);
          }
        } else {
          console.error('Failed to load content for modal editor');
        }
      })
      .catch(error => {
        console.error('Error loading content for modal editor:', error);
      });
  }

  /**
   * Initialize TinyMCE for the modal editor
   */
  initModalTinyMCE(initialContent) {
    if (typeof tinymce === 'undefined') return;

    // Remove any existing instance
    const existingEditor = tinymce.get('content-block-html-editor');
    if (existingEditor) {
      existingEditor.remove();
    }

    tinymce.init({
      selector: '#content-block-html-editor',
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
      file_picker_callback: function (callback, value, meta) {
        var cmsType = 'image';
        if (meta.filetype === 'media') {
          cmsType = 'video';
        } else if (meta.filetype === 'file') {
          cmsType = 'file';
        }
        var cmsURL = '/' + cmsType + '-browser';
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
      },
      paste_data_images: true,
      automatic_uploads: true,
      setup: (editor) => {
        editor.on('init', () => {
          if (initialContent) {
            editor.setContent(initialContent);
          }
        });
      }
    });
  }

  /**
   * Set up modal button listeners
   */
  setupModalListeners() {
    // Close button
    const closeBtn = document.getElementById('content-block-editor-close');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.closeModal());
    }

    // Save Draft button
    const saveDraftBtn = document.getElementById('content-block-save-draft');
    if (saveDraftBtn) {
      saveDraftBtn.addEventListener('click', () => this.saveModalDraft());
    }

    // Publish button
    const publishBtn = document.getElementById('content-block-publish');
    if (publishBtn) {
      publishBtn.addEventListener('click', () => this.publishModal());
    }

    // Close on overlay click
    const modal = document.getElementById('content-block-editor-modal');
    if (modal) {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          this.closeModal();
        }
      });
    }

    // Close on Escape key
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        const modal = document.getElementById('content-block-editor-modal');
        if (modal && modal.style.display === 'block') {
          this.closeModal();
        }
      }
    });
  }

  /**
   * Close the modal editor
   */
  closeModal() {
    const modal = document.getElementById('content-block-editor-modal');
    if (modal) {
      modal.style.display = 'none';
    }

    // Refresh the content blocks list to reflect any changes
    if (this.currentPageLink) {
      this.loadContentBlocks(this.currentPageLink);
    }

    // Also refresh the preview iframe
    if (this.editorBridge && this.currentPageLink) {
      this.editorBridge.showPagePreview(this.currentPageLink);
    }
  }

  /**
   * Get content from the modal TinyMCE editor
   */
  getModalContent() {
    const editor = tinymce.get('content-block-html-editor');
    if (editor) {
      return editor.getContent();
    }
    return document.getElementById('content-block-html-editor').value;
  }

  /**
   * Save draft from modal editor
   */
  saveModalDraft() {
    const modal = document.getElementById('content-block-editor-modal');
    if (!modal) return;

    const contentId = modal.dataset.contentId;
    const content = this.getModalContent();

    const params = new FormData();
    params.append('contentId', contentId);
    params.append('content', content);
    params.append('isDraft', 'true');
    params.append('token', window.getFormToken());

    fetch('/json/content/save-draft', {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.showModalNotification('Draft saved', 'success');
        } else {
          this.showModalNotification(data.error || 'Failed to save draft', 'error');
        }
      })
      .catch(error => {
        this.showModalNotification('Error saving draft: ' + error.message, 'error');
      });
  }

  /**
   * Publish from modal editor
   */
  publishModal() {
    if (!confirm('Are you sure you want to publish this content?')) {
      return;
    }

    const modal = document.getElementById('content-block-editor-modal');
    if (!modal) return;

    const contentId = modal.dataset.contentId;
    const content = this.getModalContent();

    const params = new FormData();
    params.append('contentId', contentId);
    params.append('content', content);
    params.append('token', window.getFormToken());

    fetch('/json/content/publish', {
      method: 'POST',
      headers: { 'Accept': 'application/json' },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.showModalNotification('Content published', 'success');
        } else {
          this.showModalNotification(data.error || 'Failed to publish', 'error');
        }
      })
      .catch(error => {
        this.showModalNotification('Error publishing: ' + error.message, 'error');
      });
  }

  /**
   * Show a notification in the modal
   */
  showModalNotification(message, type) {
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
      z-index: 100001;
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
   * Format a date string for display
   */
  formatDate(dateStr) {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return dateStr;
      return date.toLocaleDateString(undefined, {
        year: 'numeric', month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch (e) {
      return dateStr;
    }
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
