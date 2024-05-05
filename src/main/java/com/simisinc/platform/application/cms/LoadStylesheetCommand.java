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

package com.simisinc.platform.application.cms;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.simisinc.platform.domain.model.cms.Stylesheet;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.web.WebApp;

/**
 * Loads the stylesheet for a web page
 *
 * @author matt rajkowski
 * @created 2/1/2021 8:00 AM
 */
public class LoadStylesheetCommand {

  private static Log LOG = LogFactory.getLog(LoadStylesheetCommand.class);
  private static long GLOBAL_CSS_PAGE = -1L;
  private static String GLOBAL_CSS_FILE = "/css/global.css";

  private static List<Long> webPageIdNotFoundList = new ArrayList<>();
  private static Boolean hasGlobalStylesheetFile;

  /** Tracks whether the web application was bundled with a global stylesheet */
  public static void init() {
    URL globalStylesheetURL = WebApp.getResource(GLOBAL_CSS_FILE);
    hasGlobalStylesheetFile = globalStylesheetURL != null;
  }

  /**
   * Returns the requested stylesheet from database or a bundled file, if either exists
   * @param webPageId
   * @return
   */
  public static Stylesheet loadStylesheetByWebPageId(long webPageId) {
    // Before any processing see if function can fail-fast if stylesheet doesn't exist
    if (webPageId == GLOBAL_CSS_PAGE) {
      // Global stylesheet might have a file
      if (!hasGlobalStylesheetFile.booleanValue() && webPageIdNotFoundList.contains(GLOBAL_CSS_PAGE)) {
        LOG.debug("Avoided global stylesheet hit");
        return null;
      }
    } else {
      // Page stylesheet
      if (webPageIdNotFoundList.contains(webPageId)) {
        LOG.debug("Avoided stylesheet hit for webPageId: " + webPageId);
        return null;
      }
    }

    // Use the cache loader to find the stylesheet
    Stylesheet thisStylesheet = (Stylesheet) CacheManager.getLoadingCache(CacheManager.STYLESHEET_WEB_PAGE_ID_CACHE).get(webPageId);
    if (thisStylesheet != null && !StringUtils.isBlank(thisStylesheet.getCss())) {
      LOG.debug("Stylesheet cache found for webPageId: " + webPageId);
      return thisStylesheet;
    }

    // Additional check for the global stylesheet
    if (webPageId == GLOBAL_CSS_PAGE && hasGlobalStylesheetFile.booleanValue()) {
      thisStylesheet = possibleGlobalStylesheet();
      if (thisStylesheet != null) {
        return thisStylesheet;
      }
    }

    // If no style was found then track the webPageId (invalidate the item when a style is later set)
    markStylesheetExists(webPageId, false);

    return null;
  }

  /** The webPageIdFoundList is used to avoid hitting the database on a cache hit and the record is known not to exist */
  public static void markStylesheetExists(long webPageId, boolean exists) {
    if (exists) {
      // it will come from cache
      webPageIdNotFoundList.remove(webPageId);
    } else {
      // determine if cache hit should be avoided
      if (webPageId != GLOBAL_CSS_PAGE || !hasGlobalStylesheetFile.booleanValue()) {
        // avoid cache hit
        webPageIdNotFoundList.add(webPageId);
      }
    }
  }

  /**
   * A global stylesheet may have been included with the web application as a file,
   * so use it except when it exists in the database
   * @return
   */
  private static synchronized Stylesheet possibleGlobalStylesheet() {
    LOG.info("Checking for external CSS file: " + GLOBAL_CSS_FILE);
    URL defaultStylesheetURL = WebApp.getResource(GLOBAL_CSS_FILE);
    if (defaultStylesheetURL != null) {
      try (InputStream inputStream = WebApp.getResourceAsStream(GLOBAL_CSS_FILE)) {
        String css = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        if (StringUtils.isBlank(css)) {
          return null;
        }
        LOG.debug("Stylesheet loaded: " + GLOBAL_CSS_FILE);
        Stylesheet stylesheet = new Stylesheet();
        stylesheet.setWebPageId(-1);
        stylesheet.setModified(new Timestamp(System.currentTimeMillis()));
        stylesheet.setCss(css);
        // Cache it
        Cache cache = CacheManager.getCache(CacheManager.STYLESHEET_WEB_PAGE_ID_CACHE);
        cache.put(GLOBAL_CSS_PAGE, stylesheet);
        return stylesheet;
      } catch (Exception e) {
        LOG.error("Could not read globalCssFile: " + GLOBAL_CSS_FILE);
      }
    }
    LOG.debug("Stylesheet was not found: " + GLOBAL_CSS_FILE);
    return null;
  }

}
