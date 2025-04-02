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
package com.simisinc.platform.infrastructure.distributedmessaging;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.PGNotification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.cache.CacheManager;

/**
 * Methods for handling a PostgreSQL notification
 *
 * @author matt rajkowski
 * @created 4/2/25 8:00 AM
 */
public class MessagingNotification {

  private static Log LOG = LogFactory.getLog(MessagingNotification.class);

  /* Processes a PostgreSQL notification */
  public static void handleNotification(PGNotification notification) throws Exception {
    // Check if this backend pid sent the notification
    if (MessagingManager.getSendingPIDCache().getIfPresent(notification.getPID()) != null) {
      LOG.trace("Received own message, skipping");
      return;
    }
    LOG.debug("Received a notification from backend pid: " + notification.getPID());

    // Check the payload
    String channel = notification.getName();
    String jsonPayload = notification.getParameter();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Notification received on channel: " + channel);
    }
    if (StringUtils.isEmpty(jsonPayload)) {
      LOG.debug("No payload, skipping");
      return;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Notification json payload: " + jsonPayload);
    }

    // Process the payload
    JsonNode json = null;
    try {
      json = JsonCommand.fromString(jsonPayload);
    } catch (JsonProcessingException jpe) {
      LOG.warn("Could not parse payload as JSON", jpe);
      return;
    }

    // Invalidate the specified cache key
    String cacheName = json.get("cache").asText();
    String key = json.get("key").asText();
    String type = json.get("type").asText();
    LOG.debug("Invalidating " + cacheName + " for " + key + " of type " + type);
    if ("java.lang.Long".equals(type)) {
      CacheManager.invalidateKey(cacheName, Long.parseLong(key), false);
    } else {
      CacheManager.invalidateKey(cacheName, key, false);
    }
  }
}
