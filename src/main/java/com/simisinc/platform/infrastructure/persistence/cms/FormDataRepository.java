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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.cms.FormDataJSONCommand;
import com.simisinc.platform.domain.model.cms.FormData;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves form data objects
 *
 * @author matt rajkowski
 * @created 6/1/18 2:42 PM
 */
public class FormDataRepository {

  private static Log LOG = LogFactory.getLog(FormDataRepository.class);

  private static String TABLE_NAME = "form_data";
  private static String[] PRIMARY_KEY = new String[] { "form_data_id" };

  private static DataResult<FormData> query(FormDataSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() != -1) {
        select.AND("form_data_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getFormUniqueId())) {
        select.AND("form_unique_id = ?", specification.getFormUniqueId());
      }
      if (specification.getSessionId() != null) {
        select.AND("session_id = ?", specification.getSessionId());
      }
      if (specification.getClaimedBy() != -1L) {
        select.AND("claimed_by = ?", specification.getClaimedBy());
      }
      if (specification.getFlaggedAsSpam() != DataConstants.UNDEFINED) {
        if (specification.getFlaggedAsSpam() == DataConstants.TRUE) {
          select.AND("flagged_as_spam = true");
        } else {
          select.AND("flagged_as_spam = false");
        }
      }
      if (specification.getClaimed() != DataConstants.UNDEFINED) {
        if (specification.getClaimed() == DataConstants.TRUE) {
          select.AND("claimed IS NOT NULL");
        } else {
          select.AND("claimed IS NULL");
        }
      }
      if (specification.getDismissed() != DataConstants.UNDEFINED) {
        if (specification.getDismissed() == DataConstants.TRUE) {
          select.AND("dismissed IS NOT NULL");
        } else {
          select.AND("dismissed IS NULL");
        }
      }
      if (specification.getProcessed() != DataConstants.UNDEFINED) {
        if (specification.getProcessed() == DataConstants.TRUE) {
          select.AND("processed IS NOT NULL");
        } else {
          select.AND("processed IS NULL");
        }
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(FormDataRepository::buildRecord);
  }

  public static FormData findById(long formDataId) {
    if (formDataId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("form_data_id = ?", formDataId)
        .returnRecord(FormDataRepository::buildRecord);
  }

  public static List<FormData> findAll(FormDataSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("form_data_id desc");
    return query(specification, constraints).getRecords();
  }

  public static FormData save(FormData record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static FormData add(FormData record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("form_unique_id", StringUtils.trimToNull(record.getFormUniqueId()))
        .FIELD("ip_address", record.getIpAddress())
        .FIELD("session_id", record.getSessionId())
        .FIELD("url", record.getUrl())
        .FIELD("flagged_as_spam", record.getFlaggedAsSpam())
        .FIELD("created_by", record.getCreatedBy() == -1 ? null : record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy());
    if (StringUtils.isNotBlank(record.getQueryParameters())) {
      String queryString = record.getQueryParameters();
      queryString = Strings.CS.replace(queryString, "%20", " ");
      insert.FIELD("query_params", queryString);
      record.setQueryParameters(queryString);
    }
    insert.FIELD("field_values", FormDataJSONCommand.createJSONString(record), com.github.rajkowski.database.CastType.JSONB);
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static FormData update(FormData record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("form_data_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean markAsArchived(FormData record, long userId) {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("dismissed", timestamp)
        .SET("dismissed_by", userId);
    boolean updated = update.WHERE("form_data_id = ?", record.getId()).execute();
    if (updated) {
      record.setDismissed(timestamp);
      record.setDismissedBy(userId);
    }
    return updated;
  }

  public static boolean tryToMarkAsClaimed(FormData record, long userId) {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("claimed", timestamp)
        .SET("claimed_by", userId);
    boolean updated = update.WHERE("form_data_id = ?", record.getId()).AND("claimed IS NULL").execute();
    if (updated) {
      record.setClaimed(timestamp);
      record.setClaimedBy(userId);
    }
    return updated;
  }

  public static boolean markAsProcessed(FormData record, long userId) {
    return markAsProcessed(record, userId, null);
  }

  public static boolean markAsProcessed(FormData record, long userId, String system) {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("processed", timestamp)
        .SET("processed_by", userId);
    if (system != null) {
      update.SET("processed_system", system);
    }
    boolean updated = update.WHERE("form_data_id = ?", record.getId()).execute();
    if (updated) {
      record.setProcessed(timestamp);
      record.setProcessedBy(userId);
    }
    return updated;
  }

  private static FormData buildRecord(ResultSet rs) {
    try {
      FormData record = new FormData();
      record.setId(rs.getLong("form_data_id"));
      record.setFormUniqueId(rs.getString("form_unique_id"));
      FormDataJSONCommand.populateFromJSONString(record, rs.getString("field_values"));
      record.setIpAddress(rs.getString("ip_address"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      record.setClaimed(rs.getTimestamp("claimed"));
      record.setClaimedBy(rs.getLong("claimed_by"));
      record.setDismissed(rs.getTimestamp("dismissed"));
      record.setUrl(rs.getString("url"));
      record.setQueryParameters(rs.getString("query_params"));
      record.setFlaggedAsSpam(rs.getBoolean("flagged_as_spam"));
      record.setSessionId(rs.getString("session_id"));
      record.setDismissedBy(rs.getLong("dismissed_by"));
      record.setProcessed(rs.getTimestamp("processed"));
      record.setProcessedBy(rs.getLong("processed_by"));
      record.setProcessedSystem(rs.getString("processed_system"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
