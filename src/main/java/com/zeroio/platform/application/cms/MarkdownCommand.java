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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Methods for working with markdown
 *
 * @author matt rajkowski
 * @created 12/21/2021 3:48 PM
 */
public class MarkdownCommand {

  private static Log LOG = LogFactory.getLog(MarkdownCommand.class);

  /**
   * Turns markdown into HTML
   *
   * @param markdown the markdown text to output as HTML
   * @return
   */
  public static String html(String markdown) {
    return html(markdown, null);
  }

  public static String html(String markdown, String wikiLinkPrefix) {
    if (StringUtils.isBlank(markdown)) {
      return "";
    }

    List<Extension> extensions = List.of(
        StrikethroughExtension.create(),
        TablesExtension.create(),
        TaskListItemsExtension.create(),
        InsExtension.create(),
        HeadingAnchorExtension.create(),
        WikiParserExtension.create(wikiLinkPrefix));

    Parser parser = Parser.builder()
        .extensions(extensions)
        .build();

    HtmlRenderer renderer = HtmlRenderer.builder()
        .extensions(extensions)
        .nodeRendererFactory(MermaidNodeRenderer::new)
        .softbreak("<br />\n")
        .sanitizeUrls(true)
        .escapeHtml(true)
        .build();

    return renderer.render(parser.parse(markdown));
  }

}
