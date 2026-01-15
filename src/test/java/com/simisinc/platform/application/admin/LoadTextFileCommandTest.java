/*
 * Copyright 2025 Matt Rajkowski (https://github.com/rajkowski)
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

package com.simisinc.platform.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.infrastructure.cache.CacheManager;

/**
 * Tests for LoadTextFileCommand
 *
 * @author test
 * @created 12/16/2025
 */
class LoadTextFileCommandTest {

  @TempDir
  private Path tempDir;

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

  private Dataset dataset;

  @BeforeEach
  void setUp() {
    dataset = new Dataset();
    dataset.setFileServerPath("test/data.txt");
    sitePropertyListCache = Caffeine.newBuilder().build(LoadTextFileCommandTest::findByPrefix);
  }

  @Test
  void testLoadSomeBytes() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      // Mock FileSystemCommand to return our test file
      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        List<String[]> result = LoadTextFileCommand.loadSomeBytes(dataset);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0));
        assertEquals(1, result.get(0).length);
        assertNotNull(result.get(0)[0]);
      }
    }
  }

  @Test
  void testLoadBytes() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 16);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0)[0]);
      }
    }
  }

  @Test
  void testLoadBytesWithLargeBuffer() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        // Request more bytes than file contains
        List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 10000);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0)[0]);
      }
    }
  }

  @Test
  void testLoadBytesWithZeroBytes() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 0);

        assertNotNull(result);
        assertEquals(1, result.size());
      }
    }
  }

  @Test
  void testLoadBytesFileNotExists() throws DataException {
    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(new File("/nonexistent/path/file.txt"));

      List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 1024);

      assertNull(result);
    }
  }

  @Test
  void testLoadBytesThrowsExceptionOnIOError() throws DataException {
    // Create a directory reference instead of a file to cause an error
    File notAFile = tempDir.toFile();

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(notAFile);

      // Should throw exception when trying to read a directory as a file
      assertThrows(DataException.class, () -> {
        LoadTextFileCommand.loadBytes(dataset, 1024);
      });
    }
  }

  @Test
  void testLoadLines() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        List<String[]> result = LoadTextFileCommand.loadLines(dataset, 3);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Header", result.get(0)[0]);
        assertEquals("Value 1", result.get(1)[0]);
        assertEquals("Value 2", result.get(2)[0]);
      }
    }
  }

  @Test
  void testLoadLinesWithMoreLinesThanFile() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        // Request more lines than file contains
        List<String[]> result = LoadTextFileCommand.loadLines(dataset, 100);

        assertNotNull(result);
        assertEquals(3, result.size());
      }
    }
  }

  @Test
  void testLoadLinesWithZeroLines() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        List<String[]> result = LoadTextFileCommand.loadLines(dataset, 0);

        assertNotNull(result);
        assertEquals(0, result.size());
      }
    }
  }

  @Test
  void testLoadLinesFileNotExists() throws DataException {
    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(new File("/nonexistent/path/file.txt"));

      List<String[]> result = LoadTextFileCommand.loadLines(dataset, 10);

      assertNull(result);
    }
  }

  @Test
  void testLoadLinesThrowsDataException() throws DataException {
    // Create a directory reference instead of a file to cause an error
    File notAFile = tempDir.toFile();

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(notAFile);

      // Should throw DataException when file is actually a directory
      assertThrows(DataException.class, () -> {
        LoadTextFileCommand.loadLines(dataset, 10);
      });
    }
  }

  @Test
  void testLoadBytesPreservesContent() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 100);

        assertNotNull(result);
        String loadedContent = result.get(0)[0];
        assertTrue(loadedContent.contains("Header"));
        assertTrue(loadedContent.contains("Value 1"));
      }
    }
  }

  @Test
  void testLoadLinesPreservesLineContent() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        List<String[]> result = LoadTextFileCommand.loadLines(dataset, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Header", result.get(0)[0]);
        assertEquals("Value 1", result.get(1)[0]);
      }
    }
  }

  @Test
  void testLoadBytesReturnsRowArray() throws DataException, IOException {
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);
      File testResourcePath = FileSystemCommand.getFileServerConfigPath("src", "test", "resources");
      File testFile = new File(testResourcePath, "simple-list.csv");

      try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
        fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
            .thenReturn(testFile);

        List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 100);

        assertNotNull(result);
        assertTrue(result.size() > 0);
        String[] row = result.get(0);
        assertNotNull(row);
        assertEquals(1, row.length);
        assertNotNull(row[0]);
      }
    }
  }
}
