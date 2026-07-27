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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.datasets.DatasetFileCommand;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.infrastructure.persistence.datasets.DatasetRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.datasets.DatasetFilterCommand;
import com.zeroio.platform.application.datasets.DatasetSortCommand;

/**
 * Dataset Display Widget
 * Displays a dataset in a table with optional paging, sorting, and filtering controls
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class DatasetDisplayWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String jsp = "/datasets/dataset-display.jsp";

  public WidgetContext execute(WidgetContext context) {
    try {
      // Common attributes
      context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
      context.getRequest().setAttribute("title", context.getPreferences().get("title"));

      // Load dataset and validate
      Dataset dataset = loadDataset(context);
      if (dataset == null) {
        context.setErrorMessage("Dataset could not be shown");
        context.setJsp(jsp);
        return context;
      }

      // Get preferences and render
      renderDataset(context, dataset);

    } catch (Exception e) {
      context.setErrorMessage("Dataset could not be shown: " + e.getMessage());
      LOG.warn("Error loading dataset: ", e);
    }

    context.setJsp(jsp);
    return context;
  }

  /**
   * Load the dataset from the page path
   */
  private Dataset loadDataset(WidgetContext context) {

    String datasetName = context.getPreferences().get("dataset");
    if (datasetName != null) {
      LOG.debug("Dataset name from preferences: " + datasetName);
    } else {
      datasetName = extractDatasetNameFromPath(context.getUri());
      if (datasetName != null) {
        LOG.debug("Dataset name extracted from path: " + datasetName);
      } else {
        LOG.debug("Skipping - dataset name not found in preferences or path");
        return null;
      }
    }

    Dataset dataset = DatasetRepository.findByName(datasetName);
    if (dataset == null) {
      LOG.debug("Skipping - dataset not found: " + datasetName);
    }
    return dataset;
  }

  /**
   * Render the dataset in the context
   */
  private void renderDataset(WidgetContext context, Dataset dataset) throws Exception {
    // Parse deployment-time preferences
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(context.getPreferences());

    // Get widget preferences with defaults
    boolean showPaging = getBooleanPreference(context, "showPaging", true);
    boolean showSort = getBooleanPreference(context, "showSort", false);
    boolean showFilter = getBooleanPreference(context, "showFilter", false);
    boolean showMetadata = getBooleanPreference(context, "showMetadata", false);
    boolean showWhenEmpty = getBooleanPreference(context, "showWhenEmpty", true);

    String columnsToDisplay = context.getPreferences().get("columns");

    // Use pageSize from configuration (defaults to 25)
    int recordsPerPage = config.getPageSize();

    // Get pagination parameters from request
    int pageNumber = context.getParameterAsInt("pageNumber", 1);
    int offset = (pageNumber - 1) * recordsPerPage;

    // Load ALL rows for sorting and total count (not paginated initially)
    List<String[]> allRows = DatasetFileCommand.loadRows(dataset, -1, true);

    if ((allRows == null || allRows.isEmpty()) && !showWhenEmpty) {
      LOG.debug("Dataset is empty and showWhenEmpty is false");
      return;
    }

    if (allRows == null) {
      allRows = new ArrayList<>();
    }

    // Apply CQL filtering if configured (User Story 6: cql filter)
    if (config.hasCqlFilter()) {
      LOG.debug("Applying CQL filter: " + config.getCqlFilter());
      DatasetFilterCommand.filterRows(allRows, config.getCqlFilter(), dataset.getFieldTitles());
    }

    // Apply sorting if configured (User Story 4 & 5: sortBy and reverseSort)
    if (config.hasSortBy()) {
      LOG.debug("Applying sort by column: " + config.getSortByColumn() + ", reverse: " + config.isReverseSort());
      DatasetSortCommand.sortRows(allRows, config.getSortByColumn(), dataset.getFieldTitles(), config.isReverseSort());
    }

    LOG.debug("Loaded " + allRows.size() + " rows for dataset display");

    // Calculate total record count from sorted rows
    int totalRecords = allRows.size();
    int totalPages = (int) Math.ceil((double) totalRecords / recordsPerPage);

    // Apply pagination: slice the sorted rows
    ArrayList<String[]> rows = new ArrayList<>();
    int endIndex = Math.min(offset + recordsPerPage, allRows.size());
    if (offset < allRows.size()) {
      rows = new ArrayList<>(allRows.subList(offset, endIndex));
    }

    // Parse columns to display with firstcolumn support (User Story 1)
    List<Integer> displayColumnIndices = buildColumnIndices(dataset.getFieldTitles(), columnsToDisplay, config);

    // Build headings in display order (User Story 2: Custom headings)
    List<String> displayHeadings = buildDisplayHeadings(dataset.getFieldTitles(), displayColumnIndices, config);

    // Resolve link rendering columns
    int convertColumnToLinkIndex = DatasetDisplayConfiguration.findColumnIndex(
        context.getPreferences().get("convertColumnToLink"), dataset.getFieldTitles());
    int columnIsLinkIndex = DatasetDisplayConfiguration.findColumnIndex(
        context.getPreferences().get("columnIsLink"), dataset.getFieldTitles());

    if (context.getPreferences().get("convertColumnToLink") != null && convertColumnToLinkIndex < 0) {
      LOG.warn("convertColumnToLink specified non-existent column: " + context.getPreferences().get("convertColumnToLink"));
    }
    if (context.getPreferences().get("columnIsLink") != null && columnIsLinkIndex < 0) {
      LOG.warn("columnIsLink specified non-existent column: " + context.getPreferences().get("columnIsLink"));
    }

    // Build dataset metadata
    if (showMetadata) {
      addMetadata(context, dataset, totalRecords);
    }

    // Log widget ID if configured (User Story 7)
    if (config.hasWidgetId()) {
      LOG.debug("Rendering widget with ID: " + config.getWidgetId());
    }

    // Prepare JSP objects
    context.getRequest().setAttribute("dataset", dataset);
    context.getRequest().setAttribute("rows", rows);
    context.getRequest().setAttribute("fieldTitles", dataset.getFieldTitles());
    context.getRequest().setAttribute("displayColumnIndices", displayColumnIndices);
    context.getRequest().setAttribute("displayHeadings", displayHeadings);
    context.getRequest().setAttribute("totalRecords", totalRecords);
    context.getRequest().setAttribute("totalPages", totalPages);
    context.getRequest().setAttribute("pageNumber", pageNumber);
    context.getRequest().setAttribute("recordsPerPage", recordsPerPage);
    context.getRequest().setAttribute("showPaging", showPaging);
    context.getRequest().setAttribute("showSort", showSort);
    context.getRequest().setAttribute("showFilter", showFilter);
    context.getRequest().setAttribute("showMetadata", showMetadata);
    context.getRequest().setAttribute("convertColumnToLinkIndex", convertColumnToLinkIndex);
    context.getRequest().setAttribute("columnIsLinkIndex", columnIsLinkIndex);

    // Add configuration for JSP (User Stories 2-7)
    context.getRequest().setAttribute("datasetConfig", config);
  }

  /**
   * Build list of column indices to display
   * Supports firstcolumn preference to move a column to first position (User Story 1)
   */
  private List<Integer> buildColumnIndices(String[] fieldTitles, String columnsToDisplay,
      DatasetDisplayConfiguration config) {
    List<Integer> displayColumnIndices = new ArrayList<>();

    if (StringUtils.isNotBlank(columnsToDisplay)) {
      String[] columnNames = columnsToDisplay.split(",");
      for (String columnName : columnNames) {
        int columnIndex = findColumnIndex(fieldTitles, columnName.trim());
        if (columnIndex >= 0) {
          displayColumnIndices.add(columnIndex);
        }
      }
    }

    if (displayColumnIndices.isEmpty()) {
      // Display all columns by default
      for (int i = 0; i < fieldTitles.length; i++) {
        displayColumnIndices.add(i);
      }
    }

    // Apply firstcolumn preference (User Story 1)
    if (config.hasFirstColumn()) {
      int firstColumnIndex = DatasetDisplayConfiguration.findColumnIndex(config.getFirstColumn(), fieldTitles);

      if (firstColumnIndex >= 0) {
        // Remove the first column from its current position if present
        displayColumnIndices.remove(Integer.valueOf(firstColumnIndex));

        // Add it to the beginning
        displayColumnIndices.add(0, firstColumnIndex);

        LOG.debug("Applied firstcolumn preference: moved column '" + config.getFirstColumn() + "' to first position");
      } else {
        // Column not found - log warning and continue with original order
        LOG.warn("firstcolumn preference specified non-existent column: " + config.getFirstColumn());
      }
    }

    return displayColumnIndices;
  }

  /**
   * Build effective headings array with custom heading overrides (User Story 2)
   * Supports partial overrides where customers can customize specific column headings
   */
  private List<String> buildDisplayHeadings(String[] fieldTitles, List<Integer> displayColumnIndices,
      DatasetDisplayConfiguration config) {
    List<String> displayHeadings = new ArrayList<>();

    for (int displayIndex = 0; displayIndex < displayColumnIndices.size(); displayIndex++) {
      int fieldIndex = displayColumnIndices.get(displayIndex);
      String originalHeading = fieldTitles[fieldIndex];
      String customHeading = config.getEffectiveHeading(originalHeading, displayIndex);
      displayHeadings.add(customHeading);
    }

    if (config.hasCustomHeadings()) {
      LOG.debug("Applied custom headings in display order: configured " + config.getCustomHeadings().length
          + " heading values");
    }

    return displayHeadings;
  }

  /**
   * Return a boolean preference value with a default when missing.
   */
  private boolean getBooleanPreference(WidgetContext context, String key, boolean defaultValue) {
    String value = context.getPreferences().get(key);
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    return "true".equalsIgnoreCase(value);
  }

  /**
   * Add metadata to the context
   */
  private void addMetadata(WidgetContext context, Dataset dataset, int totalRecords) {
    java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();

    if (dataset.getLastDownload() != null) {
      SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
      metadata.put("Last Updated", sdf.format(dataset.getLastDownload()));
    } else if (dataset.getCreated() != null) {
      SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
      metadata.put("Last Updated", sdf.format(dataset.getCreated()));
    }

    metadata.put("Total Records", String.valueOf(totalRecords));
    context.getRequest().setAttribute("metadata", metadata);
  }

  /**
   * Extract dataset name from page path
   * Expected format: /datasets/dataset-name
   * 
   * @param pagePath the page path
   * @return the dataset name or null if not found
   */
  private String extractDatasetNameFromPath(String pagePath) {
    if (StringUtils.isBlank(pagePath)) {
      return null;
    }

    String datasetName = pagePath.substring(pagePath.lastIndexOf("/") + 1);
    if (StringUtils.isBlank(datasetName)) {
      LOG.debug("Skipping - dataset name is blank");
      return null;
    }

    if (datasetName.contains("?")) {
      datasetName = datasetName.substring(0, datasetName.indexOf("?"));
    }

    datasetName = java.net.URLDecoder.decode(datasetName, java.nio.charset.StandardCharsets.UTF_8);

    return datasetName;
  }

  /**
   * Find the index of a column by name
   * 
   * @param fieldTitles array of field titles
   * @param columnName the column name to find
   * @return the index or -1 if not found
   */
  private int findColumnIndex(String[] fieldTitles, String columnName) {
    for (int i = 0; i < fieldTitles.length; i++) {
      if (fieldTitles[i].equalsIgnoreCase(columnName)) {
        return i;
      }
    }
    return -1;
  }
}
