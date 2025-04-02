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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import com.simisinc.platform.infrastructure.database.ConnectionPool;

/**
 * Methods for working with PostgreSQL LISTEN NOTIFY
 *
 * @author matt rajkowski
 * @created 4/2/25 8:00 AM
 */
public class MessagingHandler {

  private static Log LOG = LogFactory.getLog(MessagingHandler.class);

  public static void pollNotifications() {
    try (Connection connection = ConnectionPool.getDistributedMessagingDataSource().getConnection()) {
      // Unwrap to access additional PostgreSQL details
      PGConnection pgConnection = connection.unwrap(PGConnection.class);

      // Attach to the database LISTEN channel
      Statement stmt = connection.createStatement();
      stmt.execute("LISTEN " + MessagingManager.CHANNEL);
      LOG.info("Started PostgreSQL LISTEN on channel: " + MessagingManager.CHANNEL);

      // Wait for notifications
      while (true) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Checking for notifications... pid cache size: " + MessagingManager.getSendingPIDCache().estimatedSize());
        }
        PGNotification[] notifications = pgConnection.getNotifications(1000);
        if (notifications != null) {
          for (PGNotification notification : notifications) {
            try {
              MessagingNotification.handleNotification(notification);
            } catch (Exception e) {
              LOG.error("Notification error, skipping", e);
            }
          }
        }
      }
    } catch (SQLException e) {
      LOG.error("Error in notification listener: " + e.getMessage());
      LOG.info("Attempting to reconnect to the database...");
      try {
        Thread.sleep(5000); // Wait before retrying
      } catch (InterruptedException ie) {
        LOG.info("Listener thread interrupted during sleep, exiting");
        Thread.currentThread().interrupt();
      }
    }
  }
}
