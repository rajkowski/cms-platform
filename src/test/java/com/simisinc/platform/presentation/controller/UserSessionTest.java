/*
 * Copyright 2024 Matt Rajkowski (https://www.github.com/rajkowski)
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.User;

class UserSessionTest {
  @Test
  void testHasGroup() {

    // Related user information
    List<Group> userGroupList = new ArrayList<>();

    Group testGroup = new Group("Testers", "testers");
    testGroup.setOAuthPath("TESTERS");
    userGroupList.add(testGroup);

    Group reviewerGroup = new Group("Reviewers", "reviewers-1");
    reviewerGroup.setOAuthPath("REVIEWERS");
    userGroupList.add(reviewerGroup);

    // User information
    User user = new User();
    user.setId(1L);
    user.setGroupList(userGroupList);

    // Log the user in
    UserSession userSession = new UserSession();
    userSession.login(user);

    // User record
    Assertions.assertTrue(user.hasGroup("testers"));
    Assertions.assertTrue(user.hasGroup("TESTERS"));
    Assertions.assertTrue(user.hasGroup("reviewers-1"));
    Assertions.assertTrue(user.hasGroup("REVIEWERS"));
    Assertions.assertFalse(user.hasGroup("reviewers"));
    Assertions.assertFalse(user.hasGroup("user"));

    // UserSession record
    Assertions.assertTrue(userSession.hasGroup("testers"));
    Assertions.assertTrue(userSession.hasGroup("TESTERS"));
    Assertions.assertTrue(userSession.hasGroup("reviewers-1"));
    Assertions.assertTrue(userSession.hasGroup("REVIEWERS"));
    Assertions.assertFalse(userSession.hasGroup("reviewers"));
    Assertions.assertFalse(userSession.hasGroup("user"));
  }
}
