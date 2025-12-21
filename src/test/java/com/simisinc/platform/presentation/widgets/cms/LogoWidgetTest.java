/*
 * Copyright 2025 Matt Rajkowski (https://github.com/rajkowski)
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
package com.simisinc.platform.presentation.widgets.cms;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.infrastructure.cache.CacheManager;

class LogoWidgetTest extends WidgetBase {

  private static List<SiteProperty> findByPrefix(String uniqueId) {
    List<SiteProperty> sitePropertyList = new ArrayList<>();
    {
      SiteProperty siteProperty = new SiteProperty();
      siteProperty.setLabel("Name of the site");
      siteProperty.setName("site.name");
      siteProperty.setValue("New Site");
      siteProperty.setId(1);
      sitePropertyList.add(siteProperty);
    }
    {
      SiteProperty siteProperty = new SiteProperty();
      siteProperty.setLabel("Logo");
      siteProperty.setName("site.logo.white");
      siteProperty.setValue("/white-color.png");
      siteProperty.setId(2);
      sitePropertyList.add(siteProperty);
    }
    return sitePropertyList;
  }

  private LoadingCache<String, List<SiteProperty>> sitePropertyListCache;

  @BeforeEach
  public void init() {
    sitePropertyListCache = Caffeine.newBuilder().build(LogoWidgetTest::findByPrefix);
  }

  @Test
  void executeColorLogoDisplay() {
    // Set widget preferences
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"logo\" style=\"margin-top:3px\" class=\"float-left margin-right-25\">" +
        "  <view>color</view>" +
        "  <maxHeight>23px</maxHeight>" +
        "</widget>");
    Assertions.assertEquals(2, widgetContext.getPreferences().size());

    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      LogoWidget widget = new LogoWidget();
      widget.execute(widgetContext);
    }

    // Verify the output
    Assertions.assertEquals(LogoWidget.JSP, widgetContext.getJsp());

    String siteTitle = (String) widgetContext.getRequest().getAttribute("siteTitle");
    Assertions.assertEquals("New Site", siteTitle);

    String logo = (String) widgetContext.getRequest().getAttribute("logoSrc");
    Assertions.assertNull(logo);
  }

  @Test
  void executeFullColorLogoDisplay() {
    // Set widget preferences
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"logo\">\n" +
            "  <view>full-color</view>\n" +
            "</widget>");
    Assertions.assertEquals(1, widgetContext.getPreferences().size());

    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      LogoWidget widget = new LogoWidget();
      widget.execute(widgetContext);
    }

    // Verify the output
    Assertions.assertEquals(LogoWidget.JSP, widgetContext.getJsp());

    String siteTitle = (String) widgetContext.getRequest().getAttribute("siteTitle");
    Assertions.assertEquals("New Site", siteTitle);

    String logo = (String) widgetContext.getRequest().getAttribute("logoSrc");
    Assertions.assertNull(logo);
  }

  @Test
  void executeWhiteLogoDisplay() {
    // Set widget preferences
    addPreferencesFromWidgetXml(widgetContext,
        "<widget name=\"logo\">\n" +
            "  <view>white</view>\n" +
            "</widget>");
    Assertions.assertEquals(1, widgetContext.getPreferences().size());

    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      LogoWidget widget = new LogoWidget();
      widget.execute(widgetContext);
    }

    // Verify the output
    Assertions.assertEquals(LogoWidget.JSP, widgetContext.getJsp());

    String siteTitle = (String) widgetContext.getRequest().getAttribute("siteTitle");
    Assertions.assertEquals("New Site", siteTitle);

    String logo = (String) widgetContext.getRequest().getAttribute("logoSrc");
    Assertions.assertNotNull(logo);
  }
}
