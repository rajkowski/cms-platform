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
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.ecommerce.SalesTaxNexusAddress;

/**
 * Persists and retrieves sales tax nexus address objects
 *
 * @author matt rajkowski
 * @created 5/29/19 1:36 PM
 */
public class SalesTaxNexusAddressRepository {

  private static Log LOG = LogFactory.getLog(SalesTaxNexusAddressRepository.class);

  private static String TABLE_NAME = "sales_tax_nexus_addresses";
  private static String[] PRIMARY_KEY = new String[] { "address_id" };

  public static List<SalesTaxNexusAddress> findAll() {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("address_id"))
        .returnDataResult(SalesTaxNexusAddressRepository::buildRecord).getRecords();
  }

  public static SalesTaxNexusAddress findById(long addressId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("address_id = ?", addressId)
        .returnRecord(SalesTaxNexusAddressRepository::buildRecord);
  }

  public static SalesTaxNexusAddress save(SalesTaxNexusAddress record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static boolean remove(SalesTaxNexusAddress record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      DB.DELETE().FROM(TABLE_NAME).WHERE("address_id = ?", record.getId()).execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The delete failed!");
    return false;
  }

  public static SalesTaxNexusAddress add(SalesTaxNexusAddress record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      long generatedId = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("street_address", record.getStreet())
          .FIELD("address_line_2", record.getAddressLine2())
          .FIELD("city", record.getCity())
          .FIELD("state", record.getState())
          .FIELD("country", record.getCountry())
          .FIELD("postal_code", record.getPostalCode())
          .FIELD("latitude", record.getLatitude())
          .FIELD("longitude", record.getLongitude())
          .FIELD("created_by", record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy())
          .execute(connection);
      record.setId(generatedId);
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static SalesTaxNexusAddress update(SalesTaxNexusAddress record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("street_address", record.getStreet())
        .SET("address_line_2", record.getAddressLine2())
        .SET("city", record.getCity())
        .SET("state", record.getState())
        .SET("country", record.getCountry())
        .SET("postal_code", record.getPostalCode())
        .SET("latitude", record.getLatitude())
        .SET("longitude", record.getLongitude())
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("address_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static SalesTaxNexusAddress buildRecord(ResultSet rs) {
    try {
      SalesTaxNexusAddress record = new SalesTaxNexusAddress();
      record.setId(rs.getLong("address_id"));
      record.setStreet(rs.getString("street_address"));
      record.setAddressLine2(rs.getString("address_line_2"));
      record.setCity(rs.getString("city"));
      record.setState(rs.getString("state"));
      record.setCountry(rs.getString("country"));
      record.setPostalCode(rs.getString("postal_code"));
      record.setLatitude(rs.getDouble("latitude"));
      record.setLongitude(rs.getDouble("longitude"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(DB.getLong(rs, "modified_by", -1));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
