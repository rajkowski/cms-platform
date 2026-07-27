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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.simisinc.platform.application.cms.TinyMceCommand;

/**
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
class ContentEditorCommandTest {

  @Test
  void prepareContentForEditorConvertsContentBlockReference() {
    String content = "<p>Before</p>${uniqueId:sample-content}<p>After</p>";

    String html = TinyMceCommand.prepareContentForEditor(content);

    Assertions.assertTrue(html.contains("class=\"content-block-ref\""));
    Assertions.assertTrue(html.contains("data-uniqueid=\"sample-content\""));
    Assertions.assertTrue(html.contains("${uniqueId:sample-content}"));
  }

  @Test
  void updateContentFromEditorConvertsContentBlockSpanBackToText() {
    String content = "<p>Before</p><span class=\"content-block-ref\" contenteditable=\"false\" data-uniqueid=\"sample-content\">${uniqueId:sample-content}</span><p>After</p>";

    String html = TinyMceCommand.updateContentFromEditor(content);

    Assertions.assertEquals("<p>Before</p>${uniqueId:sample-content}<p>After</p>", html);
  }

  @Test
  void prepareContentForEditorConvertsDiagramTokenToSpan() {
    String content = "<p>Before</p>${diagram:confluence-585768776;my diagram v2.drawio}<p>After</p>";

    String html = TinyMceCommand.prepareContentForEditor(content);

    Assertions.assertTrue(html.contains("class=\"drawio-diagram-ref\""));
    Assertions.assertTrue(html.contains("contenteditable=\"false\""));
    Assertions.assertTrue(html.contains("data-webpath=\"confluence-585768776\""));
    Assertions.assertTrue(html.contains("data-label=\"my diagram v2.drawio\""));
    Assertions.assertTrue(html.contains("diagram: my diagram v2.drawio"));
    Assertions.assertFalse(html.contains("${diagram:"));
  }

  @Test
  void prepareContentForEditorConvertsUnlabeledDiagramTokenToSpan() {
    String content = "<p>Before</p>${diagram:confluence-585768776}<p>After</p>";

    String html = TinyMceCommand.prepareContentForEditor(content);

    Assertions.assertTrue(html.contains("class=\"drawio-diagram-ref\""));
    Assertions.assertTrue(html.contains("data-webpath=\"confluence-585768776\""));
    Assertions.assertTrue(html.contains("data-label=\"\""));
    Assertions.assertTrue(html.contains("diagram: confluence-585768776"));
  }

  @Test
  void updateContentFromEditorConvertsDiagramSpanBackToToken() {
    String content = "<p>Before</p><span class=\"drawio-diagram-ref\" contenteditable=\"false\" data-webpath=\"confluence-585768776\" data-label=\"my diagram v2.drawio\" style=\"background-color: #fff3cd;\">diagram: my diagram v2.drawio</span><p>After</p>";

    String html = TinyMceCommand.updateContentFromEditor(content);

    Assertions.assertEquals("<p>Before</p>${diagram:confluence-585768776;my diagram v2.drawio}<p>After</p>", html);
  }

  @Test
  void updateContentFromEditorConvertsUnlabeledDiagramSpanBackToToken() {
    String content = "<p>Before</p><span class=\"drawio-diagram-ref\" contenteditable=\"false\" data-webpath=\"confluence-585768776\" data-label=\"\" style=\"background-color: #fff3cd;\">diagram: confluence-585768776</span><p>After</p>";

    String html = TinyMceCommand.updateContentFromEditor(content);

    Assertions.assertEquals("<p>Before</p>${diagram:confluence-585768776}<p>After</p>", html);
  }

  @Test
  void prepareContentForEditorConvertsMultipleDiagramTokensToSpans() {
    String content = "${diagram:first-path;First}${diagram:second-path}";

    String html = TinyMceCommand.prepareContentForEditor(content);

    Assertions.assertEquals(2, html.split("drawio-diagram-ref", -1).length - 1);
    Assertions.assertTrue(html.contains("diagram: First"));
    Assertions.assertTrue(html.contains("diagram: second-path"));
  }
}