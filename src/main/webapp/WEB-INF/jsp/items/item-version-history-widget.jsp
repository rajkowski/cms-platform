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
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="item" class="com.simisinc.platform.domain.model.items.Item" scope="request"/>
<jsp:useBean id="title" class="java.lang.String" scope="request"/>
<c:if test="${!empty title}">
  <h4><c:out value="${title}"/></h4>
</c:if>
<div id="itemVersionHistoryContainer${widgetContext.uniqueId}">
  <div id="itemVersionHistoryList${widgetContext.uniqueId}"><p>Loading...</p></div>
</div>
<script>
  function escapeItemVersionHtml(input) {
    if (input === null || input === undefined) {
      return '';
    }
    return String(input)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
  }

  function loadItemVersions${widgetContext.uniqueId}() {
    var target = $('#itemVersionHistoryList${widgetContext.uniqueId}');
    target.html('<p>Loading...</p>');

    $.ajax({
      url: '${ctx}/json/itemVersions?itemId=${item.id}',
      cache: false,
      dataType: 'json'
    }).done(function (versions) {
      var versionList = [];
    if (Array.isArray(versions)) {
      versionList = versions;
    } else if (versions && Array.isArray(versions.data)) {
      versionList = versions.data;
    }

    if (versionList.length === 0) {
        target.html('<p>No previous versions were found.</p>');
        return;
      }

      var html = '<ul class="no-bullet item-version-history-list">';
      versionList.forEach(function (version) {
        var payload = '';
        if (version.versionData) {
          try {
            payload = JSON.stringify(JSON.parse(version.versionData), null, 2);
          } catch (e) {
            payload = version.versionData;
          }
        }
        html += '<li class="callout secondary">'
            + '<div><strong>By:</strong> ' + escapeItemVersionHtml(version.createdByName || '') + '</div>'
            + '<div><strong>Date:</strong> ' + escapeItemVersionHtml(version.created || '') + '</div>'
            + '<div class="margin-top-10">'
            + '<a class="button tiny secondary no-gap margin-right-5 js-preview-item-version" href="#" data-preview-id="previewItemVersion${widgetContext.uniqueId}_' + version.versionId + '">Preview</a>'
            + '<a class="button tiny alert no-gap" onclick="restoreItemVersion${widgetContext.uniqueId}(' + version.versionId + ')">Restore</a>'
            + '</div>'
            + '<div class="callout secondary hide margin-top-10" id="previewItemVersion${widgetContext.uniqueId}_' + version.versionId + '"><pre style="white-space: pre-wrap; max-height: 320px; overflow: auto;">' + escapeItemVersionHtml(payload) + '</pre></div>'
            + '</li>';
      });
      html += '</ul>';
      target.html(html);
    }).fail(function (xhr) {
      var message = xhr && xhr.responseText ? xhr.responseText : 'Unable to load version history';
      target.html('<div class="callout alert">' + escapeItemVersionHtml(message) + '</div>');
    });
  }

  function restoreItemVersion${widgetContext.uniqueId}(versionId) {
    if (!confirm('Restore this version? The current item state will be saved as a new version before restore.')) {
      return;
    }
    $.ajax({
      type: 'POST',
      url: '${ctx}/json/itemVersionRestore',
      dataType: 'json',
      data: {
        itemId: '${item.id}',
        versionId: versionId,
        token: '${userSession.formToken}'
      }
    }).done(function () {
      window.location.reload();
    }).fail(function (xhr) {
      var message = xhr && xhr.responseText ? xhr.responseText : 'Unable to restore version';
      alert(message);
    });
  };

  loadItemVersions${widgetContext.uniqueId}();
  
  $('#itemVersionHistoryList${widgetContext.uniqueId}').on('click', '.js-preview-item-version', function (e) {
    e.preventDefault();
    var previewId = $(this).attr('data-preview-id');
    $('[id="' + previewId + '"]').toggleClass('hide');
  });
</script>
