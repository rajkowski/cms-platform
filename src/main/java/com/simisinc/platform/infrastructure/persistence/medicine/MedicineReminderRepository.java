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

package com.simisinc.platform.infrastructure.persistence.medicine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.medicine.Medicine;
import com.simisinc.platform.domain.model.medicine.MedicineReminder;
import com.simisinc.platform.domain.model.medicine.MedicineReminderRawData;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves medicine reminder objects
 *
 * @author matt rajkowski
 * @created 9/11/18 1:54 PM
 */
public class MedicineReminderRepository {

  private static Log LOG = LogFactory.getLog(MedicineReminderRepository.class);

  private static String TABLE_NAME = "medicine_reminders";
  private static String[] PRIMARY_KEY = new String[] { "reminder_id" };

  private static DataResult<MedicineReminder> query(MedicineReminderSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      select.LEFT_JOIN("medicines medicines").ON("medicine_reminders.medicine_id = medicines.medicine_id");
      select.LEFT_JOIN("medicine_schedule sched").ON("medicine_reminders.schedule_id = sched.schedule_id");
      if (specification.getId() != -1) {
        select.AND("reminder_id = ?", specification.getId());
      }
      if (specification.getIndividualId() != -1) {
        select.AND("medicine_reminders.individual_id = ?", specification.getIndividualId());
      }
      if (specification.getMedicineId() != -1) {
        select.AND("medicine_reminders.medicine_id = ?", specification.getMedicineId());
      }
      if (specification.getMinDate() != null) {
        select.AND("reminder_date >= ?", specification.getMinDate());
      }
      if (specification.getMaxDate() != null) {
        select.AND("reminder_date < ?", specification.getMaxDate());
      }
      if (specification.getReminderIsAfterNow() != DataConstants.UNDEFINED) {
        if (specification.getReminderIsAfterNow() == DataConstants.TRUE) {
          select.AND("reminder_date >= NOW()");
        }
      }
      if (specification.getIsWithinEndDate() != DataConstants.UNDEFINED) {
        if (specification.getIsWithinEndDate() == DataConstants.TRUE) {
          select.AND("(sched.end_date IS NULL OR sched.end_date >= NOW())");
        }
      }
      if (specification.getIsSuspended() != DataConstants.UNDEFINED) {
        if (specification.getIsSuspended() == DataConstants.TRUE) {
          select.AND("medicines.suspended IS NOT NULL");
        } else {
          select.AND("medicines.suspended IS NULL");
        }
      }
      if (specification.getIsArchived() != DataConstants.UNDEFINED) {
        if (specification.getIsArchived() == DataConstants.TRUE) {
          select.AND("medicines.archived IS NOT NULL");
        } else {
          select.AND("medicines.archived IS NULL");
        }
      }
      if (specification.getIndividualsList() != null && !specification.getIndividualsList().isEmpty()) {
        StringBuilder sb = new StringBuilder();
        for (Long id : specification.getIndividualsList()) {
          if (sb.length() > 0) {
            sb.append(",");
          }
          sb.append(id);
        }
        select.AND("medicine_reminders.individual_id IN (" + sb + ")");
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(MedicineReminderRepository::buildRecord);
  }

  public static List<MedicineReminder> findAll(MedicineReminderSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("reminder_id");
    DataResult result = query(specification, constraints);
    return (List<MedicineReminder>) result.getRecords();
  }

  public static MedicineReminder findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("reminder_id = ?", id)
        .returnRecord(MedicineReminderRepository::buildRecord);
  }

  private static PreparedStatement createPreparedStatementForDailyReminders(Connection connection, Timestamp startDate,
      Timestamp endDate, DayOfWeek dayOfWeek, long medicineId) throws SQLException {
    StringBuilder currentDay = new StringBuilder();
    switch (dayOfWeek) {
      case MONDAY:
        currentDay.append("on_monday");
        break;
      case TUESDAY:
        currentDay.append("on_tuesday");
        break;
      case WEDNESDAY:
        currentDay.append("on_wednesday");
        break;
      case THURSDAY:
        currentDay.append("on_thursday");
        break;
      case FRIDAY:
        currentDay.append("on_friday");
        break;
      case SATURDAY:
        currentDay.append("on_saturday");
        break;
      case SUNDAY:
        currentDay.append("on_sunday");
        break;
      default:
        currentDay.append("on_invalid");
        break;
    }

    String startDateValue = new SimpleDateFormat("yyyy-MM-dd").format(startDate);
    //    String endDateValue = new SimpleDateFormat("yyyy-MM-dd").format(endDate);

    String SQL_QUERY = "SELECT ind.item_id AS individual_id, m.medicine_id, sched.schedule_id, mt.time_id, mt.hour, mt.minute " +
        "FROM medicines m " +
        "LEFT JOIN items ind ON (individual_id = ind.item_id) " +
        "LEFT JOIN items drug ON (drug_id = drug.item_id) " +
        "LEFT JOIN medicine_schedule sched ON (m.medicine_id = sched.medicine_id) " +
        "LEFT JOIN medicine_times mt ON (sched.schedule_id = mt.schedule_id) " +
        "WHERE " +
        "m.archived IS NULL " +
        (medicineId > -1 ? "AND m.medicine_id = ? " : "") +
        "AND sched.start_date <= ? " +
        "AND (sched.end_date IS NULL OR sched.end_date < ?) " +
        "AND (" +
        "every_day = TRUE " +
        "OR " + currentDay.toString() + " = TRUE " +
        "OR (every_x_days IS NOT NULL AND every_x_days > 0 AND MOD(DATE_PART('day', '" + startDateValue
        + " 00:00:00'::date - sched.start_date)::NUMERIC, every_x_days) = 0) " +
        ") " +
        "ORDER BY mt.hour, mt.minute";

    int i = 0;
    PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
    if (medicineId > -1) {
      pst.setLong(++i, medicineId);
    }
    pst.setTimestamp(++i, startDate);
    pst.setTimestamp(++i, endDate);
    return pst;
  }

  public static void createMedicineReminders(long medicineId, Timestamp startDate, Timestamp endDate, DayOfWeek dayOfWeek) {
    // Verify arguments
    if (ObjectUtils.anyNull(startDate, endDate, dayOfWeek)) {
      return;
    }

    // Load the rules to determine the daily reminders
    List<MedicineReminderRawData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = createPreparedStatementForDailyReminders(connection, startDate, endDate, dayOfWeek, medicineId);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        records.add(buildRawDataRecord(rs));
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
      LOG.error(se);
    }
    if (records == null || records.isEmpty()) {
      return;
    }
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Remove all reminders for the day
      removeMedicineReminders(connection, medicineId, startDate, endDate);
      // Add the specified reminders
      for (MedicineReminderRawData rawData : records) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startDate.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, rawData.getHour());
        calendar.set(Calendar.MINUTE, rawData.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp reminder = new Timestamp(calendar.getTimeInMillis());
        DB.INSERT().INTO(TABLE_NAME)
            .FIELD("individual_id", rawData.getIndividualId())
            .FIELD("medicine_id", rawData.getMedicineId())
            .FIELD("schedule_id", rawData.getScheduleId())
            .FIELD("time_id", rawData.getTimeId())
            .FIELD("reminder_date", reminder)
            .execute(connection);
      }
      // Finish the transaction
      transaction.commit();
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
  }

  private static void removeMedicineReminders(Connection connection, long medicineId, Timestamp startDate, Timestamp endDate)
      throws SQLException {
    DB.DELETE().FROM(TABLE_NAME)
        .WHERE("medicine_id = ?", medicineId)
        .WHERE("reminder_date >= ?", startDate)
        .WHERE("reminder_date < ?", endDate)
        .execute(connection);
  }

  public static void removeAll(Connection connection, Medicine record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("medicine_id = ?", record.getId()).execute(connection);
  }

  public static void markAsTaken(Connection connection, long reminderId, Timestamp takenTimestamp) throws SQLException {
    DB.UPDATE(TABLE_NAME)
        .SET("was_taken", true)
        .SET("logged", takenTimestamp)
        .WHERE("reminder_id = ?", reminderId)
        .execute(connection);
  }

  public static void markAsSkipped(Connection connection, long reminderId) throws SQLException {
    DB.UPDATE(TABLE_NAME)
        .SET("was_skipped", true)
        .WHERE("reminder_id = ?", reminderId)
        .execute(connection);
  }

  private static MedicineReminder buildRecord(ResultSet rs) {
    try {
      MedicineReminder record = new MedicineReminder();
      record.setId(rs.getLong("reminder_id"));
      record.setIndividualId(rs.getLong("individual_id"));
      record.setMedicineId(rs.getLong("medicine_id"));
      record.setScheduleId(rs.getLong("schedule_id"));
      record.setTimeId(rs.getLong("time_id"));
      record.setReminder(rs.getTimestamp("reminder_date"));
      record.setProcessed(rs.getTimestamp("processed"));
      record.setLogged(rs.getTimestamp("logged"));
      record.setWasTaken(rs.getBoolean("was_taken"));
      record.setWasSkipped(rs.getBoolean("was_skipped"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  private static MedicineReminderRawData buildRawDataRecord(ResultSet rs) {
    try {
      MedicineReminderRawData record = new MedicineReminderRawData();
      record.setIndividualId(rs.getLong("individual_id"));
      record.setMedicineId(rs.getLong("medicine_id"));
      record.setScheduleId(rs.getLong("schedule_id"));
      record.setTimeId(rs.getLong("time_id"));
      record.setHour(rs.getInt("hour"));
      record.setMinute(rs.getInt("minute"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
