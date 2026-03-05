/*
 * Copyright 2026 Matt Rajkowski (https://www.github.com/rajkowski)
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
(function (window) {
  'use strict';

  /**
   * StaticSiteManager – manages the two-pane Web Sync editor.
   *
   * Left sidebar contains:
   *   - Site Snapshots nav item
   *   - Git Repositories list + add button
   *
   * Main panel switches between:
   *   - view-snapshots  – snapshot list + generate toolbar
   *   - view-git-repo   – git repo form (add / edit)
   *   - view-welcome    – shown on first load before any selection
   */
  function StaticSiteManager(config) {
    // Element refs
    this.generateBtn          = document.getElementById(config.generateBtnId);
    this.publishAllBtn        = document.getElementById(config.publishAllBtnId);
    this.saveGitSettingsBtn   = document.getElementById(config.saveGitSettingsBtnId);
    this.publishGitRepoBtn    = document.getElementById(config.publishGitRepoBtnId);
    this.deleteGitRepoBtn     = document.getElementById(config.deleteGitRepoBtnId);
    this.addGitRepoBtn        = document.getElementById(config.addGitRepoBtnId);
    this.fileListTbody        = document.getElementById(config.fileListId) ? document.getElementById(config.fileListId).querySelector('tbody') : null;
    this.pollingIndicator     = document.getElementById(config.pollingIndicatorId);
    this.gitPublishIndicator  = document.getElementById(config.gitPublishIndicatorId);
    this.statusMessage        = document.getElementById(config.statusMessageId);
    this.gitReposNav          = document.getElementById(config.gitReposNavId);
    this.noReposItem          = document.getElementById(config.noReposItemId);

    // URLs
    this.listUrl              = config.listUrl;
    this.generateUrl          = config.generateUrl;
    this.deleteUrl            = config.deleteUrl;
    this.downloadUrl          = config.downloadUrl;
    this.gitSettingsUrl       = config.gitSettingsUrl;
    this.saveGitSettingsUrl   = config.saveGitSettingsUrl;
    this.publishGitUrl        = config.publishGitUrl;
    this.token                = config.token;

    // State
    this.pollingInterval      = 5000;
    this.pollingTimer         = null;
    this.activeView           = null;
    this.activeRepoId         = null;   // null = "add new" mode
    this.repos                = [];     // [{id, repositoryUrl, branchName, gitProvider, ...}]
  }

  // ─── Init ────────────────────────────────────────────────────────────────────

  StaticSiteManager.prototype.init = function () {
    var self = this;

    // Sidebar navigation
    var navSnapshots = document.getElementById('nav-snapshots');
    if (navSnapshots) {
      navSnapshots.addEventListener('click', function () { self.showView('snapshots'); });
    }

    // Toolbar buttons
    if (this.generateBtn) {
      this.generateBtn.addEventListener('click', this.generateSite.bind(this));
    }
    if (this.publishAllBtn) {
      this.publishAllBtn.addEventListener('click', this.publishToAllRepos.bind(this));
    }
    if (this.saveGitSettingsBtn) {
      this.saveGitSettingsBtn.addEventListener('click', this.saveGitSettings.bind(this));
    }
    if (this.publishGitRepoBtn) {
      this.publishGitRepoBtn.addEventListener('click', this.publishToCurrentRepo.bind(this));
    }
    if (this.deleteGitRepoBtn) {
      this.deleteGitRepoBtn.addEventListener('click', this.deleteCurrentRepo.bind(this));
    }
    if (this.addGitRepoBtn) {
      this.addGitRepoBtn.addEventListener('click', function () { self.showAddRepoView(); });
    }

    // Welcome view shortcut buttons
    var welcomeSnapshotsBtn = document.getElementById('welcome-snapshots-btn');
    if (welcomeSnapshotsBtn) {
      welcomeSnapshotsBtn.addEventListener('click', function () { self.showView('snapshots'); });
    }
    var welcomeAddRepoBtn = document.getElementById('welcome-add-repo-btn');
    if (welcomeAddRepoBtn) {
      welcomeAddRepoBtn.addEventListener('click', function () { self.showAddRepoView(); });
    }

    // Snapshots file list actions
    if (this.fileListTbody) {
      this.fileListTbody.addEventListener('click', this.handleFileAction.bind(this));
    }

    // PR fields visibility toggle
    var autoCreatePr = document.getElementById('auto-create-pr');
    if (autoCreatePr) {
      autoCreatePr.addEventListener('change', this.updatePrFieldsVisibility.bind(this));
    }

    // Load initial data and show snapshots view by default
    this.loadGitRepos(function () {
      self.showView('snapshots');
    });
  };

  // ─── View Management ─────────────────────────────────────────────────────────

  StaticSiteManager.prototype.showView = function (viewName, repoId) {
    // Hide all views
    var views = document.querySelectorAll('.sync-view');
    views.forEach(function (v) { v.style.display = 'none'; });

    // Hide all toolbar panels
    var toolbarViews = document.querySelectorAll('.sync-toolbar-view');
    toolbarViews.forEach(function (t) { t.style.display = 'none'; });

    // Deactivate all sidebar items
    var sidebarItems = document.querySelectorAll('.sync-sidebar-item');
    sidebarItems.forEach(function (item) { item.classList.remove('active'); });

    this.activeView = viewName;

    if (viewName === 'snapshots') {
      document.getElementById('view-snapshots').style.display = 'flex';
      document.getElementById('toolbar-snapshots').style.display = 'flex';
      var navSnapshots = document.getElementById('nav-snapshots');
      if (navSnapshots) { navSnapshots.classList.add('active'); }
      this.loadFileList();
      // Show "Publish All" button if repos are configured
      if (this.publishAllBtn) {
        this.publishAllBtn.style.display = this.repos.length > 0 ? '' : 'none';
      }

    } else if (viewName === 'git-repo') {
      document.getElementById('view-git-repo').style.display = 'flex';
      document.getElementById('toolbar-git-repo').style.display = 'flex';
      // Activate matching sidebar item
      var repoNavItem = document.querySelector('.sync-sidebar-item[data-repo-id="' + repoId + '"]');
      if (repoNavItem) { repoNavItem.classList.add('active'); }
      this.activeRepoId = repoId || null;
      this.loadRepoForm(repoId);

    } else if (viewName === 'welcome') {
      document.getElementById('view-welcome').style.display = 'flex';
    }
  };

  StaticSiteManager.prototype.showAddRepoView = function () {
    // Clear form for new entry
    this.clearRepoForm();
    this.activeRepoId = null;
    document.getElementById('git-repo-view-title').textContent = 'Add Git Repository';
    document.getElementById('git-repo-view-subtitle').textContent = 'Configure a new Git repository for publishing your static site.';

    // Hide publish/delete buttons in add mode
    if (this.publishGitRepoBtn) { this.publishGitRepoBtn.style.display = 'none'; }
    if (this.deleteGitRepoBtn)  { this.deleteGitRepoBtn.style.display  = 'none'; }

    // Deactivate sidebar items and show view
    var sidebarItems = document.querySelectorAll('.sync-sidebar-item');
    sidebarItems.forEach(function (item) { item.classList.remove('active'); });

    var views = document.querySelectorAll('.sync-view');
    views.forEach(function (v) { v.style.display = 'none'; });
    var toolbarViews = document.querySelectorAll('.sync-toolbar-view');
    toolbarViews.forEach(function (t) { t.style.display = 'none'; });

    document.getElementById('view-git-repo').style.display = 'flex';
    document.getElementById('toolbar-git-repo').style.display = 'flex';
    this.activeView = 'git-repo';
  };

  // ─── Sidebar Rendering ───────────────────────────────────────────────────────

  StaticSiteManager.prototype.renderGitReposSidebar = function () {
    var self = this;
    if (!this.gitReposNav) return;

    // Clear existing repo items (keep no-repos item for toggling)
    var existingItems = this.gitReposNav.querySelectorAll('.sync-sidebar-item');
    existingItems.forEach(function (item) { item.remove(); });

    if (this.repos.length === 0) {
      if (this.noReposItem) { this.noReposItem.style.display = ''; }
      return;
    }

    if (this.noReposItem) { this.noReposItem.style.display = 'none'; }

    this.repos.forEach(function (repo) {
      var li = document.createElement('li');
      li.className = 'sync-sidebar-item';
      li.dataset.view = 'git-repo';
      li.dataset.repoId = repo.id;

      var providerIcon = repo.gitProvider === 'gitlab' ? 'fa-gitlab' : 'fa-github';
      var label = self.shortenRepoUrl(repo.repositoryUrl);

      li.innerHTML =
        '<i class="fab ' + providerIcon + '"></i>' +
        '<span title="' + (repo.repositoryUrl || '') + '">' + label + '</span>';

      li.addEventListener('click', function () {
        self.showView('git-repo', repo.id);
      });

      self.gitReposNav.insertBefore(li, self.noReposItem);
    });
  };

  StaticSiteManager.prototype.shortenRepoUrl = function (url) {
    if (!url) { return 'Unnamed Repository'; }
    // Strip protocol and .git suffix
    return url.replace(/^https?:\/\//, '').replace(/\.git$/, '');
  };

  // ─── Git Repos Loading ───────────────────────────────────────────────────────

  StaticSiteManager.prototype.loadGitRepos = function (callback) {
    var self = this;
    if (!this.gitSettingsUrl) {
      if (callback) { callback(); }
      return;
    }

    fetch(this.gitSettingsUrl + '&token=' + encodeURIComponent(this.token))
      .then(function (response) { return response.json(); })
      .then(function (data) {
        self.repos = [];
        // Backend currently returns a single settings object; support both array and single.
        var list = Array.isArray(data) ? data : (data && (data.repositoryUrl || data.enabled !== undefined) ? [data] : []);
        list.forEach(function (item, idx) {
          if (item.repositoryUrl || item.enabled) {
            self.repos.push(Object.assign({ id: item.id || ('repo-' + idx) }, item));
          }
        });
        self.renderGitReposSidebar();
        if (callback) { callback(); }
      })
      .catch(function (error) {
        console.error('Error loading Git repos:', error);
        if (callback) { callback(); }
      });
  };

  // ─── Git Repo Form ────────────────────────────────────────────────────────────

  StaticSiteManager.prototype.loadRepoForm = function (repoId) {
    var repo = this.repos.find(function (r) { return r.id == repoId; });
    if (!repo) {
      this.showAddRepoView();
      return;
    }

    // Populate the title
    document.getElementById('git-repo-view-title').textContent = this.shortenRepoUrl(repo.repositoryUrl);
    document.getElementById('git-repo-view-subtitle').textContent = 'Edit settings for ' + (repo.gitProvider || 'git') + ' repository.';

    // Populate form fields
    document.getElementById('git-repo-id').value              = repo.id || '';
    document.getElementById('git-provider').value             = repo.gitProvider || 'github';
    document.getElementById('repository-url').value           = repo.repositoryUrl || '';
    document.getElementById('branch-name').value              = repo.branchName || 'static-site';
    document.getElementById('base-branch').value              = repo.baseBranch || 'main';
    document.getElementById('target-directory').value         = repo.targetDirectory || '/';
    document.getElementById('git-username').value             = repo.username || '';
    document.getElementById('git-email').value                = repo.email || '';
    document.getElementById('access-token').value             = ''; // never pre-fill
    document.getElementById('commit-message-template').value  = repo.commitMessageTemplate || 'Static site update: ${timestamp}';
    document.getElementById('auto-create-pr').checked         = repo.autoCreatePr !== false;
    document.getElementById('pr-title-template').value        = repo.prTitleTemplate || 'Static site update: ${timestamp}';
    document.getElementById('pr-description-template').value  = repo.prDescriptionTemplate || 'Automated static site export';

    this.updatePrFieldsVisibility();

    // Show publish / delete buttons for existing repos
    if (this.publishGitRepoBtn) { this.publishGitRepoBtn.style.display = ''; }
    if (this.deleteGitRepoBtn)  { this.deleteGitRepoBtn.style.display  = ''; }
  };

  StaticSiteManager.prototype.clearRepoForm = function () {
    document.getElementById('git-repo-id').value              = '';
    document.getElementById('git-provider').value             = 'github';
    document.getElementById('repository-url').value           = '';
    document.getElementById('branch-name').value              = 'static-site';
    document.getElementById('base-branch').value              = 'main';
    document.getElementById('target-directory').value         = '/';
    document.getElementById('git-username').value             = '';
    document.getElementById('git-email').value                = '';
    document.getElementById('access-token').value             = '';
    document.getElementById('commit-message-template').value  = 'Static site update: ${timestamp}';
    document.getElementById('auto-create-pr').checked         = true;
    document.getElementById('pr-title-template').value        = 'Static site update: ${timestamp}';
    document.getElementById('pr-description-template').value  = 'Automated static site export';
    this.updatePrFieldsVisibility();
  };

  StaticSiteManager.prototype.updatePrFieldsVisibility = function () {
    var checked = document.getElementById('auto-create-pr').checked;
    var prTitleGroup = document.getElementById('pr-title-group');
    var prDescGroup  = document.getElementById('pr-description-group');
    if (prTitleGroup) { prTitleGroup.style.display = checked ? '' : 'none'; }
    if (prDescGroup)  { prDescGroup.style.display  = checked ? '' : 'none'; }
  };

  // ─── Save / Delete Git Repo ───────────────────────────────────────────────────

  StaticSiteManager.prototype.saveGitSettings = function () {
    var self = this;
    var formData = new FormData();
    formData.append('token', this.token);
    formData.append('action', 'saveGitSettings');
    formData.append('enabled', 'true');
    formData.append('gitProvider',            document.getElementById('git-provider').value);
    formData.append('repositoryUrl',          document.getElementById('repository-url').value);
    formData.append('branchName',             document.getElementById('branch-name').value);
    formData.append('baseBranch',             document.getElementById('base-branch').value);
    formData.append('targetDirectory',        document.getElementById('target-directory').value);
    formData.append('username',               document.getElementById('git-username').value);
    formData.append('email',                  document.getElementById('git-email').value);
    formData.append('commitMessageTemplate',  document.getElementById('commit-message-template').value);
    formData.append('autoCreatePr',           document.getElementById('auto-create-pr').checked ? 'true' : 'false');
    formData.append('prTitleTemplate',        document.getElementById('pr-title-template').value);
    formData.append('prDescriptionTemplate',  document.getElementById('pr-description-template').value);

    var accessToken = document.getElementById('access-token').value;
    if (accessToken) { formData.append('accessToken', accessToken); }

    var repoId = document.getElementById('git-repo-id').value;
    if (repoId) { formData.append('id', repoId); }

    if (this.saveGitSettingsBtn) { this.saveGitSettingsBtn.disabled = true; }

    fetch(this.saveGitSettingsUrl, { method: 'POST', body: formData })
      .then(function (response) { return response.json(); })
      .then(function (data) {
        if (self.saveGitSettingsBtn) { self.saveGitSettingsBtn.disabled = false; }
        if (data.status === 'success') {
          document.getElementById('access-token').value = '';
          self.showStatusMessage('Settings saved successfully.', 'success');
          // Reload repos list and re-select the repo
          self.loadGitRepos(function () {
            var targetId = data.id || (self.repos.length > 0 ? self.repos[0].id : null);
            if (targetId) { self.showView('git-repo', targetId); }
          });
        } else {
          self.showStatusMessage('Error: ' + (data.message || 'Could not save settings.'), 'error');
        }
      })
      .catch(function (error) {
        if (self.saveGitSettingsBtn) { self.saveGitSettingsBtn.disabled = false; }
        console.error('Error saving Git settings:', error);
        self.showStatusMessage('An error occurred while saving.', 'error');
      });
  };

  StaticSiteManager.prototype.deleteCurrentRepo = function () {
    if (!confirm('Are you sure you want to remove this Git repository configuration?')) { return; }
    var self = this;
    var formData = new FormData();
    formData.append('token', this.token);
    formData.append('action', 'deleteGitSettings');

    fetch(this.saveGitSettingsUrl, { method: 'POST', body: formData })
      .then(function (response) { return response.json(); })
      .then(function (data) {
        if (data.status === 'success') {
          self.showStatusMessage('Repository removed.', 'success');
          self.repos = [];
          self.renderGitReposSidebar();
          self.showView('snapshots');
        } else {
          self.showStatusMessage('Error: ' + (data.message || 'Could not delete repo.'), 'error');
        }
      })
      .catch(function (error) {
        console.error('Error deleting Git repo:', error);
        self.showStatusMessage('An error occurred while deleting.', 'error');
      });
  };

  // ─── Publish ──────────────────────────────────────────────────────────────────

  StaticSiteManager.prototype.publishToCurrentRepo = function () {
    this.publishToRepo(this.activeRepoId);
  };

  StaticSiteManager.prototype.publishToAllRepos = function () {
    var self = this;
    this.repos.forEach(function (repo) { self.publishToRepo(repo.id); });
  };

  StaticSiteManager.prototype.publishToRepo = function (repoId) {
    var self = this;
    if (!this.publishGitUrl) {
      self.showStatusMessage('Publish endpoint not configured.', 'error');
      return;
    }
    if (this.gitPublishIndicator) { this.gitPublishIndicator.style.display = ''; }
    if (this.publishGitRepoBtn)   { this.publishGitRepoBtn.disabled = true; }

    var formData = new FormData();
    formData.append('token', this.token);
    formData.append('action', 'publishGit');
    if (repoId) { formData.append('id', repoId); }

    fetch(this.publishGitUrl, { method: 'POST', body: formData })
      .then(function (response) { return response.json(); })
      .then(function (data) {
        if (self.gitPublishIndicator) { self.gitPublishIndicator.style.display = 'none'; }
        if (self.publishGitRepoBtn)   { self.publishGitRepoBtn.disabled = false; }
        if (data.status === 'success') {
          self.showStatusMessage('Published successfully to repository.', 'success');
        } else {
          self.showStatusMessage('Publish error: ' + (data.message || 'Unknown error.'), 'error');
        }
      })
      .catch(function (error) {
        if (self.gitPublishIndicator) { self.gitPublishIndicator.style.display = 'none'; }
        if (self.publishGitRepoBtn)   { self.publishGitRepoBtn.disabled = false; }
        console.error('Error publishing to Git:', error);
        self.showStatusMessage('An error occurred while publishing.', 'error');
      });
  };

  // ─── Snapshot File List ───────────────────────────────────────────────────────

  StaticSiteManager.prototype.loadFileList = function () {
    var self = this;
    fetch(this.listUrl + '&token=' + encodeURIComponent(this.token))
      .then(function (response) { return response.json(); })
      .then(function (data) {
        self.renderFileList(data.files);
        if (data.isGenerating) {
          self.startPolling();
        } else {
          self.stopPolling();
        }
      })
      .catch(function (error) {
        console.error('Error loading static site list:', error);
        if (self.fileListTbody) {
          self.fileListTbody.innerHTML = '<tr><td colspan="4">Error loading snapshots.</td></tr>';
        }
      });
  };

  StaticSiteManager.prototype.renderFileList = function (files) {
    if (!this.fileListTbody) { return; }
    this.fileListTbody.innerHTML = '';
    if (!files || files.length === 0) {
      this.fileListTbody.innerHTML = '<tr><td colspan="4" class="sync-empty-state">No site snapshots found. Click <strong>Generate a Site Snapshot</strong> to create one.</td></tr>';
      return;
    }
    var self = this;
    files.forEach(function (file) {
      var row = document.createElement('tr');
      row.innerHTML =
        '<td class="sync-file-name"><i class="far fa-file-archive"></i> ' + file.name + '</td>' +
        '<td>' + file.size + '</td>' +
        '<td>' + new Date(file.modified).toLocaleString() + '</td>' +
        '<td class="sync-file-actions">' +
          '<a href="' + self.downloadUrl + '&token=' + encodeURIComponent(self.token) + '&file=' + encodeURIComponent(file.name) + '" class="button tiny primary radius" download>' +
            '<i class="far fa-download"></i> Download' +
          '</a>' +
          '<button class="button tiny alert radius" data-action="delete" data-file="' + file.name + '">' +
            '<i class="far fa-trash"></i> Delete' +
          '</button>' +
        '</td>';
      self.fileListTbody.appendChild(row);
    });
  };

  StaticSiteManager.prototype.handleFileAction = function (event) {
    var target = event.target.closest('button[data-action]');
    if (!target) { return; }
    var action   = target.dataset.action;
    var fileName = target.dataset.file;
    if (action === 'delete') {
      if (confirm('Are you sure you want to delete ' + fileName + '?')) {
        this.deleteFile(fileName);
      }
    }
  };

  StaticSiteManager.prototype.deleteFile = function (fileName) {
    var self = this;
    var formData = new FormData();
    formData.append('token', this.token);
    formData.append('action', 'delete');
    formData.append('file', fileName);

    fetch(this.deleteUrl, { method: 'POST', body: formData })
      .then(function (response) { return response.json(); })
      .then(function (data) {
        if (data.status === 'success') {
          self.loadFileList();
        } else {
          self.showStatusMessage('Error deleting file: ' + data.message, 'error');
        }
      })
      .catch(function (error) {
        console.error('Error deleting file:', error);
        self.showStatusMessage('An error occurred while deleting the file.', 'error');
      });
  };

  // ─── Site Generation ──────────────────────────────────────────────────────────

  StaticSiteManager.prototype.generateSite = function () {
    var self = this;
    this.startPolling();
    var formData = new FormData();
    formData.append('token', this.token);
    formData.append('action', 'generate');

    fetch(this.generateUrl, { method: 'POST', body: formData })
      .then(function (response) { return response.json(); })
      .then(function (data) {
        if (data.status !== 'success') {
          self.stopPolling();
          self.showStatusMessage('Error starting generation: ' + data.message, 'error');
        }
      })
      .catch(function (error) {
        self.stopPolling();
        console.error('Error starting site generation:', error);
        self.showStatusMessage('An error occurred while starting generation.', 'error');
      });
  };

  StaticSiteManager.prototype.startPolling = function () {
    if (this.pollingIndicator) { this.pollingIndicator.style.display = ''; }
    if (this.generateBtn)      { this.generateBtn.disabled = true; }
    if (!this.pollingTimer) {
      var self = this;
      this.pollingTimer = setTimeout(function () { self.pollStatus(); }, this.pollingInterval);
    }
  };

  StaticSiteManager.prototype.stopPolling = function () {
    if (this.pollingIndicator) { this.pollingIndicator.style.display = 'none'; }
    if (this.generateBtn)      { this.generateBtn.disabled = false; }
    if (this.pollingTimer) {
      clearTimeout(this.pollingTimer);
      this.pollingTimer = null;
    }
  };

  StaticSiteManager.prototype.pollStatus = function () {
    var self = this;
    fetch(this.listUrl + '&token=' + encodeURIComponent(this.token))
      .then(function (response) { return response.json(); })
      .then(function (data) {
        self.renderFileList(data.files);
        if (data.isGenerating) {
          self.pollingTimer = setTimeout(function () { self.pollStatus(); }, self.pollingInterval);
        } else {
          self.stopPolling();
        }
      })
      .catch(function (error) {
        console.error('Error polling for status:', error);
        self.stopPolling();
      });
  };

  // ─── Status Messages ─────────────────────────────────────────────────────────

  StaticSiteManager.prototype.showStatusMessage = function (msg, type) {
    if (!this.statusMessage) { return; }
    this.statusMessage.textContent = msg;
    this.statusMessage.className   = 'sync-status-message sync-status-' + (type || 'info');
    this.statusMessage.style.display = '';
    var self = this;
    clearTimeout(this._statusTimer);
    this._statusTimer = setTimeout(function () {
      self.statusMessage.style.display = 'none';
    }, 5000);
  };

  window.StaticSiteManager = StaticSiteManager;
})(window);
