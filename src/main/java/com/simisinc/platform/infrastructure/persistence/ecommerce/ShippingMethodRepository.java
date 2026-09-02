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
import com.simisinc.platform.domain.model.ecommerce.ShippingMethod;

/**
 * Persists and retrieves shipping method objects
 *
 * @author matt rajkowski
 * @created 6/27/19 9:14 AM
 */
public class ShippingMethodRepository {

  private static Log LOG = LogFactory.getLog(ShippingMethodRepository.class);

  private static String TABLE_NAME = "lookup_shipping_method";
  private static String[] PRIMARY_KEY = new String[] { "method_id" };

  public static List<ShippingMethod> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("level"))
        .returnDataResult(ShippingMethodRepository::buildRecord).getRecords();
  }

  public static ShippingMethod findById(long methodId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("method_id = ?", methodId)
        .returnRecord(ShippingMethodRepository::buildRecord);
  }

  public static ShippingMethod save(ShippingMethod record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static ShippingMethod add(ShippingMethod record) {
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

  public static ShippingMethod update(ShippingMethod record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("level", record.getLevel())
        .SET("code", record.getCode())
        .SET("title", record.getTitle())
        .SET("enabled", record.getEnabled())
        .WHERE("method_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static ShippingMethod buildRecord(ResultSet rs) {
    try {
      ShippingMethod record = new ShippingMethod();
      record.setId(rs.getLong("method_id"));
      record.setLevel(rs.getInt("level"));
      record.setCode(rs.getString("code"));
      record.setTitle(rs.getString("title"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setBoxzookaCode(rs.getString("boxzooka_code"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
