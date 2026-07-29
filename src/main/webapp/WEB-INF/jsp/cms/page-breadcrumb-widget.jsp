<%--
  ~ Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
  ~ Page Breadcrumb Widget
  ~ Displays the hierarchical breadcrumb trail of parent pages
  ~
  ~ Widget Configuration:
  ~ - showRootPage: Boolean (default: true) - Show root page in breadcrumb
  ~ - separator: String (default: " / ") - Separator between breadcrumb items
  ~ - maxItems: Integer (default: 10) - Maximum breadcrumb items to display
--%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="com.simisinc.platform.domain.model.cms.WebPage" %>
<%@ page import="java.util.List" %>
<c:set var="breadcrumbItems" value="${requestScope.breadcrumbItems}" />
<c:set var="showRootPage" value="${requestScope.showRootPage != null ? requestScope.showRootPage : true}" />
<c:set var="separator" value="${requestScope.separator != null ? requestScope.separator : ' / '}" />
<c:if test="${not empty breadcrumbItems or showRootPage}">
  <nav aria-label="Breadcrumbs">
    <ul class="breadcrumbs">
      <c:if test="${showRootPage}">
        <li>
          <a href="/">Home</a>
        </li>
      </c:if>
      <c:forEach var="ancestor" items="${breadcrumbItems}">
        <li>
          <c:choose>
            <c:when test="${'/' eq ancestor.link and empty ancestor.title}">
              <a href="<c:out value="${ancestor.link}" />">Home</a>
            </c:when>
            <c:otherwise>
              <a href="<c:out value="${ancestor.link}" />"><c:out value="${ancestor.title}" /></a>
            </c:otherwise>
          </c:choose>
        </li>
      </c:forEach>
      <li aria-current="page">
        <span class="show-for-sr">Current: </span><c:out value="${currentPage.title}" />
      </li>
    </ul>
  </nav>
</c:if>
