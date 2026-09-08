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
package com.simisinc.platform.infrastructure.distributedmessaging;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.PGNotification;
import com.simisinc.platform.infrastructure.cache.CacheManager;

class MessagingNotificationTest {

  @BeforeEach
  void setUp() {
    CacheManager.startup();
  }

  @Test
  void notificationExpiresOnlyTheNamedWorkspaceCacheEntry() throws Exception {
    Object first = new Object();
    Object second = new Object();
    CacheManager.putTenantValue(CacheManager.OBJECT_CACHE, "1", "header", first);
    CacheManager.putTenantValue(CacheManager.OBJECT_CACHE, "2", "header", second);

    MessagingNotification.handleNotification(notification(MessagingCommand.createPayload(CacheManager.OBJECT_CACHE, "1", "header")));

    assertNull(CacheManager.getTenantValue(CacheManager.OBJECT_CACHE, "1", "header"));
    assertSame(second, CacheManager.getTenantValue(CacheManager.OBJECT_CACHE, "2", "header"));
  }

  @Test
  void notificationWithoutWorkspaceIdentityIsIgnored() throws Exception {
    Object value = new Object();
    CacheManager.putTenantValue(CacheManager.OBJECT_CACHE, "1", "header", value);

    MessagingNotification
        .handleNotification(notification("{\"cache\":\"ObjectCache\",\"key\":\"header\",\"type\":\"java.lang.String\"}"));

    assertSame(value, CacheManager.getTenantValue(CacheManager.OBJECT_CACHE, "1", "header"));
  }

  private static PGNotification notification(String payload) {
    PGNotification notification = mock(PGNotification.class);
    when(notification.getPID()).thenReturn(-1);
    when(notification.getName()).thenReturn(MessagingManager.CHANNEL);
    when(notification.getParameter()).thenReturn(payload);
    return notification;
  }
}
