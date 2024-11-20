/*
 * Copyright 2024 Matt Rajkowski (https://www.github.com/rajkowski)
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.simisinc.platform.application.cms.ContentHtmlCommand;
import com.simisinc.platform.application.cms.LoadStylesheetCommand;
import com.simisinc.platform.application.cms.LoadTableOfContentsCommand;
import com.simisinc.platform.application.cms.WebContainerLayoutCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.domain.model.cms.TableOfContents;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.MenuTabRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;

class PageTemplateEngineTest {

  private static List<SiteProperty> findByPrefix(String uniqueId) {
    List<SiteProperty> systemPropertyList = new ArrayList<>();
    systemPropertyList.add(new SiteProperty("system.configpath", "."));
    systemPropertyList.add(new SiteProperty("system.filepath", "."));
    systemPropertyList.add(new SiteProperty("system.www.context", ""));
    systemPropertyList.add(new SiteProperty("site.name", "Test Site"));
    systemPropertyList.add(new SiteProperty("maps.service.tiles", "openstreetmap"));
    return systemPropertyList;
  }

  private LoadingCache<String, List<SiteProperty>> sitePropertyListCache;

  @BeforeEach
  public void init() {
    sitePropertyListCache = Caffeine.newBuilder().build(PageTemplateEngineTest::findByPrefix);
  }

  @Test
  void testRender() {

    // Mock the cache manager so the FileSystemCommand works
    try (MockedStatic<CacheManager> cacheManager = mockStatic(CacheManager.class)) {
      cacheManager.when(() -> CacheManager.getLoadingCache(anyString())).thenReturn(sitePropertyListCache);

      // The webapp resources need to be found
      File webAppPath = FileSystemCommand.getFileServerRootPath("src", "main", "webapp");
      Assertions.assertTrue(webAppPath.isDirectory());

      // Mock the repository manager so that a web page to test is returned
      try (MockedStatic<WebPageRepository> webPageRepository = mockStatic(WebPageRepository.class)) {
        // Determine a page to render
        List<WebPage> webPageList = new ArrayList<WebPage>();

        // Define a Widget
        String htmlContent = "<h2>Hello</h2>";
        String contentWidget = "<widget name=\"content\"><html><![CDATA[" + htmlContent
            + "]]></html></widget>";

        String mapWidget = "<widget name=\"map\"><coordinates>10,10</coordinates></widget>";

        // Define the Page XML
        String webPageXml = "<page><section><column>" + contentWidget + mapWidget + "</column></section></page>";
        WebPage webPage = new WebPage("/test", webPageXml);
        webPage.setTitle("Testing");
        webPageList.add(webPage);
        webPageRepository.when(() -> WebPageRepository.findAll()).thenReturn(webPageList);

        try {

          // Make sure the widget library contains the classes to execute
          File widgetLibraryFile = new File(webAppPath, "/WEB-INF/widgets/widget-library.xml");
          Assertions.assertTrue(widgetLibraryFile.isFile());

          // Initialize the page layouts
          Map<String, String> widgetLibrary = WebPageXmlLayoutCommand.init(webAppPath);
          Assertions.assertTrue(!widgetLibrary.isEmpty());
          Assertions.assertNotNull(widgetLibrary.get("content"));

          // Startup the page renderer
          FileTemplateResolver templateResolver = new FileTemplateResolver();
          Assertions.assertNotNull(templateResolver);
          boolean startupSuccess = PageTemplateEngine.startup(templateResolver,
              webAppPath.getAbsolutePath() + File.separator + "WEB-INF" + File.separator + "html-templates" + File.separator,
              widgetLibrary,
              new File(webAppPath.getAbsolutePath() + File.separator + "WEB-INF" + File.separator + "dependencies.json").toURI()
                  .toURL());
          Assertions.assertTrue(startupSuccess);
        } catch (MalformedURLException mue) {
          mue.printStackTrace();
        }

        // Mock the web container so that a header and footer are found
        try (MockedStatic<WebContainerLayoutCommand> webContainerLayoutCommand = mockStatic(WebContainerLayoutCommand.class)) {
          Header header = new Header("Empty");
          Footer footer = new Footer("Empty");
          webContainerLayoutCommand.when(() -> WebContainerLayoutCommand.retrieveHeader(anyString(), any())).thenReturn(header);
          webContainerLayoutCommand.when(() -> WebContainerLayoutCommand.retrieveFooter(anyString(), any())).thenReturn(footer);

          // Mock the menu tabs so that the menu is found
          try (MockedStatic<MenuTabRepository> menuTabRepository = mockStatic(MenuTabRepository.class)) {

            List<MenuTab> menuTabList = new ArrayList<>();
            menuTabRepository.when(() -> MenuTabRepository.findAll()).thenReturn(menuTabList);

            // Mock the sticky footer feature
            try (MockedStatic<LoadTableOfContentsCommand> loadTableOfContentsCommand = mockStatic(LoadTableOfContentsCommand.class)) {
              TableOfContents tableOfContents = new TableOfContents();
              loadTableOfContentsCommand.when(() -> LoadTableOfContentsCommand.loadByUniqueId(anyString(), anyBoolean()))
                  .thenReturn(tableOfContents);

              // Mock the page's custom stylesheet
              try (MockedStatic<LoadStylesheetCommand> loadStylesheetCommand = mockStatic(LoadStylesheetCommand.class)) {
                loadStylesheetCommand.when(() -> LoadStylesheetCommand.loadStylesheetByWebPageId(anyLong())).thenReturn(null);

                // Mock the content repository so that html content is returned
                try (MockedStatic<ContentHtmlCommand> contentHtmlCommand = mockStatic(ContentHtmlCommand.class)) {
                  contentHtmlCommand.when(() -> ContentHtmlCommand.getHtmlFromPreferences(any())).thenReturn(htmlContent);

                  // Render the web page
                  String htmlPage = PageTemplateEngine.render(webPage, webAppPath);
                  Assertions.assertNotNull(htmlPage);
                  // System.out.println(htmlPage);
                  Assertions.assertTrue(htmlPage.contains("<title>Testing | Test Site</title>"));
                  Assertions.assertFalse(htmlPage.contains("\"${") || htmlPage.contains("[${") || htmlPage.contains("#{"));
                  Assertions.assertFalse(htmlPage.contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"\" />"));
                  Assertions.assertFalse(htmlPage.contains("<script src=\"\"></script>"));
                  // Check widget output
                  Assertions.assertTrue(htmlPage.contains("<div class=\"platform-content\">" + htmlContent + "</div>"));
                  Assertions.assertTrue(htmlPage.contains("href=\"/javascript/leaflet"));
                  Assertions.assertTrue(htmlPage.contains("src=\"/javascript/leaflet"));
                }
              }
            }
          }
        }
      }
    }
  }
}
