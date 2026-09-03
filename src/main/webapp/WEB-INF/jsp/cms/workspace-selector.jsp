<%--
  Copyright 2026 Matt Rajkowski

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Workspace</title>
</head>
<body>
  <main>
    <c:choose>
      <c:when test="${empty sessionScope.userSession || !sessionScope.userSession.loggedIn}">
        <p>Sign in to choose a workspace.</p>
      </c:when>
      <c:otherwise>
        <h1>Choose a workspace</h1>
        <ul>
          <c:forEach items="${workspaceList}" var="workspace">
            <li><a href="https://<c:out value='${workspace.canonicalDomain}'/>"><c:out value="${workspace.name}" /></a></li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>
  </main>
</body>
</html>
