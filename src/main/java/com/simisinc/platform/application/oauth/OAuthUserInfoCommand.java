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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.AccountException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.SaveGroupCommand;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.register.SaveUserCommand;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.login.OAuthToken;
import com.simisinc.platform.infrastructure.persistence.GroupRepository;
import com.simisinc.platform.infrastructure.persistence.RoleRepository;
import com.simisinc.platform.infrastructure.persistence.UserRepository;

/**
 * Retrieves user information and uses it to login the user
 *
 * @author matt rajkowski
 * @created 4/20/22 6:19 PM
 */
public class OAuthUserInfoCommand {

  private static Log LOG = LogFactory.getLog(OAuthUserInfoCommand.class);

  /** For the given oAuthToken, create a User from the accessToken and optional idToken */
  public static User createUser(OAuthToken oAuthToken) {
    if (oAuthToken == null || StringUtils.isBlank(oAuthToken.getAccessToken())) {
      LOG.warn("accessToken is required for createUser");
      return null;
    }
    LOG.debug("Determining user information...");

    // Find out if this is a new or returning user
    User userBean = new User();

    // Use accessToken itself to get values for display purposes
    String accessToken = oAuthToken.getAccessToken();
    if (accessToken.contains(".")) {
      LOG.debug("Found OAuth access token...");
      JsonNode accessTokenJson = JWTCommand.parseJwt(accessToken);
      populateUserFromAccessTokenPayload(accessTokenJson, userBean);
    }

    // Use id_token if available
    boolean hasAllProperties = false;
    String idToken = oAuthToken.getIdToken();
    if (!StringUtils.isBlank(idToken) && idToken.contains(".")) {
      LOG.debug("Found OAuth id token...");
      JsonNode idTokenJson = JWTCommand.parseJwt(idToken);
      hasAllProperties = populateUserFromIdTokenPayload(idTokenJson, userBean);
      if (hasAllProperties) {
        populateUserRolesFromPayload(idTokenJson, userBean);
        populateUserGroupsFromPayload(idTokenJson, userBean);
      }
    }

    // Make a userinfo request if needed
    if (!hasAllProperties) {
      LOG.debug("Making a userinfo request for user properties...");
      JsonNode userInfoJson = OAuthHttpCommand.sendHttpGet(OAuthConfigurationCommand.retrieveUserInfoEndpoint(), oAuthToken);
      hasAllProperties = populateUserFromIdTokenPayload(userInfoJson, userBean);
      if (!hasAllProperties) {
        LOG.warn("preferred_username OR email is required");
        return null;
      }
      populateUserRolesFromPayload(userInfoJson, userBean);
      populateUserGroupsFromPayload(userInfoJson, userBean);
    }
    return validateUserRecord(userBean);
  }

  /** Finalize validating and saving the user record */
  public static User validateUserRecord(User userBean) {
    // Search by optional email address first since that is a key
    User user = null;
    if (StringUtils.isNotBlank(userBean.getEmail())) {
      user = UserRepository.findByEmailAddress(userBean.getEmail());
    }
    // Then try username
    if (user == null) {
      user = UserRepository.findByUsername(userBean.getUsername());
    }
    if (user == null) {
      LOG.info("User was not found, setting up a new user: " + userBean.getEmail());
      user = new User();
    }
    // Update related values
    user.setModifiedBy(-1);
    user.setUsername(userBean.getUsername());
    user.setEmail(userBean.getEmail());
    user.setFirstName(userBean.getFirstName());
    user.setLastName(userBean.getLastName());
    user.setGroupList(userBean.getGroupList());
    user.setRoleList(userBean.getRoleList());
    // Save everything
    try {
      user = SaveUserCommand.saveUser(user, true);
      if (user == null) {
        LOG.error("User is null");
        throw new DataException("Save user error");
      }
      // Skip email validation
      UserRepository.updateValidated(user);
      return user;
    } catch (DataException | AccountException de) {
      LOG.error("User could not be saved: " + de.getMessage(), de);
      return null;
    }
  }

  /** Populate some user fields from the access token */
  public static boolean populateUserFromAccessTokenPayload(JsonNode json, User user) {
    // Validate args
    if (json == null) {
      return false;
    }
    // Update the user values
    if (json.has("given_name")) {
      JsonNode node = json.get("given_name");
      user.setFirstName(node.asText());
    }
    if (json.has("family_name")) {
      JsonNode node = json.get("family_name");
      user.setLastName(node.asText());
    }
    return true;
  }

  /** Populate user info from an id_token or user_info token */
  public static boolean populateUserFromIdTokenPayload(JsonNode json, User user) {
    // Validate args
    if (json == null) {
      return false;
    }
    // Update the user values
    if (json.has("preferred_username")) {
      user.setUsername(json.get("preferred_username").asText().trim().toLowerCase());
    }
    if (json.has("email")) {
      user.setEmail(json.get("email").asText().trim().toLowerCase());
      if (StringUtils.isBlank(user.getUsername())) {
        user.setUsername(user.getEmail());
      }
    }
    if (json.has("given_name")) {
      user.setFirstName(json.get("given_name").asText());
    }
    if (json.has("family_name")) {
      user.setLastName(json.get("family_name").asText());
    }
    if (json.has("email_verified")) {
      JsonNode node = json.get("email_verified");
      if (node.asBoolean(false)) {
        user.setValidated(new Timestamp(System.currentTimeMillis()));
      }
    }
    return !StringUtils.isAnyBlank(user.getEmail(), user.getUsername());
  }

  /** Populate the users roles from the token payload */
  public static void populateUserRolesFromPayload(JsonNode json, User user) {
    LOG.debug("Checking OAuth roles...");
    List<Role> userRoleList = user.getRoleList();
    if (userRoleList == null) {
      userRoleList = new ArrayList<>();
    }
    String roleAttribute = LoadSitePropertyCommand.loadByName("oauth.role.attribute");
    if (StringUtils.isNotBlank(roleAttribute) && json.has(roleAttribute)) {
      String oauthAdminRoleValue = LoadSitePropertyCommand.loadByName("oauth.role.admin");
      for (JsonNode jsonNode : json.get(roleAttribute)) {
        String roleValue = jsonNode.asText();
        Role thisRole = RoleRepository.findByOAuthPath(roleValue);
        // Determine if there is a setting to promote this user to admin based on role value
        if (thisRole == null && StringUtils.isNotBlank(oauthAdminRoleValue) && roleValue.equals(oauthAdminRoleValue)) {
          LOG.debug("User promoted to admin");
          thisRole = RoleRepository.findByCode("admin");
        }
        if (thisRole != null) {
          userRoleList.add(thisRole);
        }
      }
    }
    user.setRoleList(userRoleList);
  }

  /** Populate the users groups from the token payload */
  public static void populateUserGroupsFromPayload(JsonNode json, User user) {
    LOG.debug("Checking OAuth groups...");
    // All users must belong to the base "All Users" group
    Group defaultGroup = GroupRepository.findByName("All Users");
    List<Group> userGroupList = user.getGroupList();
    if (userGroupList == null) {
      userGroupList = new ArrayList<>();
    }
    userGroupList.add(defaultGroup);
    // Check the oauth attribute and values
    String groupAttribute = LoadSitePropertyCommand.loadByName("oauth.group.attribute");
    if (StringUtils.isNotBlank(groupAttribute) && json.has(groupAttribute)) {
      // Use a list of acceptable groups from site properties
      List<String> oauthGroupList = LoadSitePropertyCommand.loadByNameAsList("oauth.group.list");
      // Iterate through the found groups, looking for a match
      for (JsonNode jsonNode : json.get(groupAttribute)) {
        String groupValue = jsonNode.asText();
        // See if the system is configured with this OAuth group value, then use it
        Group thisGroup = GroupRepository.findByOAuthPath(groupValue);
        if (thisGroup == null && oauthGroupList != null && oauthGroupList.contains(groupValue)) {
          // Create the group
          thisGroup = createGroup(groupValue);
        }
        if (thisGroup != null) {
          userGroupList.add(thisGroup);
        }
      }
    }
    user.setGroupList(userGroupList);
  }

  /** Creates a user group on-the-fly unless one exists, then returns the group */
  private static synchronized Group createGroup(String groupValue) {
    LOG.debug("Creating a group... " + groupValue);
    Group thisGroup = null;
    try {
      // Create the group based on the group value provided
      Group groupBean = new Group();
      groupBean.setName(groupValue);
      groupBean.setDescription("Added from OAuth");
      groupBean.setOAuthPath(groupValue);
      thisGroup = SaveGroupCommand.saveGroup(groupBean);
      LOG.info("A group was added from OAuth: " + groupValue);
    } catch (Exception e) {
      // Already exists?, reload
      LOG.warn("Could not save group", e);
      thisGroup = GroupRepository.findByUniqueId(groupValue);
    }
    return thisGroup;
  }
}
