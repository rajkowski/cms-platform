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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.infrastructure.database.ConnectionPool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Builds and registers live workspace datasource pools from persisted configuration.
 */
public class TenantDataSourceRegistrar {

  private static final Log LOG = LogFactory.getLog(TenantDataSourceRegistrar.class);
  private static final String AZURE_SPN_AUTH_METHOD = "azure-sql-spn";
  private static final String AZURE_AUTHENTICATION_PLUGIN = "com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin";

  private final TenantDataSourceConfigurationStore store;
  private final Map<Long, HikariDataSource> activeDataSources = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<HikariDataSource> retiredDataSources = new ConcurrentLinkedQueue<>();
  private final int maximumTenantConnections;
  private final ScheduledExecutorService retirementExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
    Thread thread = new Thread(runnable, "tenant-datasource-retirement");
    thread.setDaemon(true);
    return thread;
  });

  public TenantDataSourceRegistrar(TenantDataSourceConfigurationStore store) {
    this(store, 8);
  }

  public TenantDataSourceRegistrar(TenantDataSourceConfigurationStore store, int maximumTenantConnections) {
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

  public void saveAndRegister(TenantDataSourceConfiguration configuration) {
    TenantDataSourceConfiguration savedConfiguration = store.save(configuration);
    if (savedConfiguration == null) {
      throw new IllegalStateException("Unable to save datasource configuration for workspace " + configuration.getWorkspaceId());
    }
    register(savedConfiguration);
    retireIdleDataSources();
  }

  public void retireIdleDataSources() {
    for (HikariDataSource dataSource : retiredDataSources) {
      if (dataSource.getHikariPoolMXBean().getActiveConnections() == 0 && retiredDataSources.remove(dataSource)) {
        dataSource.close();
      }
    }
  }

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

  private void register(TenantDataSourceConfiguration configuration) {
    HikariDataSource replacement = createDataSource(configuration);
    HikariDataSource previous = activeDataSources.put(configuration.getWorkspaceId(), replacement);
    ConnectionPool.registerTenantDataSource(String.valueOf(configuration.getWorkspaceId()), replacement, configuration.getPoolGroup());
    if (configuration.getPoolGroup() != null && !configuration.getPoolGroup().isBlank()) {
      ConnectionPool.setTenantMaximumConnections(configuration.getPoolGroup(), maximumTenantConnections);
    }
    if (previous != null) {
      retiredDataSources.add(previous);
    }
  }

  private HikariDataSource createDataSource(TenantDataSourceConfiguration configuration) {
    if (configuration == null || configuration.getWorkspaceId() < 1 || configuration.getJdbcUrl() == null
        || configuration.getJdbcUrl().isBlank() || configuration.getUsername() == null || configuration.getUsername().isBlank()
        || configuration.getDriverClassName() == null || configuration.getDriverClassName().isBlank()) {
      throw new IllegalArgumentException("Invalid workspace datasource configuration");
    }
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(configuration.getJdbcUrl());
    hikariConfig.setUsername(configuration.getUsername());
    hikariConfig.setPassword(configuration.getPassword());
    hikariConfig.setDriverClassName(configuration.getDriverClassName());
    if (AZURE_SPN_AUTH_METHOD.equals(configuration.getAuthMethod())) {
      hikariConfig.addDataSourceProperty("authenticationPluginClassName", AZURE_AUTHENTICATION_PLUGIN);
      hikariConfig.addDataSourceProperty("azure.tenantId", configuration.getAzureTenantId());
      hikariConfig.addDataSourceProperty("azure.clientId", configuration.getAzureClientId());
      hikariConfig.addDataSourceProperty("azure.clientSecret", configuration.getAzureClientSecret());
    }
    hikariConfig.setMaximumPoolSize(maximumTenantConnections);
    hikariConfig.setMinimumIdle(0);
    hikariConfig.setPoolName("Workspace-" + configuration.getWorkspaceId() + "-Pool");
    return new HikariDataSource(hikariConfig);
  }
}