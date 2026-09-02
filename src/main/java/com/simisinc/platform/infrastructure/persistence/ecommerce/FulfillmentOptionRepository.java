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

package com.simisinc.platform.infrastructure.persistence.ecommerce;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.ecommerce.FulfillmentOption;

/**
 * Persists and retrieves fulfillment option objects
 *
 * @author matt rajkowski
 * @created 4/9/20 1:30 PM
 */
public class FulfillmentOptionRepository {

  private static Log LOG = LogFactory.getLog(FulfillmentOptionRepository.class);

  private static String TABLE_NAME = "lookup_fulfillment_options";
  private static String[] PRIMARY_KEY = new String[] { "fulfillment_id" };

  public static List<FulfillmentOption> findAll() {
    DataResult<FulfillmentOption> result = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("enabled = ?", true)
        .returnDataResult(FulfillmentOptionRepository::buildRecord);
    return result.getRecords();
  }

  public static FulfillmentOption findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("fulfillment_id = ?", id)
        .returnRecord(FulfillmentOptionRepository::buildRecord);
  }

  public static FulfillmentOption findByCode(String code) {
    if (StringUtils.isBlank(code)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("code = ?", code)
        .returnRecord(FulfillmentOptionRepository::buildRecord);
  }

  public static boolean remove(FulfillmentOption record) {
    return DB.DELETE().FROM(TABLE_NAME).WHERE("fulfillment_id = ?", record.getId()).execute();
  }

  public static FulfillmentOption save(FulfillmentOption record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static FulfillmentOption add(FulfillmentOption record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("code", StringUtils.trimToNull(record.getCode()))
        .FIELD("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("enabled", record.getEnabled())
        .FIELD("overrides_others", record.getOverridesOthers());
    long generatedId = insert.execute();
    if (insert.isSuccess()) {
      record.setId(generatedId);
      return record;
    }
    LOG.error("An id was not set!");
    return null;
  }

  public static FulfillmentOption update(FulfillmentOption record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("code", StringUtils.trimToNull(record.getCode()))
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("enabled", record.getEnabled())
        .SET("overrides_others", record.getOverridesOthers())
        .WHERE("fulfillment_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    return null;
  }

  private static FulfillmentOption buildRecord(ResultSet rs) {
    try {
      FulfillmentOption record = new FulfillmentOption();
      record.setId(rs.getLong("fulfillment_id"));
      record.setCode(rs.getString("code"));
      record.setTitle(rs.getString("title"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setOverridesOthers(rs.getBoolean("overrides_others"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
