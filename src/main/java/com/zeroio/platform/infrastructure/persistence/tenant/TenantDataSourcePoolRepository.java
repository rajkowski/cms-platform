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
package com.zeroio.platform.infrastructure.persistence.tenant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.SecretCryptoCommand;
import com.zeroio.platform.domain.model.tenant.TenantDataSourcePool;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

/**
 * Persists tenant datasource pools in the primary CMS database.
 */
public class TenantDataSourcePoolRepository {

  private static final Log LOG = LogFactory.getLog(TenantDataSourcePoolRepository.class);
  private static final String TABLE_NAME = "data_source_pools";

  private TenantDataSourcePoolRepository() {
  }

  public static TenantDataSourcePool findById(long poolId) {
    if (poolId < 1) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(() -> DB.SELECT("*").FROM(TABLE_NAME)
        .WHERE("pool_id = ?", poolId).returnRecord(TenantDataSourcePoolRepository::buildRecord));
  }

  public static TenantDataSourcePool findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(() -> DB.SELECT("*").FROM(TABLE_NAME)
        .WHERE("pool_unique_id = ?", uniqueId).returnRecord(TenantDataSourcePoolRepository::buildRecord));
  }

  public static List<TenantDataSourcePool> findAll() {
    return WorkspaceContextManager.withoutWorkspace(() -> DB.SELECT("*").FROM(TABLE_NAME)
        .ORDER_BY("pool_id")
        .WITH(new DataConstraints().setDefaultColumnToSortBy("pool_id").setUseCount(false))
        .returnDataResult(TenantDataSourcePoolRepository::buildRecord).getRecords());
  }

  public static TenantDataSourcePool save(TenantDataSourcePool record) {
    if (record == null) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(() -> {
      String password = SecretCryptoCommand.encrypt(record.getPassword());
      String azureClientSecret = SecretCryptoCommand.encrypt(record.getAzureClientSecret());
      if (record.getId() > 0) {
        boolean updated = DB.UPDATE(TABLE_NAME)
            .SET("pool_unique_id", record.getUniqueId())
            .SET("ds_class_name", record.getDsClassName())
            .SET("driver_class_name", record.getDriverClassName())
            .SET("server_name", record.getServerName())
            .SET("username", record.getUsername())
            .SET("password", password)
            .SET("port_number", record.getPortNumber())
            .SET("ssl", record.isSsl())
            .SET("auth_method", record.getAuthMethod())
            .SET("azure_tenant_id", record.getAzureTenantId())
            .SET("azure_client_id", record.getAzureClientId())
            .SET("azure_client_secret", azureClientSecret)
            .SET("max_connections", record.getMaxConnections())
            .WHERE("pool_id = ?", record.getId()).execute();
        if (!updated) {
          LOG.error("Unable to update tenant datasource pool id: " + record.getId());
          return null;
        }
      } else {
        long id = DB.INSERT().INTO(TABLE_NAME)
            .FIELD("pool_unique_id", record.getUniqueId())
            .FIELD("ds_class_name", record.getDsClassName())
            .FIELD("driver_class_name", record.getDriverClassName())
            .FIELD("server_name", record.getServerName())
            .FIELD("username", record.getUsername())
            .FIELD("password", password)
            .FIELD("port_number", record.getPortNumber())
            .FIELD("ssl", record.isSsl())
            .FIELD("auth_method", record.getAuthMethod())
            .FIELD("azure_tenant_id", record.getAzureTenantId())
            .FIELD("azure_client_id", record.getAzureClientId())
            .FIELD("azure_client_secret", azureClientSecret)
            .FIELD("max_connections", record.getMaxConnections()).execute();
        if (id < 1) {
          LOG.error("Unable to insert tenant datasource pool");
          return null;
        }
        record.setId(id);
      }
      return record;
    });
  }

  private static TenantDataSourcePool buildRecord(ResultSet resultSet) {
    try {
      TenantDataSourcePool record = new TenantDataSourcePool();
      record.setId(resultSet.getLong("pool_id"));
      record.setUniqueId(resultSet.getString("pool_unique_id"));
      record.setDsClassName(resultSet.getString("ds_class_name"));
      record.setDriverClassName(resultSet.getString("driver_class_name"));
      record.setServerName(resultSet.getString("server_name"));
      record.setUsername(resultSet.getString("username"));
      record.setPassword(SecretCryptoCommand.decrypt(resultSet.getString("password")));
      record.setPortNumber(resultSet.getInt("port_number"));
      record.setSsl(resultSet.getBoolean("ssl"));
      record.setAuthMethod(resultSet.getString("auth_method"));
      record.setAzureTenantId(resultSet.getString("azure_tenant_id"));
      record.setAzureClientId(resultSet.getString("azure_client_id"));
      record.setAzureClientSecret(SecretCryptoCommand.decrypt(resultSet.getString("azure_client_secret")));
      record.setMaxConnections(resultSet.getInt("max_connections"));
      return record;
    } catch (SQLException exception) {
      LOG.error("Unable to read tenant datasource pool", exception);
      return null;
    }
  }
}