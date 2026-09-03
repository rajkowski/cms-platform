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

import static com.simisinc.platform.application.ecommerce.OrderCommand.generateUniqueId;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.CANCELED;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.PARTIALLY_PREPARED;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.PARTIALLY_SHIPPED;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.PREPARING;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.REFUNDED;
import static com.simisinc.platform.application.ecommerce.OrderStatusCommand.SHIPPED;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
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
import com.simisinc.platform.application.ecommerce.OrderItemCommand;
import com.simisinc.platform.application.ecommerce.OrderStatusCommand;
import com.simisinc.platform.domain.model.Session;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.dashboard.StatisticsData;
import com.simisinc.platform.domain.model.ecommerce.Address;
import com.simisinc.platform.domain.model.ecommerce.CartItem;
import com.simisinc.platform.domain.model.ecommerce.Order;
import com.simisinc.platform.domain.model.ecommerce.OrderItem;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves order objects
 *
 * @author matt rajkowski
 * @created 5/2/19 6:45 AM
 */
public class OrderRepository {

  private static Log LOG = LogFactory.getLog(OrderRepository.class);

  private static String TABLE_NAME = "orders";
  private static String[] PRIMARY_KEY = new String[] { "order_id" };

  private static DataResult<Order> query(OrderSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() != -1) {
        select.AND("order_id = ?", specification.getId());
      }
      if (specification.getCustomerId() != -1) {
        select.AND("customer_id = ?", specification.getCustomerId());
      }
      if (specification.getEmail() != null) {
        select.AND("LOWER(email) = ?", specification.getEmail().toLowerCase());
      }
      if (specification.getCreatedBy() != -1) {
        select.AND("created_by = ?", specification.getCreatedBy());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("(LOWER(order_unique_id) = LOWER(?) OR LOWER(order_unique_id) LIKE LOWER(?))",
            specification.getUniqueId(), specification.getUniqueId() + "%");
      }
      if (StringUtils.isNotBlank(specification.getCustomerNumber())) {
        select.AND(
            "EXISTS (SELECT 1 FROM customers WHERE orders.customer_id = customers.customer_id AND LOWER(customer_unique_id) = ?)",
            specification.getCustomerNumber().toLowerCase());
      }
      if (StringUtils.isNotBlank(specification.getPhoneNumber())) {
        select.AND("(billing_phone_number = ? OR shipping_phone_number = ?)",
            specification.getPhoneNumber(), specification.getPhoneNumber());
      }
      if (StringUtils.isNotBlank(specification.getName())) {
        select.AND(
            "(LOWER(concat_ws(' ', first_name, last_name)) LIKE LOWER(?) ESCAPE '!' OR LOWER(concat_ws(' ', shipping_first_name, shipping_last_name)) LIKE LOWER(?) ESCAPE '!')",
            "%" + specification.getName() + "%", "%" + specification.getName() + "%");
      }
      if (specification.getShowSandbox() != DataConstants.UNDEFINED) {
        select.AND("live_mode = ?", specification.getShowSandbox() == DataConstants.FALSE);
      }
      if (specification.getShowIncompleteOrders() != DataConstants.UNDEFINED) {
        select.AND("paid = ?", specification.getShowIncompleteOrders() == DataConstants.FALSE);
      }
      if (specification.getShowCanceledOrders() != DataConstants.UNDEFINED) {
        select.AND("canceled = ?", specification.getShowCanceledOrders() == DataConstants.TRUE);
      }
      if (specification.getShowProcessedOrders() != DataConstants.UNDEFINED) {
        select.AND("processed = ?", specification.getShowProcessedOrders() == DataConstants.TRUE);
      }
      if (specification.getShowShippedOrders() != DataConstants.UNDEFINED) {
        select.AND("shipped = ?", specification.getShowShippedOrders() == DataConstants.TRUE);
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(OrderRepository::buildRecord);
  }

  public static Order findById(long orderId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("order_id = ?", orderId)
        .returnRecord(OrderRepository::buildRecord);
  }

  public static Order findByUniqueId(String orderUniqueId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("order_unique_id = ?", orderUniqueId)
        .returnRecord(OrderRepository::buildRecord);
  }

  public static List<Order> findAll(OrderSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("order_id desc");
    return query(specification, constraints).getRecords();
  }

  public static List<Session> findDailyUniqueLocations(int daysToLimit) {
    String SQL_QUERY = "SELECT DISTINCT shipping_country AS country, " +
        "shipping_state AS state, " +
        "shipping_city AS city, " +
        "latitude, longitude " +
        "FROM orders " +
        "WHERE payment_date IS NOT NULL " +
        "AND payment_date > NOW() - INTERVAL '" + daysToLimit + " days' " +
        "AND latitude IS NOT NULL " +
        "AND live_mode = true AND processed = true " +
        "ORDER BY country, state, city, latitude, longitude";
    List<Session> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        Session data = new Session();
        //        data.setContinent(rs.getString("continent"));
        data.setCountry(rs.getString("country"));
        data.setState(rs.getString("state"));
        data.setCity(rs.getString("city"));
        data.setLatitude(rs.getDouble("latitude"));
        data.setLongitude(rs.getDouble("longitude"));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findTopLocations(int daysToLimit, int recordLimit) {
    String SQL_QUERY = "SELECT UPPER(shipping_country) AS country, " +
        "UPPER(shipping_state) AS state, " +
        "COUNT(order_id) AS location_count " +
        "FROM orders " +
        "WHERE payment_date IS NOT NULL " +
        "AND payment_date > NOW() - INTERVAL '" + daysToLimit + " days' " +
        "AND live_mode = true AND processed = true " +
        "GROUP BY UPPER(shipping_country), UPPER(shipping_state) " +
        "ORDER BY location_count desc " +
        "LIMIT " + recordLimit;
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        String label = rs.getString("country") + ", " +
            rs.getString("state");
        data.setLabel(label);
        data.setValue(String.valueOf(rs.getLong("location_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findDailyOrdersCount(int daysToLimit) {
    String SQL_QUERY = "SELECT DATE_TRUNC('day', day)::VARCHAR(10) AS date_column, COUNT(order_id) AS daily_count " +
        "FROM (SELECT generate_series(NOW() - INTERVAL '" + daysToLimit + " days', NOW(), INTERVAL '1 day')::date) d(day) " +
        "LEFT JOIN orders ON DATE_TRUNC('day', payment_date) = DATE_TRUNC('day', day) AND live_mode = true AND processed = true " +
        "GROUP BY d.day " +
        "ORDER BY d.day";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("date_column"));
        data.setValue(String.valueOf(rs.getLong("daily_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findDailyItemsSold(int daysToLimit) {
    String SQL_QUERY = "SELECT DATE_TRUNC('day', day)::VARCHAR(10) AS date_column, SUM(total_items) AS daily_count " +
        "FROM (SELECT generate_series(NOW() - INTERVAL '" + daysToLimit + " days', NOW(), INTERVAL '1 day')::date) d(day) " +
        "LEFT JOIN orders ON DATE_TRUNC('day', payment_date) = DATE_TRUNC('day', day) AND live_mode = true AND processed = true " +
        "GROUP BY d.day " +
        "ORDER BY d.day";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("date_column"));
        data.setValue(String.valueOf(rs.getLong("daily_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static List<StatisticsData> findDailyAmountSold(int daysToLimit) {
    String SQL_QUERY = "SELECT DATE_TRUNC('day', day)::VARCHAR(10) AS date_column, SUM(total_paid) AS daily_count " +
        "FROM (SELECT generate_series(NOW() - INTERVAL '" + daysToLimit + " days', NOW(), INTERVAL '1 day')::date) d(day) " +
        "LEFT JOIN orders ON DATE_TRUNC('day', payment_date) = DATE_TRUNC('day', day) AND live_mode = true AND processed = true " +
        "GROUP BY d.day " +
        "ORDER BY d.day";
    List<StatisticsData> records = null;
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      records = new ArrayList<>();
      while (rs.next()) {
        StatisticsData data = new StatisticsData();
        data.setLabel(rs.getString("date_column"));
        data.setValue(String.valueOf(rs.getLong("daily_count")));
        records.add(data);
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return records;
  }

  public static long countTotalOrders(int daysToLimit) {
    long count = -1;
    String SQL_QUERY = "SELECT COUNT(order_id) AS order_count " +
        "FROM orders " +
        "WHERE live_mode = true AND paid = true AND canceled = false " +
        "AND created > NOW() - INTERVAL '" + daysToLimit + " days'";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      if (rs.next()) {
        count = rs.getLong("order_count");
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return count;
  }

  public static long countTotalOrders() {
    long count = -1;
    String SQL_QUERY = "SELECT COUNT(order_id) AS order_count " +
        "FROM orders " +
        "WHERE live_mode = true AND paid = true AND canceled = false";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      if (rs.next()) {
        count = rs.getLong("order_count");
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return count;
  }

  public static long countTotalOrdersNotShipped() {
    long count = -1;
    String SQL_QUERY = "SELECT COUNT(order_id) AS order_count " +
        "FROM orders " +
        "WHERE live_mode = true AND paid = true AND shipped = false AND canceled = false";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      if (rs.next()) {
        count = rs.getLong("order_count");
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return count;
  }

  public static long countTotalOrdersShipped() {
    long count = -1;
    String SQL_QUERY = "SELECT COUNT(order_id) AS order_count " +
        "FROM orders " +
        "WHERE live_mode = true AND paid = true AND processed = true AND shipped = true";
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      if (rs.next()) {
        count = rs.getLong("order_count");
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return count;
  }

  public static Order create(Order record, List<CartItem> cartItemList) {
    return add(record, cartItemList);
  }

  public static Order save(Order record) {
    if (record.getId() > -1) {
      return update(record);
    } else {
      return add(record, null);
    }
  }

  public static Order add(Order record, List<CartItem> cartItemList) {
    // Generate the order number
    record.setUniqueId(generateUniqueId());
    // Save the order
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      Insert insert = DB.INSERT().INTO(TABLE_NAME)
          .FIELD("order_unique_id", record.getUniqueId())
          .FIELD_UNLESS_MATCHES("customer_id", record.getCustomerId(), -1)
          .FIELD("email", record.getEmail())
          .FIELD("first_name", record.getFirstName())
          .FIELD("last_name", record.getLastName())
          .FIELD("customer_note", record.getCustomerNote())
          .FIELD("barcode", record.getBarcode())
          .FIELD("remote_order_id", record.getRemoteOrderId())
          .FIELD("shipping_method", record.getShippingMethodId() == -1 ? null : record.getShippingMethodId())
          .FIELD("shipping_rate_id", record.getShippingRateId() == -1 ? null : record.getShippingRateId())
          .FIELD("total_items", record.getTotalItems())
          .FIELD_UNLESS_NULL("currency", record.getCurrency())
          .FIELD("subtotal_amount", record.getSubtotalAmount())
          .FIELD_UNLESS_NULL("discount_amount", record.getDiscountAmount())
          .FIELD("promo_code", record.getPromoCode())
          .FIELD("pricing_rule_1", record.getPricingRuleId() == -1 ? null : record.getPricingRuleId())
          .FIELD_UNLESS_NULL("fee_amount", record.getHandlingFee())
          .FIELD_UNLESS_NULL("fee_tax_amount", record.getHandlingFeeTaxAmount())
          .FIELD_UNLESS_NULL("shipping_amount", record.getShippingFee())
          .FIELD_UNLESS_NULL("shipping_tax_amount", record.getShippingTaxAmount())
          .FIELD_UNLESS_NULL("tax_amount", record.getTaxAmount())
          .FIELD_UNLESS_NULL("tax_rate", record.getTaxRate())
          .FIELD_UNLESS_NULL("total_amount", record.getTotalAmount())
          .FIELD_UNLESS_NULL("total_paid", record.getTotalPaid())
          .FIELD_UNLESS_NULL("total_pending", record.getTotalPending())
          .FIELD_UNLESS_NULL("total_refunded", record.getTotalRefunded())
          .FIELD("status", record.getStatusId() == -1 ? null : record.getStatusId())
          .FIELD("has_preorder", record.getHasPreOrder())
          .FIELD("has_backorder", record.getHasBackOrder())
          .FIELD("paid", record.getPaid())
          .FIELD("processed", record.getProcessed())
          .FIELD("shipped", record.getShipped())
          .FIELD("canceled", record.getCanceled())
          .FIELD("refunded", record.getRefunded())
          .FIELD("tax_id", record.getTaxId())
          .FIELD("cart_id", record.getCartId())
          .FIELD("payment_processor", record.getPaymentProcessor())
          .FIELD("payment_token", record.getPaymentToken())
          .FIELD("payment_type", record.getPaymentType())
          .FIELD("payment_brand", record.getPaymentBrand())
          .FIELD("payment_last4", record.getPaymentLast4())
          .FIELD("payment_fingerprint", record.getPaymentFingerprint())
          .FIELD("payment_country", record.getPaymentCountry())
          .FIELD("charge_token", record.getChargeToken())
          .FIELD("ip_address", record.getIpAddress())
          .FIELD("session_id", record.getSessionId())
          .FIELD("country_iso", record.getCountryIso())
          .FIELD("country", record.getCountry())
          .FIELD("city", record.getCity())
          .FIELD("state_iso", record.getStateIso())
          .FIELD("state", record.getState())
          .FIELD("latitude", record.getLatitude())
          .FIELD("longitude", record.getLongitude())
          .FIELD("payment_date", record.getPaymentDate())
          .FIELD("processing_date", record.getProcessingDate())
          .FIELD("fulfillment_date", record.getFulfillmentDate())
          .FIELD("shipped_date", record.getShippedDate())
          .FIELD("canceled_date", record.getCanceledDate())
          .FIELD("refunded_date", record.getRefundedDate())
          .FIELD_UNLESS_NULL("tracking_numbers", record.getTrackingNumbers())
          .FIELD_UNLESS_NULL("square_order_id", record.getSquareOrderId())
          .FIELD_UNLESS_MATCHES("created_by", record.getCreatedBy(), -1)
          .FIELD_UNLESS_MATCHES("modified_by", record.getModifiedBy(), -1);
      if (record.getBillingAddress() != null) {
        insert
            .FIELD("billing_first_name", record.getBillingAddress().getFirstName())
            .FIELD("billing_last_name", record.getBillingAddress().getLastName())
            .FIELD("billing_organization", record.getBillingAddress().getOrganization())
            .FIELD("billing_street_address", record.getBillingAddress().getStreet())
            .FIELD("billing_address_line_2", record.getBillingAddress().getAddressLine2())
            .FIELD("billing_address_line_3", record.getBillingAddress().getAddressLine3())
            .FIELD("billing_city", record.getBillingAddress().getCity())
            .FIELD("billing_state", record.getBillingAddress().getState())
            .FIELD("billing_country", record.getBillingAddress().getCountry())
            .FIELD("billing_postal_code", record.getBillingAddress().getPostalCode())
            .FIELD("billing_county", record.getBillingAddress().getCounty())
            .FIELD("billing_phone_number", record.getBillingAddress().getPhoneNumber())
            .FIELD_UNLESS_MATCHES("billing_latitude", record.getBillingAddress().getLatitude(), 0)
            .FIELD_UNLESS_MATCHES("billing_longitude", record.getBillingAddress().getLongitude(), 0);
      }
      if (record.getShippingAddress() != null) {
        insert
            .FIELD("shipping_first_name", record.getShippingAddress().getFirstName())
            .FIELD("shipping_last_name", record.getShippingAddress().getLastName())
            .FIELD("shipping_organization", record.getShippingAddress().getOrganization())
            .FIELD("shipping_street_address", record.getShippingAddress().getStreet())
            .FIELD("shipping_address_line_2", record.getShippingAddress().getAddressLine2())
            .FIELD("shipping_address_line_3", record.getShippingAddress().getAddressLine3())
            .FIELD("shipping_city", record.getShippingAddress().getCity())
            .FIELD("shipping_state", record.getShippingAddress().getState())
            .FIELD("shipping_country", record.getShippingAddress().getCountry())
            .FIELD("shipping_postal_code", record.getShippingAddress().getPostalCode())
            .FIELD("shipping_county", record.getShippingAddress().getCounty())
            .FIELD("shipping_phone_number", record.getShippingAddress().getPhoneNumber())
            .FIELD_UNLESS_MATCHES("shipping_latitude", record.getShippingAddress().getLatitude(), 0)
            .FIELD_UNLESS_MATCHES("shipping_longitude", record.getShippingAddress().getLongitude(), 0);
      }
      record.setId(insert.execute(connection));
      // Update the cart's link
      if (record.getCartId() > 0) {
        DB.UPDATE("carts")
            .SET("order_id", record.getId())
            .SET("order_date", new Timestamp(System.currentTimeMillis()))
            .WHERE("cart_id = ?", record.getCartId())
            .execute(connection);
      }
      // Make a copy of the cart items
      if (cartItemList != null && !cartItemList.isEmpty()) {
        for (CartItem cartItem : cartItemList) {
          OrderItem orderItem = OrderItemCommand.generateOrderItem(record, cartItem);
          OrderItemRepository.add(connection, orderItem);
        }
      }
      // Finish the transaction
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  public static Order update(Order record) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      Update update = DB.UPDATE(TABLE_NAME);
      update
          .SET_UNLESS_MATCHES("customer_id", record.getCustomerId(), -1)
          .SET("email", record.getEmail())
          .SET("first_name", record.getFirstName())
          .SET("last_name", record.getLastName())
          .SET("barcode", record.getBarcode())
          .SET("remote_order_id", record.getRemoteOrderId())
          .SET_UNLESS_NULL("square_order_id", record.getSquareOrderId())
          .SET_WHEN_MATCHES("live_mode", record.getLive(), true)
          .SET_WHEN_MATCHES("paid", record.getPaid(), true)
          .SET_UNLESS_NULL("total_paid", record.getTotalPaid())
          .SET_WHEN_MATCHES("processed", record.getProcessed(), true)
          .SET_WHEN_MATCHES("shipped", record.getShipped(), true)
          .SET_WHEN_MATCHES("canceled", record.getCanceled(), true)
          .SET_WHEN_MATCHES("refunded", record.getRefunded(), true)
          .SET_UNLESS_MATCHES("status", record.getStatusId(), -1)
          .SET_UNLESS_NULL("payment_processor", record.getPaymentProcessor())
          .SET_UNLESS_NULL("payment_token", record.getPaymentToken())
          .SET_UNLESS_NULL("payment_type", record.getPaymentType())
          .SET_UNLESS_NULL("payment_brand", record.getPaymentBrand())
          .SET_UNLESS_NULL("payment_last4", record.getPaymentLast4())
          .SET_UNLESS_NULL("payment_fingerprint", record.getPaymentFingerprint())
          .SET_UNLESS_NULL("payment_country", record.getPaymentCountry())
          .SET_UNLESS_NULL("payment_date", record.getPaymentDate())
          .SET("charge_token", record.getChargeToken())
          .SET("ip_address", record.getIpAddress())
          .SET_UNLESS_NULL("session_id", record.getSessionId())
          .SET("country_iso", record.getCountryIso())
          .SET("country", record.getCountry())
          .SET("city", record.getCity())
          .SET("state_iso", record.getStateIso())
          .SET("state", record.getState())
          .SET("latitude", record.getLatitude() != 0 ? record.getLatitude() : null)
          .SET("longitude", record.getLongitude() != 0 ? record.getLongitude() : null)
          .SET_UNLESS_MATCHES("modified_by", record.getModifiedBy(), -1)
          .SET("modified", new Timestamp(System.currentTimeMillis()));
      if (record.getBillingAddress() != null) {
        update
            .SET("billing_first_name", record.getBillingAddress().getFirstName())
            .SET("billing_last_name", record.getBillingAddress().getLastName())
            .SET("billing_organization", record.getBillingAddress().getOrganization())
            .SET("billing_street_address", record.getBillingAddress().getStreet())
            .SET("billing_address_line_2", record.getBillingAddress().getAddressLine2())
            .SET("billing_address_line_3", record.getBillingAddress().getAddressLine3())
            .SET("billing_city", record.getBillingAddress().getCity())
            .SET("billing_state", record.getBillingAddress().getState())
            .SET("billing_country", record.getBillingAddress().getCountry())
            .SET("billing_postal_code", record.getBillingAddress().getPostalCode())
            .SET("billing_county", record.getBillingAddress().getCounty())
            .SET("billing_phone_number", record.getBillingAddress().getPhoneNumber())
            .SET("billing_latitude", record.getBillingAddress().getLatitude() != 0 ? record.getBillingAddress().getLatitude() : null)
            .SET("billing_longitude",
                record.getBillingAddress().getLongitude() != 0 ? record.getBillingAddress().getLongitude() : null);
      } else {
        update
            .SET("billing_first_name", (String) null)
            .SET("billing_last_name", (String) null)
            .SET("billing_organization", (String) null)
            .SET("billing_street_address", (String) null)
            .SET("billing_address_line_2", (String) null)
            .SET("billing_address_line_3", (String) null)
            .SET("billing_city", (String) null)
            .SET("billing_state", (String) null)
            .SET("billing_country", (String) null)
            .SET("billing_postal_code", (String) null)
            .SET("billing_county", (String) null)
            .SET("billing_phone_number", (String) null)
            .SET("billing_latitude", (BigDecimal) null)
            .SET("billing_longitude", (BigDecimal) null);
      }
      if (record.getShippingAddress() != null) {
        update
            .SET("shipping_first_name", record.getShippingAddress().getFirstName())
            .SET("shipping_last_name", record.getShippingAddress().getLastName())
            .SET("shipping_organization", record.getShippingAddress().getOrganization())
            .SET("shipping_street_address", record.getShippingAddress().getStreet())
            .SET("shipping_address_line_2", record.getShippingAddress().getAddressLine2())
            .SET("shipping_address_line_3", record.getShippingAddress().getAddressLine3())
            .SET("shipping_city", record.getShippingAddress().getCity())
            .SET("shipping_state", record.getShippingAddress().getState())
            .SET("shipping_country", record.getShippingAddress().getCountry())
            .SET("shipping_postal_code", record.getShippingAddress().getPostalCode())
            .SET("shipping_county", record.getShippingAddress().getCounty())
            .SET("shipping_phone_number", record.getShippingAddress().getPhoneNumber())
            .SET("shipping_latitude",
                record.getShippingAddress().getLatitude() != 0 ? record.getShippingAddress().getLatitude() : null)
            .SET("shipping_longitude",
                record.getShippingAddress().getLongitude() != 0 ? record.getShippingAddress().getLongitude() : null);
      } else {
        update
            .SET("shipping_first_name", (String) null)
            .SET("shipping_last_name", (String) null)
            .SET("shipping_organization", (String) null)
            .SET("shipping_street_address", (String) null)
            .SET("shipping_address_line_2", (String) null)
            .SET("shipping_address_line_3", (String) null)
            .SET("shipping_city", (String) null)
            .SET("shipping_state", (String) null)
            .SET("shipping_country", (String) null)
            .SET("shipping_postal_code", (String) null)
            .SET("shipping_county", (String) null)
            .SET("shipping_phone_number", (String) null)
            .SET("shipping_latitude", (BigDecimal) null)
            .SET("shipping_longitude", (BigDecimal) null);
      }
      update.WHERE("order_id = ?", record.getId());
      if (update.execute(connection).booleanValue()) {
        // The order was successfully charged, disable the cart
        if (record.getPaid()) {
          // Update the cart reference so it cannot be reused
          DB.UPDATE("carts")
              .SET("enabled", false)
              .SET("order_date", new Timestamp(System.currentTimeMillis()))
              .WHERE("cart_id = ?", record.getCartId())
              .execute(connection);

          // @todo Append to the order_history (PAID)

          // On Update, the OrderItemList needs to be updated with the Paid status
          List<OrderItem> orderItemList = OrderItemRepository.findItemsByOrderId(record.getId());
          if (orderItemList != null && !orderItemList.isEmpty()) {
            for (OrderItem orderItem : orderItemList) {
              // Update the inventory
              if (record.getLive()) {
                // @todo consider service type products
                // Decrease the inventory if this is a good or has a limit
                ProductSkuRepository.updateInventoryCount(connection, orderItem.getSkuId(), -orderItem.getQuantity().intValue());
              }
              // Update the status
              OrderItemRepository.markStatusAsPaid(connection, orderItem, record.getPaymentDate());
            }
          }
        }
        // Finish the transaction
        transaction.commit();
        return record;
      }
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  private static Order buildRecord(ResultSet rs) {
    try {
      Order record = new Order();
      Address billingAddress = new Address();
      Address shippingAddress = new Address();
      record.setId(rs.getLong("order_id"));
      record.setUniqueId(rs.getString("order_unique_id"));
      record.setCustomerId(rs.getLong("customer_id"));
      record.setEmail(rs.getString("email"));
      record.setCustomerNote(rs.getString("customer_note"));
      billingAddress.setFirstName(rs.getString("billing_first_name"));
      billingAddress.setLastName(rs.getString("billing_last_name"));
      billingAddress.setOrganization(rs.getString("billing_organization"));
      billingAddress.setStreet(rs.getString("billing_street_address"));
      billingAddress.setAddressLine2(rs.getString("billing_address_line_2"));
      billingAddress.setAddressLine3(rs.getString("billing_address_line_3"));
      billingAddress.setCity(rs.getString("billing_city"));
      billingAddress.setState(rs.getString("billing_state"));
      billingAddress.setCountry(rs.getString("billing_country"));
      billingAddress.setPostalCode(rs.getString("billing_postal_code"));
      billingAddress.setCounty(rs.getString("billing_county"));
      billingAddress.setPhoneNumber(rs.getString("billing_phone_number"));
      shippingAddress.setFirstName(rs.getString("shipping_first_name"));
      shippingAddress.setLastName(rs.getString("shipping_last_name"));
      shippingAddress.setOrganization(rs.getString("shipping_organization"));
      shippingAddress.setStreet(rs.getString("shipping_street_address"));
      shippingAddress.setAddressLine2(rs.getString("shipping_address_line_2"));
      shippingAddress.setAddressLine3(rs.getString("shipping_address_line_3"));
      shippingAddress.setCity(rs.getString("shipping_city"));
      shippingAddress.setState(rs.getString("shipping_state"));
      shippingAddress.setCountry(rs.getString("shipping_country"));
      shippingAddress.setPostalCode(rs.getString("shipping_postal_code"));
      shippingAddress.setCounty(rs.getString("shipping_county"));
      shippingAddress.setPhoneNumber(rs.getString("shipping_phone_number"));
      record.setShippingMethodId(DB.getInt(rs, "shipping_method", -1));
      record.setTotalItems(rs.getInt("total_items"));
      record.setCurrency(rs.getString("currency"));
      record.setSubtotalAmount(rs.getBigDecimal("subtotal_amount"));
      record.setDiscountAmount(rs.getBigDecimal("discount_amount"));
      record.setHandlingFee(rs.getBigDecimal("fee_amount"));
      record.setHandlingFeeTaxAmount(rs.getBigDecimal("fee_tax_amount"));
      record.setShippingFee(rs.getBigDecimal("shipping_amount"));
      record.setShippingTaxAmount(rs.getBigDecimal("shipping_tax_amount"));
      record.setTaxAmount(rs.getBigDecimal("tax_amount"));
      record.setTotalAmount(rs.getBigDecimal("total_amount"));
      record.setTotalPaid(rs.getBigDecimal("total_paid"));
      record.setTotalPending(rs.getBigDecimal("total_pending"));
      record.setTotalRefunded(rs.getBigDecimal("total_refunded"));
      record.setStatusId(DB.getInt(rs, "status", -1));
      record.setHasPreOrder(rs.getBoolean("has_preorder"));
      record.setHasBackOrder(rs.getBoolean("has_backorder"));
      record.setPaid(rs.getBoolean("paid"));
      record.setProcessed(rs.getBoolean("processed"));
      record.setShipped(rs.getBoolean("shipped"));
      record.setCanceled(rs.getBoolean("canceled"));
      record.setRefunded(rs.getBoolean("refunded"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(DB.getLong(rs, "modified_by", -1));
      record.setBarcode(rs.getString("barcode"));
      record.setTaxId(rs.getString("tax_id"));
      billingAddress.setLatitude(rs.getDouble("billing_latitude"));
      billingAddress.setLongitude(rs.getDouble("billing_longitude"));
      shippingAddress.setLatitude(rs.getDouble("shipping_latitude"));
      shippingAddress.setLongitude(rs.getDouble("shipping_longitude"));
      record.setCartId(rs.getLong("cart_id"));
      record.setRemoteOrderId(rs.getString("remote_order_id"));
      record.setShippingRateId(DB.getInt(rs, "shipping_rate_id", -1));
      record.setPaymentToken(rs.getString("payment_token"));
      record.setPaymentType(rs.getString("payment_type"));
      record.setPaymentBrand(rs.getString("payment_brand"));
      record.setPaymentLast4(rs.getString("payment_last4"));
      record.setPaymentFingerprint(rs.getString("payment_fingerprint"));
      record.setPaymentCountry(rs.getString("payment_country"));
      record.setChargeToken(rs.getString("charge_token"));
      record.setLive(rs.getBoolean("live_mode"));
      record.setIpAddress(rs.getString("ip_address"));
      record.setCountryIso(rs.getString("country_iso"));
      record.setCountry(rs.getString("country"));
      record.setCity(rs.getString("city"));
      record.setStateIso(rs.getString("state_iso"));
      record.setState(rs.getString("state"));
      record.setLatitude(rs.getDouble("latitude"));
      record.setLongitude(rs.getDouble("longitude"));
      record.setPaymentDate(rs.getTimestamp("payment_date"));
      record.setProcessingDate(rs.getTimestamp("processing_date"));
      record.setFulfillmentDate(rs.getTimestamp("fulfillment_date"));
      record.setShippedDate(rs.getTimestamp("shipped_date"));
      record.setCanceledDate(rs.getTimestamp("canceled_date"));
      record.setRefundedDate(rs.getTimestamp("refunded_date"));
      record.setPaymentProcessor(rs.getString("payment_processor"));
      record.setTrackingNumbers(rs.getString("tracking_numbers"));
      record.setPromoCode(rs.getString("promo_code"));
      record.setPricingRuleId(DB.getLong(rs, "pricing_rule_1", -1));
      record.setTaxRate(rs.getBigDecimal("tax_rate"));
      record.setSquareOrderId(rs.getString("square_order_id"));
      record.setFirstName(rs.getString("first_name"));
      record.setLastName(rs.getString("last_name"));
      record.setSessionId(rs.getString("session_id"));
      // Update the aggregate
      record.setBillingAddress(billingAddress);
      record.setShippingAddress(shippingAddress);
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  public static void markStatusAsPreparing(Order order) {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(PREPARING);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Update the order status
    DB.UPDATE(TABLE_NAME)
        .SET("processed", true)
        .SET("processing_date", now)
        .SET("status", statusId)
        .SET("modified", now)
        .WHERE("order_id = ?", order.getId())
        .execute();
    // @todo Append to the order_history (PREPARING)
    // Update the object
    order.setModified(now);
    order.setProcessed(true);
    order.setProcessingDate(now);
    order.setStatusId(statusId);
  }

  public static void markStatusAsPartiallyPrepared(Order order) {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(PARTIALLY_PREPARED);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Update the order status
    DB.UPDATE(TABLE_NAME)
        .SET("status", statusId)
        .SET("modified", now)
        .WHERE("order_id = ?", order.getId())
        .execute();
    // @todo Append to the order_history (PARTIALLY_PREPARED)
    // Update the object
    order.setModified(now);
    order.setProcessingDate(now);
    order.setStatusId(statusId);
  }

  public static void markStatusAsCanceled(Order order) {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(CANCELED);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Update the order status
      DB.UPDATE(TABLE_NAME)
          .SET("canceled", true)
          .SET("canceled_date", now)
          .SET("status", statusId)
          .SET("modified", now)
          .WHERE("order_id = ?", order.getId())
          .execute(connection);
      // Mark the order items as canceled too
      OrderItemRepository.markStatusAsCanceled(connection, order);
      // @todo Append to the order_history (CANCELED)
      // Finish the transaction
      transaction.commit();
      // Update the object
      order.setCanceled(true);
      order.setCanceledDate(now);
      order.setStatusId(statusId);
      order.setModified(now);
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
  }

  public static void markStatusAsRefunded(Order order, BigDecimal amountRefunded) {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(REFUNDED);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Update the order status
      DB.UPDATE(TABLE_NAME)
          .SET("refunded", true)
          .SET("refunded_date", now)
          .SET("total_refunded = total_refunded + ?", amountRefunded)
          .SET("status", statusId)
          .SET("modified", now)
          .WHERE("order_id = ?", order.getId())
          .execute(connection);
      // Mark the order items as refunded too
      OrderItemRepository.markStatusAsRefunded(connection, order);
      // @todo Append to the order_history (REFUNDED)
      // Finish the transaction
      transaction.commit();
      // Update the object
      order.setRefunded(true);
      order.setRefundedDate(now);
      order.setTotalRefunded(amountRefunded);
      order.setStatusId(statusId);
      order.setModified(now);
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
  }

  public static void markStatusAsPartiallyShipped(Order order) {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(PARTIALLY_SHIPPED);
    Timestamp now = new Timestamp(System.currentTimeMillis());
    // Update the order status
    DB.UPDATE(TABLE_NAME)
        .SET("status", statusId)
        .SET("shipped_date", now)
        .SET("modified", now)
        .WHERE("order_id = ?", order.getId())
        .execute();
    // @todo Append to the order_history (PARTIALLY_SHIPPED)
    // Update the object
    order.setModified(now);
    order.setShippedDate(now);
    order.setStatusId(statusId);
  }

  public static void markStatusAsShipped(Order order) {
    // Determine the new status value
    int statusId = OrderStatusCommand.retrieveStatusId(SHIPPED);
    // Determine the date
    Timestamp now = new Timestamp(System.currentTimeMillis());
    if (order.getShippedDate() == null) {
      order.setShippedDate(now);
    }
    // Update the order status
    DB.UPDATE(TABLE_NAME)
        .SET("status", statusId)
        .SET("shipped", true)
        .SET("shipped_date", order.getShippedDate())
        .SET("modified", now)
        .WHERE("order_id = ?", order.getId())
        .execute();
    // @todo Append to the order_history (SHIPPED)
    // Update the object
    order.setModified(now);
    order.setShipped(true);
    order.setShippedDate(now);
    order.setStatusId(statusId);
  }

  public static void updateUserOrders(User user) {
    // Require user record
    if (user == null || StringUtils.isBlank(user.getEmail())) {
      return;
    }
    // Update unlinked orders
    DB.UPDATE(TABLE_NAME)
        .SET("created_by", user.getId())
        .WHERE("created_by IS NULL")
        .AND("LOWER(email) = LOWER(?)", user.getEmail())
        .execute();
    // @todo Append to the order_history (USER ASSOCIATED)
  }

  public static void export(DataConstraints constraints, File file) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("order_id");
    Select select = DB.SELECT(
        "order_unique_id AS \"Order Number\"",
        "live_mode AS \"Live Mode\"",
        "payment_date AS \"Date Ordered\"",
        "processing_date AS \"Date Processed\"",
        "shipped_date AS \"Date Shipped\"",
        "los.title AS \"Status\"",
        "concat_ws(' ', shipping_first_name, shipping_last_name) AS \"Customer Name\"",
        "shipping_city AS \"City\"",
        "shipping_state AS \"State\"",
        "shipping_country AS \"Country\"",
        "shipping_postal_code AS \"Postal Code\"",
        "currency AS \"Currency\"",
        "subtotal_amount AS \"Subtotal\"",
        "-discount_amount AS \"Discount\"",
        "shipping_amount AS \"Shipping\"",
        "subtotal_amount + shipping_amount + fee_amount - discount_amount AS \"Sales Total\"",
        "tax_amount AS \"Sales Tax\"",
        "tax_rate AS \"Sales Tax Rate\"",
        "total_amount AS \"Total\"",
        "-total_refunded AS \"Refunded\"",
        "promo_code AS \"Promo Code\"",
        "payment_processor AS \"Processor\"")
        .FROM(TABLE_NAME)
        .LEFT_JOIN("lookup_order_status los")
        .ON("orders.status = los.status_id")
        .WHERE("live_mode = ?", true)
        .AND("paid = ?", true)
        .AND("canceled = ?", false)
        .AND("(refunded = false OR (refunded = true and shipped = true))")
        .WITH(constraints);
    writeCsvExport(select, file);
  }

  public static void exportForTaxJar(DataConstraints constraints, File file) {
    // show paid orders, and only refunded ones that have shipped
    // Use the specification to filter results
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("order_id");
    Select select = DB.SELECT(
        "'web' AS provider",
        "charge_token AS \"order_id\"",
        "'Order' AS transaction_type",
        "'' AS transaction_reference_id",
        "payment_date AS \"completed_at\"",
        "concat_ws(' ', shipping_first_name, shipping_last_name) AS \"customer_name\"",
        "'' AS shiptostreet",
        "shipping_city AS \"shiptocity\"",
        "shipping_state AS \"shiptostate\"",
        "shipping_postal_code AS \"shiptozip\"",
        "shipping_country AS \"Country\"",
        "'' AS shiptocountrycode",
        "'' AS from_street",
        "'' AS from_city",
        "'' AS from_state",
        "'' AS from_zip",
        "'' AS from_country",
        "shipping_amount AS \"shipping_amount\"",
        "fee_amount AS \"handling_amount\"",
        "discount_amount AS \"discount_amount\"",
        // without tax; subtotal + shipping + handling - discount
        "subtotal_amount + shipping_amount + fee_amount - discount_amount AS \"total_sale\"",
        "tax_amount AS \"sales_tax\"",
        "total_amount AS \"Total\"",
        "-total_refunded AS \"Refunded\"")
        .FROM(TABLE_NAME)
        .LEFT_JOIN("lookup_order_status los")
        .ON("orders.status = los.status_id")
        .WHERE("live_mode = ?", true)
        .AND("paid = ?", true)
        .AND("canceled = ?", false)
        .AND("(refunded = false OR (refunded = true and shipped = true))")
        .WITH(constraints);
    writeCsvExport(select, file);
  }

  // @todo move this to a utility class
  
  private static void writeCsvExport(Select select, File file) {
    if (select == null || file == null) {
      return;
    }
    try (Connection connection = DB.getConnection();
        java.sql.PreparedStatement statement = connection.prepareStatement(select.getSql())) {
      int index = 0;
      for (Object value : select.getParameters()) {
        statement.setObject(++index, value);
      }
      try (java.sql.ResultSet rs = statement.executeQuery();
          java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file))) {
        writer.write(
            "Order Number,Live Mode,Date Ordered,Date Processed,Date Shipped,Status,Customer Name,City,State,Country,Postal Code,Currency,Subtotal,Discount,Shipping,Sales Total,Sales Tax,Sales Tax Rate,Total,Refunded,Promo Code,Processor\n");
        while (rs.next()) {
          writer.write(rs.getString(1) + "," + rs.getString(2) + "," + rs.getString(3) + "," + rs.getString(4) + "," + rs.getString(5)
              + "," + rs.getString(6) + "," + rs.getString(7) + "," + rs.getString(8) + "," + rs.getString(9) + "," + rs.getString(10)
              + "," + rs.getString(11) + "," + rs.getString(12) + "," + rs.getString(13) + "," + rs.getString(14) + ","
              + rs.getString(15) + "," + rs.getString(16) + "," + rs.getString(17) + "," + rs.getString(18) + "," + rs.getString(19)
              + "," + rs.getString(20) + "," + rs.getString(21) + "," + rs.getString(22) + "\n");
        }
        writer.flush();
      }
    } catch (Exception e) {
      LOG.error("Order export failed", e);
    }
  }
}
