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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Methods for managing threads and resources when working with PostgreSQL LISTEN NOTIFY
 *
 * @author matt rajkowski
 * @created 4/2/25 8:00 AM
 */
public class MessagingManager {

  private static Log LOG = LogFactory.getLog(MessagingManager.class);

  public static final String CHANNEL = "cms_platform";

  private static Cache<Integer, Boolean> sendingPIDCache = Caffeine.newBuilder()
      .maximumSize(10)
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .build();

  private static Thread listenerThread = null;

  /* Responsible for maintaining a PostgreSQL LISTEN connection for receiving pg notifications */
  public static void startup() {
    // Create a thread to handle notifications
    listenerThread = new Thread(() -> {
      LOG.info("Listener started...");
      while (!Thread.currentThread().isInterrupted()) {
        MessagingHandler.pollNotifications();
      }
    }, "message-queue-listener");

    listenerThread.setDaemon(true);
    listenerThread.start();
  }

  public static void shutdown() {
    LOG.info("Shutdown called");
    if (listenerThread != null && listenerThread.isAlive()) {
      listenerThread.interrupt();
      try {
        listenerThread.join(3000);
      } catch (InterruptedException e) {
        LOG.warn("Listener thread interrupted while waiting for termination", e);
      }
      LOG.info("Listener thread has been terminated.");
    }
  }

  public static Cache<Integer, Boolean> getSendingPIDCache() {
    return sendingPIDCache;
  }

  public static boolean hasStarted() {
    return listenerThread != null;
  }
}
