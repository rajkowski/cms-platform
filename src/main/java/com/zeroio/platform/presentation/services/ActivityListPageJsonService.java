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

package com.zeroio.platform.presentation.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DataConstraints;
import com.simisinc.platform.application.cms.MarkdownCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.xapi.XapiStatement;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.persistence.xapi.XapiStatementRepository;
import com.simisinc.platform.infrastructure.persistence.xapi.XapiStatementSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service for page-level activity history
 * Aggregates page-only activities from metadata and web page xAPI statements
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ActivityListPageJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(ActivityListPageJsonService.class);

  @Override
  public JsonServiceContext get(JsonServiceContext context) {
    try {
      String pageId = context.getParameter("pageId");
      if (pageId == null || pageId.trim().isEmpty()) {
        return context.writeError("Page id is required");
      }
      WebPage webPage = WebPageRepository.findByLink(pageId);
      if (webPage == null || webPage.getId() == -1L) {
        return context.writeError("Page not found for link: " + pageId);
      }

      // Query xAPI statements and render the statement snapshots as-is
      List<XapiStatement> statementList = loadWebPageStatements(webPage);

      // Build JSON response
      StringBuilder json = new StringBuilder();
      json.append("[");

      boolean firstEntry = true;

      for (XapiStatement statement : statementList) {
        if (!firstEntry) {
          json.append(",");
        }
        firstEntry = false;

        String messageText = StringUtils.defaultIfBlank(statement.getMessageSnapshot(), statement.getVerb());
        if (StringUtils.isBlank(messageText)) {
          messageText = "Updated page";
        }

        json.append("{");
        json.append("\"created\":").append(statement.getOccurredAt() != null ? statement.getOccurredAt().getTime() : 0).append(",");
        json.append("\"messageText\":\"").append(JsonCommand.toJson(messageText)).append("\",");
        json.append("\"messageHtml\":\"").append(JsonCommand.toJson(renderMessageHtml(messageText))).append("\"");
        json.append("}");
      }

      json.append("]");

      return context.writeOk(json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error loading page activity: " + e.getMessage(), e);
      return context.writeError("Error loading activity history");
    }
  }

  private List<XapiStatement> loadWebPageStatements(WebPage webPage) {
    List<XapiStatement> statements = new ArrayList<>();

    if (webPage == null || webPage.getId() == -1) {
      return statements;
    }

    try {
      XapiStatementSpecification spec = new XapiStatementSpecification();
      spec.setObject("webPage");
      spec.setObjectId(webPage.getId());
      DataConstraints constraints = new DataConstraints(1, 100, "occurred_at", "desc");

      statements = XapiStatementRepository.findAll(spec, constraints);
    } catch (Exception e) {
      LOG.warn("Error loading webPage statements: " + e.getMessage());
    }

    return statements != null ? statements : new ArrayList<>();
  }

  private String renderMessageHtml(String messageText) {
    return MarkdownCommand.html(StringUtils.defaultString(messageText));
  }
}