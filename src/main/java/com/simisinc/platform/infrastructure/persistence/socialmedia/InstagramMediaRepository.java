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

package com.simisinc.platform.infrastructure.persistence.socialmedia;

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
import com.github.rajkowski.database.Select;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.domain.model.socialmedia.InstagramMedia;

/**
 * Persists and retrieves Instagram media objects
 *
 * @author matt rajkowski
 * @created 9/15/19 9:15 AM
 */
public class InstagramMediaRepository {

  private static Log LOG = LogFactory.getLog(InstagramMediaRepository.class);

  private static String TABLE_NAME = "instagram_media";
  private static String[] PRIMARY_KEY = new String[] { "id" };

  public static InstagramMedia findByGraphId(String graphId) {
    if (StringUtils.isBlank(graphId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("graph_id = ?", graphId)
        .returnRecord(InstagramMediaRepository::buildRecord);
  }

  public static List<InstagramMedia> findAll() {
    return findAll(null, null);
  }

  public static List<InstagramMedia> findAll(InstagramMediaSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("created DESC");
    Select select = DB.SELECT("*").FROM(TABLE_NAME);
    if (specification != null && StringUtils.isNotBlank(specification.getMediaType())) {
      select.WHERE("media_type = ?", specification.getMediaType());
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(InstagramMediaRepository::buildRecord).getRecords();
  }

  public static InstagramMedia save(InstagramMedia record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static InstagramMedia add(InstagramMedia record) {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("graph_id", record.getGraphId())
        .FIELD("permalink", StringUtils.trimToNull(record.getPermalink()))
        .FIELD("media_type", StringUtils.trimToNull(record.getMediaType()))
        .FIELD("media_url", StringUtils.trimToNull(record.getMediaUrl()))
        .FIELD("caption", HtmlCommand.text(StringUtils.trimToNull(record.getCaption())))
        .FIELD("short_code", StringUtils.trimToNull(record.getShortCode()))
        .FIELD("timestamp", StringUtils.trimToNull(record.getTimestamp()))
        .execute();
    record.setId(generatedId);
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static InstagramMedia update(InstagramMedia record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("graph_id", record.getGraphId())
        .SET("permalink", StringUtils.trimToNull(record.getPermalink()))
        .SET("media_type", StringUtils.trimToNull(record.getMediaType()))
        .SET("media_url", StringUtils.trimToNull(record.getMediaUrl()))
        .SET("caption", HtmlCommand.text(StringUtils.trimToNull(record.getCaption())))
        .SET("short_code", StringUtils.trimToNull(record.getShortCode()))
        .SET("timestamp", StringUtils.trimToNull(record.getTimestamp()))
        .WHERE("id = ?", record.getId())
        .execute();
    if (updated) {
      // CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(InstagramMedia record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      // ItemCategoryRepository.removeAll(connection, record);
      // CollectionRepository.updateItemCount(connection, record.getCollectionId(), -1);
      // CategoryRepository.updateItemCount(connection, record.getCategoryId(), -1);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static InstagramMedia buildRecord(ResultSet rs) {
    try {
      InstagramMedia record = new InstagramMedia();
      record.setId(rs.getLong("id"));
      record.setGraphId(rs.getString("graph_id"));
      record.setPermalink(rs.getString("permalink"));
      record.setMediaType(rs.getString("media_type"));
      record.setMediaUrl(rs.getString("media_url"));
      record.setCaption(rs.getString("caption"));
      record.setShortCode(rs.getString("short_code"));
      record.setTimestamp(rs.getString("timestamp"));
      record.setCreated(rs.getTimestamp("created"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
