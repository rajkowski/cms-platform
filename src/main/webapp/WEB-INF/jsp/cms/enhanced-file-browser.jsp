<%--
  ~ Copyright 2026 Matt Rajkowski
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
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/tlds/html-functions.tld" %>
<%@ taglib prefix="text" uri="/WEB-INF/tlds/text-functions.tld" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="number" uri="/WEB-INF/tlds/number-functions.tld" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<%-- Include the formatting for when TinyMCE uses an iFrame to open the browser --%>
<%-- All of Foundation.css would override colors and stuff when using the browser directly --%>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/${font:fontawesome()}/css/all.min.css" />
  <link rel="stylesheet" type="text/css" href="${ctx}/css/${font:fontawesome()}/css/v4-shims.min.css" />
  <web:stylesheet package="foundation-sites" file="foundation.min.css" />
</g:compress>
<style>
  .tinymce-browser-container {
    display: flex;
    height: 100vh;
    max-height: 600px;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  }
  .browser-sidebar {
    width: 30%;
    min-width: 200px;
    max-width: 300px;
    border-right: 1px solid #e0e0e0;
    overflow-y: auto;
    padding: 1rem;
  }
  .browser-content {
    flex: 1;
    overflow-y: auto;
    padding: 1rem;
  }
  .search-box {
    margin-bottom: 1rem;
  }
  .search-box input {
    width: 100%;
    padding: 0.5rem;
    border: 1px solid #ccc;
    border-radius: 4px;
  }
  .folder-item, .subfolder-item {
    padding: 0.75rem;
    margin-bottom: 0.5rem;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    cursor: pointer;
    transition: background-color 0.2s;
  }
  .folder-item:hover, .subfolder-item:hover {
    background-color: #f5f5f5;
  }
  .folder-item.active, .subfolder-item.active {
    background-color: #e3f2fd;
    border-color: #2196F3;
  }
  .folder-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .folder-icon {
    color: #1976d2;
    font-size: 1.2em;
  }
  .folder-name {
    flex: 1;
    font-weight: 500;
  }
  .folder-nav-btn {
    background: none;
    border: none;
    cursor: pointer;
    color: #666;
    padding: 0.25rem;
  }
  .folder-nav-btn:hover {
    color: #2196F3;
  }
  .folder-meta {
    font-size: 0.85rem;
    color: #666;
    margin-top: 0.25rem;
    display: flex;
    gap: 1rem;
  }
  .breadcrumbs {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1rem;
    padding: 0.5rem;
    background-color: #f5f5f5;
    border-radius: 4px;
  }
  .breadcrumb-btn {
    background: none;
    border: none;
    color: #2196F3;
    cursor: pointer;
    text-decoration: underline;
  }
  .breadcrumb-btn:hover {
    color: #1565C0;
  }
  .breadcrumb-btn.active {
    color: #666;
    text-decoration: none;
    cursor: default;
  }
  .breadcrumb-separator {
    color: #999;
  }
  .file-list {
    list-style: none;
    padding: 0;
    margin: 0;
  }
  .file-item, .subfolder-list-item {
    padding: 0.75rem;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    margin-bottom: 0.5rem;
    cursor: pointer;
    transition: background-color 0.2s;
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }
  .file-item:hover, .subfolder-list-item:hover {
    background-color: #f5f5f5;
  }
  .file-icon {
    font-size: 1.5em;
    color: #1976d2;
  }
  .file-info {
    flex: 1;
  }
  .file-title {
    font-weight: 500;
    margin-bottom: 0.25rem;
  }
  .file-meta {
    font-size: 0.85rem;
    color: #666;
  }
  .empty-state {
    text-align: center;
    padding: 2rem;
    color: #999;
  }
  .parent-folder-link {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    margin-bottom: 1rem;
    cursor: pointer;
    background-color: #f9f9f9;
  }
  .parent-folder-link:hover {
    background-color: #e3f2fd;
  }
  .loading {
    text-align: center;
    padding: 2rem;
    color: #666;
  }
  .file-controls {
    display: flex;
    gap: 1rem;
    margin-bottom: 1rem;
    align-items: center;
  }
  .file-search {
    flex: 1;
    padding: 0.5rem;
    border: 1px solid #ccc;
    border-radius: 4px;
  }
  .sort-select {
    flex: 1;
    padding: 0.5rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    min-width: 150px;
  }
  .pagination {
    display: flex;
    justify-content: center;
    gap: 0.5rem;
    margin-top: 1rem;
    padding: 1rem 0;
  }
  .pagination button {
    padding: 0.5rem 0.75rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    background: white;
    cursor: pointer;
  }
  .pagination button:hover:not(:disabled) {
    background-color: #e3f2fd;
    border-color: #2196F3;
  }
  .pagination button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .pagination .page-info {
    padding: 0.5rem 0.75rem;
    color: #666;
  }
</style>
<div class="tinymce-browser-container">
  <!-- Sidebar with folders -->
  <div class="browser-sidebar">
    <div class="search-box">
      <input type="text" id="folder-search" placeholder="Search repositories..." />
    </div>
    <div id="folder-list-container">
      <div class="loading">Loading...</div>
    </div>
  </div>

  <!-- Main content with files -->
  <div class="browser-content">
    <div id="breadcrumb-container"></div>
    <div class="file-controls">
      <input type="text" id="file-search" class="file-search" placeholder="Search files..." />
      <select id="sort-select" class="sort-select">
        <option value="modified-desc">Modified (Newest First)</option>
        <option value="modified-asc">Modified (Oldest First)</option>
        <option value="title-asc">Title (A-Z)</option>
        <option value="title-desc">Title (Z-A)</option>
        <option value="size-desc">Size (Largest First)</option>
        <option value="size-asc">Size (Smallest First)</option>
      </select>
    </div>
    <div id="file-list-container">
      <div class="empty-state">Select a repository to browse files</div>
    </div>
    <div id="pagination-container"></div>
  </div>
</div>

<script>
class TinyMCEFileBrowser {
  constructor() {
    this.folders = [];
    this.subfolders = [];
    this.files = [];
    this.currentFolderId = -1;
    this.parentFolderId = -1;
    this.breadcrumbs = [];
    this.currentPage = 1;
    this.pageSize = 25;
    this.totalFiles = 0;
    this.sortBy = 'modified-desc';
    this.fileSearchTerm = '';
    this.folderSearch = document.getElementById('folder-search');
    this.fileSearch = document.getElementById('file-search');
    this.sortSelect = document.getElementById('sort-select');
    this.folderListContainer = document.getElementById('folder-list-container');
    this.fileListContainer = document.getElementById('file-list-container');
    this.breadcrumbContainer = document.getElementById('breadcrumb-container');
    this.paginationContainer = document.getElementById('pagination-container');
  }

  init() {
    if (this.folderSearch) {
      this.folderSearch.addEventListener('input', () => this.loadFolders());
    }
    if (this.fileSearch) {
      this.fileSearch.addEventListener('input', () => {
        this.fileSearchTerm = this.fileSearch.value.trim();
        this.currentPage = 1;
        if (this.currentFolderId > 0) {
          this.loadFiles(this.currentFolderId, this.currentSubFolderId);
        }
      });
    }
    if (this.sortSelect) {
      this.sortSelect.addEventListener('change', () => {
        this.sortBy = this.sortSelect.value;
        this.currentPage = 1;
        if (this.currentFolderId > 0) {
          this.loadFiles(this.currentFolderId, this.currentSubFolderId);
        }
      });
    }
    this.loadFolders();
  }

  async loadFolders() {
    try {
      const searchTerm = this.folderSearch ? this.folderSearch.value.trim() : '';
      const url = new URL('${ctx}/json/documentLibrary', window.location.origin);
      if (searchTerm) {
        url.searchParams.set('search', searchTerm);
      }
      
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }
      const payload = await response.json();
      this.folders = payload.folders || [];
      this.renderFolders();
    } catch (err) {
      console.error('Unable to load folders', err);
      if (this.folderListContainer) {
        this.folderListContainer.innerHTML = '<div class="empty-state">Unable to load repositories</div>';
      }
    }
  }

  renderFolders() {
    if (!this.folderListContainer) {
      return;
    }
    this.folderListContainer.innerHTML = '';

    // Render breadcrumbs if in subfolder view
    if (this.breadcrumbs.length > 0) {
      const breadcrumbDiv = document.createElement('div');
      breadcrumbDiv.className = 'breadcrumbs';

      const homeBtn = document.createElement('button');
      homeBtn.className = 'breadcrumb-btn';
      homeBtn.innerHTML = '<i class="fas fa-home"></i> All';
      homeBtn.addEventListener('click', () => this.navigateToRoot());
      breadcrumbDiv.appendChild(homeBtn);

      this.breadcrumbs.forEach((crumb, index) => {
        const separator = document.createElement('span');
        separator.className = 'breadcrumb-separator';
        separator.textContent = '/';
        breadcrumbDiv.appendChild(separator);

        const crumbBtn = document.createElement('button');
        crumbBtn.className = 'breadcrumb-btn';
        crumbBtn.textContent = crumb.name;
        if (index < this.breadcrumbs.length - 1) {
          crumbBtn.addEventListener('click', () => this.navigateToFolder(crumb.id, crumb.name));
        } else {
          crumbBtn.classList.add('active');
        }
        breadcrumbDiv.appendChild(crumbBtn);
      });

      this.folderListContainer.appendChild(breadcrumbDiv);
    }

    const items = this.parentFolderId === -1 ? this.folders : this.subfolders;
    if (!items.length) {
      this.folderListContainer.insertAdjacentHTML('beforeend', '<div class="empty-state">No repositories found</div>');
      return;
    }

    items.forEach((folder) => {
      const item = document.createElement('div');
      item.className = this.parentFolderId === -1 ? 'folder-item' : 'subfolder-item';
      item.dataset.folderId = folder.id;

      const header = document.createElement('div');
      header.className = 'folder-header';

      const icon = document.createElement('span');
      icon.className = 'folder-icon';
      icon.innerHTML = this.parentFolderId === -1 ? '<i class="fas fa-book"></i>' : '<i class="fa-regular fa-folder"></i>';
      header.appendChild(icon);

      const name = document.createElement('div');
      name.className = 'folder-name';
      name.textContent = folder.name || 'Untitled';
      header.appendChild(name);

      // Add subfolder navigation button if at root level
      if (this.parentFolderId === -1) {
        const navBtn = document.createElement('button');
        navBtn.className = 'folder-nav-btn';
        navBtn.innerHTML = '<i class="fas fa-chevron-right"></i>';
        navBtn.title = 'View subfolders';
        navBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          this.navigateToFolder(folder.id, folder.name);
        });
        header.appendChild(navBtn);
      }

      item.appendChild(header);

      const meta = document.createElement('div');
      meta.className = 'folder-meta';
      meta.innerHTML = '<span>' + (folder.fileCount || 0) + ' files</span>';
      item.appendChild(meta);

      item.addEventListener('click', () => {
        this.selectFolder(folder.id);
      });

      this.folderListContainer.appendChild(item);
    });
  }

  selectFolder(folderId) {
    if (!this.folderListContainer) {
      return;
    }
    this.folderListContainer.querySelectorAll('.folder-item, .subfolder-item').forEach((el) => {
      el.classList.toggle('active', Number(el.dataset.folderId) === Number(folderId));
    });
    
    this.currentFolderId = folderId;
    this.loadFiles(folderId);
  }

  async navigateToFolder(folderId, folderName) {
    // If clicking on an existing breadcrumb, remove entries after it
    const existingIndex = this.breadcrumbs.findIndex((crumb) => Number(crumb.id) === Number(folderId));
    if (existingIndex >= 0) {
      this.breadcrumbs = this.breadcrumbs.slice(0, existingIndex + 1);
    } else {
      this.breadcrumbs.push({ id: folderId, name: folderName });
    }

    this.parentFolderId = folderId;
    await this.loadSubfolders(folderId);
  }

  navigateToRoot() {
    this.parentFolderId = -1;
    this.breadcrumbs = [];
    this.subfolders = [];
    this.renderFolders();
    this.fileListContainer.innerHTML = '<div class="empty-state">Select a repository to browse files</div>';
  }

  async loadSubfolders(folderId) {
    try {
      const url = new URL('${ctx}/json/documentSubfolders', window.location.origin);
      url.searchParams.set('folderId', folderId);

      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }
      const payload = await response.json();
      this.subfolders = payload.subfolders || [];
      this.renderFolders();
    } catch (err) {
      console.error('Unable to load subfolders', err);
      if (this.folderListContainer) {
        this.folderListContainer.innerHTML = '<div class="empty-state">Unable to load subfolders</div>';
      }
    }
  }

  async loadFiles(folderId, subFolderId) {
    try {
      this.currentSubFolderId = subFolderId;
      const url = new URL('${ctx}/json/documentFileList', window.location.origin);
      if (subFolderId && subFolderId > 0) {
        url.searchParams.set('folderId', this.parentFolderId);
        url.searchParams.set('subFolderId', subFolderId);
      } else {
        url.searchParams.set('folderId', folderId);
      }
      url.searchParams.set('page', this.currentPage);
      url.searchParams.set('limit', this.pageSize);
      
      if (this.fileSearchTerm) {
        url.searchParams.set('search', this.fileSearchTerm);
      }

      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }
      const payload = await response.json();
      this.files = payload.files || [];
      this.totalFiles = payload.total || this.files.length;
      
      // Apply client-side sorting if needed
      this.sortFiles();
      
      // If we're viewing a root folder, load its subfolders too
      if (!subFolderId && this.parentFolderId === -1) {
        await this.loadSubfoldersForDisplay(folderId);
      }
      
      this.renderFiles(subFolderId);
      this.renderPagination();
    } catch (err) {
      console.error('Unable to load files', err);
      if (this.fileListContainer) {
        this.fileListContainer.innerHTML = '<div class="empty-state">Unable to load files</div>';
      }
    }
  }

  sortFiles() {
    const [field, direction] = this.sortBy.split('-');
    this.files.sort((a, b) => {
      let aVal, bVal;
      
      if (field === 'title') {
        aVal = (a.title || a.filename || '').toLowerCase();
        bVal = (b.title || b.filename || '').toLowerCase();
      } else if (field === 'modified') {
        aVal = new Date(a.modified || 0).getTime();
        bVal = new Date(b.modified || 0).getTime();
      } else if (field === 'size') {
        aVal = a.fileLength || 0;
        bVal = b.fileLength || 0;
      }
      
      if (direction === 'asc') {
        return aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
      } else {
        return aVal < bVal ? 1 : aVal > bVal ? -1 : 0;
      }
    });
  }

  async loadSubfoldersForDisplay(folderId) {
    try {
      const url = new URL('${ctx}/json/documentSubfolders', window.location.origin);
      url.searchParams.set('folderId', folderId);

      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }
      const payload = await response.json();
      this.currentSubfolders = payload.subfolders || [];
    } catch (err) {
      console.error('Unable to load subfolders for display', err);
      this.currentSubfolders = [];
    }
  }

  renderFiles(subFolderId) {
    if (!this.fileListContainer) {
      return;
    }

    this.fileListContainer.innerHTML = '';

    // Show breadcrumb if in subfolder
    if (subFolderId && this.breadcrumbContainer) {
      const breadcrumb = document.createElement('div');
      breadcrumb.className = 'breadcrumbs';
      const backBtn = document.createElement('button');
      backBtn.className = 'breadcrumb-btn';
      backBtn.innerHTML = '<i class="fas fa-arrow-left"></i> Back to parent folder';
      backBtn.addEventListener('click', () => {
        this.loadFiles(this.currentFolderId);
      });
      breadcrumb.appendChild(backBtn);
      this.breadcrumbContainer.innerHTML = '';
      this.breadcrumbContainer.appendChild(breadcrumb);
    } else if (this.breadcrumbContainer) {
      this.breadcrumbContainer.innerHTML = '';
    }

    // Render subfolders first if we have them (and not in a subfolder already)
    if (!subFolderId && this.currentSubfolders && this.currentSubfolders.length > 0) {
      const list = document.createElement('ul');
      list.className = 'file-list';

      this.currentSubfolders.forEach((subfolder) => {
        const item = document.createElement('li');
        item.className = 'subfolder-list-item';
        item.innerHTML = '<span class="file-icon"><i class="fa-regular fa-folder"></i></span>' +
          '<div class="file-info">' +
            '<div class="file-title">' + (subfolder.name || 'Untitled') + '</div>' +
            '<div class="file-meta">' + (subfolder.fileCount || 0) + ' files</div>' +
          '</div>';
        item.addEventListener('click', () => {
          this.loadFiles(this.currentFolderId, subfolder.id);
        });
        list.appendChild(item);
      });

      this.fileListContainer.appendChild(list);
    }

    // Render files
    if (!this.files.length && (!this.currentSubfolders || this.currentSubfolders.length === 0)) {
      this.fileListContainer.innerHTML = '<div class="empty-state">No files or folders in this location</div>';
      return;
    }

    const list = document.createElement('ul');
    list.className = 'file-list';

    this.files.forEach((file) => {
      const item = document.createElement('li');
      item.className = 'file-item';
      
      const icon = this.getMimeIcon(file.mimeType, file.filename);
      const size = file.fileLength ? this.formatSize(file.fileLength) : '';
      
      item.innerHTML = '<span class="file-icon">' + icon + '</span>' +
        '<div class="file-info">' +
          '<div class="file-title">' + (file.title || file.filename || 'Untitled') + '</div>' +
          '<div class="file-meta">' + (file.mimeType || '') + (size ? ' &bull; ' + size : '') + '</div>' +
        '</div>';
      
      item.addEventListener('click', () => {
        this.selectFile(file);
      });
      
      list.appendChild(item);
    });

    this.fileListContainer.appendChild(list);
  }

  selectFile(file) {
    const fileUrl = '${ctx}/assets/view/' + file.url;
    <c:choose>
      <c:when test="${!empty inputId}">
      // Legacy mode - set input field (handle both anchor tags and input elements)
      var element = top.document.getElementById("<c:out value="${inputId}" />");
      if (element) {
        if (element.tagName === 'A') {
          element.href = fileUrl;
        } else if (element.tagName === 'INPUT') {
          element.value = fileUrl;
        }
        if (typeof top.$ !== 'undefined' && top.$('#imageBrowserReveal').length) {
          top.$('#imageBrowserReveal').foundation('close');
        }
      }
      </c:when>
      <c:otherwise>
      // Modern mode - post message to TinyMCE
      window.parent.postMessage({
          mceAction: 'FileSelected',
          content: fileUrl
      }, '*');
      </c:otherwise>
    </c:choose>
  }

  renderPagination() {
    if (!this.paginationContainer) {
      return;
    }
    
    this.paginationContainer.innerHTML = '';
    
    if (this.totalFiles <= this.pageSize) {
      return; // No pagination needed
    }
    
    const totalPages = Math.ceil(this.totalFiles / this.pageSize);
    const pagination = document.createElement('div');
    pagination.className = 'pagination';
    
    // Previous button
    const prevBtn = document.createElement('button');
    prevBtn.innerHTML = '&laquo; Previous';
    prevBtn.disabled = this.currentPage === 1;
    prevBtn.addEventListener('click', () => {
      if (this.currentPage > 1) {
        this.currentPage--;
        this.loadFiles(this.currentFolderId, this.currentSubFolderId);
      }
    });
    pagination.appendChild(prevBtn);
    
    // Page info
    const pageInfo = document.createElement('span');
    pageInfo.className = 'page-info';
    pageInfo.textContent = 'Page ' + this.currentPage + ' of ' + totalPages;
    pagination.appendChild(pageInfo);
    
    // Next button
    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = 'Next &raquo;';
    nextBtn.disabled = this.currentPage >= totalPages;
    nextBtn.addEventListener('click', () => {
      if (this.currentPage < totalPages) {
        this.currentPage++;
        this.loadFiles(this.currentFolderId, this.currentSubFolderId);
      }
    });
    pagination.appendChild(nextBtn);
    
    this.paginationContainer.appendChild(pagination);
  }

  getMimeIcon(mimeType, filename) {
    const type = (mimeType || '').toLowerCase();
    const name = (filename || '').toLowerCase();
    if (type === 'text/uri-list') return '<i class="fas fa-link"></i>';
    if (type.startsWith('image/')) return '<i class="fas fa-file-image"></i>';
    if (type === 'application/pdf') return '<i class="fas fa-file-pdf"></i>';
    if (type.startsWith('video/')) return '<i class="fas fa-file-video"></i>';
    if (type.startsWith('audio/')) return '<i class="fas fa-file-audio"></i>';
    if (type.startsWith('text/') || type === 'application/json') return '<i class="fas fa-file-alt"></i>';
    if (type.includes('spreadsheet') || type.includes('excel')) return '<i class="fas fa-file-excel"></i>';
    if (type.includes('presentation') || type.includes('powerpoint')) return '<i class="fas fa-file-powerpoint"></i>';
    if (type.includes('word')) return '<i class="fas fa-file-word"></i>';
    if (type.includes('zip') || type.includes('compressed')) return '<i class="fas fa-file-archive"></i>';
    if (type.includes('xml') || type.includes('html') || type.includes('javascript')) return '<i class="fas fa-file-code"></i>';
    return '<i class="fas fa-file"></i>';
  }

  formatSize(bytes) {
    if (!bytes || bytes < 0) return '';
    const units = ['B', 'KB', 'MB', 'GB'];
    const power = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    const size = bytes / Math.pow(1024, power);
    return size.toFixed(1) + ' ' + units[power];
  }
}

document.addEventListener('DOMContentLoaded', function() {
  const browser = new TinyMCEFileBrowser();
  browser.init();
});
</script>
