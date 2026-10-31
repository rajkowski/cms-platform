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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.rajkowski.database.DB;
import com.simisinc.platform.infrastructure.database.ConnectionPool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zeroio.platform.domain.model.tenant.TenantDataSourceDatabase;
import com.zeroio.platform.domain.model.tenant.TenantDataSourcePool;
import com.zeroio.platform.infrastructure.persistence.tenant.TenantDataSourceDatabaseRepository;
import com.zeroio.platform.infrastructure.persistence.tenant.TenantDataSourcePoolRepository;
import com.zeroio.platform.infrastructure.persistence.tenant.WorkspaceRepository;

class TenantDataSourceRegistrarTest {

  private String previousSecretKey;

  @BeforeEach
  void setUp() {
    previousSecretKey = System.getProperty("cms.secret.key");
    System.setProperty("cms.secret.key", Base64.getEncoder().encodeToString(new byte[32]));
  }

  @AfterEach
  void tearDown() {
    ConnectionPool.unregisterTenantDataSource("1");
    ConnectionPool.unregisterTenantDataSource("2");
  if (previousSecretKey == null) {
      System.clearProperty("cms.secret.key");
    } else {
      System.setProperty("cms.secret.key", previousSecretKey);
    }
  }

  @Test
  void repositoriesPersistAndReplaceWorkspaceDatasourceConfiguration() throws Exception {
    try (HikariDataSource dataSource = createDataSource()) {
      DB.setDataSource(dataSource);
      try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
        statement.execute("CREATE TABLE workspaces (workspace_id BIGINT PRIMARY KEY)");
        statement.execute(
            "CREATE TABLE data_source_pools (pool_id BIGSERIAL PRIMARY KEY, pool_unique_id VARCHAR(255) UNIQUE NOT NULL, ds_class_name VARCHAR(255), driver_class_name VARCHAR(255), server_name VARCHAR(255) NOT NULL, username VARCHAR(255) NOT NULL, password VARCHAR(2048), port_number INT DEFAULT 5432, ssl BOOLEAN DEFAULT FALSE, auth_method VARCHAR(50), azure_tenant_id VARCHAR(255), azure_client_id VARCHAR(255), azure_client_secret VARCHAR(2048), max_connections INT DEFAULT 8)");
        statement.execute(
            "CREATE TABLE pool_databases (pool_database_id BIGSERIAL PRIMARY KEY, pool_id BIGINT REFERENCES data_source_pools(pool_id), database_name VARCHAR(255), active BOOLEAN, available BOOLEAN, created TIMESTAMP DEFAULT CURRENT_TIMESTAMP, modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        statement.execute(
            "CREATE TABLE workspace_data_sources (workspace_id BIGINT PRIMARY KEY REFERENCES workspaces(workspace_id), pool_database_id BIGINT REFERENCES pool_databases(pool_database_id), created TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        statement.execute("INSERT INTO workspaces VALUES (1)");
      }

      TenantDataSourceConfigurationStore store = new TenantDataSourceConfigurationStore();
      TenantDataSourcePool pool = new TenantDataSourcePool();
      pool.setUniqueId("shared-server");
      pool.setDriverClassName("org.postgresql.Driver");
      pool.setServerName("db.example.com");
      pool.setPortNumber(5433);
      pool.setSsl(true);
      pool.setUsername("sa");
      pool.setMaxConnections(4);
      pool.setPassword("test-password");
      pool.setAuthMethod("azure-sql-spn");
      pool.setAzureTenantId("test-tenant");
      pool.setAzureClientId("test-client");
      pool.setAzureClientSecret("test-secret");
      assertNotNull(TenantDataSourcePoolRepository.save(pool));

      TenantDataSourceDatabase first = database(pool.getId(), "first");
      assertNotNull(TenantDataSourceDatabaseRepository.save(first));
      assertTrue(WorkspaceRepository.saveDataSourceMapping(1, first.getId()));

      TenantDataSourceDatabase replacement = database(pool.getId(), "replacement");
      assertNotNull(TenantDataSourceDatabaseRepository.save(replacement));
      assertTrue(WorkspaceRepository.saveDataSourceMapping(1, replacement.getId()));

      List<TenantDataSourceConfiguration> configurations = store.findAll();
      assertEquals(1, configurations.size());
      assertEquals("replacement", configurations.get(0).getDatabaseName());
      assertEquals("jdbc:postgresql://db.example.com:5433/replacement?sslmode=require"
          + "&authenticationPluginClassName=com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin",
          configurations.get(0).getJdbcUrl());
      assertEquals("shared-server", configurations.get(0).getPoolGroup());
      assertEquals("org.postgresql.Driver", configurations.get(0).getDriverClassName());
      assertEquals(4, configurations.get(0).getMaxConnections());
      assertEquals("test-password", configurations.get(0).getPassword());
      assertEquals("test-secret", configurations.get(0).getAzureClientSecret());

      try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO workspaces VALUES (2)");
      }
      TenantDataSourceDatabase second = database(pool.getId(), "second");
      assertNotNull(TenantDataSourceDatabaseRepository.save(second));
      assertTrue(WorkspaceRepository.saveDataSourceMapping(2, second.getId()));
      configurations = store.findAll();
      assertEquals(2, configurations.size());
      assertEquals("replacement", configurations.get(0).getDatabaseName());
      assertEquals("second", configurations.get(1).getDatabaseName());
      assertEquals(1, TenantDataSourcePoolRepository.findAll().size());

      assertThrows(IllegalStateException.class, () -> DB.withTransaction(dataSource, () -> {
        TenantDataSourcePool rolledBackPool = new TenantDataSourcePool();
        rolledBackPool.setUniqueId("rolled-back-pool");
        rolledBackPool.setServerName("db.example.com");
        rolledBackPool.setUsername("sa");
        assertNotNull(TenantDataSourcePoolRepository.save(rolledBackPool));
        TenantDataSourceDatabase rolledBackDatabase = database(rolledBackPool.getId(), "rolled_back");
        assertNotNull(TenantDataSourceDatabaseRepository.save(rolledBackDatabase));
        assertTrue(WorkspaceRepository.saveDataSourceMapping(1, rolledBackDatabase.getId()));
        throw new IllegalStateException("Roll back workspace creation");
      }));
      assertEquals(1, TenantDataSourcePoolRepository.findAll().size());
      assertEquals(3, TenantDataSourceDatabaseRepository.findAll().size());
      assertEquals("shared-server", store.findAll().get(0).getPoolGroup());
      assertEquals("replacement", store.findAll().get(0).getDatabaseName());

      pool.setDsClassName("org.postgresql.ds.PGSimpleDataSource");
      pool.setDriverClassName(null);
      assertNotNull(TenantDataSourcePoolRepository.save(pool));
      assertEquals("org.postgresql.ds.PGSimpleDataSource", store.findAll().get(0).getDataSourceClassName());
      second.setActive(false);
      assertNotNull(TenantDataSourceDatabaseRepository.save(second));
      assertEquals(1, store.findAll().size());
    }
  }

  @Test
  void startupRegistersValidConfigurationsAndSkipsInvalidConfigurations() {
    List<TenantDataSourceConfiguration> configurations = new ArrayList<>();
    TenantDataSourceConfiguration validConfiguration = configuration(1,
        "jdbc:h2:mem:workspace_valid;DB_CLOSE_DELAY=-1");
    validConfiguration.setPoolGroup("shared-server");
    validConfiguration.setMaxConnections(3);
    configurations.add(validConfiguration);
    configurations.add(configuration(2, ""));
    TenantDataSourceRegistrar registrar = new TenantDataSourceRegistrar(new InMemoryStore(configurations));

    registrar.registerAllAtStartup();

    assertNotNull(ConnectionPool.getTenantRegistry().getDataSource("1"));
    assertEquals(3, ((HikariDataSource) ConnectionPool.getTenantRegistry().getDataSource("1")).getMaximumPoolSize());
    assertFalse(ConnectionPool.getTenantRegistry().contains("2"));
    registrar.shutdown();
  }

  private static TenantDataSourceDatabase database(long poolId, String databaseName) {
    TenantDataSourceDatabase database = new TenantDataSourceDatabase();
    database.setPoolId(poolId);
    database.setDatabaseName(databaseName);
    database.setActive(true);
    return database;
  }

  private static HikariDataSource createDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(
        "jdbc:h2:mem:tenant_data_source_configuration_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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

  private static class InMemoryStore implements TenantDataSourceConfigurationStoreInterface {

    private final List<TenantDataSourceConfiguration> configurations;

    InMemoryStore(List<TenantDataSourceConfiguration> configurations) {
      this.configurations = configurations;
    }

    @Override
    public List<TenantDataSourceConfiguration> findAll() {
      return configurations;
    }

  }
}