/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.WebContainer;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.persistence.cms.WebContainerRepository;
import com.simisinc.platform.presentation.controller.Footer;
import com.simisinc.platform.presentation.controller.Header;
import com.simisinc.platform.presentation.controller.XMLFooterLoader;
import com.simisinc.platform.presentation.controller.XMLHeaderLoader;

/**
 * Loads header and footer objects
 *
 * @author matt rajkowski
 * @created 2/8/21 5:06 PM
 */
public class WebContainerLayoutCommand {

  private static Log LOG = LogFactory.getLog(WebContainerLayoutCommand.class);
  private static final String LAST_VALID_HEADER_PREFIX = "LastValidatedHeader:";
  private static final String LAST_VALID_FOOTER_PREFIX = "LastValidatedFooter:";

  /** 
   * Returns the cache key for the specified header layout name.
   *
   * @param layoutName the name of the header layout
   * @return the cache key for the header layout
   */
  public static String headerCacheKey(String layoutName) {
    if ("header.default".equals(layoutName)) {
      return CacheManager.WEBSITE_HEADER;
    }
    if ("header.plain".equals(layoutName)) {
      return CacheManager.WEBSITE_PLAIN_HEADER;
    }
    return CacheManager.WEBSITE_HEADER + ":" + layoutName;
  }

  /** 
   * Retrieves the header for the specified layout, either from the cache, the database, or the provided URL.
   *
   * @param layout the name of the header layout
   * @param url the URL to load the header from if not found in the cache or database
   * @return the Header object for the specified layout, or null if it could not be retrieved
   */
  public static Header retrieveHeader(String layout, URL url) {
    String cacheName = headerCacheKey(layout);
    Header header = (Header) CacheManager.getCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, cacheName);
    if (header == null) {
      header = retrieveHeaderFromDatabase(layout);
      if (header == null) {
        header = XMLHeaderLoader.loadFromURL(layout, url);
      }
      if (header == null) {
        header = (Header) CacheManager.getCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, LAST_VALID_HEADER_PREFIX + layout);
        if (header == null) {
          return null;
        }
        LOG.warn("Retaining the last validated header definition for " + layout);
      }
      CacheManager.putCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, cacheName, header);
      CacheManager.putCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, LAST_VALID_HEADER_PREFIX + layout, header);
    }
    return header;
  }

  /** 
   * Retrieves the header for the specified layout from the database.
   *
   * @param layout the name of the header layout
   * @return the Header object for the specified layout, or null if it could not be retrieved from the database
   */
  private static Header retrieveHeaderFromDatabase(String layout) {
    // Try the database
    WebContainer container = WebContainerRepository.findByName(layout);
    if (container == null) {
      return null;
    }
    try {
      // Process the header document
      return XMLHeaderLoader.addFromXml(container);
    } catch (Exception e) {
      LOG.error(e);
    }
    return null;
  }

  /** 
   * Returns the cache key for the specified footer layout name.
   *
   * @param layoutName the name of the footer layout
   * @return the cache key for the footer layout
   */
  public static String footerCacheKey(String layoutName) {
    if ("footer.default".equals(layoutName)) {
      return CacheManager.WEBSITE_FOOTER;
    }
    return CacheManager.WEBSITE_FOOTER + ":" + layoutName;
  }

  /** 
   * Retrieves the footer for the specified layout, either from the cache, the database, or the provided URL.
   *
   * @param layout the name of the footer layout
   * @param url the URL to load the footer from if not found in the cache or database
   * @return the Footer object for the specified layout, or null if it could not be retrieved
   */
  public static Footer retrieveFooter(String layout, URL url) {
    if (layout == null || layout.isBlank()) {
      layout = "footer.default";
    }
    String cacheName = footerCacheKey(layout);
    Footer footer = (Footer) CacheManager.getCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, cacheName);
    if (footer == null) {
      footer = retrieveFooterFromDatabase(layout);
      if (footer == null) {
        footer = XMLFooterLoader.loadFromURL(layout, url);
      }
      if (footer == null) {
        footer = (Footer) CacheManager.getCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, LAST_VALID_FOOTER_PREFIX + layout);
        if (footer == null) {
          if ("footer.default".equals(layout)) {
            return null;
          }
          LOG.warn("Selected footer could not be loaded: " + layout + "; using footer.default");
          return retrieveFooter("footer.default", url);
        }
        LOG.warn("Retaining the last validated footer definition for " + layout);
      }
      CacheManager.putCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, cacheName, footer);
      CacheManager.putCurrentWorkspaceValue(CacheManager.OBJECT_CACHE, LAST_VALID_FOOTER_PREFIX + layout, footer);
    }
    return footer;
  }

  /** 
   * Retrieves the footer for the specified layout from the database.
   *
   * @param layout the name of the footer layout
   * @return the Footer object for the specified layout, or null if it could not be retrieved from the database
   */
  private static Footer retrieveFooterFromDatabase(String layout) {
    // Try the database
    WebContainer container = WebContainerRepository.findByName(layout);
    if (container == null) {
      return null;
    }
    try {
      // Process the footer document
      return XMLFooterLoader.addFromXml(container);
    } catch (Exception e) {
      LOG.error(e);
    }
    return null;
  }
}
