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
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<form method="get" action="${ctx}/search" class="search-form" autocomplete="off" data-header-search data-min-chars="${searchMinChars}" data-debounce-ms="${searchDebounceMs}">
  <div class="search-form-row">
    <input type="search" name="query" value="<c:out value='${param.query}'/>" placeholder="<c:out value='${placeholder}'/>" class="search-input" autocomplete="off" aria-label="Search" aria-controls="<c:out value='${searchInstanceId}'/>-results" aria-expanded="false" />
    <button type="submit" class="search-submit" title="Search" aria-label="Search"><i class="${font:fas()} fa-search" aria-hidden="true"></i></button>
  </div>
  <div class="search-dropdown-panel" aria-hidden="true" data-header-search-panel>
    <div id="<c:out value='${searchInstanceId}'/>-results" class="search-dropdown-results" aria-live="polite" data-header-search-results></div>
  </div>
</form>
<div class="search-backdrop" aria-hidden="true" data-header-search-backdrop></div>