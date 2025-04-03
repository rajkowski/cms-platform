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

package com.simisinc.platform.application.oauth;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.RandomStringGenerator;

import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.domain.model.login.OAuthState;
import com.simisinc.platform.infrastructure.persistence.login.OAuthStateRepository;

/**
 * Configures and verifies OpenAuth2 and OIDC
 *
 * @author matt rajkowski
 * @created 4/20/22 6:19 PM
 */
public class OAuthAuthorizationCommand {

  private static Log LOG = LogFactory.getLog(OAuthAuthorizationCommand.class);

  private static RandomStringGenerator generator = new RandomStringGenerator.Builder()
      .selectFrom("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz0123456789".toCharArray()).build();

  public static String getAuthorizationUrl(String resource) {
    // Check the required configuration
    String clientId = OAuthConfigurationCommand.getClientId();
    if (StringUtils.isAnyBlank(clientId)) {
      return null;
    }
    String authEndpoint = OAuthConfigurationCommand.retrieveAuthEndpoint();
    if (StringUtils.isBlank(authEndpoint)) {
      return null;
    }

    String callback = OAuthConfigurationCommand.getRedirectUrl();

    // Store the resource to redirect back to after credentials are verified
    String state = System.currentTimeMillis() + generator.generate(14);
    if (StringUtils.isBlank(resource) || "/logout".equals(resource) || "/login".equals(resource)) {
      resource = "/";
    }
    OAuthState oAuthState = new OAuthState();
    oAuthState.setState(state);
    oAuthState.setResource(resource);
    OAuthStateRepository.add(oAuthState);

    // Prepare the oauth redirect
    String authorizationUrl = authEndpoint +
        "?client_id=" + UrlCommand.encodeUri(clientId) +
        "&response_type=code" +
        "&scope=openid%20profile%20email%20offline_access" +
        "&redirect_uri=" + UrlCommand.encodeUri(callback) +
        "&nonce=" + UrlCommand.encodeUri(state) +
        "&state=" + UrlCommand.encodeUri(state);

    LOG.debug("Using authorizationUrl: " + authorizationUrl);
    return authorizationUrl;
  }

  public static String resourceIfStateIsValid(String state) {
    OAuthState oAuthState = OAuthStateRepository.findByStateIfValid(state);
    return oAuthState != null ? oAuthState.getResource() : null;
  }
}
