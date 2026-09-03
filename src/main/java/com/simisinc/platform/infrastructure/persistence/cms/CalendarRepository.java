/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.cms.Calendar;

/**
 * Persists and retrieves calendar objects
 *
 * @author matt rajkowski
 * @created 10/29/18 2:02 PM
 */
public class CalendarRepository {

  private static Log LOG = LogFactory.getLog(CalendarRepository.class);

  private static String TABLE_NAME = "calendars";
  private static String[] PRIMARY_KEY = new String[] { "calendar_id" };

  private static DataResult<Calendar> query(CalendarSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("calendars.*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("calendar_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("calendar_unique_id = ?", specification.getUniqueId());
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(CalendarRepository::buildRecord);
  }

  public static Calendar findById(long calendarId) {
    if (calendarId == -1) {
      return null;
    }
    return DB.SELECT("calendars.*")
        .FROM(TABLE_NAME)
        .WHERE("calendar_id = ?", calendarId)
        .returnRecord(CalendarRepository::buildRecord);
  }

  public static Calendar findByUniqueId(String calendarUniqueId) {
    if (StringUtils.isBlank(calendarUniqueId)) {
      return null;
    }
    return DB.SELECT("calendars.*")
        .FROM(TABLE_NAME)
        .WHERE("calendar_unique_id = ?", calendarUniqueId)
        .returnRecord(CalendarRepository::buildRecord);
  }

  public static List<Calendar> findAll() {
    return findAll(null, null);
  }

  public static List<Calendar> findAll(CalendarSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("calendar_id");
    return query(specification, constraints).getRecords();
  }

  public static Calendar save(Calendar record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static Calendar add(Calendar record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("calendar_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("description", StringUtils.trimToNull(record.getDescription()))
        .FIELD("color", StringUtils.trimToNull(record.getColor()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("enabled", record.getEnabled());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static Calendar update(Calendar record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("calendar_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("color", StringUtils.trimToNull(record.getColor()))
        .SET("enabled", record.getEnabled())
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("calendar_id = ?", record.getId());
    if (update.execute()) {
      //      CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Calendar record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      CalendarEventRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("calendar_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static Calendar buildRecord(ResultSet rs) {
    try {
      Calendar record = new Calendar();
      record.setId(rs.getLong("calendar_id"));
      record.setUniqueId(rs.getString("calendar_unique_id"));
      record.setName(rs.getString("name"));
      record.setDescription(rs.getString("description"));
      record.setColor(rs.getString("color"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setEnabled(rs.getBoolean("enabled"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
