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
    const darkModeToggle = document.getElementById('dark-mode-toggle-menu');
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

  setupDatasetTabs() {
    document.querySelectorAll('.dataset-tab').forEach(tab => {
      tab.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const tabName = tab.dataset.tab;

        // Update tab navigation
        document.querySelectorAll('.dataset-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');

        // Update tab content
        document.querySelectorAll('.dataset-tab-content, .dataset-properties-content').forEach(c => c.classList.remove('active'));
        const targetContent = document.getElementById(`dataset-${tabName}-content`);
        if (targetContent) {
          targetContent.classList.add('active');
        }

        return false;
      });
    });
  }

  setupCollectionTabs() {
    document.querySelectorAll('.collection-tab').forEach(tab => {
      tab.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const tabName = tab.dataset.tab;

        // Update tab navigation
        document.querySelectorAll('.collection-tab').forEach(t => t.classList.remove('active'));
        tab.classList.add('active');

        // Update tab content
        document.querySelectorAll('.collection-tab-content').forEach(c => c.classList.remove('active'));
        const targetContent = document.getElementById(`collection-${tabName}-content`);
        if (targetContent) {
          targetContent.classList.add('active');
        }

        return false;
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
    // Load both collections and datasets to populate the counts in tabs
    this.loadCollections();
    this.loadDatasets();

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
    const countElement = document.getElementById('collections-count');

    if (!this.collections || this.collections.length === 0) {
      list.innerHTML = '';
      emptyDiv.style.display = 'block';
      if (countElement) countElement.textContent = '(0)';
      return;
    }

    emptyDiv.style.display = 'none';
    if (countElement) countElement.textContent = `(${this.collections.length})`;

    list.innerHTML = this.collections.map(collection => `
      <li class="data-list-item" data-id="${collection.id}" data-unique-id="${collection.uniqueId}">
        <div class="data-list-item-icon">
          <i class="fa fa-folder"></i>
        </div>
        <div class="data-list-item-content">
          <div class="data-list-item-title">${this.escapeHtml(collection.name)}</div>
          <div class="data-list-item-meta">${collection.itemCount || 0} items</div>
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
    const countElement = document.getElementById('datasets-count');

    if (!this.datasets || this.datasets.length === 0) {
      list.innerHTML = '';
      emptyDiv.style.display = 'block';
      if (countElement) countElement.textContent = '(0)';
      return;
    }

    emptyDiv.style.display = 'none';
    if (countElement) countElement.textContent = `(${this.datasets.length})`;

    list.innerHTML = this.datasets.map(dataset => `
      <li class="data-list-item" data-id="${dataset.id}">
        <div class="data-list-item-icon">
          <i class="fa fa-database"></i>
        </div>
        <div class="data-list-item-content">
          <div class="data-list-item-title">${this.escapeHtml(dataset.name)}</div>
          <div class="data-list-item-meta">
            ${dataset.recordCount && dataset.recordCount > 0 ? `${dataset.recordCount} records` : '0 records'}
            ${dataset.syncEnabled ? ' • <span class="status-badge scheduled">Scheduled</span>' : ''}
          </div>
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
      <div class="collection-editor-container">
        <div class="collection-header">
          <div class="collection-header-icon">
            <i class="fa fa-folder"></i>
          </div>
          <div class="collection-header-content">
            <h3>${this.escapeHtml(collection.name)}</h3>
            <p>${this.escapeHtml(collection.description || 'No description')} • ${collection.itemCount || 0} items</p>
          </div>
          <div class="collection-header-actions">
            <button class="button tiny primary radius" id="save-collection-btn">
              <i class="fa fa-save"></i> Save Changes
            </button>
            <button class="button tiny secondary radius" id="refresh-collection-btn">
              <i class="fa fa-redo"></i> Refresh
            </button>
          </div>
        </div>
        
        <div class="collection-tabs-container">
          <ul class="collection-tabs-nav">
            <li><a href="#collection-overview" class="collection-tab active" data-tab="overview"><i class="fa fa-info-circle"></i> Overview</a></li>
            <li><a href="#collection-permissions" class="collection-tab" data-tab="permissions"><i class="fa fa-lock"></i> Permissions</a></li>
            <li><a href="#collection-theme" class="collection-tab" data-tab="theme"><i class="fa fa-palette"></i> Theme</a></li>
            <li><a href="#collection-tabs" class="collection-tab" data-tab="tabs"><i class="fa fa-th-list"></i> Tabs</a></li>
            <li><a href="#collection-categories" class="collection-tab" data-tab="categories"><i class="fa fa-tags"></i> Categories</a></li>
            <li><a href="#collection-columns" class="collection-tab" data-tab="columns"><i class="fa fa-columns"></i> Table Columns</a></li>
            <li><a href="#collection-fields" class="collection-tab" data-tab="fields"><i class="fa fa-th"></i> Custom Fields</a></li>
            <li><a href="#collection-related" class="collection-tab" data-tab="related"><i class="fa fa-link"></i> Related</a></li>
          </ul>
          
          <div class="collection-tab-content active" id="collection-overview-content">
            ${this.renderCollectionOverview(collection)}
          </div>
          
          <div class="collection-tab-content" id="collection-permissions-content">
            ${this.renderCollectionPermissions(collection)}
          </div>
          
          <div class="collection-tab-content" id="collection-theme-content">
            ${this.renderCollectionTheme(collection)}
          </div>
          
          <div class="collection-tab-content" id="collection-tabs-content">
            ${this.renderCollectionTabs(collection)}
          </div>
          
          <div class="collection-tab-content" id="collection-categories-content">
            ${this.renderCollectionCategories(collection)}
          </div>
          
          <div class="collection-tab-content" id="collection-columns-content">
            ${this.renderCollectionTableColumns(collection)}
          </div>
          
          <div class="collection-tab-content" id="collection-fields-content">
            ${this.renderCollectionCustomFields(collection)}
          </div>
          
          <div class="collection-tab-content" id="collection-related-content">
            ${this.renderCollectionRelated(collection)}
          </div>
        </div>
      </div>
    `;

    // Setup collection tab switching
    this.setupCollectionTabs();

    // Setup collection action buttons
    document.getElementById('save-collection-btn')?.addEventListener('click', () => this.saveCollectionConfig());
    document.getElementById('refresh-collection-btn')?.addEventListener('click', () => this.loadCollectionDetails(collection.uniqueId));

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
      <div class="dataset-editor-container">
        <div class="dataset-header">
          <div class="dataset-header-icon">
            <i class="fa fa-database"></i>
          </div>
          <div class="dataset-header-content">
            <h3>${this.escapeHtml(dataset.name)}</h3>
            <p>${this.escapeHtml(dataset.filename || 'No file')} • ${dataset.recordCount && dataset.recordCount > 0 ? dataset.recordCount : 0} records • ${syncStatus}</p>
          </div>
          <div class="dataset-header-actions">
            <button class="button tiny primary radius" id="sync-now-btn">
              <i class="fa fa-sync"></i> Sync Now
            </button>
            <button class="button tiny alert radius" id="delete-dataset-btn">
              <i class="fa fa-trash"></i> Delete
            </button>
            <button class="button tiny secondary radius" id="refresh-dataset-btn">
              <i class="fa fa-redo"></i> Refresh
            </button>
          </div>
        </div>
        
        <div class="dataset-tabs-container">
          <ul class="dataset-tabs-nav">
            <li><a href="#dataset-overview" class="dataset-tab active" data-tab="overview"><i class="fa fa-info-circle"></i> Overview</a></li>
            <li><a href="#dataset-scheduling" class="dataset-tab" data-tab="scheduling"><i class="fa fa-clock"></i> Scheduling</a></li>
            <li><a href="#dataset-mapping" class="dataset-tab" data-tab="mapping"><i class="fa fa-random"></i> Field Mapping</a></li>
            <li><a href="#dataset-transform" class="dataset-tab" data-tab="transform"><i class="fa fa-magic"></i> Transformation</a></li>
            <li><a href="#dataset-properties" class="dataset-tab" data-tab="properties"><i class="fa fa-cog"></i> Properties</a></li>
          </ul>
          
          <div class="dataset-tab-content active" id="dataset-overview-content">
            ${this.renderDatasetOverview(dataset)}
          </div>
          
          <div class="dataset-tab-content" id="dataset-scheduling-content">
            ${this.renderDatasetScheduling(dataset)}
          </div>
          
          <div class="dataset-tab-content" id="dataset-mapping-content">
            ${this.renderDatasetMapping(dataset)}
          </div>
          
          <div class="dataset-tab-content" id="dataset-transform-content">
            ${this.renderDatasetTransformation(dataset)}
          </div>
          
          <div class="dataset-tab-content" id="dataset-properties-content">
            ${this.renderDatasetProperties(dataset)}
          </div>
        </div>
      </div>
    `;

    // Setup dataset tab switching
    this.setupDatasetTabs();

    // Setup dataset action buttons
    document.getElementById('sync-now-btn')?.addEventListener('click', () => this.syncDatasetNow(dataset.id));
    document.getElementById('delete-dataset-btn')?.addEventListener('click', () => this.deleteDataset(dataset.id));
    document.getElementById('refresh-dataset-btn')?.addEventListener('click', () => this.loadDatasetDetails(dataset.id));

    // Update info tab
    this.updateInfoTab(dataset, 'dataset');
    this.updateConfigTab(dataset, 'dataset');

    // Load records for the Records tab (if we have an ID)
    if (dataset.id) {
      this.loadDatasetRecords(dataset.id);
    }
  }

  renderCollectionOverview(collection) {
    return `
      <div class="overview-section">
        <div class="collection-stats-grid">
          <div class="stat-card">
            <div class="stat-icon"><i class="fa fa-cube"></i></div>
            <div class="stat-content">
              <div class="stat-label">Total Items</div>
              <div class="stat-value">${collection.itemCount || 0}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon"><i class="fa fa-tags"></i></div>
            <div class="stat-content">
              <div class="stat-label">Categories</div>
              <div class="stat-value">${collection.categoryCount || 0}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon"><i class="fa fa-th"></i></div>
            <div class="stat-content">
              <div class="stat-label">Custom Fields</div>
              <div class="stat-value">${collection.customFieldCount || 0}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon"><i class="fa fa-link"></i></div>
            <div class="stat-content">
              <div class="stat-label">Related Collections</div>
              <div class="stat-value">${collection.relatedCount || 0}</div>
            </div>
          </div>
        </div>

        <div class="info-section">
          <h4>Collection Information</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">Unique ID</span>
              <span class="info-value">${this.escapeHtml(collection.uniqueId)}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Created</span>
              <span class="info-value">${collection.created || 'Unknown'}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Modified</span>
              <span class="info-value">${collection.modified || 'Never'}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Guest Access</span>
              <span class="info-value">
                <span class="status-badge ${collection.allowsGuests ? 'enabled' : 'disabled'}">
                  ${collection.allowsGuests ? 'Enabled' : 'Disabled'}
                </span>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">Search</span>
              <span class="info-value">
                <span class="status-badge ${collection.showSearch ? 'enabled' : 'disabled'}">
                  ${collection.showSearch ? 'Enabled' : 'Disabled'}
                </span>
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">Listings Link</span>
              <span class="info-value">
                <span class="status-badge ${collection.showListingsLink ? 'enabled' : 'disabled'}">
                  ${collection.showListingsLink ? 'Enabled' : 'Disabled'}
                </span>
              </span>
            </div>
          </div>
        </div>

        <div class="info-section">
          <h4>Description</h4>
          <p>${this.escapeHtml(collection.description || 'No description provided')}</p>
        </div>
      </div>
    `;
  }

  renderCollectionPermissions(collection) {
    return `
      <div class="permissions-section">
        <div class="permissions-header">
          <h4>Access Control</h4>
          <button class="button tiny primary radius" id="add-permission-btn">
            <i class="fa fa-plus"></i> Add Permission
          </button>
        </div>

        <div class="permission-group">
          <h5>Guest Access</h5>
          <label class="checkbox-label">
            <input type="checkbox" id="perm-allows-guests" ${collection.allowsGuests ? 'checked' : ''} />
            Allow guests (non-authenticated users) to view this collection
          </label>
        </div>

        <div class="permission-group">
          <h5>Role-Based Permissions</h5>
          <table class="permissions-table">
            <thead>
              <tr>
                <th>Role/Group</th>
                <th>View</th>
                <th>Add</th>
                <th>Edit</th>
                <th>Delete</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>All Users</td>
                <td><input type="checkbox" checked /></td>
                <td><input type="checkbox" /></td>
                <td><input type="checkbox" /></td>
                <td><input type="checkbox" /></td>
                <td><button class="button tiny alert" title="Remove"><i class="fa fa-trash"></i></button></td>
              </tr>
              <tr>
                <td>Admins</td>
                <td><input type="checkbox" checked /></td>
                <td><input type="checkbox" checked /></td>
                <td><input type="checkbox" checked /></td>
                <td><input type="checkbox" checked /></td>
                <td><button class="button tiny alert" title="Remove"><i class="fa fa-trash"></i></button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="permission-actions">
          <button class="button primary radius">Save Permissions</button>
          <button class="button secondary radius">Reset</button>
        </div>
      </div>
    `;
  }

  renderCollectionTheme(collection) {
    return `
      <div class="theme-section">
        <h4>Visual Customization</h4>
        
        <div class="theme-group">
          <h5>Header Colors</h5>
          <div class="color-picker-row">
            <div class="color-picker-item">
              <label>Header Background</label>
              <input type="color" id="theme-header-bg" value="${collection.headerBgColor || '#0f4f79'}" class="color-input" />
              <input type="text" value="${collection.headerBgColor || '#0f4f79'}" class="color-text" />
            </div>
            <div class="color-picker-item">
              <label>Header Text</label>
              <input type="color" id="theme-header-text" value="${collection.headerTextColor || '#ffffff'}" class="color-input" />
              <input type="text" value="${collection.headerTextColor || '#ffffff'}" class="color-text" />
            </div>
          </div>
        </div>

        <div class="theme-group">
          <h5>Menu Colors</h5>
          <div class="color-picker-row">
            <div class="color-picker-item">
              <label>Menu Background</label>
              <input type="color" id="theme-menu-bg" value="${collection.menuBgColor || '#f4f4f4'}" class="color-input" />
              <input type="text" value="${collection.menuBgColor || '#f4f4f4'}" class="color-text" />
            </div>
            <div class="color-picker-item">
              <label>Menu Text</label>
              <input type="color" id="theme-menu-text" value="${collection.menuTextColor || '#333333'}" class="color-input" />
              <input type="text" value="${collection.menuTextColor || '#333333'}" class="color-text" />
            </div>
          </div>
        </div>

        <div class="theme-preview">
          <h5>Preview</h5>
          <div class="preview-box">
            <div class="preview-header" style="background-color: ${collection.headerBgColor || '#0f4f79'}; color: ${collection.headerTextColor || '#ffffff'};">
              <h3>Collection Header</h3>
            </div>
            <div class="preview-menu" style="background-color: ${collection.menuBgColor || '#f4f4f4'}; color: ${collection.menuTextColor || '#333333'};">
              Menu Item 1 • Menu Item 2 • Menu Item 3
            </div>
          </div>
        </div>

        <div class="theme-actions">
          <button class="button primary radius">Save Theme</button>
          <button class="button secondary radius">Reset to Defaults</button>
        </div>
      </div>
    `;
  }

  renderCollectionTabs(collection) {
    return `
      <div class="tabs-section">
        <div class="tabs-header">
          <h4>Navigation Tabs</h4>
          <button class="button tiny primary radius" id="add-tab-btn">
            <i class="fa fa-plus"></i> Add Tab
          </button>
        </div>

        <div class="tabs-list">
          <div class="tab-item">
            <div class="tab-item-handle"><i class="fa fa-grip-vertical"></i></div>
            <div class="tab-item-content">
              <input type="text" value="All Items" class="tab-name-input" />
              <label class="checkbox-label">
                <input type="checkbox" checked /> Visible
              </label>
            </div>
            <div class="tab-item-actions">
              <button class="button tiny" title="Edit"><i class="fa fa-edit"></i></button>
              <button class="button tiny alert" title="Delete"><i class="fa fa-trash"></i></button>
            </div>
          </div>
          <div class="tab-item">
            <div class="tab-item-handle"><i class="fa fa-grip-vertical"></i></div>
            <div class="tab-item-content">
              <input type="text" value="Featured" class="tab-name-input" />
              <label class="checkbox-label">
                <input type="checkbox" checked /> Visible
              </label>
            </div>
            <div class="tab-item-actions">
              <button class="button tiny" title="Edit"><i class="fa fa-edit"></i></button>
              <button class="button tiny alert" title="Delete"><i class="fa fa-trash"></i></button>
            </div>
          </div>
        </div>

        <div class="tabs-settings">
          <h5>Tab Settings</h5>
          <label class="checkbox-label">
            <input type="checkbox" id="tabs-show-counts" checked />
            Show item counts on tabs
          </label>
          <label class="checkbox-label">
            <input type="checkbox" id="tabs-allow-multiple" />
            Allow multiple tabs to be selected
          </label>
        </div>

        <div class="tabs-actions">
          <button class="button primary radius">Save Tabs</button>
          <button class="button secondary radius">Reset</button>
        </div>
      </div>
    `;
  }

  renderCollectionCategories(collection) {
    return `
      <div class="categories-section">
        <div class="categories-header">
          <h4>Manage Categories</h4>
          <button class="button tiny primary radius" id="add-category-btn">
            <i class="fa fa-plus"></i> Add Category
          </button>
        </div>

        <div class="categories-list">
          <div class="category-item">
            <div class="category-item-handle"><i class="fa fa-grip-vertical"></i></div>
            <div class="category-item-content">
              <input type="text" value="Technology" class="category-name-input" />
              <span class="category-count">12 items</span>
            </div>
            <div class="category-item-actions">
              <button class="button tiny" title="Edit"><i class="fa fa-edit"></i></button>
              <button class="button tiny alert" title="Delete"><i class="fa fa-trash"></i></button>
            </div>
          </div>
          <div class="category-item">
            <div class="category-item-handle"><i class="fa fa-grip-vertical"></i></div>
            <div class="category-item-content">
              <input type="text" value="Business" class="category-name-input" />
              <span class="category-count">8 items</span>
            </div>
            <div class="category-item-actions">
              <button class="button tiny" title="Edit"><i class="fa fa-edit"></i></button>
              <button class="button tiny alert" title="Delete"><i class="fa fa-trash"></i></button>
            </div>
          </div>
        </div>

        <div class="categories-settings">
          <h5>Category Settings</h5>
          <label class="checkbox-label">
            <input type="checkbox" id="cat-hierarchical" />
            Enable hierarchical categories
          </label>
          <label class="checkbox-label">
            <input type="checkbox" id="cat-multiple" checked />
            Allow items to have multiple categories
          </label>
        </div>

        <div class="categories-actions">
          <button class="button primary radius">Save Categories</button>
          <button class="button secondary radius">Reset</button>
        </div>
      </div>
    `;
  }

  renderCollectionTableColumns(collection) {
    return `
      <div class="columns-section">
        <div class="columns-header">
          <h4>Table Column Designer</h4>
          <button class="button tiny primary radius" id="add-column-btn">
            <i class="fa fa-plus"></i> Add Column
          </button>
        </div>

        <table class="columns-table">
          <thead>
            <tr>
              <th width="30"></th>
              <th>Column Name</th>
              <th>Field</th>
              <th>Width</th>
              <th>Sortable</th>
              <th>Visible</th>
              <th width="80"></th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><i class="fa fa-grip-vertical"></i></td>
              <td><input type="text" value="Name" class="column-input" /></td>
              <td>
                <select class="column-input">
                  <option value="name" selected>Name</option>
                  <option value="title">Title</option>
                  <option value="summary">Summary</option>
                </select>
              </td>
              <td><input type="text" value="40%" class="column-input" /></td>
              <td><input type="checkbox" checked /></td>
              <td><input type="checkbox" checked /></td>
              <td>
                <button class="button tiny alert" title="Remove"><i class="fa fa-trash"></i></button>
              </td>
            </tr>
            <tr>
              <td><i class="fa fa-grip-vertical"></i></td>
              <td><input type="text" value="Category" class="column-input" /></td>
              <td>
                <select class="column-input">
                  <option value="category" selected>Category</option>
                  <option value="tags">Tags</option>
                </select>
              </td>
              <td><input type="text" value="20%" class="column-input" /></td>
              <td><input type="checkbox" checked /></td>
              <td><input type="checkbox" checked /></td>
              <td>
                <button class="button tiny alert" title="Remove"><i class="fa fa-trash"></i></button>
              </td>
            </tr>
            <tr>
              <td><i class="fa fa-grip-vertical"></i></td>
              <td><input type="text" value="Date" class="column-input" /></td>
              <td>
                <select class="column-input">
                  <option value="created" selected>Created Date</option>
                  <option value="modified">Modified Date</option>
                </select>
              </td>
              <td><input type="text" value="20%" class="column-input" /></td>
              <td><input type="checkbox" checked /></td>
              <td><input type="checkbox" checked /></td>
              <td>
                <button class="button tiny alert" title="Remove"><i class="fa fa-trash"></i></button>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="columns-settings">
          <h5>Table Settings</h5>
          <label class="checkbox-label">
            <input type="checkbox" id="table-striped" checked />
            Striped rows
          </label>
          <label class="checkbox-label">
            <input type="checkbox" id="table-hover" checked />
            Hover effect
          </label>
          <label class="checkbox-label">
            <input type="checkbox" id="table-responsive" checked />
            Responsive layout
          </label>
        </div>

        <div class="columns-actions">
          <button class="button primary radius">Save Columns</button>
          <button class="button secondary radius">Reset to Defaults</button>
        </div>
      </div>
    `;
  }

  renderCollectionCustomFields(collection) {
    return `
      <div class="fields-section">
        <div class="fields-header">
          <h4>Custom Fields</h4>
          <button class="button tiny primary radius" id="add-field-btn">
            <i class="fa fa-plus"></i> Add Field
          </button>
        </div>

        <div class="fields-list">
          <div class="field-item">
            <div class="field-item-header">
              <div class="field-item-handle"><i class="fa fa-grip-vertical"></i></div>
              <div class="field-item-title">
                <strong>Price</strong>
                <span class="field-type-badge">Number</span>
              </div>
              <div class="field-item-actions">
                <button class="button tiny" title="Edit"><i class="fa fa-edit"></i></button>
                <button class="button tiny alert" title="Delete"><i class="fa fa-trash"></i></button>
              </div>
            </div>
            <div class="field-item-details">
              <div class="field-detail"><strong>Name:</strong> price</div>
              <div class="field-detail"><strong>Label:</strong> Price</div>
              <div class="field-detail"><strong>Required:</strong> No</div>
              <div class="field-detail"><strong>Searchable:</strong> Yes</div>
            </div>
          </div>

          <div class="field-item">
            <div class="field-item-header">
              <div class="field-item-handle"><i class="fa fa-grip-vertical"></i></div>
              <div class="field-item-title">
                <strong>Location</strong>
                <span class="field-type-badge">Text</span>
              </div>
              <div class="field-item-actions">
                <button class="button tiny" title="Edit"><i class="fa fa-edit"></i></button>
                <button class="button tiny alert" title="Delete"><i class="fa fa-trash"></i></button>
              </div>
            </div>
            <div class="field-item-details">
              <div class="field-detail"><strong>Name:</strong> location</div>
              <div class="field-detail"><strong>Label:</strong> Location</div>
              <div class="field-detail"><strong>Required:</strong> Yes</div>
              <div class="field-detail"><strong>Searchable:</strong> Yes</div>
            </div>
          </div>
        </div>

        <div class="field-types">
          <h5>Available Field Types</h5>
          <div class="field-type-grid">
            <div class="field-type-card">
              <i class="fa fa-font"></i>
              <span>Text</span>
            </div>
            <div class="field-type-card">
              <i class="fa fa-paragraph"></i>
              <span>Textarea</span>
            </div>
            <div class="field-type-card">
              <i class="fa fa-hashtag"></i>
              <span>Number</span>
            </div>
            <div class="field-type-card">
              <i class="fa fa-calendar"></i>
              <span>Date</span>
            </div>
            <div class="field-type-card">
              <i class="fa fa-check-square"></i>
              <span>Checkbox</span>
            </div>
            <div class="field-type-card">
              <i class="fa fa-list"></i>
              <span>Select</span>
            </div>
            <div class="field-type-card">
              <i class="fa fa-image"></i>
              <span>Image</span>
            </div>
            <div class="field-type-card">
              <i class="fa fa-file"></i>
              <span>File</span>
            </div>
          </div>
        </div>

        <div class="fields-actions">
          <button class="button primary radius">Save Fields</button>
          <button class="button secondary radius">Reset</button>
        </div>
      </div>
    `;
  }

  renderCollectionRelated(collection) {
    return `
      <div class="related-section">
        <div class="related-header">
          <h4>Related Collections</h4>
          <button class="button tiny primary radius" id="add-related-btn">
            <i class="fa fa-plus"></i> Add Relationship
          </button>
        </div>

        <div class="related-list">
          <div class="related-item">
            <div class="related-item-icon"><i class="fa fa-folder"></i></div>
            <div class="related-item-content">
              <div class="related-item-title">Products</div>
              <div class="related-item-meta">Linked as "Parent Collection" • 25 items</div>
            </div>
            <div class="related-item-actions">
              <button class="button tiny" title="Configure"><i class="fa fa-cog"></i></button>
              <button class="button tiny alert" title="Remove"><i class="fa fa-unlink"></i></button>
            </div>
          </div>

          <div class="related-item">
            <div class="related-item-icon"><i class="fa fa-folder"></i></div>
            <div class="related-item-content">
              <div class="related-item-title">Reviews</div>
              <div class="related-item-meta">Linked as "Child Collection" • 100 items</div>
            </div>
            <div class="related-item-actions">
              <button class="button tiny" title="Configure"><i class="fa fa-cog"></i></button>
              <button class="button tiny alert" title="Remove"><i class="fa fa-unlink"></i></button>
            </div>
          </div>
        </div>

        <div class="related-info">
          <h5>Relationship Types</h5>
          <ul>
            <li><strong>Parent-Child:</strong> Items in this collection can have items from another collection</li>
            <li><strong>Related:</strong> Items can reference items from another collection</li>
            <li><strong>Many-to-Many:</strong> Items can be linked to multiple items in another collection</li>
          </ul>
        </div>

        <div class="related-actions">
          <button class="button primary radius">Save Relationships</button>
          <button class="button secondary radius">Reset</button>
        </div>
      </div>
    `;
  }

  renderDatasetOverview(dataset) {
    return `
      <div class="dataset-panel">
        <div class="dataset-stats-grid">
          <div class="stat-card">
            <div class="stat-icon"><i class="fa fa-table"></i></div>
            <div class="stat-content">
              <div class="stat-value">${dataset.recordCount || 0}</div>
              <div class="stat-label">Records</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon"><i class="fa fa-columns"></i></div>
            <div class="stat-content">
              <div class="stat-value">${dataset.columnCount || 0}</div>
              <div class="stat-label">Columns</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon"><i class="fa fa-file"></i></div>
            <div class="stat-content">
              <div class="stat-value">${this.escapeHtml(dataset.fileType || 'N/A')}</div>
              <div class="stat-label">File Type</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon"><i class="fa fa-sync"></i></div>
            <div class="stat-content">
              <div class="stat-value">${dataset.syncRecordCount || 0}</div>
              <div class="stat-label">Last Sync</div>
            </div>
          </div>
        </div>
        
        <div class="dataset-info-section">
          <h4>Dataset Information</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">Source URL:</span>
              <span class="info-value">${this.escapeHtml(dataset.sourceUrl || 'Not specified')}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Collection:</span>
              <span class="info-value">${this.escapeHtml(dataset.collectionUniqueId || 'None')}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Last Modified:</span>
              <span class="info-value">${dataset.modified ? new Date(dataset.modified).toLocaleString() : 'Never'}</span>
            </div>
            <div class="info-item">
              <span class="info-label">File Size:</span>
              <span class="info-value">${dataset.fileLength ? this.formatBytes(dataset.fileLength) : 'Unknown'}</span>
            </div>
          </div>
        </div>
        
        ${dataset.syncRecordCount > 0 ? `
        <div class="dataset-sync-summary">
          <h4>Last Synchronization</h4>
          <div class="sync-summary-grid">
            <div class="sync-stat">
              <span class="sync-value success">${dataset.syncAddCount || 0}</span>
              <span class="sync-label">Added</span>
            </div>
            <div class="sync-stat">
              <span class="sync-value warning">${dataset.syncUpdateCount || 0}</span>
              <span class="sync-label">Updated</span>
            </div>
            <div class="sync-stat">
              <span class="sync-value info">${dataset.syncRecordCount || 0}</span>
              <span class="sync-label">Total</span>
            </div>
          </div>
        </div>
        ` : ''}
      </div>
    `;
  }

  renderDatasetScheduling(dataset) {
    return `
      <div class="dataset-panel">
        <div class="panel-section">
          <h4>Automatic Synchronization</h4>
          <p class="help-text">Configure when this dataset should automatically sync with its source.</p>
          
          <div class="property-group">
            <label class="property-label">
              <input type="checkbox" id="schedule-enabled" ${dataset.scheduleEnabled ? 'checked' : ''} />
              Enable automatic synchronization
            </label>
          </div>
          
          <div id="schedule-options" style="${dataset.scheduleEnabled ? '' : 'display:none;'}">
            <div class="property-group">
              <label class="property-label">Frequency</label>
              <select id="schedule-frequency" class="property-input">
                <option value="hourly" ${dataset.scheduleFrequency === 'hourly' ? 'selected' : ''}>Hourly</option>
                <option value="daily" ${dataset.scheduleFrequency === 'daily' ? 'selected' : ''}>Daily</option>
                <option value="weekly" ${dataset.scheduleFrequency === 'weekly' ? 'selected' : ''}>Weekly</option>
                <option value="monthly" ${dataset.scheduleFrequency === 'monthly' ? 'selected' : ''}>Monthly</option>
                <option value="custom" ${dataset.scheduleFrequency === 'custom' ? 'selected' : ''}>Custom (Cron)</option>
              </select>
            </div>
            
            <div class="property-group" id="daily-time-picker" style="display: ${dataset.scheduleFrequency === 'daily' ? 'block' : 'none'};">
              <label class="property-label">Time of Day</label>
              <input type="time" id="schedule-time" class="property-input" value="${dataset.scheduleTime || '00:00'}" />
            </div>
            
            <div class="property-group" id="weekly-day-picker" style="display: ${dataset.scheduleFrequency === 'weekly' ? 'block' : 'none'};">
              <label class="property-label">Day of Week</label>
              <select id="schedule-day" class="property-input">
                <option value="0" ${dataset.scheduleDay === 0 ? 'selected' : ''}>Sunday</option>
                <option value="1" ${dataset.scheduleDay === 1 ? 'selected' : ''}>Monday</option>
                <option value="2" ${dataset.scheduleDay === 2 ? 'selected' : ''}>Tuesday</option>
                <option value="3" ${dataset.scheduleDay === 3 ? 'selected' : ''}>Wednesday</option>
                <option value="4" ${dataset.scheduleDay === 4 ? 'selected' : ''}>Thursday</option>
                <option value="5" ${dataset.scheduleDay === 5 ? 'selected' : ''}>Friday</option>
                <option value="6" ${dataset.scheduleDay === 6 ? 'selected' : ''}>Saturday</option>
              </select>
            </div>
            
            <div class="property-group" id="cron-expression" style="display: ${dataset.scheduleFrequency === 'custom' ? 'block' : 'none'};">
              <label class="property-label">Cron Expression</label>
              <input type="text" id="schedule-cron" class="property-input" placeholder="0 0 * * *" value="${dataset.scheduleCron || ''}" />
              <small class="help-text">Standard cron format (minute hour day month weekday)</small>
            </div>
            
            <div class="property-group">
              <label class="property-label">Next Scheduled Run</label>
              <div class="property-value">${dataset.nextScheduledRun ? new Date(dataset.nextScheduledRun).toLocaleString() : 'Not scheduled'}</div>
            </div>
          </div>
          
          <div class="button-group" style="margin-top: 20px;">
            <button class="button primary radius" id="save-schedule-btn">
              <i class="fa fa-save"></i> Save Schedule
            </button>
            <button class="button secondary radius" id="test-sync-btn">
              <i class="fa fa-play"></i> Test Sync
            </button>
          </div>
        </div>
      </div>
    `;
  }

  renderDatasetMapping(dataset) {
    return `
      <div class="dataset-panel">
        <div class="panel-section">
          <h4>Field Mapping Configuration</h4>
          <p class="help-text">Map dataset columns to collection fields for proper data synchronization.</p>
          
          <div class="property-group">
            <label class="property-label">Target Collection</label>
            <select id="target-collection" class="property-input">
              <option value="">Select a collection...</option>
              ${this.collections.map(c => `
                <option value="${c.uniqueId}" ${dataset.collectionUniqueId === c.uniqueId ? 'selected' : ''}>
                  ${this.escapeHtml(c.name)}
                </option>
              `).join('')}
            </select>
          </div>
          
          <div id="mapping-table-container" style="${dataset.collectionUniqueId ? '' : 'display:none;'}">
            <h5>Column Mappings</h5>
            <div class="mapping-table">
              <div class="mapping-header">
                <div>Dataset Column</div>
                <div>Maps To</div>
                <div>Collection Field</div>
                <div>Transform</div>
                <div>Actions</div>
              </div>
              <div id="mapping-rows">
                ${this.renderMappingRows(dataset)}
              </div>
            </div>
            <button class="button tiny secondary radius" id="add-mapping-btn" style="margin-top: 10px;">
              <i class="fa fa-plus"></i> Add Mapping
            </button>
          </div>
          
          <div class="button-group" style="margin-top: 20px;">
            <button class="button primary radius" id="save-mappings-btn">
              <i class="fa fa-save"></i> Save Mappings
            </button>
            <button class="button secondary radius" id="auto-map-btn">
              <i class="fa fa-magic"></i> Auto-Map Fields
            </button>
          </div>
        </div>
      </div>
    `;
  }

  renderDatasetTransformation(dataset) {
    return `
      <div class="dataset-panel">
        <div class="panel-section">
          <h4>Data Transformation Rules</h4>
          <p class="help-text">Apply transformations to dataset values during synchronization.</p>
          
          <div id="transformation-rules">
            ${this.renderTransformationRules(dataset)}
          </div>
          
          <button class="button tiny secondary radius" id="add-transformation-btn" style="margin-top: 10px;">
            <i class="fa fa-plus"></i> Add Transformation
          </button>
          
          <div class="panel-section" style="margin-top: 30px;">
            <h5>Available Transformations</h5>
            <div class="transformation-library">
              <div class="transform-card">
                <strong>Trim Whitespace</strong>
                <p>Remove leading/trailing spaces</p>
              </div>
              <div class="transform-card">
                <strong>Uppercase/Lowercase</strong>
                <p>Convert text case</p>
              </div>
              <div class="transform-card">
                <strong>Date Format</strong>
                <p>Convert date formats</p>
              </div>
              <div class="transform-card">
                <strong>Number Format</strong>
                <p>Format numeric values</p>
              </div>
              <div class="transform-card">
                <strong>Replace Text</strong>
                <p>Find and replace patterns</p>
              </div>
              <div class="transform-card">
                <strong>Split/Join</strong>
                <p>Split or join text values</p>
              </div>
            </div>
          </div>
          
          <div class="button-group" style="margin-top: 20px;">
            <button class="button primary radius" id="save-transformations-btn">
              <i class="fa fa-save"></i> Save Transformations
            </button>
            <button class="button secondary radius" id="test-transform-btn">
              <i class="fa fa-flask"></i> Test Transformations
            </button>
          </div>
        </div>
      </div>
    `;
  }

  renderDatasetProperties(dataset) {
    return `
      <div class="dataset-panel">
        <div class="panel-section">
          <h4>Dataset Configuration</h4>
          
          <div class="property-group">
            <label class="property-label" for="dataset-name-edit">Name</label>
            <input type="text" id="dataset-name-edit" class="property-input" value="${this.escapeHtml(dataset.name)}" />
          </div>
          
          <div class="property-group">
            <label class="property-label" for="dataset-source-url-edit">Source URL</label>
            <input type="text" id="dataset-source-url-edit" class="property-input" value="${this.escapeHtml(dataset.sourceUrl || '')}" />
          </div>
          
          <div class="property-group">
            <h5>Request Configuration</h5>
            <p class="help-text" style="font-size: 12px; margin-bottom: 10px;">Configure HTTP request headers and authentication for data source access.</p>
            
            <div id="request-headers-container">
              ${this.renderRequestHeaders(dataset)}
            </div>
            
            <button class="button tiny secondary radius" id="add-request-header-btn" style="margin-top: 5px;">
              <i class="fa fa-plus"></i> Add Header
            </button>
          </div>
          
          <div class="property-group">
            <label class="property-label" for="dataset-file-type">File Type</label>
            <select id="dataset-file-type" class="property-input">
              <option value="csv" ${dataset.fileType === 'csv' ? 'selected' : ''}>CSV</option>
              <option value="json" ${dataset.fileType === 'json' ? 'selected' : ''}>JSON</option>
              <option value="geojson" ${dataset.fileType === 'geojson' ? 'selected' : ''}>GeoJSON</option>
              <option value="xml" ${dataset.fileType === 'xml' ? 'selected' : ''}>XML</option>
              <option value="rss" ${dataset.fileType === 'rss' ? 'selected' : ''}>RSS</option>
            </select>
          </div>
          
          <div class="property-group">
            <label class="property-label">
              <input type="checkbox" id="dataset-sync-enabled" ${dataset.syncEnabled ? 'checked' : ''} />
              Enable synchronization
            </label>
          </div>
          
          <div class="property-group">
            <label class="property-label">
              <input type="checkbox" id="dataset-has-header-row" ${dataset.hasHeaderRow ? 'checked' : ''} />
              First row contains headers
            </label>
          </div>
          
          <div class="property-group">
            <label class="property-label" for="dataset-encoding">Character Encoding</label>
            <select id="dataset-encoding" class="property-input">
              <option value="UTF-8" ${dataset.encoding === 'UTF-8' ? 'selected' : ''}>UTF-8</option>
              <option value="ISO-8859-1" ${dataset.encoding === 'ISO-8859-1' ? 'selected' : ''}>ISO-8859-1</option>
              <option value="Windows-1252" ${dataset.encoding === 'Windows-1252' ? 'selected' : ''}>Windows-1252</option>
            </select>
          </div>
          
          <div class="property-group">
            <label class="property-label" for="dataset-delimiter">CSV Delimiter</label>
            <input type="text" id="dataset-delimiter" class="property-input" value="${this.escapeHtml(dataset.delimiter || ',')}" maxlength="1" />
          </div>
          
          <div class="button-group" style="margin-top: 20px;">
            <button class="button primary radius" id="save-dataset-properties-btn">
              <i class="fa fa-save"></i> Save Properties
            </button>
            <button class="button secondary radius" id="reload-dataset-btn">
              <i class="fa fa-redo"></i> Reload from Source
            </button>
          </div>
        </div>
      </div>
    `;
  }

  renderRequestHeaders(dataset) {
    let headers = {};
    
    // Parse the requestConfig JSON if it exists
    if (dataset.requestConfig) {
      try {
        const config = JSON.parse(dataset.requestConfig);
        if (config && config.headers) {
          headers = config.headers;
        }
      } catch (e) {
        console.warn('Failed to parse request config:', e);
      }
    }
    
    // Render header rows
    const headerEntries = Object.entries(headers);
    if (headerEntries.length === 0) {
      return '<div class="empty-headers" style="padding: 10px; text-align: center; color: #999; font-size: 12px;">No headers configured</div>';
    }
    
    return headerEntries.map(([key, value]) => `
      <div class="header-row" style="display: flex; gap: 10px; margin-bottom: 8px; align-items: center;">
        <input type="text" class="header-key property-input" placeholder="Header Name" value="${this.escapeHtml(key)}" style="flex: 1;" />
        <input type="text" class="header-value property-input" placeholder="Header Value" value="${this.escapeHtml(value)}" style="flex: 2;" />
        <button class="button tiny alert remove-header-btn" title="Remove">
          <i class="fa fa-trash"></i>
        </button>
      </div>
    `).join('');
  }

  renderEditRequestHeaders(dataset) {
    let headers = {};
    
    // Parse the requestConfig JSON if it exists
    if (dataset.requestConfig) {
      try {
        const config = JSON.parse(dataset.requestConfig);
        if (config && config.headers) {
          headers = config.headers;
        }
      } catch (e) {
        console.warn('Failed to parse request config:', e);
      }
    }
    
    // Render header rows
    const headerEntries = Object.entries(headers);
    if (headerEntries.length === 0) {
      return '<div class="empty-headers" style="padding: 10px; text-align: center; color: #999; font-size: 12px;">No headers configured</div>';
    }
    
    return headerEntries.map(([key, value]) => `
      <div class="header-row" style="display: flex; gap: 10px; margin-bottom: 8px; align-items: center;">
        <input type="text" class="header-key property-input" placeholder="Header Name" value="${this.escapeHtml(key)}" style="flex: 1;" />
        <input type="text" class="header-value property-input" placeholder="Header Value" value="${this.escapeHtml(value)}" style="flex: 2;" />
        <button class="button tiny alert remove-header-btn" title="Remove">
          <i class="fa fa-trash"></i>
        </button>
      </div>
    `).join('');
  }

  addRequestHeader(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    
    // Remove empty message if present
    const emptyMsg = container.querySelector('.empty-headers');
    if (emptyMsg) {
      emptyMsg.remove();
    }
    
    // Add new header row
    const headerRow = document.createElement('div');
    headerRow.className = 'header-row';
    headerRow.style.cssText = 'display: flex; gap: 10px; margin-bottom: 8px; align-items: center;';
    headerRow.innerHTML = `
      <input type="text" class="header-key property-input" placeholder="Header Name" style="flex: 1;" />
      <input type="text" class="header-value property-input" placeholder="Header Value" style="flex: 2;" />
      <button class="button tiny alert remove-header-btn" title="Remove">
        <i class="fa fa-trash"></i>
      </button>
    `;
    container.appendChild(headerRow);
    
    // Setup remove listener for new row
    headerRow.querySelector('.remove-header-btn').addEventListener('click', () => {
      headerRow.remove();
      // Show empty message if no headers left
      if (container.querySelectorAll('.header-row').length === 0) {
        container.innerHTML = '<div class="empty-headers" style="padding: 10px; text-align: center; color: #999; font-size: 12px;">No headers configured</div>';
      }
    });
  }

  setupRemoveHeaderListeners(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    
    container.querySelectorAll('.remove-header-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        btn.closest('.header-row').remove();
        // Show empty message if no headers left
        if (container.querySelectorAll('.header-row').length === 0) {
          container.innerHTML = '<div class="empty-headers" style="padding: 10px; text-align: center; color: #999; font-size: 12px;">No headers configured</div>';
        }
      });
    });
  }

  collectRequestHeaders(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return null;
    
    const headers = {};
    container.querySelectorAll('.header-row').forEach(row => {
      const key = row.querySelector('.header-key').value.trim();
      const value = row.querySelector('.header-value').value.trim();
      if (key && value) {
        headers[key] = value;
      }
    });
    
    return Object.keys(headers).length > 0 ? { headers } : null;
  }

  renderMappingRows(dataset) {
    const mappings = dataset.fieldMappings || [];
    if (mappings.length === 0) {
      return '<div class="empty-mappings">No field mappings configured. Click "Auto-Map Fields" to get started.</div>';
    }
    return mappings.map((mapping, index) => `
      <div class="mapping-row" data-index="${index}">
        <select class="mapping-source property-input">
          <option value="">Select column...</option>
          ${(dataset.columns || []).map(col => `
            <option value="${col}" ${mapping.source === col ? 'selected' : ''}>${this.escapeHtml(col)}</option>
          `).join('')}
        </select>
        <div class="mapping-arrow"><i class="fa fa-arrow-right"></i></div>
        <select class="mapping-target property-input">
          <option value="name">Name</option>
          <option value="description">Description</option>
          <option value="location">Location</option>
          <option value="latitude">Latitude</option>
          <option value="longitude">Longitude</option>
          <option value="custom">Custom Field</option>
        </select>
        <select class="mapping-transform property-input">
          <option value="">None</option>
          <option value="trim">Trim</option>
          <option value="uppercase">Uppercase</option>
          <option value="lowercase">Lowercase</option>
        </select>
        <button class="button tiny alert" onclick="this.parentElement.remove()">
          <i class="fa fa-trash"></i>
        </button>
      </div>
    `).join('');
  }

  renderTransformationRules(dataset) {
    const rules = dataset.transformationRules || [];
    if (rules.length === 0) {
      return '<div class="empty-transformations">No transformation rules configured.</div>';
    }
    return rules.map((rule, index) => `
      <div class="transformation-rule" data-index="${index}">
        <div class="rule-header">
          <strong>Rule ${index + 1}:</strong> ${this.escapeHtml(rule.name || 'Unnamed')}
          <button class="button tiny alert" onclick="this.parentElement.parentElement.remove()">
            <i class="fa fa-trash"></i>
          </button>
        </div>
        <div class="rule-body">
          <p>${this.escapeHtml(rule.description || 'No description')}</p>
        </div>
      </div>
    `).join('');
  }

  formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  async syncDatasetNow(datasetId) {
    if (!confirm('Sync this dataset now?')) return;

    this.showLoading(true);
    try {
      const response = await fetch('/json/datasetSync', {
        method: 'POST',
        body: (() => {
          const params = new FormData();
          params.append('datasetId', datasetId);
          params.append('token', this.token);
          return params;
        })()
      });

      if (!response.ok) {
        throw new Error('Sync failed');
      }

      await response.json();
      this.showNotification('Dataset synced successfully');
      this.loadDatasetDetails(datasetId);
    } catch (error) {
      console.error('Error syncing dataset:', error);
      this.showNotification('Error syncing dataset: ' + error.message);
    } finally {
      this.showLoading(false);
    }
  }

  async deleteDataset(datasetId) {
    if (!confirm('Are you sure you want to delete this dataset? This action cannot be undone.')) return;

    this.showLoading(true);
    try {
      const response = await fetch('/json/datasetDelete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({
          datasetId: datasetId,
          token: this.token
        })
      });

      if (!response.ok) {
        throw new Error('Delete failed');
      }

      await response.json();
      this.showNotification('Dataset deleted successfully');

      // Clear the canvas and reload the datasets list
      this.showEmptyCanvas();
      this.loadDatasets();
    } catch (error) {
      console.error('Error deleting dataset:', error);
      this.showNotification('Error deleting dataset: ' + error.message);
    } finally {
      this.showLoading(false);
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
            <label class="property-label">Request Headers</label>
            <p class="help-text" style="font-size: 12px; margin-bottom: 10px;">Add HTTP headers for authentication or custom request configuration.</p>
            <div id="edit-request-headers-container">
              ${this.renderEditRequestHeaders(item)}
            </div>
            <button class="button tiny secondary radius" id="add-edit-request-header-btn" style="margin-top: 5px;">
              <i class="fa fa-plus"></i> Add Header
            </button>
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
      
      // Add header button listener
      document.getElementById('add-edit-request-header-btn')?.addEventListener('click', () => {
        this.addRequestHeader('edit-request-headers-container');
      });
      
      // Add remove header listeners
      this.setupRemoveHeaderListeners('edit-request-headers-container');
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
      
      // Collect request headers and create requestConfig JSON
      const requestConfig = this.collectRequestHeaders('edit-request-headers-container');
      if (requestConfig) {
        formData.append('requestConfig', JSON.stringify(requestConfig));
      } else {
        formData.append('requestConfig', '');
      }

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

  showEmptyCanvas() {
    const canvas = document.getElementById('data-editor-canvas');
    const emptyCanvas = canvas.querySelector('.empty-canvas');
    if (emptyCanvas) {
      emptyCanvas.style.display = 'block';
    } else {
      // If empty canvas doesn't exist, recreate it
      canvas.innerHTML = `
        <div class="empty-canvas">
          <i class="fa fa-database fa-3x margin-bottom-10"></i>
          <h5>Welcome to the Data Editor</h5>
          <p>Select a collection or dataset from the left panel to get started</p>
          <p style="margin-top: 20px;">Or create something new:</p>
          <div style="margin-top: 15px;">
            <button class="button radius success new-collection-action">
              <i class="fa fa-folder-plus"></i> Create Collection
            </button>
            <button class="button radius primary import-dataset-action">
              <i class="fa fa-file-import"></i> Import Dataset
            </button>
          </div>
        </div>
      `;
      // Re-attach event listeners for the action buttons
      document.querySelectorAll('.new-collection-action').forEach(btn => {
        btn.addEventListener('click', () => this.showNewCollectionModal());
      });
      document.querySelectorAll('.import-dataset-action').forEach(btn => {
        btn.addEventListener('click', () => this.showImportDatasetModal());
      });
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
