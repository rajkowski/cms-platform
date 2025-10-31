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
package com.simisinc.platform.application.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.register.SaveUserCommand;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.login.OAuthToken;
import com.simisinc.platform.infrastructure.persistence.GroupRepository;
import com.simisinc.platform.infrastructure.persistence.RoleRepository;
import com.simisinc.platform.infrastructure.persistence.UserRepository;

// These tests use a well-known public sample payload from Microsoft documentation for parsing tests.
class OAuthUserInfoCommandTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void testPopulateUserFromAccessTokenJWT() {
    // Check JWT using test token from https://learn.microsoft.com/en-us/entra/identity-platform/access-tokens
    String accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Imk2bEdrM0ZaenhSY1ViMkMzbkVRN3N5SEpsWSJ9.eyJhdWQiOiI2ZTc0MTcyYi1iZTU2LTQ4NDMtOWZmNC1lNjZhMzliYjEyZTMiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3L3YyLjAiLCJpYXQiOjE1MzcyMzEwNDgsIm5iZiI6MTUzNzIzMTA0OCwiZXhwIjoxNTM3MjM0OTQ4LCJhaW8iOiJBWFFBaS84SUFBQUF0QWFaTG8zQ2hNaWY2S09udHRSQjdlQnE0L0RjY1F6amNKR3hQWXkvQzNqRGFOR3hYZDZ3TklJVkdSZ2hOUm53SjFsT2NBbk5aY2p2a295ckZ4Q3R0djMzMTQwUmlvT0ZKNGJDQ0dWdW9DYWcxdU9UVDIyMjIyZ0h3TFBZUS91Zjc5UVgrMEtJaWpkcm1wNjlSY3R6bVE9PSIsImF6cCI6IjZlNzQxNzJiLWJlNTYtNDg0My05ZmY0LWU2NmEzOWJiMTJlMyIsImF6cGFjciI6IjAiLCJuYW1lIjoiQWJlIExpbmNvbG4iLCJvaWQiOiI2OTAyMjJiZS1mZjFhLTRkNTYtYWJkMS03ZTRmN2QzOGU0NzQiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhYmVsaUBtaWNyb3NvZnQuY29tIiwicmgiOiJJIiwic2NwIjoiYWNjZXNzX2FzX3VzZXIiLCJzdWIiOiJIS1pwZmFIeVdhZGVPb3VZbGl0anJJLUtmZlRtMjIyWDVyclYzeERxZktRIiwidGlkIjoiNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3IiwidXRpIjoiZnFpQnFYTFBqMGVRYTgyUy1JWUZBQSIsInZlciI6IjIuMCJ9.pj4N-w_3Us9DrBLfpCt";
    JsonNode json = JWTCommand.parseJwt(accessToken);
    assertNotNull(json);
    assertEquals("Abe Lincoln", json.get("name").asText());

    // Check method (this JWT doesn't have discrete values)
    User user = new User();
    boolean result = OAuthUserInfoCommand.populateUserFromAccessTokenPayload(json, user);
    assertTrue(result);
  }

  @Test
  void testPopulateUserFromIdTokenJWT() {
    // Check JWT using test token from https://learn.microsoft.com/en-us/entra/identity-platform/access-tokens
    String idToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IjFMVE16YWtpaGlSbGFfOHoyQkVKVlhlV01xbyJ9.eyJ2ZXIiOiIyLjAiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTEyMjA0MGQtNmM2Ny00YzViLWIxMTItMzZhMzA0YjY2ZGFkL3YyLjAiLCJzdWIiOiJBQUFBQUFBQUFBQUFBQUFBQUFBQUFJa3pxRlZyU2FTYUZIeTc4MmJidGFRIiwiYXVkIjoiNmNiMDQwMTgtYTNmNS00NmE3LWI5OTUtOTQwYzc4ZjVhZWYzIiwiZXhwIjoxNTM2MzYxNDExLCJpYXQiOjE1MzYyNzQ3MTEsIm5iZiI6MTUzNjI3NDcxMSwibmFtZSI6IkFiZSBMaW5jb2xuIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiQWJlTGlAbWljcm9zb2Z0LmNvbSIsIm9pZCI6IjAwMDAwMDAwLTAwMDAtMDAwMC02NmYzLTMzMzJlY2E3ZWE4MSIsInRpZCI6IjkxMjIwNDBkLTZjNjctNGM1Yi1iMTEyLTM2YTMwNGI2NmRhZCIsIm5vbmNlIjoiMTIzNTIzIiwiYWlvIjoiRGYyVVZYTDFpeCFsTUNXTVNPSkJjRmF0emNHZnZGR2hqS3Y4cTVnMHg3MzJkUjVNQjVCaXN2R1FPN1lXQnlqZDhpUURMcSFlR2JJRGFreXA1bW5PcmNkcUhlWVNubHRlcFFtUnA2QUlaOGpZIn0.1AFWW-Ck5nROwSlltm7GzZvDwUkqvhSQpm55TQsmVo9Y59cLhRXpvB8n-55HCr9Z6G_31_UbeUkoz612I2j_Sm9FFShSDDjoaLQr54CreGIJvjtmS3EkK9a7SJBbcpL1MpUtlfygow39tFjY7EVNW9plWUvRrTgVk7lYLprvfzw-CIqw3gHC-T7IK_m_xkr08INERBtaecwhTeN4chPC4W3jdmw_lIxzC48YoQ0dB1L9-ImX98Egypfrlbm0IBL5spFzL6JDZIRRJOu8vecJvj1mq-IUhGt0MacxX8jdxYLP-KUu2d9MbNKpCKJuZ7p8gwTL5B7NlUdh_dmSviPWrw";
    JsonNode json = JWTCommand.parseJwt(idToken);
    assertNotNull(json);
    assertEquals("Abe Lincoln", json.get("name").asText());
    assertEquals("AbeLi@microsoft.com", json.get("preferred_username").asText());

    User user = new User();
    boolean result = OAuthUserInfoCommand.populateUserFromIdTokenPayload(json, user);
    assertFalse(result);
    assertNull(user.getEmail());
    assertEquals("abeli@microsoft.com", user.getUsername());
  }

  @Test
  void testCreateUserWithCompleteJwt() throws Exception {
    OAuthToken token = new OAuthToken();
    token.setAccessToken("access.token");
    token.setIdToken("id.token");

    JsonNode accessJson = OBJECT_MAPPER.readTree("{\"given_name\":\"Abe\",\"family_name\":\"Lincoln\"}");
    JsonNode idJson = OBJECT_MAPPER.readTree("{\"preferred_username\":\"abe@example.com\",\"email\":\"abe@example.com\",\"given_name\":\"Abe\",\"family_name\":\"Lincoln\",\"email_verified\":true,\"roles\":[\"ROLE_EDITOR\"],\"groups\":[\"ITS-Group\"]}");

    Group defaultGroup = new Group();
    defaultGroup.setName("All Users");
    defaultGroup.setUniqueId("default-group");

    Group membershipGroup = new Group();
    membershipGroup.setName("ITS-Group");
    membershipGroup.setUniqueId("its-group");
    membershipGroup.setOAuthPath("ITS-Group");

    Role membershipRole = new Role();
    membershipRole.setCode("editor");
    membershipRole.setOAuthPath("ROLE_EDITOR");

    try (MockedStatic<JWTCommand> jwtMock = Mockito.mockStatic(JWTCommand.class);
        MockedStatic<LoadSitePropertyCommand> siteMock = Mockito.mockStatic(LoadSitePropertyCommand.class);
        MockedStatic<GroupRepository> groupRepoMock = Mockito.mockStatic(GroupRepository.class);
        MockedStatic<RoleRepository> roleRepoMock = Mockito.mockStatic(RoleRepository.class);
        MockedStatic<SaveUserCommand> saveUserMock = Mockito.mockStatic(SaveUserCommand.class);
        MockedStatic<UserRepository> userRepoMock = Mockito.mockStatic(UserRepository.class);
        MockedStatic<OAuthHttpCommand> httpMock = Mockito.mockStatic(OAuthHttpCommand.class)) {

      jwtMock.when(() -> JWTCommand.parseJwt("access.token")).thenReturn(accessJson);
      jwtMock.when(() -> JWTCommand.parseJwt("id.token")).thenReturn(idJson);

    siteMock.when(() -> LoadSitePropertyCommand.loadByName(Mockito.anyString())).thenReturn(null);
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.role.attribute")).thenReturn("roles");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.group.attribute")).thenReturn("groups");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.role.admin")).thenReturn("ADMIN_ROLE");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.graph.ad.prefixes")).thenReturn(null);
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.graph.group.overrides")).thenReturn(null);

    siteMock.when(() -> LoadSitePropertyCommand.loadByNameAsList(Mockito.anyString())).thenReturn(null);
    siteMock.when(() -> LoadSitePropertyCommand.loadByNameAsList("oauth.group.list"))
      .thenReturn(List.of("ITS-Group"));

      groupRepoMock.when(() -> GroupRepository.findByName("All Users")).thenReturn(defaultGroup);
    groupRepoMock.when(() -> GroupRepository.findByOAuthPath(Mockito.anyString())).thenReturn(null);
    groupRepoMock.when(() -> GroupRepository.findByOAuthPath("ITS-Group")).thenReturn(membershipGroup);
      groupRepoMock.when(() -> GroupRepository.findByUniqueId(Mockito.anyString())).thenReturn(null);

    roleRepoMock.when(() -> RoleRepository.findByOAuthPath(Mockito.anyString())).thenReturn(null);
    roleRepoMock.when(() -> RoleRepository.findByOAuthPath("ROLE_EDITOR")).thenReturn(membershipRole);
      roleRepoMock.when(() -> RoleRepository.findByCode("admin")).thenReturn(null);

      saveUserMock.when(() -> SaveUserCommand.saveUser(Mockito.any(User.class), Mockito.eq(true)))
          .thenAnswer(inv -> inv.getArgument(0));

      userRepoMock.when(() -> UserRepository.findByEmailAddress(Mockito.anyString())).thenReturn(null);
      userRepoMock.when(() -> UserRepository.findByUsername(Mockito.anyString())).thenReturn(null);
      userRepoMock.when(() -> UserRepository.updateValidated(Mockito.any(User.class))).thenAnswer(inv -> null);

      httpMock.when(() -> OAuthHttpCommand.sendHttpGet(Mockito.anyString(), Mockito.any(OAuthToken.class)))
          .thenAnswer(inv -> {
            throw new AssertionError("userinfo request not expected");
          });
      httpMock.when(() -> OAuthHttpCommand.sendHttpGet(Mockito.anyString(), Mockito.anyMap(), Mockito.any(OAuthToken.class)))
          .thenAnswer(inv -> {
            throw new AssertionError("graph request not expected");
          });

      User result = OAuthUserInfoCommand.createUser(token);

      assertNotNull(result);
      assertEquals("abe@example.com", result.getEmail());
      assertEquals("abe@example.com", result.getUsername());
      assertEquals(2, result.getGroupList().size());
      assertTrue(result.getGroupList().stream().anyMatch(g -> "ITS-Group".equals(g.getName())));
      assertTrue(result.getRoleList().stream().anyMatch(r -> "ROLE_EDITOR".equals(r.getOAuthPath())));
    }
  }

  @Test
  void testCreateUserRequiresUserInfoRequest() throws Exception {
    OAuthToken token = new OAuthToken();
    token.setAccessToken("access.token");
    token.setIdToken("id.token");

    JsonNode accessJson = OBJECT_MAPPER.readTree("{\"given_name\":\"Abe\",\"family_name\":\"Lincoln\"}");
    JsonNode idJson = OBJECT_MAPPER.readTree("{\"preferred_username\":\"abe@example.com\",\"given_name\":\"Abe\",\"family_name\":\"Lincoln\"}");
    JsonNode userInfoJson = OBJECT_MAPPER.readTree("{\"preferred_username\":\"abe@example.com\",\"email\":\"abe@example.com\",\"given_name\":\"Abe\",\"family_name\":\"Lincoln\",\"email_verified\":true,\"roles\":[\"ROLE_EDITOR\"],\"groups\":[\"ITS-Group\"]}");

    Group defaultGroup = new Group();
    defaultGroup.setName("All Users");
    defaultGroup.setUniqueId("default-group");

    Group membershipGroup = new Group();
    membershipGroup.setName("ITS-Group");
    membershipGroup.setUniqueId("its-group");
    membershipGroup.setOAuthPath("ITS-Group");

    Role membershipRole = new Role();
    membershipRole.setCode("editor");
    membershipRole.setOAuthPath("ROLE_EDITOR");

    try (MockedStatic<JWTCommand> jwtMock = Mockito.mockStatic(JWTCommand.class);
        MockedStatic<LoadSitePropertyCommand> siteMock = Mockito.mockStatic(LoadSitePropertyCommand.class);
        MockedStatic<GroupRepository> groupRepoMock = Mockito.mockStatic(GroupRepository.class);
        MockedStatic<RoleRepository> roleRepoMock = Mockito.mockStatic(RoleRepository.class);
        MockedStatic<SaveUserCommand> saveUserMock = Mockito.mockStatic(SaveUserCommand.class);
        MockedStatic<UserRepository> userRepoMock = Mockito.mockStatic(UserRepository.class);
        MockedStatic<OAuthHttpCommand> httpMock = Mockito.mockStatic(OAuthHttpCommand.class);
        MockedStatic<OAuthConfigurationCommand> configMock = Mockito.mockStatic(OAuthConfigurationCommand.class)) {

      jwtMock.when(() -> JWTCommand.parseJwt("access.token")).thenReturn(accessJson);
      jwtMock.when(() -> JWTCommand.parseJwt("id.token")).thenReturn(idJson);

    siteMock.when(() -> LoadSitePropertyCommand.loadByName(Mockito.anyString())).thenReturn(null);
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.role.attribute")).thenReturn("roles");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.group.attribute")).thenReturn("groups");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.role.admin")).thenReturn("ADMIN_ROLE");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.graph.ad.prefixes")).thenReturn(null);
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.graph.group.overrides")).thenReturn(null);

    siteMock.when(() -> LoadSitePropertyCommand.loadByNameAsList(Mockito.anyString())).thenReturn(null);
    siteMock.when(() -> LoadSitePropertyCommand.loadByNameAsList("oauth.group.list"))
      .thenReturn(List.of("ITS-Group"));

      groupRepoMock.when(() -> GroupRepository.findByName("All Users")).thenReturn(defaultGroup);
    groupRepoMock.when(() -> GroupRepository.findByOAuthPath(Mockito.anyString())).thenReturn(null);
    groupRepoMock.when(() -> GroupRepository.findByOAuthPath("ITS-Group")).thenReturn(membershipGroup);
      groupRepoMock.when(() -> GroupRepository.findByUniqueId(Mockito.anyString())).thenReturn(null);

    roleRepoMock.when(() -> RoleRepository.findByOAuthPath(Mockito.anyString())).thenReturn(null);
    roleRepoMock.when(() -> RoleRepository.findByOAuthPath("ROLE_EDITOR")).thenReturn(membershipRole);
      roleRepoMock.when(() -> RoleRepository.findByCode("admin")).thenReturn(null);

      saveUserMock.when(() -> SaveUserCommand.saveUser(Mockito.any(User.class), Mockito.eq(true)))
          .thenAnswer(inv -> inv.getArgument(0));

      userRepoMock.when(() -> UserRepository.findByEmailAddress(Mockito.anyString())).thenReturn(null);
      userRepoMock.when(() -> UserRepository.findByUsername(Mockito.anyString())).thenReturn(null);
      userRepoMock.when(() -> UserRepository.updateValidated(Mockito.any(User.class))).thenAnswer(inv -> null);

      configMock.when(OAuthConfigurationCommand::retrieveUserInfoEndpoint).thenReturn("https://graph/userinfo");

      httpMock.when(() -> OAuthHttpCommand.sendHttpGet(Mockito.anyString(), Mockito.any(OAuthToken.class)))
          .thenAnswer(inv -> {
            String endpoint = inv.getArgument(0);
            if ("https://graph/userinfo".equals(endpoint)) {
              return userInfoJson;
            }
            throw new AssertionError("Unexpected userinfo request: " + endpoint);
          });
      httpMock.when(() -> OAuthHttpCommand.sendHttpGet(Mockito.anyString(), Mockito.anyMap(), Mockito.any(OAuthToken.class)))
          .thenAnswer(inv -> {
            throw new AssertionError("graph request not expected");
          });

      User result = OAuthUserInfoCommand.createUser(token);

      assertNotNull(result);
      assertEquals("abe@example.com", result.getEmail());
      assertEquals("abe@example.com", result.getUsername());
      assertEquals(2, result.getGroupList().size());
      assertTrue(result.getGroupList().stream().anyMatch(g -> "ITS-Group".equals(g.getName())));
      assertTrue(result.getRoleList().stream().anyMatch(r -> "ROLE_EDITOR".equals(r.getOAuthPath())));

      httpMock.verify(() -> OAuthHttpCommand.sendHttpGet("https://graph/userinfo", token));
    }
  }

  @Test
  void testCreateUserUsesGraphFallback() throws Exception {
    OAuthToken token = new OAuthToken();
    token.setAccessToken("access.token");
    token.setIdToken("id.token");

    JsonNode accessJson = OBJECT_MAPPER.readTree("{\"given_name\":\"Abe\",\"family_name\":\"Lincoln\"}");
    JsonNode idJson = OBJECT_MAPPER.readTree("{\"preferred_username\":\"abe@example.com\",\"email\":\"abe@example.com\",\"given_name\":\"Abe\",\"family_name\":\"Lincoln\",\"email_verified\":true}");
    JsonNode graphJson = OBJECT_MAPPER.readTree("{\"value\":[{\"displayName\":null,\"id\":\"123-123\"}]}");

    Group defaultGroup = new Group();
    defaultGroup.setName("All Users");
    defaultGroup.setUniqueId("default-group");

    Group graphGroup = new Group();
    graphGroup.setName("ITS-Graph");
    graphGroup.setUniqueId("its-graph");
    graphGroup.setOAuthPath("ITS-Graph");

    Role graphRole = new Role();
    graphRole.setCode("graph-role");
    graphRole.setOAuthPath("ITS-Graph");

    try (MockedStatic<JWTCommand> jwtMock = Mockito.mockStatic(JWTCommand.class);
        MockedStatic<LoadSitePropertyCommand> siteMock = Mockito.mockStatic(LoadSitePropertyCommand.class);
        MockedStatic<GroupRepository> groupRepoMock = Mockito.mockStatic(GroupRepository.class);
        MockedStatic<RoleRepository> roleRepoMock = Mockito.mockStatic(RoleRepository.class);
        MockedStatic<SaveUserCommand> saveUserMock = Mockito.mockStatic(SaveUserCommand.class);
        MockedStatic<UserRepository> userRepoMock = Mockito.mockStatic(UserRepository.class);
        MockedStatic<OAuthHttpCommand> httpMock = Mockito.mockStatic(OAuthHttpCommand.class);
        MockedStatic<OAuthConfigurationCommand> configMock = Mockito.mockStatic(OAuthConfigurationCommand.class)) {

      jwtMock.when(() -> JWTCommand.parseJwt("access.token")).thenReturn(accessJson);
      jwtMock.when(() -> JWTCommand.parseJwt("id.token")).thenReturn(idJson);

    siteMock.when(() -> LoadSitePropertyCommand.loadByName(Mockito.anyString())).thenReturn(null);
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.role.attribute")).thenReturn("roles");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.group.attribute")).thenReturn("groups");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.role.admin")).thenReturn("ADMIN_ROLE");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.graph.ad.prefixes")).thenReturn("ITS-");
    siteMock.when(() -> LoadSitePropertyCommand.loadByName("oauth.graph.group.overrides")).thenReturn("ITS-Graph=123-123");

    siteMock.when(() -> LoadSitePropertyCommand.loadByNameAsList(Mockito.anyString())).thenReturn(null);
    siteMock.when(() -> LoadSitePropertyCommand.loadByNameAsList("oauth.group.list"))
      .thenReturn(List.of("ITS-Graph"));

      groupRepoMock.when(() -> GroupRepository.findByName("All Users")).thenReturn(defaultGroup);
    groupRepoMock.when(() -> GroupRepository.findByOAuthPath(Mockito.anyString())).thenReturn(null);
    groupRepoMock.when(() -> GroupRepository.findByOAuthPath("ITS-Graph")).thenReturn(graphGroup);
      groupRepoMock.when(() -> GroupRepository.findByUniqueId(Mockito.anyString())).thenReturn(null);

    roleRepoMock.when(() -> RoleRepository.findByOAuthPath(Mockito.anyString())).thenReturn(null);
    roleRepoMock.when(() -> RoleRepository.findByOAuthPath("ITS-Graph")).thenReturn(graphRole);
      roleRepoMock.when(() -> RoleRepository.findByCode("admin")).thenReturn(null);

      saveUserMock.when(() -> SaveUserCommand.saveUser(Mockito.any(User.class), Mockito.eq(true)))
          .thenAnswer(inv -> inv.getArgument(0));

      userRepoMock.when(() -> UserRepository.findByEmailAddress(Mockito.anyString())).thenReturn(null);
      userRepoMock.when(() -> UserRepository.findByUsername(Mockito.anyString())).thenReturn(null);
      userRepoMock.when(() -> UserRepository.updateValidated(Mockito.any(User.class))).thenAnswer(inv -> null);

      configMock.when(() -> OAuthConfigurationCommand.retrieveUserGroupsEndpoint("ITS-"))
          .thenReturn("https://graph/groups");
      configMock.when(OAuthConfigurationCommand::retrieveUserInfoEndpoint)
          .thenReturn("https://graph/userinfo");

      httpMock.when(() -> OAuthHttpCommand.sendHttpGet(Mockito.anyString(), Mockito.any(OAuthToken.class)))
          .thenAnswer(inv -> {
            throw new AssertionError("userinfo request not expected");
          });
      httpMock.when(() -> OAuthHttpCommand.sendHttpGet(Mockito.anyString(), Mockito.anyMap(), Mockito.any(OAuthToken.class)))
          .thenAnswer(inv -> {
            String endpoint = inv.getArgument(0);
            if (!"https://graph/groups".equals(endpoint)) {
              throw new AssertionError("Unexpected graph request: " + endpoint);
            }
            return graphJson;
          });

      User result = OAuthUserInfoCommand.createUser(token);

      assertNotNull(result);
      assertEquals("abe@example.com", result.getEmail());
      assertEquals(2, result.getGroupList().size());
      assertTrue(result.getGroupList().stream().anyMatch(g -> "ITS-Graph".equals(g.getName())));
      assertTrue(result.getRoleList().stream().anyMatch(r -> "ITS-Graph".equals(r.getOAuthPath())));

  httpMock.verify(() -> OAuthHttpCommand.sendHttpGet(Mockito.eq("https://graph/groups"), Mockito.anyMap(), Mockito.eq(token)));
    }
  }
}
