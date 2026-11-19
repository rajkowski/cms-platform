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
(function (global) {
  function normalizeValue(value) {
    return String(value || '').trim().toLowerCase();
  }

  function collectColumnValues(table, columnIndex) {
    const values = [];
    const rows = table.querySelectorAll('tbody > tr');

    rows.forEach((row) => {
      const cell = row.cells[columnIndex];
      if (!cell) {
        return;
      }

      // skip the first row
      if (row.rowIndex === 0) {
        return;
      }

      const rawText = cell.textContent.trim();
      if (rawText) {
        values.push({ raw: rawText, normalized: normalizeValue(rawText) });
      }
    });

    return values;
  }

  function getHeaderCells(table) {
    const theadCells = table.querySelectorAll('thead > tr > th');
    if (theadCells.length > 0) {
      return Array.from(theadCells);
    }

    const firstRow = table.querySelector('tbody > tr');
    if (!firstRow) {
      return [];
    }

    const firstRowHeaderCells = Array.from(firstRow.querySelectorAll(':scope > th'));
    if (firstRowHeaderCells.length > 0) {
      return firstRowHeaderCells;
    }

    return [];
  }

  function shouldAttachToTable(table) {
    if (!table || !table.querySelector('tbody')) {
      return false;
    }

    const bodyRows = table.querySelectorAll('tbody > tr');
    if (bodyRows.length < 6) {
      return false;
    }

    if (table.querySelector('th[rowspan], td[rowspan], th[colspan], td[colspan]')) {
      return false;
    }

    return true;
  }

  function buildFilterControls(table) {
    if (!shouldAttachToTable(table)) {
      return null;
    }

    const headerCells = getHeaderCells(table);
    const controls = [];

    headerCells.forEach((headerCell, index) => {
      const values = collectColumnValues(table, index);
      const normalizedValues = values.map((value) => value.normalized);
      const uniqueValues = Array.from(new Set(normalizedValues));

      if (uniqueValues.length < 1) {
        return;
      }

      const headerTitle = (headerCell.textContent || '').trim() || `Column ${index + 1}`;
      const titleWrapper = document.createElement('span');
      titleWrapper.className = 'table-filter-title';

      while (headerCell.firstChild) {
        titleWrapper.appendChild(headerCell.firstChild);
      }

      headerCell.appendChild(titleWrapper);

      // Defaults to work on light or dark backgrounds, but can be overridden with CSS
      const dropdown = document.createElement('div');
      dropdown.className = 'table-filter-dropdown';
      dropdown.hidden = true;
      dropdown.style.color = '#000000';
      dropdown.style.minWidth = '160px';
      dropdown.style.boxShadow = '0 2px 4px rgba(0, 0, 0, 0.1)';
      dropdown.style.fontSize = '14px';

      const select = document.createElement('select');
      select.className = 'table-filter-control';
      select.dataset.columnIndex = String(index);
      select.setAttribute('aria-label', headerTitle);
      select.style.color = 'inherit';
      select.style.fontFamily = 'inherit';
      select.style.fontSize = 'inherit';
      select.style.margin = '0';
      select.addEventListener('change', () => {
        applyFilters(table, controls);
      });

      const defaultOption = document.createElement('option');
      defaultOption.value = '';
      defaultOption.textContent = `All ${headerTitle}`;
      select.appendChild(defaultOption);

      uniqueValues.sort().forEach((value) => {
        const matchingItem = values.find((item) => item.normalized === value);
        const option = document.createElement('option');
        option.value = matchingItem ? matchingItem.raw : value;
        option.dataset.normalizedValue = value;
        option.textContent = matchingItem ? matchingItem.raw : value;
        select.appendChild(option);
      });

      dropdown.appendChild(select);
      headerCell.appendChild(dropdown);

      const control = { headerCell, select, dropdown };
      controls.push(control);
    });

    return controls;
  }

  function attachTableFilters(table) {
    const controls = buildFilterControls(table);
    if (!controls || controls.length === 0) {
      return;
    }

    // Single toggle button placed inline after the first column's header text
    const toggleButton = document.createElement('button');
    toggleButton.type = 'button';
    toggleButton.className = 'table-filter-trigger';
    toggleButton.setAttribute('aria-label', 'Toggle column filters');
    toggleButton.style.border = 'none';
    toggleButton.style.cursor = 'pointer';
    toggleButton.style.fontSize = 'inherit';
    toggleButton.style.marginLeft = '3px';
    toggleButton.style.padding = '0 1px';
    toggleButton.style.background = '#f5f5f5';
    toggleButton.style.setProperty('border-radius', '2px', 'important');

    const icon = document.createElement('i');
    icon.className = 'fa-solid fa-filter';
    icon.style.padding = '1px 2px';
    icon.style.fontSize = '12px';
    toggleButton.appendChild(icon);

    // when the toggle is closed, reset all drop-down controls and reapply filters
    toggleButton.addEventListener('click', (event) => {
      event.stopPropagation();
      const anyOpen = controls.some((ctrl) => !ctrl.dropdown.hidden);
      controls.forEach((ctrl) => {
        ctrl.dropdown.hidden = anyOpen;
      });
      if (anyOpen) {
        controls.forEach((ctrl) => {
          ctrl.select.value = '';
        });
        applyFilters(table, controls);
      }
    });

    controls[0].headerCell.insertBefore(toggleButton, controls[0].dropdown);
  }

  function applyFilters(table, controls) {
    const rows = table.querySelectorAll('tbody > tr');
    const head = table.querySelector('thead');
    const headerRows = table.querySelectorAll('thead > tr');

    if (head) {
      head.style.display = '';
    }

    headerRows.forEach((headerRow) => {
      headerRow.style.display = '';
    });

    rows.forEach((row) => {
      // Do not hide rows that contain a 'th'
      if (row.querySelector('th')) {
        return;
      }

      let matches = true;
      controls.forEach((control) => {
        const selectedValue = normalizeValue(control.select.value);
        if (!selectedValue) {
          return;
        }

        const columnIndex = Number(control.select.dataset.columnIndex || 0);
        const cell = row.cells[columnIndex];
        const cellValue = normalizeValue(cell ? cell.textContent : '');
        if (cellValue !== selectedValue) {
          matches = false;
        }
      });

      row.style.display = matches ? '' : 'none';
    });
  }

  function initTableFilters(root) {
    const container = root ? root.querySelector('.platform-content') : null;
    if (!container) {
      return;
    }

    const tables = container.querySelectorAll('table');
    tables.forEach((table) => {
      attachTableFilters(table);
    });
  }

  global.initTableFilters = initTableFilters;

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { initTableFilters };
  }

  if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', () => initTableFilters(document));
  }
})(typeof window !== 'undefined' ? window : globalThis);
