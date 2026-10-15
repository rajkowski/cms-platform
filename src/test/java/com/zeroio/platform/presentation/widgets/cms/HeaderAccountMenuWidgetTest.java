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
package com.zeroio.platform.presentation.widgets.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.application.LoadUserCommand;
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.cms.WebPage;

class HeaderAccountMenuWidgetTest extends WidgetBase {

  private MockedStatic<LoadUserCommand> users;

  @BeforeEach
  void accountUserFixture() {
    User user = new User();
    user.setId(1L);
    user.setFirstName("Fixture");
    user.setLastName("User");
    users = mockStatic(LoadUserCommand.class);
    users.when(() -> LoadUserCommand.loadUser(1L)).thenReturn(user);
  }

  @AfterEach
  void closeUserFixture() {
    users.close();
  }

  private static Map<?, ?> record(Object value) {
    return Objects.requireNonNull((Map<?, ?>) value);
  }

  private static List<?> entries(Object value) {
    return Objects.requireNonNull((List<?>) value);
  }

  private static String text(Object value, String key) {
    return Objects.requireNonNull((String) record(value).get(key));
  }

  private Set<String> actionNames() {
    Object value = pageRequest.getAttribute("headerAccountSections");
    Set<String> names = new HashSet<>();
    if (value instanceof List<?> sections) {
      for (Object section : sections) {
        for (Object action : entries(record(section).get("actions"))) {
          names.add(text(action, "action"));
        }
      }
    }
    return names;
  }

  private void admin() {
    widgetContext.getUserSession().setRoleList(List.of(new Role("Administrator", "admin")));
  }

  @Test
  void configuredVisibilityCannotBroadenAnyBuiltInPermissionCeiling() {
    List<String> actions = List.of("pageInfo", "pageAnalytics", "pageDesign", "files", "images", "hierarchy", "instructions",
        "siteAnalytics", "users", "announcement", "siteCss", "setup", "appearance", "account", "logout", "link");
    StringBuilder xml = new StringBuilder("<widget name='headerAccountMenu'><links>");
    for (String action : actions) {
      xml.append("<link action='").append(action).append("' name='").append(action).append("' role='users'");
      if ("link".equals(action))
        xml.append(" link='/contact-us'");
      xml.append("/>");
    }
    xml.append("</links></widget>");
    addPreferencesFromWidgetXml(widgetContext, xml.toString());
    when(pageRequest.getPagePath()).thenReturn("/current");
    WebPage master = new WebPage();
    master.setLink("/master");
    pageRequest.setAttribute("webPage", master);
    Set<String> ordinary = Set.of("appearance", "account", "logout", "link");
    for (String role : List.of("guest", "ordinary", "admin", "content-manager", "community-manager", "data-manager")) {
      login(widgetContext);
      if ("guest".equals(role))
        logout(widgetContext);
      else if (!"ordinary".equals(role))
        widgetContext.getUserSession().setRoleList(List.of(new Role(role, role)));
      pageRequest.setAttribute("headerAccountSections", null);
      widgetContext.setJsp(null);
      new HeaderAccountMenuWidget().execute(widgetContext);
      Set<String> expected = new HashSet<>(ordinary);
      switch (role) {
        case "guest":
          expected.clear();
          break;
        case "admin":
          expected.addAll(actions);
          break;
        case "content-manager":
          expected.addAll(
              List.of("pageInfo", "pageAnalytics", "pageDesign", "files", "images", "hierarchy", "instructions", "siteAnalytics"));
          break;
        case "community-manager":
          expected.addAll(List.of("instructions", "users"));
          break;
        case "data-manager":
          expected.add("instructions");
          break;
        default:
          break;
      }
      assertEquals(expected, actionNames(), role);
    }
  }

  @Test
  void contextualDestinationsAreEncodedAndPrefixedExactlyOnce() {
    admin();
    when(pageRequest.getPagePath()).thenReturn("/page with spaces&query");
    when(pageRequest.getContextPath()).thenReturn("/cms");
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='pageInfo' name='Info'/><link action='account' name='Account' link='/cms/account'/></links></widget>");
    new HeaderAccountMenuWidget().execute(widgetContext);
    List<?> sections = (List<?>) pageRequest.getAttribute("headerAccountSections");
    List<?> actions = entries(record(sections.get(0)).get("actions"));
    assertEquals("/cms/admin/web-page?webPage=%2Fpage+with+spaces%26query", text(actions.get(0), "link"));
    assertEquals("/cms/account", text(actions.get(1), "link"));
  }

  @Test
  void missingContextOnlySuppressesTheDependentAction() {
    admin();
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='pageDesign' name='Design'/><link action='account' name='Account'/></links></widget>");
    new HeaderAccountMenuWidget().execute(widgetContext);
    assertEquals(Set.of("account"), actionNames());
  }

  @Test
  void disabledAndRestrictedGroupsAreOmitted() {
    admin();
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='setup' name='Disabled' enabled='false' section='tools' sectionLabel='Tools'/><link action='account' name='Restricted' group='missing'/></links></widget>");
    new HeaderAccountMenuWidget().execute(widgetContext);
    assertNull(widgetContext.getJsp());
  }

  @Test
  void absoluteContextOverridesRetainTheirAuthority() {
    admin();
    when(pageRequest.getPagePath()).thenReturn("/current");
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='pageInfo' name='Info' link='https://example.org//tools/page?mode=info#details'/></links></widget>");
    new HeaderAccountMenuWidget().execute(widgetContext);
    List<?> sections = (List<?>) pageRequest.getAttribute("headerAccountSections");
    List<?> actions = entries(record(sections.get(0)).get("actions"));
    java.net.URI uri = java.net.URI.create(text(actions.get(0), "link"));
    assertEquals("example.org", uri.getHost());
    assertEquals("//tools/page", uri.getPath());
  }

  @Test
  void configuredRestrictionsAreAdditionalToThePermissionCeiling() {
    admin();
    setGroups(widgetContext, "editors");
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='setup' name='Restricted' role='community-manager'/><link action='account' name='Allowed group' group='editors' rule='site.feature'/><link action='logout' name='Denied group' group='missing'/></links></widget>");
    try (var properties = mockStatic(com.simisinc.platform.application.admin.LoadSitePropertyCommand.class)) {
      properties.when(() -> com.simisinc.platform.application.admin.LoadSitePropertyCommand.loadByNameAsBoolean("site.feature"))
          .thenReturn(true);
      new HeaderAccountMenuWidget().execute(widgetContext);
    }
    assertEquals(Set.of("account"), actionNames());
  }

  @Test
  void contextualBindingsReplaceEarlierContextKeys() {
    admin();
    when(pageRequest.getPagePath()).thenReturn("/current");
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='pageInfo' name='Info' link='/admin/web-page?webPage=%2Fwrong&amp;mode=info'/></links></widget>");
    new HeaderAccountMenuWidget().execute(widgetContext);
    List<?> sections = (List<?>) pageRequest.getAttribute("headerAccountSections");
    List<?> actions = entries(record(sections.get(0)).get("actions"));
    String link = text(actions.get(0), "link");
    assertTrue(link.contains("webPage=%2Fcurrent"));
    assertTrue(!link.contains("wrong"));
  }

  @Test
  void contextualOverridesKeepQueryValuesBeforeTheFragment() {
    admin();
    when(pageRequest.getPagePath()).thenReturn("/current");
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='pageInfo' name='Info' link='/admin/web-page?mode=info#details'/></links></widget>");
    new HeaderAccountMenuWidget().execute(widgetContext);
    List<?> sections = (List<?>) pageRequest.getAttribute("headerAccountSections");
    List<?> actions = entries(record(sections.get(0)).get("actions"));
    java.net.URI uri = java.net.URI.create(text(actions.get(0), "link"));
    assertEquals("details", uri.getFragment());
    assertTrue(uri.getQuery().contains("webPage="));
    assertTrue(uri.getQuery().contains("mode=info"));
  }

  @Test
  void missingActionLabelsAreRejectedBeforeRendering() {
    addPreferencesFromWidgetXml(widgetContext, "<widget name='headerAccountMenu'><links><link action='account'/></links></widget>");
    assertThrows(IllegalArgumentException.class, () -> new HeaderAccountMenuWidget().execute(widgetContext));
  }

  @Test
  void appearanceDoesNotAcceptUrlOverrides() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='appearance' name='Theme' link='/unsafe-override'/></links></widget>");
    assertThrows(IllegalArgumentException.class, () -> new HeaderAccountMenuWidget().execute(widgetContext));
  }

  @Test
  void labelsRemainDataAndGroupingFollowsDocumentOrder() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='headerAccountMenu'><links><link action='account' name='&lt;strong&gt;Account&lt;/strong&gt;' section='first' sectionLabel='First'/><link action='logout' name='Logout' section='second' sectionLabel='Second'/></links></widget>");
    new HeaderAccountMenuWidget().execute(widgetContext);
    List<?> sections = (List<?>) pageRequest.getAttribute("headerAccountSections");
    assertEquals("First", text(sections.get(0), "label"));
    assertEquals("Second", text(sections.get(1), "label"));
    List<?> actions = entries(record(sections.get(0)).get("actions"));
    assertTrue(text(actions.get(0), "name").startsWith("<strong>"));
  }
}