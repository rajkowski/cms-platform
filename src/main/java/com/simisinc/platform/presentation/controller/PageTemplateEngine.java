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

package com.simisinc.platform.presentation.controller;

import static com.simisinc.platform.presentation.controller.RequestConstants.CONTEXT_PATH;
import static com.simisinc.platform.presentation.controller.RequestConstants.FOOTER_RENDER_INFO;
import static com.simisinc.platform.presentation.controller.RequestConstants.FOOTER_STICKY_LINKS;
import static com.simisinc.platform.presentation.controller.RequestConstants.HEADER_RENDER_INFO;
import static com.simisinc.platform.presentation.controller.RequestConstants.LOG_USER;
import static com.simisinc.platform.presentation.controller.RequestConstants.MASTER_MENU_TAB_LIST;
import static com.simisinc.platform.presentation.controller.RequestConstants.PAGE_COLLECTION;
import static com.simisinc.platform.presentation.controller.RequestConstants.PAGE_COLLECTION_CATEGORY;
import static com.simisinc.platform.presentation.controller.RequestConstants.PAGE_RENDER_INFO;
import static com.simisinc.platform.presentation.controller.RequestConstants.SHOW_MAIN_MENU;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;

import com.simisinc.platform.ApplicationInfo;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.LoadMenuTabsCommand;
import com.simisinc.platform.application.cms.LoadStylesheetCommand;
import com.simisinc.platform.application.cms.LoadTableOfContentsCommand;
import com.simisinc.platform.application.cms.WebContainerLayoutCommand;
import com.simisinc.platform.application.cms.WebPackageCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.application.items.LoadCategoryCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.Role;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.domain.model.cms.Stylesheet;
import com.simisinc.platform.domain.model.cms.TableOfContents;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.presentation.widgets.cms.WebContainerContext;

/**
 * Handles page template rendering requests
 *
 * @author matt rajkowski
 * @created 1/20/24 2:02 PM
 */
public class PageTemplateEngine {

  private static Log LOG = LogFactory.getLog(PageTemplateEngine.class);

  // Template renderer
  private static TemplateEngine templateEngine = new TemplateEngine();

  // Widget Cache
  private static Map<String, Object> widgetInstances = new HashMap<>();

  // Web Packages
  private static Map<String, WebPackage> webPackageList = null;

  /** Initialize the Thymeleaf renderer engine and widgets */
  public static boolean startup(AbstractConfigurableTemplateResolver templateResolver, String htmlTemplateLocation,
      Map<String, String> widgetLibrary, URL webPackageFile) {

    if (templateEngine.isInitialized()) {
      LOG.info("PageTemplateEngine is already initialized");
      return true;
    }

    LOG.info("PageTemplateEngine starting up...");

    // Set the template resolver configuration
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setPrefix(htmlTemplateLocation);
    templateResolver.setSuffix(".html");
    templateResolver.setCacheable(false);
    templateEngine.setTemplateResolver(templateResolver);

    // Load the frontend web resource catalog for handling web package tags
    WebPackageResolver webPackageResolver = new WebPackageResolver();
    try {
      LOG.info("Loading web packages...");
      webPackageList = WebPackageCommand.init(webPackageFile);
      LOG.info("Added web packages: " + webPackageList.size());
      webPackageResolver.setWebPackageList(webPackageList);
    } catch (Exception e) {
      LOG.error("Web packages error:", e);
      // throw new ServletException(e);
    }

    // Add the Page Template Message Resolvers
    templateEngine.addMessageResolver(webPackageResolver);
    templateEngine.addMessageResolver(new TemplateMessageResolver());

    // Instantiate the widgets
    if (!isInitialized()) {
      LOG.info("Instantiating the widgets...");
      for (String widgetName : widgetLibrary.keySet()) {
        String widgetClass = widgetLibrary.get(widgetName);
        try {
          if (StringUtils.isBlank(widgetClass)) {
            LOG.error("Widget class is blank for widget: " + widgetName);
            continue;
          }
          Class<?> clazz = Class.forName(widgetClass);
          if (clazz == null) {
            LOG.error("Class not found for widget: " + widgetName);
            continue;
          }
          Constructor<?> constructor = clazz.getDeclaredConstructor();
          if (constructor == null) {
            LOG.error("Constructor not found for widget: " + widgetName);
            continue;
          }
          constructor.setAccessible(true);
          Object classRef = constructor.newInstance();
          widgetInstances.put(widgetName, classRef);
          LOG.info("Added widget class: " + widgetName + " = " + widgetClass);
        } catch (Exception | NoClassDefFoundError e) {
          LOG.error("Class not initialized for: " + widgetName + " = " + widgetClass + ": " + e.getMessage());
        }
      }
      LOG.info("Widgets loaded: " + widgetInstances.size());
    }

    return true;
  }

  public void destroy() {
    // Consider any resources to destroy
  }

  public static boolean isInitialized() {
    return !widgetInstances.isEmpty();
  }

  public static TemplateEngine useTemplateEngine() {
    if (!isInitialized()) {
      return null;
    }
    return templateEngine;
  }

  public static String render(WebPage webPage, File webAppPath) {

    String redirectLocation = webPage.getRedirectUrl();
    if (StringUtils.isNotBlank(redirectLocation)) {
      LOG.debug("Returning... has redirect");
      // @todo generate a redirect page for now
      // Use an NGINX include file sitename.redirects
      // Use a meta-tag... 
      // 
      // Handle a redirect immediately
      // if (!redirectLocation.startsWith("http:") && !redirectLocation.startsWith("https:")) {
      //   redirectLocation = pageRequest.getBaseUrl() +
      //       (redirectLocation.startsWith("/") ? "" : "/") +
      //       redirectLocation;
      // }
      // response.setHeader("Location", redirectLocation);
      // response.setStatus(SC_MOVED_PERMANENTLY);
      return null;
    }

    if (webPage.getDraft()) {
      LOG.debug("Returning... page is a draft");
      return null;
    }

    long startRequestTime = System.currentTimeMillis();

    try {
      // Determine information about the request being made
      PageRequest pageRequest = new PageRequest(
          "GET", "http", "localhost", 80, "", webPage.getLink(), null, false, "localhost");
      pageRequest.setParameterMap(new LinkedHashMap<>());

      // The controller manages the widget data
      ControllerSession controllerSession = new ControllerSession();

      // Widgets are accessed by users and guests
      List<Role> roleList = new ArrayList<>();

      // Related user information
      List<Group> groupList = new ArrayList<>();

      // User information
      User user = new User();
      user.setId(UserSession.GUEST_ID);
      user.setRoleList(roleList);
      user.setGroupList(groupList);

      UserSession userSession = new UserSession();
      userSession.login(user);

      // Generate a static web page
      Page pageRef = WebPageXmlLayoutCommand.retrievePageForRequest(webPage, webPage.getLink());
      if (pageRef == null) {
        LOG.error("PAGE NOT FOUND: " + webPage.getLink());
        // response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      // Verify the user has access to the page
      if (!WebComponentCommand.allowsUser(pageRef, userSession)) {
        LOG.warn("PAGE NOT ALLOWED: " + webPage.getLink() + " " +
            (!pageRef.getRoles().isEmpty() ? "[roles=" + pageRef.getRoles().toString() + "]" + " " : "") +
            (!pageRef.getGroups().isEmpty() ? "[groups=" + pageRef.getGroups().toString() + "]" + " " : ""));
        // response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      LOG.info("Page info: " + pageRef.getName());

      // Determine the system and user data to be used by local and remote widgets...
      Map<String, String> coreData = new HashMap<>();
      coreData.put("userId", String.valueOf(userSession.getUserId()));

      // Determine the global collection for this request
      Collection thisCollection = null;
      if (pageRef.checkForCollectionUniqueId()) {
        String collectionUniqueId = null;
        // Determine if Id is in Request or URI
        if (pageRef.getCollectionUniqueId().startsWith("?")) {
          if ("?collectionId".equals(pageRef.getCollectionUniqueId())) {
            String collectionIdValue = pageRequest.getParameter("collectionId");
            if (StringUtils.isNumeric(collectionIdValue)) {
              long collectionId = Long.parseLong(collectionIdValue);
              thisCollection = LoadCollectionCommand.loadCollectionByIdForAuthorizedUser(collectionId,
                  userSession.getUserId());
              if (thisCollection != null) {
                collectionUniqueId = thisCollection.getUniqueId();
              }
            }
          } else {
            collectionUniqueId = pageRequest.getParameter("collectionUniqueId");
          }
        } else if (webPage.getLink().startsWith("/") && webPage.getLink().contains("*")) {
          collectionUniqueId = webPage.getLink().substring(webPage.getLink().indexOf("*"));
        }
        if (!StringUtils.isBlank(collectionUniqueId)) {
          if (thisCollection == null) {
            thisCollection = LoadCollectionCommand.loadCollectionByUniqueIdForAuthorizedUser(collectionUniqueId,
                userSession.getUserId());
          }
          if (thisCollection == null) {
            LOG.error(
                "COLLECTION NOT ALLOWED: " + webPage.getLink() + " [roles=" + pageRef.getRoles().toString() + "]");
            // response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
          }
          coreData.put("collectionUniqueId", collectionUniqueId);
          LOG.debug("Added collection to coreData: " + collectionUniqueId);
        }
      }

      // Determine the global item for this request
      Item thisItem = null;
      if (pageRef.checkForItemUniqueId()) {
        String subTab = "";
        // Extract the item unique id
        // String itemUniqueId = pageRequest.getUri().substring(pageRef.getItemUniqueId().indexOf("*"));
        String itemUniqueId = webPage.getLink().substring(pageRef.getItemUniqueId().indexOf("*"));
        if (itemUniqueId.contains("/")) {
          subTab = "/_" + itemUniqueId.substring(itemUniqueId.indexOf("/") + 1) + "_";
          itemUniqueId = itemUniqueId.substring(0, itemUniqueId.indexOf("/"));
        }
        // User must be authorized here...
        thisItem = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(itemUniqueId, userSession.getUserId());
        if (thisItem == null) {
          LOG.error("ITEM NOT ALLOWED: " + webPage.getLink() + " [roles=" + pageRef.getRoles().toString() + "]");
          // response.sendError(HttpServletResponse.SC_NOT_FOUND);
          return null;
        }
        LOG.debug("Added item to coreData: " + itemUniqueId);
        coreData.put("itemUniqueId", itemUniqueId);
        // Look for the collection page
        if (!coreData.containsKey("collectionUniqueId")) {
          thisCollection = LoadCollectionCommand.loadCollectionById(thisItem.getCollectionId());
          coreData.put("collectionUniqueId", thisCollection.getUniqueId());
          LOG.debug("Added item's collection to coreData: " + thisCollection.getUniqueId());
          // Check for a collection customized page
          // @note plan for sub-tabs too
          int slashIndex = webPage.getLink().indexOf("/", 1);
          String itemMethod = webPage.getLink().substring(1, slashIndex);
          String itemCollectionKey = "_" + itemMethod + "_" + thisCollection.getUniqueId() + "_" + subTab;
          LOG.debug("itemCollectionKey=" + itemCollectionKey);
          if (WebPageXmlLayoutCommand.containsPage(itemCollectionKey)) {
            pageRef = WebPageXmlLayoutCommand.retrievePage(itemCollectionKey);
          }
        }
      }

      // Determine the global collection category for this request
      Category thisCollectionCategory = null;
      if (thisItem != null) {
        thisCollectionCategory = LoadCategoryCommand.loadCategoryById(thisItem.getCategoryId());
      }

      // Load the properties
      Map<String, String> systemPropertyMap = LoadSitePropertyCommand.loadAsMap("system");
      Map<String, String> sitePropertyMap = LoadSitePropertyCommand.loadAsMap("site");
      sitePropertyMap.put("site.login", "false");
      sitePropertyMap.put("site.cart", "false");
      sitePropertyMap.put("site.registrations", "false");
      Map<String, String> themePropertyMap = LoadSitePropertyCommand.loadAsMap("theme");
      Map<String, String> socialPropertyMap = LoadSitePropertyCommand.loadAsMap("social");
      Map<String, String> analyticsPropertyMap = LoadSitePropertyCommand.loadAsMap("analytics");
      Map<String, String> ecommercePropertyMap = LoadSitePropertyCommand.loadAsMap("ecommerce");

      // Setup the rendering info
      PageRenderInfo pageRenderInfo = new PageRenderInfo(pageRef, webPage.getLink());
      if (pageRenderInfo.getName().startsWith("_")) {
        // Show the actual name from the request, not the template name
        pageRenderInfo.setName(webPage.getLink());
      }
      if (thisCollection != null && thisItem != null) {
        pageRenderInfo.setTitle(thisItem.getName() + " | " + thisCollection.getName());
      } else {
        if (thisCollection != null) {
          pageRenderInfo.setTitle(thisCollection.getName());
        }
        if (thisItem != null) {
          pageRenderInfo.setTitle(thisItem.getName());
        }
      }
      // HTML metadata (which can also be updated later via widgets)
      if (webPage != null) {
        if (StringUtils.isBlank(pageRenderInfo.getTitle())) {
          pageRenderInfo.setTitle(webPage.getTitle());
        }
        if (StringUtils.isBlank(pageRenderInfo.getKeywords())) {
          pageRenderInfo.setKeywords(webPage.getKeywords());
        }
        if (StringUtils.isBlank(pageRenderInfo.getDescription())) {
          pageRenderInfo.setDescription(webPage.getDescription());
        }
        if (StringUtils.isNotBlank(webPage.getImageUrl())) {
          pageRenderInfo.setImageUrl(webPage.getImageUrl());
        }
      }

      if (StringUtils.isBlank(pageRenderInfo.getKeywords())) {
        pageRenderInfo.setKeywords(sitePropertyMap.get("site.keywords"));
      }
      if (StringUtils.isBlank(pageRenderInfo.getDescription())) {
        pageRenderInfo.setDescription(sitePropertyMap.get("site.description"));
      }

      // Finally... we have a page ready to be processed...
      if (LOG.isDebugEnabled()) {
        LOG.debug(pageRequest.getMethod() + " page " + pageRef.getName());
      }

      // Create a context for processing the widgets
      URL applicationUrl = webAppPath.toURI().toURL();
      WebContainerContext webContainerContext = new WebContainerContext(applicationUrl, pageRequest, null,
          controllerSession, widgetInstances, webPackageList, webPage, pageRef);

      // Render the page widgets
      PageResponse pageResponse = WebContainerCommand.processWidgets(webContainerContext, pageRef.getSections(),
          pageRenderInfo, coreData, userSession, themePropertyMap, null);

      if (pageResponse.isHandled()) {
        // The widget processor handled the response, immediately return
        LOG.debug("Returning... widget handled response");
        return null;
      }

      // Additional layouts
      File webLayoutsPath = new File(webAppPath, Paths.get("WEB-INF", "web-layouts").toString());

      // Render the header based on the web page
      URL headerUrl = new File(webLayoutsPath, Paths.get("header", "header-layout.xml").toString()).toURI().toURL();
      Header header = WebContainerLayoutCommand.retrieveHeader("header.default", headerUrl);
      HeaderRenderInfo headerRenderInfo = new HeaderRenderInfo(header, webPage.getLink());
      WebContainerCommand.processWidgets(webContainerContext, header.getSections(), headerRenderInfo, coreData,
          userSession, themePropertyMap, null);

      // Render the footer
      URL footerUrl = new File(webLayoutsPath, Paths.get("footer", "footer-layout.xml").toString()).toURI().toURL();
      Footer footer = WebContainerLayoutCommand.retrieveFooter("footer.default", footerUrl);
      FooterRenderInfo footerRenderInfo = new FooterRenderInfo(footer, webPage.getLink());
      WebContainerCommand.processWidgets(webContainerContext, footer.getSections(), footerRenderInfo, coreData,
          userSession, themePropertyMap, null);

      // Finalize the controller session (zero out the widget's session data)
      controllerSession.clearAllWidgetData();

      long endRequestTime = System.currentTimeMillis();
      long totalTime = endRequestTime - startRequestTime;
      LOG.info("Processing time: " + totalTime + " ms");

      // Error out if there are no widgets rendered or allowed
      if (!pageRenderInfo.hasWidgets()) {
        LOG.warn("NO WIDGETS - PAGE WILL NOT RENDER");
        // response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
      }

      // Set the final page title
      if (StringUtils.isBlank(pageRenderInfo.getTitle())) {
        pageRenderInfo.setTitle(sitePropertyMap.get("site.name"));
      } else {
        pageRenderInfo.setTitle(pageRenderInfo.getTitle() + " | " + sitePropertyMap.get("site.name"));
      }
      if (sitePropertyMap.containsKey("site.name.keyword")
          && StringUtils.isNotBlank(sitePropertyMap.get("site.name.keyword"))) {
        pageRenderInfo.setTitle(pageRenderInfo.getTitle() + " - " + sitePropertyMap.get("site.name.keyword"));
      }

      // Prepare the Thymeleaf engine main template
      LOG.info("Preparing the Thymeleaf engine...");
      Locale locale = Locale.getDefault();
      Context ctx = new Context(locale);
      ctx.setVariable("VERSION", ApplicationInfo.VERSION);
      ctx.setVariable(CONTEXT_PATH, "");

      String timeZoneProperty = LoadSitePropertyCommand.loadByName("site.timezone");
      if (StringUtils.isNotBlank(timeZoneProperty)) {
        ctx.setVariable("TIMEZONE", TimeZone.getTimeZone(ZoneId.of(timeZoneProperty)));
      } else {
        ctx.setVariable("TIMEZONE", TimeZone.getDefault());
      }
      ctx.setVariable("DATETIME_FORMAT", "MM-dd-yyyy HH:mm");

      // Provide values to the Tomcat web server log
      if (userSession.isLoggedIn()) {
        ctx.setVariable(LOG_USER, String.valueOf(userSession.getUserId()));
      }

      // Allow the layout to use the properties
      ctx.setVariable("systemPropertyMap", systemPropertyMap);
      ctx.setVariable("sitePropertyMap", sitePropertyMap);
      ctx.setVariable("themePropertyMap", themePropertyMap);
      ctx.setVariable("socialPropertyMap", socialPropertyMap);
      ctx.setVariable("analyticsPropertyMap", analyticsPropertyMap);
      ctx.setVariable("ecommercePropertyMap", ecommercePropertyMap);

      if ("/".equals(webPage.getLink())) {
        if ("true".equals(LoadSitePropertyCommand.loadByName("site.newsletter.overlay"))) {
          String headline = LoadSitePropertyCommand.loadByName("site.newsletter.headline");
          String message = LoadSitePropertyCommand.loadByName("site.newsletter.message");
          if (StringUtils.isNotBlank(headline) && StringUtils.isNotBlank(message)) {
            ctx.setVariable(RequestConstants.OVERLAY_HEADLINE, headline);
            ctx.setVariable(RequestConstants.OVERLAY_MESSAGE, message);
          }
        }
      }

      // Determine global items
      LOG.info("Determine the global items...");

      // @todo determine if this is needed still (it is, but until all JSP layouts are removed?)
      ctx.setVariable(SHOW_MAIN_MENU, "true");
      List<MenuTab> menuTabList = LoadMenuTabsCommand.loadActiveIncludeMenuItemList();
      ctx.setVariable(MASTER_MENU_TAB_LIST, menuTabList);

      // @note this is needed globally
      // if (!"container".equals(pageRequest.getSession().getAttribute(SessionConstants.X_VIEW_MODE))) {
      TableOfContents footerStickyLinks = LoadTableOfContentsCommand.loadByUniqueId("footer-sticky-links", false);
      ctx.setVariable(FOOTER_STICKY_LINKS, footerStickyLinks);
      // }

      // Start rendering the page
      ctx.setVariable(PAGE_RENDER_INFO, pageRenderInfo);
      if (thisCollection != null) {
        ctx.setVariable(PAGE_COLLECTION, thisCollection);
      }
      if (thisCollectionCategory != null) {
        ctx.setVariable(PAGE_COLLECTION_CATEGORY, thisCollectionCategory);
      }

      // Determine the custom stylesheets
      LOG.info("Determine the stylesheets...");
      Stylesheet globalStylesheet = LoadStylesheetCommand.loadStylesheetByWebPageId(-1);
      if (globalStylesheet != null) {
        ctx.setVariable("includeGlobalStylesheet", "true");
        ctx.setVariable("includeGlobalStylesheetLastModified", globalStylesheet.getModified().getTime());
      }
      if (webPage != null) {
        Stylesheet pageStylesheet = LoadStylesheetCommand.loadStylesheetByWebPageId(webPage.getId());
        if (pageStylesheet != null) {
          ctx.setVariable("includeStylesheet", pageStylesheet.getWebPageId());
          ctx.setVariable("includeStylesheetLastModified", pageStylesheet.getModified().getTime());
        }
      }

      // Determine the output page requirements (css/scripts/etc)
      String template = "main";
      // if (!"container".equals(pageRequest.getSession().getAttribute(SessionConstants.X_VIEW_MODE))) {
      // For web content with a header and footer
      ctx.setVariable(HEADER_RENDER_INFO, headerRenderInfo);
      ctx.setVariable(FOOTER_RENDER_INFO, footerRenderInfo);
      // }

      // Call the Thymeleaf engine and render the complete page
      LOG.info("Processing the template...");
      String html = templateEngine.process(template, ctx);
      html = html.replaceAll(Pattern.quote("${ctx}"), "");
      LOG.debug("-----------------------------------------------------------------------");
      return html;

    } catch (Exception e) {
      LOG.error("Page error caught: " + e.getMessage(), e);
      return null;
    }
  }
}
