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

package com.simisinc.platform.application.oauth;

import java.sql.Timestamp;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.login.AuthenticateLoginCommand;
import com.simisinc.platform.domain.model.login.OAuthState;
import com.simisinc.platform.domain.model.login.OAuthToken;
import com.simisinc.platform.domain.model.login.UserToken;
import com.simisinc.platform.infrastructure.persistence.login.UserTokenRepository;
import com.simisinc.platform.infrastructure.persistence.oauth.OAuthTokenRepository;
import com.simisinc.platform.presentation.controller.CookieConstants;
import com.simisinc.platform.presentation.controller.SessionConstants;

/**
 * Configures and verifies OpenAuth2
 *
 * @author matt rajkowski
 * @created 4/20/22 6:19 PM
 */
public class OAuthRequestCommand {

  private static Log LOG = LogFactory.getLog(OAuthRequestCommand.class);

  private OAuthRequestCommand() {
  }

  public static String handleRequest(HttpServletRequest request, HttpServletResponse response, String resource) {
    if (!OAuthConfigurationCommand.isEnabled()) {
      // Skip if not turned on
      LOG.trace("OAuth is not enabled");
      return null;
    }

    // Check the URL for a "/oauth/callback"...
    if (OAuthConfigurationCommand.getRedirectUri().equals(resource)) {
      return handleOAuthCallback(request, response);
    }
    if (hasValidSession(request)) {
      LOG.debug("OAuth check valid");
      return null;
    }
    return handleCookieOrLogin(request, response, resource);
  }

  private static String handleOAuthCallback(HttpServletRequest request, HttpServletResponse response) {
    LOG.debug("Handling OAuth callback, retrieving remote access token...");
    String state = request.getParameter("state");
    String code = request.getParameter("code");
    OAuthToken oAuthToken = OAuthAccessTokenCommand.retrieveAccessToken(state, code);
    if (oAuthToken == null) {
      // Failed, return user back to login
      LOG.error("NO OAUTH TOKEN FOUND... check the SSO client credentials");
      return "/logout";
    }
    // Determine the user's information and log them in
    OAuthLoginCommand.loginTheUser(request, response, oAuthToken);
    OAuthState oAuthState = OAuthAuthorizationCommand.stateIfValid(state);
    if (oAuthState == null) {
      // The state used to track the original destination has expired, is missing, or is invalid
      LOG.warn("OAUTH: No valid OAuthState found for callback");
      return "/";
    }
    // Return the user to the site home page (or back to provider) - the default tenant login flow
    if (StringUtils.isNotBlank(oAuthToken.getResource())) {
      return oAuthToken.getResource();
    }
    return "/";
  }

  /** The workspace can validate the workspace handoff token */
  private static boolean hasValidSession(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      return false;
    }
    String userTokenValue = (String) session.getAttribute(SessionConstants.OAUTH_USER_TOKEN);
    Object oAuthExpiresValue = session.getAttribute(SessionConstants.OAUTH_USER_EXPIRATION_TIME);
    if (StringUtils.isBlank(userTokenValue)) {
      return false;
    }
    if (oAuthExpiresValue == null) {
      return true;
    }
    return (Long) oAuthExpiresValue >= System.currentTimeMillis();
  }

  private static String handleCookieOrLogin(HttpServletRequest request, HttpServletResponse response, String resource) {
    LOG.debug("Checking cookie value");
    String cookieToken = findUserTokenCookieValue(request);
    if (cookieToken == null) {
      return OAuthAuthorizationCommand.getAuthorizationUrl(resource);
    }
    return handleCookieToken(request, response, cookieToken);
  }

  private static String handleCookieToken(HttpServletRequest request, HttpServletResponse response, String userTokenValue) {
    UserToken userToken = UserTokenRepository.findByToken(userTokenValue);
    if (userToken == null) {
      LOG.debug("Cookie userToken not found");
      return OAuthAuthorizationCommand.getAuthorizationUrl("/");
    }
    OAuthToken oAuthToken = OAuthTokenRepository.findByUserTokenId(userToken.getUserId(), userToken.getId());
    if (oAuthToken == null) {
      LOG.debug("Cookie oAuthToken not found");
      return OAuthAuthorizationCommand.getAuthorizationUrl("/");
    }
    Timestamp now = new Timestamp(System.currentTimeMillis());
    if (userToken.getExpires().before(now)) {
      UserTokenRepository.remove(userToken);
      LOG.debug("Token is already expired");
      return OAuthAuthorizationCommand.getAuthorizationUrl("/");
    }
    LOG.debug("User token is not expired");
    if (oAuthToken.getExpires().after(now)) {
      LOG.debug("Access token is not expired");
      setOAuthSession(request, userToken, oAuthToken);
      return null;
    }
    if (oAuthToken.getRefreshExpires() != null && oAuthToken.getRefreshExpires().before(now)) {
      LOG.debug("Refresh token is expired, force login");
      return OAuthAuthorizationCommand.getAuthorizationUrl("/");
    }
    LOG.debug("Access token is expired, refresh token is not expired (or unknown)");
    oAuthToken = OAuthAccessTokenCommand.refreshAccessToken(oAuthToken);
    if (oAuthToken == null) {
      LOG.debug("Refreshed token not found");
      return OAuthAuthorizationCommand.getAuthorizationUrl("/");
    }
    int oAuthExpirationSeconds = calculateExpirationSeconds(oAuthToken);
    LOG.debug("Extending the token expiration");
    AuthenticateLoginCommand.extendTokenExpiration(userTokenValue, oAuthExpirationSeconds);
    LOG.debug("Updating the OAuthToken record");
    oAuthToken = OAuthTokenRepository.save(oAuthToken);
    if (oAuthToken == null) {
      LOG.debug("Token not updated");
      return OAuthAuthorizationCommand.getAuthorizationUrl("/");
    }
    extendUserTokenCookie(request, response, userTokenValue, oAuthExpirationSeconds);
    setOAuthSession(request, userToken, oAuthToken);
    return null;
  }

  private static int calculateExpirationSeconds(OAuthToken oAuthToken) {
    int oAuthExpirationSeconds = 14 * 24 * 60 * 60;
    if (oAuthToken.getRefreshExpiresIn() > 0) {
      oAuthExpirationSeconds = oAuthToken.getRefreshExpiresIn();
    } else if (oAuthToken.getExpiresIn() > 0) {
      oAuthExpirationSeconds = oAuthToken.getExpiresIn();
    }
    return oAuthExpirationSeconds;
  }

  private static void extendUserTokenCookie(HttpServletRequest request, HttpServletResponse response, String userTokenValue,
      int oAuthExpirationSeconds) {
    Cookie cookie = new Cookie(CookieConstants.USER_TOKEN, userTokenValue);
    if (request.isSecure()) {
      cookie.setSecure(true);
    }
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    cookie.setMaxAge(oAuthExpirationSeconds);
    response.addCookie(cookie);
  }

  private static void setOAuthSession(HttpServletRequest request, UserToken userToken, OAuthToken oAuthToken) {
    request.getSession().setAttribute(SessionConstants.OAUTH_USER_TOKEN, userToken.getToken());
    if (oAuthToken.getExpires() != null) {
      request.getSession().setAttribute(SessionConstants.OAUTH_USER_EXPIRATION_TIME, oAuthToken.getExpires().getTime());
    }
  }

  private static String findUserTokenCookieValue(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    String userTokenValue = null;
    for (Cookie cookie : cookies) {
      if (CookieConstants.USER_TOKEN.equals(cookie.getName())) {
        userTokenValue = cookie.getValue();
      }
    }
    return userTokenValue;
  }
}
