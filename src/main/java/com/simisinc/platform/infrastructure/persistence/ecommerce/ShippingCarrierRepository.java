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
import com.simisinc.platform.domain.model.ecommerce.ShippingCarrier;

/**
 * Persists and retrieves shipping carrier objects
 *
 * @author matt rajkowski
 * @created 4/23/20 7:00 AM
 */
public class ShippingCarrierRepository {

  private static Log LOG = LogFactory.getLog(ShippingCarrierRepository.class);

  private static String TABLE_NAME = "lookup_shipping_carrier";
  private static String[] PRIMARY_KEY = new String[] { "carrier_id" };

  public static List<ShippingCarrier> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("level"))
        .returnDataResult(ShippingCarrierRepository::buildRecord).getRecords();
  }

  public static ShippingCarrier findById(long carrierId) {
    if (carrierId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("carrier_id = ?", carrierId)
        .returnRecord(ShippingCarrierRepository::buildRecord);
  }

  public static ShippingCarrier findByCode(String code) {
    if (StringUtils.isBlank(code)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("UPPER(code) = ?", code.toUpperCase())
        .returnRecord(ShippingCarrierRepository::buildRecord);
  }

  public static ShippingCarrier save(ShippingCarrier record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static ShippingCarrier add(ShippingCarrier record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      Insert insert = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("level", record.getLevel())
          .FIELD("code", record.getCode())
          .FIELD("title", record.getTitle())
          .FIELD("enabled", record.getEnabled());
      record.setId(insert.execute(connection));
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static ShippingCarrier update(ShippingCarrier record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("level", record.getLevel())
        .SET("code", record.getCode())
        .SET("title", record.getTitle())
        .SET("enabled", record.getEnabled())
        .WHERE("carrier_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static ShippingCarrier buildRecord(ResultSet rs) {
    try {
      ShippingCarrier record = new ShippingCarrier();
      record.setId(rs.getLong("carrier_id"));
      record.setLevel(rs.getInt("level"));
      record.setCode(rs.getString("code"));
      record.setTitle(rs.getString("title"));
      record.setEnabled(rs.getBoolean("enabled"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
