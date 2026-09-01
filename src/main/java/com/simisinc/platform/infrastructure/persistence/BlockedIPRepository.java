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

import java.io.File;
import java.io.IOException;
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
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.BlockedIP;

/**
 * Persists and retrieves blocked IP objects
 *
 * @author matt rajkowski
 * @created 3/25/20 10:10 AM
 */
public class BlockedIPRepository {

  private static Log LOG = LogFactory.getLog(BlockedIPRepository.class);

  private static String TABLE_NAME = "block_list";
  private static String[] PRIMARY_KEY = new String[] { "block_list_id" };

  private static DataResult<BlockedIP> query(DataConstraints constraints) {
    Select select = DB.SELECT("block_list.*").FROM(TABLE_NAME);
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(BlockedIPRepository::buildRecord);
  }

  public static List<BlockedIP> findAll() {
    return findAll(null);
  }

  public static List<BlockedIP> findAll(DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("created DESC");
    return query(constraints).getRecords();
  }

  public static BlockedIP findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("block_list.*")
        .FROM(TABLE_NAME)
        .WHERE("block_list_id = ?", id)
        .returnRecord(BlockedIPRepository::buildRecord);
  }

  public static BlockedIP findByIpAddress(String ipAddress) {
    if (StringUtils.isBlank(ipAddress)) {
      return null;
    }
    return DB.SELECT("block_list.*")
        .FROM(TABLE_NAME)
        .WHERE("ip_address = ?", ipAddress)
        .returnRecord(BlockedIPRepository::buildRecord);
  }

  public static BlockedIP save(BlockedIP record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static BlockedIP add(BlockedIP record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("ip_address", StringUtils.trimToNull(record.getIpAddress()))
        .FIELD("reason", StringUtils.trimToNull(record.getReason()))
        .FIELD("created", record.getCreated());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static BlockedIP update(BlockedIP record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("ip_address", StringUtils.trimToNull(record.getIpAddress()))
        .SET("reason", StringUtils.trimToNull(record.getReason()))
        .WHERE("block_list_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(BlockedIP record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      DB.DELETE().FROM(TABLE_NAME).WHERE("block_list_id = ?", record.getId()).execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static BlockedIP buildRecord(ResultSet rs) {
    try {
      BlockedIP record = new BlockedIP();
      record.setId(rs.getLong("block_list_id"));
      record.setIpAddress(rs.getString("ip_address"));
      record.setCreated(rs.getTimestamp("created"));
      record.setReason(rs.getString("reason"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  public static void export(DataConstraints constraints, File file) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("block_list_id");
    List<BlockedIP> records = findAll(constraints);
    try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
      writer.write("IP Address,Date,Reason\n");
      for (BlockedIP record : records) {
        writer.write((record.getIpAddress() == null ? "" : record.getIpAddress()) + ","
            + (record.getCreated() == null ? "" : record.getCreated()) + ","
            + (record.getReason() == null ? "" : record.getReason().replace(",", " ")) + "\n");
      }
    } catch (IOException e) {
      LOG.error("SQLException: " + e.getMessage(), e);
    }
  }
}
