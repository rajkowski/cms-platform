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

package com.simisinc.platform.infrastructure.persistence.datasets;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.CustomFieldListJSONCommand;
import com.simisinc.platform.application.datasets.DatasetColumnJSONCommand;
import com.simisinc.platform.domain.model.datasets.Dataset;

/**
 * Persists and retrieves dataset objects
 *
 * @author matt rajkowski
 * @created 4/24/18 7:40 PM
 */
public class DatasetRepository {

  private static Log LOG = LogFactory.getLog(DatasetRepository.class);

  private static String TABLE_NAME = "datasets";
  private static String[] PRIMARY_KEY = new String[] { "dataset_id" };

  private static final int STATUS_READY = 0;
  private static final int STATUS_RUNNING = 1;
  private static final int STATUS_FAILED = 2;

  public static Dataset findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("dataset_id = ?", id)
        .returnRecord(DatasetRepository::buildRecord);
  }

  public static Dataset findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    String normalizedName = name.toLowerCase().trim();
    Dataset record = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(name) = ?", normalizedName)
        .returnRecord(DatasetRepository::buildRecord);
    if (record == null) {
      record = DB.SELECT("*")
          .FROM(TABLE_NAME)
          .WHERE("LOWER(name) = ?", normalizedName.replace("-", " "))
          .returnRecord(DatasetRepository::buildRecord);
    }
    return record;
  }

  public static Dataset findByWebPath(String versionWebPath) {
    if (StringUtils.isBlank(versionWebPath)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("web_path = ?", versionWebPath)
        .returnRecord(DatasetRepository::buildRecord);
  }

  public static Dataset findByWebPathAndId(String versionWebPath, long id) {
    if (StringUtils.isBlank(versionWebPath) || id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("web_path = ?", versionWebPath)
        .AND("dataset_id = ?", id)
        .returnRecord(DatasetRepository::buildRecord);
  }

  public static List<Dataset> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("name"))
        .returnDataResult(DatasetRepository::buildRecord).getRecords();
  }

  public static List<Dataset> findAllScheduledForDownload() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("schedule_enabled = ?", true)
        .AND("schedule_frequency IS NOT NULL")
        .AND("source_url IS NOT NULL")
        .AND("CURRENT_TIMESTAMP > last_download + schedule_frequency")
        .AND("queue_date IS NULL OR CURRENT_TIMESTAMP > queue_date + queue_interval")
        .AND("queue_status = ?", STATUS_READY)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("queue_status"))
        .returnDataResult(DatasetRepository::buildRecord).getRecords();
  }

  public static Dataset save(Dataset record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static Dataset add(Dataset record) {
    record.setId(DB.INSERT().INTO(TABLE_NAME)
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("source_url", record.getSourceUrl())
        .FIELD("source_info", record.getSourceInfo())
        .FIELD("filename", StringUtils.trimToNull(record.getFilename()))
        .FIELD("file_length", record.getFileLength())
        .FIELD("file_type", record.getFileType())
        .FIELD("file_hash", record.getFileHash())
        .FIELD("web_path", StringUtils.trimToNull(record.getWebPath()))
        .FIELD("path", StringUtils.trimToNull(record.getFileServerPath()))
        .FIELD("last_download", record.getLastDownload())
        .FIELD("records_path", record.getRecordsPath())
        .FIELD("paging_url_path", record.getPagingUrlPath())
        .FIELD("column_count", record.getColumnCount() == -1 ? null : record.getColumnCount())
        .FIELD("row_count", record.getRowCount())
        .FIELD("collection_unique_id", record.getCollectionUniqueId())
        .FIELD("category_id", record.getCategoryId())
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("column_config", DatasetColumnJSONCommand.createColumnJSONString(record), CastType.JSONB)
        .FIELD("request_config", StringUtils.trimToNull(record.getRequestConfig()), CastType.JSONB)
        .execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static Dataset update(Dataset record) {
    // Update
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("source_url", record.getSourceUrl())
        .SET("source_info", record.getSourceInfo())
        .SET("filename", StringUtils.trimToNull(record.getFilename()))
        .SET("file_length", record.getFileLength())
        .SET("file_type", record.getFileType())
        .SET("file_hash", record.getFileHash())
        .SET("web_path", StringUtils.trimToNull(record.getWebPath()))
        .SET("path", StringUtils.trimToNull(record.getFileServerPath()))
        .SET("last_download", record.getLastDownload())
        .SET("records_path", record.getRecordsPath())
        .SET("paging_url_path", record.getPagingUrlPath())
        .SET("column_count", record.getColumnCount() == -1 ? null : record.getColumnCount())
        .SET("row_count", record.getRowCount())
        .SET("modified_by", record.getModifiedBy())
        .SET("column_config", DatasetColumnJSONCommand.createColumnJSONString(record), CastType.JSONB)
        .SET("request_config", StringUtils.trimToNull(record.getRequestConfig()), CastType.JSONB)
        .WHERE("dataset_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static Dataset updateDetails(Dataset record) {
    // Update
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("name", record.getName())
        .SET("source_info", record.getSourceInfo())
        .SET("modified_by", record.getModifiedBy())
        .WHERE("dataset_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("updateDetails failed!");
    return null;
  }

  public static Dataset updateConfiguration(Dataset record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("records_path", record.getRecordsPath())
        .SET("paging_url_path", record.getPagingUrlPath())
        .SET("column_count", record.getColumnCount() == -1 ? null : record.getColumnCount())
        .SET("row_count", record.getRowCount())
        .SET("column_config", DatasetColumnJSONCommand.createColumnJSONString(record), CastType.JSONB)
        .WHERE("dataset_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("updateConfiguration failed!");
    return null;
  }

  public static Dataset updateMapping(Dataset record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("collection_unique_id", StringUtils.trimToNull(record.getCollectionUniqueId()))
        .SET("category_id", record.getCategoryId())
        .SET("unique_column_name", record.getUniqueColumnName())
        .SET("column_config", DatasetColumnJSONCommand.createColumnJSONString(record), CastType.JSONB)
        .WHERE("dataset_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("updateMapping failed!");
    return null;
  }

  public static Dataset updateScheduleAndSyncDetails(Dataset record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("schedule_enabled", record.getScheduleEnabled())
        .SET("schedule_frequency", record.getScheduleFrequency(), CastType.INTERVAL)
        .SET("sync_enabled", record.getSyncEnabled())
        .SET("sync_merge_type", record.getSyncMergeType())
        .WHERE("dataset_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("updateScheduleAndSyncDetails failed!");
    return null;
  }

  public static Dataset updateCustomFields(Dataset record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (record.getCustomFieldList() != null && !record.getCustomFieldList().isEmpty()) {
      update.SET("field_values", CustomFieldListJSONCommand.createJSONString(record.getCustomFieldList()), CastType.JSONB);
    } else {
      update.SET("field_values", (String) null, CastType.JSONB);
    }
    update.WHERE("dataset_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("updateCustomFields failed!");
    return null;
  }

  public static Dataset updateCollectionUniqueId(Dataset record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("collection_unique_id", StringUtils.trimToNull(record.getCollectionUniqueId()))
        .SET("category_id", record.getCategoryId())
        .WHERE("dataset_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("updateCollectionUniqueId failed!");
    return null;
  }

  public static Dataset updateRowsProcessed(Dataset record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("rows_processed", record.getRowsProcessed())
        .WHERE("dataset_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("updateRowsProcessed failed!");
    return null;
  }

  /*
   * Attempts to lock a record for processing.
   * Status 0 = available, 1 = locked, 2 = red flag
   */
  public static boolean markAsQueuedIfAllowed(Dataset record) {
    return DB.UPDATE(TABLE_NAME)
        .SET("queue_status", STATUS_RUNNING)
        .SET("queue_date = CURRENT_TIMESTAMP")
        .SET("queue_attempts = queue_attempts + 1")
        .SET("schedule_last_run = CURRENT_TIMESTAMP")
        .WHERE("dataset_id = ?", record.getId())
        .AND("queue_status = ?", STATUS_READY)
        .execute();
  }

  public static boolean markAsUnqueued(Dataset record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("queue_status", STATUS_READY)
        .SET("queue_date = NULL")
        .SET("queue_message = NULL")
        .SET("queue_attempts = 0")
        .SET("queue_interval", "PT5M", CastType.INTERVAL);
    return update.WHERE("dataset_id = ?", record.getId()).execute();
  }

  public static boolean markToRetryDownload(Dataset record, String reason) {
    int queueStatus = STATUS_READY;
    String retryInterval = "PT5M";
    if (record.getQueueAttempts() < 5) {
      retryInterval = "PT5M";
    } else if (record.getQueueAttempts() < 10) {
      retryInterval = "PT30M";
    } else if (record.getQueueAttempts() < 20) {
      retryInterval = "PT6H";
    } else if (record.getQueueAttempts() < 30) {
      retryInterval = "P1D";
    } else {
      queueStatus = STATUS_FAILED;
    }
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("queue_status", queueStatus)
        .SET("queue_message", reason)
        .SET("queue_interval", retryInterval, CastType.INTERVAL);
    return update.WHERE("dataset_id = ?", record.getId()).execute();
  }

  public static boolean markLastDownload(Dataset record) {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("last_download", timestamp)
        .WHERE("dataset_id = ?", record.getId());
    boolean updated = update.execute().booleanValue();
    if (updated) {
      record.setLastDownload(timestamp);
    }
    return updated;
  }

  public static boolean markScheduleLastRun(Dataset record, int status, String message) {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("schedule_last_run", timestamp)
        .WHERE("dataset_id = ?", record.getId());
    boolean updated = update.execute().booleanValue();
    if (updated) {
      record.setScheduleLastRun(timestamp);
    }
    return updated;
  }

  public static boolean markAsProcessStarted(Dataset record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("process_status", STATUS_RUNNING)
        .SET("process_message", (String) null)
        .WHERE("dataset_id = ?", record.getId());
    boolean updated = update.execute().booleanValue();
    if (updated) {
      record.setProcessStatus(STATUS_RUNNING);
      record.setProcessMessage(null);
    }
    return updated;
  }

  public static boolean markAsProcessFinished(Dataset record, String message) {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("process_status", STATUS_READY)
        .SET("processed", timestamp)
        .SET("process_message", message)
        .SET("processed_ms", record.getTotalProcessTime())
        .WHERE("dataset_id = ?", record.getId());
    boolean updated = update.execute().booleanValue();
    if (updated) {
      record.setProcessStatus(STATUS_READY);
      record.setProcessMessage(message);
    }
    return updated;
  }

  public static boolean resetSyncTimestamp(Dataset record, Timestamp timestamp) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("sync_status", STATUS_RUNNING)
        .SET("sync_date", timestamp)
        .SET("sync_message", (String) null)
        .WHERE("dataset_id = ?", record.getId());
    boolean updated = update.execute().booleanValue();
    if (updated) {
      record.setSyncStatus(STATUS_RUNNING);
      record.setSyncDate(timestamp);
      record.setSyncMessage(null);
    }
    return updated;
  }

  public static boolean saveSyncResult(Dataset record, String message) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("sync_status", STATUS_READY)
        .SET("sync_message", message)
        .WHERE("dataset_id = ?", record.getId());
    boolean updated = update.execute().booleanValue();
    if (updated) {
      record.setSyncStatus(STATUS_READY);
      record.setSyncMessage(message);
    }
    return updated;
  }

  public static boolean remove(Dataset record) {
    return DB.DELETE().FROM(TABLE_NAME).WHERE("dataset_id = ?", record.getId()).execute();
  }

  public static long findTotalFileSize() {
    return DB.SELECT("SUM(file_length) AS total_file_length").FROM(TABLE_NAME).returnValue(Long.class);
  }

  private static Dataset buildRecord(ResultSet rs) {
    try {
      Dataset record = new Dataset();
      record.setId(rs.getLong("dataset_id"));
      record.setName(rs.getString("name"));
      record.setFilename(rs.getString("filename"));
      record.setFileServerPath(rs.getString("path"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setProcessed(rs.getTimestamp("processed"));
      record.setTotalProcessTime(rs.getLong("processed_ms"));
      record.setFileLength(rs.getLong("file_length"));
      record.setRowCount(rs.getInt("row_count"));
      record.setColumnCount(DB.getInt(rs, "column_count", 0));
      record.setFileType(rs.getString("file_type"));
      record.setCollectionUniqueId(rs.getString("collection_unique_id"));
      record.setRowsProcessed(rs.getInt("rows_processed"));
      record.setSourceInfo(rs.getString("source_info"));
      record.setSourceUrl(rs.getString("source_url"));
      DatasetColumnJSONCommand.populateFromColumnConfig(record, rs.getString("column_config"));
      record.setCategoryId(rs.getLong("category_id"));
      record.setRecordsPath(rs.getString("records_path"));
      record.setScheduledDate(rs.getTimestamp("scheduled_date"));
      record.setLastDownload(rs.getTimestamp("last_download"));
      record.setRequestConfig(rs.getString("request_config"));
      record.setProcessStatus(DB.getInt(rs, "process_status", 0));
      record.setProcessMessage(rs.getString("process_message"));
      record.setScheduleEnabled(rs.getBoolean("schedule_enabled"));
      record.setScheduleFrequency(DB.getPeriod(rs, "schedule_frequency"));
      record.setScheduleLastRun(rs.getTimestamp("schedule_last_run"));
      record.setSyncEnabled(rs.getBoolean("sync_enabled"));
      record.setSyncDate(rs.getTimestamp("sync_date"));
      record.setSyncStatus(DB.getInt(rs, "sync_status", 0));
      record.setSyncMessage(rs.getString("sync_message"));
      record.setSyncMergeType(rs.getString("sync_merge_type"));
      record.setUniqueColumnName(rs.getString("unique_column_name"));
      record.setCustomFieldList(CustomFieldListJSONCommand.populateFromJSONString(rs.getString("field_values")));
      record.setQueueStatus(DB.getInt(rs, "queue_status", 0));
      record.setQueueDate(rs.getTimestamp("queue_date"));
      record.setQueueAttempts(DB.getInt(rs, "queue_attempts", 0));
      record.setPagingUrlPath(rs.getString("paging_url_path"));
      record.setRecordCount(DB.getInt(rs, "record_count", 0));
      record.setSyncRecordCount(DB.getInt(rs, "sync_record_count", 0));
      record.setSyncAddCount(DB.getInt(rs, "sync_add_count", 0));
      record.setSyncUpdateCount(DB.getInt(rs, "sync_update_count", 0));
      record.setSyncDeleteCount(DB.getInt(rs, "sync_delete_count", 0));
      record.setFileHash(rs.getString("file_hash"));
      record.setWebPath(rs.getString("web_path"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
