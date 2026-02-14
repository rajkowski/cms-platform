/**
 * Preview Manager for Visual Content Editor
 * Handles content preview rendering with directive resolution and click-to-edit
 * 
 * @author matt rajkowski
 * @created 02/07/26 02:00 PM
 */

class PreviewManager {
  constructor(editorBridge) {
    this.editorBridge = editorBridge;
    this.previewFrame = null;
    this.currentPreviewContent = null;
    this.scrollPosition = 0;
  }

  /**
   * Initialize the preview manager
   */
  init() {
    this.setupPreviewFrame();
  }

  /**
   * Set up the preview iframe
   */
  setupPreviewFrame() {
    const iframe = document.getElementById('preview-iframe');
    if (!iframe) return;

    this.previewFrame = iframe;

    // Handle iframe load
    iframe.addEventListener('load', () => {
      this.enableContentBlockEditing();
    });
  }

  /**
   * Render preview of content with directive resolution
   */
  renderPreview(contentId) {
    const params = new URLSearchParams();
    params.append('contentId', contentId);

    fetch(`/json/content/preview?${params}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok' && data.data) {
          this.currentPreviewContent = data.data;
          this.updatePreview();
        } else {
          this.showPreviewError(data.error || 'Failed to load preview');
        }
      })
      .catch(error => {
        console.error('Error loading preview:', error);
        this.showPreviewError('Error loading preview: ' + error.message);
      });
  }

  /**
   * Update the preview with resolved content
   */
  updatePreview() {
    if (!this.previewFrame || !this.currentPreviewContent) return;

    // Store current scroll position
    if (this.previewFrame.contentDocument) {
      try {
        this.scrollPosition = this.previewFrame.contentDocument.documentElement.scrollTop;
      } catch (e) {
        // Cross-origin or not yet loaded
      }
    }

    // Resolve directives
    const resolvedContent = this.resolveContentDirectives(this.currentPreviewContent);

    // Render in iframe
    const doc = this.previewFrame.contentDocument || this.previewFrame.contentWindow.document;

    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="/javascript/foundation-sites-6.9.0/foundation.min.css">
        <style>
          body {
            font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            line-height: 1.5;
            color: #333;
            padding: 20px;
          }
          [data-editable] {
            border: 1px dashed #007bff;
            padding: 5px;
            cursor: pointer;
            background: rgba(0, 123, 255, 0.05);
            transition: all 0.2s ease;
          }
          [data-editable]:hover {
            background: rgba(0, 123, 255, 0.1);
            border-color: #0056b3;
          }
          .directive {
            background: #fff3cd;
            border: 1px solid #ffc107;
            padding: 10px;
            margin: 5px 0;
            border-radius: 3px;
            font-family: monospace;
            font-size: 12px;
          }
        </style>
      </head>
      <body>
        ${resolvedContent}
      </body>
      </html>
    `;

    doc.open();
    doc.write(html);
    doc.close();

    // Restore scroll position
    try {
      if (this.scrollPosition > 0) {
        this.previewFrame.contentDocument.documentElement.scrollTop = this.scrollPosition;
      }
    } catch (e) {
      // Cross-origin or not yet loaded
    }
  }

  /**
   * Resolve content directives ${uniqueId:...}
   */
  resolveContentDirectives(content) {
    if (!content) return '';

    // Regular expression to find directives
    const directiveRegex = /\$\{uniqueId:([a-zA-Z0-9\-_]+)\}/g;

    // Replace directives with markers for now (backend should resolve these)
    return content.replace(directiveRegex, (match, uniqueId) => {
      return `<div class="directive" data-directive-id="${this.escapeHtml(uniqueId)}">${this.escapeHtml(match)}</div>`;
    });
  }

  /**
   * Enable click-to-edit functionality for content blocks
   */
  enableContentBlockEditing() {
    if (!this.previewFrame) return;

    try {
      const doc = this.previewFrame.contentDocument;
      if (!doc) return;

      const directives = doc.querySelectorAll('[data-directive-id]');

      directives.forEach(directive => {
        directive.setAttribute('data-editable', 'true');
        directive.addEventListener('click', (e) => {
          e.preventDefault();
          e.stopPropagation();

          const uniqueId = directive.dataset.directiveId;
          this.editReferencedContent(uniqueId);
        });
      });
    } catch (e) {
      // Cross-origin iframe protection - cannot modify content
      console.warn('Cannot enable edit mode for cross-origin preview');
    }
  }

  /**
   * Edit referenced content by clicking on a directive
   */
  editReferencedContent(uniqueId) {
    // Load content by unique ID
    fetch(`/json/content/get?uniqueId=${uniqueId}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok' && data.data) {
          const content = data.data;
          this.editorBridge.loadContent(content.id, uniqueId);
        } else {
          alert('Content not found: ' + uniqueId);
        }
      })
      .catch(error => {
        console.error('Error loading content:', error);
        alert('Error loading content: ' + error.message);
      });
  }

  /**
   * Update preview based on editor content
   */
  updatePreviewFromEditor() {
    if (!this.editorBridge || !this.editorBridge.currentContentId) return;

    this.renderPreview(this.editorBridge.currentContentId);
  }

  /**
   * Show preview error message
   */
  showPreviewError(message) {
    const doc = this.previewFrame.contentDocument || this.previewFrame.contentWindow.document;

    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <style>
          body {
            font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            padding: 20px;
            background: #fff3cd;
          }
          .error {
            background: #f8d7da;
            border: 1px solid #f5c6cb;
            color: #721c24;
            padding: 15px;
            border-radius: 4px;
            margin: 20px;
          }
        </style>
      </head>
      <body>
        <div class="error">
          <strong>Preview Error</strong>
          <p>${this.escapeHtml(message)}</p>
        </div>
      </body>
      </html>
    `;

    doc.open();
    doc.write(html);
    doc.close();
  }

  /**
   * Refresh preview
   */
  refresh() {
    if (this.editorBridge && this.editorBridge.currentContentId) {
      this.updatePreviewFromEditor();
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

  /**
   * Clear preview
   */
  clearPreview() {
    if (!this.previewFrame) return;

    const doc = this.previewFrame.contentDocument || this.previewFrame.contentWindow.document;

    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <style>
          body {
            font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            color: #999;
            padding: 40px 20px;
            text-align: center;
          }
        </style>
      </head>
      <body>
        <p>Select content to view preview</p>
      </body>
      </html>
    `;

    doc.open();
    doc.write(html);
    doc.close();
  }
}
