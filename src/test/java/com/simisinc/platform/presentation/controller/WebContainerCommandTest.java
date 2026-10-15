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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.simisinc.platform.presentation.widgets.cms.WebContainerContext;

/**
 * @author matt rajkowski
 * @created 7/14/2022 8:00 AM
 */
class WebContainerCommandTest {
  public static class BlankWidget extends GenericWidget {
    @Override
    public WidgetContext execute(WidgetContext context) {
      context.setHtml(" \n ");
      return context;
    }
  }

  @Test
  void disabledModernDescendantsNeverResolveWidgets() throws Exception {
    for (int disabledLevel = 0; disabledLevel < 3; disabledLevel++) {
      Header header = new Header("header.default");
      Section section = new Section();
      Column column = new Column();
      Widget widget = new Widget("probe");
      section.setEnabled(disabledLevel != 0);
      column.setEnabled(disabledLevel != 1);
      widget.setEnabled(disabledLevel != 2);
      column.getWidgets().add(widget);
      section.getColumns().add(column);
      header.getSections().add(section);
      WebContainerContext context = mock(WebContainerContext.class);
      when(context.getPageRequest()).thenReturn(mock(PageRequest.class));
      when(context.getControllerSession()).thenReturn(mock(ControllerSession.class));
      HeaderRenderInfo renderInfo = new HeaderRenderInfo(header, "/current");
      WebContainerCommand.processWidgets(context, header.getSections(), renderInfo, Map.of(), mock(UserSession.class), Map.of(), null);
      verify(context, never()).getWidgetInstances();
      Assertions.assertTrue(renderInfo.getSectionRenderInfoList().isEmpty());
    }
  }

  @Test
  void modernWhitespaceOutputDoesNotCreateEmptyBands() throws Exception {
    Header header = new Header("header.default");
    Section section = new Section();
    Column column = new Column();
    column.getWidgets().add(new Widget("blank"));
    section.getColumns().add(column);
    header.getSections().add(section);
    PageRequest request = mock(PageRequest.class);
    when(request.getAttributes()).thenReturn(new HashMap<>());
    when(request.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
    WebContainerContext context = mock(WebContainerContext.class);
    when(context.getPageRequest()).thenReturn(request);
    when(context.getControllerSession()).thenReturn(mock(ControllerSession.class));
    when(context.getWidgetInstances()).thenReturn(Map.of("blank", new BlankWidget()));
    HeaderRenderInfo renderInfo = new HeaderRenderInfo(header, "/current");
    WebContainerCommand.processWidgets(context, header.getSections(), renderInfo, Map.of(), mock(UserSession.class), Map.of(), null);
    Assertions.assertTrue(renderInfo.getSectionRenderInfoList().isEmpty());
  }

  @Test
  void disabledModernHeaderDoesNotAccessWidgetState() {
    Header header = new Header("header.default");
    header.setEnabled(false);
    WebContainerContext context = mock(WebContainerContext.class);
    HeaderRenderInfo renderInfo = new HeaderRenderInfo(header, "/current");
    Assertions.assertDoesNotThrow(
        () -> WebContainerCommand.processWidgets(context, List.of(), renderInfo, Map.of(), new UserSession(), Map.of(), null));
    Assertions.assertTrue(renderInfo.getSectionRenderInfoList().isEmpty());
  }

  @Test
  void replaceVariable() {
    // User information
    User user = new User();
    user.setId(1L);
    user.setFirstName("Sam");
    user.setLastName("O'Dell");

    String content = "Hi ${user.fullName}, welcome to the site.";
    String term = "user.fullName";
    Object bean = user;
    String property = "fullName";

    String value = WebContainerCommand.replaceVariable(content, term, bean, property);

    Assertions.assertEquals("Hi Sam O'Dell, welcome to the site.", value);
  }

  @Test
  void replaceSqlVariable() {
    // User information
    User user = new User();
    user.setId(1L);
    user.setFirstName("Sam");
    user.setLastName("O'Dell");

    String content = "Hi ${user.fullName:sql}, welcome to the site.";
    String term = "user.fullName";
    Object bean = user;
    String property = "fullName";

    String value = WebContainerCommand.replaceVariable(content, term, bean, property);

    Assertions.assertEquals("Hi Sam O''Dell, welcome to the site.", value);
  }

  @Test
  void replaceHtmlVariable() {
    // User information
    User user = new User();
    user.setId(1L);
    user.setNickname("\"Goal\" of 75%");

    String content = "<p>${user.nickname:html}</p>";
    String term = "user.nickname";
    Object bean = user;
    String property = "nickname";

    String value = WebContainerCommand.replaceVariable(content, term, bean, property);

    Assertions.assertEquals("<p>&quot;Goal&quot; of 75%</p>", value);
  }

  @Test
  void replaceToHtmlVariable() {
    // User information
    User user = new User();
    user.setId(1L);
    user.setNickname("\"Goal\" of 75%");

    String content = "${user.nickname:toHtml}";
    String term = "user.nickname";
    Object bean = user;
    String property = "nickname";

    String value = WebContainerCommand.replaceVariable(content, term, bean, property);

    Assertions.assertEquals("<html><head></head><body>\"Goal\" of 75%</body></html>", value);
  }

  @Test
  void replaceJsonVariable() {
    // User information
    User user = new User();
    user.setId(1L);
    user.setNickname("\"Goal\" of 75%");

    String content = "{\"nickname\":\"${user.nickname:json}\"}";
    String term = "user.nickname";
    Object bean = user;
    String property = "nickname";

    String value = WebContainerCommand.replaceVariable(content, term, bean, property);

    Assertions.assertEquals("{\"nickname\":\"\\\"Goal\\\" of 75%\"}", value);
  }

  @Test
  void replaceVariableButBlank() {
    // User information
    User user = new User();
    user.setId(1L);

    String content = "Hi ${user.fullName:sql}, welcome to the site.";
    String term = "user.fullName";
    Object bean = user;
    String property = "fullName";

    String value = WebContainerCommand.replaceVariable(content, term, bean, property);

    Assertions.assertEquals("Hi , welcome to the site.", value);
  }

  @Test
  void replaceWithParameterValue() {
    PageRequest request = mock(PageRequest.class);
    when(request.getParameter("name")).thenReturn("Sam");

    String content = "Hi ${request.name}, welcome to the site.";
    String value = WebContainerCommand.replaceVariableWithParameterValue(request, content);

    Assertions.assertEquals("Hi Sam, welcome to the site.", value);
  }

  @Test
  void replaceWithParameterValueEncoding() {
    PageRequest request = mock(PageRequest.class);
    when(request.getParameter("name")).thenReturn("Sam \"O'Dell\"");

    String content = "Hi ${request.name:html}, welcome to the site.";
    String value = WebContainerCommand.replaceVariableWithParameterValue(request, content);

    Assertions.assertEquals("Hi Sam &quot;O&apos;Dell&quot;, welcome to the site.", value);
  }

  @Test
  void loopReplaceWithParameterValueEncoding() {
    PageRequest request = mock(PageRequest.class);
    when(request.getParameter("name")).thenReturn("Sam");
    String content = "Hi ${request.name:html}, welcome to the site. Your name is ${request.name:html}.";
    while (content.contains("${request.")) {
      content = WebContainerCommand.replaceVariableWithParameterValue(request, content);
    }
    Assertions.assertEquals("Hi Sam, welcome to the site. Your name is Sam.", content);
  }

  @Test
  void loopErrorReplaceWithParameterValueEncoding() {
    PageRequest request = mock(PageRequest.class);
    when(request.getParameter("name")).thenReturn("Sam");
    String content = "Hi ${request.name, welcome to the site. Your name is ${request.name:html.";
    while (content.contains("${request.")) {
      content = WebContainerCommand.replaceVariableWithParameterValue(request, content);
    }
    Assertions.assertEquals("Hi ", content);
  }

}
