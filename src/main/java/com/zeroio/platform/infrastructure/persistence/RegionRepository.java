/*
 * Copyright 2026 Matt Rajkowski
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

package com.zeroio.platform.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.json.JsonCommand;
import com.zeroio.platform.domain.model.Region;

/**
 * Persists and retrieves region objects
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class RegionRepository {

  private static Log LOG = LogFactory.getLog(RegionRepository.class);

  private static String TABLE_NAME = "regions";
  private static String[] PRIMARY_KEY = new String[] { "region_id" };

  public static Region findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("region_id = ?", id)
        .returnRecord(RegionRepository::buildRecord);
  }

  public static Region findByCode(String code) {
    if (StringUtils.isBlank(code)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("code = ?", code)
        .returnRecord(RegionRepository::buildRecord);
  }

  public static List<Region> findAll() {
    DataResult<Region> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .ORDER_BY("level")
        .returnDataResult(RegionRepository::buildRecord);
    return result.getRecords();
  }

  public static Region save(Region record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static Region add(Region record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("code", StringUtils.trimToNull(record.getCode()))
        .FIELD("name", StringUtils.trimToNull(record.getName()));

    if (record.getValues() != null && record.getValues().length > 0) {
      insert.FIELD("values", JsonCommand.toJsonArray(record.getValues()), CastType.JSONB);
    }
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static Region update(Region record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("code", StringUtils.trimToNull(record.getCode()))
        .SET("name", StringUtils.trimToNull(record.getName()));
    if (record.getValues() != null && record.getValues().length > 0) {
      update.SET("values", JsonCommand.toJsonArray(record.getValues()), CastType.JSONB);
    } else {
      update.SET("values", (String) null, CastType.JSONB);
    }
    update.WHERE("region_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Region record) {
    return DB.DELETE().FROM(TABLE_NAME).WHERE("region_id = ?", record.getId()).execute();
  }

  /**
   * Build the record from the database
   *
   * @param rs
   * @return
   * @throws SQLException
   */
  private static Region buildRecord(ResultSet rs) {
    try {
      Region record = new Region();
      record.setId(rs.getLong("region_id"));
      record.setCode(rs.getString("code"));
      record.setName(rs.getString("name"));
      record.setValues(JsonCommand.fromJsonArray(rs.getString("values")));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
