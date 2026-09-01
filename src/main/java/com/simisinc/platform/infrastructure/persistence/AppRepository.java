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

package com.simisinc.platform.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.App;
import com.simisinc.platform.infrastructure.cache.CacheManager;

/**
 * Persists and retrieves app objects
 *
 * @author matt rajkowski
 * @created 4/17/18 7:48 PM
 */
public class AppRepository {

  private static Log LOG = LogFactory.getLog(AppRepository.class);

  private static String TABLE_NAME = "apps";
  private static String[] PRIMARY_KEY = new String[] { "app_id" };

  private static DataResult<App> query(AppSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("apps.*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("app_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getPublicKey())) {
        select.AND("public_key = ?", specification.getPublicKey());
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(AppRepository::buildRecord);
  }

  public static App findByPublicKey(String publicKey) {
    if (StringUtils.isBlank(publicKey)) {
      return null;
    }
    return DB.SELECT("apps.*")
        .FROM(TABLE_NAME)
        .WHERE("public_key = ?", publicKey)
        .returnRecord(AppRepository::buildRecord);
  }

  public static App findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("apps.*")
        .FROM(TABLE_NAME)
        .WHERE("app_id = ?", id)
        .returnRecord(AppRepository::buildRecord);
  }

  public static List<App> findAll() {
    return findAll(null, null);
  }

  public static List<App> findAll(AppSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("name");
    return query(specification, constraints).getRecords();
  }

  public static App save(App record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static App add(App record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
        .FIELD("public_key", record.getPublicKey())
        .FIELD("private_key", record.getPrivateKey())
        .FIELD("created_by", record.getCreatedBy());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static App update(App record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("summary", StringUtils.trimToNull(record.getSummary()))
        .WHERE("app_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      CacheManager.invalidateKey(CacheManager.APP_CACHE, record.getPublicKey());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  /**
   * Build the record from the database
   *
   * @param rs
   * @return
   * @throws SQLException
   */
  private static App buildRecord(ResultSet rs) {
    try {
      App record = new App();
      record.setId(rs.getLong("app_id"));
      record.setName(rs.getString("name"));
      record.setSummary(rs.getString("summary"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      record.setPublicKey(rs.getString("public_key"));
      record.setPrivateKey(rs.getString("private_key"));
      record.setEnabled(rs.getBoolean("enabled"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
