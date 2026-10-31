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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.database.ConnectionPool;
import com.zeroio.platform.domain.model.tenant.Workspace;
import com.zeroio.platform.infrastructure.database.TenantDataSourceConfigurationStore;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

/**
 * Repository access for workspace records.
 *
 * Purpose of this change: centralize lookup of the active workspace row, its
 * canonical host, and the ownership data
 * needed by the workspace selector and launch flow. These queries are
 * intentionally read from the global CMS database
 * so the app can resolve the correct tenant before switching to that tenant's
 * datasource.
 */
public class WorkspaceRepository {

  private static final Log LOG = LogFactory.getLog(WorkspaceRepository.class);
  private static final String TABLE_NAME = "workspaces";

  private static final String SELECT_COLUMNS = "w.*, ou.first_name AS owner_first_name, ou.last_name AS owner_last_name, "
      + "ou.email AS owner_email, wds.pool_database_id";

  private WorkspaceRepository() {
  }

  public static Workspace findById(long workspaceId) {
    if (workspaceId < 1) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT(SELECT_COLUMNS)
            .FROM(TABLE_NAME).AS("w")
            .LEFT_JOIN("workspace_data_sources wds").ON("w.workspace_id = wds.workspace_id")
            .LEFT_JOIN("users ou").ON("w.owner = ou.user_id")
            .WHERE("w.workspace_id = ?", workspaceId).returnRecord(WorkspaceRepository::buildRecord));
  }

  public static Workspace findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT(SELECT_COLUMNS)
            .FROM(TABLE_NAME).AS("w")
            .LEFT_JOIN("workspace_data_sources wds").ON("w.workspace_id = wds.workspace_id")
            .LEFT_JOIN("users ou").ON("w.owner = ou.user_id")
            .WHERE("w.workspace_unique_id = ?", uniqueId).returnRecord(WorkspaceRepository::buildRecord));
  }

  public static Workspace findActiveByCanonicalDomain(String canonicalDomain) {
    if (canonicalDomain == null || canonicalDomain.isBlank()) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT(SELECT_COLUMNS)
            .FROM(TABLE_NAME).AS("w")
            .LEFT_JOIN("workspace_data_sources wds").ON("w.workspace_id = wds.workspace_id")
            .LEFT_JOIN("users ou").ON("w.owner = ou.user_id")
            .WHERE("w.canonical_domain = ?", canonicalDomain)
            .AND("w.active = ?", true)
            .returnRecord(WorkspaceRepository::buildRecord));
  }

  public static Workspace findByCanonicalDomain(String canonicalDomain) {
    if (canonicalDomain == null || canonicalDomain.isBlank()) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT(SELECT_COLUMNS)
            .FROM(TABLE_NAME).AS("w")
            .LEFT_JOIN("workspace_data_sources wds").ON("w.workspace_id = wds.workspace_id")
            .LEFT_JOIN("users ou").ON("w.owner = ou.user_id")
            .WHERE("w.canonical_domain = ?", canonicalDomain.trim())
            .returnRecord(WorkspaceRepository::buildRecord));
  }

  public static List<Workspace> findAllActive() {
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT(SELECT_COLUMNS)
            .FROM(TABLE_NAME).AS("w")
            .LEFT_JOIN("workspace_data_sources wds").ON("w.workspace_id = wds.workspace_id")
            .LEFT_JOIN("users ou").ON("w.owner = ou.user_id")
            .WHERE("w.active = ?", true)
            .ORDER_BY("w.workspace_id")
            .returnDataResult(WorkspaceRepository::buildRecord).getRecords());
  }

  public static List<Workspace> findActiveWorkspacesByUserId(long mainTenantUserId) {
    return WorkspaceContextManager.withoutWorkspace(
        () -> DB.SELECT(SELECT_COLUMNS)
            .FROM(TABLE_NAME).AS("w")
            .JOIN("workspace_access_grants g")
            .ON("w.workspace_id = g.workspace_id")
            .LEFT_JOIN("workspace_data_sources wds").ON("w.workspace_id = wds.workspace_id")
            .LEFT_JOIN("users ou").ON("w.owner = ou.user_id")
            .WHERE("g.user_id = ?", mainTenantUserId)
            .AND("g.active = ?", true)
            .AND("w.active = ?", true)
            .ORDER_BY("w.name")
            .returnDataResult(WorkspaceRepository::buildRecord).getRecords());
  }

  private static Workspace buildRecord(ResultSet resultSet) {
    try {
      Workspace workspace = new Workspace();
      workspace.setId(resultSet.getLong("workspace_id"));
      long poolDatabaseId = resultSet.getLong("pool_database_id");
      if (!resultSet.wasNull()) {
        workspace.setPoolDatabaseId(poolDatabaseId);
      }
      workspace.setUniqueId(resultSet.getString("workspace_unique_id"));
      workspace.setName(resultSet.getString("name"));
      workspace.setSiteUrl(resultSet.getString("site_url"));
      workspace.setCanonicalDomain(resultSet.getString("canonical_domain"));
      workspace.setFileRoot(resultSet.getString("file_root"));
      workspace.setActive(resultSet.getBoolean("active"));
      workspace.setDescription(resultSet.getString("description"));
      workspace.setDepartment(resultSet.getString("department"));
      long ownerId = resultSet.getLong("owner");
      if (!resultSet.wasNull()) {
        workspace.setOwnerId(ownerId);
      }
      String ownerFirstName = resultSet.getString("owner_first_name");
      String ownerLastName = resultSet.getString("owner_last_name");
      if (ownerFirstName != null || ownerLastName != null) {
        String fullName = (StringUtils.defaultString(ownerFirstName) + " " + StringUtils.defaultString(ownerLastName))
            .trim();
        workspace.setOwnerName(fullName);
      }
      workspace.setOwnerEmail(resultSet.getString("owner_email"));
      workspace.setShowInCatalog(resultSet.getBoolean("show_in_catalog"));
      workspace.setCreated(resultSet.getTimestamp("created"));
      workspace.setModified(resultSet.getTimestamp("modified"));
      return workspace;
    } catch (SQLException e) {
      LOG.error("Unable to build workspace record", e);
      return null;
    }
  }

  public static Workspace save(Workspace record) {
    if (record == null) {
      return null;
    }
    return WorkspaceContextManager.withoutWorkspace(
        () -> {
          if (record.getId() > 0) {
            boolean updated = DB.UPDATE(TABLE_NAME)
                .SET("workspace_unique_id = ?", record.getUniqueId())
                .SET("name = ?", StringUtils.trimToNull(record.getName()))
                .SET("site_url = ?", StringUtils.trimToNull(record.getSiteUrl()))
                .SET("canonical_domain = ?", StringUtils.trimToNull(record.getCanonicalDomain()))
                .SET("file_root = ?", StringUtils.trimToNull(record.getFileRoot()))
                .SET("active = ?", record.isActive())
                .SET("show_in_catalog = ?", record.isShowInCatalog())
                .SET("department = ?", StringUtils.trimToNull(record.getDepartment()))
                .SET("description = ?", StringUtils.trimToNull(record.getDescription()))
                .SET("owner = ?", record.getOwnerId() > 0 ? record.getOwnerId() : null)
                .SET("modified_by = ?", record.getModifiedBy() > 0 ? record.getModifiedBy() : null)
                .SET("modified = ?", new Timestamp(System.currentTimeMillis()))
                .WHERE("workspace_id = ?", record.getId())
                .execute().booleanValue();
            if (!updated) {
              return null;
            }
            return record;
          } else {
            long id = DB.INSERT().INTO(TABLE_NAME)
                .FIELD("workspace_unique_id", StringUtils.trimToNull(record.getUniqueId()))
                .FIELD("name", StringUtils.trimToNull(record.getName()))
                .FIELD("site_url", StringUtils.trimToNull(record.getSiteUrl()))
                .FIELD("canonical_domain", StringUtils.trimToNull(record.getCanonicalDomain()))
                .FIELD("file_root", StringUtils.trimToNull(record.getFileRoot()))
                .FIELD("active", record.isActive())
                .FIELD("show_in_catalog", record.isShowInCatalog())
                .FIELD("department", StringUtils.trimToNull(record.getDepartment()))
                .FIELD("description", StringUtils.trimToNull(record.getDescription()))
                .FIELD("owner", record.getOwnerId() > 0 ? record.getOwnerId() : null)
                .FIELD("created_by", record.getCreatedBy() > 0 ? record.getCreatedBy() : null)
                .FIELD("modified_by", record.getModifiedBy() > 0 ? record.getModifiedBy() : null)
                .execute();
            if (id < 1) {
              LOG.error("The insert failed for workspace: " + record.getName());
              return null;
            }
            record.setId(id);
            return record;
          }
        });
  }

  /**
   * Inserts the workspace, maps the pooled database, and marks the database
   * active and unavailable in one transaction. Returns null (rolled back) if any
   * step fails.
   */
  public static Workspace saveAndAllocateDatabase(Workspace record, long poolDatabaseId) {
    if (record == null || record.getId() > 0 || poolDatabaseId < 1) {
      throw new IllegalArgumentException("A new workspace and a pool database id are required");
    }
    return WorkspaceContextManager.withoutWorkspace(() -> {
      try (Connection connection = DB.getConnection();
          AutoStartTransaction a = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {
        long id = DB.INSERT().INTO(TABLE_NAME)
            .FIELD("workspace_unique_id", StringUtils.trimToNull(record.getUniqueId()))
            .FIELD("name", StringUtils.trimToNull(record.getName()))
            .FIELD("site_url", StringUtils.trimToNull(record.getSiteUrl()))
            .FIELD("canonical_domain", StringUtils.trimToNull(record.getCanonicalDomain()))
            .FIELD("file_root", StringUtils.trimToNull(record.getFileRoot()))
            .FIELD("active", record.isActive())
            .FIELD("show_in_catalog", record.isShowInCatalog())
            .FIELD("department", StringUtils.trimToNull(record.getDepartment()))
            .FIELD("description", StringUtils.trimToNull(record.getDescription()))
            .FIELD("owner", record.getOwnerId() > 0 ? record.getOwnerId() : null)
            .execute(connection);
        if (id < 1) {
          LOG.error("The insert failed for workspace: " + record.getName());
          return null;
        }
        if (DB.INSERT().INTO("workspace_domains")
            .FIELD("workspace_id", id)
            .FIELD("host_pattern", record.getCanonicalDomain())
            .FIELD("wildcard", false)
            .FIELD("active", true)
            .execute(connection) < 1) {
          LOG.error("Unable to add domain " + record.getCanonicalDomain() + " for workspace " + id);
          return null;
        }
        if (DB.INSERT().INTO("workspace_data_sources")
            .FIELD("workspace_id", id)
            .FIELD("pool_database_id", poolDatabaseId)
            .execute(connection) < 0) {
          LOG.error("Unable to map database " + poolDatabaseId + " to workspace " + id);
          return null;
        }
        if (!TenantDataSourceDatabaseRepository.allocate(connection, poolDatabaseId)) {
          LOG.error("Unable to allocate database " + poolDatabaseId + " for workspace " + id);
          return null;
        }
        
        // Seed the database... which needs a different connection, so it is commented out for now.
        // if (!TenantSeedLoader.seedWorkspaceDatabase(tenantConnection??, id)) {
        //   LOG.error("Unable to seed database for workspace " + id);
        //   return null;
        // }

        // Add the owner to Workspace Access Grants
        if (record.getOwnerId() > 0) {
          if (DB.INSERT().INTO("workspace_access_grants")
              .FIELD("workspace_id", id)
              .FIELD("user_id", record.getOwnerId())
              .FIELD("active", true)
              .FIELD("roles",
                  JsonCommand.toJsonArray(new String[] { "community-manager", "content-manager", "data-manager" }),
                  CastType.JSONB)
              .execute(connection) < 0) {
            LOG.error("Unable to grant access to user " + record.getOwnerId() + " for workspace " + id);
            return null;
          }
        }
        
        // Finish the transaction
        transaction.commit();
        record.setId(id);
        record.setPoolDatabaseId(poolDatabaseId);

        // Register the data source
        ConnectionPool.registerConfiguredTenantDataSource(TenantDataSourceConfigurationStore.findByWorkspaceId(id));
        return record;
      } catch (SQLException se) {
        LOG.error("SQLException: " + se.getMessage());
        return null;
      }
    });
  }

  public static boolean saveDataSourceMapping(long workspaceId, long poolDatabaseId) {
    if (workspaceId < 1 || poolDatabaseId < 1) {
      throw new IllegalArgumentException("Workspace and pool database ids must be greater than zero");
    }
    return WorkspaceContextManager.withoutWorkspace(() -> {
      Long existingWorkspaceId = DB.SELECT("workspace_id")
          .FROM("workspace_data_sources")
          .WHERE("workspace_id = ?", workspaceId)
          .returnRecord(resultSet -> resultSet.getLong("workspace_id"));
      if (existingWorkspaceId != null) {
        return DB.UPDATE("workspace_data_sources")
            .SET("pool_database_id", poolDatabaseId)
            .WHERE("workspace_id = ?", workspaceId)
            .execute();
      }
      return DB.INSERT().INTO("workspace_data_sources")
          .FIELD("workspace_id", workspaceId)
          .FIELD("pool_database_id", poolDatabaseId)
          .execute() > -1;
    });
  }
}
