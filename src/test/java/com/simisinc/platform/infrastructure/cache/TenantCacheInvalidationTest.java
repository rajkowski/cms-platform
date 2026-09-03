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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantCacheInvalidationTest {

  @BeforeEach
  void setUp() {
    CacheManager.startup();
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
}
