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
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.SubFolder;
import com.simisinc.platform.presentation.controller.DataConstants;
import com.simisinc.platform.presentation.controller.UserSession;

/**
 * Persists and retrieves sub-folder objects
 *
 * @author matt rajkowski
 * @created 8/27/19 3:26 PM
 */
public class SubFolderRepository {

  private static Log LOG = LogFactory.getLog(SubFolderRepository.class);

  private static String TABLE_NAME = "sub_folders";
  private static String[] PRIMARY_KEY = new String[] { "sub_folder_id" };

  private static DataResult<SubFolder> query(SubFolderSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("sub_folders.*").FROM(TABLE_NAME);
    if (specification != null) {
      select.LEFT_JOIN("folders").ON("sub_folders.folder_id = folders.folder_id");
      if (specification.getId() != -1) {
        select.AND("sub_folder_id = ?", specification.getId());
      }
      if (specification.getFolderId() != -1) {
        select.AND("folders.folder_id = ?", specification.getFolderId());
      }
      if (specification.getForUserId() != DataConstants.UNDEFINED) {
        if (specification.getForUserId() == UserSession.GUEST_ID) {
          select.AND("folders.allows_guests = true");
        } else {
          select.AND(
              "(allows_guests = true OR (has_allowed_groups = true AND EXISTS (SELECT 1 FROM folder_groups WHERE folder_groups.folder_id = folders.folder_id AND view_all = true AND EXISTS (SELECT 1 FROM user_groups WHERE user_groups.group_id = folder_groups.group_id AND user_id = ?))))",
              specification.getForUserId());
        }
      }
      if (specification.getHasFiles() != DataConstants.UNDEFINED) {
        if (specification.getHasFiles() == DataConstants.TRUE) {
          select.AND("sub_folders.file_count > 0");
        } else {
          select.AND("sub_folders.file_count = 0");
        }
      }
      if (specification.getYear() > 0) {
        select.AND("EXTRACT(YEAR FROM start_date) = ?", specification.getYear());
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(SubFolderRepository::buildRecord);
  }

  public static SubFolder findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("sub_folder_id = ?", id)
        .returnRecord(SubFolderRepository::buildRecord);
  }

  public static List<SubFolder> findAll() {
    return findAll(null, null);
  }

  public static List<SubFolder> findAll(SubFolderSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("start_date DESC");
    DataResult<SubFolder> result = query(specification, constraints);
    return result.getRecords();
  }

  public static List<Long> queryDistinctStartDateAsYearForFolder(Folder folder) {
    return DB.SELECT("DISTINCT(EXTRACT(YEAR FROM start_date)) AS year")
        .FROM(TABLE_NAME)
        .WHERE("folder_id = ?", folder.getId())
        .ORDER_BY("year DESC")
        .returnList(rs -> rs.getLong(1));
  }

  public static SubFolder save(SubFolder record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static SubFolder add(SubFolder record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      Insert insert = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("folder_id", record.getFolderId())
          .FIELD("name", StringUtils.trimToNull(record.getName()))
          .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
          .FIELD("created_by", record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy())
          .FIELD("end_date", record.getEndDate());
      if (record.getStartDate() != null) {
        insert.FIELD("start_date", record.getStartDate());
      }
      record.setId(insert.execute(connection));
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  private static SubFolder update(SubFolder record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      FolderRepository.updateFileCountForFileId(connection, record.getId(), -1);
      if (DB.UPDATE(TABLE_NAME)
          .SET("name", StringUtils.trimToNull(record.getName()))
          .SET("summary", StringUtils.trimToNull(record.getSummary()))
          .SET("modified_by", record.getModifiedBy())
          .SET("start_date", record.getStartDate())
          .SET("end_date", record.getEndDate())
          .WHERE("sub_folder_id = ?", record.getId())
          .execute(connection)) {
        transaction.commit();
        return record;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(SubFolder record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      FileVersionRepository.removeAll(connection, record);
      int deleteCount = FileItemRepository.removeAll(connection, record);
      // Update the folder count
      FolderRepository.updateFileCount(connection, record.getFolderId(), -deleteCount);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("sub_folder_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Folder record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("folder_id = ?", record.getId()).execute(connection);
  }

  public static boolean updateFileCount(Connection connection, long subFolderId, int value) throws SQLException {
    // Update the totals
    return DB.UPDATE(TABLE_NAME)
        .SET("file_count = file_count + " + value)
        .WHERE("sub_folder_id = ?", subFolderId)
        .execute(connection);
  }

  public static boolean updateFileCountForFileId(Connection connection, long fileId, int value) throws SQLException {
    // Update the totals
    return DB.UPDATE(TABLE_NAME)
        .SET("file_count = file_count + " + value)
        .WHERE("sub_folder_id IN (SELECT sub_folder_id FROM files WHERE file_id = ?)", fileId)
        .execute(connection);
  }

  private static SubFolder buildRecord(ResultSet rs) {
    try {
      SubFolder record = new SubFolder();
      record.setId(rs.getLong("sub_folder_id"));
      record.setFolderId(rs.getLong("folder_id"));
      record.setName(rs.getString("name"));
      record.setSummary(rs.getString("summary"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setStartDate(rs.getTimestamp("start_date"));
      record.setEndDate(rs.getTimestamp("end_date"));
      record.setFileCount(rs.getInt("file_count"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
