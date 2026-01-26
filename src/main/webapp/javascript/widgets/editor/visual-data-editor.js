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
    this.token = options.token || '';
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
    this.setupPropertiesPanelResize();
    this.loadInitialData();
    this.setupTabs();
    this.setupModals();
  }

  setupEventListeners() {
    // Toolbar buttons
    document.getElementById('new-collection-btn')?.addEventListener('click', () => this.showNewCollectionModal());
    document.getElementById('new-dataset-btn')?.addEventListener('click', () => this.showNewDatasetModal());
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
    const isDarkMode = localStorage.getItem('editor-theme') === 'dark';
    if (isDarkMode) {
      document.body.dataset.theme = 'dark';
      const icon = darkModeToggle.querySelector('i');
      if (icon) {
        icon.classList.remove('fa-moon');
        icon.classList.add('fa-sun');
      }
    }

    darkModeToggle.addEventListener('click', () => {
      const isDark = document.body.dataset.theme === 'dark';
      if (isDark) {
        delete document.body.dataset.theme;
        localStorage.setItem('editor-theme', 'light');
      } else {
        document.body.dataset.theme = 'dark';
        localStorage.setItem('editor-theme', 'dark');
      }
      
      const icon = darkModeToggle.querySelector('i');
      if (icon) {
        if (isDark) {
          icon.classList.remove('fa-sun');
          icon.classList.add('fa-moon');
        } else {
          icon.classList.remove('fa-moon');
          icon.classList.add('fa-sun');
        }
      }
    });
  }

  setupPropertiesPanelResize() {
    const resizeHandle = document.getElementById('properties-panel-resize-handle');
    const propertiesPanel = document.getElementById('data-properties-panel');
    if (!resizeHandle || !propertiesPanel) return;

    let isResizing = false;
    let startX = 0;
    let startWidth = 0;

    const onMouseDown = (e) => {
      isResizing = true;
      startX = e.clientX;
      startWidth = propertiesPanel.offsetWidth;
      resizeHandle.classList.add('resizing');
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
      e.preventDefault();
    };

    const onMouseMove = (e) => {
      if (!isResizing) return;
      
      const deltaX = startX - e.clientX;
      const newWidth = startWidth + deltaX;
      const minWidth = 250;
      const maxWidth = 600;
      
      if (newWidth >= minWidth && newWidth <= maxWidth) {
        propertiesPanel.style.width = `${newWidth}px`;
      }
      e.preventDefault();
    };

    const onMouseUp = () => {
      if (isResizing) {
        isResizing = false;
        resizeHandle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
      }
    };

    resizeHandle.addEventListener('mousedown', onMouseDown);
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
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
    this.loadCollectionDetails(uniqueId);
    this.hideEmptyCanvas();
    
    // Update active state in list
    document.querySelectorAll('#collections-list .data-list-item').forEach(item => {
      item.classList.remove('active');
      if (item.dataset.uniqueId === uniqueId) {
        item.classList.add('active');
      }
    });
  }

  selectDataset(id) {
    this.loadDatasetDetails(id);
    this.hideEmptyCanvas();
    
    // Update active state in list
    document.querySelectorAll('#datasets-list .data-list-item').forEach(item => {
      item.classList.remove('active');
      if (item.dataset.id === id) {
        item.classList.add('active');
      }
    });
  }

  async loadCollectionDetails(uniqueId) {
    this.showLoading(true);
    try {
      const response = await fetch(`/json/collectionDetails?uniqueId=${encodeURIComponent(uniqueId)}`);
      const data = await response.json();
      
      if (data.error) {
        alert('Error loading collection: ' + data.error);
        return;
      }

      this.selectedItem = data;
      this.renderCollectionDetails(data);
      document.getElementById('save-btn').disabled = false;
      
    } catch (error) {
      console.error('Error loading collection details:', error);
      alert('Failed to load collection details');
    } finally {
      this.showLoading(false);
    }
  }

  async loadDatasetDetails(id) {
    this.showLoading(true);
    try {
      const response = await fetch(`/json/datasetDetails?id=${id}`);
      const data = await response.json();
      
      if (data.error) {
        alert('Error loading dataset: ' + data.error);
        return;
      }

      this.selectedItem = data;
      this.renderDatasetDetails(data);
      document.getElementById('save-btn').disabled = false;
      
    } catch (error) {
      console.error('Error loading dataset details:', error);
      alert('Failed to load dataset details');
    } finally {
      this.showLoading(false);
    }
  }

  renderCollectionDetails(collection) {
    const canvas = document.getElementById('data-editor-canvas');
    canvas.innerHTML = `
      <div class="data-card">
        <div class="data-card-header">
          <div class="data-card-icon">
            <i class="fa fa-folder"></i>
          </div>
          <div class="data-card-title">
            <h3>${this.escapeHtml(collection.name)}</h3>
            <p>${this.escapeHtml(collection.description || 'No description')}</p>
          </div>
        </div>
        <div class="data-card-body">
          <div class="data-card-section">
            <div class="data-card-section-title">Details</div>
            <div class="data-card-field">
              <span class="data-card-field-label">Unique ID</span>
              <span class="data-card-field-value">${this.escapeHtml(collection.uniqueId)}</span>
            </div>
            <div class="data-card-field">
              <span class="data-card-field-label">Items</span>
              <span class="data-card-field-value">${collection.itemCount}</span>
            </div>
            <div class="data-card-field">
              <span class="data-card-field-label">Categories</span>
              <span class="data-card-field-value">${collection.categoryCount}</span>
            </div>
            <div class="data-card-field">
              <span class="data-card-field-label">Guest Access</span>
              <span class="data-card-field-value">${collection.allowsGuests ? 'Enabled' : 'Disabled'}</span>
            </div>
            <div class="data-card-field">
              <span class="data-card-field-label">Search</span>
              <span class="data-card-field-value">${collection.showSearch ? 'Enabled' : 'Disabled'}</span>
            </div>
          </div>
        </div>
      </div>
    `;

    // Update info tab
    this.updateInfoTab(collection, 'collection');
    this.updateConfigTab(collection, 'collection');
    
    // Load items for the Records tab
    if (collection.uniqueId) {
      this.loadCollectionItems(collection.uniqueId);
    }
  }

  renderDatasetDetails(dataset) {
    const canvas = document.getElementById('data-editor-canvas');
    const syncStatus = this.getSyncStatusBadge(dataset);
    
    canvas.innerHTML = `
      <div class="data-card">
        <div class="data-card-header">
          <div class="data-card-icon">
            <i class="fa fa-database"></i>
          </div>
          <div class="data-card-title">
            <h3>${this.escapeHtml(dataset.name)}</h3>
            <p>${this.escapeHtml(dataset.filename || 'No file')}</p>
          </div>
        </div>
        <div class="data-card-body">
          <div class="data-card-section">
            <div class="data-card-section-title">Dataset Information</div>
            <div class="data-card-field">
              <span class="data-card-field-label">Records</span>
              <span class="data-card-field-value">${dataset.recordCount || 0}</span>
            </div>
            <div class="data-card-field">
              <span class="data-card-field-label">Rows</span>
              <span class="data-card-field-value">${dataset.rowCount || 0}</span>
            </div>
            <div class="data-card-field">
              <span class="data-card-field-label">Columns</span>
              <span class="data-card-field-value">${dataset.columnCount || 0}</span>
            </div>
            <div class="data-card-field">
              <span class="data-card-field-label">File Type</span>
              <span class="data-card-field-value">${this.escapeHtml(dataset.fileType || 'N/A')}</span>
            </div>
          </div>
          <div class="data-card-section">
            <div class="data-card-section-title">Synchronization</div>
            <div class="data-card-field">
              <span class="data-card-field-label">Status</span>
              <span class="data-card-field-value">${syncStatus}</span>
            </div>
            ${dataset.collectionUniqueId ? `
            <div class="data-card-field">
              <span class="data-card-field-label">Collection</span>
              <span class="data-card-field-value">${this.escapeHtml(dataset.collectionUniqueId)}</span>
            </div>
            ` : ''}
            ${dataset.syncRecordCount > 0 ? `
            <div class="data-card-field">
              <span class="data-card-field-label">Last Sync</span>
              <span class="data-card-field-value">${dataset.syncRecordCount} records (${dataset.syncAddCount} added, ${dataset.syncUpdateCount} updated)</span>
            </div>
            ` : ''}
          </div>
        </div>
      </div>
    `;

    // Update info tab
    this.updateInfoTab(dataset, 'dataset');
    this.updateConfigTab(dataset, 'dataset');
    
    // Load records for the Records tab (if we have an ID)
    if (dataset.id) {
      this.loadDatasetRecords(dataset.id);
    }
  }

  getSyncStatusBadge(dataset) {
    if (!dataset.syncEnabled) {
      return '<span class="status-badge inactive">Not Enabled</span>';
    }
    if (dataset.syncStatus === 0) {
      return '<span class="status-badge active">Ready</span>';
    }
    return '<span class="status-badge scheduled">Scheduled</span>';
  }

  updateInfoTab(item, type) {
    const infoContent = document.getElementById('info-tab-content');
    if (type === 'collection') {
      infoContent.innerHTML = `
        <div style="padding: 15px;">
          <h5 style="margin-top: 0;">Collection Information</h5>
          <div class="property-group">
            <label class="property-label">Name</label>
            <div>${this.escapeHtml(item.name)}</div>
          </div>
          <div class="property-group">
            <label class="property-label">Unique ID</label>
            <div><code>${this.escapeHtml(item.uniqueId)}</code></div>
          </div>
          <div class="property-group">
            <label class="property-label">Description</label>
            <div>${this.escapeHtml(item.description || 'No description')}</div>
          </div>
          <div class="property-group">
            <label class="property-label">Statistics</label>
            <div>${item.itemCount} items, ${item.categoryCount} categories</div>
          </div>
        </div>
      `;
    } else if (type === 'dataset') {
      infoContent.innerHTML = `
        <div style="padding: 15px;">
          <h5 style="margin-top: 0;">Dataset Information</h5>
          <div class="property-group">
            <label class="property-label">Name</label>
            <div>${this.escapeHtml(item.name)}</div>
          </div>
          <div class="property-group">
            <label class="property-label">File</label>
            <div>${this.escapeHtml(item.filename || 'N/A')}</div>
          </div>
          <div class="property-group">
            <label class="property-label">Records</label>
            <div>${item.recordCount || 0} records in ${item.rowCount || 0} rows</div>
          </div>
          ${item.sourceUrl ? `
          <div class="property-group">
            <label class="property-label">Source URL</label>
            <div style="word-break: break-all;">${this.escapeHtml(item.sourceUrl)}</div>
          </div>
          ` : ''}
        </div>
      `;
    }
  }

  updateConfigTab(item, type) {
    const configContent = document.getElementById('config-tab-content');
    if (type === 'collection') {
      configContent.innerHTML = `
        <div style="padding: 15px;">
          <h5 style="margin-top: 0;">Collection Settings</h5>
          <div class="property-group">
            <label class="property-label" for="edit-collection-name">Name</label>
            <input type="text" id="edit-collection-name" class="property-input" value="${this.escapeHtml(item.name)}" />
          </div>
          <div class="property-group">
            <label class="property-label" for="edit-collection-description">Description</label>
            <textarea id="edit-collection-description" class="property-input" rows="3">${this.escapeHtml(item.description || '')}</textarea>
          </div>
          <div class="property-group">
            <label class="property-label">
              <input type="checkbox" id="edit-collection-allows-guests" ${item.allowsGuests ? 'checked' : ''} />
              Allow guest access
            </label>
          </div>
          <div class="property-group">
            <label class="property-label">
              <input type="checkbox" id="edit-collection-show-search" ${item.showSearch ? 'checked' : ''} />
              Show search
            </label>
          </div>
          <div class="property-group">
            <label class="property-label">
              <input type="checkbox" id="edit-collection-show-listings" ${item.showListingsLink ? 'checked' : ''} />
              Show listings link
            </label>
          </div>
          <div class="property-group" style="margin-top: 20px;">
            <button id="save-collection-config-btn" class="button tiny primary radius expanded">
              <i class="fa fa-save"></i> Save Changes
            </button>
          </div>
        </div>
      `;
      
      // Add save listener
      document.getElementById('save-collection-config-btn')?.addEventListener('click', () => {
        this.saveCollectionConfig();
      });
      
    } else if (type === 'dataset') {
      configContent.innerHTML = `
        <div style="padding: 15px;">
          <h5 style="margin-top: 0;">Dataset Settings</h5>
          <div class="property-group">
            <label class="property-label" for="edit-dataset-name">Name</label>
            <input type="text" id="edit-dataset-name" class="property-input" value="${this.escapeHtml(item.name)}" />
          </div>
          <div class="property-group">
            <label class="property-label" for="edit-dataset-source-url">Source URL</label>
            <input type="text" id="edit-dataset-source-url" class="property-input" value="${this.escapeHtml(item.sourceUrl || '')}" />
          </div>
          <div class="property-group">
            <label class="property-label">
              <input type="checkbox" id="edit-dataset-sync-enabled" ${item.syncEnabled ? 'checked' : ''} />
              Enable synchronization
            </label>
          </div>
          <div class="property-group">
            <label class="property-label">
              <input type="checkbox" id="edit-dataset-schedule-enabled" ${item.scheduleEnabled ? 'checked' : ''} />
              Enable scheduling
            </label>
          </div>
          ${item.collectionUniqueId ? `
          <div class="property-group">
            <label class="property-label">Collection</label>
            <div><code>${this.escapeHtml(item.collectionUniqueId)}</code></div>
          </div>
          ` : ''}
          <div class="property-group" style="margin-top: 20px;">
            <button id="save-dataset-config-btn" class="button tiny primary radius expanded">
              <i class="fa fa-save"></i> Save Changes
            </button>
          </div>
        </div>
      `;
      
      // Add save listener
      document.getElementById('save-dataset-config-btn')?.addEventListener('click', () => {
        this.saveDatasetConfig();
      });
    }
  }

  async saveCollectionConfig() {
    if (!this.selectedItem || !this.selectedItem.id) {
      alert('No collection selected');
      return;
    }

    this.showLoading(true);
    try {
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('id', this.selectedItem.id);
      formData.append('name', document.getElementById('edit-collection-name').value);
      formData.append('description', document.getElementById('edit-collection-description').value);
      formData.append('allowsGuests', document.getElementById('edit-collection-allows-guests').checked);
      formData.append('showSearch', document.getElementById('edit-collection-show-search')?.checked || false);
      formData.append('showListingsLink', document.getElementById('edit-collection-show-listings')?.checked || false);

      const response = await fetch('/json/saveCollection', {
        method: 'POST',
        body: formData
      });

      const result = await response.json();
      if (result.success) {
        this.showNotification('Collection saved successfully');
        // Reload the collection details
        this.loadCollectionDetails(this.selectedItem.uniqueId);
        // Reload the list
        this.loadCollections();
      } else {
        alert('Error: ' + (result.message || 'Failed to save collection'));
      }
    } catch (error) {
      console.error('Error saving collection:', error);
      alert('Failed to save collection');
    } finally {
      this.showLoading(false);
    }
  }

  async saveDatasetConfig() {
    if (!this.selectedItem || !this.selectedItem.id) {
      alert('No dataset selected');
      return;
    }

    this.showLoading(true);
    try {
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('id', this.selectedItem.id);
      formData.append('name', document.getElementById('edit-dataset-name').value);
      formData.append('sourceUrl', document.getElementById('edit-dataset-source-url').value);
      formData.append('syncEnabled', document.getElementById('edit-dataset-sync-enabled').checked);
      formData.append('scheduleEnabled', document.getElementById('edit-dataset-schedule-enabled').checked);
      
      const response = await fetch('/json/saveDataset', {
        method: 'POST',
        body: formData
      });

      const result = await response.json();
      if (result.success) {
        this.showNotification('Dataset saved successfully');
        // Reload the dataset details
        this.loadDatasetDetails(this.selectedItem.id);
        // Reload the list
        this.loadDatasets();
      } else {
        alert('Error: ' + (result.message || 'Failed to save dataset'));
      }
    } catch (error) {
      console.error('Error saving dataset:', error);
      alert('Failed to save dataset');
    } finally {
      this.showLoading(false);
    }
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

    try {
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('name', document.getElementById('collection-name').value);
      formData.append('uniqueId', document.getElementById('collection-unique-id').value);
      formData.append('description', document.getElementById('collection-description').value);
      formData.append('allowsGuests', document.getElementById('collection-allows-guests').checked);

      const response = await fetch('/json/saveCollection', {
        method: 'POST',
        body: formData
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

  async loadCollectionItems(uniqueId) {
    try {
      const response = await fetch(`/json/collectionItems?uniqueId=${encodeURIComponent(uniqueId)}&limit=50&offset=0`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || 'Failed to load items');
      }

      const data = await response.json();
      this.updateRecordsTab(data.items || [], 'collection');
      return data;
    } catch (error) {
      console.error('Error loading collection items:', error);
      this.showNotification('Error loading items: ' + error.message);
      return { items: [] };
    }
  }

  updateRecordsTab(items, type) {
    const recordsTab = document.getElementById('records-tab');
    if (!recordsTab) return;

    if (items.length === 0) {
      const emptyMessage = type === 'dataset' ? 'No dataset records found' : 'No items found';
      recordsTab.innerHTML = `
        <div style="padding: 20px; text-align: center; color: #999;">
          <i class="fa fa-inbox" style="font-size: 48px; margin-bottom: 10px;"></i>
          <p>${emptyMessage}</p>
        </div>
      `;
      return;
    }

    let html = '<div class="items-list" style="padding: 15px;">';
    
    items.forEach(item => {
      const displayName = item.name || item.title || 'Unnamed';
      const uniqueId = item.uniqueId || item.id || '';
      html += `
        <div class="item-card" data-item-id="${item.id}" style="
          padding: 12px;
          margin-bottom: 10px;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          cursor: pointer;
          transition: background-color 0.2s;
        ">
          <div style="font-weight: 500; margin-bottom: 4px;">${this.escapeHtml(displayName)}</div>
          ${item.summary ? `<div style="font-size: 0.9em; color: #666;">${this.escapeHtml(item.summary)}</div>` : ''}
          <div style="font-size: 0.85em; color: #999; margin-top: 4px;">
            ID: ${item.id}${uniqueId ? ` | Unique ID: ${this.escapeHtml(uniqueId)}` : ''}
          </div>
        </div>
      `;
    });
    
    html += '</div>';
    recordsTab.innerHTML = html;

    // Add hover effects using CSS
    const style = document.createElement('style');
    style.textContent = `
      .item-card:hover {
        background-color: var(--hover-bg, #f0f0f0);
      }
    `;
    if (!document.getElementById('item-card-styles')) {
      style.id = 'item-card-styles';
      document.head.appendChild(style);
    }
  }

  async loadDatasetRecords(datasetId) {
    try {
      const response = await fetch(`/json/datasetRecords?datasetId=${datasetId}&limit=50&offset=0`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || 'Failed to load records');
      }

      const data = await response.json();
      this.updateRecordsTab(data.records || [], 'dataset');
      return data;
    } catch (error) {
      console.error('Error loading dataset records:', error);
      this.showNotification('Error loading records: ' + error.message);
      return { records: [] };
    }
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
