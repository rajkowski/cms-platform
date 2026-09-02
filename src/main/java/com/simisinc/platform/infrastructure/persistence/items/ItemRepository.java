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

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.simisinc.platform.application.CustomFieldListJSONCommand;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.application.maps.ValidateGeoRegion;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemCategory;
import com.simisinc.platform.domain.model.items.ItemFileVersion;
import com.simisinc.platform.infrastructure.persistence.medicine.MedicineRepository;
import com.simisinc.platform.presentation.controller.DataConstants;
import com.simisinc.platform.presentation.controller.UserSession;
import com.zeroio.platform.infrastructure.persistence.items.ItemVersionRepository;

/**
 * Persists and retrieves item objects
 *
 * @author matt rajkowski
 * @created 4/18/18 10:15 PM
 */
public class ItemRepository {

  private static Log LOG = LogFactory.getLog(ItemRepository.class);

  private static String TABLE_NAME = "items";
  private static String[] PRIMARY_KEY = new String[] { "item_id" };

  public static Item save(Item record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static Item add(Item record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("collection_id", record.getCollectionId())
        .FIELD("category_id", record.getCategoryId() == -1 ? null : record.getCategoryId())
        .FIELD("dataset_id", record.getDatasetId() == -1 ? null : record.getDatasetId())
        .FIELD("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
        .FIELD("description", StringUtils.trimToNull(record.getDescription()))
        .FIELD("description_text", HtmlCommand.text(StringUtils.trimToNull(record.getDescription())))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("location_name", StringUtils.trimToNull(record.getLocation()))
        .FIELD("street", StringUtils.trimToNull(record.getStreet()))
        .FIELD("address_line_2", StringUtils.trimToNull(record.getAddressLine2()))
        .FIELD("address_line_3", StringUtils.trimToNull(record.getAddressLine3()))
        .FIELD("city", StringUtils.trimToNull(record.getCity()))
        .FIELD("state", StringUtils.trimToNull(record.getState()))
        .FIELD("country", StringUtils.trimToNull(record.getCountry()))
        .FIELD("postal_code", StringUtils.trimToNull(record.getPostalCode()))
        .FIELD("county", StringUtils.trimToNull(record.getCounty()))
        .FIELD("phone_number", StringUtils.trimToNull(record.getPhoneNumber()))
        .FIELD("email", StringUtils.trimToNull(record.getEmail()))
        .FIELD("cost", record.getCost())
        .FIELD("expected_date", record.getExpectedDate())
        .FIELD("start_date", record.getStartDate())
        .FIELD("end_date", record.getEndDate())
        .FIELD("expiration_date", record.getExpirationDate())
        .FIELD("url", StringUtils.trimToNull(record.getUrl()))
        .FIELD("url_text", StringUtils.trimToNull(record.getUrlText()))
        .FIELD("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .FIELD("barcode", StringUtils.trimToNull(record.getBarcode()))
        .FIELD("keywords", StringUtils.truncate(record.getKeywords(), 255))
        .FIELD("archived_by", record.getArchivedBy() == -1 ? null : record.getArchivedBy())
        .FIELD("archived", record.getArchived())
        .FIELD("assigned_to", record.getAssignedTo() == -1 ? null : record.getAssignedTo())
        .FIELD("assigned", record.getAssigned())
        .FIELD("approved_by", record.getApprovedBy() == -1 ? null : record.getApprovedBy())
        .FIELD("approved", record.getApproved())
        .FIELD("source", record.getSource())
        .FIELD("sync_date", record.getDatasetSyncDate())
        .FIELD("dataset_key_value", record.getDatasetKeyValue())
        .FIELD("geojson", StringUtils.trimToNull(record.getGeoJSON()), CastType.JSONB);
    if (record.getTags() != null && record.getTags().length > 0) {
      insert.FIELD("tags", JsonCommand.toJsonArray(record.getTags()), CastType.JSONB);
    }

    if (record.hasGeoPoint()) {
      insert.FIELD("latitude", record.getLatitude())
          .FIELD("longitude", record.getLongitude())
          .FIELD("geom", record.getLatitude(), record.getLongitude(), CastType.GEOM);
    }
    if (record.getCustomFieldList() != null && !record.getCustomFieldList().isEmpty()) {
      insert.FIELD("field_values", CustomFieldListJSONCommand.createJSONString(record.getCustomFieldList()), CastType.JSONB);
    }

    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      record.setId(insert.execute(connection));
      // Manage the categories
      ItemCategoryRepository.insertItemCategoryList(connection, record);
      // Manage a few related tables
      CollectionRepository.updateItemCount(connection, record.getCollectionId(), 1);
      CategoryRepository.updateItemCount(connection, record.getCategoryId(), 1);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  private static Item update(Item record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("collection_id", record.getCollectionId())
        .SET("category_id", record.getCategoryId() == -1 ? null : record.getCategoryId())
        .SET("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("summary", StringUtils.trimToNull(record.getSummary()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("description_text", HtmlCommand.text(StringUtils.trimToNull(record.getDescription())))
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("location_name", StringUtils.trimToNull(record.getLocation()))
        .SET("street", StringUtils.trimToNull(record.getStreet()))
        .SET("address_line_2", StringUtils.trimToNull(record.getAddressLine2()))
        .SET("address_line_3", StringUtils.trimToNull(record.getAddressLine3()))
        .SET("city", StringUtils.trimToNull(record.getCity()))
        .SET("state", StringUtils.trimToNull(record.getState()))
        .SET("country", StringUtils.trimToNull(record.getCountry()))
        .SET("postal_code", StringUtils.trimToNull(record.getPostalCode()))
        .SET("county", StringUtils.trimToNull(record.getCounty()))
        .SET("phone_number", StringUtils.trimToNull(record.getPhoneNumber()))
        .SET("email", StringUtils.trimToNull(record.getEmail()))
        .SET("cost", record.getCost())
        .SET("expected_date", record.getExpectedDate())
        .SET("start_date", record.getStartDate())
        .SET("end_date", record.getEndDate())
        .SET("expiration_date", record.getExpirationDate())
        .SET("url", StringUtils.trimToNull(record.getUrl()))
        .SET("url_text", StringUtils.trimToNull(record.getUrlText()))
        .SET("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .SET("barcode", StringUtils.trimToNull(record.getBarcode()))
        .SET("keywords", StringUtils.truncate(record.getKeywords(), 255))
        .SET("archived_by", record.getArchivedBy() == -1 ? null : record.getArchivedBy())
        .SET("archived", record.getArchived())
        .SET("assigned_to", record.getAssignedTo() == -1 ? null : record.getAssignedTo())
        .SET("assigned", record.getAssigned())
        .SET("approved_by", record.getApprovedBy() == -1 ? null : record.getApprovedBy())
        .SET("approved", record.getApproved())
        .SET("sync_date", record.getDatasetSyncDate());
    if (record.getTags() != null && record.getTags().length > 0) {
      update.SET("tags", JsonCommand.toJsonArray(record.getTags()), CastType.JSONB);
    } else {
      update.SET("tags", (String) null, CastType.JSONB);
    }
    if (record.hasGeoPoint()) {
      update.SET("latitude", record.getLatitude())
          .SET("longitude", record.getLongitude())
          .SET("geom", record.getLatitude(), record.getLongitude(), CastType.GEOM);
    } else {
      update.SET("latitude", (Double) null)
          .SET("longitude", (Double) null)
          .SET("geom", 0, 0, CastType.GEOM);
    }
    if (record.getCustomFieldList() != null && !record.getCustomFieldList().isEmpty()) {
      update.SET("field_values", CustomFieldListJSONCommand.createJSONString(record.getCustomFieldList()), CastType.JSONB);
    } else {
      update.SET("field_values", (String) null, CastType.JSONB);
    }
    update.WHERE("item_id = ?", record.getId());

    // Use the previous records for updates
    Item previousRecord = ItemRepository.findById(record.getId());
    List<ItemCategory> existingCategoryList = ItemCategoryRepository.findAllByItemId(record.getId());
    List<Long> newCategoryList = Arrays.asList(record.getCategoryIdList());

    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      update.execute(connection);

      // If the master categoryId does not match, then update the category id counts
      if (previousRecord.getCategoryId() != record.getCategoryId()) {
        // This category was removed
        if (previousRecord.getCategoryId() > -1) {
          CategoryRepository.updateItemCount(connection, previousRecord.getCategoryId(), -1);
        }
        // This category was added
        if (record.getCategoryId() > -1) {
          CategoryRepository.updateItemCount(connection, record.getCategoryId(), 1);
        }
      }

      // Compare the existing list and the changed list
      if (existingCategoryList != null) {
        for (ItemCategory existingCategory : existingCategoryList) {
          if (!newCategoryList.contains(existingCategory.getCategoryId())) {
            // Remove from database
            ItemCategoryRepository.removeItemCategoryId(connection, record, existingCategory.getCategoryId());
          }
        }
      }

      for (Long newCategoryId : newCategoryList) {
        boolean hasCategory = false;
        if (existingCategoryList != null) {
          for (ItemCategory existingCategory : existingCategoryList) {
            if (existingCategory.getCategoryId() == newCategoryId) {
              hasCategory = true;
              break;
            }
          }
        }
        if (!hasCategory) {
          // Add to database
          ItemCategoryRepository.insertItemCategoryId(connection, record, newCategoryId);
        }
      }

      // Finish the transaction
      transaction.commit();
      // Expire the cache
      //        CacheManager.invalidateKey(CacheManager.ITEM_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static Item updateGeoJSON(Item record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("geojson", StringUtils.trimToNull(record.getGeoJSON()), CastType.JSONB)
        .WHERE("item_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Item record) {
    try {
      // Determine the files to delete
      ItemFileVersionSpecification specification = new ItemFileVersionSpecification();
      specification.setItemId(record.getId());
      List<ItemFileVersion> fileVersionList = ItemFileVersionRepository.findAll(specification, null);
      // Delete the database entries
      try (Connection connection = DB.getConnection();
          AutoStartTransaction a = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {
        // Delete the references
        ActivityRepository.removeAll(connection, record);
        ItemCategoryRepository.removeAll(connection, record);
        MemberRoleRepository.removeAll(connection, record);
        MemberRepository.removeAll(connection, record);
        ItemRelationshipRepository.removeAll(connection, record);
        ItemFileVersionRepository.removeAll(connection, record);
        ItemFileItemRepository.removeAll(connection, record);
        ItemSubFolderRepository.removeAll(connection, record);
        ItemFolderCategoryRepository.removeAll(connection, record);
        ItemFolderGroupRepository.removeAll(connection, record);
        ItemFolderRepository.removeAll(connection, record);
        ItemVersionRepository.removeAll(connection, record);
        CollectionRepository.updateItemCount(connection, record.getCollectionId(), -1);
        CategoryRepository.updateItemCount(connection, record.getCategoryId(), -1);
        MedicineRepository.removeAll(connection, record);
        // Delete the record
        DB.DELETE().FROM(TABLE_NAME).WHERE("item_id = ?", record.getId()).execute(connection);
        // Finish transaction
        transaction.commit();
      }
      // Cleanup the files
      for (ItemFileVersion fileVersion : fileVersionList) {
        String fileServerPath = fileVersion.getFileServerPath();
        if (StringUtils.isBlank(fileServerPath)) {
          continue;
        }
        File file = FileSystemCommand.getFileServerRootPath(fileServerPath);
        if (file.exists() && file.isFile()) {
          file.delete();
        }
      }
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void approve(Item record, User user) {
    DB.UPDATE(TABLE_NAME)
        .SET("approved", new Timestamp(System.currentTimeMillis()))
        .SET("approved_by", user.getId())
        .WHERE("item_id = ?", record.getId())
        .execute();
  }

  public static void removeItemApproval(Item record, User user) {
    DB.UPDATE(TABLE_NAME)
        .SET("approved", (Timestamp) null)
        .SET("approved_by", (Long) null)
        .WHERE("item_id = ?", record.getId())
        .execute();
  }

  public static void removeAll(Connection connection, Collection record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", record.getId()).execute(connection);
  }

  private static DataResult<Item> query(ItemSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      select.LEFT_JOIN("collections")
          .ON("(items.collection_id = collections.collection_id)");
      if (specification.getId() != -1) {
        select.WHERE("item_id = ?", specification.getId());
      }
      if (specification.getExcludeId() != -1) {
        select.AND("item_id <> ?", specification.getExcludeId());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("items.unique_id = ?", specification.getUniqueId());
      }
      if (specification.getCollectionId() != -1) {
        select.AND("collections.collection_id = ?", specification.getCollectionId());
      }
      if (StringUtils.isNotBlank(specification.getBarcode())) {
        select.AND("barcode = ?", specification.getBarcode());
      }
      if (specification.getDatasetId() != -1) {
        select.AND("dataset_id = ?", specification.getDatasetId());
      }
      if (specification.getDatasetSyncTimestampThreshold() != null) {
        select.AND("sync_date < ?", specification.getDatasetSyncTimestampThreshold());
      }

      if (specification.getApprovedOnly()) {
        select.AND("approved IS NOT NULL");
      } else if (specification.getUnapprovedOnly()) {
        select.AND("approved IS NULL");
      }
      if (specification.getName() != null) {
        select.AND("LOWER(items.name) = ?", specification.getName().trim().toLowerCase());
      }
      if (specification.getMatchesName() != null) {
        String likeValue = specification.getMatchesName().trim()
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
            .replace("[",
                "![");
        select.AND("LOWER(items.name) LIKE LOWER(?) ESCAPE '!'", likeValue + "%");
      }
      if (specification.getCategoryId() > -1) {
        select.AND("EXISTS (SELECT 1 FROM item_categories WHERE item_id = items.item_id AND category_id = ?)",
            specification.getCategoryId());
      }
      if (specification.getForUserId() != DataConstants.UNDEFINED) {
        if (specification.getForUserId() == UserSession.GUEST_ID) {
          select.AND("collections.allows_guests = true");
        } else {
          select.AND(
              "(collections.allows_guests = true OR (has_allowed_groups = true AND EXISTS (SELECT 1 FROM collection_groups WHERE collection_groups.collection_id = collections.collection_id AND view_all = true AND EXISTS (SELECT 1 FROM user_groups WHERE user_groups.group_id = collection_groups.group_id AND user_id = ?)) OR EXISTS (SELECT 1 FROM members WHERE items.item_id = members.item_id AND user_id = ? AND approved IS NOT NULL)))",
              specification.getForUserId(), specification.getForUserId());
        }
      }
      if (specification.getForMemberWithUserId() != DataConstants.UNDEFINED) {
        select.AND("EXISTS (SELECT 1 FROM members WHERE items.item_id = members.item_id AND user_id = ? AND approved IS NOT NULL)",
            specification.getForMemberWithUserId());
      }
      if (StringUtils.isNotBlank(specification.getSearchLocation())) {
        constraints.setUseCount(false);
        select.AND("geom IS NOT NULL");
        String value = specification.getSearchLocation();
        if (StringUtils.isNumeric(value) && value.length() == 5) {
          select.ORDER_BY("geom <-> (SELECT geom FROM zip_codes WHERE code = ?)", value);
        } else {
          // Treat this as a city with a possible region
          String city = null;
          String region = null;
          int cityIdx = value.indexOf(",");
          if (cityIdx > -1) {
            city = value.substring(0, cityIdx).trim().toLowerCase();
            region = value.substring(cityIdx + 1).trim().toUpperCase();
          } else {
            city = value.trim().toLowerCase();
          }
          if (ValidateGeoRegion.isValidWorldCitiesRegion(region)) {
            select.ORDER_BY("geom <-> (SELECT geom FROM world_cities WHERE city = ? AND region = ? ORDER BY population DESC LIMIT 1)",
                city, region);
          } else {
            select.ORDER_BY("geom <-> (SELECT geom FROM world_cities WHERE city = ? ORDER BY population DESC LIMIT 1)", city);
          }
        }
      }
      if (StringUtils.isNotBlank(specification.getSearchName())) {
        String term = specification.getSearchName().trim().toLowerCase();
        String whereToUse = term + " OR " + term.replaceAll("\\s+", " OR ");
        String[] titleSearchWords = Arrays.stream(term.split("\\s+"))
            .map(word -> "%" + word.replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("[", "![") + "%")
            .toArray(String[]::new);
        select.SELECT(
            "ts_headline('english', items.name || ' ' || coalesce(items.keywords,'') || ' ' || coalesce(items.summary,'') || ' ' || coalesce(items.description_text,''), websearch_to_tsquery('title_stem', ?), 'StartSel=${b}, StopSel=${/b}, MaxWords=30, MinWords=15, ShortWord=3, HighlightAll=FALSE, MaxFragments=2, FragmentDelimiter=\" ... \"') AS highlight",
            term);
        select.SELECT("(ts_rank_cd(tsv, websearch_to_tsquery('title_stem', ?)) " + Arrays.stream(titleSearchWords)
            .map(word -> " + CASE WHEN LOWER(items.name) LIKE LOWER(?) ESCAPE '!' THEN 10.0 ELSE 0.0 END")
            .collect(Collectors.joining()) + ") AS rank", term, titleSearchWords);
        select.AND("tsv @@ websearch_to_tsquery('title_stem', ?)", whereToUse);
        select.ORDER_BY("rank DESC, items.modified DESC");
      }
      if (specification.getNearItemId() > DataConstants.UNDEFINED) {
        constraints.setUseCount(false);
        select.AND("geom IS NOT NULL");
        select.ORDER_BY("geom <-> (SELECT geom FROM items WHERE item_id = ?)", specification.getNearItemId());
      }
      if (specification.hasGeoPoint()) {
        constraints.setUseCount(false);
        select.AND("geom IS NOT NULL");
        if (specification.getWithinMeters() > 0) {
          select.AND("ST_DWithin(geom::geography, ST_SetSRID(ST_MakePoint(" + specification.getLatitude() + ","
              + specification.getLongitude() + "), 4326)::geography, " + specification.getWithinMeters() + ")");
        }
        select.ORDER_BY(
            "geom <-> ST_SetSRID(ST_MakePoint(" + specification.getLatitude() + "," + specification.getLongitude() + "), 4326)");
      }
      if (specification.getHasCoordinates() != DataConstants.UNDEFINED) {
        if (specification.getHasCoordinates() == DataConstants.TRUE) {
          select.AND("latitude <> 0 AND longitude <> 0");
        } else {
          select.AND("latitude = 0 AND longitude = 0");
        }
      }
      if (specification.getHasGeoJSON() != DataConstants.UNDEFINED) {
        if (specification.getHasGeoJSON() == DataConstants.TRUE) {
          select.AND("geojson IS NOT NULL");
        } else {
          select.AND("geojson IS NULL");
        }
      }
      if (specification.getCustomFieldFilters() != null) {
        for (String[] filter : specification.getCustomFieldFilters()) {
          String clause = "EXISTS (SELECT 1 FROM jsonb_array_elements(items.field_values) AS elem WHERE elem->>'name' = ? AND elem->>'value' = ?)";
          select.AND(clause, filter[0], filter[1]);
        }
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
        ConditionGroup excludeCondition = ConditionGroup.build("tags", specification.getExcludeTags(),
            ConditionGroup.NOT_ANY);
        if (excludeCondition != null) {
          select.AND(excludeCondition.sql(), (Object[]) excludeCondition.values());
        }
      }
      if (specification.getFieldInFilters() != null) {
        for (Map.Entry<String, List<String>> entry : specification.getFieldInFilters().entrySet()) {
          String fieldName = entry.getKey();
          List<String> values = entry.getValue();
          if ("tags".equals(fieldName) && !values.isEmpty()) {
            String placeholders = String.join(", ", Collections.nCopies(values.size(), "?"));
            select.AND("EXISTS (SELECT 1 FROM jsonb_array_elements_text(items.tags) AS t WHERE t IN (" + placeholders + "))",
                values.toArray(new String[0]));
          }
        }
      }
      if (specification.getModifiedAfter() != null) {
        select.AND("items.modified >= ?", specification.getModifiedAfter());
      }
      if (specification.getModifiedBefore() != null) {
        select.AND("items.modified <= ?", specification.getModifiedBefore());
      }
      if (specification.getModifiedByUserIds() != null && specification.getModifiedByUserIds().length > 0) {
        StringBuilder userCondition = new StringBuilder("items.modified_by IN (");
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
    return select.returnDataResult(ItemRepository::buildRecord);
  }

  public static Item findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", id)
        .returnRecord(ItemRepository::buildRecord);
  }

  public static Item findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("unique_id = ?", uniqueId)
        .returnRecord(ItemRepository::buildRecord);
  }

  public static Item findByIdWithinCollection(long itemId, long collectionId) {
    if (itemId == -1) {
      LOG.warn("findByIdWithinCollection item ID is -1");
      return null;
    }
    if (collectionId == -1) {
      LOG.warn("findByNameWithinCollection collection ID is -1");
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", itemId)
        .AND("collection_id = ?", collectionId)
        .returnRecord(ItemRepository::buildRecord);
  }

  public static Item findByUniqueIdWithinCollection(String uniqueId, long collectionId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    if (collectionId == -1) {
      LOG.warn("findByNameWithinCollection collection ID is -1");
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("unique_id = ?", uniqueId)
        .AND("collection_id = ?", collectionId)
        .returnRecord(ItemRepository::buildRecord);
  }

  public static Item findByNameWithinCollection(String name, long collectionId) {
    if (StringUtils.isBlank(name)) {
      LOG.warn("findByNameWithinCollection name is blank");
      return null;
    }
    if (collectionId == -1) {
      LOG.warn("findByNameWithinCollection collection ID is -1");
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(name) = ?", name.trim().toLowerCase())
        .AND("collection_id = ?", collectionId)
        .returnRecord(ItemRepository::buildRecord);
  }

  public static Item findByDatasetKeyValue(String datasetKeyValue, long datasetId) {
    if (StringUtils.isBlank(datasetKeyValue)) {
      return null;
    }
    if (datasetId == -1) {
      LOG.warn("findByDatasetKeyValue dataset ID is -1");
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("dataset_key_value = ?", datasetKeyValue)
        .AND("dataset_id = ?", datasetId)
        .returnRecord(ItemRepository::buildRecord);
  }

  public static List<Item> findAll(ItemSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("LOWER(items.name)");
    return query(specification, constraints).getRecords();
  }

  private static Item buildRecord(ResultSet rs) {
    try {
      Item record = new Item();
      record.setId(rs.getLong("item_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setUniqueId(rs.getString("unique_id"));
      record.setName(rs.getString("name"));
      record.setSummary(rs.getString("summary"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
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
      record.setPhoneNumber(rs.getString("phone_number"));
      record.setEmail(rs.getString("email"));
      record.setCost(rs.getBigDecimal("cost"));
      record.setExpectedDate(rs.getTimestamp("expected_date"));
      record.setStartDate(rs.getTimestamp("start_date"));
      record.setEndDate(rs.getTimestamp("end_date"));
      record.setExpirationDate(rs.getTimestamp("expiration_date"));
      record.setUrl(rs.getString("url"));
      record.setBarcode(rs.getString("barcode"));
      record.setKeywords(rs.getString("keywords"));
      record.setAssignedTo(DB.getLong(rs, "assigned_to", -1));
      record.setAssigned(rs.getTimestamp("assigned"));
      record.setImageUrl(rs.getString("image_url"));
      record.setCategoryId(DB.getLong(rs, "category_id", -1));
      record.setCustomFieldList(CustomFieldListJSONCommand.populateFromJSONString(rs.getString("field_values")));
      record.setArchivedBy(DB.getLong(rs, "archived_by", -1));
      record.setApprovedBy(DB.getLong(rs, "approved_by", -1));
      record.setApproved(rs.getTimestamp("approved"));
      record.setSource(rs.getString("source"));
      record.setDescription(rs.getString("description"));
      record.setUrlText(rs.getString("url_text"));
      record.setDatasetSyncDate(rs.getTimestamp("sync_date"));
      record.setDatasetKeyValue(rs.getString("dataset_key_value"));
      record.setGeoJSON(rs.getString("geojson"));
      record.setTags(JsonCommand.fromJsonArray(rs.getString("tags")));

      // Other
      if (DB.hasColumn(rs, "highlight")) {
        record.setHighlight(rs.getString("highlight"));
      }
      // Populate categoryIdList
      List<ItemCategory> categoryList = ItemCategoryRepository.findAllByItemId(record.getId());
      if (categoryList != null) {
        List<Long> categoryIdList = new ArrayList<>();
        for (ItemCategory itemCategory : categoryList) {
          categoryIdList.add(itemCategory.getCategoryId());
        }
        record.setCategoryIdList(categoryIdList.toArray(new Long[0]));
      }
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
