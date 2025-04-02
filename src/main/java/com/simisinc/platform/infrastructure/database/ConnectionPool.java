/*
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

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

  private ConnectionPool() {
  }

  public static void init(Properties properties) {
    applicationDS = initApplicationCP(properties);
    LOG.info("Max pool size (applicationDS): " + applicationDS.getMaximumPoolSize());
    backgroundJobsDS = initBackgroundJobsCP(properties);
    LOG.info("Max pool size (backgroundJobsDS): " + backgroundJobsDS.getMaximumPoolSize());
    distributedMessagingDS = initDistributedMessagingCP(properties);
    LOG.info("Max pool size (messageQueueDS): " + distributedMessagingDS.getMaximumPoolSize());
  }

  /** Configure the application's connection pool */
  private static HikariDataSource initApplicationCP(Properties properties) {
    HikariConfig config = new HikariConfig(mergePropertiesFromPrefix(properties, "application"));
    config.setMaxLifetime(600_000);
    return new HikariDataSource(config);
  }

  /** Configure the application's connection pool */
  private static HikariDataSource initBackgroundJobsCP(Properties properties) {
    HikariConfig config = new HikariConfig(mergePropertiesFromPrefix(properties, "backgroundJobs"));
    config.setMaxLifetime(600_000);
    return new HikariDataSource(config);
  }

  /** Configure the application's connection pool for persistent, long-lived connections */
  private static HikariDataSource initDistributedMessagingCP(Properties properties) {
    HikariConfig config = new HikariConfig(mergePropertiesFromPrefix(properties, "distributedMessaging"));
    config.setMaxLifetime(600_000);
    return new HikariDataSource(config);
  }

  public static void shutdown() {
    applicationDS.close();
    backgroundJobsDS.close();
    distributedMessagingDS.close();
  }

  private static Properties mergePropertiesFromPrefix(Properties properties, String prefix) {
    Properties filteredProperties = new Properties();
    for (String name : properties.stringPropertyNames()) {
      if (name.startsWith("dataSource")) {
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

}
