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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.CollectionTab;

/**
 * Persists and retrieves collection tab objects
 *
 * @author matt rajkowski
 * @created 4/13/21 12:00 PM
 */
public class CollectionTabRepository {

  private static Log LOG = LogFactory.getLog(CollectionTabRepository.class);

  private static String TABLE_NAME = "collection_tabs";
  private static String[] PRIMARY_KEY = new String[] { "tab_id" };

  public static List<CollectionTab> findAllByCollectionId(long collectionId) {
    if (collectionId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("collection_id = ?", collectionId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("tab_order,name").setUseCount(false))
        .returnDataResult(CollectionTabRepository::buildRecord).getRecords();
  }

  public static boolean save(List<CollectionTab> collectionTabList) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      for (CollectionTab tab : collectionTabList) {
        save(connection, tab);
      }
      // Finish the transaction
      transaction.commit();
      return true;
    } catch (Exception e) {
      LOG.error("Tabs not saved", e);
    }
    return false;
  }

  private static CollectionTab save(Connection connection, CollectionTab record) throws SQLException {
    // Check for existing record
    if (record.getId() > -1) {
      if (StringUtils.isBlank(record.getName())) {
        // No name, so remove it
        remove(connection, record);
        return null;
      }
      // Update it
      return update(connection, record);
    }
    // Add it
    return add(connection, record);
  }

  private static CollectionTab add(Connection connection, CollectionTab record) throws SQLException {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("collection_id", record.getCollectionId())
        .FIELD("tab_order", record.getTabOrder())
        .FIELD("name", record.getName())
        .FIELD("link", record.getLink())
        .FIELD("page_title", record.getPageTitle())
        .FIELD("page_keywords", record.getPageKeywords())
        .FIELD("page_description", record.getPageDescription())
        .FIELD("draft", record.getDraft())
        .FIELD("enabled", record.getEnabled())
        .FIELD("page_xml", record.getPageXml())
        .FIELD("role_id_list", record.getRoleIdList());
    // In a transaction (use the existing connection)
    record.setId(insert.execute(connection));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static CollectionTab update(Connection connection, CollectionTab record) throws SQLException {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("tab_order", record.getTabOrder())
        .SET("name", record.getName())
        .SET("link", record.getLink())
        .SET("page_title", record.getPageTitle())
        .SET("page_keywords", record.getPageKeywords())
        .SET("page_description", record.getPageDescription())
        .SET("draft", record.getDraft())
        .SET("enabled", record.getEnabled())
        .SET("page_xml", record.getPageXml())
        .SET("role_id_list", record.getRoleIdList())
        .WHERE("tab_id = ?", record.getId());
    // In a transaction (use the existing connection)
    if (update.execute(connection).booleanValue()) {
      return record;
    }
    return null;
  }

  private static void remove(Connection connection, CollectionTab record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("tab_id = ?", record.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Collection collection) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("collection_id = ?", collection.getId()).execute(connection);
  }

  private static CollectionTab buildRecord(ResultSet rs) {
    try {
      CollectionTab record = new CollectionTab();
      record.setId(rs.getLong("tab_id"));
      record.setCollectionId(rs.getLong("collection_id"));
      record.setTabOrder(DB.getInt(rs, "tab_order", 0));
      record.setName(rs.getString("name"));
      record.setLink(rs.getString("link"));
      record.setPageTitle(rs.getString("page_title"));
      record.setPageKeywords(rs.getString("page_keywords"));
      record.setPageDescription(rs.getString("page_description"));
      record.setDraft(rs.getBoolean("draft"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setPageXml(rs.getString("page_xml"));
      record.setRoleIdList(rs.getString("role_id_list"));
      record.setPageImageUrl(rs.getString("page_image_url"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
