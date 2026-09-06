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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
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