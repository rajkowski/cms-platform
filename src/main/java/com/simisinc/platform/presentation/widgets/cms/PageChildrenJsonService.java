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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.PageTreeNode;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.cms.WebPageHierarchy;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHierarchyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.zeroio.platform.application.cms.LoadPageTreeCommand;

/**
 * Handles JSON/AJAX GET requests for /json/pages/children endpoint
 * Returns child pages for lazy tree loading
 *
 * @author matt rajkowski
 * @created 2/7/26 3:00 PM
 */
public class PageChildrenJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(PageChildrenJsonService.class);

  /**
   * Handles GET requests for child pages
   *
   * @param context the widget context
   * @return context with JSON response
   */
  public JsonServiceContext get(JsonServiceContext context) {

    try {
      // Get parent ID parameter (null means root level pages)
      String parentIdParam = context.getParameter("parentId");
      Long parentIdValue = null;

      // Handle various "no parent" values: null, "null", "0", "undefined", empty string
      if (StringUtils.isNotBlank(parentIdParam) && !"null".equals(parentIdParam) && !"0".equals(parentIdParam)
          && !"undefined".equals(parentIdParam)) {
        parentIdValue = Long.parseLong(parentIdParam);
      }
      long parentId = parentIdValue != null ? parentIdValue : -1;
      boolean includeParents = "true".equalsIgnoreCase(context.getParameter("includeParents"));

      // Load child pages using LoadPageTreeCommand, based on user permissions
      List<PageTreeNode> children;
      if (PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
        // Full permission - load all child pages
        children = LoadPageTreeCommand.loadPageTree(parentId);
      } else {
        // Limited permission - filter out unpublished pages
        children = LoadPageTreeCommand.loadPageTree(parentId, 5, true);
      }

      if (!includeParents) {
        return context.writeOk(buildChildrenArray(children), null);
      }

      List<WebPage> parentChain = new ArrayList<>();
      WebPage currentPage = null;
      if (parentId > 0) {
        currentPage = WebPageRepository.findById(parentId);
      }
      if (currentPage != null) {
        parentChain = getAncestorsFromHierarchyPath(currentPage.getId());
      }

      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"parents\":").append(buildWebPageArray(parentChain)).append(",");
      json.append("\"currentPage\":").append(buildWebPageObject(currentPage)).append(",");
      json.append("\"children\":").append(buildChildrenArray(children));
      json.append("}");

      return context.writeOk(json.toString(), null);

    } catch (Exception e) {
      LOG.error("Error loading page children: " + e.getMessage(), e);
      return context.writeError(e.getMessage());
    }
  }

  private List<WebPage> getAncestorsFromHierarchyPath(long pageId) {
    List<WebPage> ancestors = new ArrayList<>();
    WebPageHierarchy hierarchy = WebPageHierarchyRepository.findByPageId(pageId);
    if (hierarchy == null || StringUtils.isBlank(hierarchy.getPath())) {
      return ancestors;
    }

    String[] pathSegments = StringUtils.split(hierarchy.getPath(), '/');
    if (pathSegments == null || pathSegments.length < 2) {
      return ancestors;
    }

    // Path includes current page as the last segment; parents are all preceding segments.
    int ancestorCount = pathSegments.length - 1;
    for (int i = 0; i < ancestorCount; i++) {
      long ancestorId = NumberUtils.toLong(pathSegments[i], -1);
      if (ancestorId < 1) {
        continue;
      }
      WebPage ancestor = WebPageRepository.findById(ancestorId);
      if (ancestor != null) {
        ancestors.add(ancestor);
      }
    }

    return ancestors;
  }

  private String buildChildrenArray(List<PageTreeNode> children) {
    StringBuilder json = new StringBuilder();
    json.append("[");
    if (children != null && !children.isEmpty()) {
      boolean first = true;
      for (PageTreeNode node : children) {
        if (!first) {
          json.append(",");
        }
        first = false;
        json.append("{");
        json.append("\"id\":").append(node.getId()).append(",");
        json.append("\"title\":\"").append(JsonCommand.toJson(node.getTitle())).append("\",");
        json.append("\"link\":\"").append(JsonCommand.toJson(node.getLink())).append("\",");
        json.append("\"level\":").append(node.getLevel()).append(",");
        json.append("\"hasChildren\":").append(node.isHasChildren());
        json.append("}");
      }
    }
    json.append("]");
    return json.toString();
  }

  private String buildWebPageArray(List<WebPage> pages) {
    StringBuilder json = new StringBuilder();
    json.append("[");
    if (pages != null && !pages.isEmpty()) {
      boolean first = true;
      for (WebPage page : pages) {
        if (!first) {
          json.append(",");
        }
        first = false;
        json.append(buildWebPageObject(page));
      }
    }
    json.append("]");
    return json.toString();
  }

  private String buildWebPageObject(WebPage page) {
    if (page == null) {
      return "null";
    }
    String pageTitle = page.getTitle();
    if ("/".equals(page.getLink()) && StringUtils.isBlank(pageTitle)) {
      // Special handling for the root page if needed
      pageTitle = "Home";
    }
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"id\":").append(page.getId()).append(",");
    json.append("\"title\":\"").append(JsonCommand.toJson(pageTitle)).append("\",");
    json.append("\"link\":\"").append(JsonCommand.toJson(page.getLink())).append("\"");
    json.append("}");
    return json.toString();
  }
}
