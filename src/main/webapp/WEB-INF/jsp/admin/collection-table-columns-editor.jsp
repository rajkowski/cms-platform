<%--
  ~ Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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
<%@ page import="static com.simisinc.platform.ApplicationInfo.VERSION" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="tableColumnsValue" class="java.lang.String" scope="request"/>
<jsp:useBean id="activeTableColumnsList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="inactiveTableColumnsList" class="java.util.ArrayList" scope="request"/>
<script src="${ctx}/javascript/sortablejs-1.15.2/Sortable.min.js"></script>
<style>
  .collection-fields-sortable-list .callout {
    padding: 2rem;
  }
  .collection-fields-sortable-list .card {
    border: 1px solid #000000;
  }
  .collection-fields-sortable-list .card-divider {
    padding: 0 1rem;
  }
</style>
<c:if test="${!empty title}">
  <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}"/></h4>
</c:if>
<%@include file="../page_messages.jspf" %>
<form class="collection-fields-sortable-list" method="post" autocomplete="off">
  <%-- Required by controller --%>
  <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <%-- Form Fields --%>
  <input type="hidden" id="tableColumnsValue" name="tableColumnsValue" value="<c:out value="${tableColumnsValue}" />" />
  <h5>Columns to include</h5>
  <div class="grid-container callout primary">
    <div id="activeTableColumns" class="grid-x grid-margin-x small-up-3 medium-up-4 sortable-area" style="min-height:44px">
      <c:forEach var="field" items="${activeTableColumnsList}">
        <div data-id="<c:out value="${field.name}" />">
          <div class="card margin-right-10">
            <div class="card-divider">
              <c:out value="${field.label}" />
            </div>
          </div>
        </div>
      </c:forEach>
    </div>
  </div>
  <h5>Available columns</h5>
  <div class="grid-container callout box">
    <div id="inactiveTableColumns" class="grid-x grid-margin-x small-up-3 medium-up-4 sortable-area" style="min-height:44px">
      <c:forEach var="field" items="${inactiveTableColumnsList}">
        <div data-id="<c:out value="${field.name}" />">
          <div class="card margin-right-10">
            <div class="card-divider">
              <c:out value="${field.label}" />
            </div>
          </div>
        </div>
      </c:forEach>
    </div>
  </div>
  <div class="button-container gap">
    <input type="submit" class="button radius success" value="Save"/>
    <c:if test="${!empty cancelUrl}"><span class="button-gap"><a class="button radius secondary" href="${ctx}${cancelUrl}">Cancel</a></span></c:if>
  </div>
</form>

<script>
    let el1 = document.getElementById('activeTableColumns');
    let sortableActive = Sortable.create(el1, {
      group: 'shared',
      dataIdAttr: 'data-id',
      animation: 150,
      onSort: function (/**Event*/evt) {
        setActiveFieldList(sortableActive);
      }
    });

    let el2 = document.getElementById('inactiveTableColumns');
    let sortableInactive = Sortable.create(el2, {
      group: 'shared',
      dataIdAttr: 'data-id',
      animation: 150,
    });

    let tableColumnsValueEl = document.getElementById('tableColumnsValue');
    function setActiveFieldList(list) {
      tableColumnsValueEl.value = sortableActive.toArray().join('|');
    }
</script>
