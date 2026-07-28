<%--
  ~ Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
  ~ Page Children Widget
  ~ Displays child pages in a hierarchical tree structure with configurable depth filtering
  ~ 
  ~ Widget Configuration:
  ~ - pageLink: String (optional) - Use this page as the parent instead of the current page
  ~ - maxDepth: Integer (default: 1) - How many levels deep to display
  ~ - sortBy: String (default: sort_order) - Column to sort by
  ~ - showCount: Boolean (default: false) - Show number of children
--%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page import="java.util.List" %>
<c:set var="childPages" value="${requestScope.childPages}" />
<c:set var="showCount" value="${requestScope.showCount != null ? requestScope.showCount : false}" />
<style>
  
  /* .children-pages {
    --surface: linear-gradient(180deg, #ffffff 0%, #f6f9fc 100%);
    --surface-strong: #ffffff;
    --text: #1f2a37;
    --muted: #5f6f82;
    --border: rgba(23, 43, 77, 0.12);
    --shadow: 0 12px 32px rgba(20, 33, 61, 0.08);
    --accent-1: #005db5;
    --accent-2: #1b7f6b;
    --accent-3: #8f3fb2;
    margin: 0.75rem 0;
    padding: 0.9rem 1rem 1rem;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 16px;
    box-shadow: 0 10px 24px rgba(20, 33, 61, 0.06);
  } */

  .children-pages h4 {
    margin: 0;
    color: var(--text);
    font-size: 1rem;
    font-weight: 700;
    letter-spacing: 0.01em;
  }

  .page-tree {
    list-style: none;
    padding: 0.45rem 0 0;
    margin: 0;
    font-size: 0.92rem;
  }

  .tree-item {
    margin: 0.1rem 0;
  }

  .tree-content {
    display: flex;
    align-items: flex-start;
    gap: 0.5rem;
    padding: 0.28rem 0.2rem;
    color: var(--text);
    border-radius: 8px;
    transition: background-color 0.18s ease, color 0.18s ease;
  }

  .tree-item:hover .tree-content {
    background: rgba(0, 93, 181, 0.05);
  }

  .tree-item.level-1 {
    margin-top: 0.2rem;
  }

  .tree-item.level-1 .tree-link {
    color: var(--accent-1);
    font-weight: 700;
  }

  .tree-item.level-2 .tree-link {
    color: #166956;
    font-weight: 600;
  }

  .tree-item.level-3 .tree-link {
    color: var(--accent-3);
    font-weight: 600;
  }

  .tree-item.level-4 .tree-link,
  .tree-item.level-5 .tree-link,
  .tree-item.level-6 .tree-link {
    color: var(--text);
    font-weight: 500;
  }

  .tree-indicator {
    width: 1rem;
    height: 1rem;
    text-align: center;
    color: var(--surface-strong);
    flex: 0 0 auto;
    font-size: 0.72rem;
    line-height: 1rem;
    border-radius: 999px;
    background: linear-gradient(135deg, var(--accent-1) 0%, var(--accent-3) 100%);
    margin-top: 0.18rem;
  }

  .tree-text-wrapper {
    display: flex;
    align-items: baseline;
    gap: 0.45rem;
    flex-wrap: wrap;
    min-width: 0;
    width: 100%;
    padding-left: calc((var(--level, 1) - 1) * 0.8rem);
    position: relative;
  }

  .tree-item.level-1 .tree-text-wrapper::before {
    display: none;
  }

  .tree-item.level-5 .tree-indicator,
  .tree-item.level-6 .tree-indicator,
  .tree-item.level-7 .tree-indicator,
  .tree-item.level-8 .tree-indicator,
  .tree-item.level-9 .tree-indicator,
  .tree-item.level-10 .tree-indicator {
    background: linear-gradient(135deg, #607086 0%, #314257 100%);
  }

  .tree-link {
    text-decoration: none;
    line-height: 1.3;
    transition: color 0.18s ease;
  }

  .tree-link:hover {
    text-decoration: none;
    color: #003f7a;
  }

  .tree-description {
    margin: 0;
    color: var(--muted);
    font-size: 0.78rem;
    max-width: 44ch;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 0;
    background: transparent;
    border-radius: 0;
  }

  .tree-description::before {
    content: "\2014";
    margin-right: 0.35rem;
    color: rgba(95, 111, 130, 0.7);
  }

  .tree-meta,
  .tree-empty {
    margin: 0.75rem 0 0;
    color: var(--muted);
    font-size: 0.84rem;
  }

  .tree-meta {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.22rem 0.7rem;
    background: rgba(0, 93, 181, 0.08);
    border-radius: 999px;
  }

  @media (max-width: 768px) {
    .children-pages {
      padding: 0.85rem;
      border-radius: 14px;
    }

    .tree-content {
      padding: 0.32rem 0.15rem;
    }

    .tree-text-wrapper {
      align-items: flex-start;
      gap: 0.3rem;
    }

    .tree-description {
      max-width: 100%;
      white-space: normal;
    }
  }
</style>
<section class="children-pages">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
  </c:if>
  <c:if test="${showCount}">
    <p class="tree-meta">Pages: <c:out value="${fn:length(childPages)}" /></p>
  </c:if>
  <c:choose>
    <c:when test="${empty childPages}">
      <p class="tree-empty">There are no pages configured in the index.</p>
    </c:when>
    <c:otherwise>
      <ul class="page-tree">
      <c:forEach var="item" items="${childPages}">
        <c:set var="child" value="${item.page}" />
        <c:set var="level" value="${item.level}" />
        <li class="tree-item level-${level}">
          <div class="tree-content">
            <div class="tree-text-wrapper" style="--level:<c:out value='${level}'/>;">
              <c:if test="${level ge 5}">
                <span class="tree-indicator">&bull;</span>
              </c:if>
              <a href="<c:out value="${child.link}" />" class="tree-link"><c:out value="${child.title}" /></a>
              <c:if test="${not empty child.description}">
                <p class="tree-description">- <c:out value="${child.description}" /></p>
              </c:if>
            </div>
          </div>
        </li>
      </c:forEach>
    </ul>
    </c:otherwise>
  </c:choose>
</section>
