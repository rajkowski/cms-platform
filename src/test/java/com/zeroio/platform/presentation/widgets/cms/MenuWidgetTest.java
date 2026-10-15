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

package com.zeroio.platform.presentation.widgets.cms;

import static org.mockito.Mockito.mockStatic;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.presentation.controller.RequestConstants;

/**
 * @author matt rajkowski
 * @created 5/4/2022 7:00 PM
 */
class MenuWidgetTest extends WidgetBase {

  @Test
  void modernActionLinksUseASeparateView() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='menu'><links><link name='Ask AI' link='/ask-ai' icon='fa-brain'/></links></widget>");
    new MenuWidget().execute(widgetContext);
    Assertions.assertEquals("/cms/menu.jsp", widgetContext.getJsp());
  }

  @Test
  void modernActionOrderLabelsAndDestinationsAreConfigured() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='menu'><links><link name='Contact first' link='/contact-us'/><link name='Assistant' link='/ask-ai'/></links></widget>");
    new MenuWidget().execute(widgetContext);
    java.util.List<?> links = (java.util.List<?>) widgetContext.getRequest().getAttribute("linkList");
    Assertions.assertEquals("Contact first", ((java.util.Map<?, ?>) links.get(0)).get("name"));
    Assertions.assertEquals("/ask-ai", ((java.util.Map<?, ?>) links.get(1)).get("link"));
  }

  @Test
  void modernLinksKeepContextAndExternalDestinationsSeparate() {
    org.mockito.Mockito.when(pageRequest.getContextPath()).thenReturn("/cms");
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='menu'><links><link name='Internal' link='/contact-us'/><link name='External' link='https://example.org/help'/></links></widget>");
    new MenuWidget().execute(widgetContext);
    java.util.List<?> links = (java.util.List<?>) pageRequest.getAttribute("linkList");
    Assertions.assertEquals("/cms/contact-us", ((java.util.Map<?, ?>) links.get(0)).get("link"));
    Assertions.assertEquals("https://example.org/help", ((java.util.Map<?, ?>) links.get(1)).get("link"));
  }

  @Test
  void unsafeModernActionLinksAreNotRendered() {
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name='menu'><links><link name='Unsafe' link='javascript:alert(1)'/><link name='Unsafe host' link='//other.example/path'/></links></widget>");
    new MenuWidget().execute(widgetContext);
    Assertions.assertNull(pageRequest.getAttribute("linkList"));
  }

  @Test
  void execute() {
    // Set widget preferences
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"menu\">\n" +
            "  <class>vertical</class>\n" +
            "  <showWhenEmpty>false</showWhenEmpty>\n" +
            "  <links>\n" +
            "    <link name=\"Contact Us\" link=\"/contact-us\" />\n" +
            "    <link name=\"Login\" link=\"/login\" role=\"guest\" rule=\"site.login\" />\n" +
            "    <link name=\"Register\" link=\"/login\" role=\"guest\" rule=\"site.registrations\" />\n" +
            "    <link name=\"My Account\" link=\"/my-page\" role=\"users\" />\n" +
            "    <link name=\"Admin\" link=\"/admin\" role=\"admin\" />\n" +
            "    <link name=\"Log Out\" link=\"/logout\" role=\"users\" />\n" +
            "  </links>\n" +
            "</widget>");

    // Set the page the user is on
    pageRequest.setAttribute(RequestConstants.WEB_PAGE_PATH, "/");

    try (MockedStatic<LoadSitePropertyCommand> property = mockStatic(LoadSitePropertyCommand.class)) {
      property.when(() -> LoadSitePropertyCommand.loadByName("site.login")).thenReturn("true");
      property.when(() -> LoadSitePropertyCommand.loadByName("site.registrations")).thenReturn("false");

      {
        // Execute the widget
        MenuWidget widget = new MenuWidget();
        widget.execute(widgetContext);
        List<Map<String, String>> linkList = (List) pageRequest.getAttribute("linkList");

        // Verify the result
        Assertions.assertEquals(3, linkList.size());
        Assertions.assertEquals(MenuWidget.JSP, widgetContext.getJsp());

        // Upgrade the user to Admin
        setRoles(widgetContext, ADMIN);

        widget.execute(widgetContext);
        linkList = (List) pageRequest.getAttribute("linkList");

        // Verify the result
        Assertions.assertEquals(4, linkList.size());
        Assertions.assertEquals(MenuWidget.JSP, widgetContext.getJsp());

      }

      // Log the user out
      logout(widgetContext);

      {
        // Execute the widget
        MenuWidget widget = new MenuWidget();
        widget.execute(widgetContext);
        List<Map<String, String>> linkList = (List) pageRequest.getAttribute("linkList");

        Assertions.assertEquals(2, linkList.size());
        Assertions.assertEquals(MenuWidget.JSP, widgetContext.getJsp());
      }
    }
  }
}