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
  <div class="modal-content" style="max-width: 600px;">
    <h4>Static Site Generator</h4>
    <p>Manage and generate static versions of your website.</p>
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
