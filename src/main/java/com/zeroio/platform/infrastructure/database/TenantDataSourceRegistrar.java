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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.infrastructure.database.ConnectionPool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Builds and registers live workspace datasource pools from persisted
 * configuration.
 */
public class TenantDataSourceRegistrar {

  private static final Log LOG = LogFactory.getLog(TenantDataSourceRegistrar.class);
  private static final String AZURE_SPN_AUTH_METHOD = "azure-sql-spn";
  private static final String AZURE_AUTHENTICATION_PLUGIN = "com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin";

  private final TenantDataSourceConfigurationStoreInterface store;
  private final Map<Long, HikariDataSource> activeDataSources = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<HikariDataSource> retiredDataSources = new ConcurrentLinkedQueue<>();
  private final int maximumTenantConnections;
  private final ScheduledExecutorService retirementExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
    Thread thread = new Thread(runnable, "tenant-datasource-retirement");
    thread.setDaemon(true);
    return thread;
  });

  public TenantDataSourceRegistrar(TenantDataSourceConfigurationStoreInterface store) {
    this(store, 8);
  }

  public TenantDataSourceRegistrar(TenantDataSourceConfigurationStoreInterface store, int maximumTenantConnections) {
    if (store == null) {
      throw new IllegalArgumentException("TenantDataSourceConfigurationStore cannot be null");
    }
    if (maximumTenantConnections < 1) {
      throw new IllegalArgumentException("Maximum tenant connections must be greater than zero");
    }
    this.store = store;
    this.maximumTenantConnections = maximumTenantConnections;
    retirementExecutor.scheduleWithFixedDelay(this::retireIdleDataSources, 100, 100, TimeUnit.MILLISECONDS);
  }

  /**
   * Registers all tenant data sources at startup.
   * 
   * This method retrieves all tenant data source configurations from the store
   * and registers them. Any configurations that are null or cause errors during
   * registration are logged and ignored.
   */
  public void registerAllAtStartup() {
    for (TenantDataSourceConfiguration configuration : store.findAll()) {
      if (configuration == null) {
        LOG.error("Ignoring an empty workspace datasource configuration");
        continue;
      }
      try {
        register(configuration);
      } catch (Exception e) {
        LOG.error("Unable to register datasource for workspace " + configuration.getWorkspaceId(), e);
      }
    }
    retireIdleDataSources();
  }

  /**
   * Retires idle data sources that have no active connections.
   * 
   * This method iterates over the retired data sources and closes any that have
   * zero active connections.
   */
  public void retireIdleDataSources() {
    for (HikariDataSource dataSource : retiredDataSources) {
      if (dataSource.getHikariPoolMXBean().getActiveConnections() == 0 && retiredDataSources.remove(dataSource)) {
        dataSource.close();
      }
    }
  }

  /**
   * Shuts down all active and retired data sources and clears the internal
   * collections.
   * Also shuts down the retirement executor.
   */
  public void shutdown() {
    for (HikariDataSource dataSource : activeDataSources.values()) {
      dataSource.close();
    }
    for (HikariDataSource dataSource : retiredDataSources) {
      dataSource.close();
    }
    activeDataSources.clear();
    retiredDataSources.clear();
    retirementExecutor.shutdownNow();
  }

  /**
   * Register a tenant data source based on the provided configuration.
   * 
   * @param configuration
   */
  public void register(TenantDataSourceConfiguration configuration) {
    HikariDataSource replacement = createDataSourceForPool(configuration);
    HikariDataSource previous = activeDataSources.put(configuration.getWorkspaceId(), replacement);
    ConnectionPool.registerTenantDataSource(String.valueOf(configuration.getWorkspaceId()), replacement,
        configuration.getPoolGroup());
    if (configuration.getPoolGroup() != null && !configuration.getPoolGroup().isBlank()) {
      ConnectionPool.setTenantMaximumConnections(configuration.getPoolGroup(),
          configuration.getMaxConnections() > 0 ? configuration.getMaxConnections() : maximumTenantConnections);
    }
    if (previous != null) {
      retiredDataSources.add(previous);
    }
  }

  /**
   * Creates a HikariDataSource object for the given tenant data source
   * configuration.
   * 
   * @param configuration the tenant data source configuration
   * @return a configured HikariDataSource object
   */
  private HikariDataSource createDataSourceForPool(TenantDataSourceConfiguration configuration) {
    if (configuration == null || configuration.getWorkspaceId() < 1) {
      throw new IllegalArgumentException("Invalid workspace datasource configuration");
    }
    HikariConfig hikariConfig = createHikariConfig(configuration);
    // Pool settings
    hikariConfig.setMaximumPoolSize(
        configuration.getMaxConnections() > 0 ? configuration.getMaxConnections() : maximumTenantConnections);
    hikariConfig.setMinimumIdle(0);
    hikariConfig.setPoolName("Workspace-" + configuration.getWorkspaceId() + "-Pool");
    return new HikariDataSource(hikariConfig);
  }

  /**
   * Creates a HikariDataSource object based on the provided tenant data source
   * configuration.
   * 
   * @param configuration the tenant data source configuration
   * @return a configured HikariDataSource object
   */
  public static HikariDataSource createDataSource(TenantDataSourceConfiguration configuration) {
    return new HikariDataSource(createHikariConfig(configuration));
  }

  /**
   * Creates a HikariConfig object based on the provided tenant data source
   * configuration.
   * 
   * @param configuration the tenant data source configuration
   * @return a configured HikariConfig object
   */
  private static HikariConfig createHikariConfig(TenantDataSourceConfiguration configuration) {
    if (configuration == null
        || (configuration.getDataSourceClassName() == null && configuration.getJdbcUrl() == null)
        || configuration.getUsername() == null
        || configuration.getUsername().isBlank()) {
      throw new IllegalArgumentException("Invalid workspace datasource configuration");
    }
    HikariConfig hikariConfig = new HikariConfig();
    // Configure the data source class name or JDBC URL and driver class name based
    // on the configuration
    if (configuration.getDataSourceClassName() != null && !configuration.getDataSourceClassName().isBlank()) {
      hikariConfig.setDataSourceClassName(configuration.getDataSourceClassName());
      hikariConfig.addDataSourceProperty("serverName", configuration.getServerName());
      hikariConfig.addDataSourceProperty("portNumber", configuration.getPortNumber());
      hikariConfig.addDataSourceProperty("databaseName", configuration.getDatabaseName());
      hikariConfig.addDataSourceProperty("ssl", configuration.isSsl());
    } else {
      hikariConfig.setJdbcUrl(configuration.getJdbcUrl());
      hikariConfig.setDriverClassName(configuration.getDriverClassName());
    }
    hikariConfig.setUsername(configuration.getUsername());
    hikariConfig.setPassword(configuration.getPassword());

    if (AZURE_SPN_AUTH_METHOD.equals(configuration.getAuthMethod())) {
      hikariConfig.addDataSourceProperty("sslmode", "require");
      hikariConfig.addDataSourceProperty("authenticationPluginClassName", AZURE_AUTHENTICATION_PLUGIN);
      hikariConfig.addDataSourceProperty("azure.tenantId", configuration.getAzureTenantId());
      hikariConfig.addDataSourceProperty("azure.clientId", configuration.getAzureClientId());
      hikariConfig.addDataSourceProperty("azure.clientSecret", configuration.getAzureClientSecret());
    }

    return hikariConfig;
  }

}