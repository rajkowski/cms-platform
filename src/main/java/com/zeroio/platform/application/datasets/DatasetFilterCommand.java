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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Dataset Filter Command
 * Filters dataset rows based on CQL-like criteria
 * 
 * Supports operators:
 * - key=value (simple equality)
 * - key in (value1, value2) (match if value is in list)
 * - key not in (value1, value2) (match if value is NOT in list)
 * 
 * Supports functions on field values:
 * - key:split(;) in (value1, value2) (split field by separator, then check if any part matches)
 * - key:split(;) not in (value1, value2) (split field by separator, check if no parts match)
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class DatasetFilterCommand {

  private static Log LOG = LogFactory.getLog(DatasetFilterCommand.class);

  /**
   * Filter rows based on CQL criteria
   * Multiple criteria are comma-separated and treated as AND conditions
   *
   * @param rows the dataset rows to filter
   * @param cqlFilter the CQL filter string (e.g., "status=active" or "labels in (draft, review)")
   * @param fieldTitles the column titles/headers
   */
  public static void filterRows(List<String[]> rows, String cqlFilter, String[] fieldTitles) {
    if (rows == null || rows.isEmpty() || cqlFilter == null || cqlFilter.trim().isEmpty()) {
      return;
    }

    // Parse CQL criteria into FilterCondition objects
    List<FilterCondition> conditions = parseCqlConditions(cqlFilter);
    if (conditions == null || conditions.isEmpty()) {
      LOG.warn("No valid criteria found in CQL filter: " + cqlFilter);
      return;
    }

    // Filter rows that don't match all conditions
    List<String[]> rowsToRemove = new ArrayList<>();
    for (String[] row : rows) {
      if (!matchesAllConditions(row, conditions, fieldTitles)) {
        rowsToRemove.add(row);
      }
    }

    // Remove non-matching rows
    rows.removeAll(rowsToRemove);
    LOG.debug("CQL filter removed " + rowsToRemove.size() + " rows, " + rows.size() + " remaining");
  }

  /**
   * Parse CQL filter string into FilterCondition objects
   * Supports:
   * - key=value
   * - key in (value1, value2, ...)
   * - key not in (value1, value2, ...)
   * - key:split(;) in (value1, value2, ...)
   * - key:split(;) not in (value1, value2, ...)
   *
   * @param cqlFilter the CQL filter string
   * @return list of conditions or empty list if none found
   */
  private static List<FilterCondition> parseCqlConditions(String cqlFilter) {
    List<FilterCondition> conditions = new ArrayList<>();

    if (cqlFilter == null || cqlFilter.trim().isEmpty()) {
      return conditions;
    }

    // Split by comma, but be careful not to split on commas inside parentheses
    List<String> clauses = splitClauses(cqlFilter);

    for (String clause : clauses) {
      clause = clause.trim();
      if (clause.isEmpty()) {
        continue;
      }

      FilterCondition condition = parseCondition(clause);
      if (condition != null) {
        conditions.add(condition);
      }
    }

    return conditions;
  }

  /**
   * Split CQL filter by top-level commas (not inside parentheses)
   * 
   * @param cqlFilter the CQL filter string
   * @return list of clause strings
   */
  private static List<String> splitClauses(String cqlFilter) {
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
   * Parse a single condition clause
   * Examples:
   * - key=value
   * - key in (value1, value2)
   * - key not in (value1, value2)
   * - key:split(;) in (value1, value2)
   *
   * @param clause the clause string
   * @return FilterCondition or null if invalid
   */
  private static FilterCondition parseCondition(String clause) {
    clause = clause.trim();
    if (clause.isEmpty()) {
      return null;
    }

    // Strategy: Look for distinct operator patterns in order of specificity
    // Check for "not in" (most specific)
    int notInIndex = clause.indexOf(" not in ");
    if (notInIndex >= 0) {
      return parseWithOperatorAtIndex(clause, notInIndex, " not in ", FilterCondition.Operator.NOT_IN);
    }

    // Check for " in " (must be surrounded by spaces to avoid matching words like "container")
    // Actually, " in (" is more specific - find that pattern
    int inParenIndex = findOperatorWithParen(clause, " in (");
    if (inParenIndex >= 0) {
      return parseWithOperatorAtIndex(clause, inParenIndex, " in ", FilterCondition.Operator.IN);
    }

    // Check for "=" operator
    int equalsIndex = clause.indexOf('=');
    if (equalsIndex > 0 && equalsIndex < clause.length() - 1) {
      return parseEqualsOperator(clause);
    }

    LOG.warn("Invalid CQL syntax: no operator found in: " + clause);
    return null;
  }

  /**
   * Find the index of an operator pattern, handling cases where there might be a function before it
   * For example: "Labels:split(";") in (...)" or "Status in (...)"
   */
  private static int findOperatorWithParen(String clause, String operatorParen) {
    // operatorParen is something like " in ("
    int index = clause.indexOf(operatorParen);
    return index >= 0 ? index : -1;
  }

  /**
   * Parse a condition where we know the operator and its index
   */
  private static FilterCondition parseWithOperatorAtIndex(String clause, int operatorIndex,
      String operatorStr, FilterCondition.Operator operator) {
    // Extract field part (before operator)
    String fieldPart = clause.substring(0, operatorIndex).trim();

    // Extract value part (after operator)
    String valuePart = clause.substring(operatorIndex + operatorStr.length()).trim();

    // Parse field part (may contain function like split)
    String fieldName = null;
    String functionName = null;
    String functionArg = null;

    int colonIndex = fieldPart.indexOf(':');
    if (colonIndex > 0) {
      fieldName = fieldPart.substring(0, colonIndex).trim();
      int parenStart = fieldPart.indexOf('(', colonIndex);
      int parenEnd = fieldPart.indexOf(')', parenStart);

      if (parenStart > 0 && parenEnd > parenStart) {
        functionName = fieldPart.substring(colonIndex + 1, parenStart).trim();
        functionArg = fieldPart.substring(parenStart + 1, parenEnd).trim();
      }
    } else {
      fieldName = fieldPart;
    }

    if (fieldName == null || fieldName.isEmpty()) {
      LOG.warn("Invalid CQL syntax: empty field name in: " + clause);
      return null;
    }

    // Extract values from parentheses
    List<String> values = extractValues(valuePart);
    if (values.isEmpty()) {
      LOG.warn("Invalid CQL syntax: no values found in: " + clause);
      return null;
    }

    return new FilterCondition(fieldName, operator, values, functionName, functionArg);
  }

  /**
   * Parse condition with equals operator
   */
  private static FilterCondition parseEqualsOperator(String clause) {
    int equalsIndex = clause.indexOf('=');
    if (equalsIndex <= 0 || equalsIndex >= clause.length() - 1) {
      LOG.warn("Invalid CQL syntax: invalid equals operator position in: " + clause);
      return null;
    }

    String fieldName = clause.substring(0, equalsIndex).trim();
    String value = clause.substring(equalsIndex + 1).trim();

    if (fieldName.isEmpty()) {
      LOG.warn("Invalid CQL syntax: empty field name in: " + clause);
      return null;
    }

    if (value.isEmpty()) {
      LOG.warn("Invalid CQL syntax: empty value in: " + clause);
      return null;
    }

    List<String> values = new ArrayList<>();
    values.add(value);

    return new FilterCondition(fieldName, FilterCondition.Operator.EQUALS, values, null, null);
  }

  /**
   * Extract values from a comma-separated list in parentheses
   * Example: "(value1, value2)" -> ["value1", "value2"]
   *
   * @param valuesPart the string containing parentheses and values
   * @return list of trimmed values
   */
  private static List<String> extractValues(String valuesPart) {
    List<String> values = new ArrayList<>();

    if (!valuesPart.startsWith("(") || !valuesPart.endsWith(")")) {
      LOG.warn("Invalid value format: expected parentheses in: " + valuesPart);
      return values;
    }

    String inner = valuesPart.substring(1, valuesPart.length() - 1).trim();
    if (inner.isEmpty()) {
      LOG.warn("Empty value list in: " + valuesPart);
      return values;
    }

    String[] parts = inner.split(",");
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        values.add(trimmed);
      }
    }

    return values;
  }

  /**
   * Check if a row matches all filter conditions
   *
   * @param row the row to check
   * @param conditions the filter conditions
   * @param fieldTitles the column titles
   * @return true if row matches all conditions
   */
  private static boolean matchesAllConditions(String[] row, List<FilterCondition> conditions,
      String[] fieldTitles) {
    for (FilterCondition condition : conditions) {
      if (!matchesCondition(row, condition, fieldTitles)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if a row matches a single condition
   *
   * @param row the row to check
   * @param condition the condition to evaluate
   * @param fieldTitles the column titles
   * @return true if condition is met
   */
  private static boolean matchesCondition(String[] row, FilterCondition condition, String[] fieldTitles) {
    String fieldName = condition.getFieldName();
    int columnIndex = findColumnIndex(fieldTitles, fieldName);

    if (columnIndex < 0) {
      LOG.warn("Filter column not found: " + fieldName);
      return false;
    }

    if (columnIndex >= row.length) {
      LOG.warn("Column index " + columnIndex + " exceeds row width " + row.length);
      return false;
    }

    String cellValue = row[columnIndex];
    if (cellValue == null) {
      cellValue = "";
    }

    // Apply function if present
    List<String> valuesToCheck = new ArrayList<>();
    if (condition.hasFunction()) {
      if ("split".equals(condition.getFunctionName())) {
        String separator = condition.getFunctionArg();
        String[] parts = cellValue.split(java.util.regex.Pattern.quote(separator), -1);
        for (String part : parts) {
          valuesToCheck.add(part.trim());
        }
      } else {
        LOG.warn("Unknown function: " + condition.getFunctionName());
        return false;
      }
    } else {
      valuesToCheck.add(cellValue);
    }

    // Apply operator
    FilterCondition.Operator operator = condition.getOperator();
    List<String> expectedValues = condition.getValues();

    if (operator == FilterCondition.Operator.EQUALS) {
      // For EQUALS with a single value, compare case-insensitively
      String expected = expectedValues.get(0);
      return cellValue.equalsIgnoreCase(expected);
    } else if (operator == FilterCondition.Operator.IN) {
      // For IN, check if ANY of the values-to-check are in the expected list (case-insensitive)
      for (String value : valuesToCheck) {
        for (String expected : expectedValues) {
          if (value.equalsIgnoreCase(expected)) {
            return true;
          }
        }
      }
      return false;
    } else if (operator == FilterCondition.Operator.NOT_IN) {
      // For NOT_IN, check if NONE of the values-to-check are in the expected list (case-insensitive)
      for (String value : valuesToCheck) {
        for (String expected : expectedValues) {
          if (value.equalsIgnoreCase(expected)) {
            return false; // Found a match, so NOT_IN fails
          }
        }
      }
      return true; // No matches found, so NOT_IN succeeds
    }

    return false;
  }

  /**
   * Find column index by name (case-insensitive)
   *
   * @param fieldTitles the column titles
   * @param columnName the column name to find
   * @return the index or -1 if not found
   */
  private static int findColumnIndex(String[] fieldTitles, String columnName) {
    for (int i = 0; i < fieldTitles.length; i++) {
      if (fieldTitles[i].equalsIgnoreCase(columnName)) {
        return i;
      }
    }
    return -1;
  }
}
