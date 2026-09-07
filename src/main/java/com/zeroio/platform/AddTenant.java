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
package com.zeroio.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.Field;
import com.simisinc.platform.application.SecretCryptoCommand;
import com.simisinc.platform.infrastructure.database.ConnectionPool;
import com.simisinc.platform.infrastructure.database.DatabaseProperties;

/**
 * Console app for adding a tenant to the default CMS database.
 */
public class AddTenant {

  private static final String DEFAULT_DATABASE_PROPERTIES = "target/cms-platform/WEB-INF/classes/database.properties";
  private static final String DEFAULT_DRIVER_CLASS_NAME = "org.postgresql.Driver";
  private static final String OPTION_DOMAIN = "domain";
  private static final String OPTION_SITE_URL = "site-url";
  private static final String OPTION_USERNAME = "username";
  private static final String OPTION_PASSWORD = "password";

  public static void main(String[] args) throws Exception {
    Map<String, String> options = parseArguments(args);
    if (options.containsKey("help")) {
      printUsage();
      return;
    }

    require(options, "name");
    require(options, OPTION_DOMAIN);
    require(options, "file-root");
    require(options, "jdbc-url");
    require(options, OPTION_USERNAME);
    require(options, OPTION_SITE_URL);

    File propertiesFile = new File(options.getOrDefault("database-properties", DEFAULT_DATABASE_PROPERTIES));
    if (!propertiesFile.isFile()) {
      throw new IllegalArgumentException("Database properties file was not found: " + propertiesFile);
    }

    try (InputStream inputStream = new FileInputStream(propertiesFile)) {
      Properties databaseProperties = DatabaseProperties.configureDatabaseProperties(inputStream);
      ConnectionPool.init(databaseProperties);
    }

    final long[] workspaceId = { -1 };
    DB.withTransaction(DB.getDataSource(), () -> {
      workspaceId[0] = DB.INSERT().INTO("workspaces")
          .FIELDS(
              new Field("name", options.get("name")),
              new Field("site_url", options.get(OPTION_SITE_URL)),
              new Field("canonical_domain", options.get(OPTION_DOMAIN)),
              new Field("file_root", options.get("file-root")),
              new Field("active", true))
          .execute();
      if (workspaceId[0] < 1) {
        throw new IllegalStateException("Unable to create workspace");
      }

      long domainId = DB.INSERT().INTO("workspace_domains")
          .FIELDS(
              new Field("workspace_id", workspaceId[0]),
              new Field("host_pattern", options.get(OPTION_DOMAIN)),
              new Field("wildcard", false),
              new Field("active", true))
          .execute();
      if (domainId < 1) {
        throw new IllegalStateException("Unable to create workspace domain");
      }

      long datasourceId = DB.INSERT().INTO("workspace_data_sources")
          .FIELDS(
              new Field("workspace_id", workspaceId[0]),
              new Field("jdbc_url", options.get("jdbc-url")),
              new Field(OPTION_USERNAME, options.get(OPTION_USERNAME)),
              new Field(OPTION_PASSWORD, SecretCryptoCommand.encrypt(options.get(OPTION_PASSWORD))),
              new Field("driver_class_name", options.getOrDefault("driver", DEFAULT_DRIVER_CLASS_NAME)),
              new Field("pool_group", options.get("pool-group")))
          .execute();
      if (datasourceId < 0) {
        throw new IllegalStateException("Unable to create workspace datasource");
      }
    });

    System.out.println("Created tenant " + workspaceId[0] + " for " + options.get(OPTION_DOMAIN));
  }

  private static Map<String, String> parseArguments(String[] args) {
    Map<String, String> options = new HashMap<>();
    int index = 0;
    while (index < args.length) {
      String argument = args[index];
      if ("--help".equals(argument)) {
        options.put("help", "true");
        index++;
      } else if (argument.startsWith("--") && index + 1 < args.length && !args[index + 1].startsWith("--")) {
        options.put(argument.substring(2), args[index + 1]);
        index += 2;
      } else {
        throw new IllegalArgumentException("Invalid argument: " + argument);
      }
    }
    return options;
  }

  private static void require(Map<String, String> options, String name) {
    String value = options.get(name);
    if (value == null || value.isBlank()) {
      System.out.println("Missing required option: --" + name);
      System.exit(1);
    }
  }

  private static void printUsage() {
    System.out.println("Usage: AddTenant --name <name> --site-url <full_url> --domain <domain> --file-root <path> "
        + "--jdbc-url <url> --username <user> --password <password> [options]");
    System.out.println("Options: --driver <class>, --pool-group <name>, "
        + "--database-properties <path>, --help");
  }
}