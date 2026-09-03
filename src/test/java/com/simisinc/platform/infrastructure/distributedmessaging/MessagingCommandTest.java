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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.cache.CacheManager;

class MessagingCommandTest {

  @Test
  void payloadIncludesWorkspaceIdentityAndCacheKey() throws Exception {
    String payload = MessagingCommand.createPayload(CacheManager.OBJECT_CACHE, "42", "header");

    assertEquals("42", JsonCommand.fromString(payload).get("workspaceId").asText());
    assertEquals(CacheManager.OBJECT_CACHE, JsonCommand.fromString(payload).get("cache").asText());
    assertEquals("header", JsonCommand.fromString(payload).get("key").asText());
  }
}
