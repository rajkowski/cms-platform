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
package com.zeroio.platform.application.login;

import java.sql.Timestamp;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.CreateSessionCommand;
import com.simisinc.platform.application.SaveSessionCommand;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.login.UserLogin;
import com.simisinc.platform.domain.model.login.UserToken;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.infrastructure.persistence.login.UserLoginRepository;
import com.simisinc.platform.infrastructure.persistence.login.UserTokenRepository;
import com.simisinc.platform.presentation.controller.CookieConstants;
import com.simisinc.platform.presentation.controller.PageServlet;
import com.simisinc.platform.presentation.controller.SessionConstants;
import com.simisinc.platform.presentation.controller.UserSession;
import com.zeroio.platform.application.cms.WorkspaceResolutionCommand;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.domain.model.tenant.WorkspaceAccessGrant;
import com.zeroio.platform.domain.model.tenant.WorkspaceSessionHandoffToken;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceRepository;

public class WorkspaceCoordinatorCommand {

  private static Log LOG = LogFactory.getLog(WorkspaceCoordinatorCommand.class);

  private WorkspaceCoordinatorCommand() {
  }

  /** Default tenant can prepare for launch */
  public static String prepareForLaunch(UserSession userSession, long targetWorkspaceId) {
    Workspace workspace = WorkspaceRepository.findById(targetWorkspaceId);
    if (!WorkspaceAccessCommand.hasAccess(userSession.getUserId(), workspace)) {
      LOG.warn("Main Tenant User " + userSession.getUserId() + " does not have access to workspace " + targetWorkspaceId);
      return defaultTenantRedirect(PageServlet.WORKSPACE_SELECTOR_PATH);
    }
    // Issue a token that will be used by the target tenant
    String handoffToken = WorkspaceSessionHandoffCommand.issueToken(userSession.getUserId(), workspace.getId(), "/");
    if (handoffToken == null) {
      LOG.error("Unable to issue a workspace session handoff token for launch");
      return defaultTenantRedirect("/");
    }
    return workspace.getSiteUrl() + PageServlet.WORKSPACE_HANDOFF_RESOURCE + "?token=" + UrlCommand.encodeUri(handoffToken);

  }

  /** Target tenant completes the workspace handoff */
  public static String handleWorkspaceHandoff(HttpServletRequest request, HttpServletResponse response) {
    String token = request.getParameter("token");
    if (token == null) {
      LOG.error("No workspace handoff token provided");
      return null;
    }
    WorkspaceSessionHandoffToken handoff = WorkspaceSessionHandoffCommand.consumeToken(token);
    if (handoff == null) {
      LOG.warn("Workspace handoff token is missing, expired, or already used");
      return defaultTenantRedirect(PageServlet.WORKSPACE_SELECTOR_PATH);
    }

    // Check the user information in the default site
    Workspace workspace = WorkspaceRepository.findById(handoff.getWorkspaceId());
    boolean hasAccess = WorkspaceAccessCommand.hasAccess(handoff.getMainTenantUserId(), workspace);
    if (!hasAccess) {
      LOG.warn("Main Tenant User " + handoff.getMainTenantUserId() + " no longer has access to workspace "
          + handoff.getWorkspaceId());
      return defaultTenantRedirect(PageServlet.WORKSPACE_SELECTOR_PATH);
    }

    // Create or update the user record in the workspace
    User defaultUser = WorkspaceContextManager.withoutWorkspace(
        () -> UserRepository.findByUserId(handoff.getMainTenantUserId()));
    WorkspaceAccessGrant accessGrant = WorkspaceAccessCommand.loadAccessGrant(handoff.getMainTenantUserId(), workspace);

    User user = WorkspaceUserCommand.createOrUpdateUserInTenant(defaultUser, accessGrant);
    if (user == null) {
      LOG.warn("The user was not found");
      return null;
    }

    // Track the login
    UserLogin userLogin = new UserLogin();
    userLogin.setSource(UserSession.WORKSPACE_SOURCE);
    userLogin.setUserId(user.getId());
    userLogin.setIpAddress(request.getRemoteAddr());
    userLogin.setSessionId(request.getSession().getId());
    userLogin.setUserAgent(request.getHeader("USER-AGENT"));
    UserLoginRepository.save(userLogin);

    // Store a temporary token to log the user in
    int tenantExpirationSeconds = 1 * 24 * 60 * 60;
    String loginToken = UUID.randomUUID().toString() + user.getId();
    UserToken userToken = new UserToken();
    userToken.setUserId(user.getId());
    userToken.setLoginId(userLogin.getId());
    userToken.setToken(loginToken);
    userToken.setExpires(new Timestamp(System.currentTimeMillis() + (tenantExpirationSeconds * 1000)));
    UserTokenRepository.add(userToken);

    // Set the browser cookie in case session moves
    LOG.debug("Adding a temporary user cookie");
    Cookie cookie = new Cookie(CookieConstants.USER_TOKEN, userToken.getToken());
    if (request.isSecure()) {
      cookie.setSecure(true);
    }
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    response.addCookie(cookie);

    // Give the user a userSession
    String ipAddress = request.getRemoteAddr();
    String referer = request.getHeader("Referer");
    String userAgent = request.getHeader("USER-AGENT");
    UserSession userSession = CreateSessionCommand.createSession(UserSession.WORKSPACE_SOURCE, request.getSession().getId(),
        ipAddress,
        referer, userAgent);
    userSession.login(user);
    SaveSessionCommand.saveSession(userSession);
    request.getSession().setAttribute(SessionConstants.USER, userSession);
    LOG.info("OAuth user has been signed in: " + user.getEmail());

    return workspace.getSiteUrl();
  }

  private static String defaultTenantRedirect(String path) {
    String siteUrl = WorkspaceResolutionCommand.getDefaultWorkspaceUrl();
    if (siteUrl == null) {
      LOG.error("Default workspace URL is not configured");
      return "/";
    }
    if (siteUrl.endsWith("/")) {
      siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
    }
    return siteUrl + path;
  }
}
