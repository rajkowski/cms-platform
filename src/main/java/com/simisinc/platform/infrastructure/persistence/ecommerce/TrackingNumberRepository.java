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
import com.simisinc.platform.domain.model.ecommerce.TrackingNumber;

/**
 * Persists and retrieves tracking number objects
 *
 * @author matt rajkowski
 * @created 4/22/20 7:59 PM
 */
public class TrackingNumberRepository {

  private static Log LOG = LogFactory.getLog(TrackingNumberRepository.class);

  private static String TABLE_NAME = "order_tracking_numbers";
  private static String[] PRIMARY_KEY = new String[] { "tracking_id" };

  public static TrackingNumber findById(long trackingId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("tracking_id = ?", trackingId)
        .returnRecord(TrackingNumberRepository::buildRecord);
  }

  public static List<TrackingNumber> findAllForOrderId(long orderId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("order_id = ?", orderId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("created").setUseCount(false))
        .returnDataResult(TrackingNumberRepository::buildRecord).getRecords();
  }

  public static boolean exists(TrackingNumber record) {
    return DB.SELECT().COUNT("*")
        .FROM(TABLE_NAME)
        .WHERE("order_id = ?", record.getOrderId())
        .AND("tracking_number = ?", StringUtils.trimToNull(record.getTrackingNumber()))
        .returnCount() > 0;
  }

  public static TrackingNumber save(TrackingNumber record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  /** Save the tracking number, update the order */
  private static TrackingNumber add(TrackingNumber record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      Insert insert = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("order_id", record.getOrderId())
          .FIELD("tracking_number", StringUtils.trimToNull(record.getTrackingNumber()))
          .FIELD_UNLESS_MATCHES("shipping_carrier", record.getShippingCarrierId(), -1)
          .FIELD_UNLESS_MATCHES("created_by", record.getCreatedBy(), -1)
          .FIELD_UNLESS_NULL("ship_date", record.getShipDate())
          .FIELD_UNLESS_NULL("delivery_date", record.getDeliveryDate())
          .FIELD("cart_item_id_list", StringUtils.trimToNull(record.getCartItemIdList()))
          .FIELD("order_item_id_list", StringUtils.trimToNull(record.getOrderItemIdList()));
      record.setId(insert.execute(connection));
      updateOrderField(connection, record);
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  /** Save the tracking number, update the order **/
  private static TrackingNumber update(TrackingNumber record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      Update update = DB.UPDATE(TABLE_NAME)
          .SET("tracking_number", StringUtils.trimToNull(record.getTrackingNumber()))
          .SET_UNLESS_MATCHES("shipping_carrier", record.getShippingCarrierId(), -1)
          .SET_UNLESS_NULL("ship_date", record.getShipDate())
          .SET_UNLESS_NULL("delivery_date", record.getDeliveryDate())
          .SET("cart_item_id_list", StringUtils.trimToNull(record.getCartItemIdList()))
          .SET("order_item_id_list", StringUtils.trimToNull(record.getOrderItemIdList()))
          .WHERE("tracking_id = ?", record.getId());
      if (update.execute(connection).booleanValue()) {
        updateOrderField(connection, record);
        transaction.commit();
        return record;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  /** Update the order field */
  private static void updateOrderField(Connection connection, TrackingNumber record) throws SQLException {
    DB.UPDATE("orders")
        .SET(
            "tracking_numbers = sub_q.agg_value FROM (SELECT string_agg(tracking_number, ',') AS agg_value " +
                "FROM order_tracking_numbers AS tn WHERE tn.order_id = ?) AS sub_q",
            record.getOrderId())
        .WHERE("orders.order_id = ?", record.getOrderId())
        .execute(connection);
  }

  private static TrackingNumber buildRecord(ResultSet rs) {
    try {
      TrackingNumber record = new TrackingNumber();
      record.setId(rs.getLong("tracking_id"));
      record.setOrderId(rs.getLong("order_id"));
      record.setTrackingNumber(rs.getString("tracking_number"));
      record.setShippingCarrierId(rs.getInt("shipping_carrier"));
      record.setShipDate(rs.getTimestamp("ship_date"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setDeliveryDate(rs.getTimestamp("delivery_date"));
      record.setCartItemIdList(rs.getString("cart_item_id_list"));
      record.setOrderItemIdList(rs.getString("order_item_id_list"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
