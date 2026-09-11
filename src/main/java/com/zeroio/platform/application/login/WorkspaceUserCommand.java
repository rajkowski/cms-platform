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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.security.auth.login.AccountException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.register.SaveUserCommand;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.infrastructure.persistence.GroupRepository;
import com.simisinc.platform.infrastructure.persistence.RoleRepository;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.zeroio.platform.domain.model.tenant.WorkspaceAccessGrant;

public class WorkspaceUserCommand {
  private WorkspaceUserCommand() {
    /* This utility class should not be instantiated */
  }

  private static Log LOG = LogFactory.getLog(WorkspaceUserCommand.class);

  /** In the tenant, create or update a user based on the default user information */
  public static User createOrUpdateUserInTenant(User defaultUser, WorkspaceAccessGrant accessGrant) {
    User existingUser = loadUser(defaultUser);
    if (existingUser == null) {
      LOG.info("User was not found, setting up a new user: " + defaultUser.getEmail());
      return createAndSaveNewUser(defaultUser, accessGrant);
    }
    applyUserValues(existingUser, defaultUser);
    existingUser.setRoleList(loadRoles(accessGrant));
    return saveUser(existingUser);
  }

  private static User createAndSaveNewUser(User defaultUser, WorkspaceAccessGrant accessGrant) {
    User user = new User();
    applyUserValues(user, defaultUser);
    user.setRoleList(loadRoles(accessGrant));
    user.setGroupList(loadGroups(accessGrant));
    return saveUser(user);
  }

  private static User loadUser(User defaultUser) {
    User user = null;
    if (StringUtils.isNotBlank(defaultUser.getEmail())) {
      user = UserRepository.findByEmailAddress(defaultUser.getEmail());
    }
    if (user == null) {
      user = UserRepository.findByUsername(defaultUser.getUsername());
    }
    return user;
  }

  private static void applyUserValues(User user, User defaultUser) {
    user.setModifiedBy(-1);
    user.setUsername(defaultUser.getUsername());
    user.setEmail(defaultUser.getEmail());
    user.setFirstName(defaultUser.getFirstName());
    user.setLastName(defaultUser.getLastName());
  }

  private static List<Role> loadRoles(WorkspaceAccessGrant accessGrant) {
    List<Role> roles = new ArrayList<>();
    for (String roleValue : accessGrant.getRoles()) {
      Role role = RoleRepository.findByCode(roleValue);
      if (role != null) {
        roles.add(role);
      }
    }
    return roles;
  }

  private static List<Group> loadGroups(WorkspaceAccessGrant accessGrant) {
    List<Group> groups = new ArrayList<>();
    for (String groupValue : accessGrant.getGroups()) {
      Group group = GroupRepository.findByUniqueId(groupValue);
      if (group != null) {
        groups.add(group);
      }
    }
    Group defaultGroup = GroupRepository.findByUniqueId("users");
    if (defaultGroup != null && groups.stream()
        .noneMatch(g -> g != null && (Objects.equals(g.getUniqueId(), defaultGroup.getUniqueId())
            || Objects.equals(g.getName(), defaultGroup.getName())))) {
      groups.add(defaultGroup);
    }
    return groups;
  }

  private static User saveUser(User user) {
    try {
      User savedUser = SaveUserCommand.saveUser(user, true);
      if (savedUser == null) {
        LOG.error("User is null");
        throw new DataException("Save user error");
      }
      UserRepository.updateValidated(savedUser);
      return savedUser;
    } catch (DataException | AccountException de) {
      LOG.error("User could not be saved: " + de.getMessage(), de);
      return null;
    }
  }

}
