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

class VisualCRMEditor {
  constructor(options) {
    this.token = options.token || '';
    this.section = options.section || 'forms';
    this.selectedId = options.selectedId || null;

    // State
    this.currentSection = null;
    this.currentSelectedId = null;
    this.currentSubmissionFilter = 'new';
    this.currentPage = 1;
    this.currentRecordId = null;

    // Cached data
    this.formCategories = [];
    this.mailingLists = [];
  }

  init() {
    this.setupDarkMode();
    this.setupEventListeners();
    this.setupModalListeners();
    this.setupDetailPanelResize();
    this.loadInitialData();
  }

  // -------------------------------------------------------------------------
  // Setup
  // -------------------------------------------------------------------------

  setupDarkMode() {
    const darkModeToggle = document.getElementById('dark-mode-toggle-menu');
    if (!darkModeToggle) return;

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

  setupEventListeners() {
    // Reload button
    document.getElementById('reload-btn')?.addEventListener('click', () => {
      this.loadInitialData();
      if (this.currentSection) {
        this.loadListForSection(this.currentSection, this.currentSelectedId);
      }
    });

    // Search
    document.getElementById('crm-search-btn')?.addEventListener('click', () => this.handleSearch());
    document.getElementById('crm-search-input')?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') this.handleSearch();
    });

    // Submit filter buttons
    document.querySelectorAll('.crm-filter-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        document.querySelectorAll('.crm-filter-btn').forEach(b => b.classList.remove('active'));
        e.currentTarget.classList.add('active');
        this.currentSubmissionFilter = e.currentTarget.dataset.filter;
        this.currentPage = 1;
        this.loadFormSubmissions(this.currentSelectedId);
      });
    });

    // Submission action buttons
    document.getElementById('btn-claim-submission')?.addEventListener('click', () => {
      this.updateSubmission(this.currentRecordId, 'claim');
    });
    document.getElementById('btn-process-submission')?.addEventListener('click', () => {
      this.updateSubmission(this.currentRecordId, 'process');
    });
    document.getElementById('btn-dismiss-submission')?.addEventListener('click', () => {
      this.updateSubmission(this.currentRecordId, 'dismiss');
    });

    // View customer orders
    document.getElementById('btn-view-customer-orders')?.addEventListener('click', () => {
      if (this.currentRecordId) {
        this.showPanel('orders');
        this.loadOrders(this.currentRecordId);
      }
    });

    // Export products CSV
    document.getElementById('btn-export-products')?.addEventListener('click', () => {
      window.location.href = '/json/ecommerce/products/export-csv?token=' + encodeURIComponent(this.token);
    });

    // Sync products
    document.getElementById('btn-sync-products')?.addEventListener('click', () => {
      this.syncProducts();
    });

    // Inline users search bar
    document.getElementById('crm-users-search-btn')?.addEventListener('click', () => {
      this.currentPage = 1;
      this.loadUsers();
    });
    document.getElementById('crm-users-search-input')?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        this.currentPage = 1;
        this.loadUsers();
      }
    });

    // Back button from members to mailing lists index
    document.getElementById('btn-back-to-mailing-lists')?.addEventListener('click', () => {
      this.clearDetailPanel();
      this.showPanel('mailing-lists');
      this.showMailingListsIndex();
      document.querySelectorAll('.crm-nav-item').forEach(i => i.classList.remove('active'));
      const listsNavItem = document.querySelector('.crm-nav-item[data-section="mailing-lists"]');
      if (listsNavItem) listsNavItem.classList.add('active');
    });

    // Back button from form submissions to forms index
    document.getElementById('btn-back-to-forms')?.addEventListener('click', () => {
      this.clearDetailPanel();
      this.showPanel('forms-index');
      this.showFormsIndex();
      document.querySelectorAll('.crm-nav-item').forEach(i => i.classList.remove('active'));
      const formsNavItem = document.querySelector('.crm-nav-item[data-section="forms-index"]');
      if (formsNavItem) formsNavItem.classList.add('active');
    });

    // Nav items with data-section (includes new static mailing list items)
    document.querySelectorAll('.crm-nav-item[data-section]').forEach(item => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        const section = item.dataset.section;
        const id = item.dataset.id || null;
        this.activateNavItem(item);
        this.loadListForSection(section, id);
      });
    });
  }

  // -------------------------------------------------------------------------
  // Modals — Add Records
  // -------------------------------------------------------------------------

  setupModalListeners() {
    // Generic close (X button or Cancel button with crm-modal-close class)
    document.addEventListener('click', (e) => {
      const closeBtn = e.target.closest('.crm-modal-close');
      if (closeBtn) {
        const modalId = closeBtn.dataset.modal;
        if (modalId) this.closeModal(modalId);
      }
      // Click on overlay background itself
      if (e.target.classList.contains('crm-modal-overlay')) {
        this.closeModal(e.target.id);
      }
    });

    // Escape key closes any open modal
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        document.querySelectorAll('.crm-modal-overlay').forEach(overlay => {
          if (overlay.style.display !== 'none') this.closeModal(overlay.id);
        });
      }
    });

    // --- Add Mailing List ---
    document.getElementById('btn-add-mailing-list')?.addEventListener('click', () => {
      this.resetModal('crm-modal-mailing-list');
      this.openModal('crm-modal-mailing-list');
    });
    document.getElementById('btn-save-mailing-list')?.addEventListener('click', () => {
      this.saveMailingList();
    });

    // --- Add Customer ---
    document.getElementById('btn-add-customer')?.addEventListener('click', () => {
      this.resetModal('crm-modal-customer');
      this.openModal('crm-modal-customer');
    });
    document.getElementById('btn-save-customer')?.addEventListener('click', () => {
      this.saveCustomer();
    });

    // Inline customer search
    document.getElementById('crm-customers-search-btn')?.addEventListener('click', () => {
      this.currentPage = 1;
      this.loadCustomers();
    });
    document.getElementById('crm-customers-search-input')?.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        this.currentPage = 1;
        this.loadCustomers();
      }
    });

    // --- Add Product Category ---
    document.getElementById('btn-add-product-category')?.addEventListener('click', () => {
      this.resetModal('crm-modal-product-category');
      this.openModal('crm-modal-product-category');
    });
    document.getElementById('btn-save-product-category')?.addEventListener('click', () => {
      this.saveProductCategory();
    });

    // --- Add Product ---
    document.getElementById('btn-add-product')?.addEventListener('click', () => {
      this.resetModal('crm-modal-product');
      this.openModal('crm-modal-product');
    });
    document.getElementById('btn-save-product')?.addEventListener('click', () => {
      this.saveProduct();
    });

    // --- Add Pricing Rule ---
    document.getElementById('btn-add-pricing-rule')?.addEventListener('click', () => {
      this.resetModal('crm-modal-pricing-rule');
      this.openModal('crm-modal-pricing-rule');
    });
    document.getElementById('btn-save-pricing-rule')?.addEventListener('click', () => {
      this.savePricingRule();
    });

    // --- Add Sales Tax Nexus ---
    document.getElementById('btn-add-sales-tax')?.addEventListener('click', () => {
      this.resetModal('crm-modal-sales-tax');
      this.openModal('crm-modal-sales-tax');
    });
    document.getElementById('btn-save-sales-tax')?.addEventListener('click', () => {
      this.saveSalesTaxNexus();
    });

    // --- Add Shipping Rate ---
    document.getElementById('btn-add-shipping-rate')?.addEventListener('click', () => {
      this.resetModal('crm-modal-shipping-rate');
      this.loadShippingMethods().then(() => {
        this.openModal('crm-modal-shipping-rate');
      });
    });
    document.getElementById('btn-save-shipping-rate')?.addEventListener('click', () => {
      this.saveShippingRate();
    });

    // --- Add User Group ---
    document.getElementById('btn-add-user-group')?.addEventListener('click', () => {
      this.resetModal('crm-modal-user-group');
      document.getElementById('crm-modal-user-group-title').textContent = 'Add User Group';
      this.openModal('crm-modal-user-group');
    });
    document.getElementById('btn-save-user-group')?.addEventListener('click', () => {
      this.saveUserGroup();
    });
  }

  openModal(id) {
    const modal = document.getElementById(id);
    if (modal) {
      modal.style.display = 'flex';
      // Focus first input
      const firstInput = modal.querySelector('input, select, textarea');
      if (firstInput) setTimeout(() => firstInput.focus(), 50);
    }
  }

  closeModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.style.display = 'none';
  }

  resetModal(id) {
    const modal = document.getElementById(id);
    if (!modal) return;
    modal.querySelectorAll('input[type=text], input[type=email], input[type=number], textarea').forEach(el => {
      el.value = '';
    });
    modal.querySelectorAll('input[type=checkbox]').forEach(el => {
      // Re-apply the defaults that were set via the `checked` attribute
      el.checked = el.defaultChecked;
    });
    modal.querySelectorAll('select').forEach(el => {
      el.selectedIndex = 0;
    });
    const errEl = modal.querySelector('.crm-modal-error');
    if (errEl) { errEl.style.display = 'none'; errEl.textContent = ''; }
  }

  showModalError(modalId, message) {
    const errEl = document.querySelector('#' + modalId + ' .crm-modal-error');
    if (errEl) {
      errEl.textContent = message;
      errEl.style.display = 'block';
    }
  }

  // -------------------------------------------------------------------------
  // Modal AJAX Save Handlers
  // -------------------------------------------------------------------------

  saveMailingList() {
    const name = document.getElementById('ml-name')?.value.trim();
    if (!name) { this.showModalError('crm-modal-mailing-list', 'Name is required.'); return; }

    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('name', name);
    formData.append('title', document.getElementById('ml-title')?.value.trim() || '');
    formData.append('description', document.getElementById('ml-description')?.value.trim() || '');
    formData.append('showOnline', document.getElementById('ml-show-online')?.checked ? 'true' : 'false');

    fetch('/json/crm/mailinglists/save', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      if (data.status === 'ok') {
        this.closeModal('crm-modal-mailing-list');
        this.fetchMailingLists().then(() => {
          const listsPanel = document.getElementById('crm-mailing-lists-panel');
          if (listsPanel && listsPanel.style.display !== 'none') {
            this.showMailingListsIndex();
          }
          const dashPanel = document.getElementById('crm-mailing-dashboard');
          if (dashPanel && dashPanel.style.display !== 'none') {
            this.showMailingDashboard();
          }
        });
      } else {
        this.showModalError('crm-modal-mailing-list', data.error || 'Failed to save mailing list.');
      }
    })
    .catch(() => this.showModalError('crm-modal-mailing-list', 'An error occurred. Please try again.'));
  }

  saveCustomer() {
    const firstName = document.getElementById('cust-first-name')?.value.trim();
    const lastName = document.getElementById('cust-last-name')?.value.trim();
    const email = document.getElementById('cust-email')?.value.trim();
    if (!firstName) { this.showModalError('crm-modal-customer', 'First name is required.'); return; }
    if (!lastName) { this.showModalError('crm-modal-customer', 'Last name is required.'); return; }
    if (!email) { this.showModalError('crm-modal-customer', 'Email is required.'); return; }

    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('firstName', firstName);
    formData.append('lastName', lastName);
    formData.append('email', email);
    formData.append('organization', document.getElementById('cust-organization')?.value.trim() || '');

    fetch('/json/crm/customers/save', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      if (data.status === 'ok') {
        this.closeModal('crm-modal-customer');
        this.currentPage = 1;
        this.loadCustomers();
      } else {
        this.showModalError('crm-modal-customer', data.error || 'Failed to save customer.');
      }
    })
    .catch(() => this.showModalError('crm-modal-customer', 'An error occurred. Please try again.'));
  }

  saveProductCategory() {
    const name = document.getElementById('pc-name')?.value.trim();
    if (!name) { this.showModalError('crm-modal-product-category', 'Name is required.'); return; }

    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('name', name);
    formData.append('uniqueId', document.getElementById('pc-unique-id')?.value.trim() || '');
    formData.append('description', document.getElementById('pc-description')?.value.trim() || '');
    formData.append('enabled', document.getElementById('pc-enabled')?.checked ? 'true' : 'false');

    fetch('/json/crm/product-categories/save', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      if (data.status === 'ok') {
        this.closeModal('crm-modal-product-category');
        this.currentPage = 1;
        this.loadProductCategories();
      } else {
        this.showModalError('crm-modal-product-category', data.error || 'Failed to save product category.');
      }
    })
    .catch(() => this.showModalError('crm-modal-product-category', 'An error occurred. Please try again.'));
  }

  saveProduct() {
    const name = document.getElementById('prod-name')?.value.trim();
    if (!name) { this.showModalError('crm-modal-product', 'Name is required.'); return; }

    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('name', name);
    formData.append('uniqueId', document.getElementById('prod-unique-id')?.value.trim() || '');
    formData.append('description', document.getElementById('prod-description')?.value.trim() || '');
    formData.append('enabled', document.getElementById('prod-enabled')?.checked ? 'true' : 'false');

    fetch('/json/crm/products/save', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      if (data.status === 'ok') {
        this.closeModal('crm-modal-product');
        this.currentPage = 1;
        this.loadProducts();
      } else {
        this.showModalError('crm-modal-product', data.error || 'Failed to save product.');
      }
    })
    .catch(() => this.showModalError('crm-modal-product', 'An error occurred. Please try again.'));
  }

  savePricingRule() {
    const name = document.getElementById('pr-name')?.value.trim();
    if (!name) { this.showModalError('crm-modal-pricing-rule', 'Display name is required.'); return; }

    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('name', name);
    formData.append('promoCode', document.getElementById('pr-promo-code')?.value.trim() || '');
    formData.append('description', document.getElementById('pr-description')?.value.trim() || '');
    formData.append('subtotalPercent', document.getElementById('pr-subtotal-percent')?.value || '');
    formData.append('subtractAmount', document.getElementById('pr-subtract-amount')?.value || '');
    formData.append('freeShipping', document.getElementById('pr-free-shipping')?.checked ? 'true' : 'false');
    formData.append('enabled', document.getElementById('pr-enabled')?.checked ? 'true' : 'false');

    fetch('/json/crm/pricing-rules/save', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      if (data.status === 'ok') {
        this.closeModal('crm-modal-pricing-rule');
        this.currentPage = 1;
        this.loadPricingRules();
      } else {
        this.showModalError('crm-modal-pricing-rule', data.error || 'Failed to save pricing rule.');
      }
    })
    .catch(() => this.showModalError('crm-modal-pricing-rule', 'An error occurred. Please try again.'));
  }

  saveSalesTaxNexus() {
    const street = document.getElementById('st-street')?.value.trim();
    const city = document.getElementById('st-city')?.value.trim();
    const state = document.getElementById('st-state')?.value.trim();
    const country = document.getElementById('st-country')?.value.trim();
    const postalCode = document.getElementById('st-postal-code')?.value.trim();
    if (!street) { this.showModalError('crm-modal-sales-tax', 'Street address is required.'); return; }
    if (!city) { this.showModalError('crm-modal-sales-tax', 'City is required.'); return; }
    if (!state) { this.showModalError('crm-modal-sales-tax', 'State is required.'); return; }
    if (!country) { this.showModalError('crm-modal-sales-tax', 'Country is required.'); return; }
    if (!postalCode) { this.showModalError('crm-modal-sales-tax', 'Postal code is required.'); return; }

    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('street', street);
    formData.append('addressLine2', document.getElementById('st-address-line2')?.value.trim() || '');
    formData.append('city', city);
    formData.append('state', state);
    formData.append('country', country);
    formData.append('postalCode', postalCode);

    fetch('/json/crm/sales-tax/save', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      if (data.status === 'ok') {
        this.closeModal('crm-modal-sales-tax');
        this.currentPage = 1;
        this.loadSalesTaxNexus();
      } else {
        this.showModalError('crm-modal-sales-tax', data.error || 'Failed to save nexus address.');
      }
    })
    .catch(() => this.showModalError('crm-modal-sales-tax', 'An error occurred. Please try again.'));
  }

  loadShippingMethods() {
    return fetch('/json/crm/shipping-rates/save', {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      const select = document.getElementById('sr-shipping-method');
      if (!select) return;
      const methods = data.shippingMethods || [];
      const options = methods.map(m => `<option value="${m.id}">${this.escHtml(m.name)}</option>`).join('');
      select.innerHTML = '<option value="">Select method...</option>' + options;
    })
    .catch(() => {
      console.error('Failed to load shipping methods');
    });
  }

  saveShippingRate() {
    const countryCode = document.getElementById('sr-country')?.value;
    const region = document.getElementById('sr-region')?.value;
    const postalCode = document.getElementById('sr-postal-code')?.value.trim();
    const shippingMethodId = document.getElementById('sr-shipping-method')?.value;
    const shippingFee = document.getElementById('sr-shipping-fee')?.value;
    const handlingFee = document.getElementById('sr-handling-fee')?.value;
    if (!countryCode) { this.showModalError('crm-modal-shipping-rate', 'Country is required.'); return; }
    if (!region) { this.showModalError('crm-modal-shipping-rate', 'State/Region is required.'); return; }
    if (!postalCode) { this.showModalError('crm-modal-shipping-rate', 'Postal code is required.'); return; }
    if (!shippingMethodId) { this.showModalError('crm-modal-shipping-rate', 'Shipping method is required.'); return; }
    if (shippingFee === '' || shippingFee === null) { this.showModalError('crm-modal-shipping-rate', 'Shipping fee is required.'); return; }
    if (handlingFee === '' || handlingFee === null) { this.showModalError('crm-modal-shipping-rate', 'Handling fee is required.'); return; }

    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('countryCode', countryCode);
    formData.append('region', region);
    formData.append('postalCode', postalCode);
    formData.append('shippingMethodId', shippingMethodId);
    formData.append('shippingFee', shippingFee);
    formData.append('handlingFee', handlingFee);
    formData.append('minSubTotal', document.getElementById('sr-min-subtotal')?.value || '');
    formData.append('displayText', document.getElementById('sr-display-text')?.value.trim() || '');

    fetch('/json/crm/shipping-rates/save', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      if (data.status === 'ok') {
        this.closeModal('crm-modal-shipping-rate');
        this.currentPage = 1;
        this.loadShippingRates();
      } else {
        this.showModalError('crm-modal-shipping-rate', data.error || 'Failed to save shipping rate.');
      }
    })
    .catch(() => this.showModalError('crm-modal-shipping-rate', 'An error occurred. Please try again.'));
  }

  setupDetailPanelResize() {
    const handle = document.getElementById('crm-properties-panel-resize-handle');
    const panel = document.getElementById('crm-detail-panel');
    if (!handle || !panel) return;

    let isResizing = false;
    let startX = 0;
    let startWidth = 0;

    handle.addEventListener('mousedown', (e) => {
      isResizing = true;
      startX = e.clientX;
      startWidth = panel.offsetWidth;
      document.body.style.userSelect = 'none';
      document.body.style.cursor = 'col-resize';
    });

    document.addEventListener('mousemove', (e) => {
      if (!isResizing) return;
      const delta = startX - e.clientX;
      const newWidth = Math.max(240, Math.min(600, startWidth + delta));
      panel.style.width = newWidth + 'px';
    });

    document.addEventListener('mouseup', () => {
      isResizing = false;
      document.body.style.userSelect = '';
      document.body.style.cursor = '';
    });
  }

  // -------------------------------------------------------------------------
  // Initial data load
  // -------------------------------------------------------------------------

  loadInitialData() {
    this.showLoading(true);
    Promise.all([
      this.fetchFormCategories(),
      this.fetchMailingLists()
    ]).then(() => {
      this.updateDashboardStats();
      this.showLoading(false);

      // Navigate to initial section if provided
      if (this.section && this.section !== 'forms') {
        this.loadListForSection(this.section, this.selectedId);
      } else if (this.selectedId) {
        // Deep-link directly to a specific form's submissions
        this.loadFormSubmissions(this.selectedId);
      } else {
        // Default to Forms Dashboard when no specific section is set
        this.loadListForSection('forms-dashboard', null);
        document.querySelectorAll('.crm-nav-item').forEach(i => i.classList.remove('active'));
        const navItem = document.querySelector('.crm-nav-item[data-section="forms-dashboard"]');
        if (navItem) navItem.classList.add('active');
      }
    }).catch(() => {
      this.showLoading(false);
    });
  }

  fetchFormCategories() {
    return fetch('/json/crm/forms/list', {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.formCategories = Array.isArray(data) ? data : [];
      this.renderFormCategoriesNav();
    })
    .catch(err => {
      console.error('Error loading form categories', err);
    });
  }

  fetchMailingLists() {
    return fetch('/json/crm/mailinglists', {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.mailingLists = (data && data.mailingLists) ? data.mailingLists : [];
      this.renderMailingListsNav();
    })
    .catch(err => {
      console.error('Error loading mailing lists', err);
      document.getElementById('mailing-lists-nav').innerHTML =
        '<div class="crm-nav-loading" style="color: var(--editor-error, #c0392b);">Failed to load</div>';
    });
  }

  renderFormCategoriesNav() {
    // The nav now has static 'Dashboard' and 'Forms' items — just update the badge counts
    const totalBadge = document.getElementById('forms-total-badge');
    if (totalBadge) {
      const total = this.formCategories ? this.formCategories.length : 0;
      if (total > 0) {
        totalBadge.textContent = total;
        totalBadge.style.display = '';
      } else {
        totalBadge.style.display = 'none';
      }
    }
    // Update the main nav new-submissions badge if any forms have new submissions
    // (could be used in the future for a top-level badge)
  }

  renderMailingListsNav() {
    // The nav now has static 'Dashboard' and 'Lists' items — just update the badge count
    const totalBadge = document.getElementById('mailing-lists-total-badge');
    if (totalBadge) {
      const total = this.mailingLists ? this.mailingLists.length : 0;
      if (total > 0) {
        totalBadge.textContent = total;
        totalBadge.style.display = '';
      } else {
        totalBadge.style.display = 'none';
      }
    }
  }

  showFormsDashboard() {
    const container = document.getElementById('crm-forms-dashboard-content');
    if (!container) return;

    const totalForms = this.formCategories ? this.formCategories.length : 0;
    const totalNew = this.formCategories ? this.formCategories.reduce((sum, c) => sum + (c.newCount || 0), 0) : 0;

    if (!this.formCategories || this.formCategories.length === 0) {
      container.innerHTML = `
        <div class="crm-stats-row">
          <div class="crm-stat-card">
            <div class="crm-stat-icon"><i class="far fa-file-alt"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">0</div>
              <div class="crm-stat-label">Forms</div>
            </div>
          </div>
        </div>
        <p style="text-align:center; color:var(--editor-text-muted); font-size:13px; margin-top:8px;">No forms found.</p>`;
      return;
    }

    container.innerHTML = `
      <div class="crm-stats-row">
        <div class="crm-stat-card">
          <div class="crm-stat-icon"><i class="far fa-file-alt"></i></div>
          <div class="crm-stat-info">
            <div class="crm-stat-value">${totalForms}</div>
            <div class="crm-stat-label">Forms</div>
          </div>
        </div>
        <div class="crm-stat-card">
          <div class="crm-stat-icon"><i class="far fa-inbox"></i></div>
          <div class="crm-stat-info">
            <div class="crm-stat-value">${totalNew}</div>
            <div class="crm-stat-label">New Submissions</div>
          </div>
        </div>
      </div>
      <div style="padding: 0 16px 16px;">
        <div style="font-size:11px; font-weight:600; color:var(--editor-text-muted); text-transform:uppercase; letter-spacing:0.05em; margin-bottom:10px; padding-bottom:6px; border-bottom:1px solid var(--editor-border);">
          Forms
        </div>
        ${this.formCategories.map(form => `
          <div class="crm-forms-dash-card" data-unique-id="${this.escHtml(form.formUniqueId || '')}" style="display:flex; align-items:center; gap:12px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; margin-bottom:8px; cursor:pointer; background:var(--editor-panel-bg); transition:background 0.12s;">
            <div style="flex:1; min-width:0;">
              <div style="font-size:13px; font-weight:500; color:var(--editor-text);">${this.escHtml(form.formUniqueId || '\u2014')}</div>
            </div>
            <div style="text-align:right; flex-shrink:0;">
              ${(form.newCount || 0) > 0 ? `<div style="font-size:18px; font-weight:700; color:var(--editor-primary);">${form.newCount}</div><div style="font-size:10px; color:var(--editor-text-muted);">new</div>` : ''}
            </div>
            <div style="color:var(--editor-text-muted); font-size:12px;"><i class="far fa-chevron-right"></i></div>
          </div>`).join('')}
      </div>`;

    container.querySelectorAll('.crm-forms-dash-card').forEach(card => {
      card.addEventListener('click', () => {
        const uniqueId = card.dataset.uniqueId;
        this.activateNavItem(document.querySelector('.crm-nav-item[data-section="forms-index"]'));
        this.loadFormSubmissions(uniqueId);
      });
      card.addEventListener('mouseenter', () => { card.style.background = 'var(--editor-hover-bg)'; });
      card.addEventListener('mouseleave', () => { card.style.background = 'var(--editor-panel-bg)'; });
    });
  }

  showFormsIndex() {
    const container = document.getElementById('crm-forms-index-list');
    if (!container) return;

    if (!this.formCategories || this.formCategories.length === 0) {
      container.innerHTML = '<div style="padding:20px; color:var(--editor-text-muted); text-align:center;">No forms found.</div>';
      return;
    }

    container.innerHTML = this.formCategories.map(form => {
      const newCount = form.newCount || 0;
      return `<div class="crm-record-item" data-unique-id="${this.escHtml(form.formUniqueId || '')}" data-type="form">
        <div class="crm-record-title">${this.escHtml(form.formUniqueId || '\u2014')}</div>
        <div class="crm-record-meta">
          ${newCount > 0 ? `<span class="crm-record-badge" style="background:var(--editor-primary,#428bca); color:#fff; opacity:0.9;">${newCount} new</span>` : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const uniqueId = item.dataset.uniqueId;
        this.currentSelectedId = uniqueId;
        this.currentPage = 1;
        this.clearDetailPanel();
        this.loadFormSubmissions(uniqueId);
      });
    });
  }

  showMailingDashboard() {
    const container = document.getElementById('crm-mailing-dashboard-content');
    if (!container) return;

    if (!this.mailingLists || this.mailingLists.length === 0) {
      container.innerHTML = `
        <div class="crm-stats-row">
          <div class="crm-stat-card">
            <div class="crm-stat-icon"><i class="far fa-envelope"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">0</div>
              <div class="crm-stat-label">Mailing Lists</div>
            </div>
          </div>
        </div>
        <p style="text-align:center; color:var(--editor-text-muted); font-size:13px; margin-top:8px;">
          No mailing lists yet. <a href="#" id="dash-go-to-lists">Go to Lists</a> to add one.
        </p>`;
      container.querySelector('#dash-go-to-lists')?.addEventListener('click', (e) => {
        e.preventDefault();
        this.showPanel('mailing-lists');
        this.showMailingListsIndex();
        document.querySelectorAll('.crm-nav-item').forEach(i => i.classList.remove('active'));
        const listsNavItem = document.querySelector('.crm-nav-item[data-section="mailing-lists"]');
        if (listsNavItem) listsNavItem.classList.add('active');
      });
      return;
    }

    const totalMembers = this.mailingLists.reduce((sum, l) => sum + (l.memberCount || 0), 0);
    container.innerHTML = `
      <div class="crm-stats-row">
        <div class="crm-stat-card">
          <div class="crm-stat-icon"><i class="far fa-envelope"></i></div>
          <div class="crm-stat-info">
            <div class="crm-stat-value">${this.mailingLists.length}</div>
            <div class="crm-stat-label">Mailing Lists</div>
          </div>
        </div>
        <div class="crm-stat-card">
          <div class="crm-stat-icon"><i class="far fa-users"></i></div>
          <div class="crm-stat-info">
            <div class="crm-stat-value">${totalMembers.toLocaleString()}</div>
            <div class="crm-stat-label">Total Members</div>
          </div>
        </div>
      </div>
      <div style="padding: 0 16px 16px;">
        <div style="font-size:11px; font-weight:600; color:var(--editor-text-muted); text-transform:uppercase; letter-spacing:0.05em; margin-bottom:10px; padding-bottom:6px; border-bottom:1px solid var(--editor-border);">
          Lists
        </div>
        ${this.mailingLists.map(list => `
          <div class="crm-mailing-dash-card" data-id="${list.id}" style="display:flex; align-items:center; gap:12px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; margin-bottom:8px; cursor:pointer; background:var(--editor-panel-bg); transition:background 0.12s;">
            <div style="flex:1; min-width:0;">
              <div style="font-size:13px; font-weight:500; color:var(--editor-text);">${this.escHtml(list.name || list.title || '\u2014')}</div>
              ${list.title && list.name !== list.title ? `<div style="font-size:11px; color:var(--editor-text-muted);">${this.escHtml(list.title)}</div>` : ''}
              ${list.description ? `<div style="font-size:11px; color:var(--editor-text-muted);">${this.escHtml(list.description)}</div>` : ''}
            </div>
            <div style="text-align:right; flex-shrink:0;">
              <div style="font-size:18px; font-weight:700; color:var(--editor-primary);">${(list.memberCount || 0).toLocaleString()}</div>
              <div style="font-size:10px; color:var(--editor-text-muted);">members</div>
            </div>
            <div style="color:var(--editor-text-muted); font-size:12px;"><i class="far fa-chevron-right"></i></div>
          </div>`).join('')}
      </div>`;

    container.querySelectorAll('.crm-mailing-dash-card').forEach(card => {
      card.addEventListener('click', () => {
        const id = card.dataset.id;
        this.currentSelectedId = id;
        this.currentPage = 1;
        this.clearDetailPanel();
        this.loadMailingListMembers(id);
      });
      card.addEventListener('mouseenter', () => { card.style.background = 'var(--editor-hover-bg)'; });
      card.addEventListener('mouseleave', () => { card.style.background = 'var(--editor-panel-bg)'; });
    });
  }

  showMailingListsIndex() {
    const container = document.getElementById('crm-mailing-lists-index');
    if (!container) return;

    if (!this.mailingLists || this.mailingLists.length === 0) {
      container.innerHTML = '<div style="padding:20px; color:var(--editor-text-muted); text-align:center;">No mailing lists found. Click Add to create one.</div>';
      return;
    }

    container.innerHTML = this.mailingLists.map(list => {
      const memberCount = list.memberCount || 0;
      return `<div class="crm-record-item" data-id="${list.id}" data-type="mailinglist">
        <div class="crm-record-title">${this.escHtml(list.name || list.title || '\u2014')}</div>
        <div class="crm-record-meta">
          <span class="crm-record-badge" style="background:var(--editor-primary,#428bca); color:#fff; opacity:0.9;">${memberCount.toLocaleString()} members</span>
          ${list.title && list.name !== list.title ? `<span>${this.escHtml(list.title)}</span>` : ''}
          ${list.description ? `<span>${this.escHtml(list.description)}</span>` : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = item.dataset.id;
        this.currentSelectedId = id;
        this.currentPage = 1;
        this.clearDetailPanel();
        this.loadMailingListMembers(id);
      });
    });
  }

  updateDashboardStats() {
    const totalForms = this.formCategories ? this.formCategories.length : 0;
    const totalNew = this.formCategories ? this.formCategories.reduce((sum, c) => sum + (c.newCount || 0), 0) : 0;
    const totalMembers = this.mailingLists ? this.mailingLists.reduce((sum, l) => sum + (l.memberCount || 0), 0) : 0;

    const statFormsEl = document.getElementById('stat-forms-value');
    const statNewEl = document.getElementById('stat-new-value');
    const statListsEl = document.getElementById('stat-lists-value');

    if (statFormsEl) statFormsEl.textContent = totalForms;
    if (statNewEl) statNewEl.textContent = totalNew;
    if (statListsEl) statListsEl.textContent = totalMembers.toLocaleString();
  }

  // -------------------------------------------------------------------------
  // Navigation
  // -------------------------------------------------------------------------

  activateNavItem(activeItem) {
    document.querySelectorAll('.crm-nav-item').forEach(i => i.classList.remove('active'));
    if (activeItem) activeItem.classList.add('active');
  }

  loadListForSection(section, id) {
    this.currentSection = section;
    this.currentSelectedId = id;
    this.currentPage = 1;
    this.clearDetailPanel();

    switch (section) {
      case 'forms-dashboard':
        this.showPanel('forms-dashboard');
        this.showFormsDashboard();
        break;
      case 'forms-index':
        this.showPanel('forms-index');
        this.showFormsIndex();
        break;
      case 'forms':
        this.loadFormSubmissions(id);
        break;
      case 'mailing-dashboard':
        this.showPanel('mailing-dashboard');
        this.showMailingDashboard();
        break;
      case 'mailing-lists':
        this.showPanel('mailing-lists');
        this.showMailingListsIndex();
        break;
      case 'mailinglist':
        this.loadMailingListMembers(id);
        break;
      case 'customers':
        this.showPanel('customers');
        this.loadCustomers();
        break;
      case 'orders':
        this.showPanel('orders');
        this.loadOrders(id);
        break;
      case 'product-catalog-dashboard':
        this.showPanel('product-catalog-dashboard');
        this.showProductCatalogDashboard();
        break;
      case 'product-categories':
        this.showPanel('product-categories');
        this.loadProductCategories();
        break;
      case 'products':
        this.showPanel('products');
        this.loadProducts();
        break;
      case 'pricing-rules':
        this.showPanel('pricing-rules');
        this.loadPricingRules();
        break;
      case 'sales-tax':
        this.showPanel('sales-tax');
        this.loadSalesTaxNexus();
        break;
      case 'shipping-rates':
        this.showPanel('shipping-rates');
        this.loadShippingRates();
        break;
      case 'users-dashboard':
        this.showPanel('users-dashboard');
        this.showUsersDashboard();
        break;
      case 'users':
        this.showPanel('users');
        this.loadUsers();
        break;
      case 'user-groups':
        this.showPanel('user-groups');
        this.loadUserGroups();
        break;
      case 'user-roles':
        this.showPanel('user-roles');
        this.loadUserRoles();
        break;
    }
  }

  showPanel(name) {
    document.querySelectorAll('.crm-panel-content').forEach(p => {
      p.style.display = 'none';
    });
    const panelMap = {
      dashboard: 'crm-dashboard',
      'forms-dashboard': 'crm-forms-dashboard',
      'forms-index': 'crm-forms-index-panel',
      'mailing-dashboard': 'crm-mailing-dashboard',
      'mailing-lists': 'crm-mailing-lists-panel',
      forms: 'crm-submissions-panel',
      mailinglist: 'crm-members-panel',
      customers: 'crm-customers-panel',
      orders: 'crm-orders-panel',
      'product-catalog-dashboard': 'crm-product-catalog-dashboard',
      'product-categories': 'crm-product-categories-panel',
      products: 'crm-products-panel',
      'pricing-rules': 'crm-pricing-rules-panel',
      'sales-tax': 'crm-sales-tax-panel',
      'shipping-rates': 'crm-shipping-rates-panel',
      'users-dashboard': 'crm-users-dashboard',
      users: 'crm-users-panel',
      'user-groups': 'crm-user-groups-panel',
      'user-roles': 'crm-user-roles-panel'
    };
    const panelId = panelMap[name];
    if (panelId) {
      const panel = document.getElementById(panelId);
      if (panel) panel.style.display = 'flex';
    }
  }

  // -------------------------------------------------------------------------
  // Form Submissions
  // -------------------------------------------------------------------------

  loadFormSubmissions(formUniqueId) {
    if (!formUniqueId) return;
    this.showPanel('forms');

    const titleEl = document.getElementById('crm-submissions-title');
    if (titleEl) titleEl.innerHTML = `<i class="far fa-file-alt"></i> ${this.escHtml(formUniqueId)}`;

    this.showLoading(true);
    const params = new URLSearchParams({
      formUniqueId,
      status: this.currentSubmissionFilter,
      page: this.currentPage,
      limit: 25
    });

    fetch(`/json/crm/forms/submissions?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderSubmissionsList(data.submissions || []);
      this.renderPagination('crm-submissions-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadFormSubmissions(formUniqueId);
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-submissions-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load submissions</div>';
    });
  }

  renderSubmissionsList(submissions) {
    const container = document.getElementById('crm-submissions-list');
    if (!container) return;

    if (!submissions || submissions.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No submissions found</div>';
      return;
    }

    container.innerHTML = submissions.map(sub => {
      const status = sub.processed ? 'processed' : (sub.dismissed ? 'dismissed' : (sub.claimed ? 'claimed' : 'new'));
      const statusBadge = `<span class="crm-record-badge crm-badge-${status}">${status}</span>`;
      const date = sub.created ? new Date(sub.created).toLocaleDateString() : '';
      const summary = sub.summary || '(no content)';
      return `<div class="crm-record-item" data-id="${sub.id}" data-type="submission">
        <div class="crm-record-title">${this.escHtml(summary)}</div>
        <div class="crm-record-meta">
          ${statusBadge}
          <span>${this.escHtml(date)}</span>
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        this.loadSubmissionDetail(parseInt(item.dataset.id, 10));
      });
    });
  }

  loadSubmissionDetail(id) {
    this.currentRecordId = id;
    this.showLoading(true);

    fetch(`/json/crm/forms/submission?id=${id}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderSubmissionDetail(data);
    })
    .catch(() => {
      this.showLoading(false);
    });
  }

  renderSubmissionDetail(data) {
    this.showDetailPanel('crm-submission-detail');

    const titleEl = document.getElementById('crm-detail-submission-title');
    if (titleEl) titleEl.textContent = `Form: ${data.formUniqueId || '—'}`;

    const metaEl = document.getElementById('crm-submission-meta');
    if (metaEl) {
      const date = data.created ? new Date(data.created).toLocaleString() : '—';
      metaEl.innerHTML = `
        <div class="crm-meta-row">
          <span class="crm-meta-label">Submitted</span>
          <span class="crm-meta-value">${this.escHtml(date)}</span>
        </div>
        <div class="crm-meta-row">
          <span class="crm-meta-label">IP Address</span>
          <span class="crm-meta-value">${this.escHtml(data.ipAddress || '—')}</span>
        </div>
        <div class="crm-meta-row">
          <span class="crm-meta-label">Page URL</span>
          <span class="crm-meta-value">${this.escHtml(data.url || '—')}</span>
        </div>
        <div class="crm-submission-status">
          ${data.claimed ? `<span class="crm-record-badge crm-badge-claimed">Claimed ${data.claimedDate ? new Date(data.claimedDate).toLocaleDateString() : ''}</span>` : ''}
          ${data.processed ? `<span class="crm-record-badge crm-badge-processed">Processed ${data.processedDate ? new Date(data.processedDate).toLocaleDateString() : ''}</span>` : ''}
          ${data.dismissed ? `<span class="crm-record-badge crm-badge-dismissed">Dismissed</span>` : ''}
          ${!data.claimed && !data.processed && !data.dismissed ? `<span class="crm-record-badge crm-badge-new">New</span>` : ''}
        </div>
      `;
    }

    const fieldsEl = document.getElementById('crm-submission-fields');
    if (fieldsEl) {
      if (data.fields && data.fields.length > 0) {
        fieldsEl.innerHTML = data.fields.map(field => {
          const label = field.label || field.name || 'Field';
          const value = field.value || '';
          const isEmpty = !value.trim();
          return `<div class="crm-field-item">
            <div class="crm-field-label">${this.escHtml(label)}</div>
            <div class="crm-field-value ${isEmpty ? 'empty' : ''}">${isEmpty ? '(empty)' : this.escHtml(value)}</div>
          </div>`;
        }).join('');
      } else {
        fieldsEl.innerHTML = '<div style="color: var(--editor-text-muted); font-size: 13px;">No fields available</div>';
      }
    }

    // Show/hide action buttons based on current status
    const btnClaim = document.getElementById('btn-claim-submission');
    const btnProcess = document.getElementById('btn-process-submission');
    const btnDismiss = document.getElementById('btn-dismiss-submission');
    if (btnClaim) btnClaim.style.display = data.claimed ? 'none' : '';
    if (btnProcess) btnProcess.style.display = data.processed ? 'none' : '';
    if (btnDismiss) btnDismiss.style.display = data.dismissed ? 'none' : '';
  }

  updateSubmission(id, action) {
    if (!id) return;
    this.showLoading(true);

    const formData = new FormData();
    formData.append('id', id);
    formData.append('action', action);
    formData.append('token', this.token);

    fetch('/json/crm/forms/submission/update', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      if (data.status === 'ok') {
        // Reload current list and clear detail
        this.loadFormSubmissions(this.currentSelectedId);
        this.clearDetailPanel();
      } else {
        alert('Error: ' + (data.error || 'Action failed'));
      }
    })
    .catch(() => {
      this.showLoading(false);
      alert('An error occurred. Please try again.');
    });
  }

  // -------------------------------------------------------------------------
  // Mailing Lists
  // -------------------------------------------------------------------------

  loadMailingListMembers(listId) {
    this.showPanel('mailinglist');
    const list = this.mailingLists.find(l => String(l.id) === String(listId));
    const titleEl = document.getElementById('crm-members-title');
    if (titleEl && list) titleEl.innerHTML = `<i class="far fa-envelope"></i> ${this.escHtml(list.name || list.title)}`;

    this.showLoading(true);
    const search = document.getElementById('crm-search-input')?.value || '';
    const params = new URLSearchParams({
      listId,
      page: this.currentPage,
      limit: 25
    });
    if (search) params.append('search', search);

    fetch(`/json/crm/mailinglists/members?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderMembersList(data.members || []);
      this.renderPagination('crm-members-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadMailingListMembers(listId);
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-members-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load members</div>';
    });
  }

  renderMembersList(members) {
    const container = document.getElementById('crm-members-list');
    if (!container) return;

    if (!members || members.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No members found</div>';
      return;
    }

    container.innerHTML = members.map(member => {
      const name = [member.firstName, member.lastName].filter(Boolean).join(' ') || member.email;
      const status = member.unsubscribed ? '<span class="crm-record-badge crm-badge-dismissed">Unsubscribed</span>'
        : (member.valid ? '' : '<span class="crm-record-badge crm-badge-claimed">Invalid</span>');
      return `<div class="crm-record-item" data-id="${member.id}" data-type="member">
        <div class="crm-record-title">${this.escHtml(name)}</div>
        <div class="crm-record-meta">
          ${status}
          <span>${this.escHtml(member.email || '')}</span>
          ${member.organization ? `<span>${this.escHtml(member.organization)}</span>` : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        // Find member data
        const id = parseInt(item.dataset.id, 10);
        const member = members.find(m => m.id === id);
        if (member) this.renderMemberDetail(member);
      });
    });
  }

  renderMemberDetail(member) {
    this.showDetailPanel('crm-member-detail');
    const infoEl = document.getElementById('crm-member-info');
    if (!infoEl) return;

    const name = [member.firstName, member.lastName].filter(Boolean).join(' ') || '—';
    const subscribed = member.subscribed ? new Date(member.subscribed).toLocaleDateString() : '—';
    const status = member.unsubscribed ? 'Unsubscribed' : (member.valid ? 'Active' : 'Invalid');

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Name</span>
        <span class="crm-meta-value">${this.escHtml(name)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Email</span>
        <span class="crm-meta-value">${this.escHtml(member.email || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Organization</span>
        <span class="crm-meta-value ${!member.organization ? 'empty-value' : ''}">${this.escHtml(member.organization || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Subscribed</span>
        <span class="crm-meta-value">${this.escHtml(subscribed)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Status</span>
        <span class="crm-meta-value">${this.escHtml(status)}</span>
      </div>
    `;
  }

  // -------------------------------------------------------------------------
  // Customers
  // -------------------------------------------------------------------------

  loadCustomers() {
    this.showPanel('customers');
    this.showLoading(true);

    // Use inline search bar (primary) or toolbar search as fallback
    const inlineSearch = document.getElementById('crm-customers-search-input')?.value.trim() || '';
    const toolbarSearch = document.getElementById('crm-search-input')?.value.trim() || '';
    const search = inlineSearch || toolbarSearch;
    const params = new URLSearchParams({ page: this.currentPage, limit: 25 });
    if (search) params.append('search', search);

    fetch(`/json/crm/customers?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderCustomersList(data.customers || []);
      this.renderPagination('crm-customers-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadCustomers();
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-customers-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load customers</div>';
    });
  }

  renderCustomersList(customers) {
    const container = document.getElementById('crm-customers-list');
    if (!container) return;

    if (!customers || customers.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No customers found</div>';
      return;
    }

    container.innerHTML = customers.map(c => {
      const name = [c.firstName, c.lastName].filter(Boolean).join(' ') || c.email || c.uniqueId;
      const orderInfo = c.orderCount > 0 ? `${c.orderCount} order${c.orderCount > 1 ? 's' : ''}` : 'No orders';
      const spend = c.totalSpend && parseFloat(c.totalSpend) > 0 ? ` · $${parseFloat(c.totalSpend).toFixed(2)}` : '';
      return `<div class="crm-record-item" data-id="${c.id}" data-type="customer">
        <div class="crm-record-title">${this.escHtml(name)}</div>
        <div class="crm-record-meta">
          <span>${this.escHtml(c.email || '')}</span>
          <span>${this.escHtml(orderInfo + spend)}</span>
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const customer = customers.find(c => c.id === id);
        if (customer) {
          this.currentRecordId = id;
          this.renderCustomerDetail(customer);
        }
      });
    });
  }

  renderCustomerDetail(customer) {
    this.showDetailPanel('crm-customer-detail');

    const titleEl = document.getElementById('crm-customer-name-title');
    if (titleEl) {
      const name = [customer.firstName, customer.lastName].filter(Boolean).join(' ') || customer.email || customer.uniqueId;
      titleEl.textContent = name;
    }

    const infoEl = document.getElementById('crm-customer-info');
    if (!infoEl) return;

    const created = customer.created ? new Date(customer.created).toLocaleDateString() : '—';
    const spend = customer.totalSpend && parseFloat(customer.totalSpend) > 0 ? `$${parseFloat(customer.totalSpend).toFixed(2)}` : '—';

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Email</span>
        <span class="crm-meta-value">${this.escHtml(customer.email || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Organization</span>
        <span class="crm-meta-value ${!customer.organization ? 'empty-value' : ''}">${this.escHtml(customer.organization || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Orders</span>
        <span class="crm-meta-value">${customer.orderCount || 0}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Total Spend</span>
        <span class="crm-meta-value">${this.escHtml(spend)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Customer Since</span>
        <span class="crm-meta-value">${this.escHtml(created)}</span>
      </div>
    `;
  }

  // -------------------------------------------------------------------------
  // Orders
  // -------------------------------------------------------------------------

  loadOrders(customerId) {
    this.showPanel('orders');
    this.showLoading(true);

    const titleEl = document.getElementById('crm-orders-title');
    if (titleEl) {
      titleEl.innerHTML = customerId
        ? `<i class="far fa-shopping-bag"></i> Orders for Customer`
        : `<i class="far fa-shopping-bag"></i> All Orders`;
    }

    const params = new URLSearchParams({ page: this.currentPage, limit: 25 });
    if (customerId) params.append('customerId', customerId);

    fetch(`/json/crm/orders?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderOrdersList(data.orders || []);
      this.renderPagination('crm-orders-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadOrders(customerId);
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-orders-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load orders</div>';
    });
  }

  renderOrdersList(orders) {
    const container = document.getElementById('crm-orders-list');
    if (!container) return;

    if (!orders || orders.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No orders found</div>';
      return;
    }

    container.innerHTML = orders.map(order => {
      const name = [order.firstName, order.lastName].filter(Boolean).join(' ') || order.email || `#${order.uniqueId}`;
      const total = order.totalAmount && parseFloat(order.totalAmount) > 0
        ? `$${parseFloat(order.totalAmount).toFixed(2)}` : '—';
      const statusText = order.canceled ? 'Canceled' : (order.shipped ? 'Shipped' : (order.processed ? 'Processed' : (order.paid ? 'Paid' : 'Pending')));
      const statusClass = order.canceled ? 'crm-badge-dismissed' : (order.shipped ? 'crm-badge-processed' : (order.paid ? 'crm-badge-new' : 'crm-badge-claimed'));
      const date = order.created ? new Date(order.created).toLocaleDateString() : '';
      return `<div class="crm-record-item" data-id="${order.id}" data-type="order">
        <div class="crm-record-title">${this.escHtml(name)} — ${this.escHtml(total)}</div>
        <div class="crm-record-meta">
          <span class="crm-record-badge ${statusClass}">${statusText}</span>
          <span>#${this.escHtml(order.uniqueId || String(order.id))}</span>
          <span>${this.escHtml(date)}</span>
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const order = orders.find(o => o.id === id);
        if (order) this.renderOrderDetail(order);
      });
    });
  }

  renderOrderDetail(order) {
    this.showDetailPanel('crm-order-detail');

    const titleEl = document.getElementById('crm-order-number-title');
    if (titleEl) titleEl.textContent = `Order #${order.uniqueId || order.id}`;

    const infoEl = document.getElementById('crm-order-info');
    if (!infoEl) return;

    const name = [order.firstName, order.lastName].filter(Boolean).join(' ') || order.email || '—';
    const total = order.totalAmount && parseFloat(order.totalAmount) > 0
      ? `$${parseFloat(order.totalAmount).toFixed(2)}` : '—';
    const created = order.created ? new Date(order.created).toLocaleString() : '—';
    const statusText = order.canceled ? 'Canceled' : (order.shipped ? 'Shipped' : (order.processed ? 'Processed' : (order.paid ? 'Paid' : 'Pending')));

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Customer</span>
        <span class="crm-meta-value">${this.escHtml(name)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Email</span>
        <span class="crm-meta-value">${this.escHtml(order.email || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Order Total</span>
        <span class="crm-meta-value">${this.escHtml(total)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Status</span>
        <span class="crm-meta-value">${this.escHtml(statusText)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Date</span>
        <span class="crm-meta-value">${this.escHtml(created)}</span>
      </div>
    `;
  }

  // -------------------------------------------------------------------------
  // Product Catalog - Dashboard
  // -------------------------------------------------------------------------

  showProductCatalogDashboard() {
    const container = document.getElementById('crm-product-catalog-dashboard-content');
    if (!container) return;

    container.innerHTML = '<div style="padding:20px; text-align:center; color:var(--editor-text-muted);"><i class="far fa-spinner fa-spin"></i> Loading...</div>';
    this.showLoading(true);

    Promise.all([
      fetch('/json/ecommerce/product-categories?page=1&limit=100', { headers: { 'X-CSRF-Token': this.token } }).then(r => r.json()),
      fetch('/json/ecommerce/products?page=1&limit=1', { headers: { 'X-CSRF-Token': this.token } }).then(r => r.json()),
      fetch('/json/ecommerce/pricing-rules?page=1&limit=1', { headers: { 'X-CSRF-Token': this.token } }).then(r => r.json()),
      fetch('/json/ecommerce/shipping-rates?page=1&limit=1', { headers: { 'X-CSRF-Token': this.token } }).then(r => r.json())
    ])
    .then(([categoriesData, productsData, pricingData, shippingData]) => {
      this.showLoading(false);
      const categories = categoriesData.categories || categoriesData.records || [];
      const totalCategories = categories.length;
      const totalProducts = productsData.total || 0;
      const totalPricingRules = pricingData.total || 0;
      const totalShippingRates = shippingData.total || 0;

      container.innerHTML = `
        <div class="crm-stats-row">
          <div class="crm-stat-card" style="cursor:pointer;" id="prod-dash-stat-categories">
            <div class="crm-stat-icon"><i class="far fa-tags"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">${totalCategories}</div>
              <div class="crm-stat-label">Product Categories</div>
            </div>
          </div>
          <div class="crm-stat-card" style="cursor:pointer;" id="prod-dash-stat-products">
            <div class="crm-stat-icon"><i class="far fa-cubes"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">${totalProducts.toLocaleString()}</div>
              <div class="crm-stat-label">Products</div>
            </div>
          </div>
          <div class="crm-stat-card" style="cursor:pointer;" id="prod-dash-stat-pricing">
            <div class="crm-stat-icon"><i class="far fa-percent"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">${totalPricingRules}</div>
              <div class="crm-stat-label">Pricing Rules</div>
            </div>
          </div>
          <div class="crm-stat-card" style="cursor:pointer;" id="prod-dash-stat-shipping">
            <div class="crm-stat-icon"><i class="far fa-truck"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">${totalShippingRates}</div>
              <div class="crm-stat-label">Shipping Rates</div>
            </div>
          </div>
        </div>
        ${categories.length > 0 ? `
        <div style="padding: 0 16px 16px;">
          <div style="font-size:11px; font-weight:600; color:var(--editor-text-muted); text-transform:uppercase; letter-spacing:0.05em; margin-bottom:10px; padding-bottom:6px; border-bottom:1px solid var(--editor-border);">
            Product Categories
          </div>
          ${categories.map(cat => `
            <div class="crm-prod-dash-card" data-id="${cat.id}" style="display:flex; align-items:center; gap:12px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; margin-bottom:8px; cursor:pointer; background:var(--editor-panel-bg); transition:background 0.12s;">
              <div style="flex:1; min-width:0;">
                <div style="font-size:13px; font-weight:500; color:var(--editor-text);">${this.escHtml(cat.name || cat.title || '\u2014')}</div>
                ${cat.description ? `<div style="font-size:11px; color:var(--editor-text-muted);">${this.escHtml(cat.description)}</div>` : ''}
              </div>
              <div style="text-align:right; flex-shrink:0;">
                <div style="font-size:18px; font-weight:700; color:var(--editor-primary);">${(cat.productCount || 0).toLocaleString()}</div>
                <div style="font-size:10px; color:var(--editor-text-muted);">products</div>
              </div>
              <div style="color:var(--editor-text-muted); font-size:12px;"><i class="far fa-chevron-right"></i></div>
            </div>`).join('')}
        </div>` : ''}
        <div style="padding: 0 16px 16px;">
          <div style="font-size:11px; font-weight:600; color:var(--editor-text-muted); text-transform:uppercase; letter-spacing:0.05em; margin-bottom:10px; padding-bottom:6px; border-bottom:1px solid var(--editor-border);">
            Quick Links
          </div>
          <div style="display:flex; flex-direction:column; gap:8px;">
            <a href="#" class="prod-dash-quick-link" data-section="products" style="display:flex; align-items:center; gap:10px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; background:var(--editor-panel-bg); text-decoration:none; color:var(--editor-text); transition:background 0.12s;">
              <i class="far fa-cubes" style="color:var(--editor-primary); width:16px; text-align:center;"></i>
              <span style="font-size:13px;">Browse All Products</span>
              <i class="far fa-chevron-right" style="margin-left:auto; color:var(--editor-text-muted); font-size:11px;"></i>
            </a>
            <a href="#" class="prod-dash-quick-link" data-section="pricing-rules" style="display:flex; align-items:center; gap:10px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; background:var(--editor-panel-bg); text-decoration:none; color:var(--editor-text); transition:background 0.12s;">
              <i class="far fa-percent" style="color:var(--editor-primary); width:16px; text-align:center;"></i>
              <span style="font-size:13px;">Pricing Rules &amp; Promo Codes</span>
              <i class="far fa-chevron-right" style="margin-left:auto; color:var(--editor-text-muted); font-size:11px;"></i>
            </a>
            <a href="#" class="prod-dash-quick-link" data-section="shipping-rates" style="display:flex; align-items:center; gap:10px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; background:var(--editor-panel-bg); text-decoration:none; color:var(--editor-text); transition:background 0.12s;">
              <i class="far fa-truck" style="color:var(--editor-primary); width:16px; text-align:center;"></i>
              <span style="font-size:13px;">Shipping Rates</span>
              <i class="far fa-chevron-right" style="margin-left:auto; color:var(--editor-text-muted); font-size:11px;"></i>
            </a>
          </div>
        </div>`;

      // Stat card click navigation
      document.getElementById('prod-dash-stat-categories')?.addEventListener('click', () => {
        this.loadListForSection('product-categories', '');
        this.activateNavItem(document.querySelector('.crm-nav-item[data-section="product-categories"]'));
      });
      document.getElementById('prod-dash-stat-products')?.addEventListener('click', () => {
        this.loadListForSection('products', '');
        this.activateNavItem(document.querySelector('.crm-nav-item[data-section="products"]'));
      });
      document.getElementById('prod-dash-stat-pricing')?.addEventListener('click', () => {
        this.loadListForSection('pricing-rules', '');
        this.activateNavItem(document.querySelector('.crm-nav-item[data-section="pricing-rules"]'));
      });
      document.getElementById('prod-dash-stat-shipping')?.addEventListener('click', () => {
        this.loadListForSection('shipping-rates', '');
        this.activateNavItem(document.querySelector('.crm-nav-item[data-section="shipping-rates"]'));
      });

      // Category card click — navigate to product-categories and select the item
      container.querySelectorAll('.crm-prod-dash-card').forEach(card => {
        card.addEventListener('mouseenter', () => { card.style.background = 'var(--editor-hover-bg)'; });
        card.addEventListener('mouseleave', () => { card.style.background = 'var(--editor-panel-bg)'; });
        card.addEventListener('click', () => {
          this.loadListForSection('product-categories', '');
          this.activateNavItem(document.querySelector('.crm-nav-item[data-section="product-categories"]'));
        });
      });

      // Quick link navigation
      container.querySelectorAll('.prod-dash-quick-link').forEach(link => {
        link.addEventListener('mouseenter', () => { link.style.background = 'var(--editor-hover-bg)'; });
        link.addEventListener('mouseleave', () => { link.style.background = 'var(--editor-panel-bg)'; });
        link.addEventListener('click', (e) => {
          e.preventDefault();
          const section = link.dataset.section;
          this.loadListForSection(section, '');
          this.activateNavItem(document.querySelector(`.crm-nav-item[data-section="${section}"]`));
        });
      });
    })
    .catch(() => {
      this.showLoading(false);
      container.innerHTML = '<div style="padding:20px; color:var(--editor-error, #c0392b); text-align:center;">Failed to load product catalog overview</div>';
    });
  }

  // -------------------------------------------------------------------------
  // Product Catalog - Product Categories
  // -------------------------------------------------------------------------

  loadProductCategories() {
    this.showPanel('product-categories');
    this.showLoading(true);

    const search = document.getElementById('crm-search-input')?.value || '';
    const params = new URLSearchParams({ page: this.currentPage, limit: 25 });
    if (search) params.append('search', search);

    fetch(`/json/ecommerce/product-categories?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderProductCategoriesList(data.categories || data.records || []);
      this.renderPagination('crm-product-categories-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadProductCategories();
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-product-categories-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load product categories</div>';
    });
  }

  renderProductCategoriesList(categories) {
    const container = document.getElementById('crm-product-categories-list');
    if (!container) return;

    if (!categories || categories.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No product categories found</div>';
      return;
    }

    container.innerHTML = categories.map(cat => {
      const name = cat.name || cat.title || cat.uniqueId || `Category #${cat.id}`;
      const count = cat.productCount != null ? `${cat.productCount} product${cat.productCount !== 1 ? 's' : ''}` : '';
      return `<div class="crm-record-item" data-id="${cat.id}" data-type="product-category">
        <div class="crm-record-title">${this.escHtml(name)}</div>
        <div class="crm-record-meta">
          ${count ? `<span>${this.escHtml(count)}</span>` : ''}
          ${cat.enabled === false ? '<span class="crm-record-badge crm-badge-dismissed">Disabled</span>' : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const cat = categories.find(c => c.id === id);
        if (cat) this.renderProductCategoryDetail(cat);
      });
    });
  }

  renderProductCategoryDetail(cat) {
    this.showDetailPanel('crm-product-category-detail');
    const titleEl = document.getElementById('crm-product-category-title');
    if (titleEl) titleEl.textContent = cat.name || cat.title || `Category #${cat.id}`;
    const editBtn = document.getElementById('btn-edit-product-category');
    if (editBtn) editBtn.href = `/admin/e-commerce/product-categories/modify?id=${cat.id}`;
    const infoEl = document.getElementById('crm-product-category-info');
    if (!infoEl) return;

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Name</span>
        <span class="crm-meta-value">${this.escHtml(cat.name || cat.title || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Unique ID</span>
        <span class="crm-meta-value">${this.escHtml(cat.uniqueId || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Products</span>
        <span class="crm-meta-value">${cat.productCount != null ? cat.productCount : '—'}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Status</span>
        <span class="crm-meta-value">${cat.enabled === false ? 'Disabled' : 'Active'}</span>
      </div>
    `;
  }

  // -------------------------------------------------------------------------
  // Product Catalog - Products
  // -------------------------------------------------------------------------

  loadProducts() {
    this.showPanel('products');
    this.showLoading(true);

    const search = document.getElementById('crm-search-input')?.value || '';
    const params = new URLSearchParams({ page: this.currentPage, limit: 25 });
    if (search) params.append('search', search);

    fetch(`/json/ecommerce/products?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderProductsList(data.products || data.records || []);
      this.renderPagination('crm-products-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadProducts();
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-products-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load products</div>';
    });
  }

  renderProductsList(products) {
    const container = document.getElementById('crm-products-list');
    if (!container) return;

    if (!products || products.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No products found</div>';
      return;
    }

    container.innerHTML = products.map(p => {
      const name = p.name || p.title || `Product #${p.id}`;
      const price = p.price != null ? `$${parseFloat(p.price).toFixed(2)}` : '';
      return `<div class="crm-record-item" data-id="${p.id}" data-type="product">
        <div class="crm-record-title">${this.escHtml(name)}</div>
        <div class="crm-record-meta">
          ${price ? `<span>${this.escHtml(price)}</span>` : ''}
          ${p.sku ? `<span>SKU: ${this.escHtml(p.sku)}</span>` : ''}
          ${p.enabled === false ? '<span class="crm-record-badge crm-badge-dismissed">Disabled</span>' : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const product = products.find(p => p.id === id);
        if (product) this.renderProductDetail(product);
      });
    });
  }

  renderProductDetail(product) {
    this.showDetailPanel('crm-product-detail');
    const titleEl = document.getElementById('crm-product-title');
    if (titleEl) titleEl.textContent = product.name || product.title || `Product #${product.id}`;
    const editBtn = document.getElementById('btn-edit-product');
    if (editBtn) editBtn.href = `/admin/e-commerce/products/modify?id=${product.id}`;
    const infoEl = document.getElementById('crm-product-info');
    if (!infoEl) return;

    const price = product.price != null ? `$${parseFloat(product.price).toFixed(2)}` : '—';

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Name</span>
        <span class="crm-meta-value">${this.escHtml(product.name || product.title || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">SKU</span>
        <span class="crm-meta-value ${!product.sku ? 'empty-value' : ''}">${this.escHtml(product.sku || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Price</span>
        <span class="crm-meta-value">${this.escHtml(price)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Status</span>
        <span class="crm-meta-value">${product.enabled === false ? 'Disabled' : 'Active'}</span>
      </div>
    `;
  }

  syncProducts() {
    this.showLoading(true);
    const formData = new FormData();
    formData.append('token', this.token);

    fetch('/json/ecommerce/products/sync', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      if (data.status === 'ok') {
        this.loadProducts();
      } else {
        alert('Sync error: ' + (data.error || 'Failed'));
      }
    })
    .catch(() => {
      this.showLoading(false);
      alert('An error occurred during sync. Please try again.');
    });
  }

  // -------------------------------------------------------------------------
  // Product Catalog - Pricing Rules & Promo Codes
  // -------------------------------------------------------------------------

  loadPricingRules() {
    this.showPanel('pricing-rules');
    this.showLoading(true);

    const params = new URLSearchParams({ page: this.currentPage, limit: 25 });

    fetch(`/json/ecommerce/pricing-rules?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderPricingRulesList(data.pricingRules || data.records || []);
      this.renderPagination('crm-pricing-rules-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadPricingRules();
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-pricing-rules-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load pricing rules</div>';
    });
  }

  renderPricingRulesList(rules) {
    const container = document.getElementById('crm-pricing-rules-list');
    if (!container) return;

    if (!rules || rules.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No pricing rules or promo codes found</div>';
      return;
    }

    container.innerHTML = rules.map(rule => {
      const name = rule.name || rule.promoCode || `Rule #${rule.id}`;
      const typeLabel = rule.promoCode ? 'Promo Code' : 'Pricing Rule';
      const discountText = rule.discountPercent ? `${rule.discountPercent}% off`
        : (rule.discountAmount ? `$${parseFloat(rule.discountAmount).toFixed(2)} off` : '');
      return `<div class="crm-record-item" data-id="${rule.id}" data-type="pricing-rule">
        <div class="crm-record-title">${this.escHtml(name)}</div>
        <div class="crm-record-meta">
          <span class="crm-record-badge crm-badge-claimed">${this.escHtml(typeLabel)}</span>
          ${discountText ? `<span>${this.escHtml(discountText)}</span>` : ''}
          ${rule.enabled === false ? '<span class="crm-record-badge crm-badge-dismissed">Disabled</span>' : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const rule = rules.find(r => r.id === id);
        if (rule) this.renderPricingRuleDetail(rule);
      });
    });
  }

  renderPricingRuleDetail(rule) {
    this.showDetailPanel('crm-pricing-rule-detail');
    const titleEl = document.getElementById('crm-pricing-rule-title');
    if (titleEl) titleEl.textContent = rule.name || rule.promoCode || `Rule #${rule.id}`;
    const editBtn = document.getElementById('btn-edit-pricing-rule');
    if (editBtn) editBtn.href = `/admin/e-commerce/pricing-rules/modify?id=${rule.id}`;
    const infoEl = document.getElementById('crm-pricing-rule-info');
    if (!infoEl) return;

    const discount = rule.discountPercent ? `${rule.discountPercent}% off`
      : (rule.discountAmount ? `$${parseFloat(rule.discountAmount).toFixed(2)} off` : '—');
    const startDate = rule.startDate ? new Date(rule.startDate).toLocaleDateString() : '—';
    const endDate = rule.endDate ? new Date(rule.endDate).toLocaleDateString() : '—';

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Name</span>
        <span class="crm-meta-value">${this.escHtml(rule.name || '—')}</span>
      </div>
      ${rule.promoCode ? `<div class="crm-meta-row">
        <span class="crm-meta-label">Promo Code</span>
        <span class="crm-meta-value">${this.escHtml(rule.promoCode)}</span>
      </div>` : ''}
      <div class="crm-meta-row">
        <span class="crm-meta-label">Discount</span>
        <span class="crm-meta-value">${this.escHtml(discount)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Start Date</span>
        <span class="crm-meta-value">${this.escHtml(startDate)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">End Date</span>
        <span class="crm-meta-value">${this.escHtml(endDate)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Status</span>
        <span class="crm-meta-value">${rule.enabled === false ? 'Disabled' : 'Active'}</span>
      </div>
    `;
  }

  // -------------------------------------------------------------------------
  // Product Catalog - Sales Tax Nexus
  // -------------------------------------------------------------------------

  loadSalesTaxNexus() {
    this.showPanel('sales-tax');
    this.showLoading(true);

    const params = new URLSearchParams({ page: this.currentPage, limit: 25 });

    fetch(`/json/ecommerce/tax-nexus?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderSalesTaxNexusList(data.taxNexus || data.records || []);
      this.renderPagination('crm-sales-tax-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadSalesTaxNexus();
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-sales-tax-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load sales tax nexus</div>';
    });
  }

  renderSalesTaxNexusList(nexusList) {
    const container = document.getElementById('crm-sales-tax-list');
    if (!container) return;

    if (!nexusList || nexusList.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No sales tax nexus records found</div>';
      return;
    }

    container.innerHTML = nexusList.map(nexus => {
      const label = nexus.state || nexus.region || nexus.country || `Nexus #${nexus.id}`;
      const rate = nexus.taxRate != null ? `${nexus.taxRate}%` : '';
      return `<div class="crm-record-item" data-id="${nexus.id}" data-type="sales-tax">
        <div class="crm-record-title">${this.escHtml(label)}</div>
        <div class="crm-record-meta">
          ${rate ? `<span>${this.escHtml(rate)}</span>` : ''}
          ${nexus.country ? `<span>${this.escHtml(nexus.country)}</span>` : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const nexus = nexusList.find(n => n.id === id);
        if (nexus) this.renderSalesTaxNexusDetail(nexus);
      });
    });
  }

  renderSalesTaxNexusDetail(nexus) {
    this.showDetailPanel('crm-sales-tax-detail');
    const label = nexus.state || nexus.region || nexus.country || `Nexus #${nexus.id}`;
    const titleEl = document.getElementById('crm-sales-tax-title');
    if (titleEl) titleEl.textContent = label;
    const editBtn = document.getElementById('btn-edit-sales-tax');
    if (editBtn) editBtn.href = `/admin/e-commerce/tax-nexus/modify?id=${nexus.id}`;
    const infoEl = document.getElementById('crm-sales-tax-info');
    if (!infoEl) return;

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">State/Region</span>
        <span class="crm-meta-value">${this.escHtml(nexus.state || nexus.region || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Country</span>
        <span class="crm-meta-value">${this.escHtml(nexus.country || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Tax Rate</span>
        <span class="crm-meta-value">${nexus.taxRate != null ? nexus.taxRate + '%' : '—'}</span>
      </div>
    `;
  }

  // -------------------------------------------------------------------------
  // Product Catalog - Shipping Rates
  // -------------------------------------------------------------------------

  loadShippingRates() {
    this.showPanel('shipping-rates');
    this.showLoading(true);

    const params = new URLSearchParams({ page: this.currentPage, limit: 25 });

    fetch(`/json/ecommerce/shipping-rates?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderShippingRatesList(data.shippingRates || data.records || []);
      this.renderPagination('crm-shipping-rates-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadShippingRates();
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-shipping-rates-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load shipping rates</div>';
    });
  }

  renderShippingRatesList(rates) {
    const container = document.getElementById('crm-shipping-rates-list');
    if (!container) return;

    if (!rates || rates.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No shipping rates found</div>';
      return;
    }

    container.innerHTML = rates.map(rate => {
      const name = rate.name || rate.title || `Rate #${rate.id}`;
      const price = rate.price != null ? `$${parseFloat(rate.price).toFixed(2)}` : '';
      return `<div class="crm-record-item" data-id="${rate.id}" data-type="shipping-rate">
        <div class="crm-record-title">${this.escHtml(name)}</div>
        <div class="crm-record-meta">
          ${price ? `<span>${this.escHtml(price)}</span>` : ''}
          ${rate.enabled === false ? '<span class="crm-record-badge crm-badge-dismissed">Disabled</span>' : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const rate = rates.find(r => r.id === id);
        if (rate) this.renderShippingRateDetail(rate);
      });
    });
  }

  renderShippingRateDetail(rate) {
    this.showDetailPanel('crm-shipping-rate-detail');
    const titleEl = document.getElementById('crm-shipping-rate-title');
    if (titleEl) titleEl.textContent = rate.name || rate.title || `Rate #${rate.id}`;
    const editBtn = document.getElementById('btn-edit-shipping-rate');
    if (editBtn) editBtn.href = `/admin/e-commerce/shipping-rates/modify?id=${rate.id}`;
    const infoEl = document.getElementById('crm-shipping-rate-info');
    if (!infoEl) return;

    const price = rate.price != null ? `$${parseFloat(rate.price).toFixed(2)}` : '—';
    const minOrderAmount = rate.minOrderAmount != null ? `$${parseFloat(rate.minOrderAmount).toFixed(2)}` : '—';
    const maxOrderAmount = rate.maxOrderAmount != null ? `$${parseFloat(rate.maxOrderAmount).toFixed(2)}` : '—';

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Name</span>
        <span class="crm-meta-value">${this.escHtml(rate.name || rate.title || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Price</span>
        <span class="crm-meta-value">${this.escHtml(price)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Min. Order</span>
        <span class="crm-meta-value">${this.escHtml(minOrderAmount)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Max. Order</span>
        <span class="crm-meta-value">${this.escHtml(maxOrderAmount)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Status</span>
        <span class="crm-meta-value">${rate.enabled === false ? 'Disabled' : 'Active'}</span>
      </div>
    `;
  }

  // -------------------------------------------------------------------------
  // Users
  // -------------------------------------------------------------------------

  showUsersDashboard() {
    const container = document.getElementById('crm-users-dashboard-content');
    if (!container) return;

    container.innerHTML = '<div style="padding:20px; text-align:center; color:var(--editor-text-muted);"><i class="far fa-spinner fa-spin"></i> Loading...</div>';
    this.showLoading(true);

    Promise.all([
      fetch(`/json/crm/users?page=1&limit=1`, { headers: { 'X-CSRF-Token': this.token } }).then(r => r.json()),
      fetch('/json/crm/user-groups', { headers: { 'X-CSRF-Token': this.token } }).then(r => r.json()),
      fetch('/json/crm/user-roles', { headers: { 'X-CSRF-Token': this.token } }).then(r => r.json())
    ])
    .then(([usersData, groupsData, rolesData]) => {
      this.showLoading(false);
      const totalUsers = usersData.total || 0;
      const totalGroups = (groupsData.groups || []).length;
      const totalRoles = (rolesData.roles || []).length;
      const recentUsers = (usersData.users || []);

      container.innerHTML = `
        <div class="crm-stats-row">
          <div class="crm-stat-card" style="cursor:pointer;" id="users-dash-stat-users">
            <div class="crm-stat-icon"><i class="far fa-users"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">${totalUsers.toLocaleString()}</div>
              <div class="crm-stat-label">Total Users</div>
            </div>
          </div>
          <div class="crm-stat-card" style="cursor:pointer;" id="users-dash-stat-groups">
            <div class="crm-stat-icon"><i class="far fa-layer-group"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">${totalGroups}</div>
              <div class="crm-stat-label">User Groups</div>
            </div>
          </div>
          <div class="crm-stat-card" style="cursor:pointer;" id="users-dash-stat-roles">
            <div class="crm-stat-icon"><i class="far fa-shield-alt"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value">${totalRoles}</div>
              <div class="crm-stat-label">User Roles</div>
            </div>
          </div>
        </div>
        <div style="padding: 0 16px 16px;">
          <div style="font-size:11px; font-weight:600; color:var(--editor-text-muted); text-transform:uppercase; letter-spacing:0.05em; margin-bottom:10px; padding-bottom:6px; border-bottom:1px solid var(--editor-border);">
            Quick Links
          </div>
          <div style="display:flex; flex-direction:column; gap:8px;">
            <a href="#" class="users-dash-quick-link" data-section="users" style="display:flex; align-items:center; gap:10px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; background:var(--editor-panel-bg); text-decoration:none; color:var(--editor-text); transition:background 0.12s;">
              <i class="far fa-users" style="color:var(--editor-primary); width:16px; text-align:center;"></i>
              <span style="font-size:13px;">Browse All Users</span>
              <i class="far fa-chevron-right" style="margin-left:auto; color:var(--editor-text-muted); font-size:11px;"></i>
            </a>
            <a href="#" class="users-dash-quick-link" data-section="user-groups" style="display:flex; align-items:center; gap:10px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; background:var(--editor-panel-bg); text-decoration:none; color:var(--editor-text); transition:background 0.12s;">
              <i class="far fa-layer-group" style="color:var(--editor-primary); width:16px; text-align:center;"></i>
              <span style="font-size:13px;">Manage User Groups</span>
              <i class="far fa-chevron-right" style="margin-left:auto; color:var(--editor-text-muted); font-size:11px;"></i>
            </a>
            <a href="#" class="users-dash-quick-link" data-section="user-roles" style="display:flex; align-items:center; gap:10px; padding:10px 12px; border:1px solid var(--editor-border); border-radius:6px; background:var(--editor-panel-bg); text-decoration:none; color:var(--editor-text); transition:background 0.12s;">
              <i class="far fa-shield-alt" style="color:var(--editor-primary); width:16px; text-align:center;"></i>
              <span style="font-size:13px;">Manage User Roles</span>
              <i class="far fa-chevron-right" style="margin-left:auto; color:var(--editor-text-muted); font-size:11px;"></i>
            </a>
          </div>
        </div>`;

      // Stat card click navigation
      document.getElementById('users-dash-stat-users')?.addEventListener('click', () => {
        this.loadListForSection('users', '');
        const navItem = document.querySelector('.crm-nav-item[data-section="users"]');
        this.activateNavItem(navItem);
      });
      document.getElementById('users-dash-stat-groups')?.addEventListener('click', () => {
        this.loadListForSection('user-groups', '');
        const navItem = document.querySelector('.crm-nav-item[data-section="user-groups"]');
        this.activateNavItem(navItem);
      });
      document.getElementById('users-dash-stat-roles')?.addEventListener('click', () => {
        this.loadListForSection('user-roles', '');
        const navItem = document.querySelector('.crm-nav-item[data-section="user-roles"]');
        this.activateNavItem(navItem);
      });

      // Quick link navigation
      container.querySelectorAll('.users-dash-quick-link').forEach(link => {
        link.addEventListener('mouseenter', () => { link.style.background = 'var(--editor-hover-bg)'; });
        link.addEventListener('mouseleave', () => { link.style.background = 'var(--editor-panel-bg)'; });
        link.addEventListener('click', (e) => {
          e.preventDefault();
          const section = link.dataset.section;
          this.loadListForSection(section, '');
          const navItem = document.querySelector(`.crm-nav-item[data-section="${section}"]`);
          this.activateNavItem(navItem);
        });
      });
    })
    .catch(() => {
      this.showLoading(false);
      container.innerHTML = '<div style="padding:20px; color:var(--editor-error, #c0392b); text-align:center;">Failed to load users dashboard</div>';
    });
  }

  loadUsers() {
    this.showPanel('users');
    this.showLoading(true);

    const inlineSearch = document.getElementById('crm-users-search-input')?.value.trim() || '';
    const toolbarSearch = document.getElementById('crm-search-input')?.value.trim() || '';
    const search = inlineSearch || toolbarSearch;
    const params = new URLSearchParams({ page: this.currentPage, limit: 25 });
    if (search) params.append('search', search);

    fetch(`/json/crm/users?${params}`, {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderUsersList(data.users || []);
      this.renderPagination('crm-users-pagination', data.page, data.limit, data.total, (page) => {
        this.currentPage = page;
        this.loadUsers();
      });
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-users-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load users</div>';
    });
  }

  renderUsersList(users) {
    const container = document.getElementById('crm-users-list');
    if (!container) return;

    if (!users || users.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No users found</div>';
      return;
    }

    container.innerHTML = users.map(u => {
      const name = [u.firstName, u.lastName].filter(Boolean).join(' ') || u.email || u.username || `User #${u.id}`;
      const statusBadge = !u.enabled
        ? '<span class="crm-record-badge crm-badge-dismissed">Disabled</span>'
        : (!u.validated ? '<span class="crm-record-badge crm-badge-claimed">Unverified</span>' : '');
      return `<div class="crm-record-item" data-id="${u.id}" data-type="user">
        <div class="crm-record-title">${this.escHtml(name)}</div>
        <div class="crm-record-meta">
          ${statusBadge}
          <span>${this.escHtml(u.email || '')}</span>
          ${u.organization ? `<span>${this.escHtml(u.organization)}</span>` : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const user = users.find(u => u.id === id);
        if (user) this.renderUserDetail(user);
      });
    });
  }

  renderUserDetail(user) {
    this.showDetailPanel('crm-user-detail');

    const titleEl = document.getElementById('crm-user-name-title');
    if (titleEl) {
      const name = [user.firstName, user.lastName].filter(Boolean).join(' ') || user.email || user.username || `User #${user.id}`;
      titleEl.textContent = name;
    }

    const infoEl = document.getElementById('crm-user-info');
    if (!infoEl) return;

    const created = user.created ? new Date(user.created).toLocaleDateString() : '—';
    const statusText = !user.enabled ? 'Disabled' : (!user.validated ? 'Unverified' : 'Active');

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Email</span>
        <span class="crm-meta-value">${this.escHtml(user.email || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Username</span>
        <span class="crm-meta-value ${!user.username ? 'empty-value' : ''}">${this.escHtml(user.username || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Title</span>
        <span class="crm-meta-value ${!user.title ? 'empty-value' : ''}">${this.escHtml(user.title || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Organization</span>
        <span class="crm-meta-value ${!user.organization ? 'empty-value' : ''}">${this.escHtml(user.organization || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Location</span>
        <span class="crm-meta-value ${(!user.city && !user.state) ? 'empty-value' : ''}">${this.escHtml([user.city, user.state].filter(Boolean).join(', ') || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Status</span>
        <span class="crm-meta-value">${this.escHtml(statusText)}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Member Since</span>
        <span class="crm-meta-value">${this.escHtml(created)}</span>
      </div>
    `;
  }

  // -------------------------------------------------------------------------
  // User Groups
  // -------------------------------------------------------------------------

  loadUserGroups() {
    this.showPanel('user-groups');
    this.showLoading(true);

    fetch('/json/crm/user-groups', {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderUserGroupsList(data.groups || []);
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-user-groups-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load user groups</div>';
    });
  }

  renderUserGroupsList(groups) {
    const container = document.getElementById('crm-user-groups-list');
    if (!container) return;

    if (!groups || groups.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No user groups found</div>';
      return;
    }

    container.innerHTML = groups.map(g => {
      const memberText = `${g.userCount} member${g.userCount !== 1 ? 's' : ''}`;
      return `<div class="crm-record-item" data-id="${g.id}" data-type="user-group">
        <div class="crm-record-title">${this.escHtml(g.name)}</div>
        <div class="crm-record-meta">
          <span>${this.escHtml(memberText)}</span>
          ${g.uniqueId ? `<span>${this.escHtml(g.uniqueId)}</span>` : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const group = groups.find(g => g.id === id);
        if (group) this.renderUserGroupDetail(group);
      });
    });
  }

  renderUserGroupDetail(group) {
    this.showDetailPanel('crm-user-group-detail');

    const titleEl = document.getElementById('crm-user-group-title');
    if (titleEl) titleEl.textContent = group.name;

    // Wire up the edit button to pre-populate and open modal
    const editBtn = document.getElementById('btn-edit-user-group');
    if (editBtn) {
      editBtn.onclick = () => {
        this.resetModal('crm-modal-user-group');
        document.getElementById('crm-modal-user-group-title').textContent = 'Edit User Group';
        document.getElementById('ug-id').value = group.id;
        document.getElementById('ug-name').value = group.name || '';
        document.getElementById('ug-unique-id').value = group.uniqueId || '';
        document.getElementById('ug-description').value = group.description || '';
        this.openModal('crm-modal-user-group');
      };
    }

    const infoEl = document.getElementById('crm-user-group-info');
    if (!infoEl) return;

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Name</span>
        <span class="crm-meta-value">${this.escHtml(group.name || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Unique ID</span>
        <span class="crm-meta-value ${!group.uniqueId ? 'empty-value' : ''}">${this.escHtml(group.uniqueId || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Description</span>
        <span class="crm-meta-value ${!group.description ? 'empty-value' : ''}">${this.escHtml(group.description || '(none)')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Members</span>
        <span class="crm-meta-value">${group.userCount}</span>
      </div>
      ${group.oAuthPath ? `<div class="crm-meta-row">
        <span class="crm-meta-label">OAuth Path</span>
        <span class="crm-meta-value">${this.escHtml(group.oAuthPath)}</span>
      </div>` : ''}
    `;
  }

  saveUserGroup() {
    const name = document.getElementById('ug-name')?.value.trim();
    if (!name) { this.showModalError('crm-modal-user-group', 'Group name is required.'); return; }

    const id = document.getElementById('ug-id')?.value.trim() || '';
    const formData = new FormData();
    formData.append('token', this.token);
    if (id) formData.append('id', id);
    formData.append('name', name);
    formData.append('uniqueId', document.getElementById('ug-unique-id')?.value.trim() || '');
    formData.append('description', document.getElementById('ug-description')?.value.trim() || '');

    fetch('/json/crm/user-groups/save', {
      method: 'POST',
      headers: { 'X-CSRF-Token': this.token },
      body: formData
    })
    .then(r => r.json())
    .then(data => {
      if (data.status === 'ok') {
        this.closeModal('crm-modal-user-group');
        this.loadUserGroups();
        this.clearDetailPanel();
      } else {
        this.showModalError('crm-modal-user-group', data.error || 'Failed to save user group.');
      }
    })
    .catch(() => this.showModalError('crm-modal-user-group', 'An error occurred. Please try again.'));
  }

  // -------------------------------------------------------------------------
  // User Roles (read-only)
  // -------------------------------------------------------------------------

  loadUserRoles() {
    this.showPanel('user-roles');
    this.showLoading(true);

    fetch('/json/crm/user-roles', {
      headers: { 'X-CSRF-Token': this.token }
    })
    .then(r => r.json())
    .then(data => {
      this.showLoading(false);
      this.renderUserRolesList(data.roles || []);
    })
    .catch(() => {
      this.showLoading(false);
      document.getElementById('crm-user-roles-list').innerHTML =
        '<div style="padding:20px; color: var(--editor-error, #c0392b);">Failed to load user roles</div>';
    });
  }

  renderUserRolesList(roles) {
    const container = document.getElementById('crm-user-roles-list');
    if (!container) return;

    if (!roles || roles.length === 0) {
      container.innerHTML = '<div style="padding:20px; color: var(--editor-text-muted); text-align:center;">No roles found</div>';
      return;
    }

    container.innerHTML = roles.map(r => {
      return `<div class="crm-record-item" data-id="${r.id}" data-type="user-role">
        <div class="crm-record-title">${this.escHtml(r.title || r.code || `Role #${r.id}`)}</div>
        <div class="crm-record-meta">
          <span class="crm-record-badge crm-badge-claimed">${this.escHtml(r.code || '')}</span>
          ${r.level >= 0 ? `<span>Level ${r.level}</span>` : ''}
        </div>
      </div>`;
    }).join('');

    container.querySelectorAll('.crm-record-item').forEach(item => {
      item.addEventListener('click', () => {
        container.querySelectorAll('.crm-record-item').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
        const id = parseInt(item.dataset.id, 10);
        const role = roles.find(r => r.id === id);
        if (role) this.renderUserRoleDetail(role);
      });
    });
  }

  renderUserRoleDetail(role) {
    this.showDetailPanel('crm-user-role-detail');

    const titleEl = document.getElementById('crm-user-role-title');
    if (titleEl) titleEl.textContent = role.title || role.code || `Role #${role.id}`;

    const infoEl = document.getElementById('crm-user-role-info');
    if (!infoEl) return;

    infoEl.innerHTML = `
      <div class="crm-meta-row">
        <span class="crm-meta-label">Title</span>
        <span class="crm-meta-value">${this.escHtml(role.title || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Code</span>
        <span class="crm-meta-value">${this.escHtml(role.code || '—')}</span>
      </div>
      <div class="crm-meta-row">
        <span class="crm-meta-label">Level</span>
        <span class="crm-meta-value">${role.level >= 0 ? role.level : '—'}</span>
      </div>
      <p style="margin-top: 16px; font-size: 12px; color: var(--editor-text-muted);">Roles are system-defined and read-only.</p>
    `;
  }

  // -------------------------------------------------------------------------
  // Search
  // -------------------------------------------------------------------------

  handleSearch() {
    this.currentPage = 1;
    if (this.currentSection === 'customers') {
      this.loadCustomers();
    } else if (this.currentSection === 'mailinglist') {
      this.loadMailingListMembers(this.currentSelectedId);
    } else if (this.currentSection === 'forms') {
      this.loadFormSubmissions(this.currentSelectedId);
    } else if (this.currentSection === 'products') {
      this.loadProducts();
    } else if (this.currentSection === 'product-categories') {
      this.loadProductCategories();
    } else if (this.currentSection === 'pricing-rules') {
      this.loadPricingRules();
    } else if (this.currentSection === 'sales-tax') {
      this.loadSalesTaxNexus();
    } else if (this.currentSection === 'shipping-rates') {
      this.loadShippingRates();
    } else if (this.currentSection === 'users') {
      this.loadUsers();
    } else if (this.currentSection === 'user-groups') {
      this.loadUserGroups();
    }
  }

  // -------------------------------------------------------------------------
  // Utilities
  // -------------------------------------------------------------------------

  showLoading(show) {
    const indicator = document.getElementById('loading-indicator');
    if (indicator) indicator.style.display = show ? 'block' : 'none';
  }

  showDetailPanel(id) {
    document.querySelectorAll('.crm-detail-content').forEach(p => p.style.display = 'none');
    const panel = document.getElementById(id);
    if (panel) panel.style.display = 'flex';
  }

  clearDetailPanel() {
    document.querySelectorAll('.crm-detail-content').forEach(p => p.style.display = 'none');
    const empty = document.getElementById('crm-detail-empty');
    if (empty) empty.style.display = 'flex';
  }

  renderPagination(containerId, page, limit, total, onPageChange) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!total || total <= limit) {
      container.innerHTML = '';
      return;
    }

    const totalPages = Math.ceil(total / limit);
    let html = '';

    if (page > 1) {
      html += `<button class="button small secondary radius" data-page="${page - 1}">&#8592; Prev</button>`;
    }
    html += `<span>Page ${page} of ${totalPages}</span>`;
    if (page < totalPages) {
      html += `<button class="button small secondary radius" data-page="${page + 1}">Next &#8594;</button>`;
    }

    container.innerHTML = html;
    container.querySelectorAll('button[data-page]').forEach(btn => {
      btn.addEventListener('click', () => {
        const p = parseInt(btn.dataset.page, 10);
        if (onPageChange) onPageChange(p);
      });
    });
  }

  escHtml(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }
}
