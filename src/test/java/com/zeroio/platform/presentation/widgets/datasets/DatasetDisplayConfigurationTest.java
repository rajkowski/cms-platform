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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DatasetDisplayConfiguration
 * Validates preference parsing, validation, and edge cases
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
@DisplayName("DatasetDisplayConfiguration Tests")
class DatasetDisplayConfigurationTest {

  // ==================== Factory Method Tests ====================

  @Test
  @DisplayName("T007: fromPreferences() creates config from map")
  void testFromPreferences() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("firstcolumn", "Activity");
    prefs.put("pageSize", "50");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertNotNull(config);
    Assertions.assertEquals("Activity", config.getFirstColumn());
    Assertions.assertEquals(50, config.getPageSize());
  }

  @Test
  @DisplayName("fromPreferences() handles null map")
  void testFromPreferencesNullMap() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(null);

    Assertions.assertNotNull(config);
    Assertions.assertNull(config.getFirstColumn());
    Assertions.assertEquals(25, config.getPageSize()); // Default
  }

  @Test
  @DisplayName("fromPreferences() handles empty map")
  void testFromPreferencesEmptyMap() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(new HashMap<>());

    Assertions.assertNotNull(config);
    Assertions.assertNull(config.getFirstColumn());
    Assertions.assertEquals(25, config.getPageSize()); // Default
  }

  // ==================== User Story 1: firstcolumn Tests ====================

  @Test
  @DisplayName("T008: Parses valid firstcolumn preference")
  void testFirstColumnValid() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("firstcolumn", "Activity");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.hasFirstColumn());
    Assertions.assertEquals("Activity", config.getFirstColumn());
  }

  @Test
  @DisplayName("T008: Parses firstcolumn with whitespace")
  void testFirstColumnWithWhitespace() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("firstcolumn", "  Activity  ");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.hasFirstColumn());
    Assertions.assertEquals("Activity", config.getFirstColumn());
  }

  @Test
  @DisplayName("firstcolumn returns null when not set")
  void testFirstColumnNull() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(new HashMap<>());

    Assertions.assertFalse(config.hasFirstColumn());
    Assertions.assertNull(config.getFirstColumn());
  }

  @Test
  @DisplayName("firstcolumn ignores empty string")
  void testFirstColumnEmpty() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("firstcolumn", "");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertFalse(config.hasFirstColumn());
    Assertions.assertNull(config.getFirstColumn());
  }

  // ==================== User Story 2: headings Tests ====================

  @Test
  @DisplayName("T008: Parses custom headings preference")
  void testHeadingsValid() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("headings", "Col1,Col2,Col3");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.hasCustomHeadings());
    Assertions.assertEquals(3, config.getCustomHeadings().length);
    Assertions.assertEquals("Col1", config.getCustomHeadings()[0]);
    Assertions.assertEquals("Col2", config.getCustomHeadings()[1]);
    Assertions.assertEquals("Col3", config.getCustomHeadings()[2]);
  }

  @Test
  @DisplayName("T008: Parses headings with empty values")
  void testHeadingsWithEmpty() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("headings", ",Custom,");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.hasCustomHeadings());
    Assertions.assertEquals(3, config.getCustomHeadings().length);
    Assertions.assertNull(config.getCustomHeadings()[0]); // Empty stays null
    Assertions.assertEquals("Custom", config.getCustomHeadings()[1]);
    Assertions.assertNull(config.getCustomHeadings()[2]); // Empty stays null
  }

  @Test
  @DisplayName("T008: getEffectiveHeading uses custom when available")
  void testEffectiveHeadingCustom() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("headings", "CustomA,CustomB");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    String effective = config.getEffectiveHeading("Original", 0);
    Assertions.assertEquals("CustomA", effective);
  }

  @Test
  @DisplayName("T008: getEffectiveHeading uses original when custom is null")
  void testEffectiveHeadingOriginal() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("headings", ",CustomB");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    String effective = config.getEffectiveHeading("Original", 0);
    Assertions.assertEquals("Original", effective);
  }

  @Test
  @DisplayName("T008: getEffectiveHeading uses original when index out of range")
  void testEffectiveHeadingOutOfRange() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("headings", "CustomA");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    String effective = config.getEffectiveHeading("Original", 5);
    Assertions.assertEquals("Original", effective);
  }

  @Test
  @DisplayName("headings returns null when not set")
  void testHeadingsNull() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(new HashMap<>());

    Assertions.assertFalse(config.hasCustomHeadings());
    Assertions.assertNull(config.getCustomHeadings());
  }

  // ==================== User Story 3: pageSize Tests ====================

  @Test
  @DisplayName("T009: Parses valid pageSize preference")
  void testPageSizeValid() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("pageSize", "50");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.isPageSizeValid());
    Assertions.assertEquals(50, config.getPageSize());
  }

  @Test
  @DisplayName("T009: Handles large pageSize values")
  void testPageSizeLarge() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("pageSize", "999999");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.isPageSizeValid());
    Assertions.assertEquals(999999, config.getPageSize());
  }

  @Test
  @DisplayName("T009: Handles pageSize = 1")
  void testPageSizeOne() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("pageSize", "1");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.isPageSizeValid());
    Assertions.assertEquals(1, config.getPageSize());
  }

  @Test
  @DisplayName("T009: Rejects pageSize < 1")
  void testPageSizeNegative() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("pageSize", "0");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertFalse(config.isPageSizeValid());
    Assertions.assertEquals(25, config.getPageSize()); // Falls back to default
  }

  @Test
  @DisplayName("T009: Rejects invalid pageSize format")
  void testPageSizeInvalidFormat() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("pageSize", "not-a-number");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertFalse(config.isPageSizeValid());
    Assertions.assertEquals(25, config.getPageSize()); // Falls back to default
  }

  @Test
  @DisplayName("pageSize defaults to 25 when not set")
  void testPageSizeDefault() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(new HashMap<>());

    Assertions.assertTrue(config.isPageSizeValid());
    Assertions.assertEquals(25, config.getPageSize());
  }

  @Test
  @DisplayName("T009: Handles pageSize with whitespace")
  void testPageSizeWhitespace() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("pageSize", "  50  ");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.isPageSizeValid());
    Assertions.assertEquals(50, config.getPageSize());
  }

  // ==================== User Story 4: sortBy Tests ====================

  @Test
  @DisplayName("Parses valid sortBy preference")
  void testSortByValid() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("sortBy", "Date");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.hasSortBy());
    Assertions.assertEquals("Date", config.getSortByColumn());
  }

  @Test
  @DisplayName("sortBy returns null when not set")
  void testSortByNull() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(new HashMap<>());

    Assertions.assertFalse(config.hasSortBy());
    Assertions.assertNull(config.getSortByColumn());
  }

  @Test
  @DisplayName("sortBy ignores empty string")
  void testSortByEmpty() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("sortBy", "");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertFalse(config.hasSortBy());
  }

  // ==================== User Story 5: reverseSort Tests ====================

  @Test
  @DisplayName("Parses reverseSort=true")
  void testReverseSortTrue() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("reverseSort", "true");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.isReverseSort());
  }

  @Test
  @DisplayName("Parses reverseSort=false")
  void testReverseSortFalse() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("reverseSort", "false");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertFalse(config.isReverseSort());
  }

  @Test
  @DisplayName("Handles reverseSort case-insensitive (TRUE)")
  void testReverseSortCaseInsensitive() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("reverseSort", "TRUE");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.isReverseSort());
  }

  @Test
  @DisplayName("reverseSort defaults to false when not set")
  void testReverseSortDefault() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(new HashMap<>());

    Assertions.assertFalse(config.isReverseSort());
  }

  // ==================== User Story 6: cql Tests ====================

  @Test
  @DisplayName("T008: Parses valid CQL filter")
  void testCqlValid() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("cql", "status=active");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.hasCqlFilter());
    Assertions.assertTrue(config.isCqlValid());
    Assertions.assertEquals("status=active", config.getCqlFilter());
    Assertions.assertEquals(1, config.getFilterCriteria().size());
    Assertions.assertEquals("status=active", config.getFilterCriteria().get(0));
  }

  @Test
  @DisplayName("T008: Parses multiple CQL filter criteria")
  void testCqlMultiple() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("cql", "status=active,priority=high");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.hasCqlFilter());
    Assertions.assertTrue(config.isCqlValid());
    Assertions.assertEquals(2, config.getFilterCriteria().size());
    Assertions.assertEquals("status=active", config.getFilterCriteria().get(0));
    Assertions.assertEquals("priority=high", config.getFilterCriteria().get(1));
  }

  @Test
  @DisplayName("T008: Rejects invalid CQL syntax")
  void testCqlInvalid() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("cql", "invalid-syntax");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertFalse(config.isCqlValid());
    Assertions.assertEquals("invalid-syntax", config.getCqlFilter()); // Stores original value
  }

  @Test
  @DisplayName("cql returns null when not set")
  void testCqlNull() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(new HashMap<>());

    Assertions.assertFalse(config.hasCqlFilter());
    Assertions.assertNull(config.getCqlFilter());
    Assertions.assertTrue(config.isCqlValid());
  }

  @Test
  @DisplayName("cql ignores empty string")
  void testCqlEmpty() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("cql", "");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertFalse(config.hasCqlFilter());
  }

  // ==================== User Story 7: id Tests ====================

  @Test
  @DisplayName("Parses valid widget ID preference")
  void testIdValid() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("id", "my-widget-id");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertTrue(config.hasWidgetId());
    Assertions.assertEquals("my-widget-id", config.getWidgetId());
  }

  @Test
  @DisplayName("id returns null when not set")
  void testIdNull() {
    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(new HashMap<>());

    Assertions.assertFalse(config.hasWidgetId());
    Assertions.assertNull(config.getWidgetId());
  }

  @Test
  @DisplayName("id ignores empty string")
  void testIdEmpty() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("id", "");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertFalse(config.hasWidgetId());
  }

  // ==================== Utility Method Tests ====================

  @Test
  @DisplayName("T010: isValidColumnName matches existing column")
  void testIsValidColumnNameValid() {
    String[] fieldTitles = { "Name", "Email", "Status" };

    Assertions.assertTrue(DatasetDisplayConfiguration.isValidColumnName("Email", fieldTitles));
  }

  @Test
  @DisplayName("T010: isValidColumnName case-insensitive")
  void testIsValidColumnNameCaseInsensitive() {
    String[] fieldTitles = { "Name", "Email", "Status" };

    Assertions.assertTrue(DatasetDisplayConfiguration.isValidColumnName("email", fieldTitles));
    Assertions.assertTrue(DatasetDisplayConfiguration.isValidColumnName("EMAIL", fieldTitles));
  }

  @Test
  @DisplayName("T010: isValidColumnName rejects non-existent column")
  void testIsValidColumnNameInvalid() {
    String[] fieldTitles = { "Name", "Email", "Status" };

    Assertions.assertFalse(DatasetDisplayConfiguration.isValidColumnName("Phone", fieldTitles));
  }

  @Test
  @DisplayName("T010: findColumnIndex returns correct index")
  void testFindColumnIndexValid() {
    String[] fieldTitles = { "Name", "Email", "Status" };

    Assertions.assertEquals(1, DatasetDisplayConfiguration.findColumnIndex("Email", fieldTitles));
  }

  @Test
  @DisplayName("T010: findColumnIndex case-insensitive")
  void testFindColumnIndexCaseInsensitive() {
    String[] fieldTitles = { "Name", "Email", "Status" };

    Assertions.assertEquals(1, DatasetDisplayConfiguration.findColumnIndex("email", fieldTitles));
  }

  @Test
  @DisplayName("T010: findColumnIndex returns -1 for non-existent column")
  void testFindColumnIndexNotFound() {
    String[] fieldTitles = { "Name", "Email", "Status" };

    Assertions.assertEquals(-1, DatasetDisplayConfiguration.findColumnIndex("Phone", fieldTitles));
  }

  // ==================== Combined Preference Tests ====================

  @Test
  @DisplayName("Parses all preferences combined")
  void testAllPreferencesCombined() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("firstcolumn", "Activity");
    prefs.put("headings", ",Custom2");
    prefs.put("pageSize", "50");
    prefs.put("sortBy", "Date");
    prefs.put("reverseSort", "true");
    prefs.put("cql", "status=active");
    prefs.put("id", "test-id");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    Assertions.assertEquals("Activity", config.getFirstColumn());
    Assertions.assertEquals(2, config.getCustomHeadings().length);
    Assertions.assertEquals(50, config.getPageSize());
    Assertions.assertEquals("Date", config.getSortByColumn());
    Assertions.assertTrue(config.isReverseSort());
    Assertions.assertEquals("status=active", config.getCqlFilter());
    Assertions.assertEquals("test-id", config.getWidgetId());
  }

  @Test
  @DisplayName("toString() provides useful debug info")
  void testToString() {
    Map<String, String> prefs = new HashMap<>();
    prefs.put("firstcolumn", "Activity");
    prefs.put("pageSize", "50");

    DatasetDisplayConfiguration config = DatasetDisplayConfiguration.fromPreferences(prefs);

    String str = config.toString();
    Assertions.assertNotNull(str);
    Assertions.assertTrue(str.contains("Activity"));
    Assertions.assertTrue(str.contains("50"));
  }
}
