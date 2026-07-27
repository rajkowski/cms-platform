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
 * Dataset Filter Command Tests
 * Validates CQL filtering logic for dataset rows
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class DatasetFilterCommandTest {

  @Test
  @DisplayName("filterRows with single criterion")
  void testSingleCriterion() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });
    rows.add(new String[] { "Charlie", "active" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status=active", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Charlie", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with multiple AND criteria")
  void testMultipleCriteria() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active", "high" });
    rows.add(new String[] { "Bob", "active", "low" });
    rows.add(new String[] { "Charlie", "inactive", "high" });

    String[] fieldTitles = { "Name", "Status", "Priority" };
    DatasetFilterCommand.filterRows(rows, "Status=active,Priority=high", fieldTitles);

    Assertions.assertEquals(1, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("filterRows case-insensitive matching")
  void testCaseInsensitiveMatching() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "Active" });
    rows.add(new String[] { "Bob", "ACTIVE" });
    rows.add(new String[] { "Charlie", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "status=active", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Bob", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with no matches removes all rows")
  void testNoMatches() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status=pending", fieldTitles);

    Assertions.assertEquals(0, rows.size());
  }

  @Test
  @DisplayName("filterRows with null filter does nothing")
  void testNullFilter() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, null, fieldTitles);

    Assertions.assertEquals(2, rows.size());
  }

  @Test
  @DisplayName("filterRows with empty filter does nothing")
  void testEmptyFilter() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "", fieldTitles);

    Assertions.assertEquals(2, rows.size());
  }

  @Test
  @DisplayName("filterRows with null rows does nothing")
  void testNullRows() {
    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(null, "Status=active", fieldTitles);
    // No exception thrown
    Assertions.assertDoesNotThrow(() -> DatasetFilterCommand.filterRows(null, "Status=active", fieldTitles));
  }

  @Test
  @DisplayName("filterRows with empty rows does nothing")
  void testEmptyRows() {
    List<String[]> rows = new ArrayList<>();
    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status=active", fieldTitles);

    Assertions.assertEquals(0, rows.size());
  }

  @Test
  @DisplayName("filterRows with non-existent column removes all rows")
  void testNonExistentColumn() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Department=IT", fieldTitles);

    Assertions.assertEquals(0, rows.size()); // No matches because column doesn't exist
  }

  @Test
  @DisplayName("filterRows with invalid CQL syntax (no equals)")
  void testInvalidCqlNoEquals() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "active", fieldTitles);

    Assertions.assertEquals(2, rows.size()); // No valid criteria, no filtering
  }

  @Test
  @DisplayName("filterRows with invalid CQL syntax (empty key)")
  void testInvalidCqlEmptyKey() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "=active", fieldTitles);

    Assertions.assertEquals(2, rows.size()); // No valid criteria, no filtering
  }

  @Test
  @DisplayName("filterRows with invalid CQL syntax (empty value)")
  void testInvalidCqlEmptyValue() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status=", fieldTitles);

    Assertions.assertEquals(2, rows.size()); // No valid criteria, no filtering
  }

  @Test
  @DisplayName("filterRows with mixed valid and invalid criteria")
  void testMixedValidInvalidCriteria() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active", "high" });
    rows.add(new String[] { "Bob", "active", "low" });
    rows.add(new String[] { "Charlie", "inactive", "high" });

    String[] fieldTitles = { "Name", "Status", "Priority" };
    DatasetFilterCommand.filterRows(rows, "Status=active,invalid,Priority=high", fieldTitles);

    // Only valid criteria should be applied
    Assertions.assertEquals(1, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("filterRows with whitespace in CQL")
  void testWhitespaceInCql() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, " Status = active ", fieldTitles);

    Assertions.assertEquals(1, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("filterRows with trailing comma")
  void testTrailingComma() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status=active,", fieldTitles);

    Assertions.assertEquals(1, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("filterRows with null cell values")
  void testNullCellValues() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", null });
    rows.add(new String[] { "Bob", "inactive" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status=active", fieldTitles);

    Assertions.assertEquals(0, rows.size()); // null doesn't match "active"
  }

  @Test
  @DisplayName("filterRows preserves row order")
  void testPreservesRowOrder() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "inactive" });
    rows.add(new String[] { "Charlie", "active" });
    rows.add(new String[] { "David", "active" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status=active", fieldTitles);

    Assertions.assertEquals(3, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Charlie", rows.get(1)[0]);
    Assertions.assertEquals("David", rows.get(2)[0]);
  }

  @Test
  @DisplayName("filterRows with all rows matching")
  void testAllRowsMatch() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "active" });
    rows.add(new String[] { "Bob", "active" });
    rows.add(new String[] { "Charlie", "active" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status=active", fieldTitles);

    Assertions.assertEquals(3, rows.size());
  }

  @Test
  @DisplayName("filterRows with numeric values")
  void testNumericValues() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "25" });
    rows.add(new String[] { "Bob", "30" });
    rows.add(new String[] { "Charlie", "25" });

    String[] fieldTitles = { "Name", "Age" };
    DatasetFilterCommand.filterRows(rows, "Age=25", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Charlie", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows performance test with large dataset")
  void testPerformanceLargeDataset() {
    List<String[]> rows = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      rows.add(new String[] { "User" + i, i % 2 == 0 ? "active" : "inactive" });
    }

    String[] fieldTitles = { "Name", "Status" };
    long startTime = System.currentTimeMillis();
    DatasetFilterCommand.filterRows(rows, "Status=active", fieldTitles);
    long endTime = System.currentTimeMillis();

    Assertions.assertEquals(500, rows.size());
    Assertions.assertTrue(endTime - startTime < 1000, "Filtering should complete in less than 1 second");
  }

  @Test
  @DisplayName("filterRows with IN operator - single value")
  void testInOperatorSingleValue() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "draft" });
    rows.add(new String[] { "Bob", "review" });
    rows.add(new String[] { "Charlie", "published" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status in (draft)", fieldTitles);

    Assertions.assertEquals(1, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
  }

  @Test
  @DisplayName("filterRows with IN operator - multiple values")
  void testInOperatorMultipleValues() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "draft" });
    rows.add(new String[] { "Bob", "review" });
    rows.add(new String[] { "Charlie", "published" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status in (draft, review)", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Bob", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with IN operator - case insensitive")
  void testInOperatorCaseInsensitive() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "Draft" });
    rows.add(new String[] { "Bob", "REVIEW" });
    rows.add(new String[] { "Charlie", "published" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status in (draft, review)", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Bob", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with NOT IN operator")
  void testNotInOperator() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "draft" });
    rows.add(new String[] { "Bob", "review" });
    rows.add(new String[] { "Charlie", "published" });

    String[] fieldTitles = { "Name", "Status" };
    DatasetFilterCommand.filterRows(rows, "Status not in (draft, review)", fieldTitles);

    Assertions.assertEquals(1, rows.size());
    Assertions.assertEquals("Charlie", rows.get(0)[0]);
  }

  @Test
  @DisplayName("filterRows with split function - single value match")
  void testSplitFunctionSingleValue() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Alice", "draft;review" });
    rows.add(new String[] { "Bob", "draft" });
    rows.add(new String[] { "Charlie", "published" });

    String[] fieldTitles = { "Name", "Labels" };
    DatasetFilterCommand.filterRows(rows, "Labels:split(;) in (draft)", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Alice", rows.get(0)[0]);
    Assertions.assertEquals("Bob", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with split function - example case from spec")
  void testSplitFunctionSpecExample() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Item1", "draft;final" });
    rows.add(new String[] { "Item2", "draft" });
    rows.add(new String[] { "Item3", "review;final" });
    rows.add(new String[] { "Item4", "published" });

    String[] fieldTitles = { "Name", "Labels" };
    DatasetFilterCommand.filterRows(rows, "Labels:split(;) in (draft)", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Item1", rows.get(0)[0]);
    Assertions.assertEquals("Item2", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with split function - multiple split values")
  void testSplitFunctionMultipleValues() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Item1", "draft;final" });
    rows.add(new String[] { "Item2", "review" });
    rows.add(new String[] { "Item3", "draft" });
    rows.add(new String[] { "Item4", "published" });

    String[] fieldTitles = { "Name", "Labels" };
    DatasetFilterCommand.filterRows(rows, "Labels:split(;) in (draft, review)", fieldTitles);

    Assertions.assertEquals(3, rows.size());
    Assertions.assertEquals("Item1", rows.get(0)[0]);
    Assertions.assertEquals("Item2", rows.get(1)[0]);
    Assertions.assertEquals("Item3", rows.get(2)[0]);
  }

  @Test
  @DisplayName("filterRows with split function - not in operator")
  void testSplitFunctionNotIn() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Item1", "draft;final" });
    rows.add(new String[] { "Item2", "review" });
    rows.add(new String[] { "Item3", "published" });

    String[] fieldTitles = { "Name", "Labels" };
    DatasetFilterCommand.filterRows(rows, "Labels:split(;) not in (draft)", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Item2", rows.get(0)[0]);
    Assertions.assertEquals("Item3", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with split function - no matches")
  void testSplitFunctionNoMatches() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Item1", "review;final" });
    rows.add(new String[] { "Item2", "published" });

    String[] fieldTitles = { "Name", "Labels" };
    DatasetFilterCommand.filterRows(rows, "Labels:split(;) in (draft)", fieldTitles);

    Assertions.assertEquals(0, rows.size());
  }

  @Test
  @DisplayName("filterRows with split function - case insensitive match")
  void testSplitFunctionCaseInsensitive() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Item1", "Draft;Final" });
    rows.add(new String[] { "Item2", "DRAFT" });
    rows.add(new String[] { "Item3", "published" });

    String[] fieldTitles = { "Name", "Labels" };
    DatasetFilterCommand.filterRows(rows, "Labels:split(;) in (draft)", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Item1", rows.get(0)[0]);
    Assertions.assertEquals("Item2", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with split function - whitespace handling")
  void testSplitFunctionWhitespace() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Item1", "draft ; final" });
    rows.add(new String[] { "Item2", " draft" });
    rows.add(new String[] { "Item3", "published" });

    String[] fieldTitles = { "Name", "Labels" };
    DatasetFilterCommand.filterRows(rows, "Labels:split(;) in (draft)", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Item1", rows.get(0)[0]);
    Assertions.assertEquals("Item2", rows.get(1)[0]);
  }

  @Test
  @DisplayName("filterRows with split function and IN operator - combined")
  void testSplitWithInOperatorCombined() {
    List<String[]> rows = new ArrayList<>();
    rows.add(new String[] { "Item1", "active;draft" });
    rows.add(new String[] { "Item2", "inactive" });
    rows.add(new String[] { "Item3", "active;review" });

    String[] fieldTitles = { "Name", "Tags" };
    DatasetFilterCommand.filterRows(rows, "Tags:split(;) in (draft, active)", fieldTitles);

    Assertions.assertEquals(2, rows.size());
    Assertions.assertEquals("Item1", rows.get(0)[0]);
    Assertions.assertEquals("Item3", rows.get(1)[0]);
  }
}
