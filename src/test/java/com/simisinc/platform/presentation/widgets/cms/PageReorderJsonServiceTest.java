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
package com.simisinc.platform.presentation.widgets.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.RepositoryDatabaseTestSupport;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHierarchyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.zeroio.platform.infrastructure.permission.Permission;

class PageReorderJsonServiceTest {

  @BeforeEach
  void setUp() throws SQLException {
    RepositoryDatabaseTestSupport.configureInMemoryH2Database();
    RepositoryDatabaseTestSupport.executeSql("CREATE TABLE web_page_hierarchy (web_page_id BIGINT PRIMARY KEY, parent_page_id BIGINT, sort_order INTEGER, depth INTEGER, path VARCHAR(255), created TIMESTAMP, modified TIMESTAMP)");
    RepositoryDatabaseTestSupport.executeSql("INSERT INTO web_page_hierarchy VALUES (1, NULL, 1, 0, '/1/', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), (2, 1, 1, 1, '/1/2/', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), (3, 2, 1, 2, '/1/2/3/', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
  }

  @AfterEach
  void tearDown() {
    RepositoryDatabaseTestSupport.shutdown();
  }

  @Test
  void movingASubtreeUpdatesDescendantPathsAndDepths() {
    JsonServiceContext context = mock(JsonServiceContext.class);
    when(context.getParameterAsLong("pageId", -1)).thenReturn(2L);
    when(context.getParameterAsLong("targetPageId", -1)).thenReturn(-1L);
    WebPage page = new WebPage();
    page.setId(2);
    try (var permissions = mockStatic(Permission.class);
        var pages = mockStatic(WebPageRepository.class)) {
      permissions.when(() -> Permission.check("cms.page.reorder", null)).thenReturn(true);
      pages.when(() -> WebPageRepository.findById(2)).thenReturn(page);
      new PageReorderJsonService().post(context);
      verify(context).writeOk(org.mockito.ArgumentMatchers.contains("Page reordered successfully"), org.mockito.ArgumentMatchers.isNull());
    }
    assertEquals("/2/", WebPageHierarchyRepository.findByPageId(2).getPath());
    assertEquals("/2/3/", WebPageHierarchyRepository.findByPageId(3).getPath());
    assertEquals(1, WebPageHierarchyRepository.findByPageId(3).getDepth());
    assertEquals("/1/", WebPageHierarchyRepository.findByPageId(1).getPath());
  }

  @Test
  void cannotMoveBesideADescendant() {
    JsonServiceContext context = mock(JsonServiceContext.class);
    when(context.getParameterAsLong("pageId", -1)).thenReturn(2L);
    when(context.getParameterAsLong("targetPageId", -1)).thenReturn(3L);
    when(context.getParameter("position")).thenReturn("before");
    WebPage page = new WebPage();
    page.setId(2);
    try (var permissions = mockStatic(Permission.class);
        var pages = mockStatic(WebPageRepository.class)) {
      permissions.when(() -> Permission.check("cms.page.reorder", null)).thenReturn(true);
      pages.when(() -> WebPageRepository.findById(2)).thenReturn(page);
      new PageReorderJsonService().post(context);
      verify(context).writeError("Cannot move a page beneath its own descendant");
    }
    assertEquals("/1/2/", WebPageHierarchyRepository.findByPageId(2).getPath());
    assertEquals("/1/2/3/", WebPageHierarchyRepository.findByPageId(3).getPath());
  }
}