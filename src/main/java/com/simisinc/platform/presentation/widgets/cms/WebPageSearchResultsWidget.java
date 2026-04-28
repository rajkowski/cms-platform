/*
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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.application.cms.LoadMenuTabsCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.MenuItem;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.simisinc.platform.domain.model.cms.TableOfContents;
import com.simisinc.platform.domain.model.cms.TableOfContentsLink;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ContentSpecification;
import com.simisinc.platform.infrastructure.persistence.cms.TableOfContentsRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHierarchyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageSpecification;
import com.simisinc.platform.presentation.controller.Column;
import com.simisinc.platform.presentation.controller.Page;
import com.simisinc.platform.presentation.controller.RequestConstants;
import com.simisinc.platform.presentation.controller.Section;
import com.simisinc.platform.presentation.controller.UserSession;
import com.simisinc.platform.presentation.controller.WebComponentCommand;
import com.simisinc.platform.presentation.controller.Widget;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 8/28/19 12:15 PM
 */
public class WebPageSearchResultsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/cms/web-page-search-results.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Determine the record paging
    int limit = Integer.parseInt(context.getPreferences().getOrDefault("limit", "15"));
    int page = context.getParameterAsInt("page", 1);
    int itemsPerPage = context.getParameterAsInt("items", limit);
    DataConstraints constraints = new DataConstraints(page, itemsPerPage);
    context.getRequest().setAttribute(RequestConstants.RECORD_PAGING, constraints);

    // Determine the search term
    String query = context.getParameter("query");
    if (StringUtils.isBlank(query)) {
      return null;
    }

    boolean requirePageInNavigation = "true".equals(context.getPreferences().getOrDefault("requirePageInNavigation", "false"));

    // Load the menu tabs, these are the directly linkable web pages
    List<MenuTab> menuTabList = requirePageInNavigation ? LoadMenuTabsCommand.findAllActiveIncludeMenuItemList() : null;

    // Load the table of contents
    List<TableOfContents> tableOfContentsList = requirePageInNavigation ? TableOfContentsRepository.findAll(null, null) : null;

    // Search the content and figure out the matching web pages
    ContentSpecification contentSpecification = new ContentSpecification();
    contentSpecification.setSearchTerm(query);
    List<Content> contentList = ContentRepository.findAll(contentSpecification, constraints);

    // Prepare the response
    Map<String, SearchResult> resultsMap = new LinkedHashMap<>();
    if (contentList == null) {
      // No content was found, return early
      return finishRequest(context, resultsMap);
    }

    // Determine the web pages that can be searched
    UserSession userSession = context.getUserSession();
    WebPageSpecification webPageSpecification = new WebPageSpecification();
    // Admins or content managers can see all pages, but regular users only see published pages that are searchable, and not drafts
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      // Limit the search to published pages that are searchable, and not drafts
      webPageSpecification.setSearchable(true);
      webPageSpecification.setDraft(false);
    }
    webPageSpecification.setHasRedirect(false);
    List<WebPage> webPageList = WebPageRepository.findAll(webPageSpecification, null);

    // Now search the web pages for a matching unique id
    contentLoop: for (Content content : contentList) {
      String contentUniqueId = content.getUniqueId();

      // Find active web pages with the matching content object
      for (WebPage webPage : webPageList) {
        String link = webPage.getLink();
        // Skip blank links
        if (StringUtils.isBlank(link)) {
          continue;
        }
        // Skip my page, unless user is logged in
        if (link.startsWith("/my-page") && !context.getUserSession().isLoggedIn()) {
          continue;
        }
        // Skip the cart to give priority to content pages
        if ("/cart".equals(link)) {
          continue;
        }

        // Find an active page the content is valid on
        if (StringUtils.isBlank(webPage.getPageXml()) ||
            (!webPage.getPageXml().contains("<uniqueId>" + contentUniqueId + "</uniqueId>") &&
                !webPage.getPageXml().contains("${uniqueId:" + contentUniqueId + "}"))) {
          // No uniqueIds on this page to confirm
          continue;
        }

        // Confirm the content is in a content widget, and verify the widget will render for this user
        Page pageRef = WebPageXmlLayoutCommand.retrievePageForRequest(webPage, link);
        if (pageRef == null) {
          continue;
        }
        if (!WebComponentCommand.allowsUser(pageRef, userSession)) {
          continue;
        }
        for (Section section : pageRef.getSections()) {
          if (!WebComponentCommand.allowsUser(section, userSession)) {
            continue;
          }
          for (Column column : section.getColumns()) {
            if (!WebComponentCommand.allowsUser(column, userSession)) {
              continue;
            }
            for (Widget widget : column.getWidgets()) {
              if (!WebComponentCommand.allowsUser(widget, userSession)) {
                continue;
              }
              // Check the widget
              if (!"content".equals(widget.getWidgetName())) {
                continue;
              }
              for (String key : widget.getPreferences().keySet()) {
                String value = widget.getPreferences().get(key);
                LOG.debug("Pref: " + key + "=" + value);
                if ((("uniqueId".equals(key) && value.contains(contentUniqueId)) || (value.contains("${uniqueId:" + contentUniqueId + "}"))) &&
                    (!requirePageInNavigation || isPageInTheNavigation(context, link, menuTabList, tableOfContentsList, webPageList))) {
                  // Page was found, but we only want to show results for linked pages, to avoid hidden pages
                  addTheSearchResult(webPage, link, content, resultsMap);
                  // No need to show more web pages which have the same repeated contentId
                  continue contentLoop;
                }
              }
            }
          }
        }
      }
    }
    context.getRequest().setAttribute("searchResultList", resultsMap.values());
    return finishRequest(context, resultsMap);
  }

  private boolean isPageInTheNavigation(WidgetContext context, String link, List<MenuTab> menuTabList,
      List<TableOfContents> tableOfContentsList, List<WebPage> webPageList) {
    // Allow the admin to see results for any page
    if (context.hasRole("admin") || context.hasRole("content-manager")) {
      return true;
    }
    // Determine if the link is in the navigation... like menu tabs, menu items, table of contents
    if (menuTabList != null) {
      for (MenuTab menuTab : menuTabList) {
        if (menuTab.getLink().equals(link)) {
          return true;
        }
        for (MenuItem menuItem : menuTab.getMenuItemList()) {
          if (menuItem.getLink().equals(link)) {
            return true;
          }
        }
      }
    }
    // Determine if the link is in the table of contents
    if (tableOfContentsList != null) {
      for (TableOfContents tableOfContents : tableOfContentsList) {
        for (TableOfContentsLink tableOfContentsLink : tableOfContents.getEntries()) {
          if (link.equals(tableOfContentsLink.getLink())) {
            return true;
          }
        }
      }
    }
    // Determine if the link is in the web page hierarchy
    if (webPageList != null) {
      for (WebPage webPage : webPageList) {
        if (link.equals(webPage.getLink())) {
          if (WebPageHierarchyRepository.findByPageId(webPage.getId()) != null) {
            return true;
          }
          break;
        }
      }
    }
    return false;
  }

  private void addTheSearchResult(WebPage webPage, String link, Content content, Map<String, SearchResult> resultsMap) {
    // Add the search result
    String htmlContent = HtmlCommand.toHtml(content.getHighlight());
    if (StringUtils.isNotBlank(htmlContent)) {
      htmlContent = StringUtils.replace(htmlContent, "${b}", "<strong>");
      htmlContent = StringUtils.replace(htmlContent, "${/b}", "</strong>");
    }
    SearchResult searchResult = resultsMap.get(link);
    if (searchResult == null) {
      searchResult = new SearchResult();
      searchResult.setLink(link);
      if ("/".equals(link)) {
        // It's the home page
        searchResult.setPageTitle(LoadSitePropertyCommand.loadByName("site.name"));
      } else {
        searchResult.setPageTitle(webPage.getTitle());
      }
      if (StringUtils.isNotBlank(webPage.getDescription())) {
        searchResult.setPageDescription(webPage.getDescription());
      }
      searchResult.setHtmlExcerpt(htmlContent);
      resultsMap.put(link, searchResult);
    } else {
      searchResult.setHtmlExcerpt(searchResult.getHtmlExcerpt() + " " + htmlContent);
    }
  }

  private WidgetContext finishRequest(WidgetContext context, Map<String, SearchResult> resultsMap) {
    // Determine if the widget is shown
    boolean showWhenEmpty = "true".equals(context.getPreferences().getOrDefault("showWhenEmpty", "true"));
    if (resultsMap.isEmpty() && !showWhenEmpty) {
      return context;
    }

    // Determine the view
    context.setJsp(JSP);
    return context;
  }
}
