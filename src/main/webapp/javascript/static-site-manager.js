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

  function StaticSiteManager(config) {
    this.modal = document.getElementById(config.modalId);
    this.openModalBtn = document.getElementById(config.openModalBtnId);
    this.closeModalBtn = document.getElementById(config.closeModalBtnId);
    this.generateBtn = document.getElementById(config.generateBtnId);
    this.fileList = document.getElementById(config.fileListId).querySelector('tbody');
    this.pollingIndicator = document.getElementById(config.pollingIndicatorId);

    this.listUrl = config.listUrl;
    this.generateUrl = config.generateUrl;
    this.deleteUrl = config.deleteUrl;
    this.downloadUrl = config.downloadUrl;
    this.gitSettingsUrl = config.gitSettingsUrl;
    this.saveGitSettingsUrl = config.saveGitSettingsUrl;
    this.token = config.token;

    this.pollingInterval = 5000; // 5 seconds
    this.pollingTimer = null;
  }

  StaticSiteManager.prototype.init = function () {
    this.openModalBtn.addEventListener('click', this.showModal.bind(this));
    this.closeModalBtn.addEventListener('click', this.hideModal.bind(this));
    this.generateBtn.addEventListener('click', this.generateSite.bind(this));
    this.fileList.addEventListener('click', this.handleFileAction.bind(this));

    // Git settings
    const gitEnabledCheckbox = document.getElementById('git-enabled');
    const gitSettingsFields = document.getElementById('git-settings-fields');
    const saveGitSettingsBtn = document.getElementById('save-git-settings-btn');

    if (gitEnabledCheckbox && gitSettingsFields) {
      gitEnabledCheckbox.addEventListener('change', function () {
        gitSettingsFields.style.display = this.checked ? 'block' : 'none';
      });
    }

    if (saveGitSettingsBtn) {
      saveGitSettingsBtn.addEventListener('click', this.saveGitSettings.bind(this));
    }
  };

  StaticSiteManager.prototype.showModal = function () {
    this.modal.style.display = 'flex';
    this.loadFileList();
    this.loadGitSettings();
  };

  StaticSiteManager.prototype.hideModal = function () {
    this.modal.style.display = 'none';
    if (this.pollingTimer) {
      clearTimeout(this.pollingTimer);
      this.pollingTimer = null;
    }
  };

  StaticSiteManager.prototype.loadFileList = function () {
    fetch(this.listUrl + '&token=' + encodeURIComponent(this.token))
      .then(response => response.json())
      .then(data => {
        this.renderFileList(data.files);
        const isGenerating = data.isGenerating;
        if (isGenerating) {
          this.startPolling();
        } else {
          this.stopPolling();
        }
      })
      .catch(error => {
        console.error('Error loading static site list:', error);
        this.fileList.innerHTML = '<tr><td colspan="4">Error loading files.</td></tr>';
      });
  };

  StaticSiteManager.prototype.renderFileList = function (files) {
    this.fileList.innerHTML = '';
    if (!files || files.length === 0) {
      this.fileList.innerHTML = '<tr><td colspan="4">No static site archives found.</td></tr>';
      return;
    }
    files.forEach(file => {
      const row = document.createElement('tr');
      row.innerHTML = `
        <td>${file.name}</td>
        <td>${file.size}</td>
        <td>${new Date(file.modified).toLocaleString()}</td>
        <td>
          <a href="${this.downloadUrl}&token=${encodeURIComponent(this.token)}&file=${encodeURIComponent(file.name)}" class="button tiny primary radius" download>Download</a>
          <button class="button tiny alert radius" data-action="delete" data-file="${file.name}">Delete</button>
        </td>
      `;
      this.fileList.appendChild(row);
    });
  };

  StaticSiteManager.prototype.handleFileAction = function (event) {
    const target = event.target;
    const action = target.dataset.action;
    const fileName = target.dataset.file;

    if (action === 'delete') {
      if (confirm(`Are you sure you want to delete ${fileName}?`)) {
        this.deleteFile(fileName);
      }
    }
  };

  StaticSiteManager.prototype.deleteFile = function (fileName) {
    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('action', 'delete');
    formData.append('file', fileName);

    fetch(this.deleteUrl, {
      method: 'POST',
      body: formData
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'success') {
          this.loadFileList();
        } else {
          alert('Error deleting file: ' + data.message);
        }
      })
      .catch(error => {
        console.error('Error deleting file:', error);
        alert('An error occurred while deleting the file.');
      });
  };

  StaticSiteManager.prototype.generateSite = function () {
    this.startPolling();
    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('action', 'generate');

    fetch(this.generateUrl, {
      method: 'POST',
      body: formData
    })
      .then(response => response.json())
      .then(data => {
        if (data.status !== 'success') {
          this.stopPolling();
          alert('Error starting site generation: ' + data.message);
        }
      })
      .catch(error => {
        this.stopPolling();
        console.error('Error starting site generation:', error);
        alert('An error occurred while starting the site generation.');
      });
  };

  StaticSiteManager.prototype.startPolling = function () {
    this.pollingIndicator.style.display = 'block';
    this.generateBtn.disabled = true;
    if (!this.pollingTimer) {
      this.pollingTimer = setTimeout(() => this.pollStatus(), this.pollingInterval);
    }
  };

  StaticSiteManager.prototype.stopPolling = function () {
    this.pollingIndicator.style.display = 'none';
    this.generateBtn.disabled = false;
    if (this.pollingTimer) {
      clearTimeout(this.pollingTimer);
      this.pollingTimer = null;
    }
  };

  StaticSiteManager.prototype.pollStatus = function () {
    fetch(this.listUrl + '&token=' + encodeURIComponent(this.token))
      .then(response => response.json())
      .then(data => {
        this.renderFileList(data.files);
        if (data.isGenerating) {
          // Continue polling
          this.pollingTimer = setTimeout(() => this.pollStatus(), this.pollingInterval);
        } else {
          // Stop polling
          this.stopPolling();
        }
      })
      .catch(error => {
        console.error('Error polling for status:', error);
        this.stopPolling();
      });
  };

  StaticSiteManager.prototype.loadGitSettings = function () {
    if (!this.gitSettingsUrl) {
      return;
    }

    fetch(this.gitSettingsUrl + '&token=' + encodeURIComponent(this.token))
      .then(response => response.json())
      .then(data => {
        // Populate the form
        const gitEnabled = document.getElementById('git-enabled');
        const gitSettingsFields = document.getElementById('git-settings-fields');

        if (gitEnabled) {
          gitEnabled.checked = data.enabled || false;
          if (gitSettingsFields) {
            gitSettingsFields.style.display = gitEnabled.checked ? 'block' : 'none';
          }
        }

        if (data.gitProvider) {
          document.getElementById('git-provider').value = data.gitProvider;
        }
        if (data.repositoryUrl) {
          document.getElementById('repository-url').value = data.repositoryUrl;
        }
        if (data.branchName) {
          document.getElementById('branch-name').value = data.branchName;
        }
        if (data.baseBranch) {
          document.getElementById('base-branch').value = data.baseBranch;
        }
        if (data.username) {
          document.getElementById('git-username').value = data.username;
        }
        if (data.email) {
          document.getElementById('git-email').value = data.email;
        }
        if (data.commitMessageTemplate) {
          document.getElementById('commit-message-template').value = data.commitMessageTemplate;
        }
        if (data.autoCreatePr !== undefined) {
          document.getElementById('auto-create-pr').checked = data.autoCreatePr;
        }
        if (data.prTitleTemplate) {
          document.getElementById('pr-title-template').value = data.prTitleTemplate;
        }
        if (data.prDescriptionTemplate) {
          document.getElementById('pr-description-template').value = data.prDescriptionTemplate;
        }
        if (data.targetDirectory) {
          document.getElementById('target-directory').value = data.targetDirectory;
        }
      })
      .catch(error => {
        console.error('Error loading Git settings:', error);
      });
  };

  StaticSiteManager.prototype.saveGitSettings = function () {
    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('action', 'saveGitSettings');
    formData.append('enabled', document.getElementById('git-enabled').checked ? 'true' : 'false');
    formData.append('gitProvider', document.getElementById('git-provider').value);
    formData.append('repositoryUrl', document.getElementById('repository-url').value);
    formData.append('branchName', document.getElementById('branch-name').value);
    formData.append('baseBranch', document.getElementById('base-branch').value);

    const accessToken = document.getElementById('access-token').value;
    if (accessToken) {
      formData.append('accessToken', accessToken);
    }

    formData.append('username', document.getElementById('git-username').value);
    formData.append('email', document.getElementById('git-email').value);
    formData.append('commitMessageTemplate', document.getElementById('commit-message-template').value);
    formData.append('autoCreatePr', document.getElementById('auto-create-pr').checked ? 'true' : 'false');
    formData.append('prTitleTemplate', document.getElementById('pr-title-template').value);
    formData.append('prDescriptionTemplate', document.getElementById('pr-description-template').value);
    formData.append('targetDirectory', document.getElementById('target-directory').value);

    fetch(this.saveGitSettingsUrl, {
      method: 'POST',
      body: formData
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'success') {
          alert('Git settings saved successfully!');
          // Clear the password field after successful save
          document.getElementById('access-token').value = '';
        } else {
          alert('Error saving Git settings: ' + (data.message || 'Unknown error'));
        }
      })
      .catch(error => {
        console.error('Error saving Git settings:', error);
        alert('An error occurred while saving Git settings.');
      });
  };

  window.StaticSiteManager = StaticSiteManager;
})(window);
