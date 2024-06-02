/*
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

package com.simisinc.platform.application.admin;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.infrastructure.cache.CacheManager;

/**
 * @author matt rajkowski
 * @created 5/3/2022 7:00 PM
 */
class LoadSitePropertyCommandTest {

  private static List<SiteProperty> findByPrefix(String uniqueId) {
    List<SiteProperty> sitePropertyList = new ArrayList<>();
    {
      SiteProperty siteProperty = new SiteProperty();
      siteProperty.setLabel("Name of the site");
      siteProperty.setName("site.name");
      siteProperty.setValue("New Site");
      siteProperty.setId(5);
      sitePropertyList.add(siteProperty);
    }
    {
      SiteProperty siteProperty = new SiteProperty();
      siteProperty.setLabel("Site Url");
      siteProperty.setName("site.url");
      siteProperty.setValue("https://simiscms.com");
      siteProperty.setId(8);
      sitePropertyList.add(siteProperty);
    }
    {
      SiteProperty siteProperty = new SiteProperty();
      siteProperty.setLabel("Header Link Name");
      siteProperty.setName("site.header.link");
      siteProperty.setValue("");
      siteProperty.setId(31);
      sitePropertyList.add(siteProperty);
    }
    {
      SiteProperty siteProperty = new SiteProperty();
      siteProperty.setLabel("Sample list of comma-separated values");
      siteProperty.setName("oauth.group.list");
      siteProperty.setValue("Group1, Group_2, Group-3");
      siteProperty.setId(185);
      sitePropertyList.add(siteProperty);
    }
    return sitePropertyList;
  }

  private LoadingCache<String, List<SiteProperty>> sitePropertyListCache;

  @BeforeEach
  public void init() {
    sitePropertyListCache = Caffeine.newBuilder().build(LoadSitePropertyCommandTest::findByPrefix);
  }

  @Test
  void loadAsMap() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      Map<String, String> sitePropertyMap = LoadSitePropertyCommand.loadAsMap("site");

      Assertions.assertEquals(4, sitePropertyMap.size());
      Assertions.assertEquals("New Site", sitePropertyMap.get("site.name"));
      Assertions.assertEquals("https://simiscms.com", sitePropertyMap.get("site.url"));
      Assertions.assertTrue(sitePropertyMap.containsKey("site.header.link"));
    }
  }

  @Test
  void loadAsMapSkipEmpty() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      Map<String, String> sitePropertyMap = LoadSitePropertyCommand.loadNonEmptyAsMap("site");

      Assertions.assertEquals(3, sitePropertyMap.size());
      Assertions.assertEquals("New Site", sitePropertyMap.get("site.name"));
      Assertions.assertEquals("https://simiscms.com", sitePropertyMap.get("site.url"));
      Assertions.assertFalse(sitePropertyMap.containsKey("site.header.link"));
    }
  }

  @Test
  void loadByName() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      String value = LoadSitePropertyCommand.loadByName("site.url");
      Assertions.assertNotNull(value);
      Assertions.assertEquals("https://simiscms.com", value);
    }
  }

  @Test
  void loadByNameWithDefaultValue() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      String value = LoadSitePropertyCommand.loadByName("site.url", "https://example.com");
      Assertions.assertNotNull(value);
      Assertions.assertEquals("https://simiscms.com", value);

      value = LoadSitePropertyCommand.loadByName("site.example", "https://example.com");
      Assertions.assertNotNull(value);
      Assertions.assertEquals("https://example.com", value);
    }
  }

  @Test
  void loadByNameAsList() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      List<String> valueList = LoadSitePropertyCommand.loadByNameAsList("oauth.group.list");
      Assertions.assertNotNull(valueList);
      Assertions.assertTrue(!valueList.isEmpty());
      Assertions.assertEquals(3, valueList.size());
      Assertions.assertTrue(valueList.contains("Group1"));
      Assertions.assertTrue(valueList.contains("Group_2"));
      Assertions.assertTrue(valueList.contains("Group-3"));
    }
  }

  @Test
  void testGetValueBasedOnEnvironment() {
    SiteProperty siteProperty = new SiteProperty();
    siteProperty.setName("site.url");
    siteProperty.setType("url");

    // Check local URLs
    siteProperty.setValue("http://localhost:8080");
    Assertions.assertEquals("http://localhost:8080", LoadSitePropertyCommand.getValueBasedOnEnvironment(siteProperty));

    siteProperty.setValue("http://localhost");
    Assertions.assertEquals("http://localhost", LoadSitePropertyCommand.getValueBasedOnEnvironment(siteProperty));

    siteProperty.setValue("http://localhost:8080/cms");
    Assertions.assertEquals("http://localhost:8080/cms", LoadSitePropertyCommand.getValueBasedOnEnvironment(siteProperty));

    // Check URL with path
    siteProperty.setValue("http://localhost/cms");
    Assertions.assertEquals("http://localhost/cms", LoadSitePropertyCommand.getValueBasedOnEnvironment(siteProperty));

    siteProperty.setValue("http://example.com/cms");
    Assertions.assertEquals("http://example.com/cms", LoadSitePropertyCommand.getValueBasedOnEnvironment(siteProperty));

    // Check invalid URLs
    siteProperty.setValue("http://example.com1/cms\n");
    Assertions.assertEquals("", LoadSitePropertyCommand.getValueBasedOnEnvironment(siteProperty));

    siteProperty.setValue("ftp://example.com1/cms");
    Assertions.assertEquals("", LoadSitePropertyCommand.getValueBasedOnEnvironment(siteProperty));
  }
}