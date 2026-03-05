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
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/static-site-manager.js"></script>
</g:compress>

<div id="visual-web-sync-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <c:set var="editorName" value="Web Sync" />
      <c:set var="activeApp" value="sync" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>

    <!-- Context Toolbar Actions (left) -->
    <div class="toolbar-section left" id="sync-toolbar-actions">
      <!-- Snapshots view toolbar -->
      <div class="sync-toolbar-view" id="toolbar-snapshots">
        <button id="generate-static-site-btn" class="button tiny success no-gap radius">
          <i class="${font:far()} fa-cogs"></i> Generate a Site Snapshot
        </button>
        <button id="publish-all-repos-btn" class="button tiny primary no-gap radius" style="display: none;">
          <i class="${font:far()} fa-upload"></i> Publish to All Repos
        </button>
      </div>
      <!-- Git repo view toolbar -->
      <div class="sync-toolbar-view" id="toolbar-git-repo" style="display: none;">
        <button id="save-git-settings-btn" class="button tiny primary no-gap radius">
          <i class="${font:far()} fa-save"></i> Save Settings
        </button>
        <button id="publish-git-repo-btn" class="button tiny success no-gap radius" style="display: none;">
          <i class="${font:far()} fa-upload"></i> Publish Now
        </button>
        <button id="delete-git-repo-btn" class="button tiny alert no-gap radius" style="display: none; margin-left: 10px;">
          <i class="${font:far()} fa-trash"></i> Delete Repo
        </button>
      </div>
    </div>

    <!-- Center Section -->
    <div class="toolbar-section center">
      <div id="static-site-polling-indicator" style="display: none;">
        <i class="${font:fas()} fa-spinner fa-spin"></i> Generating snapshot&hellip; please wait.
      </div>
      <div id="git-publish-indicator" style="display: none;">
        <i class="${font:fas()} fa-spinner fa-spin"></i> Publishing to repository&hellip; please wait.
      </div>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="sync-status-message" class="sync-status-message" style="display: none;"></div>
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
    </div>

    <!-- App Grid -->
    <div class="titlebar-right">
      <a href="#" id="dark-mode-toggle-menu" class="titlebar-btn apps-btn" title="Toggle Dark Mode">
        <i class="${font:far()} fa-moon"></i>
      </a>
      <a href="${ctx}/" class="titlebar-btn apps-btn confirm-exit" title="Exit back to site">
        <i class="${font:far()} fa-arrow-right-from-bracket"></i>
      </a>
    </div>
  </div>

  <!-- Two-Pane Layout -->
  <div id="visual-web-sync-container">

    <!-- Left Sidebar -->
    <div id="sync-sidebar">

      <!-- Site Snapshots Nav -->
      <div class="sync-sidebar-section">
        <div class="sync-sidebar-section-header">
          <i class="${font:far()} fa-archive"></i> Site Snapshots
        </div>
        <ul class="sync-sidebar-nav">
          <li class="sync-sidebar-item active" data-view="snapshots" id="nav-snapshots">
            <i class="${font:far()} fa-layer-group"></i>
            <span>All Snapshots</span>
          </li>
        </ul>
      </div>

      <!-- Git Repositories Nav -->
      <div class="sync-sidebar-section">
        <div class="sync-sidebar-section-header">
          <span><i class="${font:far()} fa-code-branch"></i> Git Repositories</span>
          <button id="add-git-repo-btn" class="sync-sidebar-add-btn" title="Add Git Repository">
            <i class="${font:far()} fa-plus"></i>
          </button>
        </div>
        <ul class="sync-sidebar-nav" id="git-repos-nav">
          <li class="sync-sidebar-empty" id="no-repos-item">No repositories configured</li>
        </ul>
      </div>

    </div>

    <!-- Main Content Panel -->
    <div id="sync-main">

      <!-- Snapshots View -->
      <div class="sync-view" id="view-snapshots">
        <div class="sync-view-header">
          <h3><i class="${font:far()} fa-archive"></i> Site Snapshots</h3>
          <p class="sync-view-subtitle">Generate and manage static versions of your website. Download or publish snapshots to a Git repository.</p>
        </div>
        <div class="sync-view-content">
          <table id="static-site-list" class="sync-file-table">
            <thead>
              <tr>
                <th>Snapshot File</th>
                <th>Size</th>
                <th>Date Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <%-- Populated via AJAX --%>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Git Repository Settings View -->
      <div class="sync-view" id="view-git-repo" style="display: none;">
        <div class="sync-view-header">
          <h3 id="git-repo-view-title"><i class="${font:far()} fa-code-branch"></i> Git Repository</h3>
          <p class="sync-view-subtitle" id="git-repo-view-subtitle">Configure repository connection settings for automated publishing.</p>
        </div>
        <div class="sync-view-content">
          <form id="git-settings-form" autocomplete="off">
            <input type="hidden" id="git-repo-id" name="repoId" value="">

            <div class="sync-form-section">
              <h5 class="sync-form-section-title">Repository Connection</h5>
              <div class="sync-form-grid">
                <div class="sync-form-group">
                  <label for="git-provider">Git Provider</label>
                  <select id="git-provider" name="gitProvider">
                    <option value="github">GitHub</option>
                    <option value="gitlab">GitLab</option>
                  </select>
                </div>
                <div class="sync-form-group sync-form-group-full">
                  <label for="repository-url">Repository URL</label>
                  <input type="text" id="repository-url" name="repositoryUrl" placeholder="https://github.com/user/repo.git">
                </div>
                <div class="sync-form-group">
                  <label for="branch-name">Branch Name</label>
                  <input type="text" id="branch-name" name="branchName" placeholder="static-site">
                </div>
                <div class="sync-form-group">
                  <label for="base-branch">Base Branch</label>
                  <input type="text" id="base-branch" name="baseBranch" placeholder="main">
                </div>
                <div class="sync-form-group">
                  <label for="target-directory">Target Directory</label>
                  <input type="text" id="target-directory" name="targetDirectory" placeholder="/">
                </div>
              </div>
            </div>

            <div class="sync-form-section">
              <h5 class="sync-form-section-title">Authentication</h5>
              <div class="sync-form-grid">
                <div class="sync-form-group">
                  <label for="git-username">Username</label>
                  <input type="text" id="git-username" name="username" placeholder="git-user" autocomplete="off">
                </div>
                <div class="sync-form-group">
                  <label for="git-email">Email</label>
                  <input type="email" id="git-email" name="email" placeholder="user@example.com" autocomplete="off">
                </div>
                <div class="sync-form-group sync-form-group-full">
                  <label for="access-token">Access Token <span class="sync-form-hint">(leave blank to keep existing)</span></label>
                  <input type="password" id="access-token" name="accessToken" placeholder="Enter token to update" autocomplete="new-password">
                </div>
              </div>
            </div>

            <div class="sync-form-section">
              <h5 class="sync-form-section-title">Commit &amp; Pull Request</h5>
              <div class="sync-form-grid">
                <div class="sync-form-group sync-form-group-full">
                  <label for="commit-message-template">Commit Message Template</label>
                  <input type="text" id="commit-message-template" name="commitMessageTemplate" placeholder="Static site update: ${'{'}timestamp{'}'}">
                </div>
                <div class="sync-form-group sync-form-group-full">
                  <label class="sync-checkbox-label">
                    <input type="checkbox" id="auto-create-pr" name="autoCreatePr" value="true" checked>
                    <span>Auto-create Pull Request after publishing</span>
                  </label>
                </div>
                <div class="sync-form-group" id="pr-title-group">
                  <label for="pr-title-template">PR Title Template</label>
                  <input type="text" id="pr-title-template" name="prTitleTemplate" placeholder="Static site update: ${'{'}timestamp{'}'}">
                </div>
                <div class="sync-form-group sync-form-group-full" id="pr-description-group">
                  <label for="pr-description-template">PR Description Template</label>
                  <textarea id="pr-description-template" name="prDescriptionTemplate" rows="3" placeholder="Automated static site export"></textarea>
                </div>
              </div>
            </div>

          </form>
        </div>
      </div>

      <!-- Empty / Welcome State -->
      <div class="sync-view sync-view-welcome" id="view-welcome" style="display: none;">
        <div class="sync-welcome-content">
          <i class="${font:far()} fa-cloud-upload-alt sync-welcome-icon"></i>
          <h3>Web Sync</h3>
          <p>Generate static snapshots of your site and publish them to Git repositories for hosting on GitHub Pages, GitLab Pages, or any static hosting provider.</p>
          <div class="sync-welcome-actions">
            <button class="button primary radius" id="welcome-snapshots-btn">
              <i class="${font:far()} fa-archive"></i> View Snapshots
            </button>
            <button class="button secondary radius" id="welcome-add-repo-btn">
              <i class="${font:far()} fa-plus"></i> Add Git Repository
            </button>
          </div>
        </div>
      </div>

    </div>
  </div>
</div>

<script>
  document.addEventListener('DOMContentLoaded', function () {
    setupAppsMenu();
    setupEditorAppSwitcher();

    const savedTheme = localStorage.getItem('editor-theme');
    if (savedTheme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      const darkModeIcon = document.querySelector('#dark-mode-toggle-menu i');
      if (darkModeIcon) {
        darkModeIcon.classList.replace('fa-moon', 'fa-sun');
      }
    }
    const darkModeToggleMenu = document.getElementById('dark-mode-toggle-menu');
    if (darkModeToggleMenu) {
      darkModeToggleMenu.addEventListener('click', function(e) {
        e.preventDefault();
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        const iconElement = darkModeToggleMenu.querySelector('i');
        if (isDark) {
          document.documentElement.removeAttribute('data-theme');
          if (iconElement) {
            iconElement.classList.replace('fa-sun', 'fa-moon');
          }
          localStorage.setItem('editor-theme', 'light');
        } else {
          document.documentElement.setAttribute('data-theme', 'dark');
          if (iconElement) {
            iconElement.classList.replace('fa-moon', 'fa-sun');
          }
          localStorage.setItem('editor-theme', 'dark');
        }
      });
    }

    const staticSiteManager = new StaticSiteManager({
      token: '<c:out value="${userSession.formToken}" />',
      generateBtnId: 'generate-static-site-btn',
      publishAllBtnId: 'publish-all-repos-btn',
      saveGitSettingsBtnId: 'save-git-settings-btn',
      publishGitRepoBtnId: 'publish-git-repo-btn',
      deleteGitRepoBtnId: 'delete-git-repo-btn',
      addGitRepoBtnId: 'add-git-repo-btn',
      fileListId: 'static-site-list',
      pollingIndicatorId: 'static-site-polling-indicator',
      gitPublishIndicatorId: 'git-publish-indicator',
      statusMessageId: 'sync-status-message',
      gitReposNavId: 'git-repos-nav',
      noReposItemId: 'no-repos-item',
      listUrl: '${ctx}/json/static-sites/list?action=list',
      generateUrl: '${ctx}/json/static-sites/generate',
      deleteUrl: '${ctx}/json/static-sites/delete',
      downloadUrl: '${ctx}/json/static-sites/download?action=download',
      gitSettingsUrl: '${ctx}/json/static-sites/git-settings?action=GET_GIT_SETTINGS',
      saveGitSettingsUrl: '${ctx}/json/static-sites/save-git-settings',
      publishGitUrl: '${ctx}/json/static-sites/publish-git'
    });
    staticSiteManager.init();
  });
</script>
