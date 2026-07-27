/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

import org.apache.commons.lang3.StringUtils;

import com.zeroio.platform.application.cms.EditorDiagramsCommand;
import com.zeroio.platform.application.cms.EditorIconsCommand;
import com.zeroio.platform.application.cms.EditorInlineContentCommand;

/**
 * Methods for working with TinyMCE content
 *
 * @author matt rajkowski
 * @created 3/2/20 10:00 PM
 */
public class TinyMceCommand {

  /**
   * Prepares the content HTML for the HTML content editor by converting tokens,
   * content block references, and HTML elements to editable span tags.
   *
   * @param contentHtml the original content HTML
   * @return the content HTML prepared for the editor
   */
  public static String prepareContentForEditor(String contentHtml) {
    if (StringUtils.isBlank(contentHtml)) {
      return contentHtml;
    }
    // Convert diagram tokens to span tags for editing
    contentHtml = EditorDiagramsCommand.convertDiagramTokensToSpans(contentHtml);
    // Convert plain text content block references to span tags for editing
    contentHtml = EditorInlineContentCommand.convertContentBlockTextToSpans(contentHtml);
    // Swap the icon tags
    contentHtml = EditorIconsCommand.switchIconTagsInContent(contentHtml, "i", "span", false);
    return contentHtml;
  }

  // Replace icon tags from Editor with FontAwesome tags
  public static String updateContentFromEditor(String contentHtml) {
    if (StringUtils.isBlank(contentHtml)) {
      return contentHtml;
    }
    contentHtml = EditorDiagramsCommand.convertDiagramSpansToTokens(contentHtml);
    // Convert content block span tags back to plain text references
    contentHtml = EditorInlineContentCommand.convertContentBlockSpansToText(contentHtml);
    // Swap the icon tags
    contentHtml = EditorIconsCommand.restoreIconTagsInContent(contentHtml);
    return contentHtml;
  }
}
