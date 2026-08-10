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
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="webPage" class="com.simisinc.platform.domain.model.cms.WebPage" scope="request"/>
<%-- Handle image uploads --%>
<script>
    function SavePhoto(e) {
        var file = e.files[0]; // similar to: document.getElementById("file").files[0]
        var formData = new FormData();
        formData.append("file", file);
        var xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function() {
            if (this.readyState === 4) {
                if (this.status === 200) {
                    var fileData = JSON.parse(this.responseText);
                    document.getElementById("imageUrl").value = fileData.location;
                    document.getElementById("imageUrlPreview").src = fileData.location;
                } else {
                    document.getElementById("imageFile").value = "";
                    alert('There was an error with the file. Make sure to use a .jpg or .png');
                }
            }
        };
        xhr.open("POST", '${ctx}/image-upload?widget=imageUpload1&token=${userSession.formToken}');
        xhr.send(formData);
    }
    <c:if test="${userSession.hasRole('admin') || userSession.hasRole('content-manager')}">
      function archivePage() {
          // Update modal content for archive action
          document.getElementById('archiveModalTitle').textContent = 'Archive Page';
          document.getElementById('archiveModalMessage').textContent = 'Are you sure you want to ARCHIVE this page?';
          document.getElementById('archiveConfirmBtn').textContent = 'Archive';
          document.getElementById('archiveConfirmBtn').className = 'button warning';
          
          // Store action URL for confirm button
          document.getElementById('archiveConfirmBtn').setAttribute('data-action-url', 
              '${widgetContext.uri}?action=archivePage&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&webPageId=${webPage.id}');
          
          // Open modal
          $('#archiveConfirmModal').foundation('open');
      }
      
      function unarchivePage() {
          // Update modal content for unarchive action
          document.getElementById('archiveModalTitle').textContent = 'Unarchive Page';
          document.getElementById('archiveModalMessage').textContent = 'Are you sure you want to UNARCHIVE this page?';
          document.getElementById('archiveConfirmBtn').textContent = 'Unarchive';
          document.getElementById('archiveConfirmBtn').className = 'button success';
          
          // Store action URL for confirm button
          document.getElementById('archiveConfirmBtn').setAttribute('data-action-url', 
              '${widgetContext.uri}?action=unarchivePage&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&webPageId=${webPage.id}');
          
          // Open modal
          $('#archiveConfirmModal').foundation('open');
      }
      
      function confirmArchive() {
          var actionUrl = document.getElementById('archiveConfirmBtn').getAttribute('data-action-url');
          window.location.href = actionUrl;
      }
    </c:if>
    <c:if test="${userSession.hasRole('admin')}">
      function deletePage() {
          if (!confirm("Are you sure you want to DELETE this page?")) {
              return;
          }
          window.location.href = '${widgetContext.uri}?action=deletePage&widget=${widgetContext.uniqueId}&token=${userSession.formToken}&webPageId=${webPage.id}';
      }
    </c:if>
</script>
<form method="post">
  <%-- Required by controller --%>
  <input type="hidden" name="widget" value="${widgetContext.uniqueId}" />
  <input type="hidden" name="token" value="${userSession.formToken}" />
  <%-- Form specific --%>
  <input type="hidden" name="returnPage" value="<c:out value="${returnPage}"/>" />
  <input type="hidden" name="id" value="${webPage.id}" />
  <%-- Title and Message block --%>
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}" /></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>
  <%-- Form Content --%>
  <div class="grid-x grid-padding-x">
    <div class="small-12 medium-6 cell">
      <label>Link <span class="required">*</span>
        <input type="text" placeholder="/example" name="link" value="<c:out value="${webPage.link}"/>" required>
      </label>
      <label>Redirect
        <input type="text" placeholder="/other/page" name="redirectUrl" value="<c:out value="${webPage.redirectUrl}"/>">
      </label>
      <label>Title
        <input type="text" placeholder="Give it a title..." name="title" value="<c:out value="${webPage.title}"/>">
      </label>
      <label>Keywords
        <input type="text" placeholder="Comma-separated keywords..." name="keywords" value="<c:out value="${webPage.keywords}"/>">
      </label>
      <label>Description
        <input type="text" placeholder="Describe it..." name="description" value="<c:out value="${webPage.description}"/>">
      </label>
      <label>Tags
        <small>Type a tag and press Tab or Enter, or select from suggestions</small>
        <div id="tag-input-container" class="tag-input-container">
          <div id="tag-chips"></div>
          <input type="text" id="tag-input" placeholder="Add tags..." autocomplete="off"/>          
          <!-- Dropdown -->
          <div id="tag-dropdown" class="tag-dropdown" style="display:none;"></div>
        </div>
        <input type="hidden" id="tags-hidden" name="tagsValue"
              value="<c:out value='${tagsValue}'/>" />
      </label>
    </div>
    <div class="small-12 medium-6 cell">
      <label>Publish?
        <div class="switch large">
          <input class="switch-input" id="publish-yes-no" type="checkbox" name="publish" value="true"<c:if test="${!webPage.draft}"> checked</c:if>>
          <label class="switch-paddle" for="publish-yes-no">
            <span class="switch-active" aria-hidden="true">Yes</span>
            <span class="switch-inactive" aria-hidden="true">No</span>
          </label>
        </div>
      </label>
      <div class="grid-x grid-padding-x">
        <div class="small-12 medium-3 cell">
          <label>Show in Sitemap.xml?
            <div class="switch large">
              <input class="switch-input" id="sitemap-yes-no" type="checkbox" name="showInSitemap" value="true"<c:if
                test="${webPage.showInSitemap}"> checked</c:if>>
              <label class="switch-paddle" for="sitemap-yes-no">
                <span class="switch-active" aria-hidden="true">Yes</span>
                <span class="switch-inactive" aria-hidden="true">No</span>
              </label>
            </div>
          </label>
        </div>
        <div class="small-12 medium-3 cell">
          <label>Priority (0.0-1.0)
            <input type="text" name="sitemapPriority" value="<fmt:formatNumber value="${webPage.sitemapPriority}" />" />
          </label>
        </div>
        <div class="small-12 medium-3 cell">
          <label>Change Frequency
            <select name="sitemapChangeFrequency">
              <option value=""></option>
              <c:forEach items="${sitemapChangeFrequencyMap}" var="option">
                <option value="<c:out value="${option.key}" />"<c:if test="${webPage.sitemapChangeFrequency eq option.key}"> selected</c:if>><c:out value="${option.value}" /></option>
              </c:forEach>
            </select>
          </label>
        </div>
      </div>
      <label>Searchable?
        <div class="switch large">
          <input class="switch-input" id="searchable-yes-no" type="checkbox" name="searchable" value="true"<c:if test="${webPage.searchable}"> checked</c:if>>
          <label class="switch-paddle" for="searchable-yes-no">
            <span class="switch-active" aria-hidden="true">Yes</span>
            <span class="switch-inactive" aria-hidden="true">No</span>
          </label>
        </div>
      </label>
      <small>Open Graph Image</small>
      <img id="imageUrlPreview" src="<c:out value="${webPage.imageUrl}"/>" style="max-height: 150px; max-width: 150px"/>
      <input type="text" class="no-gap" placeholder="Local Image URL" id="imageUrl" name="imageUrl" value="<c:out value="${webPage.imageUrl}"/>">
      <label for="imageFile" class="button">Upload Image File...</label>
      <input type="file" id="imageFile" class="show-for-sr" onchange="SavePhoto(this)">
      <p>
        <a class="button small primary radius no-gap" data-open="imageBrowserReveal">Browse Images</a>
      </p>
    </div>
  </div>
  <div class="button-container">
    <input type="submit" class="button radius success" value="Save" />
    <c:choose>
      <c:when test="${!empty returnPage}">
        <a href="${returnPage}" class="button radius secondary">Cancel</a>
      </c:when>
      <c:when test="${!empty webPage.link}">
        <a href="${ctx}${webPage.link}" class="button radius secondary">Cancel</a>
      </c:when>
      <c:otherwise>

      </c:otherwise>
    </c:choose>
    <c:if test="${userSession.hasRole('admin') || userSession.hasRole('content-manager')}">
      <c:choose>
        <c:when test="${webPage.enabled}">
          <a class="button radius warning" href="javascript:archivePage()"><i class="fa fa-archive"></i> Archive Page</a>
        </c:when>
        <c:otherwise>
          <a class="button radius success" href="javascript:unarchivePage()"><i class="fa fa-undo"></i> Unarchive Page</a>
        </c:otherwise>
      </c:choose>
    </c:if>
    <c:if test="${userSession.hasRole('admin')}">
      <a class="button radius alert" href="javascript:deletePage()"><i class="fa fa-trash-o"></i> Delete Page</a>
    </c:if>
  </div>
</form>

<%-- Archive Confirmation Modal --%>
<div class="reveal small" id="archiveConfirmModal" data-reveal>
  <h3 id="archiveModalTitle">Archive Page</h3>
  <p id="archiveModalMessage">Are you sure you want to ARCHIVE this page?</p>
  <div class="text-right" style="margin-top: 20px;">
    <button class="button secondary" data-close style="margin-right: 10px;">Cancel</button>
    <button id="archiveConfirmBtn" class="button warning" onclick="confirmArchive()">Archive</button>
  </div>
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
</div>

<div class="reveal large" id="imageBrowserReveal" data-reveal data-animation-in="slide-in-down fast">
  <h3>Loading...</h3>
</div>
<script>
    (function() {
        const tagInput = document.getElementById('tag-input');
        const tagChips = document.getElementById('tag-chips');
        const tagsHidden = document.getElementById('tags-hidden');
        const dropdown = document.getElementById('tag-dropdown');

        let tags = [];
        let allTags = []; // from API
        let filteredTags = [];
        let activeIndex = -1;

        // ===== Load existing tags =====
        try {
            const value = tagsHidden.value.trim();
            if (value && value !== '[]') {
                tags = JSON.parse(value);
            }
        } catch (e) {
            tags = [];
        }

        // ===== Fetch tags from API =====
        fetch('/json/tags')
            .then(res => res.json())
            .then(data => {
                allTags = Array.isArray(data) ? data : (data.data || []);
            })
            .catch(() => {
                allTags = [];
            });

        function renderTags() {
            tagChips.innerHTML = '';

            tags.forEach((tag, index) => {
                const chip = document.createElement('div');
                chip.className = 'tag-chip';

                const span = document.createElement('span');
                span.textContent = tag;

                const remove = document.createElement('span');
                remove.className = 'remove-tag';
                remove.innerHTML = '&times;';
                remove.dataset.index = index;

                chip.appendChild(span);
                chip.appendChild(remove);
                tagChips.appendChild(chip);
            });

            tagsHidden.value = JSON.stringify(tags);
        }

        function addTag(value) {
            const t = value.trim().toLowerCase();
            if (t && !tags.includes(t)) {
                tags.push(t);
                renderTags();
            }
            tagInput.value = '';
            hideDropdown();
        }

        function removeTag(index) {
            tags.splice(index, 1);
            renderTags();
        }

        function showDropdown(list) {
            dropdown.innerHTML = '';
            if (!list.length) {
                dropdown.style.display = 'none';
                return;
            }

            list.forEach((tag, i) => {
                const div = document.createElement('div');
                div.className = 'tag-option';
                div.textContent = tag;

                // Mark already selected
                if (tags.includes(tag.toLowerCase())) {
                    div.style.fontWeight = 'bold';
                }

                div.addEventListener('click', () => {
                    addTag(tag);
                });

                dropdown.appendChild(div);
            });

            dropdown.style.display = 'block';
        }

        function hideDropdown() {
            dropdown.style.display = 'none';
            activeIndex = -1;
        }

        function filterTags(query) {
            const q = query.toLowerCase();
            filteredTags = allTags.filter(t => t.toLowerCase().includes(q));
            showDropdown(filteredTags);
        }

        // ===== Events =====

        tagInput.addEventListener('input', function() {
            filterTags(this.value);
        });

        tagInput.addEventListener('focus', function() {
            filterTags(this.value);
        });

        tagInput.addEventListener('keydown', function(e) {

            const items = dropdown.querySelectorAll('.tag-option');

            if (e.key === 'ArrowDown') {
                e.preventDefault();
                activeIndex = Math.min(activeIndex + 1, items.length - 1);
            }
            else if (e.key === 'ArrowUp') {
                e.preventDefault();
                activeIndex = Math.max(activeIndex - 1, 0);
            }
            else if (e.key === 'Enter') {
                e.preventDefault();

                if (activeIndex >= 0 && items[activeIndex]) {
                    addTag(items[activeIndex].textContent);
                } else {
                    addTag(tagInput.value); // new tag
                }
            }
            else if (e.key === 'Tab') {
                // IMPORTANT: Allow tab default for navigation but still add tag
                addTag(tagInput.value);
            }
            else if (e.key === 'Backspace' && tagInput.value === '' && tags.length) {
                tags.pop();
                renderTags();
            }

            // Highlight active item
            items.forEach((el, i) => {
                el.classList.toggle('active', i === activeIndex);
            });
        });

        document.addEventListener('click', function(e) {
            if (!e.target.closest('#tag-input-container')) {
                hideDropdown();
            }
        });

        tagChips.addEventListener('click', function(e) {
            if (e.target.classList.contains('remove-tag')) {
                removeTag(parseInt(e.target.dataset.index));
            }
        });

        renderTags();

    })();
    $('#imageBrowserReveal').on('open.zf.reveal', function () {
        $('#imageBrowserReveal').html("<h3>Loading...</h3>");
        $.ajax({
            url: '${ctx}/image-browser?inputId=imageUrl&view=reveal',
            cache: false,
            dataType: 'html'
        }).done(function (content) {
            setTimeout(function () {
                $('#imageBrowserReveal').html(content);
                $('#imageBrowserReveal').trigger('resizeme.zf.trigger');
            }, 1000);
        });
    })
</script>
