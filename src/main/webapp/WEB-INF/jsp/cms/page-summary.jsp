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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="text" uri="/WEB-INF/tlds/text-functions.tld" %>
<%@ taglib prefix="user" uri="/WEB-INF/tlds/user-functions.tld" %>
<jsp:useBean id="webPage" class="com.simisinc.platform.domain.model.cms.WebPage" scope="request"/>
<c:if test="${!empty webPage}">
  <style>
    /* Page History Modal Overlay */
    #pageHistoryModal {
      display: none;
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.5);
      z-index: 10000;
      align-items: center;
      justify-content: center;
    }
    
    #pageHistoryModal.open {
      display: flex;
    }

    /* Inner modal content container */
    .page-history-modal-content {
      position: relative;
      background-color: white;
      padding: 20px;
      width: 90%;
      max-width: 900px;
      border-radius: 5px;
      max-height: 75vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    #pageHistoryContent {
      overflow-y: auto;
      max-height: calc(75vh - 100px);
      flex: 1;
    }
    
    .page-history-error {
      text-align: center;
      color: #ff6b6b;
      padding: 30px;
    }
    .page-history-empty {
      text-align: center;
      color: #b0b0b0;
      padding: 30px;
    }
    .page-history-loading {
      text-align: center;
      color: #999;
      padding: 30px;
    }
  </style>

  <div class="page-summary-widget no-gap-all" style="position: relative;">
    <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 20px;">
      <div style="flex: 1;">
        <h1 style="margin-top: 0;"><c:out value="${webPage.title}"/></h1>
        <p style="font-size: 0.95em;">
          <c:if test="${webPage.modifiedBy > 0}">
            Modified by <c:out value="${user:name(webPage.modifiedBy)}" />
            on <fmt:formatDate value="${webPage.modified}" pattern="MMM d, yyyy"/>
          </c:if>
          <c:if test="${webPage.draft}"><span class="badge warning">Draft</span></c:if>
          <c:if test="${!webPage.enabled}"><span class="badge alert">Archived</span></c:if>
        </p>
      </div>
      <div style="flex-shrink: 0; margin-top: 8px;">
        <p style="font-size: 0.95em;">
          <a id="pageHistoryToggleBtn" href="javascript:void(0);">View Page History</a>        
        </p>
      </div>
    </div>
  </div>

  <!-- Page History Modal -->
  <div id="pageHistoryModal">
    <div class="page-history-modal-content">
      <!-- Modal Header -->
      <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #ddd; padding-bottom: 15px; margin-bottom: 15px; flex-shrink: 0;">
        <h3 style="margin: 0;"><i class="fa fa-history"></i> Page History</h3>
        <button id="pageHistoryCloseBtn" class="close-button" aria-label="Close modal" type="button" style="background: none; border: none; font-size: 24px; cursor: pointer; color: #999; padding: 0; width: 30px; height: 30px;">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
      <!-- Modal Content (Activity List) -->
      <div id="pageHistoryContent">
        <p class="page-history-loading">Loading activity history...</p>
      </div>
    </div>
  </div>

  <!-- JavaScript for Page History Modal -->
  <script>
    (function() {
      var toggleBtn = document.getElementById('pageHistoryToggleBtn');
      var modal = document.getElementById('pageHistoryModal');
      var closeBtn = document.getElementById('pageHistoryCloseBtn');
      var contentDiv = document.getElementById('pageHistoryContent');
      var ctx = '${systemPropertyMap["system.www.context"]}';
      var pageId = '${webPage.link}';

      function openPageHistory() {
        modal.classList.add('open');
        document.body.style.overflow = 'hidden';
        loadPageHistory();
      }

      function closePageHistory() {
        modal.classList.remove('open');
        document.body.style.overflow = 'auto';
      }

      function loadPageHistory() {
        fetch(ctx + '/json/activityListPage?pageId=' + encodeURIComponent(pageId), {
          method: 'GET'
        })
        .then(function(response) { return response.json(); })
        .then(function(data) {
          if (data && data.status === 'error') {
            contentDiv.innerHTML = '<p class="page-history-error">' + (data.error || 'Error loading page history.') + '</p>';
            return;
          }
          var activityList = data && data.data ? data.data : [];
          if (Array.isArray(activityList) && activityList.length > 0) {
            renderActivityHistory(activityList);
          } else {
            contentDiv.innerHTML = '<p class="page-history-empty">No page activity was found yet.<br />Update page content to see activity here.</p>';
          }
        })
        .catch(function(error) {
          console.error('Error loading page history:', error);
          contentDiv.innerHTML = '<p class="page-history-error">Error loading page history. Please try again.</p>';
        });
      }

      function renderActivityHistory(activities) {
        var html = '';
        var lastDate = '---';

        activities.forEach(function(activity) {
          var actDate = new Date(parseInt(activity.created));
          var dateStr = actDate.toLocaleDateString('en-US', {year: 'numeric', month: 'short', day: 'numeric'});

          if (lastDate !== dateStr) {
            html += '<div class="platform-activity-date">' + dateStr + '</div>';
            lastDate = dateStr;
          }

          var timeStr = actDate.toLocaleTimeString('en-US', {hour: 'numeric', minute: '2-digit', hour12: true});
          html += '<ul class="platform-activity-ul" style="margin-bottom: 18px; margin-left: 0; list-style: none;"><li>';
          html += '<div class="platform-activity-image"><img src="' + ctx + '/images/apple-touch-icon.png" alt="avatar" style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover;"/></div>';
          html += '<div class="platform-activity-content">';
          html += '<div class="platform-activity-content-date">' + timeStr + '</div>';
          var messageHtml = (activity.messageHtml || activity.messageText || '');
          // Split by ':' and take only the first part to remove the page link
          var parts = messageHtml.split(':');
          messageHtml = parts[0];
          html += '<div class="platform-activity-content-other">' + messageHtml + '</div>';
          html += '</div></li></ul>';
        });

        contentDiv.innerHTML = html;
      }

      if (toggleBtn) {
        toggleBtn.addEventListener('click', function(e) {
          e.preventDefault();
          openPageHistory();
        });
      }
      if (closeBtn) {
        closeBtn.addEventListener('click', function(e) {
          e.preventDefault();
          closePageHistory();
        });
      }

      // Close modal on ESC key
      document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && modal.classList.contains('open')) {
          closePageHistory();
        }
      });

      // Close modal when clicking on overlay (outside the modal content)
      modal.addEventListener('click', function(event) {
        if (event.target === modal) {
          closePageHistory();
        }
      });
    })();
  </script>
</c:if>
