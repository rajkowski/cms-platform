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
<jsp:useBean id="diagramList" class="java.util.ArrayList" scope="request"/>
<%-- Include the formatting for when TinyMCE uses an iFrame to open the diagram browser --%>
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

  .diagram-browser-item {
    padding: 10px;
    margin: 5px 0;
    border: 1px solid #e6e6e6;
    border-radius: 4px;
    background: #fefefe;
    cursor: pointer;
    transition: background-color 0.2s;
  }

  .diagram-browser-item:hover {
    background-color: #f0f0f0;
  }

  .diagram-browser-item-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 5px;
  }

  .diagram-browser-item-title {
    font-weight: bold;
    font-size: 1.1em;
    color: #333;
  }

  .diagram-browser-item-webpath {
    font-size: 0.9em;
    color: #666;
    font-family: monospace;
  }

  .diagram-browser-item-details {
    font-size: 0.85em;
    color: #999;
    margin-top: 3px;
  }

  .search-box {
    width: 100%;
    padding: 8px 12px;
    margin-bottom: 15px;
    border: 1px solid #ccc;
    border-radius: 4px;
    font-size: 1em;
  }

  .no-diagram-message {
    text-align: center;
    padding: 40px;
    color: #999;
  }
</style>
<div class="grid-container">
  <h2>Select a Diagram</h2>
  <input type="text" id="diagramSearchBox" class="search-box" placeholder="Search diagrams..." onkeyup="filterDiagram()">
  
  <c:if test="${empty diagramList}">
    <div class="no-diagram-message">
      <p>No diagrams were found.</p>
    </div>
  </c:if>
  
  <div id="diagramBrowserList">
    <c:forEach items="${diagramList}" var="diagram" varStatus="status">
      <div class="diagram-browser-item" 
           onclick="mySubmit(this.dataset.webpath, this.dataset.label)" 
           data-webpath="<c:out value="${diagram.webPath}"/>"
           data-label="<c:out value="${diagram.title}"/>"
           data-searchtext="<c:out value="${fn:toLowerCase(diagram.title)} ${fn:toLowerCase(diagram.filename)} ${fn:toLowerCase(diagram.webPath)}"/>">
        <div class="diagram-browser-item-header">
          <span class="diagram-browser-item-title"><c:out value="${diagram.title != null ? diagram.title : diagram.filename}"/></span>
          <span class="diagram-browser-item-webpath"><c:out value="${diagram.webPath}"/></span>
        </div>
        <div class="diagram-browser-item-details">
          <c:out value="${diagram.filename}"/> 
          <c:if test="${diagram.fileLength > 0}">
            | <fmt:formatNumber value="${diagram.fileLength / 1024}" maxFractionDigits="2"/> KB
          </c:if>
        </div>
      </div>
    </c:forEach>
  </div>
</div>
<script>
  function filterDiagram() {
    var input = document.getElementById('diagramSearchBox');
    var filter = input.value.toLowerCase();
    var items = document.getElementsByClassName('diagram-browser-item');
    
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
      function mySubmit(webPath, label) {
        var token = webPath;
        if (label && label.trim() !== '') {
          token = webPath + ';' + label;
        }
        top.document.getElementById("<c:out value="${inputId}" />").value = '${"$"}{diagram:' + token + '}';
        $('#diagramBrowserReveal').foundation('close');
      }
    </c:when>
    <c:otherwise>
      <%-- Called by TinyMCE --%>
      function mySubmit(webPath, label) {
        var token = webPath;
        if (label && label.trim() !== '') {
          token = webPath + ';' + label;
        }
        window.parent.postMessage({
          mceAction: 'DiagramSelected',
          webPath: webPath,
          label: label,
          token: token
        }, '*');
      }
    </c:otherwise>
  </c:choose>
</script>
