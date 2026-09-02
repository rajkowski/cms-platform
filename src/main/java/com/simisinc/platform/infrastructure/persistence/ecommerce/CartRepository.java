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

import java.math.BigDecimal;
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
import com.simisinc.platform.domain.model.ecommerce.Cart;
import com.simisinc.platform.domain.model.ecommerce.CartItem;
import com.simisinc.platform.domain.model.ecommerce.Product;
import com.simisinc.platform.domain.model.ecommerce.ProductSku;
import com.simisinc.platform.domain.model.ecommerce.ShippingRate;

/**
 * Persists and retrieves cart objects
 *
 * @author matt rajkowski
 * @created 4/12/19 8:00 AM
 */
public class CartRepository {

  private static Log LOG = LogFactory.getLog(CartRepository.class);

  private static String TABLE_NAME = "carts";
  private static String[] PRIMARY_KEY = new String[] { "cart_id" };

  public static List<Cart> findAll() {
    DataConstraints constraints = new DataConstraints().setDefaultColumnToSortBy("cart_id desc");
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WITH(constraints)
        .returnDataResult(CartRepository::buildRecord).getRecords();
  }

  public static Cart findById(long cartId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("cart_id = ?", cartId)
        .returnRecord(CartRepository::buildRecord);
  }

  public static Cart findValidCartByToken(String token) {
    if (StringUtils.isBlank(token)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("cart_unique_id = ?", token)
        .AND("(expires IS NULL OR expires > ?)", new Timestamp(System.currentTimeMillis()))
        .AND("enabled = ?", true)
        .returnRecord(CartRepository::buildRecord);
  }

  public static Cart add(Cart record) {
    record.setId(DB.INSERT().INTO(TABLE_NAME)
        .FIELD("cart_unique_id", record.getToken())
        .FIELD_UNLESS_MATCHES("visitor_id", record.getVisitorId(), -1)
        .FIELD_UNLESS_NULL("session_id", record.getSessionId())
        .FIELD_UNLESS_MATCHES("customer_id", record.getCustomerId(), -1)
        .FIELD_UNLESS_MATCHES("user_id", record.getUserId(), -1)
        .FIELD_UNLESS_MATCHES("total_items", record.getTotalItems(), 0)
        .FIELD("total_qty", record.getTotalQty())
        .FIELD_UNLESS_NULL("currency", record.getCurrency())
        .FIELD("subtotal_amount", record.getSubtotalAmount())
        .FIELD_UNLESS_MATCHES("order_id", record.getOrderId(), -1)
        .FIELD_UNLESS_NULL("order_date", record.getOrderDate())
        .FIELD_UNLESS_NULL("expires", record.getExpires())
        .FIELD_UNLESS_NULL("discount_amount", record.getDiscount())
        .FIELD_UNLESS_NULL("promo_code", record.getPromoCode())
        .FIELD_UNLESS_MATCHES("pricing_rule_1", record.getPricingRuleId(), -1)
        .FIELD_UNLESS_MATCHES("created_by", record.getCreatedBy(), -1)
        .FIELD_UNLESS_MATCHES("modified_by", record.getModifiedBy(), -1)
        .execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static boolean addProductToCart(Cart cart, Product product, ProductSku productSku, BigDecimal quantity) {
    if (cart == null || productSku == null) {
      return false;
    }
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      CartItemRepository.addProductToCart(connection, cart, product, productSku, quantity);
      // Update the totals
      DB.UPDATE(TABLE_NAME)
          .SET("total_items = total_items + 1")
          .SET("total_qty = total_qty + " + quantity)
          .SET("subtotal_amount = subtotal_amount + " + productSku.getPrice().multiply(quantity))
          .SET("modified", new Timestamp(System.currentTimeMillis()))
          .WHERE("cart_id = ?", cart.getId())
          .execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static boolean updateCart(Cart cart, List<CartItem> cartItemList) {
    if (cart == null || cartItemList == null) {
      return false;
    }
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      CartItemRepository.updateCartItemList(connection, cartItemList);
      DB.UPDATE(TABLE_NAME)
          .SET("total_items", cart.getTotalItems())
          .SET("total_qty", cart.getTotalQty())
          .SET("subtotal_amount", cart.getSubtotalAmount())
          .SET("modified", new Timestamp(System.currentTimeMillis()))
          .WHERE("cart_id = ?", cart.getId())
          .execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static boolean updateDiscount(Cart cart) {
    if (cart == null) {
      return false;
    }
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      DB.UPDATE(TABLE_NAME)
          .SET("discount_amount", cart.getDiscount())
          .SET("promo_code", cart.getPromoCode())
          .SET_UNLESS_MATCHES("pricing_rule_1", cart.getPricingRuleId(), -1)
          .SET("modified", new Timestamp(System.currentTimeMillis()))
          .WHERE("cart_id = ?", cart.getId())
          .execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static boolean updateShippingRateAndTaxes(Cart cart) {
    if (cart == null) {
      return false;
    }
    long shippingMethod = -1;
    ShippingRate shippingRate = ShippingRateRepository.findById(cart.getShippingRateId());
    if (shippingRate != null) {
      shippingMethod = shippingRate.getShippingMethodId();
    }
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      DB.UPDATE(TABLE_NAME)
          .SET("shipping_method", shippingMethod == -1 ? null : shippingMethod)
          .SET("shipping_rate_id", cart.getShippingRateId() == -1 ? null : cart.getShippingRateId())
          .SET("handling_fee_amount", cart.getHandlingFee())
          .SET("handling_fee_tax_amount", cart.getHandlingTax())
          .SET("shipping_amount", cart.getShippingFee())
          .SET("shipping_tax_amount", cart.getShippingTax())
          .SET("tax_amount", cart.getTaxAmount())
          .SET("tax_rate", cart.getTaxRate())
          .SET("modified", new Timestamp(System.currentTimeMillis()))
          .WHERE("cart_id = ?", cart.getId())
          .execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static Cart updateCustomer(Cart cart) {
    if (DB.UPDATE(TABLE_NAME)
        .SET("customer_id", cart.getCustomerId() == -1 ? null : cart.getCustomerId())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("cart_id = ?", cart.getId())
        .execute().booleanValue()) {
      return cart;
    }
    LOG.error("updateCustomer failed!");
    return null;
  }

  private static Cart buildRecord(ResultSet rs) {
    try {
      Cart record = new Cart();
      record.setId(rs.getLong("cart_id"));
      record.setToken(rs.getString("cart_unique_id"));
      record.setVisitorId(rs.getLong("visitor_id"));
      record.setSessionId(rs.getString("session_id"));
      record.setCustomerId(DB.getLong(rs, "customer_id", -1));
      if (record.getCustomerId() == 0) {
        record.setCustomerId(-1);
      }
      record.setUserId(DB.getLong(rs, "user_id", -1));
      record.setTotalItems(rs.getInt("total_items"));
      record.setTotalQty(rs.getBigDecimal("total_qty"));
      // @todo currency
      record.setSubtotalAmount(rs.getBigDecimal("subtotal_amount"));
      record.setOrderId(DB.getLong(rs, "order_id", -1));
      record.setOrderDate(rs.getTimestamp("order_date"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(DB.getLong(rs, "modified_by", -1));
      record.setExpires(rs.getTimestamp("expires"));
      //      record.setShippingMethodId(rs.getInt("shipping_method"));
      record.setShippingRateId(DB.getInt(rs, "shipping_rate_id", -1));
      record.setHandlingFee(rs.getBigDecimal("handling_fee_amount"));
      record.setHandlingTax(rs.getBigDecimal("handling_fee_tax_amount"));
      record.setShippingFee(rs.getBigDecimal("shipping_amount"));
      record.setShippingTax(rs.getBigDecimal("shipping_tax_amount"));
      record.setTaxAmount(rs.getBigDecimal("tax_amount"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setPromoCode(rs.getString("promo_code"));
      record.setPricingRuleId(DB.getLong(rs, "pricing_rule_1", -1));
      record.setDiscount(rs.getBigDecimal("discount_amount"));
      record.setTaxRate(rs.getBigDecimal("tax_rate"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
