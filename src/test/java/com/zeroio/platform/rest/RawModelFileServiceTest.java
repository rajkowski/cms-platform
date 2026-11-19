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
package com.zeroio.platform.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;

class RawModelFileServiceTest {

  @TempDir
  Path modelRoot;

  private ServiceContext context;
  private HttpServletRequest request;
  private Part part;
  private RawModelFileService service;

  @BeforeEach
  void setUp() throws Exception {
    context = mock(ServiceContext.class);
    request = mock(HttpServletRequest.class);
    part = mock(Part.class);
    service = new RawModelFileService();
    when(context.hasRole("admin")).thenReturn(true);
    when(context.getParameter("folder")).thenReturn("model1");
    when(context.getRequest()).thenReturn(request);
    when(request.getPart("file")).thenReturn(part);
    when(part.getSubmittedFileName()).thenReturn("model.onnx");
    when(part.getSize()).thenReturn(4L);
    when(part.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void validatesFolderNames() {
    assertTrue(RawModelFileService.isValidFolder("model1"));
    assertFalse(RawModelFileService.isValidFolder("Model1"));
    assertFalse(RawModelFileService.isValidFolder("model-1"));
    assertFalse(RawModelFileService.isValidFolder("../model1"));
  }

  @Test
  void requiresAdminRole() {
    when(context.hasRole("admin")).thenReturn(false);
    assertEquals(403, service.post(context).getStatus());
  }

  @Test
  void createsFolderAndStoresAllowedFile() throws Exception {
    ServiceResponse response = post();

    assertEquals(200, response.getStatus());
    Map<?, ?> data = (Map<?, ?>) response.getData();
    assertEquals("ok", data.get("status"));
    assertEquals("model1", data.get("folder"));
    assertEquals("model.onnx", data.get("filename"));
    assertEquals("data", Files.readString(modelRoot.resolve("model1/model.onnx")));
  }

  @Test
  void overwritesExistingFile() throws Exception {
    Files.createDirectories(modelRoot.resolve("model1"));
    Files.writeString(modelRoot.resolve("model1/model.onnx"), "old");

    assertEquals(200, post().getStatus());
    assertEquals("data", Files.readString(modelRoot.resolve("model1/model.onnx")));
  }

  @Test
  void rejectsUnsupportedFilename() {
    when(part.getSubmittedFileName()).thenReturn("../model.onnx");
    assertEquals(400, post().getStatus());
  }

  @Test
  void rejectsInvalidFolderBeforeReadingConfiguration() {
    when(context.getParameter("folder")).thenReturn("model/other");
    try (MockedStatic<FileSystemCommand> fileSystem = mockStatic(FileSystemCommand.class)) {
      ServiceResponse response = service.post(context);

      assertEquals(400, response.getStatus());
      assertEquals("A valid folder is required using only lowercase letters and numbers",
          response.getError().get("title"));
      fileSystem.verifyNoInteractions();
    }
  }

  private ServiceResponse post() {
    try (MockedStatic<FileSystemCommand> fileSystem = mockStatic(FileSystemCommand.class)) {
      fileSystem.when(() -> FileSystemCommand.getFileServerModelsPath("model1"))
          .thenReturn(modelRoot.resolve("model1").toFile());
      return service.post(context);
    }
  }
}
