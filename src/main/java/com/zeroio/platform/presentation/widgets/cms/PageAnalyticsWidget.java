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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.analytics.PageAnalyticsDataService;

/**
 * Page Analytics Widget for site usage and visitor analytics
 */
public class PageAnalyticsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(PageAnalyticsWidget.class);

  static String PAGE_DASHBOARD_JSP = "/cms/page-analytics-dashboard.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {

    if (!context.getUserSession().hasRole("admin") && !context.getUserSession().hasRole("content-manager")) {
      context.setErrorMessage("You do not have permission to access the analytics dashboard");
      return context;
    }

    return loadPageAnalytics(context);
  }

  private WidgetContext loadPageAnalytics(WidgetContext context) {
    context.setJsp(PAGE_DASHBOARD_JSP);

    String webPageLink = StringUtils.trimToNull(context.getParameter("webPage"));
    if (StringUtils.isBlank(webPageLink)) {
      context.setErrorMessage("A web page is required");
      context.getRequest().setAttribute("pageAnalyticsData", "{\"success\":false}");
      return context;
    }

    WebPage webPage = WebPageRepository.findByLink(webPageLink);
    if (webPage == null) {
      webPage = new WebPage();
      webPage.setLink(webPageLink);
      webPage.setTitle(webPageLink);
    }

    String fromDateValue = StringUtils.trimToNull(context.getParameter("fromDate"));
    String toDateValue = StringUtils.trimToNull(context.getParameter("toDate"));
    LocalDate fromDate = parseDate(fromDateValue);
    LocalDate toDate = parseDate(toDateValue);
    int days = NumberUtils.toInt(context.getParameter("days"), 7);
    if (days < 1 || days > 365) {
      days = 7;
    }

    try {
      ObjectNode analyticsData;
      if (fromDate != null && toDate != null) {
        analyticsData = PageAnalyticsDataService.loadPageAnalytics(webPageLink, fromDate, toDate);
        days = 0;
      } else {
        analyticsData = PageAnalyticsDataService.loadPageAnalytics(webPageLink, days);
      }
      context.getRequest().setAttribute("pageAnalyticsData", analyticsData == null ? null : analyticsData.toString());
      if (analyticsData == null || !analyticsData.has("trend") || !analyticsData.get("trend").isArray()
          || analyticsData.get("trend").size() == 0) {
        context.setWarningMessage("No page analytics data is available for this request");
      }
    } catch (Exception e) {
      LOG.error("Unable to load page analytics for " + webPageLink, e);
      context.setWarningMessage("Analytics data could not be loaded");
      context.getRequest().setAttribute("pageAnalyticsData", null);
    }

    context.getRequest().setAttribute("analyticsPage", webPage);
    context.getRequest().setAttribute("pageAnalyticsDays", days);
    context.getRequest().setAttribute("pageAnalyticsFromDate", fromDateValue);
    context.getRequest().setAttribute("pageAnalyticsToDate", toDateValue);
    return context;
  }

  private LocalDate parseDate(String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
