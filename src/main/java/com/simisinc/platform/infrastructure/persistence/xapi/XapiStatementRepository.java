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

package com.simisinc.platform.infrastructure.persistence.xapi;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.xapi.XapiStatement;

/**
 * Persists and retrieves experience api (xAPI) statement objects
 *
 * @author matt rajkowski
 * @created 4/6/2021 8:15 AM
 */
public class XapiStatementRepository {

  private static Log LOG = LogFactory.getLog(XapiStatementRepository.class);

  private static String TABLE_NAME = "xapi_statements";
  private static String[] PRIMARY_KEY = new String[] { "statement_id" };

  private static DataResult<XapiStatement> query(XapiStatementSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*")
        .FROM(TABLE_NAME);
    if (specification != null) {
      select.WHERE()
          .AND_SKIP_IF_MATCHES("statement_id = ?", specification.getId(), -1)
          .AND_SKIP_IF_MATCHES("actor_id = ?", specification.getActorId(), -1)
          .AND_SKIP_IF_MATCHES("object_id = ?", specification.getObjectId(), -1)
          .AND_SKIP_IF_NULL("verb = ?", specification.getVerb())
          .AND_SKIP_IF_NULL("object = ?", specification.getObject());
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(XapiStatementRepository::buildRecord);
  }

  public static XapiStatement findById(long statementId) {
    if (statementId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("statement_id = ?", statementId)
        .returnRecord(XapiStatementRepository::buildRecord);
  }

  public static List<XapiStatement> findAll() {
    return findAll(null, null);
  }

  public static List<XapiStatement> findAll(XapiStatementSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("statement_id");
    // occurred_at < CURRENT_TIMESTAMP
    // constraints.setColumnToSortBy("occurred_at", "desc");
    return query(specification, constraints).getRecords();
  }

  public static XapiStatement save(XapiStatement record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static XapiStatement add(XapiStatement record) {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("message", StringUtils.trimToNull(record.getMessage()))
        .FIELD("message_snapshot", StringUtils.trimToNull(record.getMessageSnapshot()))
        .FIELD_UNLESS_MATCHES("actor_id", record.getActorId(), -1)
        .FIELD("verb", StringUtils.trimToNull(record.getVerb()))
        .FIELD("object", StringUtils.trimToNull(record.getObject()))
        .FIELD_UNLESS_MATCHES("object_id", record.getObjectId(), -1)
        .FIELD_UNLESS_NULL("occurred_at", record.getOccurredAt())
        .FIELD_UNLESS_NULL("authority", StringUtils.trimToNull(record.getAuthority()))
        .FIELD_UNLESS_MATCHES("user_context", record.getContextUserId(), -1)
        .FIELD_UNLESS_MATCHES("item_context", record.getContextItemId(), -1)
        .FIELD_UNLESS_MATCHES("project_context", record.getContextProjectId(), -1)
        .FIELD_UNLESS_MATCHES("issue_context", record.getContextIssueId(), -1)
        .execute();
    record.setId(generatedId);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static XapiStatement update(XapiStatement record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("message", StringUtils.trimToNull(record.getMessage()))
        .SET("message_snapshot", StringUtils.trimToNull(record.getMessageSnapshot()))
        .SET("actor_id", record.getActorId() == -1 ? null : record.getActorId())
        .SET("verb", StringUtils.trimToNull(record.getVerb()))
        .SET("object", StringUtils.trimToNull(record.getObject()))
        .SET("object_id", record.getObjectId() == -1 ? null : record.getObjectId())
        .SET("occurred_at", record.getOccurredAt())
        .SET("authority", StringUtils.trimToNull(record.getAuthority()))
        .SET("user_context", record.getContextUserId() == -1 ? null : record.getContextUserId())
        .SET("item_context", record.getContextItemId() == -1 ? null : record.getContextItemId())
        .SET("project_context", record.getContextProjectId() == -1 ? null : record.getContextProjectId())
        .SET("issue_context", record.getContextIssueId() == -1 ? null : record.getContextIssueId())
        .WHERE("statement_id = ?", record.getId())
        .execute();
    if (updated) {
      // CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(XapiStatement record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("statement_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static XapiStatement buildRecord(ResultSet rs) {
    try {
      XapiStatement record = new XapiStatement();
      record.setId(rs.getLong("statement_id"));
      record.setMessage(rs.getString("message"));
      record.setMessageSnapshot(rs.getString("message_snapshot"));
      record.setActorId(DB.getLong(rs, "actor_id", -1));
      record.setVerb(rs.getString("verb"));
      record.setObject(rs.getString("object"));
      record.setObjectId(DB.getLong(rs, "object_id", -1));
      record.setOccurredAt(rs.getTimestamp("occurred_at"));
      record.setCreated(rs.getTimestamp("created"));
      record.setAuthority(rs.getString("authority"));
      record.setContextUserId(DB.getLong(rs, "user_context", -1));
      record.setContextItemId(DB.getLong(rs, "item_context", -1));
      record.setContextProjectId(DB.getLong(rs, "project_context", -1));
      record.setContextIssueId(DB.getLong(rs, "issue_context", -1));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
