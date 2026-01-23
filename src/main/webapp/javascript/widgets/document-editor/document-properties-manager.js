/**
 * Handles right-panel metadata display for the document editor
 */

class DocumentPropertiesManager {
  constructor(editor) {
    this.editor = editor;
    this.contentEl = document.getElementById('document-properties-content');
    this.infoSection = null;
  }

  init() {
    if (this.contentEl) {
      this.infoSection = this.contentEl.querySelector('.property-section.info');
    }
  }

  async loadFile(fileId) {
    if (!fileId) {
      return;
    }
    try {
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentContent`, globalThis.location.origin);
      url.searchParams.set('fileId', fileId);
      this.editor.showLoading();
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const file = await response.json();
      this.render(file);
      this.editor.setUnsavedChanges(false);
    } catch (err) {
      console.error('Unable to load file', err);
    } finally {
      this.editor.hideLoading();
    }
  }

  render(file) {
    if (!this.contentEl) {
      return;
    }
    if (!file || file.error) {
      this.contentEl.innerHTML = '<div class="empty-state">Select a file to see details</div>';
      return;
    }

    this.contentEl.innerHTML = `
      <div class="property-tabs">
        <button class="property-tab active" data-tab="info">Properties</button>
        <button class="property-tab" data-tab="versions">Versions</button>
        <button class="property-tab" data-tab="downloads">Downloads</button>
      </div>
      <div class="property-section active" data-tab="info">
        <div class="property-group">
          <label>Title</label>
          <div class="property-value">${file.title || file.filename || 'Untitled'}</div>
        </div>
        <div class="property-group">
          <label>Filename</label>
          <div class="property-value">${file.filename || ''}</div>
        </div>
        <div class="property-group">
          <label>Version</label>
          <div class="property-value">${file.version || ''}</div>
        </div>
        <div class="property-group">
          <label>MIME Type</label>
          <div class="property-value">${file.mimeType || ''}</div>
        </div>
        <div class="property-group">
          <label>Size</label>
          <div class="property-value">${this.formatSize(file.fileLength)}</div>
        </div>
        <div class="property-group">
          <label>Downloads</label>
          <div class="property-value">${file.downloadCount || 0}</div>
        </div>
        <div class="property-group">
          <label>Summary</label>
          <div class="property-value">${file.summary || ''}</div>
        </div>
      </div>
      <div class="property-section" data-tab="versions">
        <div class="empty-state">Version history coming soon.</div>
      </div>
      <div class="property-section" data-tab="downloads">
        <div class="empty-state">Download analytics coming soon.</div>
      </div>
    `;

    this.wireTabs();
  }

  wireTabs() {
    const tabs = this.contentEl.querySelectorAll('.property-tab');
    const sections = this.contentEl.querySelectorAll('.property-section');
    tabs.forEach((tab) => {
      tab.addEventListener('click', () => {
        const tabName = tab.dataset.tab;
        tabs.forEach((t) => t.classList.toggle('active', t.dataset.tab === tabName));
        sections.forEach((section) => section.classList.toggle('active', section.dataset.tab === tabName));
      });
    });
  }

  formatSize(bytes) {
    if (!bytes || bytes < 0) {
      return '';
    }
    const units = ['B', 'KB', 'MB', 'GB'];
    const power = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    const size = bytes / Math.pow(1024, power);
    return `${size.toFixed(1)} ${units[power]}`;
  }
}
