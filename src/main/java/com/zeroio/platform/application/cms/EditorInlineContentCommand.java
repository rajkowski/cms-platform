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

/**
 * Provides methods to convert content block references to span tags and vice versa
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class EditorInlineContentCommand {

  /**
   * Convert plain text ${uniqueId:value} references to content block span tags
   * This is called when loading content into the editor
   */
  public static String convertContentBlockTextToSpans(String contentHtml) {
    if (!contentHtml.contains("${uniqueId:")) {
      return contentHtml;
    }

    // Find all ${uniqueId:value} references and convert them to span tags
    int refIdx = 0;
    while ((refIdx = contentHtml.indexOf("${uniqueId:", refIdx)) != -1) {
      // Find the end of the reference
      int refEnd = contentHtml.indexOf("}", refIdx);
      if (refEnd == -1) {
        break;
      }

      // Extract the unique ID
      String uniqueId = contentHtml.substring(refIdx + 11, refEnd); // 11 = length of '${uniqueId:'

      // Create the span tag
      String spanTag = "<span class=\"content-block-ref\" contenteditable=\"false\" data-uniqueid=\"" +
          uniqueId + "\" style=\"background-color: #e3f2fd; padding: 2px 6px; border-radius: 3px; " +
          "border: 1px solid #90caf9; display: inline-block; font-family: monospace; font-size: 0.9em;\">" +
          "${uniqueId:" + uniqueId + "}" +
          "</span>";

      // Replace the reference with the span tag
      contentHtml = contentHtml.substring(0, refIdx) + spanTag + contentHtml.substring(refEnd + 1);

      // Continue search from after the replacement
      refIdx = refIdx + spanTag.length();
    }

    return contentHtml;
  }

  /**
   * Convert content block span tags back to plain text ${uniqueId:value} references
   * This is called when saving content from the editor
   */
  public static String convertContentBlockSpansToText(String contentHtml) {
    if (!contentHtml.contains("content-block-ref")) {
      return contentHtml;
    }

    // Find all content-block-ref spans and convert them to plain text
    int spanIdx = 0;
    while ((spanIdx = contentHtml.indexOf("data-uniqueid=\"", spanIdx)) != -1) {
      // Find the unique ID value
      int uniqueIdStart = spanIdx + 15; // length of 'data-uniqueid="'
      int uniqueIdEnd = contentHtml.indexOf("\"", uniqueIdStart);
      if (uniqueIdEnd == -1) {
        break;
      }
      String uniqueId = contentHtml.substring(uniqueIdStart, uniqueIdEnd);

      // Find the start of this span tag
      int spanStart = contentHtml.lastIndexOf("<span", spanIdx);
      if (spanStart == -1) {
        break;
      }

      // Find the end of this span tag
      int spanEnd = contentHtml.indexOf("</span>", uniqueIdEnd);
      if (spanEnd == -1) {
        break;
      }
      spanEnd += 7; // length of '</span>'

      // Replace the entire span with the plain text reference
      String replacement = "${uniqueId:" + uniqueId + "}";
      contentHtml = contentHtml.substring(0, spanStart) + replacement + contentHtml.substring(spanEnd);

      // Continue search from after the replacement
      spanIdx = spanStart + replacement.length();
    }

    return contentHtml;
  }
}
