/*
 * Copyright 2026 Matt Rajkowski
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
package com.zeroio.platform.infrastructure.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.github.rajkowski.database.DB;
import com.simisinc.platform.infrastructure.database.ConnectionPool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

class TenantDataSourceRegistrarTest {

  @AfterEach
  void tearDown() {
    ConnectionPool.unregisterTenantDataSource("1");
    ConnectionPool.unregisterTenantDataSource("2");
  }

  @Test
  void storePersistsAndReplacesWorkspaceDatasourceConfiguration() throws Exception {
    HikariDataSource dataSource = createDataSource();
    DB.setDataSource(dataSource);
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE workspaces (workspace_id BIGINT PRIMARY KEY)");
      statement.execute("CREATE TABLE workspace_data_sources (workspace_id BIGINT PRIMARY KEY, jdbc_url VARCHAR(2048), username VARCHAR(255), password VARCHAR(2048), driver_class_name VARCHAR(255))");
      statement.execute("INSERT INTO workspaces VALUES (1)");
    }

    SitePropertyTenantDataSourceConfigurationStore store = new SitePropertyTenantDataSourceConfigurationStore();
    TenantDataSourceConfiguration configuration = configuration(1, "jdbc:h2:mem:first");
    assertNotNull(store.save(configuration));

    configuration.setJdbcUrl("jdbc:h2:mem:replacement");
    assertNotNull(store.save(configuration));

    List<TenantDataSourceConfiguration> configurations = store.findAll();
    assertEquals(1, configurations.size());
    assertEquals("jdbc:h2:mem:replacement", configurations.get(0).getJdbcUrl());
  }

  @Test
  void startupRegistersValidConfigurationsAndSkipsInvalidConfigurations() {
    List<TenantDataSourceConfiguration> configurations = new ArrayList<>();
    configurations.add(configuration(1, "jdbc:h2:mem:workspace_valid;DB_CLOSE_DELAY=-1"));
    configurations.add(configuration(2, ""));
    TenantDataSourceRegistrar registrar = new TenantDataSourceRegistrar(new InMemoryStore(configurations));

    registrar.registerAllAtStartup();

    assertNotNull(ConnectionPool.getTenantRegistry().getDataSource("1"));
    assertFalse(ConnectionPool.getTenantRegistry().contains("2"));
    registrar.shutdown();
  }

  @Test
  void replacementRetiresPreviousDatasourceOnlyAfterActiveWorkCompletes() throws Exception {
    TenantDataSourceRegistrar registrar = new TenantDataSourceRegistrar(new InMemoryStore(new ArrayList<>()));
    registrar.saveAndRegister(configuration(1, "jdbc:h2:mem:workspace_first;DB_CLOSE_DELAY=-1"));
    HikariDataSource previous = (HikariDataSource) ConnectionPool.getTenantRegistry().getDataSource("1");

    try (Connection connection = previous.getConnection()) {
      registrar.saveAndRegister(configuration(1, "jdbc:h2:mem:workspace_replacement;DB_CLOSE_DELAY=-1"));
      registrar.retireIdleDataSources();
      assertFalse(previous.isClosed());
    }

    registrar.retireIdleDataSources();
    assertTrue(previous.isClosed());
    registrar.shutdown();
  }

  private static HikariDataSource createDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:h2:mem:tenant_data_source_configuration_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    config.setDriverClassName("org.h2.Driver");
    config.setUsername("sa");
    config.setPassword("");
    return new HikariDataSource(config);
  }

  private static TenantDataSourceConfiguration configuration(long workspaceId, String jdbcUrl) {
    TenantDataSourceConfiguration configuration = new TenantDataSourceConfiguration();
    configuration.setWorkspaceId(workspaceId);
    configuration.setJdbcUrl(jdbcUrl);
    configuration.setUsername("sa");
    configuration.setPassword("");
    configuration.setDriverClassName("org.h2.Driver");
    return configuration;
  }

  private static class InMemoryStore implements TenantDataSourceConfigurationStore {

    private final List<TenantDataSourceConfiguration> configurations;

    InMemoryStore(List<TenantDataSourceConfiguration> configurations) {
      this.configurations = configurations;
    }

    @Override
    public List<TenantDataSourceConfiguration> findAll() {
      return configurations;
    }

    @Override
    public TenantDataSourceConfiguration save(TenantDataSourceConfiguration configuration) {
      return configuration;
    }
  }
}