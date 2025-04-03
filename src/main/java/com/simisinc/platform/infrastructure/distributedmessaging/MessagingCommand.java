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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.PGConnection;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.database.ConnectionPool;

/**
 * Methods for working with PostgreSQL LISTEN NOTIFY
 *
 * @author matt rajkowski
 * @created 4/2/25 8:00 AM
 */
public class MessagingCommand {

  private static Log LOG = LogFactory.getLog(MessagingCommand.class);

  /* Generate a PostgreSQL notification */
  public static void sendNotification(String cacheName, Object key) {
    // Check if messages are being used on this webapp instance
    if (!MessagingManager.hasStarted()) {
      return;
    }

    // Prepare the notification
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("cache", cacheName);
    params.put("key", key);
    params.put("type", key.getClass().getName());
    String message = JsonCommand.createJsonNode(params).toString();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Sending notification with payload: " + message);
    }

    // Send the notification to the channel
    try (Connection connection = ConnectionPool.getDistributedMessagingDataSource().getConnection()) {
      PGConnection pgConnection = connection.unwrap(PGConnection.class);
      MessagingManager.getSendingPIDCache().put(pgConnection.getBackendPID(), true);
      try (PreparedStatement pst = connection.prepareStatement("SELECT pg_notify(?, ?)")) {
        pst.setString(1, MessagingManager.CHANNEL);
        pst.setString(2, message);
        pst.execute();
      }
      LOG.debug("Notification sent with backend PID: " + pgConnection.getBackendPID());
    } catch (SQLException e) {
      LOG.error("Error in notification listener: " + e.getMessage());
      throw new MessagingException("Failed to send notification", e);
    }
  }

}
