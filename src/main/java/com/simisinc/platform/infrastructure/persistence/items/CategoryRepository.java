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
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;

/**
 * Persists and retrieves category objects
 *
 * @author matt rajkowski
 * @created 4/18/18 10:15 PM
 */
public class CategoryRepository {

  private static final String[] PRIMARY_KEY = new String[] { "category_id" };
  private static String TABLE_NAME = "categories";

  private static Log LOG = LogFactory.getLog(CategoryRepository.class);

  public static Category findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("category_id = ?", id)
        .returnRecord(CategoryRepository::buildRecord);
  }

  public static Category findByUniqueIdWithinCollection(String uniqueId, long collectionId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id = ?", collectionId)
        .AND("unique_id = ?", uniqueId)
        .returnRecord(CategoryRepository::buildRecord);
  }

  public static Category findByNameWithinCollection(String name, long collectionId) {
    if (collectionId == -1) {
      return null;
    }
    if (StringUtils.isBlank(name)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id = ?", collectionId)
        .AND("LOWER(name) = ?", name.trim().toLowerCase())
        .returnRecord(CategoryRepository::buildRecord);
  }

  public static List<Category> findAllByItemId(long itemId) {
    if (itemId == -1) {
      return null;
    }
    DataResult<Category> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("EXISTS (SELECT 1 FROM item_categories WHERE category_id = categories.category_id AND item_id = ?)", itemId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("name").setUseCount(false))
        .returnDataResult(CategoryRepository::buildRecord);
    return result.getRecords();
  }

  public static List<Category> findAllByCollectionId(long collectionId) {
    return findAllByCollectionId(collectionId, false);
  }

  public static List<Category> findAllByCollectionId(long collectionId, boolean basedOnItems) {
    if (collectionId == -1) {
      return null;
    }
    Select select = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id = ?", collectionId);
    if (basedOnItems) {
      select.AND("item_count > 0");
    }
    DataResult<Category> result = select
        .WITH(new DataConstraints().setDefaultColumnToSortBy("name").setUseCount(false))
        .returnDataResult(CategoryRepository::buildRecord);
    return result.getRecords();
  }

  public static List<Category> findAll() {
    DataResult<Category> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("name"))
        .returnDataResult(CategoryRepository::buildRecord);
    if (result.hasRecords()) {
      return result.getRecords();
    }
    return null;
  }

  public static Category save(Category record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return insert(record);
  }

  public static boolean remove(Category record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      ItemCategoryRepository.removeAll(connection, record);
      // Update pointers
      CollectionRepository.updateCategoryCount(connection, record.getCollectionId(), -1);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("category_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Collection record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", record.getId()).execute(connection);
  }

  private static Category insert(Category record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("collection_id", record.getCollectionId())
        .FIELD("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("description", StringUtils.trimToNull(record.getDescription()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("icon", StringUtils.trimToNull(record.getIcon()));
    if (record.getHeaderTextColor() != null) {
      insert.FIELD("header_text_color", record.getHeaderTextColor());
    }
    if (record.getHeaderBgColor() != null) {
      insert.FIELD("header_bg_color", record.getHeaderBgColor());
    }
    insert.FIELD("item_url_text", StringUtils.trimToNull(record.getItemUrlText()));
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Insert the record
      record.setId(insert.execute(connection));
      // Update the pointer
      CollectionRepository.updateCategoryCount(connection, record.getCollectionId(), 1);
      // Finish transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return null;
  }

  private static Category update(Category record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("icon", StringUtils.trimToNull(record.getIcon()))
        .SET("header_text_color", StringUtils.trimToNull(record.getHeaderTextColor()))
        .SET("header_bg_color", StringUtils.trimToNull(record.getHeaderBgColor()))
        .SET("item_url_text", StringUtils.trimToNull(record.getItemUrlText()))
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("category_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static PreparedStatement createPreparedStatementForItemCount(Connection connection, long categoryId,
      int value) throws SQLException {
    String SQL_QUERY = "UPDATE categories " +
        "SET item_count = item_count + ? " +
        "WHERE category_id = ?";
    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    pst.setInt(++i, value);
    pst.setLong(++i, categoryId);
    return pst;
  }

  public static boolean updateItemCount(Connection connection, long categoryId, int value) {
    // Increment the count
    try (PreparedStatement pst = createPreparedStatementForItemCount(connection, categoryId, value)) {
      return pst.execute();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return false;
  }

  private static Category buildRecord(ResultSet rs) {
    try {
      Category record = new Category();
      record.setId(rs.getLong("category_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setName(rs.getString("name"));
      record.setDescription(rs.getString("description"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      record.setItemCount(rs.getLong("item_count"));
      record.setIcon(rs.getString("icon"));
      record.setHeaderTextColor(rs.getString("header_text_color"));
      record.setHeaderBgColor(rs.getString("header_bg_color"));
      record.setItemUrlText(rs.getString("item_url_text"));
      record.setUniqueId(rs.getString("unique_id"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
