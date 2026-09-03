/*
 * Copyright 2025-2026 Matt Rajkowski (https://github.com/rajkowski)
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

import java.sql.Connection;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.zeroio.platform.infrastructure.database.SitePropertyTenantDataSourceConfigurationStore;
import com.zeroio.platform.infrastructure.database.TenantDataSourceConfiguration;
import com.zeroio.platform.infrastructure.database.TenantDataSourceRegistrar;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Manages the PostgreSQL database connections and generates the SQL statements
 *
 * @author matt rajkowski
 * @created 4/8/18 5:08 PM
 */
public class ConnectionPool {

  private static Log LOG = LogFactory.getLog(ConnectionPool.class);

  private static HikariDataSource applicationDS;
  private static HikariDataSource backgroundJobsDS;
  private static HikariDataSource distributedMessagingDS;
  private static final TenantRegistry TENANT_REGISTRY = new TenantRegistry();
  private static TenantDataSourceRegistrar tenantDataSourceRegistrar;

  private ConnectionPool() {
  }

  public static void init(Properties properties) {
    applicationDS = initApplicationCP(properties);
    DB.setDataSource(applicationDS);
    DB.setTenantRegistry(TENANT_REGISTRY);
    tenantDataSourceRegistrar = new TenantDataSourceRegistrar(new SitePropertyTenantDataSourceConfigurationStore());
    LOG.info("Max pool size (applicationDS): " + applicationDS.getMaximumPoolSize());
    backgroundJobsDS = initBackgroundJobsCP(properties);
    LOG.info("Max pool size (backgroundJobsDS): " + backgroundJobsDS.getMaximumPoolSize());
    distributedMessagingDS = initDistributedMessagingCP(properties);
    LOG.info("Max pool size (messageQueueDS): " + distributedMessagingDS.getMaximumPoolSize());
  }

  /** Configure the application's connection pool */
  private static HikariDataSource initApplicationCP(Properties properties) {
    HikariConfig config = new HikariConfig(mergePropertiesFromPrefix(properties, "application"));
    if (!properties.containsKey("application.connectionTimeout")) {
      config.setConnectionTimeout(5_000);
    }
    config.setMaxLifetime(600_000);
    config.setPoolName("Web-Application-Pool");
    return new HikariDataSource(config);
  }

  /** Configure the application's background jobs connection pool */
  private static HikariDataSource initBackgroundJobsCP(Properties properties) {
    HikariConfig config = new HikariConfig(mergePropertiesFromPrefix(properties, "backgroundJobs"));
    config.setMaxLifetime(600_000);
    config.setPoolName("Background-Jobs-Pool");
    return new HikariDataSource(config);
  }

  /** Configure the application's connection pool for distributed messaging connections */
  private static HikariDataSource initDistributedMessagingCP(Properties properties) {
    HikariConfig config = new HikariConfig(mergePropertiesFromPrefix(properties, "distributedMessaging"));
    config.setMaxLifetime(600_000);
    config.setPoolName("Distributed-Messaging-Pool");
    return new HikariDataSource(config);
  }

  public static void shutdown() {
    if (tenantDataSourceRegistrar != null) {
      tenantDataSourceRegistrar.shutdown();
      tenantDataSourceRegistrar = null;
      TENANT_REGISTRY.clear();
    }
    if (applicationDS != null) {
      applicationDS.close();
      applicationDS = null;
    }
    if (backgroundJobsDS != null) {
      backgroundJobsDS.close();
      backgroundJobsDS = null;
    }
    if (distributedMessagingDS != null) {
      distributedMessagingDS.close();
      distributedMessagingDS = null;
    }
  }

  /* Each pool is configured with the base dataSource properties and name specific properties */
  private static Properties mergePropertiesFromPrefix(Properties properties, String prefix) {
    Properties filteredProperties = new Properties();
    for (String name : properties.stringPropertyNames()) {
      if (name.startsWith("dataSource") || "driverClassName".equals(name) || "jdbcUrl".equals(name)) {
        filteredProperties.setProperty(name, properties.getProperty(name));
        continue;
      }
      if (name.startsWith(prefix)) {
        String keyWithoutPrefix = name.substring(prefix.length() + 1);
        filteredProperties.setProperty(keyWithoutPrefix, properties.getProperty(name));
      }
    }
    return filteredProperties;
  }

  public static javax.sql.DataSource getApplicationDataSource() {
    return applicationDS;
  }

  public static javax.sql.DataSource getBackgroundJobsDataSource() {
    return backgroundJobsDS;
  }

  public static javax.sql.DataSource getDistributedMessagingDataSource() {
    return distributedMessagingDS;
  }

  public static void registerTenantDataSource(String tenantId, javax.sql.DataSource dataSource) {
    TENANT_REGISTRY.register(tenantId, dataSource);
  }

  public static void unregisterTenantDataSource(String tenantId) {
    TENANT_REGISTRY.unregister(tenantId);
  }

  public static void registerConfiguredTenantDataSources() {
    if (tenantDataSourceRegistrar == null) {
      throw new IllegalStateException("ConnectionPool has not been initialized");
    }
    tenantDataSourceRegistrar.registerAllAtStartup();
  }

  public static void saveAndRegisterTenantDataSource(TenantDataSourceConfiguration configuration) {
    if (tenantDataSourceRegistrar == null) {
      throw new IllegalStateException("ConnectionPool has not been initialized");
    }
    tenantDataSourceRegistrar.saveAndRegister(configuration);
  }

  public static void retireIdleTenantDataSources() {
    if (tenantDataSourceRegistrar != null) {
      tenantDataSourceRegistrar.retireIdleDataSources();
    }
  }

  public static TenantRegistry getTenantRegistry() {
    return TENANT_REGISTRY;
  }

  public static boolean isLive() {
    // Use the background jobs connection pool to determine if the database is live
    try (Connection connection = backgroundJobsDS.getConnection()) {
      return connection.isValid(2);
    } catch (Exception e) {
      LOG.error("Database connection is not valid", e);
      return false;
    }
  }
}
