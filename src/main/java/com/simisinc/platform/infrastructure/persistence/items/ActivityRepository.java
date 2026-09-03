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

package com.simisinc.platform.infrastructure.persistence.items;

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
import com.simisinc.platform.domain.model.items.Activity;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;

/**
 * Persists and retrieves activity objects
 *
 * @author matt rajkowski
 * @created 8/20/18 11:32 AM
 */
public class ActivityRepository {

  private static Log LOG = LogFactory.getLog(ActivityRepository.class);

  private static String TABLE_NAME = "item_activity_stream";
  private static String[] PRIMARY_KEY = new String[] { "activity_id" };

  public static Activity save(Activity record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static Activity add(Activity record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      Insert insert = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("item_id", record.getItemId())
          .FIELD("collection_id", record.getCollectionId())
          .FIELD("activity_type", record.getActivityType())
          .FIELD("message_text", StringUtils.trimToNull(record.getMessageText()))
          .FIELD("created_by", record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy());
      record.setId(insert.execute(connection));
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  private static Activity update(Activity record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("message_text", StringUtils.trimToNull(record.getMessageText()))
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("activity_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Activity record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      DB.DELETE().FROM(TABLE_NAME).WHERE("activity_id = ?", record.getId()).execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Item record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", record.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Collection record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", record.getId()).execute(connection);
  }

  private static DataResult query(ActivitySpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getItemId() != -1) {
        select.AND("item_id = ?", specification.getItemId());
      }
      if (specification.getCollectionId() != -1) {
        select.AND("collection_id = ?", specification.getCollectionId());
      }
      if (specification.getCreatedBy() != -1) {
        select.AND("created_by = ?", specification.getCreatedBy());
      }
      if (specification.getActivityType() != null) {
        select.AND("upper(activity_type) = ?", specification.getActivityType().trim().toUpperCase());
      }
      if (specification.getMinTimestamp() > 0) {
        select.AND("created >= ?", new Timestamp(specification.getMinTimestamp()));
      }
      if (specification.getMaxTimestamp() > 0) {
        select.AND("created <= ?", new Timestamp(specification.getMaxTimestamp()));
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(ActivityRepository::buildRecord);
  }

  public static Activity findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("activity_id = ?", id)
        .returnRecord(ActivityRepository::buildRecord);
  }

  public static List<Activity> findAll(ActivitySpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("created desc");
    DataResult result = query(specification, constraints);
    return (List<Activity>) result.getRecords();
  }

  private static Activity buildRecord(ResultSet rs) {
    try {
      Activity record = new Activity();
      record.setId(rs.getLong("activity_id"));
      record.setItemId(rs.getLong("item_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setActivityType(rs.getString("activity_type"));
      record.setMessageText(rs.getString("message_text"));
      record.setSource(rs.getString("source"));
      record.setSourceLink(rs.getString("source_link"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
