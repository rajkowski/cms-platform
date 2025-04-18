/*
 * Copyright 2025 Matt Rajkowski (https://github.com/rajkowski)
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

import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * A list of where properties and values, using function names similar to SQL syntax
 *
 * @author matt rajkowski
 * @created 2/15/2025 3:11 PM
 */
public class SqlWhere {

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

}
