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
package com.simisinc.platform.application.cms;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.TenantRegistry;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.domain.model.cms.WebContainer;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.WebContainerRepository;
import com.simisinc.platform.presentation.controller.Footer;
import com.simisinc.platform.presentation.controller.Header;
import com.simisinc.platform.presentation.controller.XMLFooterLoader;
import com.simisinc.platform.presentation.controller.XMLHeaderLoader;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

class WebContainerLayoutCommandTest {

  @BeforeEach
  void setUp() {
    CacheManager.startup();
    DB.setTenantRegistry(new TenantRegistry());
    DB.registerTenantDataSource("1", mock(DataSource.class));
    DB.registerTenantDataSource("2", mock(DataSource.class));
  }

  @AfterEach
  void tearDown() {
    WorkspaceContextManager.clear();
    DB.setTenantRegistry(new TenantRegistry());
  }

  @Test
  void fallbackCopiesDoNotCollideWithSelectedLayoutNames() {
    Header modern = new Header("header.default");
    Header explicitlyNamed = new Header("header.default:LastValid");
    try (var repository = mockStatic(WebContainerRepository.class);
        var loader = mockStatic(XMLHeaderLoader.class)) {
      WorkspaceContextManager.activate(1L, "one.example.com");
      loader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(modern);
      loader.when(() -> XMLHeaderLoader.loadFromURL("header.default:LastValid", null)).thenReturn(explicitlyNamed);
      assertSame(modern, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      assertSame(explicitlyNamed, WebContainerLayoutCommand.retrieveHeader("header.default:LastValid", null));
    }
  }

  @Test
  void invalidReplacementRetainsTheLastValidatedHeader() {
    Header valid = new Header("header.default");
    try (var repository = mockStatic(WebContainerRepository.class);
        var loader = mockStatic(XMLHeaderLoader.class)) {
      WorkspaceContextManager.activate(1L, "one.example.com");
      loader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(valid);
      assertSame(valid, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      CacheManager.invalidateCurrentWorkspaceKey(CacheManager.OBJECT_CACHE, WebContainerLayoutCommand.headerCacheKey("header.default"),
          false);
      loader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(null);
      assertSame(valid, WebContainerLayoutCommand.retrieveHeader("header.default", null));
    }
  }

  @Test
  void newlyPublishedHeadersEvictSharedLayoutKeys() {
    WebContainer record = new WebContainer();
    record.setName("header.default");
    record.setContainerXml("<header name='header.default'><widget name='logo'/></header>");
    Insert insert = mock(Insert.class, RETURNS_SELF);
    when(insert.execute()).thenReturn(42L);
    try (var database = mockStatic(DB.class);
        var cache = mockStatic(CacheManager.class)) {
      database.when(DB::INSERT).thenReturn(insert);
      assertSame(record, WebContainerRepository.save(record));
      cache.verify(() -> CacheManager.invalidateCurrentWorkspaceKey(CacheManager.OBJECT_CACHE, CacheManager.WEBSITE_HEADER, true));
      cache.verify(
          () -> CacheManager.invalidateCurrentWorkspaceKey(CacheManager.OBJECT_CACHE, CacheManager.WEBSITE_PLAIN_HEADER, true));
    }
  }

  @Test
  void renamingPublishedHeadersEvictsBothNames() {
    WebContainer record = new WebContainer();
    record.setId(42L);
    record.setName("header.new");
    record.setContainerXml("<header name='header.new'><widget name='logo'/></header>");
    WebContainer previous = new WebContainer();
    previous.setName("header.old");
    Update update = mock(Update.class, RETURNS_SELF);
    when(update.execute()).thenReturn(true);
    Select select = mock(Select.class, RETURNS_SELF);
    when(select.returnRecord(ArgumentMatchers.<DB.ResultSetMapper<@Nullable Object>>any())).thenReturn(previous);
    try (var database = mockStatic(DB.class);
        var cache = mockStatic(CacheManager.class)) {
      database.when(() -> DB.UPDATE("web_containers")).thenReturn(update);
      database.when(() -> DB.SELECT("web_containers.*")).thenReturn(select);
      assertSame(record, WebContainerRepository.save(record));
      cache.verify(() -> CacheManager.invalidateCurrentWorkspaceKey(CacheManager.OBJECT_CACHE, "Header:header.new", true));
      cache.verify(() -> CacheManager.invalidateCurrentWorkspaceKey(CacheManager.OBJECT_CACHE, "Header:header.old", true));
    }
  }

  @Test
  void databaseHeadersTakePrecedenceOverBundledDefinitions() throws Exception {
    WebContainer record = new WebContainer();
    Header databaseHeader = new Header("header.default");
    try (var repository = mockStatic(WebContainerRepository.class);
        var loader = mockStatic(XMLHeaderLoader.class)) {
      WorkspaceContextManager.activate(1L, "one.example.com");
      repository.when(() -> WebContainerRepository.findByName("header.default")).thenReturn(record);
      loader.when(() -> XMLHeaderLoader.addFromXml(record)).thenReturn(databaseHeader);
      assertSame(databaseHeader, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      loader.verify(() -> XMLHeaderLoader.loadFromURL("header.default", null), never());
    }
  }

  @Test
  void invalidDatabaseHeadersFallBackToBundledDefinitions() throws Exception {
    WebContainer record = new WebContainer();
    Header bundled = new Header("header.default");
    try (var repository = mockStatic(WebContainerRepository.class);
        var loader = mockStatic(XMLHeaderLoader.class)) {
      WorkspaceContextManager.activate(1L, "one.example.com");
      repository.when(() -> WebContainerRepository.findByName("header.default")).thenReturn(record);
      loader.when(() -> XMLHeaderLoader.addFromXml(record)).thenThrow(new IllegalArgumentException("Invalid fixture"));
      loader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(bundled);
      assertSame(bundled, WebContainerLayoutCommand.retrieveHeader("header.default", null));
    }
  }

  @Test
  void evictionOfNamedHeadersDoesNotCrossWorkspaces() {
    Header first = new Header("header.default");
    Header second = new Header("header.default");
    Header refreshed = new Header("header.default");
    try (var repository = mockStatic(WebContainerRepository.class);
        var loader = mockStatic(XMLHeaderLoader.class)) {
      WorkspaceContextManager.activate(1L, "one.example.com");
      loader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(first);
      assertSame(first, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      WorkspaceContextManager.activate(2L, "two.example.com");
      loader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(second);
      assertSame(second, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      WorkspaceContextManager.activate(1L, "one.example.com");
      CacheManager.invalidateCurrentWorkspaceKey(CacheManager.OBJECT_CACHE, WebContainerLayoutCommand.headerCacheKey("header.default"),
          false);
      loader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(refreshed);
      assertSame(refreshed, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      WorkspaceContextManager.activate(2L, "two.example.com");
      assertSame(second, WebContainerLayoutCommand.retrieveHeader("header.default", null));
    }
  }

  @Test
  void invalidHeadersAreRejectedBeforePublication() {
    WebContainer record = new WebContainer();
    record.setName("header.default");
    record.setContainerXml("<header name='header.default' scrollBehavior='invalid'/>");
    try (var database = mockStatic(DB.class)) {
      assertNull(WebContainerRepository.save(record));
      database.verify(DB::INSERT, never());
    }
  }

  @Test
  void namedHeadersDoNotShareTheCheckoutCache() {
    Header plain = new Header("header.plain");
    Header modern = new Header("header.default");
    Header custom = new Header("header.custom");
    try (var containerRepository = mockStatic(WebContainerRepository.class);
        var headerLoader = mockStatic(XMLHeaderLoader.class)) {
      WorkspaceContextManager.activate(1L, "one.example.com");
      headerLoader.when(() -> XMLHeaderLoader.loadFromURL("header.plain", null)).thenReturn(plain);
      headerLoader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(modern);
      headerLoader.when(() -> XMLHeaderLoader.loadFromURL("header.custom", null)).thenReturn(custom);
      assertSame(plain, WebContainerLayoutCommand.retrieveHeader("header.plain", null));
      assertSame(modern, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      assertSame(custom, WebContainerLayoutCommand.retrieveHeader("header.custom", null));
      assertSame(plain, WebContainerLayoutCommand.retrieveHeader("header.plain", null));
    }
  }

  @Test
  void headersAndFootersAreCachedPerWorkspace() {
    Header firstHeader = new Header("first");
    Header secondHeader = new Header("second");
    Footer firstFooter = new Footer("first");
    Footer secondFooter = new Footer("second");

    try (var containerRepository = mockStatic(WebContainerRepository.class);
        var headerLoader = mockStatic(XMLHeaderLoader.class);
        var footerLoader = mockStatic(XMLFooterLoader.class)) {
      WorkspaceContextManager.activate(1L, "one.example.com");
      headerLoader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(firstHeader);
      footerLoader.when(() -> XMLFooterLoader.loadFromURL("footer.default", null)).thenReturn(firstFooter);
      assertSame(firstHeader, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      assertSame(firstFooter, WebContainerLayoutCommand.retrieveFooter("footer.default", null));

      WorkspaceContextManager.activate(2L, "two.example.com");
      headerLoader.when(() -> XMLHeaderLoader.loadFromURL("header.default", null)).thenReturn(secondHeader);
      footerLoader.when(() -> XMLFooterLoader.loadFromURL("footer.default", null)).thenReturn(secondFooter);
      assertSame(secondHeader, WebContainerLayoutCommand.retrieveHeader("header.default", null));
      assertSame(secondFooter, WebContainerLayoutCommand.retrieveFooter("footer.default", null));
    }
  }
}