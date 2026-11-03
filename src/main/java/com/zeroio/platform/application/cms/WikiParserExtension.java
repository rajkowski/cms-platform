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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.Extension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.parser.PostProcessor;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Wiki extension to set link attributes and convert [[Page Name]] wiki links
 *
 * @author matt rajkowski
 * @created 2/25/20 7:00 PM
 */
public class WikiParserExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

  private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\[\\]]+)\\]\\]");

  private final String wikiLinkPrefix;

  private WikiParserExtension(String wikiLinkPrefix) {
    this.wikiLinkPrefix = wikiLinkPrefix;
  }

  public static Extension create() {
    return new WikiParserExtension(null);
  }

  public static Extension create(String wikiLinkPrefix) {
    return new WikiParserExtension(wikiLinkPrefix);
  }

  @Override
  public void extend(Parser.Builder builder) {
    if (wikiLinkPrefix != null) {
      builder.postProcessor(new WikiLinkPostProcessor(wikiLinkPrefix));
    }
  }

  @Override
  public void extend(HtmlRenderer.Builder builder) {
    builder.attributeProviderFactory(context -> new WikiAttributeProvider());
  }

  static class WikiAttributeProvider implements AttributeProvider {
    @Override
    public void setAttributes(Node node, String tagName, Map<String, String> attributes) {
      if (node instanceof Link) {
        String url = ((Link) node).getDestination();
        if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
          attributes.put("target", "_blank");
        }
      }
    }
  }

  static class WikiLinkPostProcessor implements PostProcessor {
    private final String prefix;

    WikiLinkPostProcessor(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public Node process(Node document) {
      document.accept(new AbstractVisitor() {
        // Bracket characters can be emitted as separate Text nodes, so merge them first
        @Override
        protected void visitChildren(Node parent) {
          Node child = parent.getFirstChild();
          while (child != null) {
            Node next = child.getNext();
            if (child instanceof Text && next instanceof Text) {
              Text merged = new Text(((Text) child).getLiteral() + ((Text) next).getLiteral());
              child.insertBefore(merged);
              child.unlink();
              next.unlink();
              next = merged.getNext();
              child = merged;
              if (next instanceof Text) {
                continue;
              }
            }
            child = child.getNext();
          }
          super.visitChildren(parent);
        }

        @Override
        public void visit(Text text) {
          String literal = text.getLiteral();
          if (literal == null || !literal.contains("[[")) {
            return;
          }
          Matcher matcher = WIKI_LINK.matcher(literal);
          Node insertAfter = text;
          int last = 0;
          boolean found = false;
          while (matcher.find()) {
            found = true;
            if (matcher.start() > last) {
              Text before = new Text(literal.substring(last, matcher.start()));
              insertAfter.insertAfter(before);
              insertAfter = before;
            }
            String name = matcher.group(1).trim();
            Link link = new Link(prefix + "/" + name.replaceAll("[ +/<>]", "-"), null);
            link.appendChild(new Text(name));
            insertAfter.insertAfter(link);
            insertAfter = link;
            last = matcher.end();
          }
          if (!found) {
            return;
          }
          if (last < literal.length()) {
            insertAfter.insertAfter(new Text(literal.substring(last)));
          }
          text.unlink();
        }
      });
      return document;
    }
  }
}
