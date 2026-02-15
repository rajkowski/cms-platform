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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="html" uri="/WEB-INF/tlds/html-functions.tld" %>
<%@ taglib prefix="text" uri="/WEB-INF/tlds/text-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="contentList" class="java.util.ArrayList" scope="request"/>
<%-- Include the formatting for when TinyMCE uses an iFrame to open the content browser --%>
<style>
  body {
    font-family: system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", "Liberation Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
    font-size: 1rem;
    font-weight: 400;
    line-height: 1.5;
    -webkit-text-size-adjust: 100%;
    -webkit-tap-highlight-color: transparent;
  }

  .grid-container {
    padding-right: 0.625rem;
    padding-left: 0.625rem;
    max-width: 75rem;
    margin: 0 auto;
  }

  .content-browser-item {
    padding: 10px;
    margin: 5px 0;
    border: 1px solid #e6e6e6;
    border-radius: 4px;
    background: #fefefe;
    cursor: pointer;
    transition: background-color 0.2s;
  }

  .content-browser-item:hover {
    background-color: #f0f0f0;
  }

  .content-browser-item-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 5px;
  }

  .content-browser-item-title {
    font-weight: bold;
    font-size: 1.1em;
    color: #333;
  }

  .content-browser-item-id {
    font-size: 0.9em;
    color: #666;
    font-family: monospace;
  }

  .content-browser-item-preview {
    font-size: 0.9em;
    color: #555;
    margin-top: 5px;
    line-height: 1.4;
  }

  .search-box {
    width: 100%;
    padding: 8px 12px;
    margin-bottom: 15px;
    border: 1px solid #ccc;
    border-radius: 4px;
    font-size: 1em;
  }

  .no-content-message {
    text-align: center;
    padding: 40px;
    color: #999;
  }
</style>
<div class="grid-container">
  <input type="text" id="contentSearchBox" class="search-box" placeholder="Search content blocks..." onkeyup="filterContent()">
  
  <c:if test="${empty contentList}">
    <div class="no-content-message">
      <p>No content blocks were found.</p>
    </div>
  </c:if>
  
  <div id="contentBrowserList">
    <c:forEach items="${contentList}" var="content" varStatus="status">
      <div class="content-browser-item" 
           onclick="mySubmit(this.dataset.uniqueid)" 
           data-uniqueid="<c:out value="${content.uniqueId}"/>"
           data-searchtext="<c:out value="${fn:toLowerCase(content.uniqueId)}"/>">
        <div class="content-browser-item-header">
          <span class="content-browser-item-title"><c:out value="${content.uniqueId}"/></span>
          <span class="content-browser-item-id">${"$"}{uniqueId:<c:out value="${content.uniqueId}"/>}</span>
        </div>
        <c:if test="${!empty content.content}">
          <div class="content-browser-item-preview">
            <c:set var="contentPreview" value="${html:text(content.content)}"/>
            <c:choose>
              <c:when test="${fn:length(contentPreview) > 150}">
                <c:out value="${fn:substring(contentPreview, 0, 150)}"/>...
              </c:when>
              <c:otherwise>
                <c:out value="${contentPreview}"/>
              </c:otherwise>
            </c:choose>
          </div>
        </c:if>
      </div>
    </c:forEach>
  </div>
</div>
<script>
  function filterContent() {
    var input = document.getElementById('contentSearchBox');
    var filter = input.value.toLowerCase();
    var items = document.getElementsByClassName('content-browser-item');
    
    for (var i = 0; i < items.length; i++) {
      var searchText = items[i].getAttribute('data-searchtext');
      if (searchText.indexOf(filter) > -1) {
        items[i].style.display = '';
      } else {
        items[i].style.display = 'none';
      }
    }
  }

  <c:choose>
    <c:when test="${!empty inputId}">
      <%-- Directly called by a web page --%>
      function mySubmit(uniqueId) {
        top.document.getElementById("<c:out value="${inputId}" />").value = '${"{"}$uniqueId:' + uniqueId + '}';
        $('#contentBrowserReveal').foundation('close');
      }
    </c:when>
    <c:otherwise>
      <%-- Called by TinyMCE --%>
      function mySubmit(uniqueId) {
        window.parent.postMessage({
            mceAction: 'ContentBlockSelected',
            content: uniqueId
        }, '*');
      }
    </c:otherwise>
  </c:choose>
</script>
