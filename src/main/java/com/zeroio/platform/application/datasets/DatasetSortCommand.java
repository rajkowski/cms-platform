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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Sorts dataset rows by a specified column in ascending or descending order
 * 
 * User Story 4: Default Sort Column (sortBy preference)
 * User Story 5: Reverse Sort Direction (reverseSort preference)
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class DatasetSortCommand {

  private static final Log LOG = LogFactory.getLog(DatasetSortCommand.class);

  /**
   * Sort rows by specified column
   * 
   * @param rows list of rows to sort (each row is String array)
   * @param columnIndex the column index to sort by
   * @param reverse whether to reverse sort (descending)
   * @return the sorted list
   */
  public static List<String[]> sortRows(List<String[]> rows, int columnIndex, boolean reverse) {
    if (rows == null || rows.isEmpty()) {
      return rows;
    }

    if (columnIndex < 0) {
      LOG.warn("Cannot sort: invalid column index " + columnIndex);
      return rows;
    }

    // Verify column index is within bounds
    if (!rows.isEmpty()) {
      String[] firstRow = rows.get(0);
      if (columnIndex >= firstRow.length) {
        LOG.warn("Cannot sort: column index " + columnIndex + " exceeds row width " + firstRow.length);
        return rows;
      }
    }

    try {
      // Create comparator for sorting by column
      Comparator<String[]> comparator = new Comparator<String[]>() {
        @Override
        public int compare(String[] row1, String[] row2) {
          String val1 = (row1.length > columnIndex) ? row1[columnIndex] : "";
          String val2 = (row2.length > columnIndex) ? row2[columnIndex] : "";

          // Try numeric comparison first
          try {
            double num1 = Double.parseDouble(val1);
            double num2 = Double.parseDouble(val2);
            return Double.compare(num1, num2);
          } catch (NumberFormatException e) {
            // Fall back to string comparison
            return val1.compareToIgnoreCase(val2);
          }
        }
      };

      // Sort the list
      Collections.sort(rows, comparator);

      // Reverse if requested
      if (reverse) {
        Collections.reverse(rows);
        LOG.debug("Sorted rows by column " + columnIndex + " in reverse order");
      } else {
        LOG.debug("Sorted rows by column " + columnIndex + " in ascending order");
      }

      return rows;

    } catch (Exception e) {
      LOG.warn("Error sorting dataset by column " + columnIndex + ": " + e.getMessage());
      return rows;
    }
  }

  /**
   * Sort rows by column name
   * 
   * @param rows list of rows to sort
   * @param columnName the column name to sort by
   * @param fieldTitles array of field titles to find column index
   * @param reverse whether to reverse sort
   * @return the sorted list
   */
  public static List<String[]> sortRows(List<String[]> rows, String columnName, String[] fieldTitles,
      boolean reverse) {
    if (StringUtils.isBlank(columnName) || fieldTitles == null) {
      return rows;
    }

    // Find column index by name
    int columnIndex = -1;
    for (int i = 0; i < fieldTitles.length; i++) {
      if (fieldTitles[i].equalsIgnoreCase(columnName.trim())) {
        columnIndex = i;
        break;
      }
    }

    if (columnIndex < 0) {
      LOG.warn("Column not found for sorting: " + columnName);
      return rows;
    }

    return sortRows(rows, columnIndex, reverse);
  }

  /**
   * Check if all values in a column are numeric
   * 
   * @param rows list of rows
   * @param columnIndex the column index to check
   * @return true if all non-empty values are numeric
   */
  public static boolean isNumericColumn(List<String[]> rows, int columnIndex) {
    if (rows == null || rows.isEmpty() || columnIndex < 0) {
      return false;
    }

    for (String[] row : rows) {
      if (row.length > columnIndex && StringUtils.isNotBlank(row[columnIndex])) {
        try {
          Double.parseDouble(row[columnIndex]);
        } catch (NumberFormatException e) {
          return false;
        }
      }
    }

    return true;
  }
}
