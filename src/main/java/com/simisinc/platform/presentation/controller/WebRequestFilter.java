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

package com.simisinc.platform.presentation.controller;

import static com.simisinc.platform.presentation.controller.UserSession.WEB_SOURCE;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.core5.net.InetAddressUtils;

import com.simisinc.platform.application.CreateSessionCommand;
import com.simisinc.platform.application.LoadVisitorCommand;
import com.simisinc.platform.application.SaveSessionCommand;
import com.simisinc.platform.application.SaveVisitorCommand;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.BlockedIPListCommand;
import com.simisinc.platform.application.cms.HostnameCommand;
import com.simisinc.platform.application.cms.LoadBlockedIPListCommand;
import com.simisinc.platform.application.cms.LoadRedirectsCommand;
import com.simisinc.platform.application.ecommerce.CartCommand;
import com.simisinc.platform.application.ecommerce.LoadCartCommand;
import com.simisinc.platform.application.ecommerce.PricingRuleCommand;
import com.simisinc.platform.application.login.AuthenticateLoginCommand;
import com.simisinc.platform.application.login.LogoutCommand;
import com.simisinc.platform.application.oauth.OAuthConfigurationCommand;
import com.simisinc.platform.application.oauth.OAuthRequestCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.Visitor;
import com.simisinc.platform.domain.model.ecommerce.Cart;
import com.simisinc.platform.domain.model.ecommerce.PricingRule;
import com.simisinc.platform.domain.model.login.UserLogin;
import com.simisinc.platform.infrastructure.persistence.SessionRepository;
import com.simisinc.platform.infrastructure.persistence.login.UserLoginRepository;

/**
 * Sets up the framework for the visitor
 *
 * @author matt rajkowski
 * @created 4/6/18 8:23 AM
 */
public class WebRequestFilter implements Filter {

  private static Log LOG = LogFactory.getLog(WebRequestFilter.class);

  private boolean requireSSL = false;
  private Map<String, String> redirectMap = null;

  @Override
  public void init(FilterConfig config) throws ServletException {
    LOG.info("WebRequestFilter starting up...");
    String startupSuccessful = (String) config.getServletContext().getAttribute(ContextConstants.STARTUP_SUCCESSFUL);
    if (!"true".equals(startupSuccessful)) {
      throw new ServletException("Startup failed due to previous error");
    }
    String ssl = LoadSitePropertyCommand.loadByName("system.ssl");
    if ("true".equals(ssl)) {
      LOG.info("SSL is required by system.ssl");
      requireSSL = true;
    }

    // @todo option to reload
    redirectMap = LoadRedirectsCommand.load();

    // Preload the blocked IP list
    LoadBlockedIPListCommand.retrieveCachedIpAddressList();
  }

  @Override
  public void destroy() {
    
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse servletResponse, FilterChain chain)
      throws ServletException, IOException {

    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    String scheme = request.getScheme();
    String contextPath = request.getServletContext().getContextPath();
    String requestURI = httpServletRequest.getRequestURI();
    String resource = requestURI.substring(contextPath.length());
    String ipAddress = request.getRemoteAddr();
    String referer = httpServletRequest.getHeader("Referer");
    String userAgent = httpServletRequest.getHeader("USER-AGENT");

    // Show the resource and headers
    if (LOG.isTraceEnabled()) {
      LOG.trace("Resource: " + resource);
      Enumeration<?> headerNames = httpServletRequest.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        String name = (String) headerNames.nextElement();
        LOG.debug("Header: " + name + "=" + httpServletRequest.getHeader(name));
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(httpServletRequest.getMethod() + " uri " + resource);
    }

    // Check allowed host names
    if (!HostnameCommand.passesCheck(request.getServerName())) {
      do404(servletResponse);
      return;
    }

    // Block and log certain requests
    if (!BlockedIPListCommand.passesCheck(resource, ipAddress)) {
      do404(servletResponse);
      return;
    }

    // Allow if an SSL renewal request
    if (resource.startsWith("/.well-known/acme-challenge")) {
      chain.doFilter(request, servletResponse);
      return;
    }

    // Check redirects
    if (redirectMap != null) {
      String redirect = redirectMap.get(resource);
      if (redirect != null) {
        // Handle a redirect immediately
        do301(servletResponse, redirect);
        return;
      }
    }

    // Handle logouts immediately
    if (resource.equals("/logout")) {
      // Log out of the system
      LogoutCommand.logout(httpServletRequest.getSession(), ((HttpServletResponse) servletResponse), httpServletRequest.isSecure());
      // Redirect to OAuth Provider via the home page
      if (OAuthConfigurationCommand.isEnabled()) {
        do401(servletResponse);
        return;
      }
    }

    // Redirect to SSL
    if (requireSSL && !"https".equalsIgnoreCase(scheme)) {
      if (!"localhost".equals(request.getServerName()) && !InetAddressUtils.isIPv4Address(request.getServerName())
          && !InetAddressUtils.isIPv6Address(request.getServerName())) {
        String requestURL = httpServletRequest.getRequestURL().toString();
        requestURL = StringUtils.replace(requestURL, "http://", "https://");
        LOG.debug("Redirecting to: " + requestURL);
        do301(servletResponse, requestURL);
        return;
      }
    }

    // REST API has own clients
    if (resource.startsWith("/api")) {
      // Chain to RestRequestFilter
      chain.doFilter(request, servletResponse);
      return;
    }

    // Allow some browser resources
    if (resource.startsWith("/favicon") ||
        resource.startsWith("/css") ||
        resource.startsWith("/fonts") ||
        resource.startsWith("/html") ||
        resource.startsWith("/images") ||
        resource.startsWith("/javascript") ||
        resource.startsWith("/combined.css") ||
        resource.startsWith("/combined.js")) {
      chain.doFilter(request, servletResponse);
      return;
    }

    // If OAuth is required, and the user is not verified, redirect to provider
    String oauthRedirect = OAuthRequestCommand.handleRequest((HttpServletRequest) request,
        (HttpServletResponse) servletResponse, resource);
    if (OAuthConfigurationCommand.hasInvalidConfiguration()) {
      LOG.error("OAUTH: OAUTH is enabled but configuration is incomplete");
      do500(servletResponse);
      return;
    }
    if (oauthRedirect != null) {
      if (StringUtils.isBlank(oauthRedirect)) {
        LOG.error("OAUTH: A redirect url could not be created");
        do500(servletResponse);
        return;
      }
      LOG.debug("OAUTH: Redirecting to " + oauthRedirect);
      do302(servletResponse, oauthRedirect);
      return;
    }

    // Allow this request to forward to the sitemap.xml processor
    if (resource.equals("/sitemap.xml")) {
      chain.doFilter(request, servletResponse);
      return;
    }

    // A method to retain controller data between GET requests
    HttpSession session = httpServletRequest.getSession();
    ControllerSession controllerSession = (ControllerSession) session.getAttribute(SessionConstants.CONTROLLER);
    if (controllerSession == null) {
      synchronized (httpServletRequest.getSession()) {
        controllerSession = (ControllerSession) session.getAttribute(SessionConstants.CONTROLLER);
        if (controllerSession == null) {
          LOG.debug("Creating a new controller session");
          controllerSession = new ControllerSession();
          httpServletRequest.getSession().setAttribute(SessionConstants.CONTROLLER, controllerSession);
        }
      }
    }

    // Determine several values from user cookies to use in functions
    Cookie[] cookies = httpServletRequest.getCookies();
    String cookieViewMode = null;
    String cookieVisitorToken = null;
    String cookieCartToken = null;
    String cookieUserToken = null;
    if (cookies != null) {
      for (Cookie thisCookie : cookies) {
        if (thisCookie.getName().equals(CookieConstants.VIEW_MODE)) {
          cookieViewMode = StringUtils.trimToNull(thisCookie.getValue());
        } else if (thisCookie.getName().equals(CookieConstants.USER_TOKEN)) {
          cookieUserToken = StringUtils.trimToNull(thisCookie.getValue());
        } else if (thisCookie.getName().equals(CookieConstants.VISITOR_TOKEN)) {
          cookieVisitorToken = StringUtils.trimToNull(thisCookie.getValue());
        } else if (thisCookie.getName().equals(CookieConstants.CART_TOKEN)) {
          cookieCartToken = StringUtils.trimToNull(thisCookie.getValue());
        }
      }
    }

    // Check headers to see if this is a container-only experience (no menus/footers)
    if ("container".equals(httpServletRequest.getHeader(SessionConstants.X_VIEW_MODE))) {
      // Add a cookie in case session invalidates
      Cookie cookie = new Cookie(CookieConstants.VIEW_MODE, "container");
      if (request.isSecure()) {
        cookie.setSecure(true);
      }
      cookie.setHttpOnly(true);
      cookie.setPath("/");
      cookie.setMaxAge(-1);
      ((HttpServletResponse) servletResponse).addCookie(cookie);
      session.setAttribute(SessionConstants.X_VIEW_MODE, "container");
    } else if ("normal".equals(httpServletRequest.getHeader(SessionConstants.X_VIEW_MODE))) {
      // Remove the cookie
      Cookie cookie = new Cookie(CookieConstants.VIEW_MODE, "");
      if (request.isSecure()) {
        cookie.setSecure(true);
      }
      cookie.setHttpOnly(true);
      cookie.setPath("/");
      cookie.setMaxAge(0);
      ((HttpServletResponse) servletResponse).addCookie(cookie);
      session.setAttribute(SessionConstants.X_VIEW_MODE, "normal");
    } else {
      // Set the session either way for efficiency
      if (session.getAttribute(SessionConstants.X_VIEW_MODE) == null) {
        boolean foundCookie = false;
        if (cookieViewMode != null && "container".equals(cookieViewMode)) {
          foundCookie = true;
          session.setAttribute(SessionConstants.X_VIEW_MODE, "container");
        }
        if (!foundCookie) {
          // This is a normal web request
          session.setAttribute(SessionConstants.X_VIEW_MODE, "normal");
        }
      }
    }

    // Make sure the web visitor has session information
    LOG.debug("Checking session...");
    UserSession userSession = (UserSession) session.getAttribute(SessionConstants.USER);
    boolean doSaveSession = false;
    if (userSession == null) {
      synchronized (httpServletRequest.getSession()) {
        userSession = (UserSession) session.getAttribute(SessionConstants.USER);
        if (userSession == null) {
          LOG.debug("Creating user session...");
          // Start a new session
          userSession = CreateSessionCommand.createSession(WEB_SOURCE, httpServletRequest.getSession().getId(),
              ipAddress, referer, userAgent);
          httpServletRequest.getSession().setAttribute(SessionConstants.USER, userSession);
          // Determine if this is a monitoring app
          if (httpServletRequest.getHeader("X-Monitor") == null) {
            doSaveSession = true;
          }
        }
      }
      if (doSaveSession) {
        // Save the new session
        SaveSessionCommand.saveSession(userSession);
      }
    }

    // Check once to see if this browser has a cookie for the user
    boolean userVerifiedThisRequest = false;
    if (!userSession.isLoggedIn() && !userSession.isCookieChecked() && !resource.equals("/logout")) {
      // Only check for the cookie once per session
      userSession.setCookieChecked(true);

      // Determine if this is a visitor
      Visitor visitor = null;
      if (StringUtils.isNotBlank(cookieVisitorToken)) {
        // Found a visitor token
        visitor = LoadVisitorCommand.loadVisitorByToken(cookieVisitorToken);
        if (visitor != null) {
          userSession.setVisitorId(visitor.getId());
        }
      }

      // Determine if there is a cart
      if (StringUtils.isNotBlank(cookieCartToken)) {
        LOG.debug("Setting an existing cart from token: " + cookieCartToken);
        Cart cart = LoadCartCommand.loadCartByToken(cookieCartToken);
        if (cart != null) {
          LOG.debug("Cart was found in database: " + cookieCartToken);
          userSession.setCart(cart);
        }
      }

      // Make sure the visitor has a token
      if (visitor == null) {
        // Create and store a new token
        LOG.debug("Creating a visitor token...");
        visitor = SaveVisitorCommand.saveVisitor(userSession);
      } else {
        // Make sure the sessionId is set
        if (doSaveSession) {
          SessionRepository.updateVisitorId(userSession, visitor);
        }
      }

      {
        // Create or extend the visitor cookie
        int oneYearSecondsInt = 365 * 24 * 60 * 60;
        Cookie cookie = new Cookie(CookieConstants.VISITOR_TOKEN, visitor.getToken());
        if (request.isSecure()) {
          cookie.setSecure(true);
        }
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(oneYearSecondsInt);
        ((HttpServletResponse) servletResponse).addCookie(cookie);
      }

      // Check the visitor's cart
      if ("true".equals(LoadSitePropertyCommand.loadByName("site.cart"))) {
        // Instantiate the visitor's cart for reference
        if (userSession.getCart() != null) {
          // Create or extend the cart cookie
          int twoWeeksSecondsInt = 14 * 24 * 60 * 60;
          Cookie cookie = new Cookie(CookieConstants.CART_TOKEN, userSession.getCart().getToken());
          if (request.isSecure()) {
            cookie.setSecure(true);
          }
          cookie.setHttpOnly(true);
          cookie.setPath("/");
          cookie.setMaxAge(twoWeeksSecondsInt);
          ((HttpServletResponse) servletResponse).addCookie(cookie);
        } else {
          // Cleanup the cookie since the token is no longer valid
          Cookie cookie = new Cookie(CookieConstants.CART_TOKEN, "");
          if (request.isSecure()) {
            cookie.setSecure(true);
          }
          cookie.setHttpOnly(true);
          cookie.setPath("/");
          cookie.setMaxAge(0);
          ((HttpServletResponse) servletResponse).addCookie(cookie);
        }
      }

      // Attempt to login the user
      if (cookieUserToken != null) {
        User user = AuthenticateLoginCommand.getAuthenticatedUser(cookieUserToken);
        if (user != null) {
          // Let the request know an authenticated user was retrieved
          userVerifiedThisRequest = true;
          // Log the user in
          LOG.debug("Got a token user: " + user.getId());
          userSession.login(user);
          if (user.getTimeZone() != null) {
            Config.set(request, Config.FMT_TIME_ZONE, user.getTimeZone());
          }
          // Track the login
          UserLogin userLogin = new UserLogin();
          userLogin.setSource(WEB_SOURCE);
          userLogin.setUserId(user.getId());
          userLogin.setIpAddress(ipAddress);
          userLogin.setSessionId(userSession.getSessionId());
          userLogin.setUserAgent(httpServletRequest.getHeader("USER-AGENT"));
          UserLoginRepository.save(userLogin);
          // Extend the token expiration date
          int twoWeeksSecondsInt = 14 * 24 * 60 * 60;
          AuthenticateLoginCommand.extendTokenExpiration(cookieUserToken, twoWeeksSecondsInt);
          // Extend the cookie
          Cookie cookie = new Cookie(CookieConstants.USER_TOKEN, cookieUserToken);
          if (request.isSecure()) {
            cookie.setSecure(true);
          }
          cookie.setHttpOnly(true);
          cookie.setPath("/");
          cookie.setMaxAge(twoWeeksSecondsInt);
          ((HttpServletResponse) servletResponse).addCookie(cookie);
        } else {
          // Cleanup the cookie since the token is no longer valid
          Cookie cookie = new Cookie(CookieConstants.USER_TOKEN, "");
          if (request.isSecure()) {
            cookie.setSecure(true);
          }
          cookie.setHttpOnly(true);
          cookie.setPath("/");
          cookie.setMaxAge(0);
          ((HttpServletResponse) servletResponse).addCookie(cookie);
        }
      }
    }

    // Verify the user record on each request
    if (!userVerifiedThisRequest && userSession.isLoggedIn()) {
      // Verify the roles every request for dynamic changes
      User user = AuthenticateLoginCommand.getAuthenticatedUser(cookieUserToken);
      if (user == null) {
        // Logout
        LogoutCommand.logout(httpServletRequest.getSession(), (HttpServletResponse) servletResponse, httpServletRequest.isSecure());
        // Return to login
        do302(servletResponse, "/login");
        return;
      }

      // Update user roles and groups
      LOG.debug("Updating user roles and groups");
      userSession.setRoleList(user.getRoleList());
      userSession.setGroupList(user.getGroupList());
    }

    // The home page can show an overlay (a couple of different kinds)
    if ("get".equalsIgnoreCase(httpServletRequest.getMethod())) {
      // See if this request has an instant promo code
      boolean hasPricingRule = false;
      String promoCode = httpServletRequest.getParameter(RequestConstants.PROMO_CODE);
      if (StringUtils.isNotBlank(promoCode)) {
        PricingRule pricingRule = PricingRuleCommand.findValidPromoCode(promoCode, null);
        if (pricingRule != null) {
          hasPricingRule = true;
          if (userSession.getCart() == null) {
            CartCommand.createCart(userSession);
          }
          userSession.getCart().setPromoCode(pricingRule.getPromoCode());
          httpServletRequest.setAttribute(RequestConstants.PRICING_RULE, pricingRule);
          LOG.debug("Found promo code overlay: " + pricingRule.getPromoCode());
        }
      }
      // If on the home page, and not an instant promo code, check if the site has a promo overlay
      if (resource.equals("/") && !hasPricingRule) {
        if ("true".equals(LoadSitePropertyCommand.loadByName("site.newsletter.overlay"))) {
          String headline = LoadSitePropertyCommand.loadByName("site.newsletter.headline");
          String message = LoadSitePropertyCommand.loadByName("site.newsletter.message");
          if (StringUtils.isNotBlank(headline) && StringUtils.isNotBlank(message)) {
            httpServletRequest.setAttribute(RequestConstants.OVERLAY_HEADLINE, headline);
            httpServletRequest.setAttribute(RequestConstants.OVERLAY_MESSAGE, message);
          }
        }
      }
    }

    // Default states coordinated by cookies
    /* changed to main.jsp
    userSession.setShowSiteConfirmation(!userSession.isLoggedIn());
    userSession.setShowSiteNewsletterSignup(true);
    // Check the request cookies
    Cookie[] cookies = httpServletRequest.getCookies();
    if (cookies != null) {
      // User values
      for (Cookie thisCookie : cookies) {
        if (thisCookie.getName().equals(CookieConstants.SHOW_SITE_CONFIRMATION)) {
          // Found a saved value
          userSession.setShowSiteConfirmation(false);
        } else if (thisCookie.getName().equals(CookieConstants.SHOW_SITE_NEWSLETTER)) {
          // Found a saved value
          userSession.setShowSiteNewsletterSignup(false);
        }
      }
    }
    */

    chain.doFilter(request, servletResponse);
  }

  private void do301(ServletResponse servletResponse, String redirectLocation) throws IOException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.setHeader("Location", redirectLocation);
    response.setStatus(SC_MOVED_PERMANENTLY);
  }

  private void do302(ServletResponse servletResponse, String redirectLocation) throws IOException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.setHeader("Location", redirectLocation);
    response.setStatus(SC_MOVED_TEMPORARILY);
  }

  private void do401(ServletResponse servletResponse) throws IOException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.sendError(SC_UNAUTHORIZED);
  }

  private void do404(ServletResponse servletResponse) throws IOException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.sendError(SC_NOT_FOUND);
  }

  private void do500(ServletResponse servletResponse) throws IOException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    response.sendError(SC_INTERNAL_SERVER_ERROR);
  }

}
