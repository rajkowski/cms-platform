/*
 * Copyright 2026 Matt Rajkowski (https://www.github.com/rajkowski)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function (window) {
  'use strict';

  // Generate region options dynamically
  function generateRegionOptions() {
    const container = document.getElementById('regionOptionsContainer');
    if (!container) return;

    container.innerHTML = ''; // Clear existing content

    REGION_CONFIG.forEach(region => {
      const regionOption = document.createElement('div');
      regionOption.className = 'region-option';

      const input = document.createElement('input');
      input.type = 'radio';
      input.id = `region-${region.code}`;
      input.name = 'region';
      input.value = region.code; // Use region code as value
      input.checked = false; // Explicitly set to unchecked to prevent browser caching
      input.defaultChecked = false;

      // Add event listener for change events
      input.addEventListener('change', handleRegionChange);

      const label = document.createElement('label');
      label.setAttribute('for', `region-${region.code}`);
      label.textContent = region.name;

      // Make label clickable - add click handler to ensure selection works
      label.addEventListener('click', function (e) {
        e.preventDefault();
        input.checked = true;
        input.dispatchEvent(new Event('change', { bubbles: true }));
      });

      regionOption.appendChild(input);
      regionOption.appendChild(label);
      container.appendChild(regionOption);
    });

    if (CAN_UNSET_REGION) {
      const unsetOption = document.createElement('div');
      unsetOption.className = 'region-option';

      const input = document.createElement('input');
      input.type = 'radio';
      input.id = 'region-unset';
      input.name = 'region';
      input.value = ''; // IMPORTANT: empty/null

      input.addEventListener('change', handleRegionUnset);

      const label = document.createElement('label');
      label.setAttribute('for', 'region-unset');
      label.textContent = 'Unset';

      // label click support
      label.addEventListener('click', function (e) {
        e.preventDefault();
        input.checked = true;
        input.dispatchEvent(new Event('change', { bubbles: true }));
      });

      unsetOption.appendChild(input);
      unsetOption.appendChild(label);
      container.appendChild(unsetOption);
    }
  }

  function handleRegionUnset() {
    if (!CAN_UNSET_REGION) return;
    sessionStorage.setItem('regionUnsetByUser', 'true');

    // Store explicit NULL markers
    Cookies.set(REGION_COOKIE_CODE, 'null', { expires: REGION_COOKIE_EXPIRY_DAYS });
    Cookies.set(REGION_COOKIE_VALUES, 'null', { expires: REGION_COOKIE_EXPIRY_DAYS });

    updateRegionButtonText(null);
    setRegionFilter(null);

    // Clear filters
    const savedLabels = JSON.parse(sessionStorage.getItem('selectedLabels') || '[]');
    const filteredLabels = savedLabels.filter(label => !REGION_CODES.includes(label));
    sessionStorage.setItem('selectedLabels', JSON.stringify(filteredLabels));
    sessionStorage.removeItem('selectedContributors');
    sessionStorage.removeItem('lastModifiedFilter');
    sessionStorage.removeItem('ofTypeFilter');

    setTimeout(() => {
      const url = new URL(window.location.href);
      const params = url.searchParams;
      if (params.has('label')) {
        const labels = params.get('label')
          .split(',')
          .map(l => l.trim())
          .filter(l => l && !REGION_CODES.includes(l));

        if (labels.length > 0) {
          params.set('label', labels.join(','));
        } else {
          params.delete('label'); // remove param completely if empty
        }
      }
      // Keep everything else (query, ofType, etc.)
      window.location.href = url.toString();

    }, 100);
  }

  // Flag to track if we're in auto-selection mode
  let isAutoSelecting = false;

  // Region Modal Functions
  function openRegionModal() {
    const modal = document.getElementById('regionModal');
    if (modal) {
      modal.style.display = 'block';
      document.body.style.overflow = 'hidden';

      // Force-clear all radio buttons immediately to prevent browser cache issues
      const regionForm = document.getElementById('regionFilterForm');
      if (regionForm) {
        const allRadios = regionForm.querySelectorAll('input[name="region"]');
        allRadios.forEach(radio => {
          radio.checked = false;
          radio.removeAttribute('checked');
          radio.defaultChecked = false;
        });
      }

      // Wait for modal to be fully rendered, then apply region selection
      setTimeout(() => {
        isAutoSelecting = true; // Set flag before auto-selection
        applyRegionSelectionToForm();

        // Clear flag after auto-selection is complete
        setTimeout(() => {
          isAutoSelecting = false;
        }, 500);
      }, 100);
    }
  }

  function closeRegionModal() {
    const modal = document.getElementById('regionModal');
    if (modal) {
      modal.style.display = 'none';
      document.body.style.overflow = 'auto';
    }
  }

  // Expose functions used by inline onclick handlers in the JSP markup
  window.openRegionModal = openRegionModal;
  window.closeRegionModal = closeRegionModal;

  // Apply region selection to the modal form specifically
  function applyRegionSelectionToForm() {
    // Read from cookies ONLY - don't use server session for modal pre-selection
    // Cookies are the source of truth; if cleared, modal should show blank
    const savedRegionCode = Cookies.get(REGION_COOKIE_CODE);

    // Get the specific form
    const regionForm = document.getElementById('regionFilterForm');
    if (!regionForm) {
      return false;
    }

    // Get radio buttons specifically from the form
    const formRadios = regionForm.querySelectorAll('input[name="region"]');
    if (formRadios.length === 0) {
      return false;
    }

    // If no cookie exists, clear all radio buttons and exit
    if (!savedRegionCode || savedRegionCode === '' || savedRegionCode === 'undefined') {
      formRadios.forEach(radio => {
        radio.checked = false;
        radio.removeAttribute('checked');
        radio.defaultChecked = false;
      });
      return false;
    }

    // STEP 2: handle "null" (UNSET)
    if (savedRegionCode === 'null') {
      formRadios.forEach(radio => {
        radio.checked = false;
        radio.removeAttribute('checked');
      });

      // IMPORTANT → check role
      if (CAN_UNSET_REGION) {
        const unsetRadio = regionForm.querySelector('#region-unset');

        if (unsetRadio) {
          unsetRadio.checked = true;
          unsetRadio.setAttribute('checked', 'checked');
          unsetRadio.defaultChecked = true;
        }
      }

      updateRegionButtonText(null);
      return true;
    }

    // Find the specific radio button in the form
    let targetRadio = null;
    formRadios.forEach(radio => {
      if (radio.value === savedRegionCode) {
        targetRadio = radio;
      }
    });

    if (!targetRadio) {
      // Clear all selections if the saved region code doesn't exist in options
      formRadios.forEach(radio => {
        radio.checked = false;
        radio.removeAttribute('checked');
        radio.defaultChecked = false;
      });
      return false;
    }

    // Clear all radio buttons in the form first
    formRadios.forEach(radio => {
      radio.checked = false;
      radio.removeAttribute('checked');
    });

    // Apply selection with multiple methods
    targetRadio.checked = true;
    targetRadio.setAttribute('checked', 'checked');
    targetRadio.defaultChecked = true;

    // Update UI immediately
    updateRegionButtonText(savedRegionCode);

    // Verify selection worked
    setTimeout(() => {
      const verifyChecked = regionForm.querySelector('input[name="region"]:checked');

      if (!verifyChecked || verifyChecked.value !== savedRegionCode) {
        // Emergency fix: use click() method
        try {
          targetRadio.focus();
          targetRadio.click();
        } catch (e) {
          // Silent fail
        }
      }
    }, 150);

    return true;
  }

  // Save region selection to cookies (UserSession will read from cookies on next request)
  function saveRegionToSession(regionCode) {
    if (!regionCode || regionCode === 'null') {
      Cookies.set(REGION_COOKIE_CODE, 'null', { expires: REGION_COOKIE_EXPIRY_DAYS });
      Cookies.set(REGION_COOKIE_VALUES, 'null', { expires: REGION_COOKIE_EXPIRY_DAYS });
      return;
    }
    if (regionCode && regionCode !== 'null' && regionCode !== 'undefined' && regionCode !== '') {
      // Find the region config to get the full values array
      const region = REGION_CONFIG.find(r => r.code === regionCode);
      if (region) {
        // Save to cookies (replacing sessionStorage)
        Cookies.set(REGION_COOKIE_CODE, regionCode, { expires: REGION_COOKIE_EXPIRY_DAYS });
        Cookies.set(REGION_COOKIE_VALUES, JSON.stringify(region.values), { expires: REGION_COOKIE_EXPIRY_DAYS });

      } else {
        // Fallback to just the region code as array
        Cookies.set(REGION_COOKIE_CODE, regionCode, { expires: REGION_COOKIE_EXPIRY_DAYS });
        Cookies.set(REGION_COOKIE_VALUES, JSON.stringify([regionCode]), { expires: REGION_COOKIE_EXPIRY_DAYS });
      }
    }
  }

  // Auto-save and apply when region selection changes
  function handleRegionChange(event) {
    const regionCode = event.target.value;

    // Don't auto-close if this is triggered by auto-selection
    if (isAutoSelecting) {
      return;
    }
    sessionStorage.removeItem('regionUnsetByUser');
    // Save to cookies with full values array
    saveRegionToSession(regionCode);

    // Reload page to update UserSession on server
    // This triggers WebRequestFilter to read new cookies and update UserSession
    setTimeout(() => {
      window.location.reload();
    }, 100);
  }

  function updateRegionButtonText(regionValue) {
    const regionBtn = document.querySelector('.region-filter-trigger .btn-text');
    const regionButtonElement = document.querySelector('.region-filter-trigger');

    if (regionBtn && regionButtonElement) {
      // Find the region name from REGION_CONFIG or show default "Region" text
      if (!regionValue || regionValue === 'null' || regionValue === '') {
        regionBtn.textContent = 'Region';
        regionButtonElement.classList.remove('has-active-filter');
      } else {
        const region = REGION_CONFIG.find(r => r.code === regionValue);
        const displayName = region ? region.name : regionValue;
        regionBtn.textContent = displayName;
        regionButtonElement.classList.add('has-active-filter');
      }
    }
  }

  function filterContentByRegion(regionValue) {
    if (!regionValue || regionValue === 'null' || regionValue === '') {
      document.querySelectorAll('[data-region]').forEach(element => {
        element.style.display = '';
      });
      return;
    }

    // Get the region config to find the values array
    const region = REGION_CONFIG.find(r => r.code === regionValue);
    const allowedValues = region ? region.values : [regionValue];

    // Hide content that doesn't match any of the allowed region values
    document.querySelectorAll('[data-region]').forEach(element => {
      const elementRegions = element.getAttribute('data-region').split(',').map(r => r.trim());

      // Check if any of the element's regions match any of the allowed values
      const shouldShow = allowedValues.some(allowedValue =>
        elementRegions.includes(allowedValue) || elementRegions.includes('global')
      );
      element.style.display = shouldShow ? '' : 'none';
    });
  }

  function setRegionFilter(regionValue) {
    // Also call the standard data-region filtering
    filterContentByRegion(regionValue);

    if (!regionValue || regionValue === 'null' || regionValue === '') {
      // Show all elements when no region is selected
      document.querySelectorAll('.tag.button[class*="label-"]').forEach(element => {
        element.style.display = '';
      });
      return;
    }

    // Get the region config to find the values array (e.g., ['na'])
    const region = REGION_CONFIG.find(r => r.code === regionValue);
    const allowedValues = region ? region.values : [regionValue];

    // Filter elements with label-* CSS classes (e.g., label-1, label-2)
    document.querySelectorAll('.tag.button').forEach(element => {
      // Check if element has any label-* class
      const hasLabelClass = Array.from(element.classList).some(cls => cls.startsWith('label-'));

      if (!hasLabelClass) {
        // No region label - show it for all regions (global)
        element.style.display = '';
        return;
      }

      // Get all label classes from the element (e.g., ['label-1', 'label-2'])
      const labelClasses = Array.from(element.classList).filter(cls => cls.startsWith('label-'));

      // Extract region codes from label classes (e.g., 'label-na-us' -> 'na-us')
      const elementRegions = labelClasses.map(cls => cls.replace('label-', ''));

      // Check if any of the element's regions match any of the allowed values
      const shouldShow = allowedValues.some(allowedValue =>
        elementRegions.includes(allowedValue)
      );

      if (shouldShow) {
        element.style.display = '';
      } else {
        element.style.display = 'none';
      }
    });
  }

  // Close modal when clicking outside
  window.addEventListener('click', function (event) {
    const modal = document.getElementById('regionModal');
    if (event.target === modal) {
      closeRegionModal();
    }
  });

  // Handle escape key for modal
  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
      closeRegionModal();
    }
  });

  // Function to initialize region selection on page load
  function initializeRegionSelection() {
    // Get current region from cookies or server
    const savedRegionCode = Cookies.get(REGION_COOKIE_CODE);
    const serverRegion = USER_REGION_CODE;
    if (!CAN_UNSET_REGION) {
      sessionStorage.removeItem('regionUnsetByUser');
    }

    // Determine current region (cookie has priority)
    let currentRegion = null;
    if (savedRegionCode && savedRegionCode !== 'null') {
      currentRegion = savedRegionCode;
    } else if (serverRegion && serverRegion !== 'null' && serverRegion !== '' && serverRegion !== 'undefined') {
      currentRegion = serverRegion;
    }

    // Update UI with current region
    if (currentRegion) {
      updateRegionButtonText(currentRegion);
      if (typeof setRegionFilter === 'function') {
        setRegionFilter(currentRegion);
      } else {
        filterContentByRegion(currentRegion);
      }
    } else {
      updateRegionButtonText(null);
      if (typeof setRegionFilter === 'function') {
        setRegionFilter(null);
      } else {
        filterContentByRegion(null);
      }

      // Show modal if no region preference exists
      const wasUnsetByUser = sessionStorage.getItem('regionUnsetByUser');
      const isExplicitNull = savedRegionCode === 'null';

      if (!wasUnsetByUser && !isExplicitNull) {
        setTimeout(() => {
          openRegionModal();
        }, 1000);
      }
    }
  };

  document.addEventListener('DOMContentLoaded', function () {
    // Generate region options
    generateRegionOptions();

    // Check and initialize region selection
    initializeRegionSelection();
  });

})(window);
