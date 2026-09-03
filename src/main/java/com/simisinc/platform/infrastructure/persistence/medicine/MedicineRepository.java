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
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.medicine.Medicine;
import com.simisinc.platform.domain.model.medicine.MedicineSchedule;
import com.simisinc.platform.domain.model.medicine.Prescription;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves medicine objects
 *
 * @author matt rajkowski
 * @created 8/28/18 10:49 AM
 */
public class MedicineRepository {

  private static Log LOG = LogFactory.getLog(MedicineRepository.class);

  private static String TABLE_NAME = "medicines";
  private static String[] PRIMARY_KEY = new String[] { "medicine_id" };

  public static Medicine save(Medicine record) {
    if (record.getId() > -1) {
      return update(record, null, null);
    }
    return add(record, null, null);
  }

  public static Medicine save(Medicine record, MedicineSchedule medicineSchedule, Prescription prescription) {
    if (record.getId() > -1) {
      return update(record, medicineSchedule, prescription);
    }
    return add(record, medicineSchedule, prescription);
  }

  private static Medicine add(Medicine record, MedicineSchedule medicineSchedule, Prescription prescription) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      long medicineId = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("individual_id", record.getIndividualId() == -1 ? null : record.getIndividualId())
          .FIELD("drug_id", record.getDrugId() == -1 ? null : record.getDrugId())
          .FIELD("drug_name", record.getDrugName())
          .FIELD("dosage", StringUtils.trimToNull(record.getDosage()))
          .FIELD("form_of_medicine", StringUtils.trimToNull(record.getFormOfMedicine()))
          .FIELD("appearance", StringUtils.trimToNull(record.getAppearance()))
          .FIELD("cost", record.getCost())
          .FIELD("pills_left", record.getQuantityOnHand() == 0 ? null : record.getQuantityOnHand())
          .FIELD("barcode", StringUtils.trimToNull(record.getBarcode()))
          .FIELD("condition", StringUtils.trimToNull(record.getCondition()))
          .FIELD("comments", StringUtils.trimToNull(record.getComments()))
          .FIELD("created_by", record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy())
          .FIELD("assigned_to", record.getAssignedTo() == -1 ? null : record.getAssignedTo())
          .FIELD("suspended", record.getSuspended())
          .FIELD("suspended_by", record.getSuspendedBy() == -1 ? null : record.getSuspendedBy())
          .FIELD("archived", record.getArchived())
          .FIELD("archived_by", record.getArchivedBy() == -1 ? null : record.getArchivedBy())
          .FIELD("last_taken", record.getLastTaken())
          .FIELD("last_administered_by", record.getLastAdministeredBy() == -1 ? null : record.getLastAdministeredBy())
          .execute(connection);
      record.setId(medicineId);
      if (medicineSchedule != null) {
        medicineSchedule.setMedicineId(record.getId());
        MedicineScheduleRepository.save(connection, medicineSchedule);
      }
      if (prescription != null) {
        if (!prescription.isEmpty()) {
          prescription.setMedicineId(record.getId());
          PrescriptionRepository.save(connection, prescription);
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

  private static Medicine update(Medicine record, MedicineSchedule medicineSchedule, Prescription prescription) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      if (medicineSchedule != null) {
        // Remove the references, but keep the records
        MedicineLogRepository.removeReferences(connection, record);
        // Delete the references
        MedicineReminderRepository.removeAll(connection, record);
        // Update the related data
        MedicineTimeRepository.removeAll(connection, record);
        MedicineScheduleRepository.removeAll(connection, record);
        medicineSchedule.setMedicineId(record.getId());
        MedicineScheduleRepository.save(connection, medicineSchedule);
      }
      if (prescription != null) {
        if (!prescription.isEmpty()) {
          PrescriptionRepository.removeAll(connection, record);
          prescription.setMedicineId(record.getId());
          PrescriptionRepository.save(connection, prescription);
        }
      }
      // Update this record
      DB.UPDATE(TABLE_NAME)
          .SET("dosage", StringUtils.trimToNull(record.getDosage()))
          .SET("form_of_medicine", StringUtils.trimToNull(record.getFormOfMedicine()))
          .SET("appearance", StringUtils.trimToNull(record.getAppearance()))
          .SET("pills_left", record.getQuantityOnHand() == 0 ? null : record.getQuantityOnHand())
          .SET("cost", record.getCost())
          .SET("barcode", StringUtils.trimToNull(record.getBarcode()))
          .SET("condition", StringUtils.trimToNull(record.getCondition()))
          .SET("comments", StringUtils.trimToNull(record.getComments()))
          .SET("modified_by", record.getModifiedBy())
          .WHERE("medicine_id = ?", record.getId())
          .execute(connection);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Medicine record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      PrescriptionRepository.removeAll(connection, record);
      MedicineLogRepository.removeAll(connection, record);
      MedicineReminderRepository.removeAll(connection, record);
      MedicineTimeRepository.removeAll(connection, record);
      MedicineScheduleRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("medicine_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Item item) throws SQLException {
    // @todo Delete the references
    //    PrescriptionRepository.removeAll(connection, item);
    //    MedicineLogRepository.removeAll(connection, item);
    //    MedicineReminderRepository.removeAll(connection, item);
    //    MedicineTimeRepository.removeAll(connection, item);
    //    MedicineScheduleRepository.removeAll(connection, item);
    // Delete the records
    DB.DELETE().FROM(TABLE_NAME).WHERE("individual_id = ?", item.getId()).execute(connection);
  }

  public static boolean markAsSuspended(Medicine record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Suspend the medicine
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      DB.UPDATE(TABLE_NAME)
          .SET("modified_by", record.getModifiedBy())
          .SET("modified", timestamp)
          .SET("suspended_by", record.getModifiedBy())
          .SET("suspended", timestamp)
          .WHERE("medicine_id = ?", record.getId())
          .execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static boolean markAsResumed(Medicine record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Suspend the medicine
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      DB.UPDATE(TABLE_NAME)
          .SET("modified_by", record.getModifiedBy())
          .SET("modified", timestamp)
          .SET("suspended_by", (Long) null)
          .SET("suspended", (Timestamp) null)
          .WHERE("medicine_id = ?", record.getId())
          .execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static boolean markAsArchived(Medicine record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Archive the medicine
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      DB.UPDATE(TABLE_NAME)
          .SET("modified_by", record.getModifiedBy())
          .SET("modified", timestamp)
          .SET("archived_by", record.getModifiedBy())
          .SET("archived", timestamp)
          .WHERE("medicine_id = ?", record.getId())
          .execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static DataResult<Medicine> query(MedicineSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getMinMedicineId() != -1) {
        select.AND("medicine_id >= ?", specification.getMinMedicineId());
      }
      if (specification.getId() != -1) {
        select.AND("medicine_id = ?", specification.getId());
      }
      if (specification.getIndividualId() != -1) {
        select.AND("individual_id = ?", specification.getIndividualId());
      }
      if (StringUtils.isNotBlank(specification.getBarcode())) {
        select.AND("barcode = ?", specification.getBarcode());
      }
      if (specification.getArchivedOnly() == DataConstants.TRUE) {
        select.AND("archived IS NOT NULL");
      } else if (specification.getArchivedOnly() == DataConstants.FALSE) {
        select.AND("archived IS NULL");
      }
      if (specification.getSuspendedOnly() == DataConstants.TRUE) {
        select.AND("suspended IS NOT NULL");
      } else if (specification.getSuspendedOnly() == DataConstants.FALSE) {
        select.AND("suspended IS NULL");
      }
    }
    select.WITH(constraints);
    return select.returnDataResult(MedicineRepository::buildRecord);
  }

  public static Medicine findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT().FROM(TABLE_NAME)
        .WHERE("medicine_id = ?", id)
        .returnRecord(MedicineRepository::buildRecord);
  }

  public static List<Medicine> findAll(MedicineSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("medicine_id");
    DataResult result = query(specification, constraints);
    return (List<Medicine>) result.getRecords();
  }

  private static Medicine buildRecord(ResultSet rs) {
    try {
      Medicine record = new Medicine();
      record.setId(rs.getLong("medicine_id"));
      record.setIndividualId(rs.getLong("individual_id"));
      record.setDrugId(rs.getLong("drug_id"));
      record.setDrugName(rs.getString("drug_name"));
      record.setDosage(rs.getString("dosage"));
      record.setFormOfMedicine(rs.getString("form_of_medicine"));
      record.setAppearance(rs.getString("appearance"));
      record.setCost(rs.getBigDecimal("cost"));
      record.setQuantityOnHand(rs.getInt("pills_left"));
      record.setBarcode(rs.getString("barcode"));
      record.setCondition(rs.getString("condition"));
      record.setComments(rs.getString("comments"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setAssignedTo(rs.getLong("assigned_to"));
      record.setSuspended(rs.getTimestamp("suspended"));
      record.setSuspendedBy(rs.getLong("suspended_by"));
      record.setArchived(rs.getTimestamp("archived"));
      record.setArchivedBy(rs.getLong("archived_by"));
      record.setLastTaken(rs.getTimestamp("last_taken"));
      record.setLastAdministeredBy(rs.getLong("last_administered_by"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
