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
package com.zeroio.platform.rest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.cms.ContentValuesCommand;
import com.simisinc.platform.application.cms.LoadContentCommand;
import com.simisinc.platform.application.cms.ResolveContentDirectivesCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageSpecification;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;
import com.simisinc.platform.rest.controller.ServiceResponseCommand;

/**
 * Gets, creates or updates web pages from posted request parameters.
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class WebPagesService extends GenericRestService {

  private static final Pattern UNIQUE_ID_TAG_PATTERN = Pattern
      .compile("<(?:uniqueId|contentUniqueId)>([^<]+)</(?:uniqueId|contentUniqueId)>");
  private static final Pattern UNIQUE_ID_DIRECTIVE_PATTERN = Pattern.compile("\\$\\{uniqueId:([^}]+)\\}");

  // GET /webPages
  @Override
  public ServiceResponse get(ServiceContext context) {

    // Determine the constraints
    int pageNumber = context.getParameterAsInt("page", 1);
    int pageSize = context.getParameterAsInt("size", 10);
    if (pageNumber < 1 || pageSize < 1) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", "Required query params: page (>=1) and size (>=1)");
      return response;
    }

    // Prepare the specification
    WebPageSpecification specification = new WebPageSpecification();
    specification.setEnabled(true);
    specification.setDraft(false);

    // Check for tags
    String tagsValue = context.getParameter("tags");
    if (StringUtils.isNotBlank(tagsValue)) {
      List<String> filterTagList = new ArrayList<>();
      String[] tagsArray = tagsValue.split(",");
      for (String tag : tagsArray) {
        filterTagList.add(tag.trim());
      }
      if (!filterTagList.isEmpty()) {
        specification.setFilterTags(filterTagList.toArray(new String[0]));
      }
    }

    // Check for a search query
    String query = context.getParameter("query");
    if (StringUtils.isNotBlank(query)) {
      specification.setSearchTerm(query);
    }

    DataConstraints constraints = new DataConstraints(pageNumber, pageSize);
    constraints.setColumnToSortBy("link", "asc");

    List<WebPage> webPages = WebPageRepository.findAll(specification, constraints);
    List<Map<String, Object>> responseList = new ArrayList<>();
    for (WebPage webPage : webPages) {
      String html = compileContent(webPage);
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("link", webPage.getLink());
      entry.put("title", webPage.getTitle());
      entry.put("keywords", webPage.getKeywords());
      entry.put("redirectUrl", webPage.getRedirectUrl());
      entry.put("created", webPage.getCreated() != null ? webPage.getCreated().toInstant().toString() : null);
      entry.put("modified", webPage.getModified() != null ? webPage.getModified().toInstant().toString() : null);
      entry.put("tags", webPage.getTags());
      entry.put("html", html);
      // entry.put("text", HtmlCommand.text(html));
      responseList.add(entry);
    }

    ServiceResponse response = new ServiceResponse(200);
    ServiceResponseCommand.addMeta(response, "webPage", responseList, constraints);
    response.setData(responseList);
    return response;
  }

  private static String compileContent(WebPage webPage) {
    String pageXml = webPage.getPageXml();
    if (StringUtils.isBlank(pageXml)) {
      return "";
    }

    Set<String> contentUniqueIds = new LinkedHashSet<>();

    Matcher tagMatcher = UNIQUE_ID_TAG_PATTERN.matcher(pageXml);
    while (tagMatcher.find()) {
      String uniqueId = StringUtils.trimToNull(tagMatcher.group(1));
      if (uniqueId != null) {
        contentUniqueIds.add(uniqueId);
      }
    }

    Matcher directiveMatcher = UNIQUE_ID_DIRECTIVE_PATTERN.matcher(pageXml);
    while (directiveMatcher.find()) {
      String uniqueId = StringUtils.trimToNull(directiveMatcher.group(1));
      if (uniqueId != null) {
        contentUniqueIds.add(uniqueId);
      }
    }

    if (contentUniqueIds.isEmpty()) {
      return "";
    }

    StringBuilder contentBuilder = new StringBuilder();
    for (String uniqueId : contentUniqueIds) {
      Content content = LoadContentCommand.loadContentByUniqueId(uniqueId);
      if (content == null) {
        continue;
      }
      String html = StringUtils.defaultString(content.getContent());
      html = ResolveContentDirectivesCommand.resolveDirectives(html);
      html = ContentValuesCommand.replaceDynamicValues(html);
      if (StringUtils.isNotBlank(html)) {
        if (contentBuilder.length() > 0) {
          contentBuilder.append("\n");
        }
        contentBuilder.append(html);
      }
    }

    return contentBuilder.toString();
  }

}
