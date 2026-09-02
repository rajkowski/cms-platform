/*
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.CollectionTableColumnsJSONCommand;
import com.simisinc.platform.application.CustomFieldListJSONCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.CollectionGroup;
import com.simisinc.platform.domain.model.items.PrivacyType;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.presentation.controller.DataConstants;
import com.simisinc.platform.presentation.controller.UserSession;

/**
 * Persists and retrieves collection objects
 *
 * @author matt rajkowski
 * @created 4/18/18 10:15 PM
 */
public class CollectionRepository {

  private static Log LOG = LogFactory.getLog(CollectionRepository.class);

  private static String TABLE_NAME = "collections";
  private static String[] PRIMARY_KEY = new String[] { "collection_id" };

  public static Collection save(Collection record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static Collection add(Collection record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("description", StringUtils.trimToNull(record.getDescription()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("allows_guests", PrivacyType.isPublic(record.getGuestPrivacyType()))
        .FIELD("guest_privacy_type", record.getGuestPrivacyType())
        .FIELD("has_allowed_groups",
            record.getCollectionGroupList() != null && !record.getCollectionGroupList().isEmpty())
        .FIELD("listings_link", StringUtils.trimToNull(record.getListingsLink()))
        .FIELD("icon", StringUtils.trimToNull(record.getIcon()))
        .FIELD("show_listings_link", record.getShowListingsLink())
        .FIELD("show_search", record.getShowSearch())
        .FIELD("item_url_text", StringUtils.trimToNull(record.getItemUrlText()));

    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      record.setId(insert.execute(connection));
      // Manage the access groups
      if (record.getCollectionGroupList() != null && !record.getCollectionGroupList().isEmpty()) {
        CollectionGroupRepository.insertCollectionGroupList(connection, record);
      }
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  private static Collection update(Collection record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("allows_guests", PrivacyType.isPublic(record.getGuestPrivacyType()))
        .SET("guest_privacy_type", record.getGuestPrivacyType())
        .SET("has_allowed_groups",
            record.getCollectionGroupList() != null && !record.getCollectionGroupList().isEmpty())
        .SET("listings_link", StringUtils.trimToNull(record.getListingsLink()))
        .SET("icon", StringUtils.trimToNull(record.getIcon()))
        .SET("show_listings_link", record.getShowListingsLink())
        .SET("show_search", record.getShowSearch())
        .SET("item_url_text", StringUtils.trimToNull(record.getItemUrlText()))
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("collection_id = ?", record.getId());
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      update.execute(connection);
      // Manage the access groups
      CollectionGroupRepository.removeAll(connection, record);
      CollectionGroupRepository.insertCollectionGroupList(connection, record);
      // Finish the transaction
      transaction.commit();
      // Expire the cache
      CacheManager.invalidateKey(CacheManager.COLLECTION_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static Collection updateCustomFields(Collection record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (record.getCustomFieldList() != null && !record.getCustomFieldList().isEmpty()) {
      update.SET("field_values", CustomFieldListJSONCommand.createJSONString(record.getCustomFieldList()), CastType.JSONB);
    } else {
      update.SET("field_values", (String) null, CastType.JSONB);
    }
    update.WHERE("collection_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      // Expire the cache
      CacheManager.invalidateKey(CacheManager.COLLECTION_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static Collection updateTableColumns(Collection record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (record.getTableColumnsList() != null && !record.getTableColumnsList().isEmpty()) {
      update.SET("table_columns", CollectionTableColumnsJSONCommand.createJSONString(record.getTableColumnsList()), CastType.JSONB);
    } else {
      update.SET("table_columns", (String) null, CastType.JSONB);
    }
    update.WHERE("collection_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      // Expire the cache
      CacheManager.invalidateKey(CacheManager.COLLECTION_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static Collection updateTheme(Collection record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("header_text_color", record.getHeaderTextColor())
        .SET("header_bg_color", record.getHeaderBgColor())
        .SET("menu_text_color", record.getMenuTextColor())
        .SET("menu_bg_color", record.getMenuBgColor())
        .SET("menu_border_color", record.getMenuBorderColor())
        .SET("menu_active_text_color", record.getMenuActiveTextColor())
        .SET("menu_active_bg_color", record.getMenuActiveBgColor())
        .SET("menu_active_border_color", record.getMenuActiveBorderColor())
        .SET("menu_hover_text_color", record.getMenuHoverTextColor())
        .SET("menu_hover_bg_color", record.getMenuHoverBgColor())
        .SET("menu_hover_border_color", record.getMenuHoverBorderColor())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("collection_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      // Expire the cache
      CacheManager.invalidateKey(CacheManager.COLLECTION_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  // Remove
  public static boolean remove(Collection record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      // @note the Item, and its mapping to a Category, is currently not cleaned up until a business decision is made
      // ActivityRepository.removeAll(connection, record);
      // ItemCategoryRepository.removeAll(connection, record);
      // ItemRepository.removeAll(connection, record);
      CollectionRoleRepository.removeAll(connection, record);
      CollectionGroupRepository.removeAll(connection, record);
      CollectionTabRepository.removeAll(connection, record);
      CategoryRepository.removeAll(connection, record);
      CollectionRelationshipRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      // Invalidate the cache
      CacheManager.invalidateKey(CacheManager.COLLECTION_UNIQUE_ID_CACHE, record.getUniqueId());
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The delete failed!");
    return false;
  }

  private static DataResult<Collection> query(CollectionSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() != -1) {
        select.AND("collection_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("unique_id = ?", specification.getUniqueId());
      }
      if (specification.getName() != null) {
        select.AND("LOWER(name) = ?", specification.getName().toLowerCase());
      }
      if (specification.getForUserId() != DataConstants.UNDEFINED) {
        if (specification.getForUserId() == UserSession.GUEST_ID) {
          select.AND("allows_guests = true");
        } else {
          select.AND(
              "(allows_guests = true " +
                  "OR (has_allowed_groups = true " +
                  "AND EXISTS (SELECT 1 FROM collection_groups WHERE collection_id = collections.collection_id " +
                  "AND EXISTS (SELECT 1 FROM user_groups WHERE group_id = collection_groups.group_id AND user_id = ?))))",
              specification.getForUserId());
        }
      }
    }
    return select.WITH(constraints).returnDataResult(CollectionRepository::buildRecord);
  }

  public static Collection findById(long id) {
    if (id == -1) {
      return null;
    }
    Collection collection = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id = ?", id)
        .returnRecord(CollectionRepository::buildRecord);
    populateRelatedData(collection);
    return collection;
  }

  public static Collection findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    Collection collection = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("unique_id = ?", uniqueId)
        .returnRecord(CollectionRepository::buildRecord);
    populateRelatedData(collection);
    return collection;
  }

  public static Collection findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    Collection collection = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(name) = ?", name.toLowerCase())
        .returnRecord(CollectionRepository::buildRecord);
    populateRelatedData(collection);
    return collection;
  }

  public static List<Collection> findAll() {
    return findAll(null, null);
  }

  public static List<Collection> findAll(CollectionSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints().setUseCount(false);
    }
    constraints.setDefaultColumnToSortBy("name");
    List<Collection> collectionList = query(specification, constraints).getRecords();
    for (Collection collection : collectionList) {
      populateRelatedData(collection);
    }
    return collectionList;
  }

  private static void populateRelatedData(Collection collection) {
    if (collection == null) {
      return;
    }
    if (collection.doAllowedGroupsCheck()) {
      List<CollectionGroup> allowedGroupList = CollectionGroupRepository.findAllByCollectionId(collection.getId());
      collection.setCollectionGroupList(allowedGroupList);
    }
  }

  public static boolean updateCategoryCount(Connection connection, long collectionId, int value) throws SQLException {
    // Increment the count
    try (PreparedStatement pst = createPreparedStatementForCategoryCount(connection, collectionId, value)) {
      return pst.execute();
    }
  }

  private static PreparedStatement createPreparedStatementForCategoryCount(Connection connection, long collectionId,
      int value) throws SQLException {
    String SQL_QUERY = "UPDATE collections " +
        "SET category_count = category_count + ? " +
        "WHERE collection_id = ?";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setInt(++i, value);
    pst.setLong(++i, collectionId);
    return pst;
  }

  public static boolean updateItemCount(Connection connection, long collectionId, int value) {
    // Increment the count
    try (PreparedStatement pst = createPreparedStatementForItemCount(connection, collectionId, value)) {
      return pst.execute();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    } finally {
      // Expire the cache
      CacheManager.invalidateKey(CacheManager.COLLECTION_UNIQUE_ID_CACHE,
          LoadCollectionCommand.loadCollectionById(collectionId).getUniqueId());
    }
    LOG.error("The update failed!");
    return false;
  }

  private static PreparedStatement createPreparedStatementForItemCount(Connection connection, long collectionId,
      int value) throws SQLException {
    String SQL_QUERY = "UPDATE collections " +
        "SET item_count = item_count + ? " +
        "WHERE collection_id = ?";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setInt(++i, value);
    pst.setLong(++i, collectionId);
    return pst;
  }

  public static Collection buildRecord(ResultSet rs) {
    try {
      Collection record = new Collection();
      record.setId(rs.getLong("collection_id"));
      record.setName(rs.getString("name"));
      record.setUniqueId(rs.getString("unique_id"));
      record.setDescription(rs.getString("description"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      record.setCategoryCount(rs.getLong("category_count"));
      record.setItemCount(rs.getLong("item_count"));
      record.setHasAllowedGroups(rs.getBoolean("has_allowed_groups"));
      record.setAllowsGuests(rs.getBoolean("allows_guests"));
      record.setGuestPrivacyType(rs.getInt("guest_privacy_type"));
      record.setListingsLink(rs.getString("listings_link"));
      record.setImageUrl(rs.getString("image_url"));
      record.setHeaderXml(rs.getString("header_xml"));
      record.setIcon(rs.getString("icon"));
      record.setShowListingsLink(rs.getBoolean("show_listings_link"));
      record.setShowSearch(rs.getBoolean("show_search"));
      record.setHeaderTextColor(rs.getString("header_text_color"));
      record.setHeaderBgColor(rs.getString("header_bg_color"));
      record.setMenuTextColor(rs.getString("menu_text_color"));
      record.setMenuBgColor(rs.getString("menu_bg_color"));
      record.setMenuBorderColor(rs.getString("menu_border_color"));
      record.setMenuActiveTextColor(rs.getString("menu_active_text_color"));
      record.setMenuActiveBgColor(rs.getString("menu_active_bg_color"));
      record.setMenuActiveBorderColor(rs.getString("menu_active_border_color"));
      record.setMenuHoverTextColor(rs.getString("menu_hover_text_color"));
      record.setMenuHoverBgColor(rs.getString("menu_hover_bg_color"));
      record.setMenuHoverBorderColor(rs.getString("menu_hover_border_color"));
      record.setCustomFieldList(CustomFieldListJSONCommand.populateFromJSONString(rs.getString("field_values")));
      record.setItemUrlText(rs.getString("item_url_text"));
      record.setTableColumnsList(CollectionTableColumnsJSONCommand.populateFromJSONString(rs.getString("table_columns")));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
