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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:useBean id="title" class="java.lang.String" scope="request"/>
<jsp:useBean id="headings" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="minHeadingLevel" class="java.lang.String" scope="request"/>
<jsp:useBean id="maxHeadingLevel" class="java.lang.String" scope="request"/>
<jsp:useBean id="showToTop" class="java.lang.String" scope="request"/>
<style>
  /* .sticky.is-stuck {
    max-height: 80%;
    overflow-y: scroll;
  } */

  .page-toc-widget {
    padding: 0.5rem 0.5rem;
    background-color: transparent;
    border-left: 2px solid #cccccc;
    margin-bottom: 0.5rem;
    padding-left: 0.5rem;
  }

  .page-toc-title {
    font-size: 0.9rem;
    margin-bottom: 0.25rem;
    margin-top: 0;
    font-weight: 500;
    color: #333333;
  }

  .page-toc-title i {
    margin-right: 0.3rem;
    color: #666666;
    font-size: 0.85rem;
  }

  .page-toc-nav {
    margin: 0;
  }

  .page-toc-widget ul,
  .page-toc-widget li {
    list-style: none !important;
  }

  .page-toc-widget li::marker {
    content: '';
  }

  .page-toc-list {
    margin: 0;
    padding: 0;
    list-style-type: none;
  }

  .page-toc-list .nested {
    margin-left: 0.5rem;
    margin-top: 0;
    margin-bottom: 0;
    padding: 0;
  }

  .page-toc-item {
    margin-bottom: 0.1rem; /* Reduced spacing between items */
    line-height: 1.3;
  }

  .page-toc-item a {
    line-height: 1.3;
    padding: 0.3rem 0 0.3rem 1.7rem;
  }

  .page-toc-link {
    display: inline-block;
    padding: 0.05rem 0;
    text-decoration: none;
    transition: color 0.15s ease;
    border-bottom: none;
    font-size: 0.85rem;
    font-weight: 400;
  }

  .page-toc-link.is-active {
    font-weight: 500;
  }

  .page-toc-level-1 a {
    font-weight: 600;
    font-size: 0.85rem;
  }

  .page-toc-level-2 a {
    font-size: 0.8rem;
    font-weight: 400;
  }

  .page-toc-level-3 a {
    font-size: 0.75rem;
    font-weight: 400;
  }

  .page-toc-level-4 a,
  .page-toc-level-5 a,
  .page-toc-level-6 a {
    font-size: 0.75rem;
    font-weight: 400;
  }

  .page-toc-header-actions {
    margin: 0 0 0.3rem 1.7rem;
  }

  .page-toc-back-to-top {
    display: inline-block;
    padding: 0;
    background: transparent;
    color: #666666;
    text-decoration: none;
    border: 0;
    border-radius: 0;
    font-size: 0.78rem;
    font-weight: 600;
    transition: color 0.15s ease;
    cursor: pointer;
  }

  .page-toc-back-to-top:hover {
    background: transparent;
  }

  .page-toc-back-to-top i {
    font-size: 0.72rem;
    margin-right: 0.2rem;
  }

  .page-toc-empty {
    color: #999999;
    font-style: italic;
    margin: 0;
    font-size: 0.8rem;
  }
</style>
<div class="page-toc-widget">
  <c:if test="${!empty title}">
    <h4 class="page-toc-title"><i class="fas fa-list"></i> <c:out value="${title}"/></h4>
  </c:if>
  <c:choose>
    <c:when test="${!empty headings}">
      <c:if test="${showToTop eq 'true'}">
        <div class="page-toc-header-actions">
          <button type="button" class="page-toc-back-to-top" id="pageTocBackToTop">
            To Top
          </button>
        </div>
      </c:if>
      <nav class="page-toc-nav" aria-label="Table of Contents">
        <ul class="vertical menu page-toc-list">
          <c:set var="currentLevel" value="${minHeadingLevel}"/>
          <c:forEach items="${headings}" var="heading" varStatus="status">
            <c:set var="levelDiff" value="${heading.level - currentLevel}"/>
            <%-- Close lists if moving to a lower level --%>
            <c:if test="${levelDiff < 0}">
              <c:forEach begin="1" end="${-levelDiff}">
                </ul></li>
              </c:forEach>
            </c:if>
            <%-- Open nested lists if moving to a higher level --%>
            <c:if test="${levelDiff > 0 && !status.first}">
              <c:forEach begin="1" end="${levelDiff}">
                <li><ul class="nested vertical menu">
              </c:forEach>
            </c:if>
            <%-- Render the heading link --%>
            <li class="page-toc-item page-toc-level-${heading.level}">
              <a href="#${heading.id}" class="page-toc-link" data-heading-id="${heading.id}">
                <c:out value="${heading.text}"/>
              </a>
            </li>
            <c:set var="currentLevel" value="${heading.level}"/>
            <%-- Close nested lists at the end --%>
            <c:if test="${status.last}">
              <c:set var="levelsToClose" value="${currentLevel - minHeadingLevel}"/>
              <c:if test="${levelsToClose > 0}">
                <c:forEach begin="1" end="${levelsToClose}">
                  </ul></li>
                </c:forEach>
              </c:if>
            </c:if>
          </c:forEach>
        </ul>
      </nav>
      <script>
        document.addEventListener('DOMContentLoaded', function() {
          const tocLinks = Array.from(document.querySelectorAll('.page-toc-link'));
          const headingSelector = 'h1, h2, h3, h4, h5, h6';

          function normalizeText(value) {
            return (value || '').replace(/\s+/g, ' ').trim();
          }

          function isVisibleHeading(heading) {
            if (!heading) {
              return false;
            }
            const style = window.getComputedStyle(heading);
            if (style.display === 'none' || style.visibility === 'hidden') {
              return false;
            }
            return heading.getClientRects().length > 0;
          }

          function shouldIgnoreHeadingText(text) {
            const value = normalizeText(text).toLowerCase();
            if (!value) {
              return true;
            }
            // Avoid literal EL syntax in JSP source (it is parsed at compile time).
            const elStart = '$' + '{';
            // Ignore macro/placeholder-like headings that should never appear in TOC
            return value.indexOf('<insert title here>') !== -1 ||
              value.indexOf(elStart + 'diagram:') !== -1 ||
              value.indexOf(elStart) !== -1;
          }

          // Get all headings on the page, excluding those in the TOC widget itself
          function getPageHeadings() {
            return Array.from(document.querySelectorAll(headingSelector))
              .filter(function(heading) {
                if (heading.closest('.page-toc-widget')) {
                  return false;
                }
                if (!isVisibleHeading(heading)) {
                  return false;
                }
                return !shouldIgnoreHeadingText(heading.textContent);
              });
          }

          const pageHeadings = getPageHeadings();

          // Match TOC links to page headings by text content (exact or partial match)
          tocLinks.forEach(function(link) {
            const linkText = normalizeText(link.textContent);
            const generatedId = link.getAttribute('data-heading-id');

            // Try exact match first
            let matched = false;
            for (let i = 0; i < pageHeadings.length; i++) {
              const heading = pageHeadings[i];
              const headingText = normalizeText(heading.textContent);
              if (headingText === linkText) {
                if (!heading.id) {
                  heading.id = generatedId;
                }
                link.setAttribute('data-heading-id', heading.id);
                link.setAttribute('href', '#' + heading.id);
                matched = true;
                break;
              }
            }

            // If no exact match, try partial match
            if (!matched) {
              for (let i = 0; i < pageHeadings.length; i++) {
                const heading = pageHeadings[i];
                const headingText = normalizeText(heading.textContent);
                if (headingText.indexOf(linkText) !== -1 && linkText.length > 0) {
                  if (!heading.id) {
                    heading.id = generatedId;
                  }
                  link.setAttribute('data-heading-id', heading.id);
                  link.setAttribute('href', '#' + heading.id);
                  matched = true;
                  break;
                }
              }
            }

            // If no rendered heading matches this TOC entry, remove it from the list.
            if (!matched) {
              const tocItem = link.closest('.page-toc-item');
              if (tocItem && tocItem.parentNode) {
                tocItem.parentNode.removeChild(tocItem);
              }
            }
          });

          // Handle TOC link clicks with smooth scroll
          tocLinks.forEach(function(link) {
            link.addEventListener('click', function(e) {
              e.preventDefault();
              e.stopPropagation();

              const targetId = this.getAttribute('data-heading-id');
              const targetElement = document.getElementById(targetId);

              if (targetElement) {
                // Calculate scroll position with offset from top
                const rect = targetElement.getBoundingClientRect();
                const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                const targetY = Math.max(0, rect.top + scrollTop - 100);

                // Scroll immediately
                window.scrollTo(0, targetY);

                // Re-apply scroll after delay to ensure it sticks (handles cases where other scripts interfere)
                setTimeout(function() {
                  window.scrollTo(0, targetY);
                }, 100);
              }
            });
          });

          // Back to top button
          const backToTopBtn = document.getElementById('pageTocBackToTop');
          if (backToTopBtn) {
            backToTopBtn.addEventListener('click', function(e) {
              e.preventDefault();
              e.stopPropagation();

              // Reset TOC panel scroll state and move page to true top.
              const stickyPanel = backToTopBtn.closest('[data-sticky]') || backToTopBtn.closest('.sticky');
              const stickyContainer = backToTopBtn.closest('[data-sticky-container]') || (stickyPanel ? stickyPanel.parentElement : null);

              function resetStickyState() {
                if (stickyPanel) {
                  stickyPanel.scrollTop = 0;
                  // Clear stale bottom-anchored state/styles so the panel is visible after jumping to top.
                  stickyPanel.classList.remove('is-at-bottom');
                  stickyPanel.style.top = '';
                  stickyPanel.style.bottom = '';
                }
                if (stickyContainer) {
                  stickyContainer.scrollTop = 0;
                }
              }

              resetStickyState();

              window.scrollTo(0, 0);
              setTimeout(function() {
                resetStickyState();
                window.scrollTo(0, 0);

                // Force Foundation Sticky to recompute anchor/stuck state after programmatic scroll.
                if (window.jQuery) {
                  if (stickyPanel) {
                    window.jQuery(stickyPanel).triggerHandler('resizeme.zf.trigger');
                  }
                  window.jQuery(window).trigger('resize.zf.trigger');
                }
              }, 100);
            });
          }

          // Highlight current section as user scrolls
          let timeoutId;
          window.addEventListener('scroll', function() {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(function() {
              const scrollPosition = window.scrollY + 100;
              const headings = getPageHeadings()
                .filter(function(heading) {
                  return heading.id;
                })
                .reverse();

              tocLinks.forEach(function(link) {
                link.classList.remove('is-active');
              });

              for (let i = 0; i < headings.length; i++) {
                if (headings[i].offsetTop <= scrollPosition) {
                  const activeLink = document.querySelector('.page-toc-link[data-heading-id="' + headings[i].id + '"]');
                  if (activeLink) {
                    activeLink.classList.add('is-active');
                  }
                  break;
                }
              }
            }, 100);
          });
        });
      </script>
    </c:when>
    <c:otherwise>
      <p class="page-toc-empty">No table of contents available</p>
    </c:otherwise>
  </c:choose>
</div>

