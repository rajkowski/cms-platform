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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.FolderCategory;
import com.simisinc.platform.domain.model.cms.FolderGroup;
import com.simisinc.platform.domain.model.items.PrivacyType;
import com.simisinc.platform.presentation.controller.DataConstants;
import com.simisinc.platform.presentation.controller.UserSession;

/**
 * Persists and retrieves folder objects
 *
 * @author matt rajkowski
 * @created 4/18/18 10:15 PM
 */
public class FolderRepository {

  private static Log LOG = LogFactory.getLog(FolderRepository.class);

  private static String TABLE_NAME = "folders";
  private static String[] PRIMARY_KEY = new String[] { "folder_id" };

  public static Folder save(Folder record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static Folder add(Folder record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      Insert insert = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("folder_unique_id", StringUtils.trimToNull(record.getUniqueId()))
          .FIELD("name", StringUtils.trimToNull(record.getName()))
          .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
          .FIELD("created_by", record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy())
          .FIELD("allows_guests", record.getGuestPrivacyType() != PrivacyType.UNDEFINED)
          .FIELD("guest_privacy_type", record.getGuestPrivacyType());
      if (record.getPrivacyTypes() != null) {
        insert.FIELD("privacy_types", String.join(", ", record.getPrivacyTypes()));
      }
      insert.FIELD("has_allowed_groups", record.getFolderGroupList() != null && !record.getFolderGroupList().isEmpty())
          .FIELD("has_categories", record.getFolderCategoryList() != null && !record.getFolderCategoryList().isEmpty());
      record.setId(insert.execute(connection));
      if (record.getFolderGroupList() != null && !record.getFolderGroupList().isEmpty()) {
        FolderGroupRepository.insertFolderGroupList(connection, record);
      }
      if (record.getFolderCategoryList() != null && !record.getFolderCategoryList().isEmpty()) {
        FolderCategoryRepository.insertFolderCategoryList(connection, record);
      }
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  private static Folder update(Folder record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      Update update = DB.UPDATE(TABLE_NAME)
          .SET("name", StringUtils.trimToNull(record.getName()))
          .SET("folder_unique_id", StringUtils.trimToNull(record.getUniqueId()))
          .SET("summary", StringUtils.trimToNull(record.getSummary()))
          .SET("allows_guests", record.getGuestPrivacyType() != PrivacyType.UNDEFINED)
          .SET("guest_privacy_type", record.getGuestPrivacyType())
          .SET("modified_by", record.getModifiedBy())
          .SET("modified", new Timestamp(System.currentTimeMillis()));
      if (record.getPrivacyTypes() != null) {
        update.SET("privacy_types", String.join(", ", record.getPrivacyTypes()));
      } else {
        update.SET("privacy_types", (String) null);
      }
      update.SET("has_allowed_groups", record.getFolderGroupList() != null && !record.getFolderGroupList().isEmpty())
          .SET("has_categories", record.getFolderCategoryList() != null && !record.getFolderCategoryList().isEmpty());
      update.WHERE("folder_id = ?", record.getId()).execute(connection);
      FolderGroupRepository.removeAll(connection, record);
      FolderGroupRepository.insertFolderGroupList(connection, record);
      FolderCategoryRepository.updateFolderCategoryList(connection, record);
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  // Remove
  public static boolean remove(Folder record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      FileVersionRepository.removeAll(connection, record);
      FileItemRepository.removeAll(connection, record);
      SubFolderRepository.removeAll(connection, record);
      FolderGroupRepository.removeAll(connection, record);
      FolderCategoryRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("folder_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      // Invalidate the cache
      //        CacheManager.invalidateKey(CacheManager.COLLECTION_UNIQUE_ID_CACHE, record.getUniqueId());
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The delete failed!");
    return false;
  }

  private static DataResult<Folder> query(FolderSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() != -1) {
        select.AND("folder_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("folder_unique_id = ?", specification.getUniqueId());
      }
      if (specification.getName() != null) {
        select.AND("LOWER(name) = ?", specification.getName().toLowerCase());
      }
      if (specification.getForUserId() != DataConstants.UNDEFINED) {
        if (specification.getForUserId() == UserSession.GUEST_ID) {
          select.AND("allows_guests = true");
        } else {
          select.AND(
              "(allows_guests = true OR (has_allowed_groups = true AND EXISTS (SELECT 1 FROM folder_groups WHERE folder_id = folders.folder_id AND EXISTS (SELECT 1 FROM user_groups WHERE group_id = folder_groups.group_id AND user_id = ?))))",
              specification.getForUserId());
        }
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(FolderRepository::buildRecord);
  }

  public static Folder findById(long id) {
    if (id == -1) {
      return null;
    }
    Folder folder = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("folder_id = ?", id)
        .returnRecord(FolderRepository::buildRecord);
    populateRelatedData(folder);
    return folder;
  }

  public static Folder findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    Folder folder = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("folder_unique_id = ?", uniqueId)
        .returnRecord(FolderRepository::buildRecord);
    populateRelatedData(folder);
    return folder;
  }

  public static Folder findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    Folder folder = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(name) = ?", name.toLowerCase())
        .returnRecord(FolderRepository::buildRecord);
    populateRelatedData(folder);
    return folder;
  }

  public static List<Folder> findAll() {
    return findAll(null, null);
  }

  public static List<Folder> findAll(FolderSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints().setUseCount(false);
    }
    constraints.setDefaultColumnToSortBy("name");
    DataResult<Folder> result = query(specification, constraints);
    List<Folder> folderList = result.getRecords();
    for (Folder folder : folderList) {
      populateRelatedData(folder);
    }
    return folderList;
  }

  private static void populateRelatedData(Folder folder) {
    if (folder == null) {
      return;
    }
    if (folder.doAllowedGroupsCheck()) {
      List<FolderGroup> allowedGroupList = FolderGroupRepository.findAllByFolderId(folder.getId());
      folder.setFolderGroupList(allowedGroupList);
    }
    if (folder.doCategoriesCheck()) {
      List<FolderCategory> folderCategoryList = FolderCategoryRepository.findAllByFolderId(folder.getId());
      folder.setFolderCategoryList(folderCategoryList);
    }
  }

  /**
   * Re-computes and saves the privacy_types and has_allowed_groups pointer fields
   * on the folder from the current set of folder_groups records.
   * Call this whenever a FolderGroup is added, updated, or removed individually.
   */
  public static void updateGroupPointers(long folderId) {
    if (folderId == -1) {
      return;
    }
    List<FolderGroup> groups = FolderGroupRepository.findAllByFolderId(folderId);
    boolean hasGroups = (groups != null && !groups.isEmpty());
    String privacyTypesValue = null;
    if (hasGroups && groups != null) {
      Set<Integer> typeSet = new LinkedHashSet<>();
      for (FolderGroup fg : groups) {
        typeSet.add(fg.getPrivacyType());
      }
      List<String> typeNames = new ArrayList<>();
      for (int pt : typeSet) {
        typeNames.add(String.valueOf(pt));
      }
      privacyTypesValue = String.join(", ", typeNames);
    }
    DB.UPDATE(TABLE_NAME)
        .SET("has_allowed_groups", hasGroups)
        .SET("privacy_types", privacyTypesValue)
        .WHERE("folder_id = ?", folderId)
        .execute();
  }

  public static boolean updateGuestAccess(long folderId, int guestPrivacyType) {
    if (folderId == -1) {
      return false;
    }
    try {
      DB.UPDATE(TABLE_NAME)
          .SET("allows_guests", guestPrivacyType != PrivacyType.UNDEFINED)
          .SET("guest_privacy_type", guestPrivacyType)
          .WHERE("folder_id = ?", folderId)
          .execute();
      return true;
    } catch (Exception e) {
      LOG.error("updateGuestAccess: " + e.getMessage());
      return false;
    }
  }

  public static boolean updateFileCount(Connection connection, long folderId, int value) {
    // Increment the count
    try (PreparedStatement pst = createPreparedStatementForItemCount(connection, folderId, value)) {
      return pst.execute();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return false;
  }

  private static PreparedStatement createPreparedStatementForItemCount(Connection connection, long folderId, int value)
      throws SQLException {
    String SQL_QUERY = "UPDATE folders " +
        "SET file_count = file_count + ? " +
        "WHERE folder_id = ?";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setInt(++i, value);
    pst.setLong(++i, folderId);
    return pst;
  }

  public static boolean updateFileCountForFileId(Connection connection, long fileId, int value) {
    // Increment the count
    try (PreparedStatement pst = createPreparedStatementForItemCountForFileId(connection, fileId, value)) {
      return pst.execute();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return false;
  }

  private static PreparedStatement createPreparedStatementForItemCountForFileId(Connection connection, long fileId, int value)
      throws SQLException {
    String SQL_QUERY = "UPDATE folders " +
        "SET file_count = file_count + ? " +
        "WHERE folder_id IN (SELECT folder_id FROM files WHERE file_id = ?) ";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setInt(++i, value);
    pst.setLong(++i, fileId);
    return pst;
  }

  public static Folder buildRecord(ResultSet rs) {
    try {
      Folder record = new Folder();
      record.setId(rs.getLong("folder_id"));
      record.setUniqueId(rs.getString("folder_unique_id"));
      record.setName(rs.getString("name"));
      record.setSummary(rs.getString("summary"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setFileCount(rs.getInt("file_count"));
      String privacyTypes = rs.getString("privacy_types");
      if (privacyTypes != null) {
        record.setPrivacyTypes(privacyTypes.split("\\s*,\\s*"));
      }
      record.setHasAllowedGroups(rs.getBoolean("has_allowed_groups"));
      record.setAllowsGuests(rs.getBoolean("allows_guests"));
      record.setGuestPrivacyType(rs.getInt("guest_privacy_type"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setHasCategories(rs.getBoolean("has_categories"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
