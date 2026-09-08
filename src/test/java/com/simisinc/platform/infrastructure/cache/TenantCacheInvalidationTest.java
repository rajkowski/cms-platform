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
package com.simisinc.platform.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

class TenantCacheInvalidationTest {

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
  void tenantInvalidationDoesNotRemoveAnotherWorkspaceEntry() {
    Object first = new Object();
    Object second = new Object();
    CacheManager.putTenantValue(CacheManager.OBJECT_CACHE, "1", "header", first);
    CacheManager.putTenantValue(CacheManager.OBJECT_CACHE, "2", "header", second);

    CacheManager.invalidateTenantKey(CacheManager.OBJECT_CACHE, "1", "header", false);

    assertNull(CacheManager.getTenantValue(CacheManager.OBJECT_CACHE, "1", "header"));
    assertSame(second, CacheManager.getTenantValue(CacheManager.OBJECT_CACHE, "2", "header"));
  }

  @Test
  void currentWorkspaceValueDoesNotLeakToAnotherWorkspaceOrDefaultSite() {
    Object firstWorkspaceValue = new Object();
    Object secondWorkspaceValue = new Object();
    Object defaultSiteValue = new Object();

    WorkspaceContextManager.activate(1L, "one.example.com");
    CacheManager.putCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, "content", firstWorkspaceValue);

    WorkspaceContextManager.activate(2L, "two.example.com");
    CacheManager.putCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, "content", secondWorkspaceValue);

    WorkspaceContextManager.clear();
    CacheManager.putCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, "content", defaultSiteValue);

    WorkspaceContextManager.activate(1L, "one.example.com");
    assertSame(firstWorkspaceValue, CacheManager.getCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, "content"));

    WorkspaceContextManager.activate(2L, "two.example.com");
    assertSame(secondWorkspaceValue, CacheManager.getCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, "content"));

    WorkspaceContextManager.clear();
    assertSame(defaultSiteValue, CacheManager.getCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, "content"));
  }
}
