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
package com.zeroio.platform.application.datasets;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DatasetSortCommand
 * Validates sorting by column index and column name with various data types
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
@DisplayName("DatasetSortCommand Tests")
class DatasetSortCommandTest {

  // ==================== Helper Methods ====================

  /**
   * Create test dataset rows
   */
  private List<String[]> createTestRows(String[]... rows) {
    List<String[]> rowList = new ArrayList<>();
    for (String[] row : rows) {
      rowList.add(row);
    }
    return rowList;
  }

  /**
   * Create sample dataset with name, email, and date columns
   */
  private List<String[]> createSampleDataset() {
    return createTestRows(
        new String[] { "Alice", "alice@example.com", "2024-01-15" },
        new String[] { "Bob", "bob@example.com", "2024-03-20" },
        new String[] { "Charlie", "charlie@example.com", "2024-02-10" });
  }

  /**
   * Create dataset with numeric values in second column
   */
  private List<String[]> createNumericDataset() {
    return createTestRows(
        new String[] { "Item A", "150" },
        new String[] { "Item B", "50" },
        new String[] { "Item C", "100" });
  }

  // ==================== Sort by Index Tests ====================

  @Test
  @DisplayName("T039: sortRows by column 0 (names ascending)")
  void testSortByColumnIndexAscending() {
    List<String[]> rows = createSampleDataset();

    DatasetSortCommand.sortRows(rows, 0, false);

    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Bob", rows.get(1)[0]);
    Assertions.assertEquals("Charlie", rows.get(2)[0]);
  }

  @Test
  @DisplayName("T050: sortRows by column 0 (names descending)")
  void testSortByColumnIndexDescending() {
    List<String[]> rows = createSampleDataset();

    DatasetSortCommand.sortRows(rows, 0, true);

    Assertions.assertEquals("Charlie", rows.get(0)[0]);
    Assertions.assertEquals("Bob", rows.get(1)[0]);
    Assertions.assertEquals("Alice", rows.get(2)[0]);
  }

  @Test
  @DisplayName("T039: sortRows handles numeric values")
  void testSortNumericValues() {
    List<String[]> rows = createNumericDataset();

    DatasetSortCommand.sortRows(rows, 1, false);

    Assertions.assertEquals("50", rows.get(0)[1]);
    Assertions.assertEquals("100", rows.get(1)[1]);
    Assertions.assertEquals("150", rows.get(2)[1]);
  }

  @Test
  @DisplayName("T050: sortRows handles numeric values reversed")
  void testSortNumericValuesReversed() {
    List<String[]> rows = createNumericDataset();

    DatasetSortCommand.sortRows(rows, 1, true);

    Assertions.assertEquals("150", rows.get(0)[1]);
    Assertions.assertEquals("100", rows.get(1)[1]);
    Assertions.assertEquals("50", rows.get(2)[1]);
  }

  @Test
  @DisplayName("sortRows handles null list gracefully")
  void testSortNullList() {
    List<String[]> result = DatasetSortCommand.sortRows(null, 0, false);

    Assertions.assertNull(result);
  }

  @Test
  @DisplayName("sortRows handles empty list")
  void testSortEmptyList() {
    List<String[]> rows = new ArrayList<>();

    DatasetSortCommand.sortRows(rows, 0, false);

    Assertions.assertTrue(rows.isEmpty());
  }

  @Test
  @DisplayName("sortRows handles invalid column index")
  void testSortInvalidColumnIndex() {
    List<String[]> rows = createSampleDataset();

    DatasetSortCommand.sortRows(rows, -1, false);

    // Should remain unchanged
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("sortRows handles column index out of bounds")
  void testSortColumnIndexOutOfBounds() {
    List<String[]> rows = createSampleDataset();

    DatasetSortCommand.sortRows(rows, 99, false);

    // Should remain unchanged
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  // ==================== Sort by Column Name Tests ====================

  @Test
  @DisplayName("T040: sortRows by column name (ascending)")
  void testSortByColumnNameAscending() {
    List<String[]> rows = createSampleDataset();
    String[] fieldTitles = { "Name", "Email", "Date" };

    DatasetSortCommand.sortRows(rows, "Name", fieldTitles, false);

    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Bob", rows.get(1)[0]);
    Assertions.assertEquals("Charlie", rows.get(2)[0]);
  }

  @Test
  @DisplayName("T040: sortRows by column name (case-insensitive)")
  void testSortByColumnNameCaseInsensitive() {
    List<String[]> rows = createSampleDataset();
    String[] fieldTitles = { "Name", "Email", "Date" };

    DatasetSortCommand.sortRows(rows, "name", fieldTitles, false);

    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("sortRows by non-existent column name logs warning")
  void testSortByNonExistentColumnName() {
    List<String[]> rows = createSampleDataset();
    String[] fieldTitles = { "Name", "Email", "Date" };

    DatasetSortCommand.sortRows(rows, "NonExistent", fieldTitles, false);

    // Should remain unchanged
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("sortRows handles blank column name")
  void testSortByBlankColumnName() {
    List<String[]> rows = createSampleDataset();
    String[] fieldTitles = { "Name", "Email", "Date" };

    DatasetSortCommand.sortRows(rows, "", fieldTitles, false);

    // Should remain unchanged
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("sortRows handles null fieldTitles")
  void testSortWithNullFieldTitles() {
    List<String[]> rows = createSampleDataset();

    DatasetSortCommand.sortRows(rows, "Name", null, false);

    // Should remain unchanged
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  // ==================== Mixed Content Tests ====================

  @Test
  @DisplayName("sortRows handles mixed numeric and non-numeric values")
  void testSortMixedNumericValues() {
    List<String[]> rows = createTestRows(
        new String[] { "Data", "100" },
        new String[] { "Data", "apple" },
        new String[] { "Data", "50" });

    DatasetSortCommand.sortRows(rows, 1, false);

    // Should fall back to string comparison when mixed
    Assertions.assertNotNull(rows.get(0)[1]);
  }

  @Test
  @DisplayName("sortRows handles dates as strings")
  void testSortDatesAsStrings() {
    List<String[]> rows = createTestRows(
        new String[] { "Event", "2024-03-20" },
        new String[] { "Event", "2024-01-15" },
        new String[] { "Event", "2024-02-10" });

    DatasetSortCommand.sortRows(rows, 1, false);

    Assertions.assertEquals("2024-01-15", rows.get(0)[1]);
    Assertions.assertEquals("2024-02-10", rows.get(1)[1]);
    Assertions.assertEquals("2024-03-20", rows.get(2)[1]);
  }

  @Test
  @DisplayName("sortRows handles empty string values")
  void testSortWithEmptyValues() {
    List<String[]> rows = createTestRows(
        new String[] { "Name", "email@test.com" },
        new String[] { "Name", "" },
        new String[] { "Name", "another@test.com" });

    DatasetSortCommand.sortRows(rows, 1, false);

    Assertions.assertEquals("", rows.get(0)[1]);
  }

  @Test
  @DisplayName("sortRows handles rows with missing cells")
  void testSortRowsWithMissingCells() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "A", "Z" });
    rows.add(new String[] { "B" }); // Missing second cell
    rows.add(new String[] { "C", "A" });

    // Should handle gracefully
    DatasetSortCommand.sortRows(rows, 1, false);

    Assertions.assertNotNull(rows);
  }

  // ==================== Numeric Column Detection Tests ====================

  @Test
  @DisplayName("isNumericColumn returns true for all numeric values")
  void testIsNumericColumnAllNumeric() {
    List<String[]> rows = createTestRows(
        new String[] { "100" },
        new String[] { "200" },
        new String[] { "300" });

    boolean isNumeric = DatasetSortCommand.isNumericColumn(rows, 0);

    Assertions.assertTrue(isNumeric);
  }

  @Test
  @DisplayName("isNumericColumn returns false for mixed values")
  void testIsNumericColumnMixed() {
    List<String[]> rows = createTestRows(
        new String[] { "100" },
        new String[] { "apple" },
        new String[] { "300" });

    boolean isNumeric = DatasetSortCommand.isNumericColumn(rows, 0);

    Assertions.assertFalse(isNumeric);
  }

  @Test
  @DisplayName("isNumericColumn handles empty values")
  void testIsNumericColumnWithEmpty() {
    List<String[]> rows = createTestRows(
        new String[] { "100" },
        new String[] { "" },
        new String[] { "300" });

    // Empty values are ignored in check
    boolean isNumeric = DatasetSortCommand.isNumericColumn(rows, 0);

    Assertions.assertTrue(isNumeric);
  }

  @Test
  @DisplayName("isNumericColumn handles empty list")
  void testIsNumericColumnEmptyList() {
    List<String[]> rows = new ArrayList<>();

    boolean isNumeric = DatasetSortCommand.isNumericColumn(rows, 0);

    Assertions.assertFalse(isNumeric);
  }

  @Test
  @DisplayName("isNumericColumn handles invalid column index")
  void testIsNumericColumnInvalidIndex() {
    List<String[]> rows = createTestRows(
        new String[] { "100" },
        new String[] { "200" });

    boolean isNumeric = DatasetSortCommand.isNumericColumn(rows, -1);

    Assertions.assertFalse(isNumeric);
  }

  // ==================== Performance and Large Dataset Tests ====================

  @Test
  @DisplayName("sortRows handles large dataset efficiently")
  void testSortLargeDataset() {
    List<String[]> rows = new ArrayList<>();
    for (int i = 100; i > 0; i--) {
      rows.add(new String[] { "Item " + i, String.valueOf(i) });
    }

    long startTime = System.currentTimeMillis();
    DatasetSortCommand.sortRows(rows, 1, false);
    long elapsedTime = System.currentTimeMillis() - startTime;

    // Verify sorting worked
    Assertions.assertEquals("1", rows.get(0)[1]);

    // Performance check (should complete in reasonable time for 100 items)
    Assertions.assertTrue(elapsedTime < 1000, "Sort took too long: " + elapsedTime + "ms");
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
}
