<%--
  ~ Copyright 2022 SimIS Inc.
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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="dataset" class="com.simisinc.platform.domain.model.datasets.Dataset" scope="request"/>
<web:script package="jquery" file="jquery.min.js" />
<script>
  $(document).ready(function() {
    // Initialize request headers from JSON
    initializeRequestHeaders();

    // Show/hide request headers section based on sourceUrl input
    function toggleRequestHeaders() {
      var sourceUrl = $('#sourceUrl').val().trim();
      if (sourceUrl) {
        $('#requestHeadersSection').show();
      } else {
        $('#requestHeadersSection').hide();
      }
    }

    // Toggle on page load
    toggleRequestHeaders();

    // Toggle when sourceUrl changes
    $('#sourceUrl').on('input', function() {
      toggleRequestHeaders();
    });

    // Add header button
    $('#addHeaderBtn').on('click', function() {
      addHeaderRow('', '');
    });

    // Update hidden field before form submission
    $('form').on('submit', function() {
      collectRequestHeaders();
    });
  });

  function initializeRequestHeaders() {
    var requestConfigJson = $('#requestConfigJson').val();
    if (requestConfigJson) {
      try {
        var config = JSON.parse(requestConfigJson);
        if (config && config.headers) {
          var headers = config.headers;
          for (var key in headers) {
            if (headers.hasOwnProperty(key)) {
              addHeaderRow(key, headers[key]);
            }
          }
        }
      } catch (e) {
        console.error('Failed to parse request config:', e);
      }
    }
    // Show empty message if no headers
    if ($('#requestHeadersContainer .header-row').length === 0) {
      showEmptyMessage();
    }
  }

  function addHeaderRow(keyValue, value) {
    // Remove empty message if present
    $('#requestHeadersContainer .empty-headers').remove();

    var row = $('<div class="header-row grid-x grid-margin-x" style="margin-bottom: 10px;"></div>');
    
    var keyCell = $('<div class="small-5 cell"></div>');
    var keyInput = $('<input type="text" class="header-key" placeholder="Header Name" value="' + escapeHtml(keyValue) + '"/>');
    keyCell.append(keyInput);
    
    var valueCell = $('<div class="small-6 cell"></div>');
    var valueInput = $('<input type="text" class="header-value" placeholder="Header Value" value="' + escapeHtml(value) + '"/>');
    valueCell.append(valueInput);
    
    var removeCell = $('<div class="small-1 cell"></div>');
    var removeBtn = $('<button type="button" class="button tiny alert radius remove-header-btn" title="Remove"><i class="fa fa-trash"></i></button>');
    removeBtn.on('click', function() {
      row.remove();
      if ($('#requestHeadersContainer .header-row').length === 0) {
        showEmptyMessage();
      }
    });
    removeCell.append(removeBtn);
    
    row.append(keyCell).append(valueCell).append(removeCell);
    $('#requestHeadersContainer').append(row);
  }

  function showEmptyMessage() {
    var emptyMsg = $('<div class="empty-headers" style="padding: 10px; color: #999; font-style: italic;">No headers configured</div>');
    $('#requestHeadersContainer').append(emptyMsg);
  }

  function collectRequestHeaders() {
    var headers = {};
    $('#requestHeadersContainer .header-row').each(function() {
      var key = $(this).find('.header-key').val().trim();
      var value = $(this).find('.header-value').val().trim();
      if (key && value) {
        headers[key] = value;
      }
    });
    
    var requestConfig = null;
    if (Object.keys(headers).length > 0) {
      requestConfig = JSON.stringify({ headers: headers });
    }
    $('#requestConfigJson').val(requestConfig || '');
  }

  function escapeHtml(text) {
    if (!text) return '';
    var map = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
  }
</script>
<form method="post" enctype="multipart/form-data">
  <%-- Required by controller --%>
  <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <%-- Form values --%>
  <input type="hidden" name="id" value="${dataset.id}"/>
  <input type="hidden" id="requestConfigJson" name="requestConfig" value="<c:out value="${dataset.requestConfig}"/>"/>
  <%-- Title and Message block --%>
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>
  <%-- Form Content --%>
  <label>Name <span class="required">*</span>
    <input type="text" placeholder="Give it a name..." name="name" value="<c:out value="${dataset.name}"/>" required>
  </label>
  <label>Description
    <textarea name="sourceInfo"><c:out value="${dataset.sourceInfo}"/></textarea>
  </label>
  <label>Download data from a URL
    <input type="text" id="sourceUrl" placeholder="http(s)://" name="sourceUrl" value="<c:out value="${dataset.sourceUrl}"/>">
  </label>
  
  <fieldset id="requestHeadersSection" style="display: none;">
    <legend>Request Headers</legend>
    <p class="help-text">Configure HTTP request headers for authentication or custom request configuration (e.g., Authorization tokens, API keys).</p>
    <div id="requestHeadersContainer">
      <!-- Headers will be dynamically added here -->
    </div>
    <button type="button" class="button tiny secondary radius" id="addHeaderBtn">
      <i class="fa fa-plus"></i> Add Header
    </button>
  </fieldset>
  
  <%--<label for="file" class="button radius">Choose File...</label>--%>
  <label>Upload a file
    <input type="file" id="file" name="file" accept="text/csv,.csv,application/json,application/vnd.geo+json,.json,.geojson,text/tab-separated-values,.tsv">
  </label>
  <p class="help-text">File must be a .csv, .tsv, .json, or .geojson</p>
  <label>Dataset Type <span class="required">*</span>
    <select name="fileType">
      <option value="application/json"<c:if test="${dataset.fileType eq 'application/json'}"> selected</c:if>>JSON</option>
      <option value="application/vnd.api+json"<c:if test="${dataset.fileType eq 'application/vnd.api+json'}"> selected</c:if>>JSON API</option>
      <option value="application/vnd.geo+json"<c:if test="${dataset.fileType eq 'application/vnd.geo+json'}"> selected</c:if>>GeoJSON</option>
      <option value="text/csv"<c:if test="${dataset.fileType eq 'text/csv'}"> selected</c:if>>CSV</option>
      <option value="text/tab-separated-values"<c:if test="${dataset.fileType eq 'text/tab-separated-values'}"> selected</c:if>>TSV</option>
      <option value="text/plain"<c:if test="${dataset.fileType eq 'text/plain'}"> selected</c:if>>Plain Text List</option>
      <option value="application/rss+xml"<c:if test="${dataset.fileType eq 'application/rss+xml'}"> selected</c:if>>RSS+XML</option>
    </select>
  </label>
  <div class="button-container">
    <input type="submit" class="button radius success" value="Add Dataset"/>
    <a class="button radius secondary" href="${ctx}/admin/datasets">Cancel</a>
  </div>
</form>