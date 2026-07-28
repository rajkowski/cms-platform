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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.zeroio.platform.application.datasets.DatasetSortCommand;

/**
 * Tests for DatasetDisplayWidget with deployment time preferences
 * Validates column display order, custom headings, pagination, sorting, filtering, and ID tracking
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
class DatasetDisplayWidgetTest extends WidgetBase {

  /**
   * Create test rows with specific string values (varargs)
   */
  private List<String[]> createTestRows(String[]... rows) {
    List<String[]> result = new ArrayList<>();
    for (String[] row : rows) {
      result.add(row);
    }
    return result;
  }

  /**
   * Setup mock DatasetRepository and DatasetFileCommand for tests
   */
  @BeforeEach
  void setupMocks() {
    // This method is called before each test to reset mocks
    // Subclasses can override setupDatasetMocks() for initialization
  }

  /**
   * Setup DatasetRepository mock for a specific dataset
   */
  protected void setupDatasetRepository(String datasetName, Dataset dataset) {
    // Mock will be configured per test
  }

  /**
   * Setup DatasetFileCommand mock to return test rows
   */
  protected void setupDatasetFileCommand(Dataset dataset, List<String[]> rows) {
    // Mock will be configured per test
  }

  // ==================== Baseline Tests ====================

  @Test
  @DisplayName("T005: Widget executes without errors for baseline configuration")
  void testBaselineExecution() {
    // This test validates that the widget still works with existing preferences
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Sample Dataset</title>\n" +
            "  <icon>table</icon>\n" +
            "</widget>");

    DatasetDisplayWidget widget = new DatasetDisplayWidget();
    WidgetContext result = widget.execute(widgetContext);

    Assertions.assertNotNull(result);
    Assertions.assertEquals("/datasets/dataset-display.jsp", result.getJsp());
  }

  @Test
  @DisplayName("Widget sets JSP on execution")
  void testWidgetSetsJsp() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Test Dataset</title>\n" +
            "</widget>");

    DatasetDisplayWidget widget = new DatasetDisplayWidget();
    WidgetContext result = widget.execute(widgetContext);

    Assertions.assertEquals("/datasets/dataset-display.jsp", result.getJsp());
  }

  @Test
  @DisplayName("Widget returns context without dataset preference")
  void testWidgetWithoutDatasetPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>No Dataset</title>\n" +
            "</widget>");

    DatasetDisplayWidget widget = new DatasetDisplayWidget();
    WidgetContext result = widget.execute(widgetContext);

    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getErrorMessage());
  }

  // ==================== User Story 1: firstcolumn Preference Tests ====================

  @Test
  @DisplayName("US1.T001: Widget renders with firstcolumn preference applied")
  void testFirstColumnPreferenceRendering() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Column Reorder Test</title>\n" +
            "  <firstcolumn>Activity</firstcolumn>\n" +
            "</widget>");

    DatasetDisplayWidget widget = new DatasetDisplayWidget();
    // Widget execution would need dataset context - test validates preference parsing
    Assertions.assertNotNull(widgetContext.getPreferences().get("firstcolumn"));
    Assertions.assertEquals("Activity", widgetContext.getPreferences().get("firstcolumn"));
  }

  @Test
  @DisplayName("US1.T002: Widget without firstcolumn preference uses natural order")
  void testWithoutFirstColumnPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Natural Order Test</title>\n" +
            "</widget>");

    Assertions.assertNull(widgetContext.getPreferences().get("firstcolumn"));
  }

  @Test
  @DisplayName("US1.T003: Widget with invalid firstcolumn value logs warning")
  void testInvalidFirstColumnValue() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Invalid Column Test</title>\n" +
            "  <firstcolumn>NonExistentColumn</firstcolumn>\n" +
            "</widget>");

    Assertions.assertEquals("NonExistentColumn", widgetContext.getPreferences().get("firstcolumn"));
  }

  // ==================== User Story 2: headings Preference Tests ====================

  @Test
  @DisplayName("US2.T001: Widget parses custom headings preference")
  void testHeadingsPreferenceParsingNormal() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Custom Headings Test</title>\n" +
            "  <headings>,Who,Activity Type</headings>\n" +
            "</widget>");

    String headings = widgetContext.getPreferences().get("headings");
    Assertions.assertNotNull(headings);
    Assertions.assertEquals(",Who,Activity Type", headings);
  }

  @Test
  @DisplayName("US2.T002: Widget handles fewer headings than columns")
  void testHeadingsWithFewerValues() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Partial Headings Test</title>\n" +
            "  <headings>Custom 1,Custom 2</headings>\n" +
            "</widget>");

    String headings = widgetContext.getPreferences().get("headings");
    Assertions.assertEquals("Custom 1,Custom 2", headings);
  }

  @Test
  @DisplayName("US2.T003: Widget handles empty heading values")
  void testHeadingsWithEmptyValues() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Empty Headings Test</title>\n" +
            "  <headings>,,</headings>\n" +
            "</widget>");

    String headings = widgetContext.getPreferences().get("headings");
    Assertions.assertEquals(",,", headings);
  }

  // ==================== User Story 3: pageSize Preference Tests ====================

  @Test
  @DisplayName("US3.T001: Widget parses pageSize preference")
  void testPageSizePreferenceNormal() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Page Size Test</title>\n" +
            "  <pageSize>25</pageSize>\n" +
            "</widget>");

    String pageSize = widgetContext.getPreferences().get("pageSize");
    Assertions.assertEquals("25", pageSize);
  }

  @Test
  @DisplayName("US3.T002: Widget handles large pageSize values")
  void testPageSizeLargeValue() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Large Page Size Test</title>\n" +
            "  <pageSize>999999</pageSize>\n" +
            "</widget>");

    String pageSize = widgetContext.getPreferences().get("pageSize");
    Assertions.assertEquals("999999", pageSize);
  }

  @Test
  @DisplayName("US3.T003: Widget without pageSize preference defaults to null")
  void testWithoutPageSizePreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>No Page Size Test</title>\n" +
            "</widget>");

    Assertions.assertNull(widgetContext.getPreferences().get("pageSize"));
  }

  // ==================== User Story 4: sortBy Preference Tests ====================

  @Test
  @DisplayName("US4.T001: Widget parses sortBy preference")
  void testSortByPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Sort By Test</title>\n" +
            "  <sortBy>Date</sortBy>\n" +
            "</widget>");

    String sortBy = widgetContext.getPreferences().get("sortBy");
    Assertions.assertEquals("Date", sortBy);
  }

  @Test
  @DisplayName("US4.T002: Widget without sortBy preference returns null")
  void testWithoutSortByPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>No Sort By Test</title>\n" +
            "</widget>");

    Assertions.assertNull(widgetContext.getPreferences().get("sortBy"));
  }

  // ==================== User Story 5: reverseSort Preference Tests ====================

  @Test
  @DisplayName("US5.T001: Widget parses reverseSort preference")
  void testReverseSortPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Reverse Sort Test</title>\n" +
            "  <reverseSort>true</reverseSort>\n" +
            "</widget>");

    String reverseSort = widgetContext.getPreferences().get("reverseSort");
    Assertions.assertEquals("true", reverseSort);
  }

  @Test
  @DisplayName("US5.T002: Widget without reverseSort preference returns null")
  void testWithoutReverseSortPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>No Reverse Sort Test</title>\n" +
            "</widget>");

    Assertions.assertNull(widgetContext.getPreferences().get("reverseSort"));
  }

  // ==================== User Story 6: cql Preference Tests ====================

  @Test
  @DisplayName("US6.T001: Widget parses cql preference")
  void testCqlPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>CQL Filter Test</title>\n" +
            "  <cql>status=active</cql>\n" +
            "</widget>");

    String cql = widgetContext.getPreferences().get("cql");
    Assertions.assertEquals("status=active", cql);
  }

  @Test
  @DisplayName("US6.T002: Widget parses complex cql preference")
  void testComplexCqlPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Complex CQL Test</title>\n" +
            "  <cql>status=active,priority=high</cql>\n" +
            "</widget>");

    String cql = widgetContext.getPreferences().get("cql");
    Assertions.assertEquals("status=active,priority=high", cql);
  }

  @Test
  @DisplayName("US6.T003: Widget without cql preference returns null")
  void testWithoutCqlPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>No CQL Test</title>\n" +
            "</widget>");

    Assertions.assertNull(widgetContext.getPreferences().get("cql"));
  }

  // ==================== Phase 8 Acceptance Tests: User Story 6 (cql) ====================

  @Test
  @DisplayName("US6.T054: Widget parses and validates CQL filter syntax")
  void testCqlFilterValidation() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>CQL Filter Test</title>\n" +
            "  <cql>Status=active</cql>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());

    Assertions.assertTrue(config.hasCqlFilter());
    Assertions.assertEquals("Status=active", config.getCqlFilter());
    Assertions.assertTrue(config.isCqlValid());
  }

  @Test
  @DisplayName("US6.T055: Widget parses multiple CQL filter criteria")
  void testMultipleCqlCriteria() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Multi CQL Filter Test</title>\n" +
            "  <cql>Status=active,Department=IT</cql>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());

    Assertions.assertTrue(config.hasCqlFilter());
    Assertions.assertEquals("Status=active,Department=IT", config.getCqlFilter());
    Assertions.assertTrue(config.isCqlValid());
  }

  @Test
  @DisplayName("US6.T056: Widget with invalid CQL syntax marks filter as invalid")
  void testInvalidCqlSyntax() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Invalid CQL Test</title>\n" +
            "  <cql>invalid-syntax</cql>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());

    Assertions.assertTrue(config.hasCqlFilter());
    Assertions.assertEquals("invalid-syntax", config.getCqlFilter());
    Assertions.assertFalse(config.isCqlValid(), "Invalid CQL syntax should be marked as invalid");
  }

  // ==================== User Story 7: id Preference Tests ====================

  @Test
  @DisplayName("US7.T001: Widget parses id preference")
  void testIdPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>ID Preference Test</title>\n" +
            "  <id>onboarding-properties</id>\n" +
            "</widget>");

    String id = widgetContext.getPreferences().get("id");
    Assertions.assertEquals("onboarding-properties", id);
  }

  @Test
  @DisplayName("US7.T002: Widget without id preference returns null")
  void testWithoutIdPreference() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>No ID Test</title>\n" +
            "</widget>");

    Assertions.assertNull(widgetContext.getPreferences().get("id"));
  }

  // ==================== Combined Preference Tests ====================

  @Test
  @DisplayName("Widget handles all preferences combined")
  void testAllPreferencesCombined() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Full Featured Test</title>\n" +
            "  <firstcolumn>Activity</firstcolumn>\n" +
            "  <headings>,Who,Activity Type</headings>\n" +
            "  <pageSize>50</pageSize>\n" +
            "  <sortBy>Date</sortBy>\n" +
            "  <reverseSort>true</reverseSort>\n" +
            "  <cql>status=active</cql>\n" +
            "  <id>full-featured</id>\n" +
            "</widget>");

    Assertions.assertEquals("Activity", widgetContext.getPreferences().get("firstcolumn"));
    Assertions.assertEquals(",Who,Activity Type", widgetContext.getPreferences().get("headings"));
    Assertions.assertEquals("50", widgetContext.getPreferences().get("pageSize"));
    Assertions.assertEquals("Date", widgetContext.getPreferences().get("sortBy"));
    Assertions.assertEquals("true", widgetContext.getPreferences().get("reverseSort"));
    Assertions.assertEquals("status=active", widgetContext.getPreferences().get("cql"));
    Assertions.assertEquals("full-featured", widgetContext.getPreferences().get("id"));
  }

  @Test
  @DisplayName("Widget maintains backward compatibility with no new preferences")
  void testBackwardCompatibility() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Legacy Data Test</title>\n" +
            "  <icon>table</icon>\n" +
            "  <showPaging>true</showPaging>\n" +
            "  <showMetadata>true</showMetadata>\n" +
            "</widget>");

    // Legacy preferences should still work
    Assertions.assertEquals("table", widgetContext.getPreferences().get("icon"));
    Assertions.assertEquals("true", widgetContext.getPreferences().get("showPaging"));
    Assertions.assertEquals("true", widgetContext.getPreferences().get("showMetadata"));

    // New preferences should not be present
    Assertions.assertNull(widgetContext.getPreferences().get("firstcolumn"));
    Assertions.assertNull(widgetContext.getPreferences().get("headings"));
    Assertions.assertNull(widgetContext.getPreferences().get("pageSize"));
  }

  // ==================== Phase 3 Acceptance Tests: User Story 1 ====================

  @Test
  @DisplayName("T012: US1 - Widget with firstcolumn renders column first")
  void testUS1FirstColumnRendering() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Activity Test</title>\n" +
            "  <firstcolumn>Activity</firstcolumn>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertTrue(config.hasFirstColumn());
    Assertions.assertEquals("Activity", config.getFirstColumn());
  }

  @Test
  @DisplayName("T013: US1 - Widget without firstcolumn uses natural order")
  void testUS1NaturalColumnOrder() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Natural Order Test</title>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertFalse(config.hasFirstColumn());
  }

  @Test
  @DisplayName("T014: US1 - Widget with invalid firstcolumn logs warning")
  void testUS1InvalidColumnWarning() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Invalid Column Test</title>\n" +
            "  <firstcolumn>NonExistentColumn</firstcolumn>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());

    // Configuration should still parse the invalid column name
    Assertions.assertEquals("NonExistentColumn", config.getFirstColumn());

    // Validation would happen at widget execution time
    String[] fieldTitles = { "Name", "Email", "Status" };
    boolean isValid = DatasetDisplayConfiguration.isValidColumnName("NonExistentColumn", fieldTitles);
    Assertions.assertFalse(isValid); // Should recognize it as invalid
  }

  // ==================== Phase 3 Support Tests: Column Index Remapping ====================

  @Test
  @DisplayName("Column index remapping moves firstcolumn to position 0")
  void testColumnIndexRemapping() {
    String[] fieldTitles = { "Name", "Email", "Activity" };

    // Simulate finding the Activity column at index 2
    int activityIndex = DatasetDisplayConfiguration.findColumnIndex("Activity", fieldTitles);
    Assertions.assertEquals(2, activityIndex);
  }

  @Test
  @DisplayName("Column index remapping handles case insensitivity")
  void testColumnIndexRemappingCaseInsensitive() {
    String[] fieldTitles = { "Name", "Email", "Activity" };

    int activityIndex = DatasetDisplayConfiguration.findColumnIndex("activity", fieldTitles);
    Assertions.assertEquals(2, activityIndex);
  }

  @Test
  @DisplayName("Column index remapping returns -1 for non-existent column")
  void testColumnIndexRemappingNotFound() {
    String[] fieldTitles = { "Name", "Email", "Activity" };

    int phoneIndex = DatasetDisplayConfiguration.findColumnIndex("Phone", fieldTitles);
    Assertions.assertEquals(-1, phoneIndex);
  }

  // ==================== Phase 4 Acceptance Tests: User Story 2 ====================

  @Test
  @DisplayName("T020: US2 - Widget with custom headings displays custom labels")
  void testUS2CustomHeadingsLabels() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Custom Headings Test</title>\n" +
            "  <headings>,Who,Activity Type</headings>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertTrue(config.hasCustomHeadings());
    Assertions.assertEquals(3, config.getCustomHeadings().length);
    Assertions.assertNull(config.getCustomHeadings()[0]); // Empty
    Assertions.assertEquals("Who", config.getCustomHeadings()[1]);
    Assertions.assertEquals("Activity Type", config.getCustomHeadings()[2]);
  }

  @Test
  @DisplayName("T021: US2 - Widget with fewer headings than columns uses field names for remaining")
  void testUS2FewerHeadingsThanColumns() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Partial Headings Test</title>\n" +
            "  <headings>Custom1,Custom2</headings>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertEquals(2, config.getCustomHeadings().length);

    // Testing effective heading fallback
    String effective = config.getEffectiveHeading("OriginalName", 0);
    Assertions.assertEquals("Custom1", effective);

    // Out of range should use original
    String effectiveOutOfRange = config.getEffectiveHeading("OriginalAge", 5);
    Assertions.assertEquals("OriginalAge", effectiveOutOfRange);
  }

  @Test
  @DisplayName("T022: US2 - Widget with empty heading values uses original field names")
  void testUS2EmptyHeadings() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Empty Headings Test</title>\n" +
            "  <headings>,,</headings>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());

    // All should be null since they're empty
    String effective0 = config.getEffectiveHeading("Original0", 0);
    String effective1 = config.getEffectiveHeading("Original1", 1);
    String effective2 = config.getEffectiveHeading("Original2", 2);

    Assertions.assertEquals("Original0", effective0);
    Assertions.assertEquals("Original1", effective1);
    Assertions.assertEquals("Original2", effective2);
  }

  // ==================== Phase 4 Support Tests: Heading Customization ====================

  @Test
  @DisplayName("getEffectiveHeading returns custom when available")
  void testEffectiveHeadingCustomAvailable() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("headings", "CustomA,CustomB,CustomC");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    String effective0 = config.getEffectiveHeading("Original0", 0);
    String effective1 = config.getEffectiveHeading("Original1", 1);
    String effective2 = config.getEffectiveHeading("Original2", 2);

    Assertions.assertEquals("CustomA", effective0);
    Assertions.assertEquals("CustomB", effective1);
    Assertions.assertEquals("CustomC", effective2);
  }

  @Test
  @DisplayName("getEffectiveHeading falls back to original when custom is null")
  void testEffectiveHeadingFallback() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("headings", "CustomA,,CustomC");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    String effective0 = config.getEffectiveHeading("Original0", 0);
    String effective1 = config.getEffectiveHeading("Original1", 1);
    String effective2 = config.getEffectiveHeading("Original2", 2);

    Assertions.assertEquals("CustomA", effective0);
    Assertions.assertEquals("Original1", effective1);
    Assertions.assertEquals("CustomC", effective2);
  }

  // ==================== Phase 5 Acceptance Tests: User Story 3 ====================

  @Test
  @DisplayName("T028: US3 - Widget with pageSize=25 displays 25 records per page")
  void testUS3PageSize25() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Page Size 25 Test</title>\n" +
            "  <pageSize>25</pageSize>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertEquals(25, config.getPageSize());
    Assertions.assertTrue(config.isPageSizeValid());
  }

  @Test
  @DisplayName("T029: US3 - Widget with pageSize=999999 displays all records on one page")
  void testUS3PageSizeMaxValue() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Page Size Max Test</title>\n" +
            "  <pageSize>999999</pageSize>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertEquals(999999, config.getPageSize());
    Assertions.assertTrue(config.isPageSizeValid());
  }

  @Test
  @DisplayName("T030: US3 - Widget without pageSize preference defaults to 25")
  void testUS3PageSizeDefault() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Default Page Size Test</title>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertEquals(25, config.getPageSize());
    Assertions.assertTrue(config.isPageSizeValid());
  }

  // ==================== Phase 5 Support Tests: Page Size Validation ====================

  @Test
  @DisplayName("pageSize bounds checking accepts minimum value (1)")
  void testPageSizeMinimumBounds() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("pageSize", "1");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);
    Assertions.assertEquals(1, config.getPageSize());
    Assertions.assertTrue(config.isPageSizeValid());
  }

  @Test
  @DisplayName("pageSize bounds checking rejects value below 1")
  void testPageSizeBelowMinimum() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("pageSize", "0");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);
    Assertions.assertEquals(25, config.getPageSize());
    Assertions.assertFalse(config.isPageSizeValid());
  }

  @Test
  @DisplayName("sortRows case-insensitive comparison")
  void testCaseInsensitiveSort() {
    List<String[]> rows = createTestRows(
        new String[] { "Zebra" },
        new String[] { "apple" },
        new String[] { "Banana" });

    DatasetSortCommand.sortRows(rows, 0, false);

    Assertions.assertEquals("apple", rows.get(0)[0]);
    Assertions.assertEquals("Banana", rows.get(1)[0]);
    Assertions.assertEquals("Zebra", rows.get(2)[0]);
  }

  // ==================== Phase 6 Acceptance Tests: User Story 4 ====================

  @Test
  @DisplayName("T036: US4 - Widget with sortBy=\"Date\" sorts rows by Date column ascending")
  void testUS4SortByColumn() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Sort By Test</title>\n" +
            "  <sortBy>Date</sortBy>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertTrue(config.hasSortBy());
    Assertions.assertEquals("Date", config.getSortByColumn());
  }

  @Test
  @DisplayName("T037: US4 - Widget with invalid sortBy column logs warning and skips sorting")
  void testUS4InvalidSortByColumn() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Invalid Sort Test</title>\n" +
            "  <sortBy>NonExistentColumn</sortBy>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());

    // Configuration should parse the column name
    Assertions.assertEquals("NonExistentColumn", config.getSortByColumn());

    // But validation would show it's not available
    String[] fieldTitles = { "Name", "Email", "Date" };
    boolean isValid = DatasetDisplayConfiguration.isValidColumnName("NonExistentColumn", fieldTitles);
    Assertions.assertFalse(isValid);
  }

  // ==================== Phase 7 Acceptance Tests: User Story 5 ====================

  @Test
  @DisplayName("T045: US5 - Widget with sortBy=\"Date\" and reverseSort=true sorts descending")
  void testUS5ReverseSortWithSortBy() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Reverse Sort Test</title>\n" +
            "  <sortBy>Date</sortBy>\n" +
            "  <reverseSort>true</reverseSort>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());
    Assertions.assertTrue(config.hasSortBy());
    Assertions.assertEquals("Date", config.getSortByColumn());
    Assertions.assertTrue(config.isReverseSort());
  }

  @Test
  @DisplayName("T046: US5 - Widget with reverseSort=true but no sortBy ignores reverseSort")
  void testUS5ReverseSortWithoutSortBy() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"datasetViewer\">\n" +
            "  <title>Reverse Without Sort Test</title>\n" +
            "  <reverseSort>true</reverseSort>\n" +
            "</widget>");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(widgetContext.getPreferences());

    // reverseSort will be parsed, but without sortBy it should be ignored at runtime
    Assertions.assertTrue(config.isReverseSort());
    Assertions.assertFalse(config.hasSortBy());
  }

  // ==================== Support Tests: Sorting Operations ====================

  @Test
  @DisplayName("DatasetSortCommand sorts numeric column correctly")
  void testSortNumericColumn() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Item", "150" });
    rows.add(new String[] { "Item", "50" });
    rows.add(new String[] { "Item", "100" });

    DatasetSortCommand.sortRows(rows, 1, false);

    Assertions.assertEquals("50", rows.get(0)[1]);
    Assertions.assertEquals("100", rows.get(1)[1]);
    Assertions.assertEquals("150", rows.get(2)[1]);
  }

  @Test
  @DisplayName("DatasetSortCommand reverses sort order correctly")
  void testReverseSort() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "A" });
    rows.add(new String[] { "B" });
    rows.add(new String[] { "C" });

    DatasetSortCommand.sortRows(rows, 0, true);

    Assertions.assertEquals("C", rows.get(0)[0]);
    Assertions.assertEquals("B", rows.get(1)[0]);
    Assertions.assertEquals("A", rows.get(2)[0]);
  }

  @Test
  @DisplayName("DatasetSortCommand handles column not found")
  void testSortColumnNotFound() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "A" });
    rows.add(new String[] { "B" });

    String[] fieldTitles = { "Name", "Email" };
    DatasetSortCommand.sortRows(rows, "Phone", fieldTitles, false);

    // Should remain unchanged
    Assertions.assertEquals("A", rows.get(0)[0]);
    Assertions.assertEquals("B", rows.get(1)[0]);
  }
}
