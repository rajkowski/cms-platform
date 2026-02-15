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

package com.simisinc.platform.presentation.widgets.cms;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveWebPageCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigDecimal;

/**
 * Saves a web page's metadata/info from the visual page editor Info tab
 * Only allows pages the user has permission to edit
 *
 * @author matt rajkowski
 * @created 1/10/26 10:00 AM
 */
public class SaveWebPageInfoAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(SaveWebPageInfoAjax.class);

  public WidgetContext post(WidgetContext context) {

    LOG.debug("SaveWebPageInfoAjax...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      LOG.debug("No permission to save web page info");
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Determine the page link
    String webPageLink = context.getParameter("link");
    LOG.debug("Saving page info for: " + webPageLink);
    if (StringUtils.isBlank(webPageLink)) {
      LOG.debug("Page link is empty");
      context.setJson("{\"success\":false,\"message\":\"Page link is required\"}");
      context.setSuccess(false);
      return context;
    }

    // Retrieve the existing web page
    WebPage page = WebPageRepository.findByLink(webPageLink);
    if (page == null) {
      LOG.debug("Web page not found for link: " + webPageLink);
      context.setJson("{\"success\":false,\"message\":\"Page not found\"}");
      context.setSuccess(false);
      return context;
    }

    // Update the page info fields from the request
    page.setModifiedBy(context.getUserId());
    
    // Title
    String title = context.getParameter("title");
    page.setTitle(StringUtils.trimToNull(title));
    
    // Keywords
    String keywords = context.getParameter("keywords");
    page.setKeywords(StringUtils.trimToNull(keywords));
    
    // Description
    String description = context.getParameter("description");
    page.setDescription(StringUtils.trimToNull(description));
    
    // Image URL (Open Graph)
    String imageUrl = context.getParameter("imageUrl");
    page.setImageUrl(StringUtils.trimToNull(imageUrl));
    
    // Draft (inverse of publish)
    String publishParam = context.getParameter("publish");
    boolean publish = "true".equals(publishParam) || "1".equals(publishParam);
    page.setDraft(!publish);
    
    // Searchable
    String searchableParam = context.getParameter("searchable");
    boolean searchable = "true".equals(searchableParam) || "1".equals(searchableParam);
    page.setSearchable(searchable);
    
    // Show in Sitemap
    String showInSitemapParam = context.getParameter("showInSitemap");
    boolean showInSitemap = "true".equals(showInSitemapParam) || "1".equals(showInSitemapParam);
    page.setShowInSitemap(showInSitemap);
    
    // Sitemap Priority
    String sitemapPriorityParam = context.getParameter("sitemapPriority");
    if (StringUtils.isNotBlank(sitemapPriorityParam)) {
      try {
        BigDecimal priority = new BigDecimal(sitemapPriorityParam);
        page.setSitemapPriority(priority);
      } catch (NumberFormatException e) {
        LOG.debug("Invalid sitemap priority: " + sitemapPriorityParam);
        // Keep existing value
      }
    }
    
    // Sitemap Change Frequency
    String sitemapChangeFrequency = context.getParameter("sitemapChangeFrequency");
    page.setSitemapChangeFrequency(StringUtils.trimToNull(sitemapChangeFrequency));

    try {
      // Save using the SaveWebPageCommand
      WebPage savedPage = SaveWebPageCommand.saveWebPage(page);

      if (savedPage != null) {
        // Build success response
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"success\":true,");
        sb.append("\"message\":\"Page info saved successfully\",");
        sb.append("\"id\":").append(savedPage.getId()).append(",");
        sb.append("\"link\":\"").append(JsonCommand.toJson(savedPage.getLink())).append("\"");
        sb.append("}");
        context.setJson(sb.toString());
        LOG.debug("Page info saved successfully: " + webPageLink);
      } else {
        context.setJson("{\"success\":false,\"message\":\"Failed to save page info\"}");
        context.setSuccess(false);
        LOG.debug("Save returned null for page: " + webPageLink);
      }
    } catch (DataException e) {
      LOG.error("Data validation error saving page info: " + e.getMessage());
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"success\":false,");
      sb.append("\"message\":\"").append(JsonCommand.toJson(e.getMessage())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
      context.setSuccess(false);
    } catch (Exception e) {
      LOG.error("Error saving page info: " + e.getMessage(), e);
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"success\":false,");
      sb.append("\"message\":\"").append(JsonCommand.toJson("Error saving page info: " + e.getMessage())).append("\"");
      sb.append("}");
      context.setJson(sb.toString());
      context.setSuccess(false);
    }

    return context;
  }
}
