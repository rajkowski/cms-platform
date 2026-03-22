<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="permissionGroupList" class="java.util.ArrayList" scope="request"/>
<c:if test="${!empty title}">
  <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
</c:if>
<%@include file="../page_messages.jspf" %>
<c:choose>
  <c:when test="${empty permissionGroupList}">
    <p class="subheader">No permission groups are currently loaded.</p>
  </c:when>
  <c:otherwise>
    <c:forEach items="${permissionGroupList}" var="group">
      <div class="card" style="margin-bottom:1rem;">
        <div class="card-divider">
          <strong><c:out value="${group.code}" /></strong>
          <c:if test="${!empty group.name && group.name != group.code}">
            &mdash; <c:out value="${group.name}" />
          </c:if>
        </div>
        <div class="card-section">
          <p><strong>Cedar Policy</strong></p>
          <pre style="font-size:0.8rem;background:#f4f4f4;padding:0.5rem;overflow-x:auto;"><c:out value="${group.cedarPolicyText}" /></pre>
          <p><strong>Components</strong></p>
          <table class="unstriped" style="font-size:0.85rem;">
            <thead>
              <tr>
                <th>Class</th>
                <th width="80">Type</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach items="${group.memberClassNames}" var="className" varStatus="status">
              <tr>
                <td><code><c:out value="${className}" /></code></td>
                <td><c:out value="${group.memberTypes[status.index]}" /></td>
              </tr>
              </c:forEach>
              <c:if test="${empty group.memberClassNames}">
              <tr><td colspan="2">No members registered</td></tr>
              </c:if>
            </tbody>
          </table>
        </div>
      </div>
    </c:forEach>
  </c:otherwise>
</c:choose>
