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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.zeroio.platform.domain.model.tenant.TenantDataSourceDatabase;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

/**
 * Persists pooled tenant databases in the primary CMS database.
 */
public class TenantDataSourceDatabaseRepository {

  private static final Log LOG = LogFactory.getLog(TenantDataSourceDatabaseRepository.class);
  private static final String TABLE_NAME = "pool_databases";

  private TenantDataSourceDatabaseRepository() {
  }

  public static TenantDataSourceDatabase findById(long databaseId) {
    if (databaseId < 1) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(() -> DB.SELECT("*").FROM(TABLE_NAME)
        .WHERE("pool_database_id = ?", databaseId).returnRecord(TenantDataSourceDatabaseRepository::buildRecord));
  }

  public static TenantDataSourceDatabase findByPoolIdAndDatabaseName(long poolId, String databaseName) {
    if (poolId < 1 || StringUtils.isBlank(databaseName)) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(() -> DB.SELECT("*").FROM(TABLE_NAME)
        .WHERE("pool_id = ?", poolId).AND("database_name = ?", databaseName)
        .returnRecord(TenantDataSourceDatabaseRepository::buildRecord));
  }

  public static List<TenantDataSourceDatabase> findAll() {
    return WorkspaceContextManager.withoutWorkspace(() -> DB.SELECT("*").FROM(TABLE_NAME)
        .ORDER_BY("pool_database_id")
        .WITH(new DataConstraints().setDefaultColumnToSortBy("pool_database_id").setUseCount(false))
        .returnDataResult(TenantDataSourceDatabaseRepository::buildRecord).getRecords());
  }

  public static List<TenantDataSourceDatabase> findByPoolId(long poolId) {
    if (poolId < 1) {
      return Collections.emptyList();
    }
    return WorkspaceContextManager.withoutWorkspace(() -> DB.SELECT("*").FROM(TABLE_NAME)
        .WHERE("pool_id = ?", poolId).ORDER_BY("pool_database_id")
        .WITH(new DataConstraints().setDefaultColumnToSortBy("pool_database_id").setUseCount(false))
        .returnDataResult(TenantDataSourceDatabaseRepository::buildRecord).getRecords());
  }

  /** Marks a free pooled database as active and unavailable, within the caller's transaction. */
  public static boolean allocate(Connection connection, long databaseId) throws SQLException {
    return DB.UPDATE(TABLE_NAME)
        .SET("active", true)
        .SET("available", false)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("pool_database_id = ?", databaseId)
        .AND("active = ?", false)
        .AND("available = ?", true)
        .execute(connection).booleanValue();
  }

  public static TenantDataSourceDatabase save(TenantDataSourceDatabase record) {
    if (record == null) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(() -> {
      Timestamp modified = new Timestamp(System.currentTimeMillis());
      if (record.getId() > 0) {
        boolean updated = DB.UPDATE(TABLE_NAME)
            .SET("pool_id", record.getPoolId())
            .SET("database_name", record.getDatabaseName())
            .SET("active", record.isActive())
            .SET("available", record.isAvailable())
            .SET("modified", modified)
            .WHERE("pool_database_id = ?", record.getId()).execute();
        if (!updated) {
          LOG.error("Unable to update tenant datasource database id: " + record.getId());
          return null;
        }
      } else {
        Timestamp created = record.getCreated() == null ? modified : record.getCreated();
        long id = DB.INSERT().INTO(TABLE_NAME)
            .FIELD("pool_id", record.getPoolId())
            .FIELD("database_name", record.getDatabaseName())
            .FIELD("active", record.isActive())
            .FIELD("available", record.isAvailable())
            .FIELD("created", created)
            .FIELD("modified", modified).execute();
        if (id < 1) {
          LOG.error("Unable to insert tenant datasource database");
          return null;
        }
        record.setId(id);
        record.setCreated(created);
      }
      record.setModified(modified);
      return record;
    });
  }

  private static TenantDataSourceDatabase buildRecord(ResultSet resultSet) {
    try {
      TenantDataSourceDatabase record = new TenantDataSourceDatabase();
      record.setId(resultSet.getLong("pool_database_id"));
      record.setPoolId(resultSet.getLong("pool_id"));
      record.setDatabaseName(resultSet.getString("database_name"));
      record.setActive(resultSet.getBoolean("active"));
      record.setAvailable(resultSet.getBoolean("available"));
      record.setCreated(resultSet.getTimestamp("created"));
      record.setModified(resultSet.getTimestamp("modified"));
      return record;
    } catch (SQLException exception) {
      LOG.error("Unable to read tenant datasource database", exception);
      return null;
    }
  }
}