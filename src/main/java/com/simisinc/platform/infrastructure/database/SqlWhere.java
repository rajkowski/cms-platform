/*
 * Copyright 2025-2026 Matt Rajkowski (https://github.com/rajkowski)
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

package com.simisinc.platform.infrastructure.database;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * A list of where properties and values, using function names similar to SQL syntax
 *
 * @author matt rajkowski
 * @created 2/15/2025 3:11 PM
 */
public class SqlWhere {

  public static final String AND_OPERATOR = "AND";
  public static final String OR_OPERATOR = "OR";
  public static final String NOT_AND_OPERATOR = "NOT AND";
  public static final String NOT_OR_OPERATOR = "OR";

  private List<SqlValue> values = new ArrayList<>();

  public SqlWhere AND(SqlValue object) {
    values.add(object);
    return this;
  }

  public SqlWhere AND(String name) {
    values.add(new SqlValue(name));
    return this;
  }

  public SqlWhere AND(String name, String value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere AND(String name, String[] value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere AND(String name, String[] value, int sqlType) {
    values.add(new SqlValue(name, value, sqlType));
    return this;
  }

  public SqlWhere andAddIfHasValue(String name, String value) {
    if (value != null) {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere andAddIfHasValue(String name, String value, int maxLength) {
    if (value != null) {
      if (value.length() > maxLength) {
        value = value.substring(0, maxLength);
      }
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere AND(String name, long value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere AND(String name, Long[] value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere andAddIfHasValue(String name, long value, long emptyValue) {
    if (value != emptyValue) {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere AND(String name, long value, long nullValue) {
    if (value == nullValue) {
      values.add(new SqlValue(name, value, true));
    } else {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere AND(String name, int value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere andAddIfHasValue(String name, int value, int emptyValue) {
    if (value != emptyValue) {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere AND(String name, int value, int nullValue) {
    if (value == nullValue) {
      values.add(new SqlValue(name, value, true));
    } else {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere AND(String name, double value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere andAddIfHasValue(String name, double value, double emptyValue) {
    if (value != emptyValue) {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere AND(String name, double value, double nullValue) {
    if (value == nullValue) {
      values.add(new SqlValue(name, value, true));
    } else {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere andAddWhen(String name, boolean value, boolean checkValue) {
    if (value == checkValue) {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere andAddIfDataConstantExists(String name, int value) {
    if (value != DataConstants.UNDEFINED) {
      AND(name, value == DataConstants.TRUE);
    }
    return this;
  }

  public SqlWhere AND(String name, Timestamp value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere AND(String name, Timestamp[] value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere AND(String name, Object[] value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere andAddIfHasValue(String name, Timestamp value) {
    if (value != null) {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere AND(String name, BigDecimal value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere andAddIfHasValue(String name, BigDecimal value) {
    if (value != null) {
      values.add(new SqlValue(name, value));
    }
    return this;
  }

  public SqlWhere AND(String name, boolean value) {
    values.add(new SqlValue(name, value));
    return this;
  }

  public SqlWhere andGeomPoint(String name, double latitude, double longitude) {
    values.add(new SqlValue(name, SqlValue.GEOM_TYPE, latitude, longitude));
    return this;
  }

  public List<SqlValue> getValues() {
    return values;
  }

  public SqlWhere AND(String jsonbColumnName, String[] arrayValues, String operator) {
    String whereStatement = arrayToSqlStatementList(jsonbColumnName, arrayValues, operator);
    if (StringUtils.isBlank(whereStatement)) {
      return this;
    }

    // Each placeholder is cast to jsonb, so each value must be its own single-element JSON array
    String[] jsonArrayValues = new String[arrayValues.length];
    for (int i = 0; i < arrayValues.length; i++) {
      jsonArrayValues[i] = toJsonArrayLiteral(arrayValues[i]);
    }

    values.add(new SqlValue(whereStatement, jsonArrayValues));
    return this;
  }

  /**
   * Wraps a single string value as a one-element JSON array literal, escaping characters
   * as required by the JSON spec, so it can be bound to a {@code ?::jsonb} placeholder
   * used in a {@code @>} containment check (e.g. {@code value} → {@code ["value"]}).
   *
   * @param value the value to wrap
   * @return a JSON array literal containing the single value
   */
  private static String toJsonArrayLiteral(String value) {
    StringBuilder sql = new StringBuilder("[\"");
    if (value != null) {
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        switch (c) {
          case '"':
            sql.append("\\\"");
            break;
          case '\\':
            sql.append("\\\\");
            break;
          case '\n':
            sql.append("\\n");
            break;
          case '\r':
            sql.append("\\r");
            break;
          case '\t':
            sql.append("\\t");
            break;
          default:
            if (c < 0x20) {
              sql.append(String.format("\\u%04x", (int) c));
            } else {
              sql.append(c);
            }
        }
      }
    }
    sql.append("\"]");
    return sql.toString();
  }

  /**
   * Builds a parameterized SQL condition for filtering a JSONB array column against multiple values.
   *
   * <p>PostgreSQL has no single operator to test JSONB array containment against a list of values
   * using ANY()/ALL() semantics, so this method expands the values into individual
   * {@code sqlColumn @> ?::jsonb} containment checks and joins them with the given logical
   * operator, wrapped in parenthesis:
   * <ul>
   *   <li>{@code "OR"} → {@code (sqlColumn @> ?::jsonb OR sqlColumn @> ?::jsonb)}</li>
   *   <li>any other value (including null) → defaults to AND</li>
   * </ul>
   * </p>
   *
   * <p>The returned condition contains one {@code ?} placeholder per value, in order. The caller
   * is responsible for binding each value (individually wrapped as a JSON array, e.g.
   * {@code toJsonArray(new String[]{value})}) to its corresponding placeholder, such as via
   * {@code SqlWhere.AND(condition, Object[] values)}.</p>
   *
   * @param sqlColumn the JSONB column to filter (may include a table prefix, e.g. "files.tags")
   * @param values the values to match against the JSONB column
   * @param operator logical operator used to combine conditions ("AND" or "OR")
   * @return a parenthesized SQL condition string with one "?" placeholder per value, or {@code null}
   *         if {@code sqlColumn} is blank or {@code values} is null or empty
   */
  private static String arrayToSqlStatementList(String jsonbColumnName, String[] arrayValues, String operator) {
    if (StringUtils.isBlank(jsonbColumnName) || arrayValues == null || arrayValues.length == 0) {
      return null;
    }
    StringBuilder sql = new StringBuilder();

    // Default to AND if operator is null or invalid
    String op = (OR_OPERATOR.equalsIgnoreCase(operator) || NOT_OR_OPERATOR.equalsIgnoreCase(operator))
        ? OR_OPERATOR
        : AND_OPERATOR;

    // If the operator is a NOT variant, prepend "NOT " to the condition
    if (NOT_AND_OPERATOR.equalsIgnoreCase(operator) || NOT_OR_OPERATOR.equalsIgnoreCase(operator)) {
      sql.append("NOT ");
    }

    // Build the condition with one placeholder per value
    sql.append("(");
    for (int i = 0; i < arrayValues.length; i++) {
      if (i > 0) {
        sql.append(" ").append(op).append(" ");
      }
      sql.append(jsonbColumnName).append(" @> ?::jsonb");
    }
    sql.append(")");
    return sql.toString();
  }
}
