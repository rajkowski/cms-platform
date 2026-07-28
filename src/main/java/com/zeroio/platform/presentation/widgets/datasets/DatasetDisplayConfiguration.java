/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
package com.zeroio.platform.presentation.widgets.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Type-safe preference parsing and validation for DatasetDisplayWidget
 * 
 * Handles deployment-time configuration of:
 * - firstcolumn: Column to move to first position
 * - headings: Custom column heading labels
 * - pageSize: Number of records per page
 * - sortBy: Column name to sort by
 * - reverseSort: Whether to reverse sort order
 * - cql: Simple filtering criteria (key=value format)
 * - id: Widget configuration ID for tracking
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class DatasetDisplayConfiguration {

  private static final Log LOG = LogFactory.getLog(DatasetDisplayConfiguration.class);

  // User Story 1: Column Display Order (P1)
  private String firstColumn;

  // User Story 2: Custom Column Headings (P1)
  private String[] customHeadings;
  private List<String> customHeadingsList;

  // User Story 3: Records Per Page Control (P1)
  private int pageSize = 25; // Default to 25 records per page
  private boolean pageSizeValid = true;

  // User Story 4: Default Sort Column (P2)
  private String sortByColumn;

  // User Story 5: Reverse Sort Direction (P2)
  private boolean reverseSort = false;

  // User Story 6: CQL Data Filtering (P2)
  private String cqlFilter;
  private List<String> filterCriteria;
  private boolean cqlValid = true;

  // User Story 7: Widget Configuration ID (P3)
  private String widgetId;

  /**
   * Private constructor - use factory method fromPreferences() instead
   */
  private DatasetDisplayConfiguration() {
  }

  /**
   * Create configuration from widget preferences map
   * 
   * @param preferences map of widget preferences
   * @return DatasetDisplayConfiguration instance
   */
  public static DatasetDisplayConfiguration fromPreferences(Map<String, String> preferences) {
    DatasetDisplayConfiguration config = new DatasetDisplayConfiguration();

    if (preferences == null) {
      return config;
    }

    // Parse User Story 1: firstcolumn
    config.parseFirstColumn(preferences.get("firstcolumn"));

    // Parse User Story 2: headings
    config.parseHeadings(preferences.get("headings"));

    // Parse User Story 3: pageSize
    config.parsePageSize(preferences.get("pageSize"));

    // Parse User Story 4: sortBy
    config.parseSortBy(preferences.get("sortBy"));

    // Parse User Story 5: reverseSort
    config.parseReverseSort(preferences.get("reverseSort"));

    // Parse User Story 6: cql
    config.parseCql(preferences.get("cql"));

    // Parse User Story 7: id
    config.parseWidgetId(preferences.get("id"));

    return config;
  }

  // ==================== Parser Methods ====================

  /**
   * Parse firstcolumn preference
   */
  private void parseFirstColumn(String value) {
    if (StringUtils.isBlank(value)) {
      this.firstColumn = null;
      return;
    }

    this.firstColumn = value.trim();
    LOG.debug("Parsed firstColumn: " + firstColumn);
  }

  /**
   * Parse headings preference (comma-separated)
   */
  private void parseHeadings(String value) {
    if (StringUtils.isBlank(value)) {
      this.customHeadings = null;
      this.customHeadingsList = null;
      return;
    }

    this.customHeadingsList = new ArrayList<>();
    String[] parts = value.split(",", -1); // Keep empty strings
    this.customHeadings = new String[parts.length];

    for (int i = 0; i < parts.length; i++) {
      String heading = parts[i].trim();
      this.customHeadings[i] = heading.isEmpty() ? null : heading;
      if (heading.isEmpty()) {
        this.customHeadingsList.add(null);
      } else {
        this.customHeadingsList.add(heading);
      }
    }

    LOG.debug("Parsed " + customHeadings.length + " custom headings");
  }

  /**
   * Parse and validate pageSize preference
   */
  private void parsePageSize(String value) {
    if (StringUtils.isBlank(value)) {
      this.pageSize = 25; // Default
      this.pageSizeValid = true;
      return;
    }

    try {
      int size = Integer.parseInt(value.trim());

      // Validate bounds
      if (size < 1) {
        LOG.warn("Invalid pageSize value: " + size + " (must be >= 1). Using default.");
        this.pageSize = 25;
        this.pageSizeValid = false;
        return;
      }

      this.pageSize = size;
      this.pageSizeValid = true;
      LOG.debug("Parsed pageSize: " + pageSize);

    } catch (NumberFormatException e) {
      LOG.warn("Invalid pageSize format: " + value + ". Using default.");
      this.pageSize = 25;
      this.pageSizeValid = false;
    }
  }

  /**
   * Parse sortBy column preference
   */
  private void parseSortBy(String value) {
    if (StringUtils.isBlank(value)) {
      this.sortByColumn = null;
      return;
    }

    this.sortByColumn = value.trim();
    LOG.debug("Parsed sortByColumn: " + sortByColumn);
  }

  /**
   * Parse reverseSort boolean preference
   */
  private void parseReverseSort(String value) {
    if (StringUtils.isBlank(value)) {
      this.reverseSort = false;
      return;
    }

    this.reverseSort = "true".equalsIgnoreCase(value.trim());
    LOG.debug("Parsed reverseSort: " + reverseSort);
  }

  /**
   * Parse and validate CQL filter preference
   * Supports multiple operators: =, in, not in
   * Examples: status=active, Labels in (draft, review), Labels:split(";") not in (draft)
   */
  private void parseCql(String value) {
    if (StringUtils.isBlank(value)) {
      this.cqlFilter = null;
      this.filterCriteria = new ArrayList<>();
      this.cqlValid = true;
      return;
    }

    this.cqlFilter = value.trim();
    this.filterCriteria = new ArrayList<>();
    this.cqlValid = true;

    // Parse comma-separated expressions (but not commas inside parentheses)
    List<String> clauses = splitClauses(cqlFilter);
    for (String clause : clauses) {
      String trimmed = clause.trim();
      if (trimmed.isEmpty()) {
        continue;
      }

      // Check if it's a valid CQL expression:
      // - Contains = (equals operator)
      // - Contains " in " (in operator)
      // - Contains " not in " (not in operator)
      if (trimmed.contains("=") || trimmed.contains(" in ") || trimmed.contains(" not in ")) {
        this.filterCriteria.add(trimmed);
      } else {
        LOG.warn("Invalid CQL syntax: " + trimmed + ". Expected format: key=value, key in (...), or key not in (...)");
        this.cqlValid = false;
      }
    }

    if (this.cqlValid) {
      LOG.debug("Parsed " + filterCriteria.size() + " CQL filter criteria");
    }
  }

  /**
   * Split CQL filter by top-level commas (not inside parentheses)
   */
  private List<String> splitClauses(String cqlFilter) {
    List<String> clauses = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    int depth = 0;

    for (int i = 0; i < cqlFilter.length(); i++) {
      char c = cqlFilter.charAt(i);

      if (c == '(') {
        depth++;
        current.append(c);
      } else if (c == ')') {
        depth--;
        current.append(c);
      } else if (c == ',' && depth == 0) {
        clauses.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }

    if (current.length() > 0) {
      clauses.add(current.toString());
    }

    return clauses;
  }

  /**
   * Parse widget ID preference
   */
  private void parseWidgetId(String value) {
    if (StringUtils.isBlank(value)) {
      this.widgetId = null;
      return;
    }

    this.widgetId = value.trim();
    LOG.debug("Parsed widgetId: " + widgetId);
  }

  // ==================== Getters and Validators ====================

  /**
   * Get the first column to display
   */
  public String getFirstColumn() {
    return firstColumn;
  }

  /**
   * Check if firstcolumn preference is set
   */
  public boolean hasFirstColumn() {
    return StringUtils.isNotBlank(firstColumn);
  }

  /**
   * Get custom column headings array
   */
  public String[] getCustomHeadings() {
    return customHeadings;
  }

  /**
   * Get custom column headings as list
   */
  public List<String> getCustomHeadingsList() {
    return customHeadingsList;
  }

  /**
   * Check if custom headings are configured
   */
  public boolean hasCustomHeadings() {
    return customHeadings != null && customHeadings.length > 0;
  }

  /**
   * Get effective heading for column index (custom or original)
   */
  public String getEffectiveHeading(String originalHeading, int columnIndex) {
    if (!hasCustomHeadings() || columnIndex >= customHeadings.length) {
      return originalHeading;
    }

    String customHeading = customHeadings[columnIndex];
    return StringUtils.isNotBlank(customHeading) ? customHeading : originalHeading;
  }

  /**
   * Get page size (records per page)
   */
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Check if pageSize is valid
   */
  public boolean isPageSizeValid() {
    return pageSizeValid;
  }

  /**
   * Get column to sort by
   */
  public String getSortByColumn() {
    return sortByColumn;
  }

  /**
   * Check if sorting is configured
   */
  public boolean hasSortBy() {
    return StringUtils.isNotBlank(sortByColumn);
  }

  /**
   * Check if reverse sort is enabled
   */
  public boolean isReverseSort() {
    return reverseSort;
  }

  /**
   * Get CQL filter expression
   */
  public String getCqlFilter() {
    return cqlFilter;
  }

  /**
   * Get parsed filter criteria as list of key=value strings
   */
  public List<String> getFilterCriteria() {
    return filterCriteria;
  }

  /**
   * Check if filtering is configured
   */
  public boolean hasCqlFilter() {
    return StringUtils.isNotBlank(cqlFilter);
  }

  /**
   * Check if CQL filter is valid
   */
  public boolean isCqlValid() {
    return cqlValid;
  }

  /**
   * Get widget configuration ID
   */
  public String getWidgetId() {
    return widgetId;
  }

  /**
   * Check if widget ID is configured
   */
  public boolean hasWidgetId() {
    return StringUtils.isNotBlank(widgetId);
  }

  /**
   * Utility: Validate that a column name exists in available columns
   */
  public static boolean isValidColumnName(String columnName, String[] fieldTitles) {
    if (StringUtils.isBlank(columnName) || fieldTitles == null) {
      return false;
    }

    for (String fieldTitle : fieldTitles) {
      if (fieldTitle.equalsIgnoreCase(columnName.trim())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Utility: Find column index by name (case-insensitive)
   */
  public static int findColumnIndex(String columnName, String[] fieldTitles) {
    if (StringUtils.isBlank(columnName) || fieldTitles == null) {
      return -1;
    }

    String searchName = columnName.trim();
    for (int i = 0; i < fieldTitles.length; i++) {
      if (fieldTitles[i].equalsIgnoreCase(searchName)) {
        return i;
      }
    }

    return -1;
  }

  // ==================== Debugging ====================

  @Override
  public String toString() {
    return "DatasetDisplayConfiguration{" +
        "firstColumn='" + firstColumn + '\'' +
        ", customHeadings=" + (customHeadings != null ? customHeadings.length : 0) +
        ", pageSize=" + pageSize +
        ", sortByColumn='" + sortByColumn + '\'' +
        ", reverseSort=" + reverseSort +
        ", cqlFilter='" + cqlFilter + '\'' +
        ", cqlValid=" + cqlValid +
        ", widgetId='" + widgetId + '\'' +
        '}';
  }
}
