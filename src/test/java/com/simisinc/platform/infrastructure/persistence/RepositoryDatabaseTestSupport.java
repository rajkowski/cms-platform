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
package com.simisinc.platform.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import com.github.rajkowski.database.DB;
import com.simisinc.platform.infrastructure.database.ConnectionPool;

public final class RepositoryDatabaseTestSupport {

  private RepositoryDatabaseTestSupport() {
  }

  public static void configureInMemoryH2Database() {
    Properties properties = new Properties();
    properties.setProperty("jdbcUrl",
        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
    properties.setProperty("driverClassName", "org.h2.Driver");
    properties.setProperty("username", "sa");
    properties.setProperty("password", "");
    properties.setProperty("maximumPoolSize", "5");
    properties.setProperty("minimumIdle", "1");
    ConnectionPool.init(properties);
    DB.setDataSource(ConnectionPool.getApplicationDataSource());
    DB.clearTenantDataSource();
    DB.clearThreadLocalConnection();
  }

  public static void executeSql(String sql) throws SQLException {
    try (Connection connection = ConnectionPool.getApplicationDataSource().getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  public static void shutdown() {
    ConnectionPool.shutdown();
  }
}
