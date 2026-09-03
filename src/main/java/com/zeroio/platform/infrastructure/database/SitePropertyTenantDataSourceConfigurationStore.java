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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Field;
import com.simisinc.platform.application.SecretCryptoCommand;

/**
 * Persists workspace datasource configuration in the primary CMS database.
 */
public class SitePropertyTenantDataSourceConfigurationStore implements TenantDataSourceConfigurationStore {

  private static final Log LOG = LogFactory.getLog(SitePropertyTenantDataSourceConfigurationStore.class);
  private static final String TABLE_NAME = "workspace_data_sources";

  @Override
  public List<TenantDataSourceConfiguration> findAll() {
    return DB.SELECT("*").FROM(TABLE_NAME).ORDER_BY("workspace_id")
        .WITH(new DataConstraints().setDefaultColumnToSortBy("workspace_id").setUseCount(false))
        .returnDataResult(SitePropertyTenantDataSourceConfigurationStore::buildRecord).getRecords();
  }

  @Override
  public TenantDataSourceConfiguration save(TenantDataSourceConfiguration configuration) {
    validate(configuration);
    boolean exists = DB.SELECT("workspace_id").FROM(TABLE_NAME).WHERE("workspace_id = ?", configuration.getWorkspaceId())
        .returnRecord(resultSet -> resultSet.getLong("workspace_id")) != null;
    if (exists) {
      boolean updated = DB.UPDATE(TABLE_NAME)
          .SET("jdbc_url", configuration.getJdbcUrl())
          .SET("username", configuration.getUsername())
          .SET("password", SecretCryptoCommand.encrypt(configuration.getPassword()))
          .SET("driver_class_name", configuration.getDriverClassName())
          .WHERE("workspace_id = ?", configuration.getWorkspaceId())
          .execute();
      return updated ? configuration : null;
    }
    long generatedId = DB.INSERT().INTO(TABLE_NAME)
        .FIELDS(new Field("workspace_id", configuration.getWorkspaceId()), new Field("jdbc_url", configuration.getJdbcUrl()),
            new Field("password", SecretCryptoCommand.encrypt(configuration.getPassword())),
            new Field("driver_class_name", configuration.getDriverClassName()))
        .execute();
    return generatedId > -1 ? configuration : null;
  }

  private static TenantDataSourceConfiguration buildRecord(ResultSet resultSet) {
    try {
      TenantDataSourceConfiguration configuration = new TenantDataSourceConfiguration();
      configuration.setWorkspaceId(resultSet.getLong("workspace_id"));
      configuration.setJdbcUrl(resultSet.getString("jdbc_url"));
      configuration.setUsername(resultSet.getString("username"));
      configuration.setPassword(SecretCryptoCommand.decrypt(resultSet.getString("password")));
      configuration.setDriverClassName(resultSet.getString("driver_class_name"));
      return configuration;
    } catch (SQLException e) {
      LOG.error("Unable to read workspace datasource configuration", e);
      return null;
    }
  }

  private static void validate(TenantDataSourceConfiguration configuration) {
    if (configuration == null || configuration.getWorkspaceId() < 1) {
      throw new IllegalArgumentException("Workspace id must be greater than zero");
    }
    if (configuration.getJdbcUrl() == null || configuration.getJdbcUrl().isBlank()) {
      throw new IllegalArgumentException("JDBC URL cannot be null or blank");
    }
    if (configuration.getUsername() == null || configuration.getUsername().isBlank()) {
      throw new IllegalArgumentException("Username cannot be null or blank");
    }
    if (configuration.getDriverClassName() == null || configuration.getDriverClassName().isBlank()) {
      throw new IllegalArgumentException("Driver class name cannot be null or blank");
    }
  }
}