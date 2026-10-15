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
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.presentation.controller.RequestConstants;

class HeaderControlWidgetTest extends WidgetBase {

  @Test
  void appearanceAndMobileControlsUseDeclarativeTypes() {
    widgetContext.getPreferences().put("type", "appearance");
    new HeaderControlWidget().execute(widgetContext);
    assertEquals("appearance", pageRequest.getAttribute("headerControlType"));
    assertEquals("/cms/header-control.jsp", widgetContext.getJsp());
    widgetContext.getPreferences().put("type", "mobileMenu");
    new HeaderControlWidget().execute(widgetContext);
    assertEquals("mobileMenu", pageRequest.getAttribute("headerControlType"));
  }

  @Test
  void regionControlsRequireAvailableChoices() {
    widgetContext.getPreferences().put("type", "region");
    new HeaderControlWidget().execute(widgetContext);
    assertNull(widgetContext.getJsp());
    pageRequest.setAttribute(RequestConstants.REGION_LIST, List.of("region"));
    try (var properties = mockStatic(LoadSitePropertyCommand.class)) {
      properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("site.regions.enabled")).thenReturn(true);
      new HeaderControlWidget().execute(widgetContext);
      assertEquals("region", pageRequest.getAttribute("headerControlType"));
    }
  }

  @Test
  void disabledRegionFeatureWinsOverAvailableChoices() {
    widgetContext.getPreferences().put("type", "region");
    pageRequest.setAttribute(RequestConstants.REGION_LIST, List.of("region"));
    try (var properties = mockStatic(LoadSitePropertyCommand.class)) {
      new HeaderControlWidget().execute(widgetContext);
      assertNull(widgetContext.getJsp());
    }
  }

  @Test
  void emptyRegionChoicesCannotForceASelectorVisible() {
    widgetContext.getPreferences().put("type", "region");
    pageRequest.setAttribute(RequestConstants.REGION_LIST, List.of());
    new HeaderControlWidget().execute(widgetContext);
    assertNull(widgetContext.getJsp());
  }

  @Test
  void unknownControlsCannotBecomeExecutableHandlers() {
    widgetContext.getPreferences().put("type", "javascript");
    assertThrows(IllegalArgumentException.class, () -> new HeaderControlWidget().execute(widgetContext));
  }
}