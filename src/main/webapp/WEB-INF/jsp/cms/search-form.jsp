<%--
  ~ Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="url" uri="/WEB-INF/tlds/url-functions.tld" %>
<%@ taglib prefix="text" uri="/WEB-INF/tlds/text-functions.tld" %>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="placeholder" class="java.lang.String" scope="request"/>
<jsp:useBean id="linkText" class="java.lang.String" scope="request"/>
<jsp:useBean id="expand" class="java.lang.String" scope="request"/>
<c:if test="${!empty title}">
  <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
</c:if>
<c:if test="${expand eq 'true'}">
<style>
    #form${widgetContext.uniqueId} input[type=search] {
        display: none;
        position: absolute;
        top: 75px;
        left: 50%;
        -webkit-transform: translateX(-50%);
        -ms-transform: translateX(-50%);
        transform: translateX(-50%);
        height: 45px;
        max-width: 500px;
        width: 100%;
        font-size: large;
        border-radius: 12px;
        padding: 2px 12px 2px 10px;
        z-index:10000;
    }
    #form${widgetContext.uniqueId} input[type=search].isExpanded {
        display: unset;
    }
    #form${widgetContext.uniqueId} .button.search {
        height: 24px;
        margin: 5px 0 0 0;
        background-color: transparent;
        padding: 2px;
        border: none;
    }
</style>
</c:if>
<form id="form${widgetContext.uniqueId}" method="get" action="${ctx}/search" style="padding-bottom: 20px;">
  <div class="input-group no-gap">
    <input id="input${widgetContext.uniqueId}" class="input-group-field" type="search"<c:if test="${expand ne 'true'}"> placeholder="<c:out value="${placeholder}" />"</c:if> name="query" value="<c:out value='${param.query}'/>">
    <%-- Preserve filter parameters from URL if they exist --%>
    <c:if test="${!empty param.ofType}">
      <input type="hidden" name="ofType" value="<c:out value='${param.ofType}'/>">
    </c:if>
    <c:if test="${!empty param.label}">
      <input type="hidden" name="label" value="<c:out value='${param.label}'/>">
    </c:if>
    <c:if test="${!empty param.contributorFilter}">
      <input type="hidden" name="contributorFilter" value="<c:out value='${param.contributorFilter}'/>">
    </c:if>
    <c:if test="${!empty param.dateFilterType}">
      <input type="hidden" name="dateFilterType" value="<c:out value='${param.dateFilterType}'/>">
    </c:if>
    <c:if test="${!empty param.modifiedAfter}">
      <input type="hidden" name="modifiedAfter" value="<c:out value='${param.modifiedAfter}'/>">
    </c:if>
    <c:if test="${!empty param.modifiedBefore}">
      <input type="hidden" name="modifiedBefore" value="<c:out value='${param.modifiedBefore}'/>">
    </c:if>
    <div class="input-group-button">
      <button id="button${widgetContext.uniqueId}" type="submit" class="button search"><i id="icon${widgetContext.uniqueId}" class="fa fa-search"></i><c:out value="${linkText}" /></button>
    </div>
  </div>
</form>
<script>
  // Verify filter parameters are in the form (server-side JSP adds them)
  (function() {
    function verifyFormInputs() {
      const form = document.getElementById('form${widgetContext.uniqueId}');
      if (!form) {
        console.error('Search form not found: form${widgetContext.uniqueId}');
        return;
      }
      
      console.log('=== Search Form Verification ===');
      console.log('Current URL:', window.location.href);
      
      // List all form inputs
      const allInputs = form.querySelectorAll('input');
      console.log('Form has ' + allInputs.length + ' inputs:');
      allInputs.forEach(function(input) {
        console.log('  ' + input.name + ' = ' + input.value + ' (type: ' + input.type + ')');
      });
      
      console.log('=== Form ready to submit ===');
    }
    
    // Initialize immediately if DOM is ready, otherwise wait
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', verifyFormInputs);
    } else {
      verifyFormInputs();
    }
  })();
</script>
<c:if test="${expand eq 'true'}">
<script>
    $(document).ready(function () {
        let form = $('#form${widgetContext.uniqueId}');
        let button = $('#button${widgetContext.uniqueId}');
        let input = $('#input${widgetContext.uniqueId}');
        let icon = $('#icon${widgetContext.uniqueId}');
        function showSearchForm${widgetContext.uniqueId}() {
            input.addClass('isExpanded');
            input.attr("placeholder", "${js:escape(placeholder)}");
            input.focus();
        }
        function hideSearchForm${widgetContext.uniqueId}() {
            input.removeClass('isExpanded');
            input.attr("placeholder", "");
        }
        button.click(function (event) {
            if (!input.hasClass('isExpanded')) {
                showSearchForm${widgetContext.uniqueId}();
                event.preventDefault(); // Prevent form submission when expanding
            } else {
                hideSearchForm${widgetContext.uniqueId}();
            }
        });
        input.focusout(function () {
            setTimeout(function () {
                hideSearchForm${widgetContext.uniqueId}();
            }, 150);
        });
        form.submit(function(e){
            if (!input.val()) {
                e.preventDefault(e);
            }
        });
    });
</script>
</c:if>