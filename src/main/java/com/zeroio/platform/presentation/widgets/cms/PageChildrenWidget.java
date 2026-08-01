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
package com.zeroio.platform.presentation.widgets.cms;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.cms.LoadWebPageCommand;
import com.simisinc.platform.domain.model.cms.PageTreeNode;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.cms.LoadPageTreeCommand;

/**
 * Page Children Widget for displaying child pages with configurable depth filtering
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class PageChildrenWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908896L;

  static String widgetJsp = "/cms/page-children-widget.jsp";

  // Configuration parameters
  private static final String PARAM_USE_ROOT_PARENT = "useRootParent";
  private static final String PARAM_MAX_DEPTH = "maxDepth";
  private static final String PARAM_FILTER_PUBLISHED = "filterPublished";
  private static final String PARAM_SHOW_COUNT = "showCount";
  private static final String PARAM_PAGE_LINK = "pageLink";
  private static final String PARAM_SHOW_WHEN_EMPTY = "showWhenEmpty";

  @Override
  public WidgetContext execute(WidgetContext context) {

    // Common attributes
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Optional: Get configuration parameters from widget preferences
    String maxDepthValue = context.getPreferences().get(PARAM_MAX_DEPTH);
    if (StringUtils.isNotBlank(maxDepthValue)) {
      try {
        int maxDepth = Integer.parseInt(maxDepthValue);
        context.getRequest().setAttribute(PARAM_MAX_DEPTH, maxDepth);
      } catch (NumberFormatException nfe) {
        context.getRequest().setAttribute(PARAM_MAX_DEPTH, 1);
      }
    } else {
      context.getRequest().setAttribute(PARAM_MAX_DEPTH, 1);
    }

    String filterPublishedValue = context.getPreferences().get(PARAM_FILTER_PUBLISHED);
    if (StringUtils.isNotBlank(filterPublishedValue)) {
      context.getRequest().setAttribute(PARAM_FILTER_PUBLISHED, "true".equalsIgnoreCase(filterPublishedValue));
    } else {
      context.getRequest().setAttribute(PARAM_FILTER_PUBLISHED, true);
    }

    String showCountValue = context.getPreferences().get(PARAM_SHOW_COUNT);
    if (StringUtils.isNotBlank(showCountValue)) {
      context.getRequest().setAttribute(PARAM_SHOW_COUNT, "true".equalsIgnoreCase(showCountValue));
    } else {
      context.getRequest().setAttribute(PARAM_SHOW_COUNT, false);
    }

    boolean useRootParent = Boolean.parseBoolean(context.getPreferences().get(PARAM_USE_ROOT_PARENT));

    // Prepare the children data
    if (prepareChildrenData(context, useRootParent)) {
      context.setJsp(widgetJsp);
      return context;
    }

    boolean showWhenEmpty = Boolean.parseBoolean(context.getPreferences().get(PARAM_SHOW_WHEN_EMPTY));
    // No child pages found, check if we should show the widget when empty
    if (showWhenEmpty) {
      context.setJsp(widgetJsp);
      return context;
    }
    return null;
  }

  /**
   * Prepares the list of child pages based on configuration parameters, maintaining hierarchy
   *
   * @param context The widget context containing request and preferences
   */
  private boolean prepareChildrenData(WidgetContext context, boolean useRootParent) {
    Integer maxDepth = (Integer) context.getRequest().getAttribute(PARAM_MAX_DEPTH);
    Boolean filterPublished = (Boolean) context.getRequest().getAttribute(PARAM_FILTER_PUBLISHED);

    if (maxDepth == null || maxDepth < 1) {
      maxDepth = 1;
    }

    if (filterPublished == null) {
      filterPublished = true;
    }

    List<PageTreeNode> nodes = null;
    if (useRootParent) {
      // useRootParent uses parentId = -1
      nodes = LoadPageTreeCommand.loadPageTree(-1, maxDepth, filterPublished);
    } else {
      // Based on the current page
      String effectivePageLink = context.getRequest().getPagePath();

      // Based on a configured page link
      String pageLink = context.getPreferences().get(PARAM_PAGE_LINK);
      if (StringUtils.isNotBlank(pageLink)) {
        effectivePageLink = pageLink.startsWith("/") ? pageLink : "/" + pageLink;
      }

      // Verify the page
      WebPage page = LoadWebPageCommand.loadByLink(effectivePageLink);
      long pageId = page != null ? page.getId() : -1;
      if (pageId == -1) {
        LOG.debug("No page found for link: " + effectivePageLink);
        return false;
      }

      // Load the tree
      nodes = LoadPageTreeCommand.loadPageTree(pageId, maxDepth, filterPublished);
    }

    List<PageHierarchyItem> hierarchyItems = new ArrayList<>();
    for (int i = 0; i < nodes.size(); i++) {
      PageTreeNode node = nodes.get(i);
      WebPage child = new WebPage();
      child.setId(node.getId());
      if (StringUtils.isBlank(node.getTitle())) {
        if ("/".equals(node.getLink())) {
          child.setTitle("Home");
        } else {
          child.setTitle(node.getLink());
        }
      } else {
        child.setTitle(node.getTitle());
      }
      child.setLink(node.getLink());
      child.setDescription(node.getDescription());
      hierarchyItems.add(new PageHierarchyItem(child, node.getLevel(), isLastSibling(nodes, i)));
    }

    if (hierarchyItems.isEmpty()) {
      LOG.debug("No child pages found");
      return false;
    }

    // Set the prepared children list as a request attribute
    context.getRequest().setAttribute("childPages", hierarchyItems);
    return true;
  }

  private boolean isLastSibling(List<PageTreeNode> nodes, int currentIndex) {
    int currentLevel = nodes.get(currentIndex).getLevel();
    for (int i = currentIndex + 1; i < nodes.size(); i++) {
      int nextLevel = nodes.get(i).getLevel();
      if (nextLevel == currentLevel) {
        return false;
      }
      if (nextLevel < currentLevel) {
        return true;
      }
    }
    return true;
  }

  /**
   * Inner class to represent a page with its hierarchy level
   */
  public static class PageHierarchyItem {
    private WebPage page;
    private int level;
    private boolean lastSibling;

    public PageHierarchyItem(WebPage page, int level, boolean lastSibling) {
      this.page = page;
      this.level = level;
      this.lastSibling = lastSibling;
    }

    public WebPage getPage() {
      return page;
    }

    public int getLevel() {
      return level;
    }

    public boolean isLastSibling() {
      return lastSibling;
    }

    public boolean getLastSibling() {
      return lastSibling;
    }

    public String getIndentClass() {
      return "level-" + level;
    }

    public int getIndentPixels() {
      return level * 20;
    }
  }
}
