/*
 * Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Stylesheet;
import com.simisinc.platform.domain.model.cms.WebPage;

/**
 * Webpage Metadata commands
 *
 * @author matt rajkowski
 * @created 2/4/24 9:00 AM
 */
public class WebPageMetadataCommand {

  public static String getJSON(WebPage webPage, Stylesheet stylesheet, String webFileRoot, int contentLength) {
    // Output the web page meta-data json
    String webPageTitle = StringUtils.defaultString(webPage.getTitle());
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"").append("id").append("\"").append(":")
        .append("\"").append(webPage.getId()).append("\"");
    sb.append(",");
    sb.append("\"").append("link").append("\"").append(":")
        .append("\"").append(JsonCommand.toJson(webPage.getLink())).append("\"");
    if (StringUtils.isNotBlank(webPage.getRedirectUrl())) {
      sb.append(",");
      sb.append("\"").append("redirectUrl").append("\"").append(":")
          .append("\"").append(JsonCommand.toJson(webPage.getRedirectUrl())).append("\"");
    }
    sb.append(",");
    sb.append("\"").append("title").append("\"").append(":")
        .append("\"").append(JsonCommand.toJson(webPageTitle)).append("\"");
    // Keywords
    if (StringUtils.isNotBlank(webPage.getKeywords())) {
      sb.append(",");
      sb.append("\"").append("keywords").append("\"").append(":").append("\"")
          .append(JsonCommand.toJson(webPage.getKeywords())).append("\"");
    }
    // Description
    if (StringUtils.isNotBlank(webPage.getDescription())) {
      sb.append(",");
      sb.append("\"").append("description").append("\"").append(":").append("\"")
          .append(JsonCommand.toJson(webPage.getDescription())).append("\"");
    }
    // Last Modified
    sb.append(",");
    sb.append("\"").append("timestamp").append("\"").append(":")
        .append("\"").append(webPage.getModified().getTime()).append("\"");
    // Content Length
    sb.append(",");
    sb.append("\"").append("contentLength").append("\"").append(":")
        .append("\"").append(contentLength).append("\"");
    // Searchable
    if (webPage.isSearchable()) {
      sb.append(",");
      sb.append("\"").append("searchable").append("\"").append(":")
          .append("\"").append("true").append("\"");
    }
    // Show in Sitemap
    if (webPage.getShowInSitemap()) {
      sb.append(",");
      sb.append("\"").append("showInSitemap").append("\"").append(":")
          .append("\"").append("true").append("\"");
    }
    // Sitemap Change Frequency
    if (StringUtils.isNotBlank(webPage.getSitemapChangeFrequency())) {
      sb.append(",");
      sb.append("\"").append("sitemapChangeFrequency").append("\"").append(":").append("\"")
          .append(JsonCommand.toJson(webPage.getSitemapChangeFrequency())).append("\"");
    }
    // Sitemap priority
    if (webPage.getSitemapPriority() != null &&
        (webPage.getSitemapPriority().doubleValue() >= 0.0
            || webPage.getSitemapPriority().doubleValue() <= 1.0)) {
      sb.append(",");
      sb.append("\"").append("sitmapPriority").append("\"").append(":")
          .append("\"").append(webPage.getSitemapPriority().doubleValue()).append("\"");
    }
    // Page Stylesheet CSS file
    if (stylesheet != null) {
      sb.append(",");
      sb.append("\"").append("css").append("\"").append(":").append("\"")
          .append(JsonCommand.toJson(webFileRoot + ".css")).append("\"");
    }
    // Page XML file
    sb.append(",");
    sb.append("\"").append("layout").append("\"").append(":").append("\"")
        .append(JsonCommand.toJson(webFileRoot + ".xml")).append("\"");

    // End Json
    sb.append("}");
    return sb.toString();
  }

}
