/*
 * Copyright 2026 Matt Rajkowski
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

package com.simisinc.platform.application.cms;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.Content;

/**
 * Resolves content directives in HTML content by replacing ${uniqueId:...} patterns
 * with the actual content from referenced content blocks
 *
 * @author matt rajkowski
 * @created 2/7/26 12:00 PM
 */
public class ResolveContentDirectivesCommand {

  private static Log LOG = LogFactory.getLog(ResolveContentDirectivesCommand.class);

  // Pattern to match ${uniqueId:example-include}
  private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("\\$\\{uniqueId:([^}]+)\\}");
  
  // Maximum recursion depth to prevent infinite loops
  private static final int MAX_DEPTH = 10;

  /**
   * Resolves all content directives in the given HTML string
   *
   * @param html the HTML content containing directives
   * @return the HTML with all directives resolved
   */
  public static String resolveDirectives(String html) {
    if (StringUtils.isBlank(html)) {
      return html;
    }
    
    // Track visited uniqueIds to detect circular references
    Set<String> visitedIds = new HashSet<>();
    
    return resolveDirectives(html, visitedIds, 0);
  }

  /**
   * Recursively resolves content directives with circular reference detection
   *
   * @param html the HTML content containing directives
   * @param visitedIds set of uniqueIds already visited in this resolution chain
   * @param depth current recursion depth
   * @return the HTML with directives resolved
   */
  private static String resolveDirectives(String html, Set<String> visitedIds, int depth) {
    // Check recursion depth limit
    if (depth >= MAX_DEPTH) {
      LOG.warn("Maximum recursion depth (" + MAX_DEPTH + ") reached while resolving content directives");
      return html;
    }

    // Find all directive patterns
    Matcher matcher = DIRECTIVE_PATTERN.matcher(html);
    
    // If no directives found, return as-is
    if (!matcher.find()) {
      return html;
    }
    
    // Reset matcher to start from beginning
    matcher.reset();
    
    StringBuffer result = new StringBuffer();
    
    while (matcher.find()) {
      String uniqueId = matcher.group(1).trim();
      
      // Check for circular reference
      if (visitedIds.contains(uniqueId)) {
        LOG.warn("Circular reference detected for uniqueId: " + uniqueId);
        // Keep the directive as-is to show the circular reference
        matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
        continue;
      }
      
      // Load the referenced content
      Content referencedContent = LoadContentCommand.loadContentByUniqueId(uniqueId);
      
      if (referencedContent == null) {
        LOG.debug("Content not found for uniqueId: " + uniqueId + ", keeping directive as-is");
        // Keep directive as-is if content not found
        matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
      } else {
        // Get the content to embed (prefer published content)
        String contentToEmbed = referencedContent.getContent();
        
        if (StringUtils.isBlank(contentToEmbed)) {
          LOG.debug("Content is empty for uniqueId: " + uniqueId + ", keeping directive as-is");
          // Keep directive as-is if content is empty
          matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
        } else {
          // Add this uniqueId to visited set for circular reference detection
          Set<String> newVisitedIds = new HashSet<>(visitedIds);
          newVisitedIds.add(uniqueId);
          
          // Recursively resolve directives in the embedded content
          String resolvedContent = resolveDirectives(contentToEmbed, newVisitedIds, depth + 1);
          
          // Replace the directive with the resolved content
          matcher.appendReplacement(result, Matcher.quoteReplacement(resolvedContent));
        }
      }
    }
    
    matcher.appendTail(result);
    
    return result.toString();
  }
}
