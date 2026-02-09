<%--
  ~ Copyright 2025 Matt Rajkowski
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
<%@ page import="static com.simisinc.platform.ApplicationInfo.PRODUCT_NAME" %>
<%@ page import="static com.simisinc.platform.ApplicationInfo.VERSION" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="masterWebPage" class="com.simisinc.platform.domain.model.cms.WebPage" scope="request"/>
<jsp:useBean id="pageRenderInfo" class="com.simisinc.platform.presentation.controller.PageRenderInfo" scope="request"/>
<jsp:useBean id="PageBody" class="java.lang.String" scope="request"/>
<jsp:useBean id="systemPropertyMap" class="java.util.HashMap" scope="request"/>
<jsp:useBean id="sitePropertyMap" class="java.util.HashMap" scope="request"/>
<jsp:useBean id="themePropertyMap" class="java.util.HashMap" scope="request"/>
<jsp:useBean id="analyticsPropertyMap" class="java.util.HashMap" scope="request"/>
<jsp:useBean id="ecommercePropertyMap" class="java.util.HashMap" scope="request"/>
<c:set var="adminIframeUrl" value="${requestScope.adminIframeUrl}" />
<c:if test="${empty adminIframeUrl}">
  <c:set var="adminIframeUrl" value="${ctx}/admin" />
</c:if>
<%-- Draw the admin menu with iframe-based content loading --%>
<div class="off-canvas-wrapper">
  <div class="off-canvas position-left reveal-for-medium admin-menu hide-for-print" style="z-index: 1005 !important; padding-bottom: 50px" id="offCanvas" data-off-canvas>
    <div class="app-title">
      <c:out value="<%= PRODUCT_NAME %>"/><br />
      <small>v<c:out value="<%= VERSION %>"/></small>
    </div>
    <div class="app-site">
      <i class="${font:far()} fa-globe fa-fw"></i>
      <a href="${ctx}/"><c:out value="${sitePropertyMap['site.name']}"/></a>
    </div>
    <div class="app-user">
      <i class="${font:far()} fa-user fa-fw"></i>
      <c:out value="${userSession.user.fullName}"/>
    </div>
    <%-- Admin Link --%>
    <ul class="vertical menu">
      <li class="section-title">Admin</li>
      <li<c:if test="${pageRenderInfo.name eq '/admin'}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin', this)"><i class="${font:far()} fa-home fa-fw"></i> <span>Welcome</span></a></li>
      <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/documentation')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/documentation/wiki/Home', this)"><i class="${font:far()} fa-book fa-fw"></i> <span>Documentation</span></a></li>
      <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/activity')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/activity', this)"><i class="${font:far()} fa-exchange-alt fa-fw"></i> <span>Activity</span></a></li>
    </ul>
    <%-- Community menu --%>
    <c:if test="${userSession.hasRole('admin') || userSession.hasRole('community-manager')}">
      <ul class="vertical menu">
        <li class="section-title">Community</li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/community/analytics')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/community/analytics', this)"><i class="${font:far()} fa-chart-line fa-fw"></i> <span>Analytics</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/form-')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/form-data', this)"><i class="${font:far()} fa-list-alt fa-fw"></i> <span>Form Data</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/mailing-list') && !fn:startsWith(pageRenderInfo.name, '/admin/mailing-list-properties')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/mailing-lists', this)"><i class="${font:far()} fa-envelope fa-fw"></i> <span>Mailing Lists</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/user') || fn:startsWith(pageRenderInfo.name, '/admin/modify-user')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/users', this)"><i class="${font:far()} fa-user-circle fa-fw"></i> <span>Users</span></a></li>
        <c:if test="${userSession.hasRole('admin')}">
          <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/group')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/groups', this)"><i class="${font:far()} fa-users fa-fw"></i> <span>User Groups</span></a></li>
        </c:if>
      </ul>
    </c:if>
    <%-- Content menu --%>
    <c:if test="${userSession.hasRole('admin') || userSession.hasRole('content-manager')}">
      <ul class="vertical menu">
        <li class="section-title">Content</li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/content/analytics')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/content/analytics', this)"><i class="${font:far()} fa-chart-line fa-fw"></i> <span>Analytics</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/visual')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/visual-content-editor', this)"><i class="${font:far()} fa-edit fa-fw"></i> <span>Visual Editors</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/sitemap')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/sitemap', this)"><i class="${font:far()} fa-sitemap fa-fw"></i> <span>Site Map</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/web-page')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/web-pages', this)"><i class="${font:far()} fa-sticky-note fa-fw"></i> <span>Web Pages</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/image')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/images', this)"><i class="${font:far()} fa-image fa-fw"></i> <span>Images</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/content-list')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/content-list', this)"><i class="${font:far()} fa-th fa-fw"></i> <span>Content</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/blog')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/blogs', this)"><i class="${font:far()} fa-quote-right fa-fw"></i> <span>Blogs</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/calendar')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/calendars', this)"><i class="${font:far()} fa-calendar fa-fw"></i> <span>Calendars</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/folder')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/folders', this)"><i class="${font:far()} fa-copy fa-fw"></i> <span>Files &amp; Folders</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/wiki')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/wikis', this)"><i class="${font:far()} fa-file fa-fw"></i> <span>Wikis</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/sticky-footer-links')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/sticky-footer-links', this)"><i class="${font:far()} fa-file fa-fw"></i> <span>Sticky Page Buttons</span></a></li>
      </ul>
    </c:if>
    <%-- Data menu --%>
    <c:if test="${userSession.hasRole('admin') || userSession.hasRole('data-manager')}">
      <ul class="vertical menu">
        <li class="section-title">Data</li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/collection')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/collections', this)"><i class="${font:far()} fa-database fa-fw"></i> <span>Collections</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/dataset')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/datasets', this)"><i class="${font:far()} fa-table fa-fw"></i> <span>Datasets</span></a></li>
      </ul>
    </c:if>
    <%-- E-Commerce menu (if enabled if settings) --%>
    <c:if test="${!empty ecommercePropertyMap['ecommerce.enabled'] && ecommercePropertyMap['ecommerce.enabled'] eq 'true'}">
      <c:if test="${userSession.hasRole('admin') || userSession.hasRole('ecommerce-manager')}">
        <ul class="vertical menu">
          <li class="section-title">E-Commerce</li>
          <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/e-commerce/analytics')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/e-commerce/analytics', this)"><i class="${font:far()} fa-chart-line fa-fw"></i> <span>Analytics</span></a></li>
          <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/order')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/orders', this)"><i class="${font:far()} fa-receipt fa-fw"></i> <span>Orders</span></a></li>
          <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/customer')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/customers', this)"><i class="${font:far()} fa-address-book fa-fw"></i> <span>Customers</span></a></li>
          <li<c:if test="${pageRenderInfo.name eq '/admin/products' || pageRenderInfo.name eq '/admin/product'}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/products', this)"><i class="${font:far()} fa-dolly fa-fw"></i> <span>Products</span></a></li>
          <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/product-categor')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/product-categories', this)"><i class="${font:far()} fa-border-all fa-fw"></i> <span>Categories</span></a></li>
          <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/pricing-rule')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/pricing-rules', this)"><i class="${font:far()} fa-tags fa-fw"></i> <span>Pricing Rules</span></a></li>
          <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/sales-tax-nexus')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/sales-tax-nexus', this)"><i class="${font:far()} fa-balance-scale fa-fw"></i> <span>Sales Tax Nexus</span></a></li>
          <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/shipping-rates')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/shipping-rates', this)"><i class="${font:far()} fa-shipping-fast fa-fw"></i> <span>Shipping Rates</span></a></li>
        </ul>
      </c:if>
    </c:if>
    <%-- API, Apps, etc. --%>
    <c:if test="${userSession.hasRole('admin')}">
      <ul class="vertical menu">
        <li class="section-title">Access</li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/api')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/apis', this)"><i class="${font:far()} fa-paper-plane fa-fw"></i> <span>APIs</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/app')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/apps', this)"><i class="${font:far()} fa-mobile fa-fw"></i> <span>Apps</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/blocked-ip-list')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/blocked-ip-list', this)"><i class="${font:far()} fa-shield-halved fa-fw"></i> <span>Blocked IPs</span></a></li>
      </ul>
    </c:if>
    <%-- Settings menu --%>
    <c:if test="${userSession.hasRole('admin')}">
      <ul class="vertical menu">
        <li class="section-title">Settings</li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/theme')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/theme-properties', this)"><i class="${font:far()} fa-palette fa-fw"></i> <span>Theme</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/site-properties')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/site-properties', this)"><i class="${font:far()} fa-rocket fa-fw"></i> <span>Site Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/social')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/social-media-settings', this)"><i class="${font:far()} fa-thumbs-up fa-fw"></i> <span>Social Media</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/configure-analytics')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/configure-analytics', this)"><i class="${font:far()} fa-chart-line fa-fw"></i> <span>Analytics Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/captcha')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/captcha-properties', this)"><i class="${font:far()} fa-key fa-fw"></i> <span>Captcha Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/bi')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/bi-properties', this)"><i class="${font:far()} fa-table-columns fa-fw"></i> <span>BI Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/ecommerce')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/ecommerce-properties', this)"><i class="${font:far()} fa-shopping-cart fa-fw"></i> <span>E-commerce Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/elearning')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/elearning-properties', this)"><i class="${font:far()} fa-chalkboard-teacher fa-fw"></i> <span>E-learning Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/mail-properties')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/mail-properties', this)"><i class="${font:far()} fa-cogs fa-fw"></i> <span>Email Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/mailing-list-properties')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/mailing-list-properties', this)"><i class="${font:far()} fa-envelope fa-fw"></i> <span>Mailing List Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/maps')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/maps-properties', this)"><i class="${font:far()} fa-map fa-fw"></i> <span>Maps Settings</span></a></li>
        <li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/web-conferencing-properties')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/web-conferencing-properties', this)"><i class="${font:far()} fa-users-rectangle fa-fw"></i> <span>Web Conferencing Settings</span></a></li>
        <%--<li<c:if test="${fn:startsWith(pageRenderInfo.name, '/admin/email-templates')}"> class="is-active"</c:if>><a href="javascript:void(0)" onclick="loadAdminContent('${ctx}/admin/email-templates', this)"><i class="${font:far()} fa-file-text fa-fw"></i> <span>Email Templates</span></a></li>--%>
      </ul>
    </c:if>
  </div>
  <div class="off-canvas-content" data-off-canvas-content>
    <div class="web-content admin-web-content">
      <iframe id="admin-iframe" src="${adminIframeUrl}" style="width: 100%; height: 100vh; border: none; display: block;"></iframe>
    </div>
  </div>
</div>
<%-- Admin iframe navigation script --%>
<script>
  function loadAdminContent(url, linkElement) {
    var iframe = document.getElementById('admin-iframe');
    if (iframe) {
      // Add iframe parameter to prevent re-rendering the full layout
      var separator = url.indexOf('?') > -1 ? '&' : '?';
      // iframe.src = url + separator + 'iframe=true';
      iframe.src = url;
      window.history.pushState(null, '', url);
    }
    
    // Update active state on menu items
    if (linkElement) {
      var listItem = linkElement.closest('li');
      if (listItem) {
        // Remove is-active class from all menu items
        var allMenuItems = document.querySelectorAll('#offCanvas li');
        allMenuItems.forEach(function(item) {
          item.classList.remove('is-active');
        });
        
        // Add is-active class to the clicked item
        listItem.classList.add('is-active');
      }
    }
  }
</script>