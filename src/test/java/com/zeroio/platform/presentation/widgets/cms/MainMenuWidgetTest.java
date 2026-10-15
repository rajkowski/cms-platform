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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.zeroio.platform.application.cms.RenderMainMenuCommand;

class MainMenuWidgetTest extends WidgetBase {

  @Test
  void modernNavigationUsesItsOwnViewAndPreservesComputedTabs() {
    when(pageRequest.getPagePath()).thenReturn("/overview");
    MenuTab home = new MenuTab();
    home.setLink("/");
    MenuTab overview = new MenuTab();
    overview.setLink("/overview");
    List<MenuTab> tabs = List.of(home, overview);
    try (var properties = mockStatic(LoadSitePropertyCommand.class);
        var navigation = mockStatic(RenderMainMenuCommand.class)) {
      properties.when(() -> LoadSitePropertyCommand.loadByNameAsBoolean("site.online")).thenReturn(true);
      navigation.when(() -> RenderMainMenuCommand.renderMainMenu("/overview", widgetContext.getUserSession(), null, true, true, true))
          .thenReturn(tabs);
      new MainMenuWidget().execute(widgetContext);
    }
    assertEquals("/cms/main-menu.jsp", widgetContext.getJsp());
    assertEquals(tabs, widgetContext.getRequest().getAttribute(RequestConstants.MASTER_MENU_TAB_LIST));
    assertEquals(Boolean.TRUE, widgetContext.getRequest().getAttribute("omitFirstHomeItem"));
  }
}