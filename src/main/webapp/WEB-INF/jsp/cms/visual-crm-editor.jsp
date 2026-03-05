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
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<c:set var="section" value="${section}" scope="request"/>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
</g:compress>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-crm-editor.css" />
</g:compress>
<div id="visual-crm-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <c:set var="editorName" value="CRM" />
      <c:set var="activeApp" value="crm" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>

    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="reload-btn" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-sync"></i> Reload</button>
    </div>

    <!-- Center Section: Search -->
    <div class="toolbar-section center">
      <div id="crm-search-container" style="display: flex; gap: 8px; align-items: center;">
        <input type="text" id="crm-search-input" class="property-input" placeholder="Search..." style="width: 260px; padding: 6px 10px; font-size: 14px;" />
        <button id="crm-search-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-search"></i></button>
      </div>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
    </div>

    <div class="titlebar-right">
      <a href="#" id="dark-mode-toggle-menu" class="titlebar-btn apps-btn" title="Toggle Dark Mode">
        <i class="${font:far()} fa-moon"></i>
      </a>
      <c:choose>
        <c:when test="${!empty returnPage}">
          <a href="${returnPage}" class="titlebar-btn apps-btn confirm-exit" title="Exit back to site">
            <i class="${font:far()} fa-arrow-right-from-bracket"></i>
          </a>
        </c:when>
        <c:otherwise>
          <a href="${ctx}/" class="titlebar-btn apps-btn confirm-exit" title="Exit back to site">
            <i class="${font:far()} fa-arrow-right-from-bracket"></i>
          </a>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <!-- Main CRM Container - 3 Panel Layout -->
  <div id="visual-crm-editor-container">

    <!-- Left Panel: Section Navigation -->
    <div id="crm-nav-panel">
      <div class="crm-nav-header">
        <span>CRM Sections</span>
      </div>

      <!-- Forms Section -->
      <div class="crm-nav-section">
        <div class="crm-nav-section-title">
          <i class="${font:far()} fa-file-alt"></i> Forms
        </div>
        <div class="crm-nav-items">
          <a href="javascript:void(0)" class="crm-nav-item" data-section="forms-dashboard" data-id="">
            <i class="${font:far()} fa-tachometer-alt"></i> Dashboard
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="forms-index" data-id="">
            <i class="${font:far()} fa-list"></i> Forms
            <span id="forms-total-badge" class="crm-nav-badge" style="display:none;"></span>
          </a>
        </div>
      </div>

      <!-- Mailing Lists Section -->
      <div class="crm-nav-section">
        <div class="crm-nav-section-title">
          <i class="${font:far()} fa-envelope"></i> Mailing Lists
        </div>
        <div class="crm-nav-items">
          <a href="javascript:void(0)" class="crm-nav-item" data-section="mailing-dashboard" data-id="">
            <i class="${font:far()} fa-tachometer-alt"></i> Dashboard
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="mailing-lists" data-id="">
            <i class="${font:far()} fa-list"></i> Lists
            <span id="mailing-lists-total-badge" class="crm-nav-badge" style="display:none;"></span>
          </a>
        </div>
      </div>

      <!-- Customers Section -->
      <div class="crm-nav-section">
        <div class="crm-nav-section-title">
          <i class="${font:far()} fa-users"></i> Customers &amp; Orders
        </div>
        <div class="crm-nav-items">
          <a href="javascript:void(0)" class="crm-nav-item" data-section="customers" data-id="">
            <i class="${font:far()} fa-user"></i> All Customers
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="orders" data-id="">
            <i class="${font:far()} fa-shopping-bag"></i> All Orders
          </a>
        </div>
      </div>

      <!-- Product Catalog Section -->
      <div class="crm-nav-section">
        <div class="crm-nav-section-title">
          <i class="${font:far()} fa-box-open"></i> Product Catalog
        </div>
        <div class="crm-nav-items">
          <a href="javascript:void(0)" class="crm-nav-item" data-section="product-catalog-dashboard" data-id="">
            <i class="${font:far()} fa-tachometer-alt"></i> Dashboard
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="product-categories" data-id="">
            <i class="${font:far()} fa-tags"></i> Product Categories
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="products" data-id="">
            <i class="${font:far()} fa-cubes"></i> Products
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="pricing-rules" data-id="">
            <i class="${font:far()} fa-percent"></i> Pricing Rules &amp; Promo Codes
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="sales-tax" data-id="">
            <i class="${font:far()} fa-landmark"></i> Sales Tax Nexus
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="shipping-rates" data-id="">
            <i class="${font:far()} fa-truck"></i> Shipping Rates
          </a>
        </div>
      </div>

      <!-- Users Section -->
      <div class="crm-nav-section">
        <div class="crm-nav-section-title">
          <i class="${font:far()} fa-user-circle"></i> Users
        </div>
        <div class="crm-nav-items">
          <a href="javascript:void(0)" class="crm-nav-item" data-section="users-dashboard" data-id="">
            <i class="${font:far()} fa-tachometer-alt"></i> Dashboard
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="users" data-id="">
            <i class="${font:far()} fa-users"></i> Users
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="user-groups" data-id="">
            <i class="${font:far()} fa-layer-group"></i> User Groups
          </a>
          <a href="javascript:void(0)" class="crm-nav-item" data-section="user-roles" data-id="">
            <i class="${font:far()} fa-shield-alt"></i> User Roles
          </a>
        </div>
      </div>
    </div>

    <!-- Center Panel: List / Dashboard -->
    <div id="crm-list-panel">
      <!-- Dashboard: Analytics trend for forms -->
      <div id="crm-dashboard" class="crm-panel-content">
        <div class="crm-dashboard-header">
          <h5><i class="${font:far()} fa-tachometer-alt"></i> CRM Overview</h5>
        </div>
        <div class="crm-stats-row" id="crm-overview-stats">
          <div class="crm-stat-card" id="stat-forms">
            <div class="crm-stat-icon"><i class="${font:far()} fa-file-alt"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value" id="stat-forms-value">-</div>
              <div class="crm-stat-label">Form Categories</div>
            </div>
          </div>
          <div class="crm-stat-card" id="stat-new-submissions">
            <div class="crm-stat-icon"><i class="${font:far()} fa-bell"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value" id="stat-new-value">-</div>
              <div class="crm-stat-label">New Submissions</div>
            </div>
          </div>
          <div class="crm-stat-card" id="stat-mailing-lists">
            <div class="crm-stat-icon"><i class="${font:far()} fa-envelope"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value" id="stat-lists-value">-</div>
              <div class="crm-stat-label">Mailing List Members</div>
            </div>
          </div>
          <div class="crm-stat-card" id="stat-customers">
            <div class="crm-stat-icon"><i class="${font:far()} fa-users"></i></div>
            <div class="crm-stat-info">
              <div class="crm-stat-value" id="stat-customers-value">-</div>
              <div class="crm-stat-label">Customers</div>
            </div>
          </div>
        </div>
        <p style="text-align: center; color: var(--editor-text-muted); margin-top: 20px; font-size: 14px;">
          Select a category from the left panel to get started
        </p>
      </div>

      <!-- Forms Dashboard -->
      <div id="crm-forms-dashboard" class="crm-panel-content" style="display: none;">
        <div class="crm-dashboard-header">
          <h5><i class="${font:far()} fa-file-alt"></i> Forms Overview</h5>
        </div>
        <div id="crm-forms-dashboard-content">
          <!-- Populated by JS -->
        </div>
      </div>

      <!-- Forms Index (list of form categories) -->
      <div id="crm-forms-index-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-list"></i> Forms</h5>
        </div>
        <div id="crm-forms-index-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
      </div>

      <!-- Mailing Lists Dashboard -->
      <div id="crm-mailing-dashboard" class="crm-panel-content" style="display: none;">
        <div class="crm-dashboard-header">
          <h5><i class="${font:far()} fa-envelope"></i> Mailing Lists Overview</h5>
        </div>
        <div id="crm-mailing-dashboard-content">
          <!-- Populated by JS -->
        </div>
      </div>

      <!-- Mailing Lists Index -->
      <div id="crm-mailing-lists-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-envelope"></i> Mailing Lists</h5>
          <div class="crm-list-actions">
            <button id="btn-add-mailing-list" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add Mailing List</button>
          </div>
        </div>
        <div id="crm-mailing-lists-index" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
      </div>

      <!-- Form Submissions List -->
      <div id="crm-submissions-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5 id="crm-submissions-title"><i class="${font:far()} fa-file-alt"></i> Submissions</h5>
          <div class="crm-list-actions">
            <button id="btn-back-to-forms" class="button tiny secondary radius"><i class="${font:far()} fa-arrow-left"></i> Forms</button>
          </div>
        </div>
        <div class="crm-list-filters">
            <button class="button tiny radius crm-filter-btn active" data-filter="new">New</button>
            <button class="button tiny radius crm-filter-btn" data-filter="claimed">Claimed</button>
            <button class="button tiny radius crm-filter-btn" data-filter="processed">Processed</button>
            <button class="button tiny radius crm-filter-btn" data-filter="dismissed">Dismissed</button>
            <button class="button tiny radius crm-filter-btn" data-filter="">All</button>
        </div>
        <div id="crm-submissions-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-submissions-pagination" class="crm-pagination"></div>
      </div>

      <!-- Mailing List Members -->
      <div id="crm-members-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5 id="crm-members-title"><i class="${font:far()} fa-envelope"></i> Members</h5>
          <div class="crm-list-actions">
            <button id="btn-back-to-mailing-lists" class="button tiny secondary radius"><i class="${font:far()} fa-arrow-left"></i> Lists</button>
          </div>
        </div>
        <div id="crm-members-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-members-pagination" class="crm-pagination"></div>
      </div>

      <!-- Customers List -->
      <div id="crm-customers-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-users"></i> Customers</h5>
          <div class="crm-list-actions">
            <button id="btn-add-customer" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add Customer</button>
          </div>
        </div>
        <div id="crm-customers-search" style="padding: 8px 12px; border-bottom: 1px solid var(--editor-border, #e0e0e0); display: flex; gap: 8px;">
          <input type="text" id="crm-customers-search-input" class="property-input" placeholder="Search customers..." style="flex: 1; padding: 5px 8px; font-size: 13px;" />
          <button id="crm-customers-search-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-search"></i></button>
        </div>
        <div id="crm-customers-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-customers-pagination" class="crm-pagination"></div>
      </div>

      <!-- Orders List -->
      <div id="crm-orders-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5 id="crm-orders-title"><i class="${font:far()} fa-shopping-bag"></i> Orders</h5>
          <div class="crm-list-actions">
            <a href="${ctx}/admin/e-commerce/orders/add" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add Order</a>
          </div>
        </div>
        <div id="crm-orders-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-orders-pagination" class="crm-pagination"></div>
      </div>

      <!-- Product Catalog Dashboard -->
      <div id="crm-product-catalog-dashboard" class="crm-panel-content" style="display: none;">
        <div class="crm-dashboard-header">
          <h5><i class="${font:far()} fa-box-open"></i> Product Catalog Overview</h5>
        </div>
        <div id="crm-product-catalog-dashboard-content">
          <!-- Populated by JS -->
        </div>
      </div>

      <!-- Product Categories List -->
      <div id="crm-product-categories-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-tags"></i> Product Categories</h5>
          <div class="crm-list-actions">
            <button id="btn-add-product-category" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add</button>
          </div>
        </div>
        <div id="crm-product-categories-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-product-categories-pagination" class="crm-pagination"></div>
      </div>

      <!-- Products List -->
      <div id="crm-products-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-cubes"></i> Products</h5>
          <div class="crm-list-actions">
            <button id="btn-add-product" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add</button>
            <button id="btn-export-products" class="button tiny secondary radius"><i class="${font:far()} fa-file-csv"></i> Export CSV</button>
            <button id="btn-sync-products" class="button tiny secondary radius"><i class="${font:far()} fa-sync"></i> Sync Products</button>
          </div>
        </div>
        <div id="crm-products-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-products-pagination" class="crm-pagination"></div>
      </div>

      <!-- Pricing Rules & Promo Codes List -->
      <div id="crm-pricing-rules-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-percent"></i> Pricing Rules &amp; Promo Codes</h5>
          <div class="crm-list-actions">
            <button id="btn-add-pricing-rule" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add</button>
          </div>
        </div>
        <div id="crm-pricing-rules-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-pricing-rules-pagination" class="crm-pagination"></div>
      </div>

      <!-- Sales Tax Nexus List -->
      <div id="crm-sales-tax-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-landmark"></i> Sales Tax Nexus</h5>
          <div class="crm-list-actions">
            <button id="btn-add-sales-tax" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add</button>
          </div>
        </div>
        <div id="crm-sales-tax-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-sales-tax-pagination" class="crm-pagination"></div>
      </div>

      <!-- Shipping Rates List -->
      <div id="crm-shipping-rates-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-truck"></i> Shipping Rates</h5>
          <div class="crm-list-actions">
            <button id="btn-add-shipping-rate" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add</button>
          </div>
        </div>
        <div id="crm-shipping-rates-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-shipping-rates-pagination" class="crm-pagination"></div>
      </div>

      <!-- Users Dashboard -->
      <div id="crm-users-dashboard" class="crm-panel-content" style="display: none;">
        <div class="crm-dashboard-header">
          <h5><i class="${font:far()} fa-tachometer-alt"></i> Users Overview</h5>
        </div>
        <div id="crm-users-dashboard-content">
          <!-- Populated by JS -->
        </div>
      </div>

      <!-- Users List -->
      <div id="crm-users-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-users"></i> Users</h5>
        </div>
        <div id="crm-users-search" style="padding: 8px 12px; border-bottom: 1px solid var(--editor-border, #e0e0e0); display: flex; gap: 8px;">
          <input type="text" id="crm-users-search-input" class="property-input" placeholder="Search users by name or email..." style="flex: 1; padding: 5px 8px; font-size: 13px;" />
          <button id="crm-users-search-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-search"></i></button>
        </div>
        <div id="crm-users-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
        <div id="crm-users-pagination" class="crm-pagination"></div>
      </div>

      <!-- User Groups List -->
      <div id="crm-user-groups-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-layer-group"></i> User Groups</h5>
          <div class="crm-list-actions">
            <button id="btn-add-user-group" class="button tiny success radius"><i class="${font:far()} fa-plus"></i> Add</button>
          </div>
        </div>
        <div id="crm-user-groups-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
      </div>

      <!-- User Roles List -->
      <div id="crm-user-roles-panel" class="crm-panel-content" style="display: none;">
        <div class="crm-list-header">
          <h5><i class="${font:far()} fa-shield-alt"></i> User Roles</h5>
        </div>
        <div id="crm-user-roles-list" class="crm-record-list">
          <!-- Populated by JS -->
        </div>
      </div>
    </div>

    <!-- Right Panel: Detail View -->
    <div id="crm-detail-panel">
      <div id="crm-properties-panel-resize-handle"></div>

      <!-- Empty state -->
      <div id="crm-detail-empty" class="crm-detail-content">
        <div style="text-align: center; padding: 40px 20px; color: var(--editor-text-muted);">
          <i class="${font:far()} fa-hand-pointer fa-3x" style="opacity: 0.3; margin-bottom: 15px;"></i>
          <p>Select a record to view details</p>
        </div>
      </div>

      <!-- Submission Detail -->
      <div id="crm-submission-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-detail-submission-title">Form Submission</h6>
          <div class="crm-detail-actions">
            <button id="btn-claim-submission" class="button tiny success radius" title="Claim this submission">
              <i class="${font:far()} fa-hand-paper"></i> Claim
            </button>
            <button id="btn-process-submission" class="button tiny primary radius" title="Mark as processed">
              <i class="${font:far()} fa-check"></i> Process
            </button>
            <button id="btn-dismiss-submission" class="button tiny secondary radius" title="Dismiss">
              <i class="${font:far()} fa-times"></i> Dismiss
            </button>
          </div>
        </div>
        <div id="crm-submission-meta" class="crm-detail-meta"></div>
        <div id="crm-submission-fields" class="crm-detail-fields"></div>
      </div>

      <!-- Member Detail -->
      <div id="crm-member-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6>Subscriber Detail</h6>
        </div>
        <div id="crm-member-info" class="crm-detail-meta"></div>
      </div>

      <!-- Customer Detail -->
      <div id="crm-customer-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-customer-name-title">Customer</h6>
          <div class="crm-detail-actions">
            <button id="btn-view-customer-orders" class="button tiny primary radius">
              <i class="${font:far()} fa-shopping-bag"></i> Orders
            </button>
          </div>
        </div>
        <div id="crm-customer-info" class="crm-detail-meta"></div>
      </div>

      <!-- Order Detail -->
      <div id="crm-order-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-order-number-title">Order</h6>
        </div>
        <div id="crm-order-info" class="crm-detail-meta"></div>
      </div>

      <!-- Product Category Detail -->
      <div id="crm-product-category-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-product-category-title">Product Category</h6>
          <div class="crm-detail-actions">
            <a id="btn-edit-product-category" href="#" class="button tiny primary radius"><i class="${font:far()} fa-edit"></i> Edit</a>
          </div>
        </div>
        <div id="crm-product-category-info" class="crm-detail-meta"></div>
      </div>

      <!-- Product Detail -->
      <div id="crm-product-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-product-title">Product</h6>
          <div class="crm-detail-actions">
            <a id="btn-edit-product" href="#" class="button tiny primary radius"><i class="${font:far()} fa-edit"></i> Edit</a>
          </div>
        </div>
        <div id="crm-product-info" class="crm-detail-meta"></div>
      </div>

      <!-- Pricing Rule Detail -->
      <div id="crm-pricing-rule-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-pricing-rule-title">Pricing Rule</h6>
          <div class="crm-detail-actions">
            <a id="btn-edit-pricing-rule" href="#" class="button tiny primary radius"><i class="${font:far()} fa-edit"></i> Edit</a>
          </div>
        </div>
        <div id="crm-pricing-rule-info" class="crm-detail-meta"></div>
      </div>

      <!-- Sales Tax Nexus Detail -->
      <div id="crm-sales-tax-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-sales-tax-title">Sales Tax Nexus</h6>
          <div class="crm-detail-actions">
            <a id="btn-edit-sales-tax" href="#" class="button tiny primary radius"><i class="${font:far()} fa-edit"></i> Edit</a>
          </div>
        </div>
        <div id="crm-sales-tax-info" class="crm-detail-meta"></div>
      </div>

      <!-- Shipping Rate Detail -->
      <div id="crm-shipping-rate-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-shipping-rate-title">Shipping Rate</h6>
          <div class="crm-detail-actions">
            <a id="btn-edit-shipping-rate" href="#" class="button tiny primary radius"><i class="${font:far()} fa-edit"></i> Edit</a>
          </div>
        </div>
        <div id="crm-shipping-rate-info" class="crm-detail-meta"></div>
      </div>

      <!-- User Detail -->
      <div id="crm-user-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-user-name-title">User</h6>
        </div>
        <div id="crm-user-info" class="crm-detail-meta"></div>
      </div>

      <!-- User Group Detail -->
      <div id="crm-user-group-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-user-group-title">User Group</h6>
          <div class="crm-detail-actions">
            <button id="btn-edit-user-group" class="button tiny primary radius"><i class="${font:far()} fa-edit"></i> Edit</button>
          </div>
        </div>
        <div id="crm-user-group-info" class="crm-detail-meta"></div>
      </div>

      <!-- User Role Detail -->
      <div id="crm-user-role-detail" class="crm-detail-content" style="display: none;">
        <div class="crm-detail-header">
          <h6 id="crm-user-role-title">User Role</h6>
        </div>
        <div id="crm-user-role-info" class="crm-detail-meta"></div>
      </div>
    </div>

  </div>
</div>

<!-- CRM Add/Edit Modals -->

<!-- Add Mailing List Modal -->
<div id="crm-modal-mailing-list" class="crm-modal-overlay" style="display:none;">
  <div class="crm-modal">
    <div class="crm-modal-header">
      <h5>Add Mailing List</h5>
      <button class="crm-modal-close" data-modal="crm-modal-mailing-list"><i class="${font:far()} fa-times"></i></button>
    </div>
    <div class="crm-modal-body">
      <div id="crm-modal-mailing-list-error" class="crm-modal-error" style="display:none;"></div>
      <label>Name <span class="required">*</span>
        <input type="text" id="ml-name" class="property-input" placeholder="Internal identifier name..." maxlength="255" />
      </label>
      <label>Title
        <input type="text" id="ml-title" class="property-input" placeholder="Title displayed to users..." maxlength="255" />
      </label>
      <label>Description
        <input type="text" id="ml-description" class="property-input" placeholder="Optional description..." maxlength="512" />
      </label>
      <label>
        <input type="checkbox" id="ml-show-online" />
        Show Online
      </label>
    </div>
    <div class="crm-modal-footer">
      <button id="btn-save-mailing-list" class="button small success radius">Save</button>
      <button class="button small secondary radius crm-modal-close" data-modal="crm-modal-mailing-list">Cancel</button>
    </div>
  </div>
</div>

<!-- Add Customer Modal -->
<div id="crm-modal-customer" class="crm-modal-overlay" style="display:none;">
  <div class="crm-modal">
    <div class="crm-modal-header">
      <h5>Add Customer</h5>
      <button class="crm-modal-close" data-modal="crm-modal-customer"><i class="${font:far()} fa-times"></i></button>
    </div>
    <div class="crm-modal-body">
      <div id="crm-modal-customer-error" class="crm-modal-error" style="display:none;"></div>
      <label>First Name <span class="required">*</span>
        <input type="text" id="cust-first-name" class="property-input" placeholder="First name..." maxlength="100" />
      </label>
      <label>Last Name <span class="required">*</span>
        <input type="text" id="cust-last-name" class="property-input" placeholder="Last name..." maxlength="100" />
      </label>
      <label>Email <span class="required">*</span>
        <input type="email" id="cust-email" class="property-input" placeholder="Email address..." maxlength="255" />
      </label>
      <label>Organization
        <input type="text" id="cust-organization" class="property-input" placeholder="Company/organization (optional)..." maxlength="255" />
      </label>
    </div>
    <div class="crm-modal-footer">
      <button id="btn-save-customer" class="button small success radius">Save</button>
      <button class="button small secondary radius crm-modal-close" data-modal="crm-modal-customer">Cancel</button>
    </div>
  </div>
</div>

<!-- Add Product Category Modal -->
<div id="crm-modal-product-category" class="crm-modal-overlay" style="display:none;">
  <div class="crm-modal">
    <div class="crm-modal-header">
      <h5>Add Product Category</h5>
      <button class="crm-modal-close" data-modal="crm-modal-product-category"><i class="${font:far()} fa-times"></i></button>
    </div>
    <div class="crm-modal-body">
      <div id="crm-modal-product-category-error" class="crm-modal-error" style="display:none;"></div>
      <label>Name <span class="required">*</span>
        <input type="text" id="pc-name" class="property-input" placeholder="Category name..." maxlength="255" />
      </label>
      <label>Unique ID
        <input type="text" id="pc-unique-id" class="property-input" placeholder="Auto-generated if left blank (lowercase, a-z, 0-9, dashes)..." maxlength="255" />
      </label>
      <label>Description
        <input type="text" id="pc-description" class="property-input" placeholder="Optional description..." maxlength="255" />
      </label>
      <label>
        <input type="checkbox" id="pc-enabled" checked />
        Show Online
      </label>
    </div>
    <div class="crm-modal-footer">
      <button id="btn-save-product-category" class="button small success radius">Save</button>
      <button class="button small secondary radius crm-modal-close" data-modal="crm-modal-product-category">Cancel</button>
    </div>
  </div>
</div>

<!-- Add Product Modal -->
<div id="crm-modal-product" class="crm-modal-overlay" style="display:none;">
  <div class="crm-modal">
    <div class="crm-modal-header">
      <h5>Add Product</h5>
      <button class="crm-modal-close" data-modal="crm-modal-product"><i class="${font:far()} fa-times"></i></button>
    </div>
    <div class="crm-modal-body">
      <div id="crm-modal-product-error" class="crm-modal-error" style="display:none;"></div>
      <label>Name <span class="required">*</span>
        <input type="text" id="prod-name" class="property-input" placeholder="Product name..." maxlength="255" />
      </label>
      <label>Unique ID
        <input type="text" id="prod-unique-id" class="property-input" placeholder="Auto-generated if left blank..." maxlength="255" />
      </label>
      <label>Description
        <textarea id="prod-description" class="property-input" rows="3" placeholder="Optional description..." style="width:100%; resize:vertical;"></textarea>
      </label>
      <label>
        <input type="checkbox" id="prod-enabled" checked />
        Enabled
      </label>
    </div>
    <div class="crm-modal-footer">
      <button id="btn-save-product" class="button small success radius">Save</button>
      <button class="button small secondary radius crm-modal-close" data-modal="crm-modal-product">Cancel</button>
    </div>
  </div>
</div>

<!-- Add Pricing Rule Modal -->
<div id="crm-modal-pricing-rule" class="crm-modal-overlay" style="display:none;">
  <div class="crm-modal">
    <div class="crm-modal-header">
      <h5>Add Pricing Rule</h5>
      <button class="crm-modal-close" data-modal="crm-modal-pricing-rule"><i class="${font:far()} fa-times"></i></button>
    </div>
    <div class="crm-modal-body">
      <div id="crm-modal-pricing-rule-error" class="crm-modal-error" style="display:none;"></div>
      <label>Display Name <span class="required">*</span>
        <input type="text" id="pr-name" class="property-input" placeholder="Name shown during checkout..." maxlength="255" />
      </label>
      <label>Promo Code
        <input type="text" id="pr-promo-code" class="property-input" placeholder="Optional promo code..." maxlength="20" />
      </label>
      <label>Description
        <input type="text" id="pr-description" class="property-input" placeholder="Optional description..." maxlength="512" />
      </label>
      <label>Percent Off Subtotal
        <input type="number" id="pr-subtotal-percent" class="property-input" placeholder="e.g. 10" min="0" max="100" />
      </label>
      <label>Amount to Subtract from Total ($)
        <input type="number" id="pr-subtract-amount" class="property-input" placeholder="e.g. 5.00" min="0" step="0.01" />
      </label>
      <label>
        <input type="checkbox" id="pr-free-shipping" />
        Free Shipping
      </label>
      <label>
        <input type="checkbox" id="pr-enabled" checked />
        Enabled
      </label>
    </div>
    <div class="crm-modal-footer">
      <button id="btn-save-pricing-rule" class="button small success radius">Save</button>
      <button class="button small secondary radius crm-modal-close" data-modal="crm-modal-pricing-rule">Cancel</button>
    </div>
  </div>
</div>

<!-- Add Sales Tax Nexus Modal -->
<div id="crm-modal-sales-tax" class="crm-modal-overlay" style="display:none;">
  <div class="crm-modal">
    <div class="crm-modal-header">
      <h5>Add Sales Tax Nexus</h5>
      <button class="crm-modal-close" data-modal="crm-modal-sales-tax"><i class="${font:far()} fa-times"></i></button>
    </div>
    <div class="crm-modal-body">
      <div id="crm-modal-sales-tax-error" class="crm-modal-error" style="display:none;"></div>
      <label>Street Address <span class="required">*</span>
        <input type="text" id="st-street" class="property-input" placeholder="Address..." maxlength="255" />
      </label>
      <label>Apt / Suite / Other
        <input type="text" id="st-address-line2" class="property-input" placeholder="Optional..." maxlength="255" />
      </label>
      <label>Country <span class="required">*</span>
        <select id="st-country" class="property-input">
          <option value="United States">United States</option>
        </select>
      </label>
      <label>City <span class="required">*</span>
        <input type="text" id="st-city" class="property-input" placeholder="City..." maxlength="100" />
      </label>
      <label>State <span class="required">*</span>
        <select id="st-state" class="property-input">
          <option value="">Select state...</option>
          <option value="AL">Alabama (AL)</option><option value="AK">Alaska (AK)</option><option value="AZ">Arizona (AZ)</option>
          <option value="AR">Arkansas (AR)</option><option value="CA">California (CA)</option><option value="CO">Colorado (CO)</option>
          <option value="CT">Connecticut (CT)</option><option value="DE">Delaware (DE)</option><option value="DC">District of Columbia (DC)</option>
          <option value="FL">Florida (FL)</option><option value="GA">Georgia (GA)</option><option value="HI">Hawaii (HI)</option>
          <option value="ID">Idaho (ID)</option><option value="IL">Illinois (IL)</option><option value="IN">Indiana (IN)</option>
          <option value="IA">Iowa (IA)</option><option value="KS">Kansas (KS)</option><option value="KY">Kentucky (KY)</option>
          <option value="LA">Louisiana (LA)</option><option value="ME">Maine (ME)</option><option value="MD">Maryland (MD)</option>
          <option value="MA">Massachusetts (MA)</option><option value="MI">Michigan (MI)</option><option value="MN">Minnesota (MN)</option>
          <option value="MS">Mississippi (MS)</option><option value="MO">Missouri (MO)</option><option value="MT">Montana (MT)</option>
          <option value="NE">Nebraska (NE)</option><option value="NV">Nevada (NV)</option><option value="NH">New Hampshire (NH)</option>
          <option value="NJ">New Jersey (NJ)</option><option value="NM">New Mexico (NM)</option><option value="NY">New York (NY)</option>
          <option value="NC">North Carolina (NC)</option><option value="ND">North Dakota (ND)</option><option value="OH">Ohio (OH)</option>
          <option value="OK">Oklahoma (OK)</option><option value="OR">Oregon (OR)</option><option value="PA">Pennsylvania (PA)</option>
          <option value="RI">Rhode Island (RI)</option><option value="SC">South Carolina (SC)</option><option value="SD">South Dakota (SD)</option>
          <option value="TN">Tennessee (TN)</option><option value="TX">Texas (TX)</option><option value="UT">Utah (UT)</option>
          <option value="VT">Vermont (VT)</option><option value="VA">Virginia (VA)</option><option value="WA">Washington (WA)</option>
          <option value="WV">West Virginia (WV)</option><option value="WI">Wisconsin (WI)</option><option value="WY">Wyoming (WY)</option>
          <option value="PR">Puerto Rico (PR)</option><option value="GU">Guam (GU)</option>
        </select>
      </label>
      <label>Postal Code <span class="required">*</span>
        <input type="text" id="st-postal-code" class="property-input" placeholder="Zip/Postal Code..." maxlength="20" />
      </label>
    </div>
    <div class="crm-modal-footer">
      <button id="btn-save-sales-tax" class="button small success radius">Save</button>
      <button class="button small secondary radius crm-modal-close" data-modal="crm-modal-sales-tax">Cancel</button>
    </div>
  </div>
</div>

<!-- Add Shipping Rate Modal -->
<div id="crm-modal-shipping-rate" class="crm-modal-overlay" style="display:none;">
  <div class="crm-modal">
    <div class="crm-modal-header">
      <h5>Add Shipping Rate</h5>
      <button class="crm-modal-close" data-modal="crm-modal-shipping-rate"><i class="${font:far()} fa-times"></i></button>
    </div>
    <div class="crm-modal-body">
      <div id="crm-modal-shipping-rate-error" class="crm-modal-error" style="display:none;"></div>
      <label>Country <span class="required">*</span>
        <select id="sr-country" class="property-input">
          <option value="">Select country...</option>
          <option value="*">* (All)</option>
          <option value="US">United States (US)</option>
        </select>
      </label>
      <label>State / Region <span class="required">*</span>
        <select id="sr-region" class="property-input">
          <option value="">Select state/region...</option>
          <option value="*">* (All)</option>
          <option value="AL">Alabama (AL)</option><option value="AK">Alaska (AK)</option><option value="AZ">Arizona (AZ)</option>
          <option value="AR">Arkansas (AR)</option><option value="CA">California (CA)</option><option value="CO">Colorado (CO)</option>
          <option value="CT">Connecticut (CT)</option><option value="DE">Delaware (DE)</option><option value="DC">District of Columbia (DC)</option>
          <option value="FL">Florida (FL)</option><option value="GA">Georgia (GA)</option><option value="HI">Hawaii (HI)</option>
          <option value="ID">Idaho (ID)</option><option value="IL">Illinois (IL)</option><option value="IN">Indiana (IN)</option>
          <option value="IA">Iowa (IA)</option><option value="KS">Kansas (KS)</option><option value="KY">Kentucky (KY)</option>
          <option value="LA">Louisiana (LA)</option><option value="ME">Maine (ME)</option><option value="MD">Maryland (MD)</option>
          <option value="MA">Massachusetts (MA)</option><option value="MI">Michigan (MI)</option><option value="MN">Minnesota (MN)</option>
          <option value="MS">Mississippi (MS)</option><option value="MO">Missouri (MO)</option><option value="MT">Montana (MT)</option>
          <option value="NE">Nebraska (NE)</option><option value="NV">Nevada (NV)</option><option value="NH">New Hampshire (NH)</option>
          <option value="NJ">New Jersey (NJ)</option><option value="NM">New Mexico (NM)</option><option value="NY">New York (NY)</option>
          <option value="NC">North Carolina (NC)</option><option value="ND">North Dakota (ND)</option><option value="OH">Ohio (OH)</option>
          <option value="OK">Oklahoma (OK)</option><option value="OR">Oregon (OR)</option><option value="PA">Pennsylvania (PA)</option>
          <option value="RI">Rhode Island (RI)</option><option value="SC">South Carolina (SC)</option><option value="SD">South Dakota (SD)</option>
          <option value="TN">Tennessee (TN)</option><option value="TX">Texas (TX)</option><option value="UT">Utah (UT)</option>
          <option value="VT">Vermont (VT)</option><option value="VA">Virginia (VA)</option><option value="WA">Washington (WA)</option>
          <option value="WV">West Virginia (WV)</option><option value="WI">Wisconsin (WI)</option><option value="WY">Wyoming (WY)</option>
          <option value="PR">Puerto Rico (PR)</option><option value="GU">Guam (GU)</option>
        </select>
      </label>
      <label>Postal Code <span class="required">*</span>
        <input type="text" id="sr-postal-code" class="property-input" placeholder="Zip code or * for all..." maxlength="20" />
      </label>
      <label>Shipping Method <span class="required">*</span>
        <select id="sr-shipping-method" class="property-input">
          <option value="">Select method...</option>
        </select>
      </label>
      <label>Shipping Fee ($) <span class="required">*</span>
        <input type="number" id="sr-shipping-fee" class="property-input" placeholder="0.00" min="0" step="0.01" value="0.00" />
      </label>
      <label>Handling Fee ($) <span class="required">*</span>
        <input type="number" id="sr-handling-fee" class="property-input" placeholder="0.00" min="0" step="0.01" value="0.00" />
      </label>
      <label>Min. Order Subtotal ($)
        <input type="number" id="sr-min-subtotal" class="property-input" placeholder="0.00" min="0" step="0.01" />
      </label>
      <label>Display Text at Checkout
        <input type="text" id="sr-display-text" class="property-input" placeholder="Description shown to customers..." maxlength="255" />
      </label>
    </div>
    <div class="crm-modal-footer">
      <button id="btn-save-shipping-rate" class="button small success radius">Save</button>
      <button class="button small secondary radius crm-modal-close" data-modal="crm-modal-shipping-rate">Cancel</button>
    </div>
  </div>
</div>

<!-- Add User Group Modal -->
<div id="crm-modal-user-group" class="crm-modal-overlay" style="display:none;">
  <div class="crm-modal">
    <div class="crm-modal-header">
      <h5 id="crm-modal-user-group-title">Add User Group</h5>
      <button class="crm-modal-close" data-modal="crm-modal-user-group"><i class="${font:far()} fa-times"></i></button>
    </div>
    <div class="crm-modal-body">
      <input type="hidden" id="ug-id" value="" />
      <div id="crm-modal-user-group-error" class="crm-modal-error" style="display:none;"></div>
      <label>Name <span class="required">*</span>
        <input type="text" id="ug-name" class="property-input" placeholder="Group name..." maxlength="255" />
      </label>
      <label>Unique ID
        <input type="text" id="ug-unique-id" class="property-input" placeholder="Auto-generated if left blank (lowercase, a-z, 0-9, dashes)..." maxlength="255" />
      </label>
      <label>Description
        <input type="text" id="ug-description" class="property-input" placeholder="Optional description..." maxlength="512" />
      </label>
    </div>
    <div class="crm-modal-footer">
      <button id="btn-save-user-group" class="button small success radius">Save</button>
      <button class="button small secondary radius crm-modal-close" data-modal="crm-modal-user-group">Cancel</button>
    </div>
  </div>
</div>

<!-- JavaScript for CRM Editor -->
<g:compress>
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/widgets/editor/visual-crm-editor.js"></script>
</g:compress>

<script>
  document.addEventListener('DOMContentLoaded', function() {
    setupAppsMenu();
    setupEditorAppSwitcher();

    if (typeof VisualCRMEditor !== 'undefined') {
      const editor = new VisualCRMEditor({
        token: '<c:out value="${userSession.formToken}" />',
        section: '<c:out value="${section}"/>',
        selectedId: '<c:out value="${selectedId}"/>'
      });
      editor.init();
    }
  });
</script>
