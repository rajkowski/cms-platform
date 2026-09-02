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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.ecommerce.ShippingCountry;

/**
 * Persists and retrieves shipping country objects
 *
 * @author matt rajkowski
 * @created 6/27/19 11:52 AM
 */
public class ShippingCountryRepository {

  private static Log LOG = LogFactory.getLog(ShippingCountryRepository.class);

  private static String TABLE_NAME = "lookup_shipping_countries";
  private static String[] PRIMARY_KEY = new String[] { "country_id" };

  public static List<ShippingCountry> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("level"))
        .returnDataResult(ShippingCountryRepository::buildRecord).getRecords();
  }

  public static ShippingCountry findById(long countryId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("country_id = ?", countryId)
        .returnRecord(ShippingCountryRepository::buildRecord);
  }

  public static ShippingCountry findByEnabledCountry(String name) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(title) = ?", name.toLowerCase())
        .AND("enabled = ?", true)
        .returnRecord(ShippingCountryRepository::buildRecord);
  }

  public static ShippingCountry save(ShippingCountry record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static ShippingCountry add(ShippingCountry record) {
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

  public static ShippingCountry update(ShippingCountry record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("level", record.getLevel())
        .SET("code", record.getCode())
        .SET("title", record.getTitle())
        .SET("enabled", record.getEnabled())
        .WHERE("country_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static ShippingCountry buildRecord(ResultSet rs) {
    try {
      ShippingCountry record = new ShippingCountry();
      record.setId(rs.getLong("country_id"));
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
