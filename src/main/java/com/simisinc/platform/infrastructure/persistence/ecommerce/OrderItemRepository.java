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

import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.CANCELED;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.PAID;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.PREPARING;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.REFUNDED;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.SHIPPED;

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
import com.simisinc.platform.application.ecommerce.OrderStatusCommand;
import com.simisinc.platform.domain.model.ecommerce.Order;
import com.simisinc.platform.domain.model.ecommerce.OrderItem;

/**
 * Persists and retrieves order item objects
 *
 * @author matt rajkowski
 * @created 4/23/20 2:16 PM
 */
public class OrderItemRepository {

  private static Log LOG = LogFactory.getLog(OrderItemRepository.class);

  private static String TABLE_NAME = "order_items";
  private static String[] PRIMARY_KEY = new String[] { "item_id" };

  public static List<OrderItem> findItemsByOrderId(long orderId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("order_id = ?", orderId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("item_id").setUseCount(false))
        .returnDataResult(OrderItemRepository::buildRecord).getRecords();
  }

  public static OrderItem findById(long itemId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", itemId)
        .returnRecord(OrderItemRepository::buildRecord);
  }

  public static OrderItem add(OrderItem record) throws SQLException {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Save it
      add(connection, record);
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static OrderItem add(Connection connection, OrderItem record) throws SQLException {
    record.setId(DB.INSERT().INTO(TABLE_NAME)
        .FIELD("order_id", record.getOrderId() == -1 ? null : record.getOrderId())
        .FIELD_UNLESS_MATCHES("customer_id", record.getCustomerId(), -1)
        .FIELD_UNLESS_MATCHES("product_id", record.getProductId(), -1)
        .FIELD_UNLESS_MATCHES("sku_id", record.getSkuId(), -1)
        .FIELD_UNLESS_NULL("quantity", record.getQuantity())
        .FIELD_UNLESS_NULL("currency", record.getCurrency())
        .FIELD_UNLESS_NULL("each_amount", record.getEachAmount())
        .FIELD_UNLESS_NULL("total_amount", record.getTotalAmount())
        .FIELD_UNLESS_NULL("product_name", record.getProductName())
        .FIELD_UNLESS_NULL("product_type", record.getProductType())
        .FIELD_UNLESS_NULL("product_sku", record.getProductSku())
        .FIELD("is_preorder", record.getPreorder())
        .FIELD("is_backordered", record.getBackordered())
        .FIELD("paid", record.getPaid())
        .FIELD("processed", record.getProcessed())
        .FIELD("shipped", record.getShipped())
        .FIELD("canceled", record.getCanceled())
        .FIELD("refunded", record.getRefunded())
        .FIELD("created", record.getCreated())
        .FIELD("created_by", record.getCreatedBy() == -1 ? null : record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy())
        .FIELD_UNLESS_NULL("product_barcode", record.getProductBarcode())
        .FIELD("payment_date", record.getPaymentDate())
        .FIELD("processing_date", record.getProcessingDate())
        .FIELD("fulfillment_date", record.getFulfillmentDate())
        .FIELD("shipped_date", record.getShippedDate())
        .FIELD("canceled_date", record.getCanceledDate())
        .FIELD("refunded_date", record.getRefundedDate())
        .FIELD("status", record.getStatusId() == -1 ? null : record.getStatusId())
        .execute(connection));
    return record;
  }

  private static OrderItem buildRecord(ResultSet rs) {
    try {
      OrderItem record = new OrderItem();
      record.setId(rs.getLong("item_id"));
      record.setOrderId(rs.getLong("order_id"));
      record.setCustomerId(DB.getLong(rs, "customer_id", -1));
      record.setProductId(DB.getLong(rs, "product_id", -1));
      record.setSkuId(DB.getLong(rs, "sku_id", -1));
      record.setQuantity(rs.getBigDecimal("quantity"));
      record.setCurrency(rs.getString("currency"));
      record.setEachAmount(rs.getBigDecimal("each_amount"));
      record.setTotalAmount(rs.getBigDecimal("total_amount"));
      record.setProductName(rs.getString("product_name"));
      record.setProductType(rs.getString("product_type"));
      record.setProductSku(rs.getString("product_sku"));
      record.setPreorder(rs.getBoolean("is_preorder"));
      record.setBackordered(rs.getBoolean("is_backordered"));
      record.setPaid(rs.getBoolean("paid"));
      record.setProcessed(rs.getBoolean("processed"));
      record.setShipped(rs.getBoolean("shipped"));
      record.setCanceled(rs.getBoolean("canceled"));
      record.setRefunded(rs.getBoolean("refunded"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(DB.getLong(rs, "modified_by", -1));
      record.setProductBarcode(rs.getString("product_barcode"));
      record.setPaymentDate(rs.getTimestamp("payment_date"));
      record.setProcessingDate(rs.getTimestamp("processing_date"));
      record.setFulfillmentDate(rs.getTimestamp("fulfillment_date"));
      record.setShippedDate(rs.getTimestamp("shipped_date"));
      record.setCanceledDate(rs.getTimestamp("canceled_date"));
      record.setRefundedDate(rs.getTimestamp("refunded_date"));
      record.setStatusId(DB.getInt(rs, "status", -1));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  public static void markStatusAsPaid(Connection connection, OrderItem orderItem, Timestamp paymentDate) throws SQLException {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(PAID);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Update the order item status
    DB.UPDATE(TABLE_NAME)
        .SET("paid", true)
        .SET("payment_date", paymentDate)
        .SET("status", statusId)
        .SET("modified", now)
        .WHERE("item_id = ?", orderItem.getId())
        .execute(connection);
    // @todo Append to the order_history (PAID)
    // Update the object
    orderItem.setModified(now);
    orderItem.setPaid(true);
    orderItem.setPaymentDate(now);
    orderItem.setStatusId(statusId);
  }

  public static void markStatusAsPreparing(OrderItem orderItem) {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(PREPARING);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Update the order item status
    DB.UPDATE(TABLE_NAME)
        .SET("processed", true)
        .SET("processing_date", now)
        .SET("status", statusId)
        .SET("modified", now)
        .WHERE("item_id = ?", orderItem.getId())
        .execute();
    // @todo Append to the order_history (PREPARING)
    // Update the object
    orderItem.setModified(now);
    orderItem.setProcessed(true);
    orderItem.setProcessingDate(now);
    orderItem.setStatusId(statusId);
  }

  public static void markStatusAsShipped(OrderItem orderItem) {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(SHIPPED);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Update the order item status
    DB.UPDATE(TABLE_NAME)
        .SET("shipped", true)
        .SET("shipped_date", now)
        .SET("status", statusId)
        .SET("modified", now)
        .WHERE("item_id = ?", orderItem.getId())
        .execute();
    // @todo Append to the order_history (SHIPPED)
    // Update the object
    orderItem.setModified(now);
    orderItem.setShipped(true);
    orderItem.setShippedDate(now);
    orderItem.setStatusId(statusId);
  }

  public static void markStatusAsCanceled(Connection connection, Order order) throws SQLException {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(CANCELED);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Update the order item status
    DB.UPDATE(TABLE_NAME)
        .SET("canceled", true)
        .SET("canceled_date", now)
        .SET("status", statusId)
        .SET("modified", now)
        .WHERE("order_id = ?", order.getId())
        .execute(connection);
  }

  public static void markStatusAsRefunded(Connection connection, Order order) throws SQLException {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(REFUNDED);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Update the order item status
    DB.UPDATE(TABLE_NAME)
        .SET("refunded", true)
        .SET("refunded_date", now)
        .SET("status", statusId)
        .SET("modified", now)
        .WHERE("order_id = ?", order.getId())
        .execute(connection);
  }
}
