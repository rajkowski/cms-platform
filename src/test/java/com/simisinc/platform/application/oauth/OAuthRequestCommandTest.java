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
package com.simisinc.platform.application.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.simisinc.platform.domain.model.login.OAuthState;
import com.simisinc.platform.domain.model.login.OAuthToken;
import com.simisinc.platform.presentation.controller.SessionConstants;
import com.zeroio.platform.application.login.WorkspaceSessionHandoffCommand;

class OAuthRequestCommandTest {

  @Test
  void callback_defaultTenantWorkspaceId_behaviorUnchanged_noHandoffIssued() {
    OAuthToken oAuthToken = new OAuthToken();
    oAuthToken.setAccessToken("access-token");
    oAuthToken.setResource("/dashboard");

    OAuthState oAuthState = new OAuthState();
    oAuthState.setState("state-value");
    oAuthState.setResource("/dashboard");
    // No workspaceId set - this is the default tenant login flow

    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    HttpSession httpSession = Mockito.mock(HttpSession.class);
    when(request.getParameter("state")).thenReturn("state-value");
    when(request.getParameter("code")).thenReturn("code-value");
    when(request.getSession()).thenReturn(httpSession);
    when(httpSession.getAttribute(SessionConstants.USER)).thenReturn(null);

    try (MockedStatic<OAuthConfigurationCommand> configMock = Mockito.mockStatic(OAuthConfigurationCommand.class);
        MockedStatic<OAuthAccessTokenCommand> tokenMock = Mockito.mockStatic(OAuthAccessTokenCommand.class);
        MockedStatic<OAuthLoginCommand> loginMock = Mockito.mockStatic(OAuthLoginCommand.class);
        MockedStatic<OAuthAuthorizationCommand> authMock = Mockito.mockStatic(OAuthAuthorizationCommand.class);
        MockedStatic<WorkspaceSessionHandoffCommand> handoffMock = Mockito.mockStatic(WorkspaceSessionHandoffCommand.class)) {
      configMock.when(OAuthConfigurationCommand::isEnabled).thenReturn(true);
      configMock.when(OAuthConfigurationCommand::getRedirectUri).thenReturn("/oauth/callback");
      tokenMock.when(() -> OAuthAccessTokenCommand.retrieveAccessToken("state-value", "code-value")).thenReturn(oAuthToken);
      loginMock.when(() -> OAuthLoginCommand.loginTheUser(request, response, oAuthToken)).thenReturn("real-login-token");
      authMock.when(() -> OAuthAuthorizationCommand.stateIfValid("state-value")).thenReturn(oAuthState);

      String result = OAuthRequestCommand.handleRequest(request, response, "/oauth/callback");

      assertEquals("/dashboard", result);
      handoffMock.verifyNoInteractions();
    }
  }
}
