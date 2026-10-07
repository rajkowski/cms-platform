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

package com.simisinc.platform.presentation.controller;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zeroio.platform.presentation.controller.RoleConstants;

/**
 * Verifies a user's access to the specified web component
 *
 * @author matt rajkowski
 * @created 4/10/2022 8:51 AM
 */
public class WebComponentCommand implements Serializable {

  private static final long serialVersionUID = 536435325324169646L;
  private static Log LOG = LogFactory.getLog(WebComponentCommand.class);

  public static boolean allowsUser(Page page, UserSession userSession) {
    return allowsUser(page.getRoles(), page.getGroups(), userSession);
  }

  public static boolean allowsUser(Section section, UserSession userSession) {
    return allowsUser(section.getRoles(), section.getGroups(), userSession);
  }

  public static boolean allowsUser(Column column, UserSession userSession) {
    return allowsUser(column.getRoles(), column.getGroups(), userSession);
  }

  public static boolean allowsUser(Widget widget, UserSession userSession) {
    return allowsUser(widget.getRoles(), widget.getGroups(), userSession);
  }

  public static boolean allowsUser(List<String> roles, List<String> groups, UserSession userSession) {
    return allowsUser(roles.toArray(new String[0]), groups.toArray(new String[0]), userSession);
  }

  public static boolean allowsUser(String[] roles, String[] groups, UserSession userSession) {
    if (roles.length == 0 && groups.length == 0) {
      return true;
    }

    // Roles can be for a user that is either logged in/out
    boolean roleAllowed = roles.length == 0;
    for (String role : roles) {
      if (RoleConstants.GUEST.equals(role) && !userSession.isLoggedIn()) {
        roleAllowed = true;
      }
      if (RoleConstants.LOGGED_IN_USER.equals(role) && userSession.isLoggedIn()) {
        roleAllowed = true;
      }
      if (userSession.hasRole(role)) {
        roleAllowed = true;
      }
    }

    // Groups are for logged-in users
    boolean groupAllowed = groups.length == 0;
    for (String group : groups) {
      if (userSession.hasGroup(group)) {
        groupAllowed = true;
      }
    }
    return roleAllowed && groupAllowed;
  }
}
