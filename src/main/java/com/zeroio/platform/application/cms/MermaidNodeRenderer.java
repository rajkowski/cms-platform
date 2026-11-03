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

import java.util.Map;
import java.util.Set;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.CoreHtmlNodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

class MermaidNodeRenderer implements NodeRenderer {
  private final HtmlNodeRendererContext context;

  MermaidNodeRenderer(HtmlNodeRendererContext context) {
    this.context = context;
  }

  @Override
  public Set<Class<? extends Node>> getNodeTypes() {
    return Set.of(FencedCodeBlock.class);
  }

  @Override
  public void render(Node node) {
    FencedCodeBlock block = (FencedCodeBlock) node;
    if (!"mermaid".equals(block.getInfo())) {
      new CoreHtmlNodeRenderer(context).render(block);
      return;
    }

    HtmlWriter writer = context.getWriter();
    writer.tag("pre", Map.of("class", "mermaid"));
    writer.line();
    writer.raw(block.getLiteral().replace("&", "&amp;").replace("<", "&lt;"));
    writer.tag("/pre");
    writer.line();
  }
}