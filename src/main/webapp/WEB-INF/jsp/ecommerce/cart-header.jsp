<%--
  ~ Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<div class="cart-btn header-btn" data-header-cart>
  <button type="button" class="cart-menu-trigger" title="Shopping Cart" aria-label="Shopping Cart" aria-controls="<c:out value='${headerCartId}'/>" aria-expanded="false" data-header-cart-trigger>
    <i class="${font:fas()} fa-shopping-cart" aria-hidden="true"></i><span class="btn-text">Cart</span>
    <c:if test="${headerCartCount gt 0}"><span class="cart-badge"><fmt:formatNumber value="${headerCartQuantity}" maxFractionDigits="2" /></span></c:if>
  </button>
  <div class="cart-dropdown" id="<c:out value='${headerCartId}'/>">
    <div class="cart-dropdown-header">Shopping Cart (<fmt:formatNumber value="${headerCartQuantity}" maxFractionDigits="2" /> <c:choose><c:when test="${headerCartQuantity == 1}">Item</c:when><c:otherwise>Items</c:otherwise></c:choose>)</div>
    <div class="cart-dropdown-items">
      <c:choose>
        <c:when test="${not empty headerCartEntries}">
          <c:forEach items="${headerCartEntries}" var="cartEntry">
            <a class="cart-item" href="<c:out value='${ctx}${cartEntry.product.productUrl}'/>">
              <c:if test="${not empty cartEntry.product.imageUrl}"><img src="<c:out value='${cartEntry.product.imageUrl}'/>" alt="<c:out value='${cartEntry.product.name}'/>" class="cart-item-image" /></c:if>
              <div class="cart-item-info">
                <div class="cart-item-name"><c:out value="${cartEntry.product.name}" /></div>
                <div class="cart-item-price"><fmt:formatNumber value="${cartEntry.cartItem.quantity}" maxFractionDigits="2" /> x <fmt:formatNumber value="${cartEntry.productSku.price}" type="currency" /></div>
              </div>
            </a>
          </c:forEach>
        </c:when>
        <c:otherwise><div class="cart-empty"><c:out value="${headerCartEmptyMessage}" /></div></c:otherwise>
      </c:choose>
    </div>
    <c:if test="${headerCartCount gt 0}">
      <div class="cart-dropdown-footer">
        <div class="cart-total"><span>Total:</span><span><fmt:formatNumber value="${headerCartTotal}" type="currency" /></span></div>
        <a href="<c:out value='${headerCartLink}'/>" class="button expanded">View Cart</a>
        <a href="<c:out value='${headerCheckoutLink}'/>" class="button primary expanded">Checkout</a>
      </div>
    </c:if>
  </div>
</div>