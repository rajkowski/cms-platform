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
package com.simisinc.platform.infrastructure.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConnectionPoolTest {

  @AfterEach
  void shutdownPool() {
    ConnectionPool.shutdown();
  }

  @Test
  void applicationPoolFailsWithinConfiguredTimeoutAndRecoversAfterRelease() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("jdbcUrl", "jdbc:h2:mem:connection_pool_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
    properties.setProperty("driverClassName", "org.h2.Driver");
    properties.setProperty("username", "sa");
    properties.setProperty("password", "");
    properties.setProperty("application.maximumPoolSize", "1");
    properties.setProperty("application.minimumIdle", "0");
    properties.setProperty("application.connectionTimeout", "250");

    ConnectionPool.init(properties);
    try (Connection heldConnection = ConnectionPool.getApplicationDataSource().getConnection()) {
      assertThrows(SQLException.class, () -> ConnectionPool.getApplicationDataSource().getConnection());
    }
    assertDoesNotThrow(() -> {
      try (Connection recoveredConnection = ConnectionPool.getApplicationDataSource().getConnection()) {
        // Acquiring a released connection is the recovery condition.
      }
    });
  }
}
