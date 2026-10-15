/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
package com.zeroio.platform.presentation.widgets.ecommerce;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.ecommerce.LoadProductCommand;
import com.simisinc.platform.domain.model.ecommerce.Cart;
import com.simisinc.platform.domain.model.ecommerce.CartEntry;
import com.simisinc.platform.domain.model.ecommerce.CartItem;
import com.simisinc.platform.domain.model.ecommerce.Product;
import com.simisinc.platform.domain.model.ecommerce.ProductSku;
import com.simisinc.platform.infrastructure.persistence.ecommerce.CartItemRepository;
import com.simisinc.platform.infrastructure.persistence.ecommerce.ProductSkuRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.cms.ResolveHeaderLinkCommand;

/**
 * Handles the display and management of the shopping cart menu in the e-commerce platform.
 *
 * @author matt rajkowski
 * @created 10/10/2026 10:38 AM
 */
public class CartMenuWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/ecommerce/cart-header.jsp";

  /**
   * Handles the display and management of the shopping cart menu in the e-commerce platform.
   *
   * @param context
   * @return
   */
  public WidgetContext execute(WidgetContext context) {

    // Check if the cart feature is enabled for the site
    if (!LoadSitePropertyCommand.loadByNameAsBoolean("site.cart")) {
      return context;
    }
    List<CartEntry> entries = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal totalQuantity = BigDecimal.ZERO;
    Cart cart = context.getUserSession().getCart();
    if (cart != null && cart.getTotalItems() > 0) {
      for (CartItem item : CartItemRepository.findValidItemsByCartId(cart.getId())) {
        ProductSku sku = ProductSkuRepository.findById(item.getSkuId());
        if (sku == null || !sku.getEnabled() || sku.getPrice() == null || sku.getPrice().signum() < 0 || item.getQuantity() == null
            || item.getQuantity().signum() <= 0) {
          continue;
        }
        Product product = LoadProductCommand.loadProductMetaDataById(sku.getProductId());
        if (product == null) {
          continue;
        }
        CartEntry entry = new CartEntry();
        entry.setCartItem(item);
        entry.setProductSku(sku);
        entry.setProduct(product);
        entries.add(entry);
        total = total.add(sku.getPrice().multiply(item.getQuantity()));
        totalQuantity = totalQuantity.add(item.getQuantity());
      }
    }
    context.getRequest().setAttribute("headerCartEntries", entries);
    context.getRequest().setAttribute("headerCartId", context.getUniqueId() + "-cart-menu");
    context.getRequest().setAttribute("headerCartCount", entries.size());
    context.getRequest().setAttribute("headerCartQuantity", totalQuantity.stripTrailingZeros());
    context.getRequest().setAttribute("headerCartTotal", total);
    context.getRequest().setAttribute("headerCartEmptyMessage",
        context.getPreferences().getOrDefault("emptyMessage", "Your cart is empty"));
    context.getRequest().setAttribute("headerCartLink",
        ResolveHeaderLinkCommand.resolveLink(context.getPreferences().getOrDefault("cartLink", "/cart"), context.getContextPath()));
    context.getRequest().setAttribute("headerCheckoutLink", ResolveHeaderLinkCommand
        .resolveLink(context.getPreferences().getOrDefault("checkoutLink", "/checkout"), context.getContextPath()));
    context.setJsp(JSP);
    return context;
  }

}
