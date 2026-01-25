/**
 * Copyright 2026 Matt Rajkowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class VisualDataEditor {
  constructor(options) {
    this.viewMode = options.viewMode || 'collections';
    this.uniqueId = options.uniqueId || null;
    this.userId = options.userId || -1;
    this.selectedItem = null;
    this.collections = [];
    this.datasets = [];
    this.isDirty = false;
  }

  init() {
    this.setupEventListeners();
    this.setupDarkMode();
    this.loadInitialData();
    this.setupTabs();
    this.setupModals();
  }

  setupEventListeners() {
    // Toolbar buttons
    document.getElementById('new-collection-btn')?.addEventListener('click', () => this.showNewCollectionModal());
    document.getElementById('new-dataset-btn')?.addEventListener('click', () => this.showNewDatasetModal());
    document.getElementById('import-dataset-btn')?.addEventListener('click', () => this.showImportDatasetModal());
    document.getElementById('reload-btn')?.addEventListener('click', () => this.reloadData());
    document.getElementById('save-btn')?.addEventListener('click', () => this.saveCurrentItem());
    
    // Empty canvas action buttons
    document.querySelectorAll('.new-collection-action').forEach(btn => {
      btn.addEventListener('click', () => this.showNewCollectionModal());
    });
    document.querySelectorAll('.import-dataset-action').forEach(btn => {
      btn.addEventListener('click', () => this.showImportDatasetModal());
    });

    // Search inputs
    document.getElementById('collections-search')?.addEventListener('input', (e) => this.filterList('collections', e.target.value));
    document.getElementById('datasets-search')?.addEventListener('input', (e) => this.filterList('datasets', e.target.value));

    // Confirm exit
    document.querySelectorAll('.confirm-exit').forEach(link => {
      link.addEventListener('click', (e) => {
        if (this.isDirty && !confirm('You have unsaved changes. Are you sure you want to leave?')) {
          e.preventDefault();
        }
      });
    });
  }

  setupDarkMode() {
    const darkModeToggle = document.getElementById('dark-mode-toggle');
    if (!darkModeToggle) return;

    // Load saved preference
    const isDarkMode = localStorage.getItem('dataEditorDarkMode') === 'true';
    if (isDarkMode) {
      document.body.classList.add('dark-mode');
      darkModeToggle.querySelector('i').classList.replace('fa-moon', 'fa-sun');
    }

    darkModeToggle.addEventListener('click', () => {
      document.body.classList.toggle('dark-mode');
      const isDark = document.body.classList.contains('dark-mode');
      localStorage.setItem('dataEditorDarkMode', isDark);
      
      const icon = darkModeToggle.querySelector('i');
      if (isDark) {
        icon.classList.replace('fa-moon', 'fa-sun');
      } else {
        icon.classList.replace('fa-sun', 'fa-moon');
      }
    });
  }

  setupTabs() {
    // Left panel tabs
    document.querySelectorAll('.library-tabs-container .tabs-nav a').forEach(tab => {
      tab.addEventListener('click', (e) => {
        e.preventDefault();
        this.switchLibraryTab(tab.getAttribute('href').substring(1));
      });
    });

    // Right panel tabs
    document.querySelectorAll('.right-panel-tabs-nav a').forEach(tab => {
      tab.addEventListener('click', (e) => {
        e.preventDefault();
        this.switchRightPanelTab(tab.getAttribute('data-tab'));
      });
    });
  }

  switchLibraryTab(tabId) {
    // Update tab navigation
    document.querySelectorAll('.library-tabs-container .tabs-nav a').forEach(t => t.classList.remove('active'));
    document.querySelector(`.library-tabs-container .tabs-nav a[href="#${tabId}"]`)?.classList.add('active');

    // Update tab content
    document.querySelectorAll('.library-tabs-container .tab-content').forEach(c => c.classList.remove('active'));
    document.getElementById(tabId)?.classList.add('active');

    // Load data if needed
    if (tabId === 'collections-tab' && this.collections.length === 0) {
      this.loadCollections();
    } else if (tabId === 'datasets-tab' && this.datasets.length === 0) {
      this.loadDatasets();
    }
  }

  switchRightPanelTab(tabName) {
    // Update tab navigation
    document.querySelectorAll('.right-panel-tabs-nav a').forEach(t => t.classList.remove('active'));
    document.querySelector(`.right-panel-tabs-nav a[data-tab="${tabName}"]`)?.classList.add('active');

    // Update tab content
    document.querySelectorAll('.right-panel-tab-content').forEach(c => c.classList.remove('active'));
    document.querySelector(`.right-panel-tab-content[data-tab="${tabName}"]`)?.classList.add('active');
  }

  setupModals() {
    // Close modal buttons
    document.querySelectorAll('.modal-close, .modal-cancel').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        this.closeAllModals();
      });
    });

    // Close on overlay click
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
      overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
          this.closeAllModals();
        }
      });
    });

    // Import source radio buttons
    document.querySelectorAll('input[name="import-source"]').forEach(radio => {
      radio.addEventListener('change', (e) => this.handleImportSourceChange(e.target.value));
    });

    // Forms
    document.getElementById('new-collection-form')?.addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleNewCollection();
    });

    document.getElementById('import-dataset-form')?.addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleImportDataset();
    });
  }

  loadInitialData() {
    if (this.viewMode === 'collections') {
      this.loadCollections();
    } else if (this.viewMode === 'datasets') {
      this.loadDatasets();
    }

    // If a specific item is selected, load it
    if (this.uniqueId) {
      this.loadItem(this.uniqueId);
    }
  }

  async loadCollections() {
    this.showLoading(true);
    try {
      const response = await fetch('/json/dataCollections');
      const data = await response.json();
      
      if (data.collections) {
        this.collections = data.collections;
        this.renderCollectionsList();
      }
    } catch (error) {
      console.error('Error loading collections:', error);
      this.showError('collections', 'Failed to load collections');
    } finally {
      this.showLoading(false);
    }
  }

  async loadDatasets() {
    this.showLoading(true);
    try {
      const response = await fetch('/json/dataDatasets');
      const data = await response.json();
      
      if (data.datasets) {
        this.datasets = data.datasets;
        this.renderDatasetsList();
      }
    } catch (error) {
      console.error('Error loading datasets:', error);
      this.showError('datasets', 'Failed to load datasets');
    } finally {
      this.showLoading(false);
    }
  }

  renderCollectionsList() {
    const list = document.getElementById('collections-list');
    const emptyDiv = document.getElementById('collections-empty');
    
    if (!this.collections || this.collections.length === 0) {
      list.innerHTML = '';
      emptyDiv.style.display = 'block';
      return;
    }

    emptyDiv.style.display = 'none';
    list.innerHTML = this.collections.map(collection => `
      <li class="data-list-item" data-id="${collection.id}" data-unique-id="${collection.uniqueId}">
        <div class="data-list-item-icon">
          <i class="fa fa-folder"></i>
        </div>
        <div class="data-list-item-content">
          <div class="data-list-item-title">${this.escapeHtml(collection.name)}</div>
          <div class="data-list-item-meta">${collection.itemCount || 0} items</div>
        </div>
        <div class="data-list-item-actions">
          <button class="data-list-item-action" data-action="edit" title="Edit">
            <i class="fa fa-edit"></i>
          </button>
          <button class="data-list-item-action" data-action="delete" title="Delete">
            <i class="fa fa-trash"></i>
          </button>
        </div>
      </li>
    `).join('');

    // Add click listeners
    list.querySelectorAll('.data-list-item').forEach(item => {
      item.addEventListener('click', () => this.selectCollection(item.dataset.uniqueId));
    });
  }

  renderDatasetsList() {
    const list = document.getElementById('datasets-list');
    const emptyDiv = document.getElementById('datasets-empty');
    
    if (!this.datasets || this.datasets.length === 0) {
      list.innerHTML = '';
      emptyDiv.style.display = 'block';
      return;
    }

    emptyDiv.style.display = 'none';
    list.innerHTML = this.datasets.map(dataset => `
      <li class="data-list-item" data-id="${dataset.id}">
        <div class="data-list-item-icon">
          <i class="fa fa-database"></i>
        </div>
        <div class="data-list-item-content">
          <div class="data-list-item-title">${this.escapeHtml(dataset.name)}</div>
          <div class="data-list-item-meta">
            ${dataset.recordCount ? `${dataset.recordCount} records` : 'No data'}
            ${dataset.syncEnabled ? ' • <span class="status-badge scheduled">Scheduled</span>' : ''}
          </div>
        </div>
        <div class="data-list-item-actions">
          <button class="data-list-item-action" data-action="edit" title="Edit">
            <i class="fa fa-edit"></i>
          </button>
          <button class="data-list-item-action" data-action="sync" title="Sync">
            <i class="fa fa-sync"></i>
          </button>
          <button class="data-list-item-action" data-action="delete" title="Delete">
            <i class="fa fa-trash"></i>
          </button>
        </div>
      </li>
    `).join('');

    // Add click listeners
    list.querySelectorAll('.data-list-item').forEach(item => {
      item.addEventListener('click', () => this.selectDataset(item.dataset.id));
    });
  }

  filterList(type, query) {
    const list = document.getElementById(`${type}-list`);
    const items = list.querySelectorAll('.data-list-item');
    const lowerQuery = query.toLowerCase();

    items.forEach(item => {
      const title = item.querySelector('.data-list-item-title').textContent.toLowerCase();
      if (title.includes(lowerQuery)) {
        item.style.display = '';
      } else {
        item.style.display = 'none';
      }
    });
  }

  selectCollection(uniqueId) {
    // TODO: Load and display collection details
    console.log('Selected collection:', uniqueId);
    this.hideEmptyCanvas();
  }

  selectDataset(id) {
    // TODO: Load and display dataset details
    console.log('Selected dataset:', id);
    this.hideEmptyCanvas();
  }

  hideEmptyCanvas() {
    const canvas = document.getElementById('data-editor-canvas');
    const emptyCanvas = canvas.querySelector('.empty-canvas');
    if (emptyCanvas) {
      emptyCanvas.style.display = 'none';
    }
  }

  showNewCollectionModal() {
    document.getElementById('new-collection-modal').style.display = 'flex';
  }

  showNewDatasetModal() {
    document.getElementById('import-dataset-modal').style.display = 'flex';
  }

  showImportDatasetModal() {
    document.getElementById('import-dataset-modal').style.display = 'flex';
  }

  closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(modal => {
      modal.style.display = 'none';
    });
  }

  handleImportSourceChange(source) {
    document.getElementById('file-upload-section').style.display = source === 'file' ? 'block' : 'none';
    document.getElementById('url-section').style.display = source === 'url' ? 'block' : 'none';
    document.getElementById('stock-data-section').style.display = source === 'stock' ? 'block' : 'none';
  }

  async handleNewCollection() {
    const name = document.getElementById('collection-name').value;
    const uniqueId = document.getElementById('collection-unique-id').value;
    const description = document.getElementById('collection-description').value;
    const allowsGuests = document.getElementById('collection-allows-guests').checked;

    try {
      const response = await fetch('/json/saveCollection', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, uniqueId, description, allowsGuests })
      });

      const result = await response.json();
      if (result.success) {
        this.closeAllModals();
        this.loadCollections();
        this.showNotification('Collection created successfully');
      } else {
        alert('Error creating collection: ' + (result.message || 'Unknown error'));
      }
    } catch (error) {
      console.error('Error creating collection:', error);
      alert('Failed to create collection');
    }
  }

  async handleImportDataset() {
    // TODO: Implement dataset import
    console.log('Importing dataset...');
    this.closeAllModals();
  }

  async saveCurrentItem() {
    // TODO: Implement save logic
    console.log('Saving current item...');
    this.isDirty = false;
    document.getElementById('save-btn').disabled = true;
  }

  async reloadData() {
    if (this.viewMode === 'collections') {
      await this.loadCollections();
    } else {
      await this.loadDatasets();
    }
  }

  showLoading(show) {
    const indicator = document.getElementById('loading-indicator');
    if (indicator) {
      indicator.style.display = show ? 'block' : 'none';
    }
  }

  showError(section, message) {
    const errorDiv = document.getElementById(`${section}-error`);
    if (errorDiv) {
      errorDiv.textContent = message;
      errorDiv.style.display = 'block';
    }
  }

  showNotification(message) {
    // TODO: Implement notification system
    console.log('Notification:', message);
  }

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}

// Export for use in JSP
if (typeof module !== 'undefined' && module.exports) {
  module.exports = VisualDataEditor;
}
