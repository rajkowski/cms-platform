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

/**
 * Persists and retrieves cart item objects
 *
 * @author matt rajkowski
 * @created 4/14/19 9:57 PM
 */
public class CartItemRepository {

  private static Log LOG = LogFactory.getLog(CartItemRepository.class);

  private static String TABLE_NAME = "cart_items";
  private static String[] PRIMARY_KEY = new String[] { "item_id" };

  public static List<CartItem> findValidItemsByCartId(long cartId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("is_removed = ?", false)
        .AND("cart_id = ?", cartId)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("item_id").setUseCount(false))
        .returnDataResult(CartItemRepository::buildRecord).getRecords();
  }

  public static CartItem findById(long itemId) {
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("item_id = ?", itemId)
        .returnRecord(CartItemRepository::buildRecord);
  }

  public static void addProductToCart(Connection connection, Cart cart, Product product, ProductSku productSku, BigDecimal quantity)
      throws SQLException {
    if (cart == null || productSku == null) {
      throw new SQLException("Invalid request");
    }
    DB.INSERT().INTO(TABLE_NAME)
        .FIELD("cart_id", cart.getId())
        .FIELD("product_id", productSku.getProductId())
        .FIELD("sku_id", productSku.getId())
        .FIELD("quantity", quantity)
        .FIELD("each_amount", productSku.getPrice())
        .FIELD("total_amount", productSku.getPrice().multiply(quantity))
        .FIELD("product_name", product.getNameWithCaption())
        .FIELD("product_sku", productSku.getSku())
        .FIELD("product_barcode", productSku.getBarcode())
        .FIELD("is_preorder", false)
        .FIELD("is_backordered", false)
        .FIELD("is_removed", false)
        .execute(connection);
  }

  public static void updateCartItemList(Connection connection, List<CartItem> cartItemList) throws SQLException {
    if (cartItemList == null) {
      throw new SQLException("List is null");
    }
    for (CartItem cartItem : cartItemList) {
      DB.UPDATE(TABLE_NAME)
          .SET("quantity", cartItem.getQuantity())
          .SET("each_amount", cartItem.getEachAmount())
          .SET("total_amount", cartItem.getTotalAmount())
          .SET("is_removed", cartItem.getRemoved())
          .WHERE("item_id = ?", cartItem.getId())
          .execute(connection);
    }
  }

  public static boolean removeItemFromCart(Cart cart, CartItem cartItem) {
    // Use a transaction
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // In a transaction (use the existing connection)
      {
        DB.UPDATE(TABLE_NAME)
            .SET("is_removed", true)
            .SET("modified", new Timestamp(System.currentTimeMillis()))
            .WHERE("item_id = ?", cartItem.getId())
            .execute(connection);
      }
      {
        DB.UPDATE("carts")
            .SET("total_items = total_items - 1")
            .SET("total_qty = total_qty - " + cartItem.getQuantity())
            .SET("subtotal_amount = subtotal_amount - " + cartItem.getQuantity().multiply(cartItem.getEachAmount()))
            .SET("modified", new Timestamp(System.currentTimeMillis()))
            .WHERE("cart_id = ?", cartItem.getCartId())
            .execute(connection);
      }
      // Finish the transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void updateQuantityFree(CartItem cartItem) {
    DB.UPDATE(TABLE_NAME)
        .SET("quantity_free", cartItem.getQuantityFree())
        .WHERE("item_id = ?", cartItem.getId())
        .execute();
  }

  public static void resetQuantityFree(Cart cart) {
    if (cart == null) {
      return;
    }
    DB.UPDATE(TABLE_NAME)
        .SET("quantity_free", new BigDecimal(0))
        .WHERE("cart_id = ?", cart.getId())
        .execute();
  }

  private static CartItem buildRecord(ResultSet rs) {
    try {
      CartItem record = new CartItem();
      record.setId(rs.getLong("item_id"));
      record.setCartId(rs.getLong("cart_id"));
      record.setProductId(rs.getLong("product_id"));
      record.setSkuId(rs.getLong("sku_id"));
      record.setQuantity(rs.getBigDecimal("quantity"));
      record.setCurrency(rs.getString("currency"));
      record.setEachAmount(rs.getBigDecimal("each_amount"));
      record.setTotalAmount(rs.getBigDecimal("total_amount"));
      record.setProductName(rs.getString("product_name"));
      record.setProductType(rs.getString("product_type"));
      record.setProductSku(rs.getString("product_sku"));
      record.setPreorder(rs.getBoolean("is_preorder"));
      record.setBackordered(rs.getBoolean("is_backordered"));
      record.setCreated(rs.getTimestamp("created"));
      record.setCreatedBy(DB.getLong(rs, "created_by", -1));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(DB.getLong(rs, "modified_by", -1));
      record.setProductBarcode(rs.getString("product_barcode"));
      record.setQuantityFree(rs.getBigDecimal("quantity_free"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
