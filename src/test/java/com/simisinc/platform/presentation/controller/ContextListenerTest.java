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
package com.simisinc.platform.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceRepository;

class ContextListenerTest {

  private final Content defaultContent = content("default");
  private final Content firstWorkspaceContent = content("content");
  private final Content secondWorkspaceContent = content("content");

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
  void preloadsContentSeparatelyForDefaultSiteAndEveryWorkspace() {
    Workspace firstWorkspace = workspace(1L, "one.example.com");
    Workspace secondWorkspace = workspace(2L, "two.example.com");

    try (var contentRepository = mockStatic(ContentRepository.class);
        var workspaceRepository = mockStatic(WorkspaceRepository.class)) {
      workspaceRepository.when(WorkspaceRepository::findAllActive).thenReturn(List.of(firstWorkspace, secondWorkspace));
      contentRepository.when(ContentRepository::findAll).thenAnswer(invocation -> List.of(contentForCurrentScope()));
      contentRepository.when(() -> ContentRepository.findByUniqueId("content")).thenAnswer(invocation -> contentForCurrentScope());
      contentRepository.when(() -> ContentRepository.findByUniqueId("default")).thenReturn(defaultContent);

      ContextListener.preloadContentForActiveWorkspaces();

      assertSame(defaultContent, CacheManager.getCurrentWorkspaceLoadingValue(CacheManager.CONTENT_UNIQUE_ID_CACHE, "default"));
      WorkspaceContextManager.activate(1L, "one.example.com");
      assertSame(firstWorkspaceContent, CacheManager.getCurrentWorkspaceLoadingValue(CacheManager.CONTENT_UNIQUE_ID_CACHE, "content"));
      WorkspaceContextManager.activate(2L, "two.example.com");
      assertSame(secondWorkspaceContent, CacheManager.getCurrentWorkspaceLoadingValue(CacheManager.CONTENT_UNIQUE_ID_CACHE, "content"));
    }
  }

  private Content contentForCurrentScope() {
    if ("1".equals(DB.getTenantId())) {
      return firstWorkspaceContent;
    }
    if ("2".equals(DB.getTenantId())) {
      return secondWorkspaceContent;
    }
    return defaultContent;
  }

  private static Content content(String uniqueId) {
    Content content = new Content();
    content.setUniqueId(uniqueId);
    return content;
  }

  private static Workspace workspace(long id, String domain) {
    Workspace workspace = new Workspace();
    workspace.setId(id);
    workspace.setCanonicalDomain(domain);
    return workspace;
  }
}