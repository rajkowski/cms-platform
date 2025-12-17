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
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.datasets.Dataset;

/**
 * Tests for LoadTextFileCommand
 *
 * @author test
 * @created 12/16/2025
 */
class LoadTextFileCommandTest {

  @TempDir
  private Path tempDir;

  private Dataset dataset;

  @BeforeEach
  void setUp() {
    dataset = new Dataset();
    dataset.setFileServerPath("test/data.txt");
  }

  @Test
  void testLoadSomeBytes() throws DataException, IOException {
    // Create a test file with known content
    String testContent = "Line 1\nLine 2\nLine 3\nThis is a longer line to test byte reading";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

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

  @Test
  void testLoadBytes() throws DataException, IOException {
    String testContent = "This is test content for byte loading";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 16);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertNotNull(result.get(0)[0]);
    }
  }

  @Test
  void testLoadBytesWithLargeBuffer() throws DataException, IOException {
    String testContent = "Short content";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

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

  @Test
  void testLoadBytesWithZeroBytes() throws DataException, IOException {
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), "Content".getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 0);

      assertNotNull(result);
      assertEquals(1, result.size());
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
    // Create a directory instead of a file to cause an error
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
    String testContent = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadLines(dataset, 3);

      assertNotNull(result);
      assertEquals(3, result.size());
      assertEquals("Line 1", result.get(0)[0]);
      assertEquals("Line 2", result.get(1)[0]);
      assertEquals("Line 3", result.get(2)[0]);
    }
  }

  @Test
  void testLoadLinesWithMoreLinesThanFile() throws DataException, IOException {
    String testContent = "Line 1\nLine 2\nLine 3";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      // Request more lines than file contains
      List<String[]> result = LoadTextFileCommand.loadLines(dataset, 100);

      assertNotNull(result);
      assertEquals(3, result.size());
    }
  }

  @Test
  void testLoadLinesWithZeroLines() throws DataException, IOException {
    String testContent = "Line 1\nLine 2\nLine 3";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadLines(dataset, 0);

      assertNotNull(result);
      assertEquals(0, result.size());
    }
  }

  @Test
  void testLoadLinesWithEmptyFile() throws DataException, IOException {
    File testFile = tempDir.resolve("empty.txt").toFile();
    Files.write(testFile.toPath(), "".getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadLines(dataset, 10);

      assertNotNull(result);
      assertEquals(0, result.size());
    }
  }

  @Test
  void testLoadLinesWithUTF8Content() throws DataException, IOException {
    String testContent = "Hello World\nGrüße\n日本語\nПривет";
    File testFile = tempDir.resolve("utf8.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadLines(dataset, 10);

      assertNotNull(result);
      assertEquals(4, result.size());
      assertEquals("Hello World", result.get(0)[0]);
      assertEquals("Grüße", result.get(1)[0]);
      assertEquals("日本語", result.get(2)[0]);
      assertEquals("Привет", result.get(3)[0]);
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
    // Create a directory instead of a file to cause an error
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
  void testLoadBytesWithMalformedUTF8() throws DataException, IOException {
    // Create a file with some invalid UTF-8 sequences
    byte[] invalidUTF8 = new byte[] { (byte) 0xFF, (byte) 0xFE, 0x48, 0x69 };
    File testFile = tempDir.resolve("malformed.txt").toFile();
    Files.write(testFile.toPath(), invalidUTF8);

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      // Should handle malformed input gracefully (with replacement characters)
      List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 1024);

      assertNotNull(result);
      assertEquals(1, result.size());
      assertNotNull(result.get(0)[0]);
    }
  }

  @Test
  void testLoadBytesPreservesContent() throws DataException, IOException {
    String testContent = "First line\nSecond line";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadBytes(dataset, 100);

      assertNotNull(result);
      String loadedContent = result.get(0)[0];
      assertTrue(loadedContent.contains("First line"));
      assertTrue(loadedContent.contains("Second line"));
    }
  }

  @Test
  void testLoadLinesPreservesLineContent() throws DataException, IOException {
    String testContent = "Important data\nMore important data";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadLines(dataset, 2);

      assertNotNull(result);
      assertEquals(2, result.size());
      assertEquals("Important data", result.get(0)[0]);
      assertEquals("More important data", result.get(1)[0]);
    }
  }

  @Test
  void testLoadBytesReturnsRowArray() throws DataException, IOException {
    String testContent = "Test content";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

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

  @Test
  void testLoadLinesWithLineBreakVariations() throws DataException, IOException {
    String testContent = "Line 1\r\nLine 2\rLine 3\nLine 4";
    File testFile = tempDir.resolve("data.txt").toFile();
    Files.write(testFile.toPath(), testContent.getBytes());

    try (MockedStatic<FileSystemCommand> fsCommand = mockStatic(FileSystemCommand.class)) {
      fsCommand.when(() -> FileSystemCommand.getFileServerRootPath(any()))
          .thenReturn(testFile);

      List<String[]> result = LoadTextFileCommand.loadLines(dataset, 10);

      assertNotNull(result);
      assertTrue(result.size() >= 3);
    }
  }
}
