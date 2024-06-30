/*
 * Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Properties for accessing the CMS database */
public class DatabaseProperties {

  private static Log LOG = LogFactory.getLog(DatabaseProperties.class);

  public static Properties configureDatabaseProperties(InputStream is) throws Exception {
    LOG.info("Configuring the database properties...");
    // Start with the default properties...
    Properties databaseProperties = new Properties();
    databaseProperties.load(is);
    // Check for environment variables
    if (System.getenv().containsKey("DB_SERVER_NAME")) {
      LOG.info("Found variable DB_SERVER_NAME=" + System.getenv("DB_SERVER_NAME"));
      databaseProperties.setProperty("dataSource.serverName", System.getenv("DB_SERVER_NAME"));
    }
    if (System.getenv().containsKey("DB_USER")) {
      LOG.info("Found variable DB_USER");
      databaseProperties.setProperty("dataSource.user", System.getenv("DB_USER"));
    }
    if (System.getenv().containsKey("DB_PASSWORD")) {
      LOG.info("Found variable DB_PASSWORD");
      databaseProperties.setProperty("dataSource.password", System.getenv("DB_PASSWORD"));
    }
    if (System.getenv().containsKey("DB_NAME")) {
      LOG.info("Found variable DB_NAME=" + System.getenv("DB_NAME"));
      databaseProperties.setProperty("dataSource.databaseName", System.getenv("DB_NAME"));
    }
    if (System.getenv().containsKey("DB_SSL") && "true".equals(System.getenv("DB_SSL"))) {
      LOG.info("Found variable DB_SSL=" + System.getenv("DB_SSL"));
      databaseProperties.setProperty("dataSource.ssl", "true");
    }
    return databaseProperties;
  }
}
