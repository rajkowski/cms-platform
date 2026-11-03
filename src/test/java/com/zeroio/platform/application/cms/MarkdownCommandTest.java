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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownCommandTest {

  @Test
  void html_withConfiguredExtensions_rendersExpectedMarkup() {
    String markdown = "~~removed~~\n\n"
        + "| Name | Value |\n| --- | --- |\n| alpha | beta |\n\n"
        + "- [x] done\n\n"
        + "++added++\n\n"
        + "[[Home]] and [external](https://example.com)";

    String html = MarkdownCommand.html(markdown, "/knowledge");

    assertTrue(html.contains("<del>removed</del>"));
    assertTrue(html.contains("<table>"));
    assertTrue(html.contains("type=\"checkbox\""));
    assertTrue(html.contains("checked"));
    assertTrue(html.contains("<ins>added</ins>"));
    assertTrue(html.contains("href=\"/knowledge/"), html);
    assertTrue(html.contains("href=\"https://example.com\" target=\"_blank\""));
  }

  @Test
  void html_blankMarkdown_returnsEmptyString() {
    assertEquals("", MarkdownCommand.html("  \n  "));
  }

  @Test
  void html_mermaidFence_rendersMermaidContainerWithoutCodeBlock() {
    String markdown = "```mermaid\n"
        + "graph TD\n"
        + "  A[Client] --> B[Load Balancer]\n"
        + "  B --> C[Server01]\n"
        + "  B --> D[Server02]\n"
        + "```";

    String html = MarkdownCommand.html(markdown);

    assertTrue(html.contains("<pre class=\"mermaid\">"), html);
    assertTrue(html.contains("graph TD\n  A[Client] --> B[Load Balancer]"), html);
    assertTrue(html.contains("B --> C[Server01]\n  B --> D[Server02]"), html);
    assertTrue(!html.contains("<code"), html);
    assertTrue(!html.contains("--&gt;"), html);
  }
}