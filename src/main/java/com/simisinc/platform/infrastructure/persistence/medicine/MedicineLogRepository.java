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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Field;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.medicine.Medicine;
import com.simisinc.platform.domain.model.medicine.MedicineLog;

/**
 * Persists and retrieves medicine log objects
 *
 * @author matt rajkowski
 * @created 9/18/18 9:44 AM
 */
public class MedicineLogRepository {

  private static Log LOG = LogFactory.getLog(MedicineLogRepository.class);

  private static String TABLE_NAME = "medicine_log";
  private static String[] PRIMARY_KEY = new String[] { "log_id" };

  private static DataResult<MedicineLog> query(MedicineLogSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME);
    if (specification != null) {
      select.LEFT_JOIN("medicines medicines").ON("medicine_log.medicine_id = medicines.medicine_id").WHERE();
      if (specification.getId() != -1) {
        select.AND("log_id = ?", specification.getId());
      }
      if (specification.getIndividualId() != -1) {
        select.AND("medicine_log.individual_id = ?", specification.getIndividualId());
      }
      if (specification.getMedicineId() != -1) {
        select.AND("medicine_log.medicine_id = ?", specification.getMedicineId());
      }
      if (specification.getMinDate() != null) {
        select.AND("administered >= ?", specification.getMinDate());
      }
      if (specification.getMaxDate() != null) {
        select.AND("administered < ?", specification.getMaxDate());
      }
      if (specification.getIndividualsList() != null && !specification.getIndividualsList().isEmpty()) {
        StringBuilder sb = new StringBuilder();
        for (Long id : specification.getIndividualsList()) {
          if (sb.length() > 0) {
            sb.append(",");
          }
          sb.append(id);
        }
        select.AND("medicine_log.individual_id IN (" + sb.toString() + ")");
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(MedicineLogRepository::buildRecord);
  }

  public static List<MedicineLog> findAll(MedicineLogSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("reminder_id");
    return query(specification, constraints).getRecords();
  }

  public static MedicineLog findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("log_id = ?", id)
        .returnRecord(MedicineLogRepository::buildRecord);
  }

  public static MedicineLog save(MedicineLog record) {
    if (record.getId() > -1) {
      // Not supported
      return null;
    }
    return add(record);
  }

  private static MedicineLog add(MedicineLog record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("medicine_id", record.getMedicineId())
        .FIELD("individual_id", record.getIndividualId() != -1 ? record.getIndividualId() : null)
        .FIELD("reminder_id", record.getReminderId() != -1 ? record.getReminderId() : null)
        .FIELD("reminder_date", record.getReminderDate())
        .FIELD("drug_id", record.getDrugId() != -1 ? record.getDrugId() : null)
        .FIELD("drug_name", record.getDrugName())
        .FIELD("dosage", StringUtils.trimToNull(record.getDosage()))
        .FIELD("form_of_medicine", StringUtils.trimToNull(record.getFormOfMedicine()))
        .FIELD("quantity", record.getQuantityGiven())
        .FIELD("comments", StringUtils.trimToNull(record.getComments()))
        .FIELD("pills_left", record.getPillsLeft() != -1 ? record.getPillsLeft() : null)
        .FIELD("administered_by", record.getAdministeredBy())
        .FIELD("administered", record.getAdministered())
        .FIELD("was_taken", record.getWasTaken())
        .FIELD("taken_on_time", record.getTakenOnTime())
        .FIELD("was_skipped", record.getWasSkipped())
        .FIELD("reason_comments", StringUtils.trimToNull(record.getReasonComments()));
    if (record.getReasonCode() == MedicineLog.REASON_INDIVIDUAL_UNAVAILABLE) {
      insert.FIELD("reason_individual", true);
    } else if (record.getReasonCode() == MedicineLog.REASON_CAREGIVER_UNAVAILABLE) {
      insert.FIELD("reason_caregiver", true);
    } else if (record.getReasonCode() == MedicineLog.REASON_MEDICINE_UNAVAILABLE) {
      insert.FIELD("reason_medicine", true);
    } else if (record.getReasonCode() == MedicineLog.REASON_REFUSED) {
      insert.FIELD("reason_refused", true);
    } else if (record.getReasonCode() == MedicineLog.REASON_HEALTH_CONCERNS) {
      insert.FIELD("reason_health_concerns", true);
    } else if (record.getReasonCode() == MedicineLog.REASON_RAN_OUT) {
      insert.FIELD("reason_med_ran_out", true);
    } else if (record.getReasonCode() == MedicineLog.REASON_DOSE_NOT_NEEDED) {
      insert.FIELD("reason_dose_not_needed", true);
    } else if (record.getReasonCode() == MedicineLog.REASON_OTHER) {
      insert.FIELD("reason_other_concern", true);
    }

    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      record.setId(insert.execute(connection));
      if (record.getReminderId() > -1) {
        if (record.getWasTaken()) {
          MedicineReminderRepository.markAsTaken(connection, record.getReminderId(), record.getAdministered());
        } else if (record.getWasSkipped()) {
          MedicineReminderRepository.markAsSkipped(connection, record.getReminderId());
        }
      }
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  public static void removeAll(Connection connection, Medicine record) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("medicine_id = ?", record.getId()).execute(connection);
  }

  public static void removeReferences(Connection connection, Medicine record) throws SQLException {
    DB.UPDATE(TABLE_NAME)
        .SET(new Field("reminder_id", -1L, true))
        .WHERE("medicine_id = ?", record.getId())
        .execute(connection);
  }

  public static boolean remove(MedicineLog record) {
    try {
      try (Connection connection = DB.getConnection();
          AutoStartTransaction a = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {
        // Delete the references
        // Delete the record
        DB.DELETE().FROM(TABLE_NAME).WHERE("log_id = ?", record.getId()).execute(connection);
        // Finish transaction
        transaction.commit();
        return true;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static MedicineLog buildRecord(ResultSet rs) {
    try {
      MedicineLog record = new MedicineLog();
      record.setId(rs.getLong("log_id"));
      record.setMedicineId(rs.getLong("medicine_id"));
      record.setIndividualId(rs.getLong("individual_id"));
      record.setReminderId(rs.getLong("reminder_id"));
      record.setReminderDate(rs.getTimestamp("reminder_date"));
      record.setDrugId(rs.getLong("drug_id"));
      record.setDrugName(rs.getString("drug_name"));
      record.setDosage(rs.getString("dosage"));
      record.setFormOfMedicine(rs.getString("form_of_medicine"));
      record.setQuantityGiven(rs.getInt("quantity"));
      record.setComments(rs.getString("comments"));
      record.setQuantityGiven(rs.getInt("pills_left"));
      record.setAdministeredBy(rs.getLong("administered_by"));
      record.setAdministered(rs.getTimestamp("administered"));
      record.setWasTaken(rs.getBoolean("was_taken"));
      record.setWasSkipped(rs.getBoolean("was_skipped"));
      record.setTakenOnTime(rs.getBoolean("taken_on_time"));
      if (rs.getBoolean("reason_refused")) {
        record.setReasonCode(MedicineLog.REASON_REFUSED);
      } else if (rs.getBoolean("reason_individual")) {
        record.setReasonCode(MedicineLog.REASON_INDIVIDUAL_UNAVAILABLE);
      } else if (rs.getBoolean("reason_caregiver")) {
        record.setReasonCode(MedicineLog.REASON_CAREGIVER_UNAVAILABLE);
      } else if (rs.getBoolean("reason_medicine")) {
        record.setReasonCode(MedicineLog.REASON_MEDICINE_UNAVAILABLE);
      } else if (rs.getBoolean("reason_med_ran_out")) {
        record.setReasonCode(MedicineLog.REASON_RAN_OUT);
      } else if (rs.getBoolean("reason_dose_not_needed")) {
        record.setReasonCode(MedicineLog.REASON_DOSE_NOT_NEEDED);
      } else if (rs.getBoolean("reason_health_concerns")) {
        record.setReasonCode(MedicineLog.REASON_HEALTH_CONCERNS);
      } else if (rs.getBoolean("reason_other_concern")) {
        record.setReasonCode(MedicineLog.REASON_OTHER);
      }
      record.setReasonComments(rs.getString("reason_comments"));
      record.setCreated(rs.getTimestamp("created"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
