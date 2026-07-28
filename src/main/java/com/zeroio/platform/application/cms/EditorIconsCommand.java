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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides methods to convert icon tags to FontAwesome tags and vice versa
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class EditorIconsCommand {

  private static Log LOG = LogFactory.getLog(EditorIconsCommand.class);

  private static final String[] TINY_MCE_ICON_TAGS = new String[] { "span", "em" };
  private static final String[] FA_ICON_CSS = new String[] { "far", "fas", "fal", "fad", "fab", "fa" };

  /**
   * Replace icon tags from HTML Editor with FontAwesome tags
   *
   * @param contentHtml the content HTML to process
   * @param tag the tag to replace (e.g., "span" or "em")
   * @param newTag the new tag to use (e.g., "i")
   * @param fromTinyMCE true if converting from TinyMCE, false if converting to TinyMCE
   * @return the updated content HTML
   */
  public static String switchIconTagsInContent(String contentHtml, String tag, String newTag, boolean fromTinyMCE) {
    // Content received will look like: <em class="far fa-code"></em> <em class="far fa-code-2"></em>
    if (tag.equals(newTag)) {
      return contentHtml;
    }
    if (!contentHtml.contains("<" + tag)) {
      return contentHtml;
    }
    int tagIdx = 0;
    int endTagIdx = 0;
    int endTagLength = tag.length() + 3;
    while (tagIdx > -1) {
      // <em
      tagIdx = contentHtml.indexOf("<" + tag + " ", tagIdx);
      if (tagIdx == -1) {
        break;
      }
      // </em>
      endTagIdx = contentHtml.indexOf("</" + tag + ">", tagIdx);
      if (endTagIdx == -1) {
        break;
      }

      LOG.trace("TAG IDX (tagIdx:" + tagIdx + "; endTagIdx:" + endTagIdx + ")");

      // Look for a class attribute in-between
      int classIdx = contentHtml.indexOf("class=\"", tagIdx);
      if (classIdx == -1 || classIdx > endTagIdx) {
        tagIdx = endTagIdx + endTagLength;
        continue;
      }
      int endClassIdx = contentHtml.indexOf("\"", classIdx + 7);
      if (endClassIdx == -1) {
        tagIdx = endTagIdx + endTagLength;
        continue;
      }
      // If the class values contain 1 or more of the cssClassArray, switch this to required <i></i>
      String classValue = contentHtml.substring(classIdx + 7, endClassIdx).trim();
      LOG.trace("classValue: " + classValue);
      if (!classValue.isEmpty()) {
        List<String> cssValueList = Stream.of(classValue.split(" ")).map(String::trim).collect(toList());
        if (CollectionUtils.containsAny(cssValueList, FA_ICON_CSS)) {
          if (fromTinyMCE) {
            // Remove TinyMCE editor
            cssValueList.remove("tinymce-noedit");
          } else {
            // Add TinyMCE editor
            cssValueList.add("tinymce-noedit");
          }
          // Switch the tag content
          contentHtml = contentHtml.substring(0, tagIdx) + "<" + newTag + " class=\"" + StringUtils.join(cssValueList, " ") + "\">" +
              (fromTinyMCE ? "" : "&nbsp;") +
              "</" + newTag + ">" + contentHtml.substring(endTagIdx + endTagLength);
          tagIdx = contentHtml.indexOf("</" + newTag + ">", tagIdx + 1) + newTag.length() + 3;
          continue;
        }
      }
      tagIdx = endTagIdx + endTagLength;
    }
    return contentHtml;
  }

  public static String restoreIconTagsInContent(String contentHtml) {
    // Swap the tags
    for (String tag : TINY_MCE_ICON_TAGS) {
      contentHtml = EditorIconsCommand.switchIconTagsInContent(contentHtml, tag, "i", true);
    }
    return contentHtml;
  }

}
