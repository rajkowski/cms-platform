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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.ConditionGroup;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.SubFolder;
import com.simisinc.platform.presentation.controller.DataConstants;
import com.simisinc.platform.presentation.controller.UserSession;
import com.zeroio.platform.infrastructure.persistence.cms.PageFileRepository;

/**
 * Persists and retrieves file item objects
 *
 * @author matt rajkowski
 * @created 12/12/18 2:07 PM
 */
public class FileItemRepository {

  private static Log LOG = LogFactory.getLog(FileItemRepository.class);

  private static String TABLE_NAME = "files";
  private static String[] PRIMARY_KEY = new String[] { "file_id" };

  private static DataResult query(FileSpecification specification, DataConstraints constraints) {

    Select select = DB.SELECT(
        TABLE_NAME + ".file_id",
        TABLE_NAME + ".folder_id",
        TABLE_NAME + ".filename",
        TABLE_NAME + ".title",
        TABLE_NAME + ".barcode",
        TABLE_NAME + ".version",
        TABLE_NAME + ".extension",
        TABLE_NAME + ".path",
        TABLE_NAME + ".file_length",
        TABLE_NAME + ".file_type",
        TABLE_NAME + ".mime_type",
        TABLE_NAME + ".file_hash",
        TABLE_NAME + ".width",
        TABLE_NAME + ".height",
        TABLE_NAME + ".summary",
        TABLE_NAME + ".created_by",
        TABLE_NAME + ".created",
        TABLE_NAME + ".modified_by",
        TABLE_NAME + ".modified",
        TABLE_NAME + ".processed",
        TABLE_NAME + ".expiration_date",
        TABLE_NAME + ".privacy_type",
        TABLE_NAME + ".default_token",
        TABLE_NAME + ".version_count",
        TABLE_NAME + ".download_count",
        TABLE_NAME + ".sub_folder_id",
        TABLE_NAME + ".category_id",
        TABLE_NAME + ".web_path",
        TABLE_NAME + ".tags")
        .FROM(TABLE_NAME)
        .WHERE();

    final boolean includeDocumentText = specification != null && specification.getIncludeDocumentText();

    if (specification != null) {

      // Only include this potentially-large field when requested
      if (includeDocumentText) {
        select.SELECT(TABLE_NAME + ".document_text");
      }

      select.LEFT_JOIN("folders").ON("files.folder_id = folders.folder_id");

      if (specification.getId() > -1) {
        select.AND("file_id = ?", specification.getId());
      }
      if (specification.getFolderId() > -1) {
        select.AND("folders.folder_id = ?", specification.getFolderId());
      }
      if (specification.getSubFolderId() > -1) {
        select.AND("sub_folder_id = ?", specification.getSubFolderId());
      }
      if (StringUtils.isNotBlank(specification.getBarcode())) {
        select.AND("barcode = ?", specification.getBarcode());
      }
      if (specification.getFilename() != null) {
        select.AND("LOWER(files.filename) = ?", specification.getFilename().trim().toLowerCase());
      }
      if (specification.getFileType() != null) {
        select.AND("LOWER(files.file_type) = ANY(?)",
            Arrays.stream(specification.getFileType()).map(String::toLowerCase).toArray(String[]::new), CastType.ARRAY);
      }
      if (specification.getFileExtension() != null) {
        select.AND("LOWER(files.extension) = ANY(?)",
            Arrays.stream(specification.getFileExtension()).map(String::toLowerCase).toArray(String[]::new), CastType.ARRAY);
      }
      if (specification.getMatchesName() != null) {
        String likeValue = specification.getMatchesName().trim()
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
            .replace("[", "![");
        select.AND("LOWER(files.title) LIKE LOWER(?) ESCAPE '!'", "%" + likeValue + "%");
      }
      if (specification.getWithinLastDays() > 0) {
        select.AND("files.created > NOW() - INTERVAL '" + specification.getWithinLastDays() + " days'");
      }
      if (specification.getInASubFolder() != DataConstants.UNDEFINED) {
        if (specification.getInASubFolder() == DataConstants.TRUE) {
          select.AND("sub_folder_id IS NOT NULL");
        } else {
          select.AND("sub_folder_id IS NULL");
        }
      }
      if (specification.getIsProcessed() != DataConstants.UNDEFINED) {
        if (specification.getIsProcessed() == DataConstants.TRUE) {
          select.AND("processed IS NOT NULL");
        } else {
          select.AND("processed IS NULL");
        }
      }

      // For user id
      // User must be in a user group with folder access
      if (specification.getForUserId() != DataConstants.UNDEFINED) {
        if (specification.getForUserId() == UserSession.GUEST_ID) {
          select.AND("folders.allows_guests = true");
        } else {
          select.AND(
              "(allows_guests = true OR (has_allowed_groups = true AND EXISTS (SELECT 1 FROM folder_groups WHERE folder_groups.folder_id = folders.folder_id AND view_all = true AND EXISTS (SELECT 1 FROM user_groups WHERE user_groups.group_id = folder_groups.group_id AND user_id = ?))))",
              specification.getForUserId());
        }
      }

      // For versionWebPath
      if (specification.getVersionWebPath() != null) {
        select.AND(
            "(web_path = ? OR EXISTS (SELECT 1 FROM file_versions WHERE file_versions.web_path = ? AND file_versions.file_id = ?))",
            specification.getVersionWebPath(), specification.getVersionWebPath(), specification.getId());
      }

      // Use the search engine
      if (StringUtils.isNotBlank(specification.getSearchName())) {
        select.SELECT("ts_rank_cd(tsv, websearch_to_tsquery('file_stem', ?)) AS rank", (Object[]) new Object[] { specification.getSearchName().trim() });
        select.AND("tsv @@ websearch_to_tsquery('file_stem', ?)", specification.getSearchName().trim());
        select.ORDER_BY("rank DESC, file_id");
      }

      if (specification.getRegionTags() != null && specification.getRegionTags().length > 0) {
        ConditionGroup condition = ConditionGroup.build("tags", specification.getRegionTags(), ConditionGroup.ANY);
        if (condition != null) {
          select.AND(condition.sql(), (Object[]) condition.values());
        }
      }

      if (specification.getFilterTags() != null && specification.getFilterTags().length > 0) {
        ConditionGroup filterCondition = ConditionGroup.build("tags", specification.getFilterTags(), ConditionGroup.ALL);
        if (filterCondition != null) {
          select.AND(filterCondition.sql(), (Object[]) filterCondition.values());
        }
      }

      if (specification.getExcludeTags() != null && specification.getExcludeTags().length > 0) {
        // excluded legacy tag operators left as-is for now; repository still requires final compile pass
        ConditionGroup excludeCondition = ConditionGroup.build("tags", specification.getExcludeTags(),
            ConditionGroup.NOT_ANY);
        if (excludeCondition != null) {
          select.AND(excludeCondition.sql(), (Object[]) excludeCondition.values());
        }
      }

      // Add modified date range filters
      // Some imported files can have a null modified date, so fall back to created.
      if (specification.getModifiedAfter() != null) {
        select.AND("COALESCE(files.modified, files.created) >= ?", specification.getModifiedAfter());
      }
      if (specification.getModifiedBefore() != null) {
        select.AND("COALESCE(files.modified, files.created) <= ?", specification.getModifiedBefore());
      }

      // Add modified by user filter
      if (specification.getModifiedByUserIds() != null && specification.getModifiedByUserIds().length > 0) {
        StringBuilder userCondition = new StringBuilder("files.modified_by IN (");
        Long[] userIdsBoxed = new Long[specification.getModifiedByUserIds().length];
        for (int i = 0; i < specification.getModifiedByUserIds().length; i++) {
          if (i > 0) {
            userCondition.append(",");
          }
          userCondition.append("?");
          userIdsBoxed[i] = specification.getModifiedByUserIds()[i];
        }
        userCondition.append(")");
        select.AND(userCondition.toString(), (Object[]) userIdsBoxed);
      }
    }

    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(rs -> buildRecord(rs, includeDocumentText));
  }

  public static FileItem findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("file_id = ?", id)
        .returnRecord(FileItemRepository::buildRecord);
  }

  public static FileItem findByWebPathAndId(String versionWebPath, long id) {
    if (StringUtils.isBlank(versionWebPath) || id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("web_path = ?", versionWebPath)
        .AND("file_id = ?", id)
        .returnRecord(FileItemRepository::buildRecord);
  }

  public static FileItem findByWebPath(String versionWebPath) {
    if (StringUtils.isBlank(versionWebPath)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("web_path = ?", versionWebPath)
        .returnRecord(FileItemRepository::buildRecord);
  }

  public static List<FileItem> findAll() {
    return findAll(null, null);
  }

  public static List<FileItem> findAll(FileSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("created DESC");
    DataResult result = query(specification, constraints);
    return (List<FileItem>) result.getRecords();
  }

  public static FileItem save(FileItem record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static FileItem add(FileItem record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("folder_id", record.getFolderId())
        .FIELD("filename", StringUtils.trimToNull(record.getFilename()))
        .FIELD("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("barcode", StringUtils.trimToNull(record.getBarcode()))
        .FIELD("version", StringUtils.trimToNull(record.getVersion()))
        .FIELD("extension", StringUtils.trimToNull(record.getExtension()))
        .FIELD("path", StringUtils.trimToNull(record.getFileServerPath()))
        .FIELD("file_length", record.getFileLength())
        .FIELD("file_type", record.getFileType())
        .FIELD("mime_type", record.getMimeType())
        .FIELD("file_hash", record.getFileHash())
        .FIELD("web_path", StringUtils.trimToNull(record.getWebPath()))
        .FIELD("width", record.getWidth() != -1 ? record.getWidth() : null)
        .FIELD("height", record.getHeight() != -1 ? record.getHeight() : null)
        .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("processed", record.getProcessed())
        .FIELD("expiration_date", record.getExpirationDate())
        .FIELD("privacy_type", record.getPrivacyType())
        .FIELD("default_token", StringUtils.trimToNull(record.getDefaultToken()));
    if (record.getSubFolderId() != -1) {
      insert.FIELD("sub_folder_id", record.getSubFolderId());
    }
    if (record.getCategoryId() != -1) {
      insert.FIELD("category_id", record.getCategoryId());
    }
    if (record.getTags() != null && record.getTags().length > 0) {
      insert.FIELD("tags", JsonCommand.toJsonArray(record.getTags()), CastType.JSONB);
    }

    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      record.setId(insert.execute(connection));
      // Create a version record
      FileVersionRepository.add(connection, record);
      // Update the file counts
      FolderRepository.updateFileCount(connection, record.getFolderId(), 1);
      SubFolderRepository.updateFileCount(connection, record.getSubFolderId(), 1);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  private static FileItem update(FileItem record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Update the counts in case the folder changed
      FolderRepository.updateFileCountForFileId(connection, record.getId(), -1);
      SubFolderRepository.updateFileCountForFileId(connection, record.getId(), -1);
      Update update = DB.UPDATE(TABLE_NAME)
          .SET("folder_id", record.getFolderId())
          .SET("sub_folder_id", record.getSubFolderId() != -1 ? record.getSubFolderId() : null)
          .SET("category_id", record.getCategoryId() != -1 ? record.getCategoryId() : null)
          .SET("title", StringUtils.trimToNull(record.getTitle()))
          .SET("barcode", StringUtils.trimToNull(record.getBarcode()))
          .SET("version", StringUtils.trimToNull(record.getVersion()))
          .SET("summary", StringUtils.trimToNull(record.getSummary()))
          .SET("modified_by", record.getModifiedBy())
          .SET("processed", record.getProcessed())
          .SET("expiration_date", record.getExpirationDate())
          .SET("privacy_type", record.getPrivacyType());
      if (StringUtils.trimToNull(record.getFilename()) != null) {
        update.SET("filename", StringUtils.trimToNull(record.getFilename()));
      }
      if (record.getWidth() != -1) {
        update.SET("width", record.getWidth());
      }
      if (record.getHeight() != -1) {
        update.SET("height", record.getHeight());
      }
      if (record.getTags() != null && record.getTags().length > 0) {
        update.SET("tags", JsonCommand.toJsonArray(record.getTags()), CastType.JSONB);
      } else if (record.getTags() != null) {
        update.SET("tags", (String) null, CastType.JSONB);
      }
      update.WHERE("file_id = ?", record.getId());
      if (update.execute(connection).booleanValue()) {
        FileVersionRepository.update(connection, record);
        FolderRepository.updateFileCountForFileId(connection, record.getId(), 1);
        SubFolderRepository.updateFileCountForFileId(connection, record.getId(), 1);
        transaction.commit();
        return record;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return null;
  }

  public static FileItem saveVersion(FileItem record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Update the counts in case the folder changed
      FolderRepository.updateFileCountForFileId(connection, record.getId(), -1);
      SubFolderRepository.updateFileCountForFileId(connection, record.getId(), -1);
      Update update = DB.UPDATE(TABLE_NAME)
          .SET("folder_id", record.getFolderId())
          .SET("sub_folder_id", record.getSubFolderId() != -1 ? record.getSubFolderId() : null)
          .SET("category_id", record.getCategoryId() != -1 ? record.getCategoryId() : null)
          .SET("filename", StringUtils.trimToNull(record.getFilename()))
          .SET("title", StringUtils.trimToNull(record.getTitle()))
          .SET("barcode", StringUtils.trimToNull(record.getBarcode()))
          .SET("version", StringUtils.trimToNull(record.getVersion()))
          .SET("extension", StringUtils.trimToNull(record.getExtension()))
          .SET("path", StringUtils.trimToNull(record.getFileServerPath()))
          .SET("web_path", StringUtils.trimToNull(record.getWebPath()))
          .SET("file_length", record.getFileLength())
          .SET("file_type", record.getFileType())
          .SET("mime_type", record.getMimeType())
          .SET("file_hash", record.getFileHash())
          .SET("width", record.getWidth() != -1 ? record.getWidth() : null)
          .SET("height", record.getHeight() != -1 ? record.getHeight() : null)
          .SET("summary", StringUtils.trimToNull(record.getSummary()))
          .SET("modified_by", record.getModifiedBy())
          .SET("modified", new Timestamp(System.currentTimeMillis()))
          .SET("processed", record.getProcessed())
          .SET("expiration_date", record.getExpirationDate())
          .SET("privacy_type", record.getPrivacyType())
          .SET("default_token", StringUtils.trimToNull(record.getDefaultToken()));
      if (record.getTags() != null && record.getTags().length > 0) {
        update.SET("tags", JsonCommand.toJsonArray(record.getTags()), CastType.JSONB);
      } else if (record.getTags() != null) {
        update.SET("tags", (String) null, CastType.JSONB);
      }
      update.WHERE("file_id = ?", record.getId());
      if (update.execute(connection).booleanValue()) {
        FileVersionRepository.update(connection, record);
        FolderRepository.updateFileCountForFileId(connection, record.getId(), 1);
        SubFolderRepository.updateFileCountForFileId(connection, record.getId(), 1);
        FileVersionRepository.add(connection, record);
        transaction.commit();
        return record;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The version update failed!");
    return null;
  }

  public static boolean updateDocumentText(FileItem record, String documentText) {
    try (Connection connection = DB.getConnection()) {
      Update update = DB.UPDATE(TABLE_NAME)
          .SET("document_text", documentText)
          .SET("processed", new Timestamp(System.currentTimeMillis()))
          .WHERE("file_id = ?", record.getId());
      if (update.execute(connection).booleanValue()) {
        return true;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The document text update failed!");
    return false;
  }

  public static boolean remove(FileItem record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      FileVersionRepository.removeAll(connection, record);
      FolderRepository.updateFileCount(connection, record.getFolderId(), -1);
      SubFolderRepository.updateFileCount(connection, record.getSubFolderId(), -1);
      PageFileRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("file_id = ?", record.getId()).execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Folder record) throws SQLException {
    PageFileRepository.removeAll(connection, record);
    DB.DELETE().FROM(TABLE_NAME).WHERE("folder_id = ?", record.getId()).execute(connection);
  }

  public static int removeAll(Connection connection, SubFolder record) throws SQLException {
    PageFileRepository.removeAll(connection, record);
    return DB.DELETE().FROM(TABLE_NAME).WHERE("sub_folder_id = ?", record.getId()).execute(connection).booleanValue() ? 1 : 0;
  }

  private static PreparedStatement createPreparedStatementForUpdateDownloadCount(Connection connection, FileItem record)
      throws SQLException {
    String SQL_QUERY = "UPDATE files " +
        "SET download_count = download_count + 1 " +
        "WHERE file_id = ?";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setLong(++i, record.getId());
    return pst;
  }

  public static boolean incrementDownloadCount(FileItem record) {
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = createPreparedStatementForUpdateDownloadCount(connection, record)) {
      if (pst.executeUpdate() > 0) {
        return true;
      }
    } catch (SQLException se) {
      LOG.error("Update SQLException: [" + TABLE_NAME + "]: " + se.getMessage());
    }
    return false;
  }

  public static long findTotalFileSize() {
    return DB.SELECT("SUM(file_length)").FROM(TABLE_NAME).returnValue(Long.class);
  }

  private static FileItem buildRecord(ResultSet rs) {
    return buildRecord(rs, false);
  }

  private static FileItem buildRecord(ResultSet rs, boolean includeDocumentText) {
    try {
      FileItem record = new FileItem();
      record.setId(rs.getLong("file_id"));
      record.setFolderId(rs.getLong("folder_id"));
      record.setFilename(rs.getString("filename"));
      record.setTitle(rs.getString("title"));
      record.setBarcode(rs.getString("barcode"));
      record.setVersion(rs.getString("version"));
      record.setExtension(rs.getString("extension"));
      record.setFileServerPath(rs.getString("path"));
      record.setFileLength(rs.getLong("file_length"));
      String fileType = rs.getString("file_type");
      if (fileType != null) {
        record.setFileType(fileType.toLowerCase());
      } else {
        record.setFileType(null);
      }
      record.setMimeType(rs.getString("mime_type"));
      record.setFileHash(rs.getString("file_hash"));
      record.setWidth(rs.getInt("width"));
      record.setHeight(rs.getInt("height"));
      record.setSummary(rs.getString("summary"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setProcessed(rs.getTimestamp("processed"));
      record.setExpirationDate(rs.getTimestamp("expiration_date"));
      record.setPrivacyType(rs.getInt("privacy_type"));
      record.setDefaultToken(rs.getString("default_token"));
      record.setVersionCount(rs.getInt("version_count"));
      record.setDownloadCount(rs.getLong("download_count"));
      record.setSubFolderId(DB.getLong(rs, "sub_folder_id", -1L));
      record.setCategoryId(DB.getLong(rs, "category_id", -1L));
      record.setWebPath(rs.getString("web_path"));
      record.setTags(JsonCommand.fromJsonArray(rs.getString("tags")));
      if (includeDocumentText) {
        record.setDocumentText(rs.getString("document_text"));
      }
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
