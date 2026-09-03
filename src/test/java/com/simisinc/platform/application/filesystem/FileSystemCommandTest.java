/*
 * Copyright 2024-2026 Matt Rajkowski (https://github.com/rajkowski)
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
package com.simisinc.platform.application.filesystem;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.TenantRegistry;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

/**
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class FileSystemCommandTest {

  private static List<SiteProperty> findByPrefix(String uniqueId) {
    List<SiteProperty> systemPropertyList = new ArrayList<>();
    {
      SiteProperty siteProperty = new SiteProperty();
      siteProperty.setLabel("Config Path");
      siteProperty.setName("system.configpath");
      siteProperty.setValue(".");
      siteProperty.setId(1);
      systemPropertyList.add(siteProperty);
    }
    {
      SiteProperty siteProperty = new SiteProperty();
      siteProperty.setLabel("File Path");
      siteProperty.setName("system.filepath");
      siteProperty.setValue(".");
      siteProperty.setId(1);
      systemPropertyList.add(siteProperty);
    }
    return systemPropertyList;
  }

  private LoadingCache<String, List<SiteProperty>> sitePropertyListCache;

  @TempDir
  Path temporaryDirectory;

  /**
   * FileSystemCommand memoizes its resolved paths in private static fields, and also prefers a
   * CMS_PATH environment variable over the (mocked) site property cache. Force the cached fields
   * back to the value the mocked site properties would resolve to, so tests behave the same
   * whether or not CMS_PATH happens to be set on the host running the tests.
   */
  private static void setStaticField(String fieldName, String value) throws Exception {
    Field field = FileSystemCommand.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }

  @BeforeEach
  public void init() throws Exception {
    WorkspaceContextManager.clear();
    TenantRegistry tenantRegistry = new TenantRegistry();
    tenantRegistry.register("1", mock(DataSource.class));
    DB.setTenantRegistry(tenantRegistry);
    sitePropertyListCache = Caffeine.newBuilder().build(FileSystemCommandTest::findByPrefix);
    setStaticField("configPath", "." + File.separator);
    setStaticField("filesPath", "." + File.separator);
    setStaticField("staticSitePath", null);
  }

  @Test
  void testIsModified() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File file = new File(testResourcePath, "simplelogger.properties");
      Assertions.assertFalse(FileSystemCommand.isModified(file, file.lastModified()));
      Assertions.assertTrue(FileSystemCommand.isModified(file, file.lastModified() - 100));
    }
  }

  @Test
  void testGenerateUniqueFilename() {
    long now = System.currentTimeMillis();
    long id = 321;
    String uniqueId = FileSystemCommand.generateUniqueFilename(id);
    Assertions.assertTrue(uniqueId.endsWith("-" + id));
    long timestamp = Long.parseLong(uniqueId.substring(0, uniqueId.indexOf("-")));
    Assertions.assertTrue(timestamp >= now);
  }

  @Test
  void testGetFileServerConfigPath() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      Assertions.assertTrue(testResourcePath.isDirectory());
      Assertions.assertTrue(testResourcePath.exists());

      File properties = new File(testResourcePath, "simplelogger.properties");
      Assertions.assertTrue(properties.isFile());
      Assertions.assertTrue(properties.exists());
    }
  }

  @Test
  void testGetFileServerConfigPathValue() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      String pathValue = FileSystemCommand.getFileServerConfigPathValue();
      Assertions.assertEquals("." + File.separator, pathValue);

      File path = new File(pathValue);
      Assertions.assertTrue(path.isDirectory());
      Assertions.assertTrue(path.exists());
    }
  }

  @Test
  void testGetFileServerRootPath() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      File testResourcePath = FileSystemCommand.getFileServerRootPath("src", "test", "resources");
      Assertions.assertTrue(testResourcePath.isDirectory());
      Assertions.assertTrue(testResourcePath.exists());

      File properties = new File(testResourcePath, "simplelogger.properties");
      Assertions.assertTrue(properties.isFile());
      Assertions.assertTrue(properties.exists());
    }
  }

  @Test
  void testGetFileServerRootPathValue() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      String pathValue = FileSystemCommand.getFileServerRootPathValue();
      Assertions.assertEquals("." + File.separator, pathValue);

      File path = new File(pathValue);
      Assertions.assertTrue(path.isDirectory());
      Assertions.assertTrue(path.exists());
    }
  }

  @Test
  void workspaceContextUsesItsConfiguredFileRoot() throws Exception {
    Path workspaceRoot = Files.createDirectory(temporaryDirectory.resolve("workspace-one"));
    WorkspaceContextManager.activate(1, "one.example.com", workspaceRoot.toString());

    Assertions.assertEquals(workspaceRoot.toAbsolutePath() + File.separator, FileSystemCommand.getFileServerRootPathValue());
    Assertions.assertEquals(workspaceRoot.resolve("uploads").toFile(), FileSystemCommand.getFileServerRootPath("uploads"));
  }

  @Test
  void workspaceContextRejectsMissingOrEscapingFileRoots() {
    WorkspaceContextManager.activate(1, "one.example.com", temporaryDirectory.resolve("missing").toString());
    Assertions.assertNull(FileSystemCommand.getFileServerRootPath());

    WorkspaceContextManager.activate(1, "one.example.com", temporaryDirectory.toString());
    Assertions.assertNull(FileSystemCommand.getFileServerRootPath("..", "other-workspace"));
  }

  @Test
  void testLoadFileToList() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      Assertions.assertTrue(testResourcePath.isDirectory());
      Assertions.assertTrue(testResourcePath.exists());

      File properties = new File(testResourcePath, "simple-list.csv");
      Assertions.assertTrue(properties.isFile());
      Assertions.assertTrue(properties.exists());

      List<String> list = FileSystemCommand.loadFileToList(properties);
      Assertions.assertFalse(list.isEmpty());
      Assertions.assertEquals(2, list.size());
    }
  }

  @Test
  void testGetFileChecksum() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File properties = new File(testResourcePath, "simple-list.csv");
      String checksum = FileSystemCommand.getFileChecksum(properties);
      Assertions.assertNotNull(checksum);
      Assertions.assertTrue(checksum.startsWith("SHA-512;"));
      Assertions.assertEquals(136, checksum.length());
    }
  }

  @Test
  void testGetFileChecksumMissingFile() {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File missing = new File("nonexistent-file-xyz.tmp");
      Assertions.assertNull(FileSystemCommand.getFileChecksum(missing));
    }
  }
}
