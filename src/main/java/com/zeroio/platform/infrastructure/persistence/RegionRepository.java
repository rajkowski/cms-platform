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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.database.DataResult;
import com.simisinc.platform.infrastructure.database.SqlUtils;
import com.simisinc.platform.infrastructure.database.SqlValue;
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
    return (Region) DB.selectRecordFrom(
        TABLE_NAME,
        DB.WHERE("region_id = ?", id),
        RegionRepository::buildRecord);
  }

  public static Region findByCode(String code) {
    if (StringUtils.isBlank(code)) {
      return null;
    }
    return (Region) DB.selectRecordFrom(
        TABLE_NAME,
        DB.WHERE("code = ?", code),
        RegionRepository::buildRecord);
  }

  public static List<Region> findAll() {
    DataResult result = DB.selectAllFrom(
        TABLE_NAME,
        null,
        new DataConstraints().setDefaultColumnToSortBy("level").setUseCount(false),
        RegionRepository::buildRecord);
    return (List<Region>) result.getRecords();
  }

  public static Region save(Region record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static Region add(Region record) {
    SqlUtils insertValues = new SqlUtils()
        .add("code", StringUtils.trimToNull(record.getCode()))
        .add("name", StringUtils.trimToNull(record.getName()));
    if (record.getValues() != null && record.getValues().length > 0) {
      insertValues.add(new SqlValue("values", SqlValue.JSONB_TYPE, JsonCommand.toJsonArray(record.getValues())));
    }
    record.setId(DB.insertInto(TABLE_NAME, insertValues, PRIMARY_KEY));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static Region update(Region record) {
    SqlUtils updateValues = new SqlUtils()
        .add("code", StringUtils.trimToNull(record.getCode()))
        .add("name", StringUtils.trimToNull(record.getName()));
    if (record.getValues() != null && record.getValues().length > 0) {
      updateValues.add(new SqlValue("values", SqlValue.JSONB_TYPE, JsonCommand.toJsonArray(record.getValues())));
    } else {
      updateValues.add(new SqlValue("values", SqlValue.JSONB_TYPE, null));
    }
    if (DB.update(TABLE_NAME, updateValues, DB.WHERE("region_id = ?", record.getId()))) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Region record) {
    return DB.deleteFrom(TABLE_NAME, DB.WHERE("region_id = ?", record.getId())) > 0;
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
