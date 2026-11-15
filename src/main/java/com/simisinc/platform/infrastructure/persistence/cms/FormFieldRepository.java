/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2026 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.infrastructure.persistence.cms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.cms.FormDefinition;
import com.simisinc.platform.domain.model.cms.FormField;

/**
 * Persists and retrieves the fields belonging to a database-backed form definition
 */
public class FormFieldRepository {

  private static Log LOG = LogFactory.getLog(FormFieldRepository.class);

  private static final String TABLE_NAME = "form_fields";

  public static FormField findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("form_field_id = ?", id)
        .returnRecord(FormFieldRepository::buildRecord);
  }

  public static List<FormField> findAllByFormDefinitionId(long formDefinitionId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("form_definition_id = ?", formDefinitionId)
        .ORDER_BY("field_order, form_field_id")
        .returnDataResult(FormFieldRepository::buildRecord)
        .getRecords();
  }

  public static FormField save(FormField record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static FormField add(FormField record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("form_definition_id", record.getFormDefinitionId());
    // Omit field_order when unset so the column default applies to a new field.
    if (record.getFieldOrder() != -1) {
      insert.FIELD("field_order", record.getFieldOrder());
    }
    insert.FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("label", StringUtils.trimToNull(record.getLabel()))
        .FIELD_UNLESS_NULL("field_type", StringUtils.trimToNull(record.getType()))
        .FIELD("required", record.isRequired())
        .FIELD_UNLESS_NULL("placeholder", StringUtils.trimToNull(record.getPlaceholder()))
        .FIELD_UNLESS_NULL("default_value", StringUtils.trimToNull(record.getDefaultValue()))
        .FIELD_UNLESS_NULL("options", formatOptions(record.getListOfOptions()));
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static FormField update(FormField record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("field_order", record.getFieldOrder())
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("label", StringUtils.trimToNull(record.getLabel()))
        .SET("field_type", StringUtils.trimToNull(record.getType()))
        .SET("required", record.isRequired())
        .SET("placeholder", StringUtils.trimToNull(record.getPlaceholder()))
        .SET("default_value", StringUtils.trimToNull(record.getDefaultValue()))
        .SET("options", formatOptions(record.getListOfOptions()))
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (update.WHERE("form_field_id = ?", record.getId()).execute()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(FormField record) {
    return DB.DELETE().FROM(TABLE_NAME).WHERE("form_field_id = ?", record.getId()).execute();
  }

  /**
   * The next field_order for a new field on the given form
   */
  public static int getNextFieldOrder(long formDefinitionId) {
    Integer maxOrder = DB.SELECT("max(field_order)")
        .FROM(TABLE_NAME)
        .WHERE("form_definition_id = ?", formDefinitionId)
        .returnRecord(rs -> rs.getInt(1));
    return maxOrder == null ? 0 : maxOrder + 1;
  }

  public static void removeAll(Connection connection, FormDefinition record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("form_definition_id = ?", record.getId()).execute(connection);
  }

  /**
   * Persists a new display order for a form's fields in one transaction
   */
  public static boolean reorderFields(long formDefinitionId, List<Long> orderedFieldIds) {
    if (orderedFieldIds == null || orderedFieldIds.isEmpty()) {
      return false;
    }
    try {
      try (Connection connection = DB.getConnection();
          AutoStartTransaction a = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {
        int order = 10;
        for (Long fieldId : orderedFieldIds) {
          if (fieldId == null) {
            continue;
          }
          FormField field = findById(fieldId);
          if (field == null || field.getFormDefinitionId() != formDefinitionId) {
            continue;
          }
          DB.UPDATE(TABLE_NAME)
              .SET("field_order", order)
              .WHERE("form_field_id = ?", fieldId)
              .execute(connection);
          order += 10;
        }
        transaction.commit();
        return true;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  /**
   * Serializes a field's options using the same comma-separated "key=value,key2=value2" format
   */
  private static String formatOptions(Map<String, String> options) {
    if (options == null || options.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : options.entrySet()) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      sb.append(entry.getKey()).append("=").append(entry.getValue());
    }
    return sb.toString();
  }

  /**
   * An entry without an "=" is stored as both its own key and value verbatim
   */
  private static Map<String, String> parseOptions(String options) {
    if (StringUtils.isBlank(options)) {
      return null;
    }
    Map<String, String> optionsMap = new LinkedHashMap<>();
    for (String option : options.split(",")) {
      if (StringUtils.isBlank(option)) {
        continue;
      }
      int idx = option.indexOf('=');
      if (idx > -1) {
        optionsMap.put(option.substring(0, idx), option.substring(idx + 1));
      } else {
        optionsMap.put(option, option);
      }
    }
    return optionsMap;
  }

  private static FormField buildRecord(ResultSet rs) {
    try {
      FormField record = new FormField();
      record.setId(rs.getLong("form_field_id"));
      record.setFormDefinitionId(rs.getLong("form_definition_id"));
      record.setFieldOrder(rs.getInt("field_order"));
      record.setName(rs.getString("name"));
      record.setLabel(rs.getString("label"));
      record.setType(rs.getString("field_type"));
      record.setRequired(rs.getBoolean("required"));
      record.setPlaceholder(rs.getString("placeholder"));
      record.setDefaultValue(rs.getString("default_value"));
      record.setListOfOptions(parseOptions(rs.getString("options")));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
