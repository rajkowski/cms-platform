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
package com.zeroio.platform.application.cms;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;

/**
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
class ContentHtmlCommandTest {

  @Test
  void replaceDiagramTokenRendersHtmlWithLabelWhenFileNotFound() {
    // When no DB is available the file lookup returns null; the fallback shows the label
    String content = "<p>Before</p>${diagram:confluence-585768776;my diagram v2.drawio}<p>After</p>";

    try (MockedStatic<FileItemRepository> mockRepo = mockStatic(FileItemRepository.class)) {
      mockRepo.when(() -> FileItemRepository.findByWebPath("confluence-585768776")).thenReturn(null);

      String html = DiagramHtmlCommand.replaceDiagramTokens(content);

      Assertions.assertTrue(html.contains("class=\"drawio-diagram\""));
      Assertions.assertTrue(html.contains("my diagram v2.drawio"));
      Assertions.assertFalse(html.contains("${diagram:"));
    }
  }

  @Test
  void replaceDiagramTokenLeavesUnrelatedHtmlUnchanged() {
    String content = "<p>No diagrams here</p>";

    String html = DiagramHtmlCommand.replaceDiagramTokens(content);

    Assertions.assertEquals(content, html);
  }

  @Test
  void replaceDiagramTokenPrefersExplicitLabelOverStoredTitle() {
    String content = "${diagram:confluence-585768776;my diagram v2.drawio}";
    FileItem fileItem = mock(FileItem.class);

    when(fileItem.getTitle()).thenReturn("Stored Diagram Title");
    when(fileItem.getUrl()).thenReturn("confluence-585768776/plan%20v2.drawio");

    try (MockedStatic<FileItemRepository> mockRepo = mockStatic(FileItemRepository.class)) {
      mockRepo.when(() -> FileItemRepository.findByWebPath("confluence-585768776")).thenReturn(fileItem);

      String html = DiagramHtmlCommand.replaceDiagramTokens(content);

      Assertions.assertTrue(html.contains("my diagram v2.drawio"));
      Assertions.assertFalse(html.contains("Stored Diagram Title"));
      Assertions.assertTrue(html.contains("<iframe"));
      Assertions.assertTrue(html.contains("embedded=true"));
      Assertions.assertTrue(html.contains("/assets/drawio/confluence-585768776/plan%20v2.drawio"));
      Assertions.assertTrue(html.contains("/assets/file/confluence-585768776/plan%20v2.drawio"));
    }
  }

  @Test
  void replaceDiagramTokensRendersMultipleDiagrams() {
    String content = "${diagram:first-path;First Diagram}<p>Between</p>${diagram:second-path}";

    try (MockedStatic<FileItemRepository> mockRepo = mockStatic(FileItemRepository.class)) {
      mockRepo.when(() -> FileItemRepository.findByWebPath("first-path")).thenReturn(null);
      mockRepo.when(() -> FileItemRepository.findByWebPath("second-path")).thenReturn(null);

      String html = DiagramHtmlCommand.replaceDiagramTokens(content);

      Assertions.assertEquals(2, html.split("class=\"drawio-diagram\"", -1).length - 1);
      Assertions.assertTrue(html.contains("First Diagram"));
      Assertions.assertTrue(html.contains("second-path"));
      Assertions.assertFalse(html.contains("${diagram:"));
    }
  }

  @Test
  void replaceDiagramTokenRendersEmbeddedViewerAndKeepsDiagramLinks() {
    String content = "${diagram:confluence-585768776;my diagram v2.drawio}";
    FileItem fileItem = mock(FileItem.class);

    when(fileItem.getTitle()).thenReturn("my diagram v2");
    when(fileItem.getUrl()).thenReturn("confluence-585768776/plan%20v2.drawio");

    try (MockedStatic<FileItemRepository> mockRepo = mockStatic(FileItemRepository.class)) {
      mockRepo.when(() -> FileItemRepository.findByWebPath("confluence-585768776")).thenReturn(fileItem);

      String html = DiagramHtmlCommand.replaceDiagramTokens(content);

      Assertions.assertTrue(html.contains("<iframe"));
      Assertions.assertTrue(html.contains("embedded=true"));
      Assertions.assertTrue(html.contains("/assets/drawio/confluence-585768776/plan%20v2.drawio"));
      Assertions.assertTrue(html.contains("/assets/file/confluence-585768776/plan%20v2.drawio"));
    }
  }
}