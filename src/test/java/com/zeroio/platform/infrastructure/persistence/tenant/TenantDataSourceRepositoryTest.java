package com.zeroio.platform.infrastructure.persistence.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Base64;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.zeroio.platform.domain.model.tenant.TenantDataSourceDatabase;
import com.zeroio.platform.domain.model.tenant.TenantDataSourcePool;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

class TenantDataSourceRepositoryTest {

  private String previousSecretKey;

  @BeforeEach
  void setUp() throws Exception {
    previousSecretKey = System.getProperty("cms.secret.key");
    System.setProperty("cms.secret.key", Base64.getEncoder().encodeToString(new byte[32]));
    WorkspaceContextManager.clear();
    DB.setTenantRegistry(new TenantRegistry());
    JdbcDataSource source = new JdbcDataSource();
    source.setURL("jdbc:h2:mem:pool_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    source.setUser("sa");
    DB.setDataSource(source);
    try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("""
          CREATE TABLE data_source_pools (
            pool_id BIGSERIAL PRIMARY KEY,
            pool_unique_id VARCHAR(255) UNIQUE NOT NULL,
            ds_class_name VARCHAR(255) DEFAULT 'org.postgresql.ds.PGSimpleDataSource',
            driver_class_name VARCHAR(255), server_name VARCHAR(255) NOT NULL,
            username VARCHAR(255) NOT NULL, password VARCHAR(2048),
            port_number INT DEFAULT 5432, ssl BOOLEAN NOT NULL DEFAULT FALSE,
            auth_method VARCHAR(50), azure_tenant_id VARCHAR(255),
            azure_client_id VARCHAR(255), azure_client_secret VARCHAR(2048),
            max_connections INT NOT NULL DEFAULT 8)
          """);
      statement.execute("""
          CREATE TABLE pool_databases (
            pool_database_id BIGSERIAL PRIMARY KEY,
            pool_id BIGINT NOT NULL REFERENCES data_source_pools(pool_id) ON DELETE CASCADE,
            database_name VARCHAR(255) NOT NULL, active BOOLEAN NOT NULL DEFAULT FALSE,
            available BOOLEAN NOT NULL DEFAULT TRUE,
            created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
            modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP)
          """);
    }
    JdbcDataSource tenantSource = new JdbcDataSource();
    tenantSource.setURL("jdbc:h2:mem:empty_tenant_" + System.nanoTime());
    tenantSource.setUser("sa");
    DB.getTenantRegistry().register("1", tenantSource);
    WorkspaceContextManager.activate(1, "tenant.example.com");
  }

  @AfterEach
  void tearDown() {
    WorkspaceContextManager.clear();
    DB.setTenantRegistry(new TenantRegistry());
    if (previousSecretKey == null) {
      System.clearProperty("cms.secret.key");
    } else {
      System.setProperty("cms.secret.key", previousSecretKey);
    }
  }

  @Test
  void poolRoundTripsAllSettingsAndEncryptsSecretsInPrimaryDatabase() {
    TenantDataSourcePool pool = createPool();
    pool.setDriverClassName("org.postgresql.Driver");
    pool.setPassword("test-password");
    pool.setPortNumber(5433);
    pool.setSsl(true);
    pool.setAuthMethod("azure-sql-spn");
    pool.setAzureTenantId("test-tenant");
    pool.setAzureClientId("test-client");
    pool.setAzureClientSecret("test-secret");
    pool.setMaxConnections(12);
    assertSame(pool, TenantDataSourcePoolRepository.save(pool));
    assertTrue(pool.getId() > 0);

    TenantDataSourcePool loaded = TenantDataSourcePoolRepository.findByUniqueId("shared-pool");
    assertNotNull(loaded);
    assertEquals(pool.getId(), loaded.getId());
    assertEquals(pool.getDsClassName(), loaded.getDsClassName());
    assertEquals(pool.getDriverClassName(), loaded.getDriverClassName());
    assertEquals(pool.getServerName(), loaded.getServerName());
    assertEquals(pool.getUsername(), loaded.getUsername());
    assertEquals(pool.getPassword(), loaded.getPassword());
    assertEquals(5433, loaded.getPortNumber());
    assertTrue(loaded.isSsl());
    assertEquals(pool.getAuthMethod(), loaded.getAuthMethod());
    assertEquals(pool.getAzureTenantId(), loaded.getAzureTenantId());
    assertEquals(pool.getAzureClientId(), loaded.getAzureClientId());
    assertEquals(pool.getAzureClientSecret(), loaded.getAzureClientSecret());
    assertEquals(12, loaded.getMaxConnections());
    WorkspaceContextManager.withoutWorkspace(() -> {
      assertNotEquals("test-password", DB.SELECT("password").FROM("data_source_pools")
          .returnRecord(resultSet -> resultSet.getString("password")));
      assertNotEquals("test-secret", DB.SELECT("azure_client_secret").FROM("data_source_pools")
          .returnRecord(resultSet -> resultSet.getString("azure_client_secret")));
    });

    loaded.setServerName("replacement.example.com");
    loaded.setPassword("replacement-password");
    loaded.setAzureClientSecret(null);
    assertSame(loaded, TenantDataSourcePoolRepository.save(loaded));
    loaded = TenantDataSourcePoolRepository.findById(pool.getId());
    assertEquals("replacement.example.com", loaded.getServerName());
    assertEquals("replacement-password", loaded.getPassword());
    assertNull(loaded.getAzureClientSecret());
    assertEquals(1, TenantDataSourcePoolRepository.findAll().size());
    assertWorkspaceRestored();
  }

  @Test
  void databaseRoundTripsDefaultsUpdatesAndPoolLookupsInPrimaryDatabase() {
    TenantDataSourcePool pool = createPool();
    assertNotNull(TenantDataSourcePoolRepository.save(pool));
    TenantDataSourceDatabase database = new TenantDataSourceDatabase();
    assertEquals(-1, database.getId());
    assertEquals(-1, database.getPoolId());
    assertFalse(database.isActive());
    assertTrue(database.isAvailable());
    database.setPoolId(pool.getId());
    database.setDatabaseName("tenant_one");
    assertSame(database, TenantDataSourceDatabaseRepository.save(database));
    assertTrue(database.getId() > 0);
    assertNotNull(database.getCreated());
    assertNotNull(database.getModified());

    TenantDataSourceDatabase loaded = TenantDataSourceDatabaseRepository
        .findByPoolIdAndDatabaseName(pool.getId(), "tenant_one");
    assertNotNull(loaded);
    assertEquals(database.getId(), loaded.getId());
    assertEquals(pool.getId(), loaded.getPoolId());
    assertFalse(loaded.isActive());
    assertTrue(loaded.isAvailable());
    assertEquals(database.getCreated(), loaded.getCreated());
    assertEquals(database.getModified(), loaded.getModified());

    loaded.setDatabaseName("tenant_renamed");
    loaded.setActive(true);
    loaded.setAvailable(false);
    assertSame(loaded, TenantDataSourceDatabaseRepository.save(loaded));
    loaded = TenantDataSourceDatabaseRepository.findById(database.getId());
    assertEquals("tenant_renamed", loaded.getDatabaseName());
    assertTrue(loaded.isActive());
    assertFalse(loaded.isAvailable());
    assertEquals(database.getCreated(), loaded.getCreated());
    assertFalse(loaded.getModified().before(database.getModified()));
    assertEquals(1, TenantDataSourceDatabaseRepository.findAll().size());
    assertEquals(1, TenantDataSourceDatabaseRepository.findByPoolId(pool.getId()).size());
    assertTrue(TenantDataSourceDatabaseRepository.findByPoolId(pool.getId() + 1).isEmpty());
    assertNull(TenantDataSourceDatabaseRepository.findByPoolIdAndDatabaseName(pool.getId(), "tenant_one"));
    assertWorkspaceRestored();
  }

  @Test
  void defaultsAndMissingRecordsAreHandled() {
    TenantDataSourcePool pool = createPool();
    assertEquals(-1, pool.getId());
    assertEquals(5432, pool.getPortNumber());
    assertEquals(8, pool.getMaxConnections());
    assertFalse(pool.isSsl());
    assertNull(TenantDataSourcePoolRepository.save(null));
    assertNull(TenantDataSourceDatabaseRepository.save(null));
    assertNull(TenantDataSourcePoolRepository.findById(-1));
    assertNull(TenantDataSourcePoolRepository.findById(999));
    assertNull(TenantDataSourcePoolRepository.findByUniqueId(" "));
    assertNull(TenantDataSourcePoolRepository.findByUniqueId("missing"));
    assertNull(TenantDataSourceDatabaseRepository.findById(-1));
    assertNull(TenantDataSourceDatabaseRepository.findById(999));
    assertNull(TenantDataSourceDatabaseRepository.findByPoolIdAndDatabaseName(-1, "tenant"));
    assertNull(TenantDataSourceDatabaseRepository.findByPoolIdAndDatabaseName(1, " "));
    assertTrue(TenantDataSourceDatabaseRepository.findByPoolId(-1).isEmpty());
    assertTrue(TenantDataSourcePoolRepository.findAll().isEmpty());
    assertTrue(TenantDataSourceDatabaseRepository.findAll().isEmpty());
    assertWorkspaceRestored();
  }

  private static TenantDataSourcePool createPool() {
    TenantDataSourcePool pool = new TenantDataSourcePool();
    pool.setUniqueId("shared-pool");
    pool.setServerName("db.example.com");
    pool.setUsername("cms");
    return pool;
  }

  private static void assertWorkspaceRestored() {
    assertEquals("1", DB.getTenantId());
    assertEquals(1, WorkspaceContextManager.getCurrentContext().workspaceId());
  }
}