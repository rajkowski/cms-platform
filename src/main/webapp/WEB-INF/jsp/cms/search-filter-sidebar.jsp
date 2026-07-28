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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="query" class="java.lang.String" scope="request"/>
<jsp:useBean id="labelFilter" class="java.lang.String" scope="request"/>
<jsp:useBean id="modifiedAfter" class="java.lang.String" scope="request"/>
<jsp:useBean id="modifiedBefore" class="java.lang.String" scope="request"/>
<jsp:useBean id="dateFilterType" class="java.lang.String" scope="request"/>
<jsp:useBean id="contributorFilter" class="java.lang.String" scope="request"/>
<jsp:useBean id="ofType" class="java.lang.String" scope="request"/>
<jsp:useBean id="region" class="com.zeroio.platform.domain.model.Region" scope="request"/>
<jsp:useBean id="useRegions" class="java.lang.String" scope="request"/>
<jsp:useBean id="useTags" class="java.lang.String" scope="request"/>
<jsp:useBean id="useTypes" class="java.lang.String" scope="request"/>
<jsp:useBean id="useLastModified" class="java.lang.String" scope="request"/>
<jsp:useBean id="useContributors" class="java.lang.String" scope="request"/>
<g:compress>
  <link rel="stylesheet" href="${ctx}/css/platform-search-form.css">
</g:compress>

<!-- Region Display Section -->
<c:if test="${'true' eq useRegions && !empty region.name}">
  <div class="search-filter-section">
    <div class="search-filter-section-title">
      <span>Region</span>
    </div>
    <div class="region-display-text" style="cursor: pointer; font-size: 14px; border-radius: 3px; padding: 0px 12px; margin-left: .5rem; margin-right: 1rem;">
      <c:out value="${region.name}" />
    </div>
  </div>
</c:if>

<!-- Of Type Filter Section -->
 <c:if test="${'true' eq useTypes}">
<div class="search-filter-section">
  <div class="search-filter-section-title" onclick="toggleFilterSection('of-type')">
    <span>Of Type</span>
    <i class="fa fa-chevron-down"></i>
  </div>
  <ul id="filter-section-of-type" class="search-filter-options">
    <li class="search-filter-option">
      <input type="radio" name="of-type" value="all"<c:if test="${empty ofType}"> checked</c:if>>
      <label>All content</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="of-type" value="pages"<c:if test="${ofType == 'pages'}"> checked</c:if>>
      <label>Web Pages</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="of-type" value="posts"<c:if test="${ofType == 'posts'}"> checked</c:if>>
      <label>Posts</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="of-type" value="resources"<c:if test="${ofType == 'resources'}"> checked</c:if>>
      <label>Resources</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="of-type" value="attachments"<c:if test="${ofType == 'attachments'}"> checked</c:if>>
      <label>Attachments</label>
    </li>
  </ul>
</div>
</c:if>

<!-- Tags Label Filter Section -->
<c:if test="${'true' eq useTags}">
<div class="search-filter-section">
  <div class="search-filter-section-title">
    <span>Tags</span>
  </div>
  <div class="label-multiselect">
    <div class="label-input-wrapper" id="label-input-wrapper">
      <div class="label-selected-items" id="label-selected-items"></div>
        <input type="text" id="label-search-input" class="label-search-input" placeholder="Please select..." autocomplete="off">
        <span class="label-dropdown-icon">
          <i class="fa fa-chevron-down"></i>
        </span>
    </div>
    <div class="label-dropdown-panel" id="label-dropdown-panel">
      <div class="label-dropdown-content" id="label-dropdown-content">
        <!-- Tags will be populated dynamically from API -->
      </div>
    </div>
  </div>
</div>
</c:if>

<!-- Last Modified Filter Section -->
<c:if test="${'true' eq useLastModified}">
<div class="search-filter-section">
  <div class="search-filter-section-title" onclick="toggleFilterSection('last-modified')">
    <span>Last Modified</span>
    <i class="fa fa-chevron-down"></i>
  </div>
  <ul id="filter-section-last-modified" class="search-filter-options">
    <li class="search-filter-option">
      <input type="radio" name="last-modified" value="any"<c:if test="${empty dateFilterType || dateFilterType eq 'any'}"> checked</c:if>>
      <label>Any date</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="last-modified" value="24hours"<c:if test="${dateFilterType eq '24hours'}"> checked</c:if>>
      <label>Last 24 hours</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="last-modified" value="week"<c:if test="${dateFilterType eq 'week'}"> checked</c:if>>
      <label>Last week</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="last-modified" value="month"<c:if test="${dateFilterType eq 'month'}"> checked</c:if>>
      <label>Last month</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="last-modified" value="year"<c:if test="${dateFilterType eq 'year'}"> checked</c:if>>
      <label>Last year</label>
    </li>
    <li class="search-filter-option">
      <input type="radio" name="last-modified" value="custom"<c:if test="${dateFilterType eq 'custom'}"> checked</c:if>>
      <label>Custom</label>
    </li>
    <li class="search-filter-option custom-date-range" id="custom-date-range" style="display: ${dateFilterType eq 'custom' ? 'block' : 'none'};">
      <div class="custom-date-inputs">
        <div class="date-input-group">
          <label for="start-date">From:</label>
          <input type="date" id="start-date" name="start-date" class="date-input" value="<c:out value="${modifiedAfter}"/>">
        </div>
        <div class="date-input-group">
          <label for="end-date">To:</label>
          <input type="date" id="end-date" name="end-date" class="date-input" value="<c:out value="${modifiedBefore}"/>">
        </div>
      </div>
    </li>
  </ul>
</div>
</c:if>

<!-- Contributor Filter Section -->
<c:if test="${'true' eq useContributors}">
<div class="search-filter-section">
  <div class="search-filter-section-title">
    <span>Contributor</span>
  </div>
  <div class="contributor-multiselect">
    <div class="contributor-input-wrapper" id="contributor-input-wrapper">
      <div class="contributor-selected-items" id="contributor-selected-items"></div>
      <input type="text" id="contributor-search-input" class="contributor-search-input" placeholder="Please select..." autocomplete="off">
      <span class="contributor-dropdown-icon">
        <i class="fa fa-chevron-down"></i>
      </span>
    </div>
    <div class="contributor-dropdown-panel" id="contributor-dropdown-panel">
      <div class="contributor-dropdown-content" id="contributor-dropdown-content">
        <!-- Users will be populated dynamically from API -->
      </div>
    </div>
  </div>
</div>
</c:if>

<script>
  // Region values from user session
  let regionFilterInitialized = false;
  let isRestoringFilters = false; // Flag to prevent search trigger during restoration
  
  // Constants
  const MS_PER_DAY = 24 * 60 * 60 * 1000;
  const DATE_OFFSET_DAYS = {
    '24hours': 1,
    'week': 7,
    'month': 30,
    'year': 365
  };
  const SELECTORS = {
    LABEL_CHECKBOXES: '#label-dropdown-content input[type="checkbox"]:checked',
    CONTRIBUTOR_CHECKBOXES: '#contributor-dropdown-content input[type="checkbox"]:checked'
  };

  // Each filter section can be independently enabled/disabled via widget preferences.
  // These flags let the restoration/search logic skip sections that are not rendered
  // instead of waiting on them or bailing out entirely.
  const SECTIONS_ENABLED = {
    tags: ${useTags eq 'true'},
    types: ${useTypes eq 'true'},
    lastModified: ${useLastModified eq 'true'},
    contributors: ${useContributors eq 'true'}
  };
  
  // Wait for page to fully load before attaching event listeners
  document.addEventListener('DOMContentLoaded', function() {
    // Prevent double initialization
    if (regionFilterInitialized) {
      // Region filter already initialized
      return;
    }
    regionFilterInitialized = true;
    
    // Initializing search filters
    
    // Track when dynamic content is loaded (sections that are disabled are
    // treated as already "loaded" so they never block filter restoration)
    let tagsLoaded = !SECTIONS_ENABLED.tags;
    let usersLoaded = !SECTIONS_ENABLED.contributors;
    
    // Global reference to update functions (defined later, used in restoration)
    let updateLabelInputDisplay = null;
    let updateContributorInputDisplay = null;
    
    // Utility function to clear date inputs and constraints
    function clearDateInputs() {
      const startDateInput = document.getElementById('start-date');
      const endDateInput = document.getElementById('end-date');
      if (startDateInput) {
        startDateInput.value = '';
        startDateInput.removeAttribute('max');
      }
      if (endDateInput) {
        endDateInput.value = '';
        endDateInput.removeAttribute('min');
      }
    }
    
    // Utility function to toggle chevron icon for dropdowns
    function toggleChevronIcon(wrapper, isOpen) {
      const icon = wrapper.querySelector('.fa');
      if (icon) {
        icon.classList.toggle('fa-chevron-down', !isOpen);
        icon.classList.toggle('fa-chevron-up', isOpen);
      }
    }
    
    // Utility function to clear and close a dropdown panel
    function clearDropdownPanel(selectedItemsId, panelId, searchInputId) {
      const selectedItems = document.getElementById(selectedItemsId);
      const panel = document.getElementById(panelId);
      const searchInput = document.getElementById(searchInputId);
      
      if (selectedItems) selectedItems.innerHTML = '';
      if (searchInput) {
        searchInput.value = '';
        searchInput.placeholder = 'Please select...';
      }
      if (panel) panel.classList.remove('show');
    }
    
    // Utility function to update date constraints
    function updateDateConstraint(changedInput, otherInput, constraint) {
      if (changedInput.value) {
        otherInput[constraint] = changedInput.value;
        if (otherInput.value) {
          const isInvalid = constraint === 'min' ? 
            otherInput.value < changedInput.value : 
            otherInput.value > changedInput.value;
          if (isInvalid) otherInput.value = '';
        }
      } else {
        otherInput.removeAttribute(constraint);
      }
    }
    
    function checkAndRestoreFilters() {
      // Wait for tags to load for all users (they need to see selected labels even if read-only)
      if (tagsLoaded && usersLoaded) {
        
        // WAIT FOR ALL FILTER ELEMENTS TO EXIST IN DOM
        // Sections that are disabled (not rendered) are skipped instead of
        // blocking restoration of the sections that ARE enabled.
        const waitForElements = function(attempt = 1) {
          const filterSection = document.getElementById('filter-section-last-modified');
          const allRadios = document.querySelectorAll('input[name="last-modified"]');
          const labelDropdown = document.getElementById('label-dropdown-content');
          const contributorDropdown = document.getElementById('contributor-dropdown-content');
          
          // Check if dropdowns have checkboxes populated
          const labelCheckboxes = labelDropdown ? labelDropdown.querySelectorAll('input[type="checkbox"]') : [];
          const contributorCheckboxes = contributorDropdown ? contributorDropdown.querySelectorAll('input[type="checkbox"]') : [];
          
          // Now require that checkboxes are also populated (or dropdowns show error/no data message)
          const lastModifiedReady = !SECTIONS_ENABLED.lastModified || (filterSection && allRadios.length > 0);
          const labelReady = !SECTIONS_ENABLED.tags || (labelDropdown && (labelCheckboxes.length > 0 || labelDropdown.textContent.includes('No tags available') || labelDropdown.textContent.includes('Failed to load')));
          const contributorReady = !SECTIONS_ENABLED.contributors || (contributorDropdown && (contributorCheckboxes.length > 0 || contributorDropdown.textContent.includes('No users available') || contributorDropdown.textContent.includes('Failed to load')));
          
          if (lastModifiedReady && labelReady && contributorReady) {
            // All enabled sections are ready, restore filters
            if (typeof restoreFiltersFromURL === 'function') {
              restoreFiltersFromURL();
            }
          } else {
            // Not ready yet, retry
            if (attempt < 10) { // Max 10 attempts = 1 second
              setTimeout(() => waitForElements(attempt + 1), 100);
            }
          }
        };
        
        waitForElements();
      }
    }
    
    // Fetch and populate tags from API (fetch for all users to show selected labels)
    const labelPanel = document.getElementById('label-dropdown-panel');
    if (labelPanel) {
      fetchAndPopulateTags();
    }
    
    // Fetch and populate users from API
    const contributorPanel = document.getElementById('contributor-dropdown-panel');
    if (contributorPanel) {
      fetchAndPopulateUsers();
    }
    
    // Function to fetch tags from API and populate dropdown
    function fetchAndPopulateTags() {
      // Fetching tags from API
      
      fetch('/json/tags', {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        },
        credentials: 'same-origin'
      })
      .then(response => {
        if (!response.ok) {
          throw new Error('Failed to fetch tags: ' + response.status);
        }
        return response.json();
      })
      .then(data => {
        // Tags received        
        // Check if data is an array or has a data property
        const tags = Array.isArray(data) ? data : (data.data || []);
        
        // Populate the label dropdown
        const dropdownContent = document.getElementById('label-dropdown-content');
        dropdownContent.innerHTML = '';
        
        if (tags.length === 0) {
          dropdownContent.innerHTML = '<div style="padding: 10px; color: #666;">No tags available</div>';
        } else {
          tags.forEach(tag => {
            const label = document.createElement('label');
            label.className = 'label-checkbox-item';
            
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = tag;
            checkbox.setAttribute('data-label', tag);
            
            const span = document.createElement('span');
            span.textContent = tag;
            
            label.appendChild(checkbox);
            label.appendChild(span);
            dropdownContent.appendChild(label);
          });
          
          // Tags populated successfully
        }
        
        // Now setup event listeners with the populated elements
        setupLabelEventListeners();
        
        // Mark tags as loaded and check if we can restore filters
        tagsLoaded = true;
        checkAndRestoreFilters();
      })
      .catch(error => {
        // Show error message in dropdown
        const dropdownContent = document.getElementById('label-dropdown-content');
        dropdownContent.innerHTML = '<div style="padding: 10px; color: #d32f2f;">Failed to load tags</div>';
        
        // Still setup event listeners even if fetch fails
        setupLabelEventListeners();
        
        // Mark as loaded even on error so we don't block restoration
        tagsLoaded = true;
        checkAndRestoreFilters();
      });
    }
    
    // Setup label select event listeners
    function setupLabelEventListeners() {
      const labelInputWrapper = document.getElementById('label-input-wrapper');
      const labelPanel = document.getElementById('label-dropdown-panel');
      const labelSearchInput = document.getElementById('label-search-input');
      let isDropdownOpen = false;
      
      // Function to get current checkboxes and items (dynamic)
      function getLabelCheckboxes() {
        return labelPanel.querySelectorAll('input[type="checkbox"]');
      }
      
      function getAllLabelItems() {
        return labelPanel.querySelectorAll('.label-checkbox-item');
      }
      
      // Click on wrapper to toggle dropdown (only if user can modify)
      labelInputWrapper.addEventListener('click', function(e) {
        
        // Don't toggle if clicking on a chip or the input
        if (e.target.classList.contains('label-chip-item') || 
            e.target.classList.contains('label-chip-cross') ||
            e.target === labelSearchInput) {
          return;
        }
        
        if (!isDropdownOpen) {
          openDropdown();
        }
      });
      
      // Click on input to open dropdown
      labelSearchInput.addEventListener('click', function(e) {
        e.stopPropagation();
        if (!isDropdownOpen) {
          openDropdown();
        }
      });
      
      // Search functionality - filter as user types
      labelSearchInput.addEventListener('input', function() {
        const searchText = this.value.toLowerCase();
        
        // Auto-open dropdown when typing
        if (!isDropdownOpen) {
          openDropdown();
        }
        
        // Filter options
        const allLabelItems = getAllLabelItems();
        allLabelItems.forEach(item => {
          const label = item.querySelector('span').textContent.toLowerCase();
          item.style.display = label.includes(searchText) ? 'flex' : 'none';
        });
      });
      
      function openDropdown() {
        isDropdownOpen = true;
        labelPanel.classList.add('show');
        toggleChevronIcon(labelInputWrapper, true);
        
        // Clear search input and focus it
        labelSearchInput.value = '';
        labelSearchInput.focus();
        
        // Show all items
        const allLabelItems = getAllLabelItems();
        allLabelItems.forEach(item => item.style.display = 'flex');
      }
      
      function closeDropdown() {
        isDropdownOpen = false;
        labelPanel.classList.remove('show');
        toggleChevronIcon(labelInputWrapper, false);
        
        // Update display when closing
        updateLabelInputDisplay();
      }
      
      // Close panel when clicking outside
      document.addEventListener('click', function(e) {
        if (!labelInputWrapper.contains(e.target) && !labelPanel.contains(e.target) && isDropdownOpen) {
          closeDropdown();
        }
      });
      
      // Handle checkbox changes using event delegation
      labelPanel.addEventListener('change', function(e) {
        if (e.target.type === 'checkbox') {
          e.stopPropagation();
          e.stopImmediatePropagation();          
          // Clear search text when selecting
          labelSearchInput.value = '';
          
          // Show all items again
          const allLabelItems = getAllLabelItems();
          allLabelItems.forEach(item => item.style.display = 'flex');
          
          updateLabelInputDisplay();
          filterLabels();
          
          // Trigger search after checkbox change
          setTimeout(triggerSearchWithFilters, 300);
          
          // Close dropdown after selection
          closeDropdown();
          
          return false;
        }
      });
      
      // Function to update input display with selected values
      updateLabelInputDisplay = function() {
        const labelCheckboxes = getLabelCheckboxes();
        const selectedCheckboxes = Array.from(labelCheckboxes).filter(cb => cb.checked);
        const container = document.getElementById('label-selected-items');
        
        // Clear container
        container.innerHTML = '';
        
        if (selectedCheckboxes.length > 0) {
          // Create clickable chips for each selected item
          selectedCheckboxes.forEach(cb => {
            const label = cb.getAttribute('data-label');
            const chip = document.createElement('span');
            chip.className = 'label-chip-item';
            chip.setAttribute('data-value', cb.value);
            
            // Add label text
            const labelText = document.createElement('span');
            labelText.className = 'label-chip-label';
            labelText.textContent = label;
            chip.appendChild(labelText);
            
            // Add cross mark
            const crossMark = document.createElement('span');
            crossMark.className = 'label-chip-cross';
            crossMark.innerHTML = '&times;';
            chip.appendChild(crossMark);
            
            // Add click handler to remove
            chip.addEventListener('click', function(e) {
              e.stopPropagation();
              cb.checked = false;
              updateLabelInputDisplay();
              filterLabels();
              // Trigger search after removal
              setTimeout(triggerSearchWithFilters, 300);
            });
            
            container.appendChild(chip);
          });

          // Always show placeholder even with chips
          labelSearchInput.placeholder = 'Please select...';
        } else {
          // Always show placeholder
          labelSearchInput.placeholder = 'Please select...';
        }
      };
      
      // Initialize display state
      updateLabelInputDisplay();
    }
    
    // Function to fetch users from API and populate contributor dropdown
    function fetchAndPopulateUsers() {
      // Fetching users from API
      
      fetch('/json/users', {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        },
        credentials: 'same-origin'
      })
      .then(response => {
        if (!response.ok) {
          throw new Error('Failed to fetch users: ' + response.status);
        }
        return response.json();
      })
      .then(data => {
        // Users received
        
        // Check if data is an array or has a data property
        const users = Array.isArray(data) ? data : (data.data || []);
        
        // Populate the contributor dropdown
        const dropdownContent = document.getElementById('contributor-dropdown-content');
        dropdownContent.innerHTML = '';
        
        if (users.length === 0) {
          dropdownContent.innerHTML = '<div style="padding: 10px; color: #666;">No users available</div>';
        } else {
          users.forEach(user => {
            const label = document.createElement('label');
            label.className = 'contributor-checkbox-item';
            
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = user.id;
            checkbox.setAttribute('data-name', user.displayName);
            checkbox.setAttribute('data-email', user.email || '');
            
            const span = document.createElement('span');
            
            const nameSpan = document.createElement('span');
            nameSpan.className = 'contributor-name';
            nameSpan.textContent = user.displayName;
            
            const emailSpan = document.createElement('span');
            emailSpan.className = 'contributor-email';
            emailSpan.textContent = user.email || '';
            
            span.appendChild(nameSpan);
            span.appendChild(emailSpan);
            
            label.appendChild(checkbox);
            label.appendChild(span);
            dropdownContent.appendChild(label);
          });
          
          // Users populated successfully
        }
        
        // Now setup event listeners with the populated elements
        setupContributorEventListeners();
        
        // Mark users as loaded and check if we can restore filters
        usersLoaded = true;
        checkAndRestoreFilters();
      })
      .catch(error => {
        // Show error message in dropdown
        const dropdownContent = document.getElementById('contributor-dropdown-content');
        dropdownContent.innerHTML = '<div style="padding: 10px; color: #d32f2f;">Failed to load users</div>';
        
        // Still setup event listeners even if fetch fails
        setupContributorEventListeners();
        
        // Mark as loaded even on error so we don't block restoration
        usersLoaded = true;
        checkAndRestoreFilters();
      });
    }
    
    // Setup contributor select event listeners
    function setupContributorEventListeners() {
      const contributorInputWrapper = document.getElementById('contributor-input-wrapper');
      const contributorPanel = document.getElementById('contributor-dropdown-panel');
      const contributorSearchInput = document.getElementById('contributor-search-input');
      let isContributorDropdownOpen = false;
      
      // Function to get current checkboxes and items (dynamic)
      function getContributorCheckboxes() {
        return contributorPanel.querySelectorAll('input[type="checkbox"]');
      }
      
      function getAllContributorItems() {
        return contributorPanel.querySelectorAll('.contributor-checkbox-item');
      }
      
      // Click on wrapper to toggle dropdown
      contributorInputWrapper.addEventListener('click', function(e) {
        // Don't toggle if clicking on a chip or the input
        if (e.target.classList.contains('contributor-chip-item') || 
            e.target.classList.contains('contributor-chip-cross') ||
            e.target === contributorSearchInput) {
          return;
        }
        
        if (!isContributorDropdownOpen) {
          openContributorDropdown();
        }
      });
      
      // Click on input to open dropdown
      contributorSearchInput.addEventListener('click', function(e) {
        e.stopPropagation();
        if (!isContributorDropdownOpen) {
          openContributorDropdown();
        }
      });
      
      // Search functionality - filter as user types
      contributorSearchInput.addEventListener('input', function() {
        const searchText = this.value.toLowerCase();
        
        // Auto-open dropdown when typing
        if (!isContributorDropdownOpen) {
          openContributorDropdown();
        }
        
        // Filter options - search both name and email
        const allContributorItems = getAllContributorItems();
        allContributorItems.forEach(item => {
          const checkbox = item.querySelector('input[type="checkbox"]');
          const name = checkbox.getAttribute('data-name').toLowerCase();
          const email = checkbox.getAttribute('data-email').toLowerCase();
          const matches = name.includes(searchText) || email.includes(searchText);
          item.style.display = matches ? 'flex' : 'none';
        });
      });
      
      function openContributorDropdown() {
        isContributorDropdownOpen = true;
        contributorPanel.classList.add('show');
        toggleChevronIcon(contributorInputWrapper, true);
        
        // Clear search input and focus it
        contributorSearchInput.value = '';
        contributorSearchInput.focus();
        
        // Show all items
        const allContributorItems = getAllContributorItems();
        allContributorItems.forEach(item => item.style.display = 'flex');
      }
      
      function closeContributorDropdown() {
        isContributorDropdownOpen = false;
        contributorPanel.classList.remove('show');
        toggleChevronIcon(contributorInputWrapper, false);
        
        // Update display when closing
        updateContributorInputDisplay();
      }
      
      // Close panel when clicking outside
      document.addEventListener('click', function(e) {
        if (!contributorInputWrapper.contains(e.target) && !contributorPanel.contains(e.target) && isContributorDropdownOpen) {
          closeContributorDropdown();
        }
      });
      
      // Handle checkbox changes using event delegation
      contributorPanel.addEventListener('change', function(e) {
        if (e.target.type === 'checkbox') {
          e.stopPropagation();
          e.stopImmediatePropagation();
          
          // Clear search text when selecting
          contributorSearchInput.value = '';
          
          // Show all items again
          const allContributorItems = getAllContributorItems();
          allContributorItems.forEach(item => item.style.display = 'flex');
          
          updateContributorInputDisplay();
          filterContributor();
          
          // Trigger search after checkbox change
          setTimeout(triggerSearchWithFilters, 300);
          
          // Close dropdown after selection
          closeContributorDropdown();
          
          return false;
        }
      });
      
      // Function to update input display with selected values
      updateContributorInputDisplay = function() {
        const contributorCheckboxes = getContributorCheckboxes();
        const selectedCheckboxes = Array.from(contributorCheckboxes).filter(cb => cb.checked);
        const container = document.getElementById('contributor-selected-items');
        
        // Clear container
        container.innerHTML = '';
        
        if (selectedCheckboxes.length > 0) {
          // Create clickable chips for each selected item
          selectedCheckboxes.forEach(cb => {
            const userName = cb.getAttribute('data-name');
            const chip = document.createElement('span');
            chip.className = 'contributor-chip-item';
            chip.setAttribute('data-value', cb.value);
            
            // Add name text
            const nameText = document.createElement('span');
            nameText.className = 'contributor-chip-label';
            nameText.textContent = userName;
            chip.appendChild(nameText);
            
            // Add cross mark
            const crossMark = document.createElement('span');
            crossMark.className = 'contributor-chip-cross';
            crossMark.innerHTML = '&times;';
            chip.appendChild(crossMark);
            
            // Add click handler to remove
            chip.addEventListener('click', function(e) {
              e.stopPropagation();
              cb.checked = false;
              updateContributorInputDisplay();
              filterContributor();
              // Trigger search after removal
              setTimeout(triggerSearchWithFilters, 300);
            });
            
            container.appendChild(chip);
          });
        }
        
        // Always show placeholder
        contributorSearchInput.placeholder = 'Please select...';
      };
      
      // Initialize display state
      updateContributorInputDisplay();
    }
    
    // Attach last modified filter handlers
    const lastModifiedRadios = document.querySelectorAll('input[name="last-modified"]');
    const customDateRange = document.getElementById('custom-date-range');
    let lastModifiedCustomVisible = false;
    
    lastModifiedRadios.forEach(radio => {
      const label = radio.nextElementSibling;
      
      label.addEventListener('click', function(e) {
        // Prevent ALL default behaviors and propagation
        e.preventDefault();
        e.stopPropagation();
        e.stopImmediatePropagation();
        
        // If clicking on custom
        if (radio.value === 'custom') {
          // Toggle visibility
          lastModifiedCustomVisible = !lastModifiedCustomVisible;
          customDateRange.style.display = lastModifiedCustomVisible ? 'block' : 'none';
          
          // If hiding, clear date values and constraints
          if (!lastModifiedCustomVisible) {
            clearDateInputs();
          }
          
          // Always keep custom radio checked when toggling
          radio.checked = true;
        } else {
          // For other options, hide custom and select this option
          lastModifiedCustomVisible = false;
          customDateRange.style.display = 'none';
          
          // Clear date values and constraints when switching away from custom
          clearDateInputs();
          
          radio.checked = true;
          
          // Manually trigger change event (programmatic changes don't auto-trigger)
          const changeEvent = new Event('change', { bubbles: true });
          radio.dispatchEvent(changeEvent);
        }
        
        // Store filter data
        filterLastModified(radio);
        
        // Do NOT return anything - stay on page
        return false;
      });
    });
    
    // Attach custom date input handlers
    const startDateInput = document.getElementById('start-date');
    const endDateInput = document.getElementById('end-date');
    
    if (startDateInput && endDateInput) {
      startDateInput.addEventListener('change', function() {
        filterLastModified(document.querySelector('input[name="last-modified"]:checked'));
        updateDateConstraint(this, endDateInput, 'min');
        
        // Only trigger search if BOTH dates are filled
        if (this.value && endDateInput.value) {
          setTimeout(triggerSearchWithFilters, 300);
        }
      });
      
      endDateInput.addEventListener('change', function() {
        filterLastModified(document.querySelector('input[name="last-modified"]:checked'));
        updateDateConstraint(this, startDateInput, 'max');
        
        // Only trigger search if BOTH dates are filled
        if (this.value && startDateInput.value) {
          setTimeout(triggerSearchWithFilters, 300);
        }
      });
    }
    
    // Attach of-type filter handlers
    const ofTypeRadios = document.querySelectorAll('input[name="of-type"]');
    
    ofTypeRadios.forEach(radio => {
      const label = radio.nextElementSibling;
      
      label.addEventListener('click', function(e) {
        // Prevent ALL default behaviors and propagation
        e.preventDefault();
        e.stopPropagation();
        e.stopImmediatePropagation();
        
        // Set radio as checked
        radio.checked = true;
        
        // Manually trigger change event (programmatic changes don't auto-trigger)
        const changeEvent = new Event('change', { bubbles: true });
        radio.dispatchEvent(changeEvent);
        
        // Store filter data
        filterOfType(radio);        
        // Do NOT return anything - stay on page
        return false;
      });
    });
  });
  
  function toggleFilterSection(sectionId) {
    const sectionContent = document.getElementById('filter-section-' + sectionId);
    const title = sectionContent.previousElementSibling;
    
    if (sectionContent.classList.contains('hidden')) {
      sectionContent.classList.remove('hidden');
      title.classList.remove('collapsed');
    } else {
      sectionContent.classList.add('hidden');
      title.classList.add('collapsed');
    }
  }
  
  function filterLastModified(radio) {
    if (!radio) return;
    
    const selectedValue = radio.value;
    const filterData = {
      type: selectedValue
    };
    
    // If custom is selected, get the date range
    if (selectedValue === 'custom') {
      const startDate = document.getElementById('start-date').value;
      const endDate = document.getElementById('end-date').value;
      
      if (startDate) filterData.startDate = startDate;
      if (endDate) filterData.endDate = endDate;
    }    
  }
  
  function filterLabels() {
    // Get all selected checkboxes (no-op if the Tags section is disabled/not rendered)
    const labelPanel = document.getElementById('label-dropdown-panel');
    if (!labelPanel) {
      return;
    }
    const selectedLabels = Array.from(labelPanel.querySelectorAll('input[type="checkbox"]:checked'))
      .map(cb => cb.value);    
  }
  
  function filterOfType(radio) {
    if (!radio) return;
    
    const selectedValue = radio.value;
    const filterData = {
      type: selectedValue
    };
    
    setTimeout(triggerSearchWithFilters, 300);
  }
  
  function filterContributor() {
    // Get all selected checkboxes (no-op if the Contributor section is disabled/not rendered)
    const contributorPanel = document.getElementById('contributor-dropdown-panel');
    if (!contributorPanel) {
      return;
    }
    const selectedContributors = Array.from(contributorPanel.querySelectorAll('input[type="checkbox"]:checked'))
      .map(cb => ({
        id: cb.value,
        name: cb.getAttribute('data-name')
      }));    
  }
  
  // ========================================
  // FILTER TRIGGER FUNCTIONALITY
  // ========================================
  
  // Function to collect all active filters
  function collectActiveFilters() {
    const filters = {
      labelFilter: [],
      modifiedAfter: null,
      modifiedBefore: null,
      dateFilterType: null,  // Track which radio button is selected
      contributorFilter: [],
      ofType: null  // Track which "Of Type" radio is selected
    };
    
    // Collect Label filter (tags)
    const labelCheckboxes = document.querySelectorAll(SELECTORS.LABEL_CHECKBOXES);
    labelCheckboxes.forEach(cb => {
      filters.labelFilter.push(cb.value);
    });
    
    // Collect Last Modified filter and calculate dates on frontend
    const lastModifiedRadio = document.querySelector('input[name="last-modified"]:checked');
    if (lastModifiedRadio && lastModifiedRadio.value !== 'any') {
      // Store which radio was selected
      filters.dateFilterType = lastModifiedRadio.value;
      
      if (lastModifiedRadio.value === 'custom') {
        // Get custom date range from inputs
        const startDate = document.getElementById('start-date');
        const endDate = document.getElementById('end-date');
        if (startDate && startDate.value) {
          filters.modifiedAfter = startDate.value;
        }
        if (endDate && endDate.value) {
          filters.modifiedBefore = endDate.value;
        }
      } else if (DATE_OFFSET_DAYS[lastModifiedRadio.value]) {
        // Calculate date based on offset
        const daysAgo = DATE_OFFSET_DAYS[lastModifiedRadio.value];
        const pastDate = new Date(Date.now() - (daysAgo * MS_PER_DAY));
        filters.modifiedAfter = pastDate.toISOString().split('T')[0];
      }
    }
    
    // Collect Contributor filter
    const contributorCheckboxes = document.querySelectorAll(SELECTORS.CONTRIBUTOR_CHECKBOXES);
    contributorCheckboxes.forEach(cb => {
      filters.contributorFilter.push(cb.value);
    });
    
    // Collect Of Type filter
    const ofTypeRadio = document.querySelector('input[name="of-type"]:checked');
    if (ofTypeRadio && ofTypeRadio.value !== 'all') {
      filters.ofType = ofTypeRadio.value;
    }
    return filters;
  }
  
  // Function to trigger search with current filters (AJAX - no page reload)
  function triggerSearchWithFilters() {
    // Skip if we're restoring filters from URL (already on correct results page)
    if (isRestoringFilters) {
      return;
    }    
    
    // Get current query parameter from URL
    const urlParams = new URLSearchParams(window.location.search);
    const currentQuery = urlParams.get('query') || '';
    
    const currentLabel = urlParams.get('label') || '';
    const currentOfType = urlParams.get('ofType') || '';
    const currentModifiedAfter = urlParams.get('modifiedAfter') || '';
    const currentModifiedBefore = urlParams.get('modifiedBefore') || '';
    const currentContributor = urlParams.get('contributorFilter') || '';

    const filters = collectActiveFilters();

    // New values from filters
    const newLabel = filters.labelFilter.join(',');
    const newOfType = filters.ofType || '';
    const newModifiedAfter = filters.modifiedAfter || '';
    const newModifiedBefore = filters.modifiedBefore || '';
    const newContributor = filters.contributorFilter.join(',');

    // Only skip if EVERYTHING is same
    if (
      currentLabel === newLabel &&
      currentOfType === newOfType &&
      currentModifiedAfter === newModifiedAfter &&
      currentModifiedBefore === newModifiedBefore &&
      currentContributor === newContributor
    ) {
      return;
    }


    // Build new URL with filters
    const newUrl = new URL(window.location.origin + '/search');
    newUrl.searchParams.set('query', currentQuery);
    
    // Add label filter - use 'label' parameter for advanced search (from query string)
    if (filters.labelFilter.length > 0) {
      newUrl.searchParams.set('label', filters.labelFilter.join(','));
    }
    
    // Add date filters (calculated on frontend)
    if (filters.modifiedAfter) {
      newUrl.searchParams.set('modifiedAfter', filters.modifiedAfter);
    }
    if (filters.modifiedBefore) {
      newUrl.searchParams.set('modifiedBefore', filters.modifiedBefore);
    }
    // Add dateFilterType to remember which radio was selected
    if (filters.dateFilterType) {
      newUrl.searchParams.set('dateFilterType', filters.dateFilterType);
    }
    
    // Add contributor filter
    if (filters.contributorFilter.length > 0) {
      newUrl.searchParams.set('contributorFilter', filters.contributorFilter.join(','));
    }
    if (filters.ofType) {
      newUrl.searchParams.set('ofType', filters.ofType);
    }
    
    const targetUrl = newUrl.toString();
    
    // Show loading state
    const currentMainContent = document.querySelector('.search-main-content');
    if (currentMainContent) {
      const loadingDiv = document.createElement('div');
      loadingDiv.style.cssText = 'position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(0,0,0,0.8); color: white; padding: 20px; border-radius: 8px; z-index: 9999;';
      loadingDiv.textContent = 'Loading results...';
      loadingDiv.id = 'ajax-loading-indicator';
      document.body.appendChild(loadingDiv);
    }

    <!-- <%-- // Handle via page reload until the new URL structure is supported by backend --%> -->
    window.location.href = targetUrl;
    return;
    
    // Update URL without reload using History API
    window.history.pushState({filters: filters}, '', targetUrl);
    
    // Fetch search results via AJAX
    fetch(targetUrl, {
      method: 'GET',
      headers: {
        'X-Requested-With': 'XMLHttpRequest'
      }
    })
    .then(response => {
      if (!response.ok) {
        throw new Error('Network response was not ok: ' + response.status);
      }
      return response.text();
    })
    .then(html => {
      
      // Remove loading indicator
      const loadingIndicator = document.getElementById('ajax-loading-indicator');
      if (loadingIndicator) {
        loadingIndicator.remove();
      }
      
      // Parse the HTML response
      const parser = new DOMParser();
      const doc = parser.parseFromString(html, 'text/html');
      
      // Find the main content column in the response (where search results are displayed)
      const newMainContent = doc.querySelector('.search-main-content');
      
      // Find the current main content column in the page
      const currentMainContent = document.querySelector('.search-main-content');
      
      if (newMainContent && currentMainContent) {
        // Replace the main content (preserving the sidebar)
        currentMainContent.innerHTML = newMainContent.innerHTML;        
        // Scroll to top of results
        window.scrollTo({top: 0, behavior: 'smooth'});
      } else {
        window.location.href = targetUrl;
      }
    })
    .catch(error => {
      // Remove loading indicator
      const loadingIndicator = document.getElementById('ajax-loading-indicator');
      if (loadingIndicator) loadingIndicator.remove();
      
      window.location.href = targetUrl;
    });
  }
  
  // Setup Of Type filter event listeners (similar pattern to labels/contributors)
  function setupOfTypeEventListeners() {
    
    // Get all Of Type radio buttons
    const ofTypeRadios = document.querySelectorAll('input[name="of-type"]');
    
    if (ofTypeRadios.length === 0) {
      return;
    }
    
    // Add direct click listener to each radio button as a backup
    ofTypeRadios.forEach((radio, index) => {
      
      radio.addEventListener('click', function(e) {
        if (isRestoringFilters) {
          return;
        }
        // Trigger search
        setTimeout(function() {
          triggerSearchWithFilters();
        }, 300);
      });
    });
  }
  
  // Setup filter change listeners
  function setupFilterChangeListeners() {
    
    // Label checkboxes are handled by labelPanel event listener above (line ~384)
    // Contributor checkboxes are handled by contributorPanel event listener above (line ~624)
    // Custom date inputs are handled by old event listeners above (lines ~733-748)
    // These all have e.stopPropagation(), so we only need to handle radio buttons here
    
    // Use global document listener for all radio buttons (most reliable approach)
    document.addEventListener('change', function(e) {
      
      // Last Modified radio buttons - trigger search when changed
      if (e.target.matches('input[name="last-modified"]')) {
        
        // Skip if restoring filters from URL
        if (isRestoringFilters) {
          return;
        }
        
        // For custom, wait for date input
        if (e.target.value !== 'custom') {
          setTimeout(triggerSearchWithFilters, 300);
        }
        return; // Important: prevent other handlers from running
      }
      
      // Of Type radio buttons - trigger search when changed
      if (e.target.matches('input[name="of-type"]')) {
        // Skip if restoring filters from URL
        if (isRestoringFilters) {
          return;
        }
        
        // Trigger search for all selections
        setTimeout(function() {
          triggerSearchWithFilters();
        }, 300);
        return; // Important: prevent other handlers from running
      }
      });
    }
  
  // Function to restore filter states from URL parameters on page load
  function restoreFiltersFromURL() {
    // Set flag to prevent triggering search during restoration
    isRestoringFilters = true;
    
    
    const urlParams = new URLSearchParams(window.location.search);    
    // STEP 1: Restore Last Modified filter (radio buttons)
    // Skipped entirely when the section is disabled/not rendered
    const dateFilterType = SECTIONS_ENABLED.lastModified ? urlParams.get('dateFilterType') : null;
    
    if (dateFilterType && dateFilterType !== 'any') {
      // Find all radio buttons
      const allRadios = document.querySelectorAll('input[name="last-modified"]');      
      if (allRadios.length === 0) {
      } else {
        // LOG ALL RADIO BUTTON VALUES TO DEBUG
        allRadios.forEach((radio, index) => {
          const val = radio.getAttribute('value');
          const prop = radio.value;
        });
        // Find the target radio
        const targetRadio = document.querySelector(`input[name="last-modified"][value="${dateFilterType}"]`);        
        // If not found, try a different approach - search manually
        if (!targetRadio) {
          let foundRadio = null;
          allRadios.forEach(radio => {
            if (radio.getAttribute('value') === dateFilterType || radio.value === dateFilterType) {
              foundRadio = radio;
            }
          });
          if (foundRadio) {
            foundRadio.checked = true;
            const label = foundRadio.nextElementSibling;
            if (label) void label.offsetHeight;
            // DON'T dispatch change event - would cause reload loop
          }
        } else {        
          // Set checked state
          targetRadio.checked = true;        
          // Force browser to recalculate styles by accessing offsetHeight
          const label = targetRadio.nextElementSibling;
          if (label) {
            void label.offsetHeight; // Force reflow
          }
          // If custom, show and populate date inputs
          if (dateFilterType === 'custom') {
            const modifiedAfter = urlParams.get('modifiedAfter');
            const modifiedBefore = urlParams.get('modifiedBefore');
            const customDateRange = document.getElementById('custom-date-range');
            if (customDateRange) {
              customDateRange.style.display = 'block';
            }
            
            const startDateInput = document.getElementById('start-date');
            const endDateInput = document.getElementById('end-date');
            if (startDateInput && modifiedAfter) {
              startDateInput.value = modifiedAfter;
              // Set constraint: end date must be after start date
              if (endDateInput) {
                endDateInput.min = modifiedAfter;
              }
            }
            if (endDateInput && modifiedBefore) {
              endDateInput.value = modifiedBefore;
              // Set constraint: start date must be before end date
              if (startDateInput) {
                startDateInput.max = modifiedBefore;
              }
            }
          }
        }
      } // end else (allRadios.length > 0)
    }
    
    // STEP 2: Restore Label filter (checkboxes and chips) - for all users (read-only for non-privileged)
    // Skipped entirely when the Tags section is disabled/not rendered
    const labelFilter = SECTIONS_ENABLED.tags ? urlParams.get('label') : null;
    
    // Build label values from the active URL parameter - this will be used to check checkboxes and create chips
    let labelValues = [];
    if (labelFilter) {
      const urlLabels = labelFilter.split(',')
        .map(v => v.trim())
        .filter(v => v);

      urlLabels.forEach(label => {
        if (!labelValues.includes(label)) {
          labelValues.push(label);
        }
      });
    }

    if (labelValues.length > 0) {
      
      // DEBUG: Show what checkboxes actually exist
      const allLabelCheckboxes = document.querySelectorAll('#label-dropdown-content input[type="checkbox"]');
      
      const labelSelectedContainer = document.getElementById('label-selected-items');
      if (labelSelectedContainer) {
        labelSelectedContainer.innerHTML = ''; // Clear existing chips
      }
      
      let labelCount = 0;
      labelValues.forEach(labelValue => {        
        // Try exact match first
        let checkbox = document.querySelector(`#label-dropdown-content input[type="checkbox"][value="${labelValue}"]`);
        
        // If not found, try case-insensitive match
        if (!checkbox) {
          const allCheckboxes = document.querySelectorAll('#label-dropdown-content input[type="checkbox"]');
          checkbox = Array.from(allCheckboxes).find(cb => cb.value.toLowerCase() === labelValue.toLowerCase());
        }
        
        if (checkbox) {
          // Check the checkbox
          checkbox.checked = true;
          labelCount++;
          
          // Force style recalculation to apply :checked CSS
          const labelElement = checkbox.parentElement;
          if (labelElement) {
            void labelElement.offsetHeight; // Force reflow
          }
          
          // Create visual chip directly (self-contained, no function dependency)
          if (labelSelectedContainer) {
            const chip = document.createElement('span');
            chip.className = 'label-chip-item';
            chip.setAttribute('data-value', labelValue);
            
            // Add label text
            const labelText = document.createElement('span');
            labelText.className = 'label-chip-label';
            labelText.textContent = checkbox.getAttribute('data-label') || labelValue;
            chip.appendChild(labelText);

            const crossMark = document.createElement('span');
            crossMark.className = 'label-chip-cross';
            crossMark.innerHTML = '&times;';
            chip.appendChild(crossMark);

            chip.addEventListener('click', function(e) {
              e.stopPropagation();
              checkbox.checked = false;
              chip.remove();
              filterLabels();

              const searchInput = document.getElementById('label-search-input');
              if (searchInput && labelSelectedContainer.children.length === 0) {
                searchInput.placeholder = 'Please select...';
              }

              setTimeout(triggerSearchWithFilters, 300);
            });
            
            labelSelectedContainer.appendChild(chip);
          }
        }
      });
    }
    
    // STEP 3: Restore Contributor filter (checkboxes and chips)
    // Skipped entirely when the Contributor section is disabled/not rendered
    const contributorFilter = SECTIONS_ENABLED.contributors ? urlParams.get('contributorFilter') : null;
    
    if (contributorFilter) {
      const contributorIds = contributorFilter.split(',').map(v => v.trim()).filter(v => v);
      
      // DEBUG: Show what checkboxes actually exist
      const allContributorCheckboxes = document.querySelectorAll('#contributor-dropdown-content input[type="checkbox"]');
      
      const contributorSelectedContainer = document.getElementById('contributor-selected-items');
      if (contributorSelectedContainer) {
        contributorSelectedContainer.innerHTML = ''; // Clear existing chips
      }
      
      let contributorCount = 0;
      contributorIds.forEach(contributorId => {
        
        // Try exact match first
        let checkbox = document.querySelector(`#contributor-dropdown-content input[type="checkbox"][value="${contributorId}"]`);
        
        // If not found, try case-insensitive match
        if (!checkbox) {
          const allCheckboxes = document.querySelectorAll('#contributor-dropdown-content input[type="checkbox"]');
          checkbox = Array.from(allCheckboxes).find(cb => cb.value.toLowerCase() === contributorId.toLowerCase());
        }
        
        if (checkbox) {
          // Check the checkbox
          checkbox.checked = true;
          contributorCount++;
          
          // Force style recalculation to apply :checked CSS
          const labelElement = checkbox.parentElement;
          if (labelElement) {
            void labelElement.offsetHeight; // Force reflow
          }
          
          // Create visual chip directly (self-contained, no function dependency)
          if (contributorSelectedContainer) {
            const chip = document.createElement('span');
            chip.className = 'contributor-chip-item';
            chip.setAttribute('data-value', contributorId);
            
            // Add name text
            const nameText = document.createElement('span');
            nameText.className = 'contributor-chip-label';
            nameText.textContent = checkbox.getAttribute('data-name') || contributorId;
            chip.appendChild(nameText);
            
            // Add cross mark
            const crossMark = document.createElement('span');
            crossMark.className = 'contributor-chip-cross';
            crossMark.innerHTML = '&times;';
            chip.appendChild(crossMark);
            
            // Add click handler to remove
            chip.addEventListener('click', function(e) {
              e.stopPropagation();
              checkbox.checked = false;
              chip.remove();
              // Clear placeholder if needed
              const searchInput = document.getElementById('contributor-search-input');
              if (searchInput && contributorSelectedContainer.children.length === 0) {
                searchInput.placeholder = 'Please select...';
              }
              // Trigger search after removal
              setTimeout(triggerSearchWithFilters, 300);
            });
            
            contributorSelectedContainer.appendChild(chip);
          }
        }
      });
      
    }
    
    // STEP 4: Restore Of Type filter (radio buttons and custom type checkboxes)
    // Skipped entirely when the Of Type section is disabled/not rendered
    const ofType = SECTIONS_ENABLED.types ? urlParams.get('ofType') : null;
    
    if (ofType && ofType !== 'all') {
      
      // Find all Of Type radio buttons for debugging
      const allOfTypeRadios = document.querySelectorAll('input[name="of-type"]');
      
      if (allOfTypeRadios.length === 0) {
      } else {
        // LOG ALL RADIO BUTTON VALUES TO DEBUG
        allOfTypeRadios.forEach((radio, index) => {
          const val = radio.getAttribute('value');
          const prop = radio.value;
        });
        // Find the target radio
        let ofTypeRadio = document.querySelector(`input[name="of-type"][value="${ofType}"]`);
        
        // If not found, try manual search
        if (!ofTypeRadio) {
          allOfTypeRadios.forEach(radio => {
            if (radio.getAttribute('value') === ofType || radio.value === ofType) {
              ofTypeRadio = radio;
            }
          });
        }
        
        if (ofTypeRadio) {
          
          // Set checked state
          ofTypeRadio.checked = true;
          
          // Force browser to recalculate styles by accessing offsetHeight
          const label = ofTypeRadio.nextElementSibling;
          if (label) {
            void label.offsetHeight; // Force reflow
          }
        }
      }
    }
    
    // Final status
    const checkedRadio = document.querySelector('input[name="last-modified"]:checked');
    const checkedLabels = document.querySelectorAll('#label-dropdown-content input[type="checkbox"]:checked');
    const checkedContributors = document.querySelectorAll('#contributor-dropdown-content input[type="checkbox"]:checked');
    const checkedOfType = document.querySelector('input[name="of-type"]:checked');
    const labelChips = document.querySelectorAll('#label-selected-items .label-chip-item');
    const contributorChips = document.querySelectorAll('#contributor-selected-items .contributor-chip-item');
    
    // Verify visual styling is applied
    if (checkedRadio) {
      const radioLabel = checkedRadio.nextElementSibling;
      if (radioLabel) {
        // Force one final reflow to ensure styles are applied
        void radioLabel.offsetHeight;
        
        const computedStyle = window.getComputedStyle(radioLabel);
        // Check if expected style is applied
        const bgColor = computedStyle.backgroundColor;
      }
    }
    
    // Verify Of Type radio button visual styling
    if (checkedOfType && checkedOfType.value !== 'all') {
      const ofTypeLabel = checkedOfType.nextElementSibling;
      if (ofTypeLabel) {
        // Force reflow to ensure styles are applied
        void ofTypeLabel.offsetHeight;
        
        const computedStyle = window.getComputedStyle(ofTypeLabel);
        // Check if expected style is applied
        const bgColor = computedStyle.backgroundColor;
      }
    }
    
    if (checkedLabels.length > 0) {
      const firstCheckedLabel = checkedLabels[0];
      const labelSpan = firstCheckedLabel.nextElementSibling;
      if (labelSpan) {
        // Force one final reflow
        void labelSpan.offsetHeight;
        
        const computedStyle = window.getComputedStyle(labelSpan);
      }
    }  

    const currentParams = new URLSearchParams(window.location.search);
    const currentQuery = currentParams.get('query') || '';

    const finalLabels = labelValues.join(',');
    const currentLabel = currentParams.get('label') || '';

    if (currentLabel !== finalLabels) {
      const newUrl = new URL(window.location.origin + '/search');

      newUrl.searchParams.set('query', currentQuery);

      if (finalLabels) {
        newUrl.searchParams.set('label', finalLabels);
      }

      const ofType = currentParams.get('ofType');
      if (ofType) newUrl.searchParams.set('ofType', ofType);

      const contributor = currentParams.get('contributorFilter');
      if (contributor) newUrl.searchParams.set('contributorFilter', contributor);

      const modifiedAfter = currentParams.get('modifiedAfter');
      if (modifiedAfter) newUrl.searchParams.set('modifiedAfter', modifiedAfter);

      const modifiedBefore = currentParams.get('modifiedBefore');
      if (modifiedBefore) newUrl.searchParams.set('modifiedBefore', modifiedBefore);

      // Update URL WITHOUT reload
      window.history.replaceState({}, '', newUrl.toString());

      // Trigger search with correct filters
    }

    // Clear restoration flag
    isRestoringFilters = false;
    setTimeout(triggerSearchWithFilters, 100);

  }
  
  // Make restoration function globally accessible for debugging
  window.restoreFiltersFromURL = restoreFiltersFromURL;
  
  // Manual test function for Of Type filter
  window.testOfTypeFilter = function(value) {
    const radio = document.querySelector(`input[name="of-type"][value="${value}"]`);
    if (radio) {
      radio.checked = true;
      triggerSearchWithFilters();
    }
  };
  // Handle browser back/forward buttons
  window.addEventListener('popstate', function(event) {
    // Reload the page to show the correct results for the URL
    window.location.reload();
  });
  
  // Initialize filter listeners when DOM is fully loaded
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
      setupOfTypeEventListeners();
      setupFilterChangeListeners();
    });
  } else {
    // DOM already loaded
    setupOfTypeEventListeners();
    setupFilterChangeListeners();
  }
  
</script>
