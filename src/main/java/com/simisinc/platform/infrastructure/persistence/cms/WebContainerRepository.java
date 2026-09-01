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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.domain.model.cms.WebContainer;
import com.simisinc.platform.infrastructure.cache.CacheManager;

/**
 * Persists and retrieves web container objects
 *
 * @author matt rajkowski
 * @created 1/21/21 9:41 PM
 */
public class WebContainerRepository {

  private static Log LOG = LogFactory.getLog(WebContainerRepository.class);

  private static String TABLE_NAME = "web_containers";
  private static String[] PRIMARY_KEY = new String[] { "container_id" };

  public static WebContainer findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("web_containers.*")
        .FROM(TABLE_NAME)
        .WHERE("container_id = ?", id)
        .returnRecord(WebContainerRepository::buildRecord);
  }

  public static WebContainer findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    return DB.SELECT("web_containers.*")
        .FROM(TABLE_NAME)
        .WHERE("container_name = ?", name)
        .returnRecord(WebContainerRepository::buildRecord);
  }

  public static List<WebContainer> findAllByPrefix(String prefix) {
    DataConstraints constraints = new DataConstraints().setDefaultColumnToSortBy("container_name").setUseCount(false);
    return DB.SELECT("web_containers.*")
        .FROM(TABLE_NAME)
        .WHERE("container_name LIKE ?", prefix + ".%")
        .WITH(constraints)
        .returnDataResult(WebContainerRepository::buildRecord)
        .getRecords();
  }

  public static List<WebContainer> findAll() {
    return DB.SELECT("web_containers.*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("container_id"))
        .returnDataResult(WebContainerRepository::buildRecord)
        .getRecords();
  }

  public static WebContainer save(WebContainer record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static WebContainer add(WebContainer record) {
    record.setId(DB.INSERT().INTO(TABLE_NAME)
        .FIELD("container_name", StringUtils.trimToNull(record.getName()))
        .FIELD("label", StringUtils.trimToNull(record.getLabel()))
        .FIELD("image_path", StringUtils.trimToNull(record.getImagePath()))
        .FIELD("draft", record.getDraft())
        .FIELD("container_xml", StringUtils.trimToNull(record.getContainerXml()))
        .FIELD("draft_xml", StringUtils.trimToNull(record.getDraftXml()))
        .execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static WebContainer update(WebContainer record) {
    if (DB.UPDATE(TABLE_NAME)
        .SET("container_name", StringUtils.trimToNull(record.getName()))
        .SET("label", StringUtils.trimToNull(record.getLabel()))
        .SET("image_path", StringUtils.trimToNull(record.getImagePath()))
        .SET("draft", record.getDraft())
        .SET("container_xml", StringUtils.trimToNull(record.getContainerXml()))
        .SET("draft_xml", StringUtils.trimToNull(record.getDraftXml()))
        .WHERE("container_id = ?", record.getId())
        .execute()) {
      // Invalidate the cache
      if (record.getName().startsWith("header")) {
        CacheManager.invalidateObjectCacheKey(CacheManager.WEBSITE_HEADER);
        CacheManager.invalidateObjectCacheKey(CacheManager.WEBSITE_PLAIN_HEADER);
      } else if (record.getName().startsWith("footer")) {
        CacheManager.invalidateObjectCacheKey(CacheManager.WEBSITE_FOOTER);
      }
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static WebContainer buildRecord(ResultSet rs) {
    try {
      WebContainer record = new WebContainer();
      record.setId(rs.getLong("container_id"));
      record.setName(rs.getString("container_name"));
      record.setLabel(rs.getString("label"));
      record.setImagePath(rs.getString("image_path"));
      record.setDraft(rs.getBoolean("draft"));
      record.setContainerXml(rs.getString("container_xml"));
      record.setDraftXml(rs.getString("draft_xml"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
