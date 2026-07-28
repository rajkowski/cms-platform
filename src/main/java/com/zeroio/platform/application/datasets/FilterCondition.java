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

/**
 * Represents a single filter condition in a CQL query
 * 
 * Supports operators:
 * - EQUALS: key=value (simple equality)
 * - IN: key in (value1, value2) (value must be in list)
 * - NOT_IN: key not in (value1, value2) (value must NOT be in list)
 * 
 * Supports functions:
 * - split(separator): splits field value by separator and applies operator to array
 * 
 * Examples:
 * - Labels=draft
 * - Labels in (draft, review)
 * - Labels not in (draft, review)
 * - Labels:split(;) in (draft, review)
 * - Labels:split(;) not in (draft, review)
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class FilterCondition {

  public enum Operator {
    EQUALS, IN, NOT_IN
  }

  private String fieldName;
  private Operator operator;
  private List<String> values;
  private String functionName;
  private String functionArg;

  /**
   * Create a simple equality condition
   * 
   * @param fieldName the field name
   * @param value the value to match
   */
  public FilterCondition(String fieldName, String value) {
    this.fieldName = fieldName;
    this.operator = Operator.EQUALS;
    this.values = new ArrayList<>();
    this.values.add(value);
    this.functionName = null;
    this.functionArg = null;
  }

  /**
   * Create a condition with operator and values
   * 
   * @param fieldName the field name
   * @param operator the operator to use
   * @param values the list of values to compare against
   */
  public FilterCondition(String fieldName, Operator operator, List<String> values) {
    this.fieldName = fieldName;
    this.operator = operator;
    this.values = values;
    this.functionName = null;
    this.functionArg = null;
  }

  /**
   * Create a condition with function support
   * 
   * @param fieldName the field name
   * @param operator the operator to use
   * @param values the list of values to compare against
   * @param functionName the function name (e.g., "split")
   * @param functionArg the function argument (e.g., ";")
   */
  public FilterCondition(String fieldName, Operator operator, List<String> values, String functionName,
      String functionArg) {
    this.fieldName = fieldName;
    this.operator = operator;
    this.values = values;
    this.functionName = functionName;
    this.functionArg = functionArg;
  }

  // ==================== Getters ====================

  public String getFieldName() {
    return fieldName;
  }

  public Operator getOperator() {
    return operator;
  }

  public List<String> getValues() {
    return values;
  }

  public String getFunctionName() {
    return functionName;
  }

  public String getFunctionArg() {
    return functionArg;
  }

  public boolean hasFunction() {
    return functionName != null && !functionName.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(fieldName);
    if (hasFunction()) {
      sb.append(":").append(functionName).append("(").append(functionArg).append(")");
    }
    sb.append(" ").append(operator.name().toLowerCase()).append(" (");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0)
        sb.append(", ");
      sb.append(values.get(i));
    }
    sb.append(")");
    return sb.toString();
  }
}
