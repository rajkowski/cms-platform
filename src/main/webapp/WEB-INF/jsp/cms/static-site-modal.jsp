<%--
  ~ Copyright 2026 Matt Rajkowski (https://www.github.com/rajkowski)
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
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<div id="static-site-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 800px;">
    <h4>Static Site Generator</h4>
    <p>Manage and generate static versions of your website.</p>

    <!-- Git Publishing Settings Section -->
    <div id="git-settings-section" style="margin-bottom: 20px; padding: 15px; border: 1px solid #ddd; border-radius: 5px;">
      <h5>Git Publishing Settings</h5>
      <form id="git-settings-form">
        <label>
          <input type="checkbox" id="git-enabled" name="enabled" value="true">
          Enable Git Publishing
        </label>
        <div id="git-settings-fields" style="display: none; margin-top: 15px;">
          <label>Git Provider:
            <select id="git-provider" name="gitProvider">
              <option value="github">GitHub</option>
              <option value="gitlab">GitLab</option>
            </select>
          </label>
          <label>Repository URL:
            <input type="text" id="repository-url" name="repositoryUrl" placeholder="https://github.com/user/repo.git">
          </label>
          <label>Branch Name:
            <input type="text" id="branch-name" name="branchName" placeholder="static-site">
          </label>
          <label>Base Branch:
            <input type="text" id="base-branch" name="baseBranch" placeholder="main">
          </label>
          <label>Access Token:
            <input type="password" id="access-token" name="accessToken" placeholder="Enter token to update">
          </label>
          <label>Username:
            <input type="text" id="git-username" name="username" placeholder="git-user">
          </label>
          <label>Email:
            <input type="email" id="git-email" name="email" placeholder="user@example.com">
          </label>
          <label>Commit Message Template:
            <input type="text" id="commit-message-template" name="commitMessageTemplate" placeholder="Static site update: ${timestamp}">
          </label>
          <label>
            <input type="checkbox" id="auto-create-pr" name="autoCreatePr" value="true" checked>
            Auto-create Pull Request
          </label>
          <label>PR Title Template:
            <input type="text" id="pr-title-template" name="prTitleTemplate" placeholder="Static site update: ${timestamp}">
          </label>
          <label>PR Description Template:
            <textarea id="pr-description-template" name="prDescriptionTemplate" rows="3" placeholder="Automated static site export"></textarea>
          </label>
          <label>Target Directory:
            <input type="text" id="target-directory" name="targetDirectory" placeholder="/">
          </label>
          <button type="button" id="save-git-settings-btn" class="button primary radius">Save Git Settings</button>
        </div>
      </form>
    </div>

    <div id="static-site-list-container">
      <table id="static-site-list">
        <thead>
        <tr>
          <th>File</th>
          <th>Size</th>
          <th>Date</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <%-- AJAX will populate this --%>
        </tbody>
      </table>
      <div id="static-site-polling-indicator" style="display: none; margin-top: 10px;">
        <i class="${font:fas()} fa-spinner fa-spin"></i> Generating new static site snapshot... please wait.
      </div>
    </div>
    <div style="text-align: right; margin-top: 20px; display: flex; gap: 10px; justify-content: flex-end;">
      <button id="generate-static-site-btn" class="button success radius"><i class="${font:far()} fa-cogs"></i> Generate a Site Snapshot</button>
      <button id="close-static-site-modal" class="button secondary radius">Close</button>
    </div>
  </div>
</div>
