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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.SecretCryptoCommand;

/**
 * Persists workspace datasource configuration in the primary CMS database.
 */
public class TenantDataSourceConfigurationStore implements TenantDataSourceConfigurationStoreInterface {

  private static final Log LOG = LogFactory.getLog(TenantDataSourceConfigurationStore.class);
  private static final String TABLE_NAME = "workspace_data_sources";

  public List<TenantDataSourceConfiguration> findAll() {
    return WorkspaceContextManager
        .withoutWorkspace(() -> DB.SELECT("wds.workspace_id, pd.pool_database_id, pd.database_name, p.*")
            .FROM(TABLE_NAME).AS("wds")
            .JOIN("pool_databases pd").ON("wds.pool_database_id = pd.pool_database_id")
            .JOIN("data_source_pools p").ON("pd.pool_id = p.pool_id")
            .WHERE("pd.active = ?", true)
            .ORDER_BY("wds.workspace_id")
            .WITH(new DataConstraints().setDefaultColumnToSortBy("wds.workspace_id").setUseCount(false))
            .returnDataResult(TenantDataSourceConfigurationStore::buildRecord).getRecords());
  }

  public static TenantDataSourceConfiguration findByWorkspaceId(long workspaceId) {
    return WorkspaceContextManager
        .withoutWorkspace(() -> DB.SELECT("wds.workspace_id, pd.pool_database_id, pd.database_name, p.*")
            .FROM(TABLE_NAME).AS("wds")
            .JOIN("pool_databases pd").ON("wds.pool_database_id = pd.pool_database_id")
            .JOIN("data_source_pools p").ON("pd.pool_id = p.pool_id")
            .WHERE("wds.workspace_id = ?", workspaceId)
            .returnRecord(TenantDataSourceConfigurationStore::buildRecord));
  }

  public static TenantDataSourceConfiguration findByDatabaseId(long databaseId) {
    return WorkspaceContextManager
        .withoutWorkspace(() -> DB.SELECT("pd.pool_database_id, pd.database_name, p.*")
            .FROM("pool_databases").AS("pd")
            .JOIN("data_source_pools p").ON("pd.pool_id = p.pool_id")
            .WHERE("pd.pool_database_id = ?", databaseId)
            .returnRecord(TenantDataSourceConfigurationStore::buildRecord));
  }

  private static TenantDataSourceConfiguration buildRecord(ResultSet resultSet) {
    try {
      // Workspaces Table
      long workspaceId = -1;
      if (DB.hasColumn(resultSet, "workspace_id")) {
        workspaceId = resultSet.getLong("workspace_id");
      }
      // Databases Table
      String databaseName = resultSet.getString("database_name");
      // Pools Table
      String poolUniqueId = resultSet.getString("pool_unique_id");
      String serverName = resultSet.getString("server_name");
      int portNumber = resultSet.getInt("port_number");
      boolean isSSL = resultSet.getBoolean("ssl");
      String authMethod = resultSet.getString("auth_method");
      String azureTenantId = resultSet.getString("azure_tenant_id");
      String azureClientId = resultSet.getString("azure_client_id");
      String azureClientSecret = SecretCryptoCommand.decrypt(resultSet.getString("azure_client_secret"));
      String username = resultSet.getString("username");
      String password = SecretCryptoCommand.decrypt(resultSet.getString("password"));
      String dataSourceClassName = resultSet.getString("ds_class_name");
      String driverClassName = resultSet.getString("driver_class_name");
      int maxConnections = resultSet.getInt("max_connections");

      // Build the TenantDataSourceConfiguration object from the result set values
      TenantDataSourceConfiguration configuration = new TenantDataSourceConfiguration();
      configuration.setPoolGroup(poolUniqueId);
      configuration.setServerName(serverName);
      configuration.setPortNumber(portNumber);
      configuration.setWorkspaceId(workspaceId);
      // Use the data source class name if provided, otherwise build the JDBC URL and
      // set the driver class name
      if (!StringUtils.isBlank(dataSourceClassName)) {
        configuration.setDataSourceClassName(dataSourceClassName);
      } else if (!StringUtils.isBlank(driverClassName)) {
        configuration.setDriverClassName(driverClassName);
        String jdbcUrl = "jdbc:postgresql://" + serverName + ":" + portNumber + "/" + databaseName +
            (authMethod != null && authMethod.equals("azure-sql-spn") ? "?sslmode=require" +
                "&authenticationPluginClassName=com.azure.identity.extensions.jdbc.postgresql.AzurePostgresqlAuthenticationPlugin"
                : "");
        configuration.setJdbcUrl(jdbcUrl);
      }
      configuration.setSsl(isSSL);
      configuration.setDatabaseName(databaseName);
      configuration.setUsername(username);
      configuration.setPassword(password);
      configuration.setAuthMethod(authMethod);
      configuration.setAzureTenantId(azureTenantId);
      configuration.setAzureClientId(azureClientId);
      configuration.setAzureClientSecret(azureClientSecret);
      configuration.setMaxConnections(maxConnections);

      return configuration;
    } catch (SQLException e) {
      LOG.error("Unable to read workspace datasource configuration", e);
      return null;
    }
  }

}