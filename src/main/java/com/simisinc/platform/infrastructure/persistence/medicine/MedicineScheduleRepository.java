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
import com.github.rajkowski.database.DataResult;
import com.simisinc.platform.domain.model.medicine.Medicine;
import com.simisinc.platform.domain.model.medicine.MedicineSchedule;

/**
 * Persists and retrieves medicine schedule objects
 *
 * @author matt rajkowski
 * @created 9/10/18 4:51 PM
 */
public class MedicineScheduleRepository {

  private static Log LOG = LogFactory.getLog(MedicineScheduleRepository.class);

  private static String TABLE_NAME = "medicine_schedule";
  private static String[] PRIMARY_KEY = new String[] { "schedule_id" };

  public static MedicineSchedule save(MedicineSchedule record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static MedicineSchedule save(Connection connection, MedicineSchedule record) throws SQLException {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(connection, record);
  }

  private static MedicineSchedule add(MedicineSchedule record) {
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

  private static MedicineSchedule add(Connection connection, MedicineSchedule record) throws SQLException {
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("medicine_id", record.getMedicineId() == -1 ? null : record.getMedicineId())
        .FIELD("as_needed", record.getFrequency() == MedicineSchedule.AS_NEEDED)
        .FIELD("every_day", record.getFrequency() == MedicineSchedule.EVERY_DAY)
        .FIELD("every_x_days", record.getDaysToRepeat())
        .FIELD("on_monday", record.isOnMonday())
        .FIELD("on_tuesday", record.isOnTuesday())
        .FIELD("on_wednesday", record.isOnWednesday())
        .FIELD("on_thursday", record.isOnThursday())
        .FIELD("on_friday", record.isOnFriday())
        .FIELD("on_saturday", record.isOnSaturday())
        .FIELD("on_sunday", record.isOnSunday())
        .FIELD("times_a_day", record.getMedicineTimeList() != null ? record.getMedicineTimeList().size() : 0)
        .FIELD("start_date", record.getStartDate())
        .FIELD("end_date", record.getEndDate())
        .FIELD("comments", record.getNotes())
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .execute(connection);
    record.setId(generatedId);
    if (record.getMedicineTimeList() != null) {
      MedicineTimeRepository.insertMedicineTimeList(connection, record);
    }
    return record;
  }

  private static MedicineSchedule update(MedicineSchedule record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("as_needed", record.getFrequency() == MedicineSchedule.AS_NEEDED)
        .SET("every_day", record.getFrequency() == MedicineSchedule.EVERY_DAY)
        .SET("every_x_days", record.getDaysToRepeat())
        .SET("on_monday", record.isOnMonday())
        .SET("on_tuesday", record.isOnTuesday())
        .SET("on_wednesday", record.isOnWednesday())
        .SET("on_thursday", record.isOnThursday())
        .SET("on_friday", record.isOnFriday())
        .SET("on_saturday", record.isOnSaturday())
        .SET("on_sunday", record.isOnSunday())
        .SET("times_a_day", record.getMedicineTimeList() != null ? record.getMedicineTimeList().size() : 0)
        .SET("start_date", record.getStartDate())
        .SET("end_date", record.getEndDate())
        .SET("comments", record.getNotes())
        .SET("modified_by", record.getModifiedBy())
        .WHERE("schedule_id = ?", record.getId())
        .execute();
    if (updated) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(MedicineSchedule record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("schedule_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Medicine record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("medicine_id = ?", record.getId()).execute(connection);
  }

  public static MedicineSchedule findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("schedule_id = ?", id)
        .returnRecord(MedicineScheduleRepository::buildRecord);
  }

  public static MedicineSchedule findByMedicineId(long medicineId) {
    if (medicineId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("medicine_id = ?", medicineId)
        .returnRecord(MedicineScheduleRepository::buildRecord);
  }

  public static List<MedicineSchedule> findAllByMedicineId(long medicineId) {
    if (medicineId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("medicine_id = ?", medicineId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("schedule_id").setUseCount(false))
        .returnDataResult(MedicineScheduleRepository::buildRecord).getRecords();
  }

  private static MedicineSchedule buildRecord(ResultSet rs) {
    try {
      MedicineSchedule record = new MedicineSchedule();
      record.setId(rs.getLong("schedule_id"));
      record.setMedicineId(rs.getLong("medicine_id"));
      boolean asNeeded = rs.getBoolean("as_needed");
      boolean everyDay = rs.getBoolean("every_day");
      int everyNDays = rs.getInt("every_x_days");
      record.setOnMonday(rs.getBoolean("on_monday"));
      record.setOnTuesday(rs.getBoolean("on_tuesday"));
      record.setOnWednesday(rs.getBoolean("on_wednesday"));
      record.setOnThursday(rs.getBoolean("on_thursday"));
      record.setOnFriday(rs.getBoolean("on_friday"));
      record.setOnSaturday(rs.getBoolean("on_saturday"));
      record.setOnSunday(rs.getBoolean("on_sunday"));
      if (asNeeded) {
        record.setFrequency(MedicineSchedule.AS_NEEDED);
      } else if (everyDay) {
        record.setFrequency(MedicineSchedule.EVERY_DAY);
      } else if (everyNDays > 0) {
        record.setFrequency(MedicineSchedule.EVERY_N_DAYS);
        record.setDaysToRepeat(everyNDays);
      } else {
        record.setFrequency(MedicineSchedule.SPECIFIC_DAYS);
      }
      record.setTimesADay(rs.getInt("times_a_day"));
      record.setStartDate(rs.getTimestamp("start_date"));
      record.setEndDate(rs.getTimestamp("end_date"));
      record.setNotes(rs.getString("comments"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
