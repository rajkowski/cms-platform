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
import com.simisinc.platform.domain.model.cms.CalendarEvent;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves calendar event objects
 *
 * @author matt rajkowski
 * @created 10/29/18 1:27 PM
 */
public class CalendarEventRepository {

  private static Log LOG = LogFactory.getLog(CalendarEventRepository.class);

  private static String TABLE_NAME = "calendar_events";
  private static String[] PRIMARY_KEY = new String[] { "event_id" };

  private static void appendWhereClause(Select select, CalendarEventSpecification specification) {
    if (specification == null) {
      return;
    }
    if (specification.getId() > -1) {
      select.AND("event_id = ?", specification.getId());
    }
    if (specification.getCalendarId() > -1) {
      select.AND("calendar_id = ?", specification.getCalendarId());
    }
    if (StringUtils.isNotBlank(specification.getUniqueId())) {
      select.AND("event_unique_id = ?", specification.getUniqueId());
    }
    if (specification.getPublishedOnly() != DataConstants.UNDEFINED) {
      if (specification.getPublishedOnly() == DataConstants.TRUE) {
        select.AND("published IS NOT NULL");
      } else {
        select.AND("published IS NULL");
      }
    }
    if (specification.getStartingDateRange() != null && specification.getEndingDateRange() != null) {
      select.AND("((start_date >= ? AND start_date < ?) OR (end_date >= ? AND end_date < ?))",
          specification.getStartingDateRange(), specification.getEndingDateRange(), specification.getStartingDateRange(),
          specification.getEndingDateRange());
    } else if (specification.getStartingDateRange() != null) {
      select.AND("start_date >= ?", specification.getStartingDateRange());
    } else if (specification.getEndingDateRange() != null) {
      select.AND("(start_date < ? AND end_date < ?)", specification.getEndingDateRange(), specification.getEndingDateRange());
    }
  }

  private static DataResult<CalendarEvent> query(CalendarEventSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("calendar_events.*").FROM(TABLE_NAME);
    appendWhereClause(select, specification);
    if (specification != null && StringUtils.isNotBlank(specification.getSearchTerm())) {
      String searchTerm = specification.getSearchTerm().trim();
      select.SELECT("ts_rank_cd(tsv, websearch_to_tsquery('title_stem', ?)) AS rank", (Object[]) new Object[] { searchTerm });
      select.AND("tsv @@ websearch_to_tsquery('title_stem', ?)", searchTerm);
      if (specification.getStartingDateRange() != null) {
        select.ORDER_BY("rank DESC, start_date");
      } else {
        select.ORDER_BY("rank DESC, start_date DESC");
      }
    }

    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(CalendarEventRepository::buildRecord);
  }

  public static CalendarEvent findByUniqueId(Long calendarId, String eventUniqueId) {
    if (StringUtils.isBlank(eventUniqueId)) {
      return null;
    }
    return DB.SELECT("calendar_events.*")
        .FROM(TABLE_NAME)
        .WHERE("calendar_id = ?", calendarId)
        .AND("event_unique_id = ?", eventUniqueId)
        .returnRecord(CalendarEventRepository::buildRecord);
  }

  public static CalendarEvent findByUniqueId(String eventUniqueId) {
    if (StringUtils.isBlank(eventUniqueId)) {
      return null;
    }
    return DB.SELECT("calendar_events.*")
        .FROM(TABLE_NAME)
        .WHERE("event_unique_id = ?", eventUniqueId)
        .returnRecord(CalendarEventRepository::buildRecord);
  }

  public static CalendarEvent findById(Long calendarEventId) {
    if (calendarEventId == -1) {
      return null;
    }
    return DB.SELECT("calendar_events.*")
        .FROM(TABLE_NAME)
        .WHERE("event_id = ?", calendarEventId)
        .returnRecord(CalendarEventRepository::buildRecord);
  }

  public static List<CalendarEvent> findAll() {
    return findAll(null, null);
  }

  public static List<CalendarEvent> findAll(CalendarEventSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("start_date");
    DataResult result = query(specification, constraints);
    return (List<CalendarEvent>) result.getRecords();
  }

  public static long findCount(CalendarEventSpecification specification) {
    Select select = DB.SELECT().COUNT("*").FROM(TABLE_NAME);
    appendWhereClause(select, specification);
    return select.returnCount();
  }

  public static CalendarEvent save(CalendarEvent record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static CalendarEvent add(CalendarEvent record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("calendar_id", record.getCalendarId())
        .FIELD("event_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("body", StringUtils.trimToNull(record.getBody()))
        .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
        .FIELD("all_day", record.getAllDay())
        .FIELD("start_date", record.getStartDate())
        .FIELD("end_date", record.getEndDate())
        .FIELD("details_url", StringUtils.trimToNull(record.getDetailsUrl()))
        .FIELD("sign_up_url", StringUtils.trimToNull(record.getSignUpUrl()))
        .FIELD("location_name", StringUtils.trimToNull(record.getLocation()))
        .FIELD("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("published", record.getPublished())
        .FIELD("archived", record.getArchived());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static CalendarEvent update(CalendarEvent record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("calendar_id", record.getCalendarId())
        .SET("event_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("body", StringUtils.trimToNull(record.getBody()))
        .SET("summary", StringUtils.trimToNull(record.getSummary()))
        .SET("all_day", record.getAllDay())
        .SET("start_date", record.getStartDate())
        .SET("end_date", record.getEndDate())
        .SET("details_url", StringUtils.trimToNull(record.getDetailsUrl()))
        .SET("sign_up_url", StringUtils.trimToNull(record.getSignUpUrl()))
        .SET("location_name", StringUtils.trimToNull(record.getLocation()))
        .SET("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("published", record.getPublished())
        .SET("archived", record.getArchived())
        .WHERE("event_id = ?", record.getId());
    if (update.execute()) {
      // CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(CalendarEvent record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      //        ItemCategoryRepository.removeAll(connection, record);
      //        CollectionRepository.updateItemCount(connection, record.getCollectionId(), -1);
      //        CategoryRepository.updateItemCount(connection, record.getCategoryId(), -1);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("event_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Calendar calendar) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("calendar_id = ?", calendar.getId()).execute(connection);
  }

  private static CalendarEvent buildRecord(ResultSet rs) {
    try {
      CalendarEvent record = new CalendarEvent();
      record.setId(rs.getLong("event_id"));
      record.setCalendarId(rs.getLong("calendar_id"));
      record.setUniqueId(rs.getString("event_unique_id"));
      record.setTitle(rs.getString("title"));
      record.setBody(rs.getString("body"));
      record.setSummary(rs.getString("summary"));
      record.setAllDay(rs.getBoolean("all_day"));
      record.setStartDate(rs.getTimestamp("start_date"));
      record.setEndDate(rs.getTimestamp("end_date"));
      record.setDetailsUrl(rs.getString("details_url"));
      record.setSignUpUrl(rs.getString("sign_up_url"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setPublished(rs.getTimestamp("published"));
      record.setArchived(rs.getTimestamp("archived"));
      record.setLatitude(rs.getDouble("latitude"));
      record.setLongitude(rs.getDouble("longitude"));
      record.setLocation(rs.getString("location_name"));
      record.setStreet(rs.getString("street"));
      record.setAddressLine2(rs.getString("address_line_2"));
      record.setAddressLine3(rs.getString("address_line_3"));
      record.setCity(rs.getString("city"));
      record.setState(rs.getString("state"));
      record.setCountry(rs.getString("country"));
      record.setPostalCode(rs.getString("postal_code"));
      record.setCounty(rs.getString("county"));
      record.setImageUrl(rs.getString("image_url"));
      record.setVideoUrl(rs.getString("video_url"));
      record.setVideoEmbed(rs.getString("video_embed"));
      record.setScriptEmbed(rs.getString("script_embed"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
