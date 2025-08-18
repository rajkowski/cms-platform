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

  public static Header retrieveHeader(String layout, URL url) {
    String cacheName = ("header.default".equals(layout) ? CacheManager.WEBSITE_HEADER
        : CacheManager.WEBSITE_PLAIN_HEADER);
    Header header = (Header) CacheManager.getFromObjectCache(cacheName);
    if (header == null) {
      header = retrieveHeaderFromDatabase(layout);
      if (header == null) {
        header = XMLHeaderLoader.loadFromURL(layout, url);
      }
      if (header == null) {
        return null;
      }
      CacheManager.addToObjectCache(cacheName, header);
    }
    return header;
  }

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

  public static Footer retrieveFooter(String layout, URL url) {
    Footer footer = (Footer) CacheManager.getFromObjectCache(CacheManager.WEBSITE_FOOTER);
    if (footer == null) {
      footer = retrieveFooterFromDatabase(layout);
      if (footer == null) {
        footer = XMLFooterLoader.loadFromURL(layout, url);
      }
      if (footer == null) {
        return null;
      }
      CacheManager.addToObjectCache(CacheManager.WEBSITE_FOOTER, footer);
    }
    return footer;
  }

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
