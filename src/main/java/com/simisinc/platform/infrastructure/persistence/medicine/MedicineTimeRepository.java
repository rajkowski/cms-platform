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

package com.simisinc.platform.infrastructure.persistence.medicine;

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
import com.simisinc.platform.domain.model.medicine.Medicine;
import com.simisinc.platform.domain.model.medicine.MedicineSchedule;
import com.simisinc.platform.domain.model.medicine.MedicineTime;

/**
 * Persists and retrieves medicine time objects
 *
 * @author matt rajkowski
 * @created 9/10/18 5:53 PM
 */
public class MedicineTimeRepository {

  private static Log LOG = LogFactory.getLog(MedicineTimeRepository.class);

  private static String TABLE_NAME = "medicine_times";
  private static String[] PRIMARY_KEY = new String[] { "time_id" };

  public static MedicineTime save(MedicineTime record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static MedicineTime save(Connection connection, MedicineTime record) throws SQLException {
    if (record.getId() > -1) {
      LOG.error("Not supported...");
      return update(record);
    }
    return add(connection, record);
  }

  private static MedicineTime add(MedicineTime record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      add(connection, record);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  private static MedicineTime add(Connection connection, MedicineTime record) throws SQLException {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("schedule_id", record.getScheduleId() == -1 ? null : record.getScheduleId())
        .FIELD("medicine_id", record.getMedicineId() == -1 ? null : record.getMedicineId())
        .FIELD("hour", record.getHour())
        .FIELD("minute", record.getMinute())
        .FIELD("quantity", record.getQuantity())
        .execute(connection);
    record.setId(generatedId);
    return record;
  }

  private static MedicineTime update(MedicineTime record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("hour", record.getHour())
        .SET("minute", record.getMinute())
        .SET("quantity", record.getQuantity())
        .WHERE("time_id = ?", record.getId())
        .execute();
    if (updated) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static long insertMedicineTimeList(Connection connection, MedicineSchedule medicineSchedule) throws SQLException {
    if (medicineSchedule.getMedicineTimeList() == null) {
      return 0;
    }
    long count = 0;
    for (MedicineTime record : medicineSchedule.getMedicineTimeList()) {
      DB.INSERT().INTO(TABLE_NAME)
          .FIELD("schedule_id", medicineSchedule.getId() == -1 ? null : medicineSchedule.getId())
          .FIELD("medicine_id", medicineSchedule.getMedicineId() == -1 ? null : medicineSchedule.getMedicineId())
          .FIELD("hour", record.getHour())
          .FIELD("minute", record.getMinute())
          .FIELD("quantity", record.getQuantity())
          .execute(connection);
      ++count;
    }
    return count;
  }

  public static boolean remove(MedicineTime record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("time_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, MedicineSchedule record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("schedule_id = ?", record.getId()).execute(connection);
  }

  public static void removeAll(Connection connection, Medicine record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("medicine_id = ?", record.getId()).execute(connection);
  }

  public static MedicineTime findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("time_id = ?", id)
        .returnRecord(MedicineTimeRepository::buildRecord);
  }

  public static List<MedicineTime> findAllByMedicineId(long medicineId) {
    if (medicineId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("medicine_id = ?", medicineId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("time_id").setUseCount(false))
        .returnDataResult(MedicineTimeRepository::buildRecord).getRecords();
  }

  private static MedicineTime buildRecord(ResultSet rs) {
    try {
      MedicineTime record = new MedicineTime();
      record.setId(rs.getLong("time_id"));
      record.setScheduleId(rs.getLong("schedule_id"));
      record.setMedicineId(rs.getLong("medicine_id"));
      record.setHour(rs.getInt("hour"));
      record.setMinute(rs.getInt("minute"));
      record.setQuantity(rs.getInt("quantity"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
