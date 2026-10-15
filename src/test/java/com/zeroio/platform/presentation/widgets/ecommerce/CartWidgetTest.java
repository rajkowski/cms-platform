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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.application.ecommerce.CartValidationCommand;
import com.simisinc.platform.application.ecommerce.LoadProductCommand;
import com.simisinc.platform.domain.model.ecommerce.Cart;
import com.simisinc.platform.domain.model.ecommerce.CartItem;
import com.simisinc.platform.domain.model.ecommerce.Product;
import com.simisinc.platform.domain.model.ecommerce.ProductSku;
import com.simisinc.platform.infrastructure.persistence.ecommerce.CartItemRepository;
import com.simisinc.platform.infrastructure.persistence.ecommerce.CartRepository;
import com.simisinc.platform.infrastructure.persistence.ecommerce.ProductSkuRepository;
import com.simisinc.platform.presentation.controller.UserSession;

class CartWidgetTest extends WidgetBase {

  @Test
  void hiddenEmptyCartDoesNotLoadOrMutateCartData() {
    try (var items = mockStatic(CartItemRepository.class);
        var carts = mockStatic(CartRepository.class)) {
      widgetContext.getPreferences().put("showWhenEmpty", "false");
      new CartWidget().execute(widgetContext);
      assertNull(widgetContext.getJsp());
      items.verifyNoInteractions();
      carts.verifyNoInteractions();
    }
  }

  @Test
  void nullCartShowsTheEmptyViewWithoutLoadingEntries() {
    widgetContext.setUserSession(mock(UserSession.class));
    try (var items = mockStatic(CartItemRepository.class)) {
      new CartWidget().execute(widgetContext);
      assertEquals("/ecommerce/cart-empty.jsp", widgetContext.getJsp());
      items.verifyNoInteractions();
    }
  }

  @Test
  void negativeQuantityIsNotMutatedWhileRenderingCart() {
    Cart cart = new Cart();
    cart.setId(10L);
    cart.setTotalItems(1);
    UserSession userSession = mock(UserSession.class);
    when(userSession.getCart()).thenReturn(cart);
    widgetContext.setUserSession(userSession);
    CartItem item = new CartItem();
    item.setSkuId(20L);
    item.setQuantity(new BigDecimal("-1"));
    item.setEachAmount(new BigDecimal("12.50"));
    ProductSku sku = new ProductSku();
    sku.setEnabled(true);
    sku.setProductId(30L);
    sku.setPrice(new BigDecimal("12.50"));
    Product product = new Product();
    try (var items = mockStatic(CartItemRepository.class);
        var skus = mockStatic(ProductSkuRepository.class);
        var products = mockStatic(LoadProductCommand.class)) {
      items.when(() -> CartItemRepository.findValidItemsByCartId(10L)).thenReturn(List.of(item));
      skus.when(() -> ProductSkuRepository.findById(20L)).thenReturn(sku);
      products.when(() -> LoadProductCommand.loadProductMetaDataById(30L)).thenReturn(product);
      new CartWidget().execute(widgetContext);
      assertEquals("/ecommerce/cart.jsp", widgetContext.getJsp());
      assertEquals(-12.5, pageRequest.getAttribute("runningTotal"));
      assertEquals(new BigDecimal("-1"), item.getQuantity());
    }
  }

  @Test
  void populatedCartRendersDomainAmountsWithoutCheckoutSideEffects() {
    Cart cart = new Cart();
    cart.setId(10L);
    cart.setTotalItems(1);
    UserSession userSession = mock(UserSession.class);
    when(userSession.getCart()).thenReturn(cart);
    widgetContext.setUserSession(userSession);
    CartItem item = new CartItem();
    item.setSkuId(20L);
    item.setQuantity(new BigDecimal("2"));
    item.setEachAmount(new BigDecimal("12.50"));
    ProductSku sku = new ProductSku();
    sku.setEnabled(true);
    sku.setProductId(30L);
    sku.setPrice(new BigDecimal("12.50"));
    Product product = new Product();
    product.setName("Fixture product");
    try (var items = mockStatic(CartItemRepository.class);
        var skus = mockStatic(ProductSkuRepository.class);
        var products = mockStatic(LoadProductCommand.class);
        var carts = mockStatic(CartRepository.class);
        var validation = mockStatic(CartValidationCommand.class)) {
      items.when(() -> CartItemRepository.findValidItemsByCartId(10L)).thenReturn(List.of(item));
      skus.when(() -> ProductSkuRepository.findById(20L)).thenReturn(sku);
      products.when(() -> LoadProductCommand.loadProductMetaDataById(30L)).thenReturn(product);
        validation.when(() -> CartValidationCommand.checkIfCartEntryMeetsRules(org.mockito.ArgumentMatchers.any()))
          .thenReturn(true);
      new CartWidget().execute(widgetContext);
        assertEquals("/ecommerce/cart.jsp", widgetContext.getJsp());
        assertEquals(25.0, pageRequest.getAttribute("runningTotal"));
      assertEquals(1, cart.getTotalItems());
      assertNull(widgetContext.getRedirect());
      carts.verifyNoInteractions();
      validation.verify(() -> CartValidationCommand.checkIfCartEntryMeetsRules(org.mockito.ArgumentMatchers.any()));
    }
  }
}