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
import org.apache.commons.lang3.math.NumberUtils;

import com.simisinc.platform.application.cms.LoadWebPageCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.cms.WebPageHierarchy;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHierarchyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Page Breadcrumb Widget for displaying page parent trail as breadcrumb navigation
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class PageBreadcrumbWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;

  static String widgetJsp = "/cms/page-breadcrumb-widget.jsp";

  private static final String PARAM_SHOW_ROOT_PAGE = "showRootPage";
  private static final String PARAM_SEPARATOR = "separator";
  private static final String PARAM_MAX_ITEMS = "maxItems";

  public WidgetContext execute(WidgetContext context) {

    // Set the JSP
    context.setJsp(widgetJsp);

    // Optional: Get configuration parameters from widget preferences
    String showRootPageValue = context.getPreferences().get(PARAM_SHOW_ROOT_PAGE);
    if (StringUtils.isNotBlank(showRootPageValue)) {
      context.getRequest().setAttribute(PARAM_SHOW_ROOT_PAGE, "true".equalsIgnoreCase(showRootPageValue));
    } else {
      context.getRequest().setAttribute(PARAM_SHOW_ROOT_PAGE, true);
    }

    String separatorValue = context.getPreferences().get(PARAM_SEPARATOR);
    if (StringUtils.isNotBlank(separatorValue)) {
      context.getRequest().setAttribute(PARAM_SEPARATOR, separatorValue);
    } else {
      context.getRequest().setAttribute(PARAM_SEPARATOR, " / ");
    }

    String maxItemsValue = context.getPreferences().get(PARAM_MAX_ITEMS);
    if (StringUtils.isNotBlank(maxItemsValue)) {
      try {
        int maxItems = Integer.parseInt(maxItemsValue);
        context.getRequest().setAttribute(PARAM_MAX_ITEMS, maxItems);
      } catch (NumberFormatException nfe) {
        context.getRequest().setAttribute(PARAM_MAX_ITEMS, 10);
      }
    } else {
      context.getRequest().setAttribute(PARAM_MAX_ITEMS, 10);
    }

    // Prepare the breadcrumb data
    prepareBreadcrumbData(context);

    return context;
  }

  /**
   * Prepares the breadcrumb trail based on configuration parameters
   *
   * @param context The widget context containing request and preferences
   */
  private void prepareBreadcrumbData(WidgetContext context) {
    Integer maxItems = (Integer) context.getRequest().getAttribute(PARAM_MAX_ITEMS);

    if (maxItems == null || maxItems < 1) {
      maxItems = 10;
    }

    WebPage page = LoadWebPageCommand.loadByLink(context.getRequest().getPagePath());
    long pageId = page != null ? page.getId() : -1;

    if (pageId > 0) {
      List<WebPage> ancestors = getAncestorsFromHierarchy(pageId, maxItems);

      // Set the prepared breadcrumb list as a request attribute
      context.getRequest().setAttribute("breadcrumbItems", ancestors);

      // Set the current page as the last item
      WebPage currentPage = WebPageRepository.findById(pageId);
      context.getRequest().setAttribute("currentPage", currentPage);
    }
  }

  /**
   * Gets ancestor pages from the persisted hierarchy path in root-to-parent order
   *
   * @param pageId the current page id
   * @param maxItems max number of ancestor pages
   * @return list of ancestor pages in breadcrumb order
   */
  private List<WebPage> getAncestorsFromHierarchy(long pageId, int maxItems) {
    List<WebPage> ancestors = new ArrayList<>();
    WebPageHierarchy hierarchy = WebPageHierarchyRepository.findByPageId(pageId);
    if (hierarchy == null || StringUtils.isBlank(hierarchy.getPath())) {
      return ancestors;
    }

    String[] pathSegments = StringUtils.split(hierarchy.getPath(), '/');
    if (pathSegments == null || pathSegments.length == 0) {
      return ancestors;
    }

    int ancestorCount = Math.max(0, pathSegments.length - 1);
    int startIndex = Math.max(0, ancestorCount - maxItems);
    for (int i = startIndex; i < ancestorCount; i++) {
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
}
