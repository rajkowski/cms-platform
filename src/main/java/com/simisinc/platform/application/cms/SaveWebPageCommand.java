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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.events.cms.WebPagePublishedEvent;
import com.simisinc.platform.domain.events.cms.WebPageUpdatedEvent;
import com.simisinc.platform.domain.model.cms.SitemapChangeFrequencyOptions;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.workflow.WorkflowManager;
import com.zeroio.platform.domain.model.cms.WebPageVersion;
import com.zeroio.platform.infrastructure.persistence.cms.WebPageVersionRepository;

/**
 * Validates and saves web page objects
 *
 * @author matt rajkowski
 * @created 5/4/18 6:21 PM
 */
public class SaveWebPageCommand {

  private static Log LOG = LogFactory.getLog(SaveWebPageCommand.class);

  public static WebPage saveWebPage(WebPage webPageBean) throws DataException {

    // Validate the required fields
    StringBuilder errorMessages = new StringBuilder();
    if (StringUtils.isBlank(webPageBean.getLink())) {
      errorMessages.append("A link is required");
    }

    // Link requirements
    if (StringUtils.isNotBlank(webPageBean.getLink())) {
      // remove whitespace
      webPageBean.setLink(webPageBean.getLink().trim());
      // validate external links
      if (webPageBean.getLink().startsWith("http:") || webPageBean.getLink().startsWith("https:")) {
        if (UrlCommand.isUrlValid(webPageBean.getLink())) {
          errorMessages.append("The link cannot be external");
        }
      } else if (!webPageBean.getLink().startsWith("/")) {
        errorMessages.append("Link must start with a /");
      }
    }

    // Redirect requirements
    if (StringUtils.isNotBlank(webPageBean.getRedirectUrl())) {
      // remove whitespace
      webPageBean.setRedirectUrl(webPageBean.getRedirectUrl().trim());
      // validate external links
      if (webPageBean.getRedirectUrl().startsWith("http:") || webPageBean.getRedirectUrl().startsWith("https:")) {
        if (!UrlCommand.isUrlValid(webPageBean.getRedirectUrl())) {
          errorMessages.append("The redirect link formatting did not validate");
        }
      } else if (!webPageBean.getRedirectUrl().startsWith("/")) {
        errorMessages.append("Redirect must start with a /");
      }
      // Compare the link and redirect
      if (StringUtils.isNotBlank(webPageBean.getLink()) &&
          StringUtils.isNotBlank(webPageBean.getRedirectUrl()) &&
          webPageBean.getLink().equals(webPageBean.getRedirectUrl())) {
        errorMessages.append("A link cannot redirect to itself");
      }
    }

    // Sitemap priority
    if (webPageBean.getSitemapPriority() != null &&
        (webPageBean.getSitemapPriority().doubleValue() > 1.0
            || webPageBean.getSitemapPriority().doubleValue() < 0.0)) {
      errorMessages.append("Sitemap priority must be in the rang 0.0 - 1.0 (0.5 is the default)");
    }

    // Sitemap change frequency
    if (StringUtils.isNotBlank(webPageBean.getSitemapChangeFrequency())
        && !SitemapChangeFrequencyOptions.map.containsKey(webPageBean.getSitemapChangeFrequency())) {
      errorMessages.append("Sitemap change frequency choice is unavailable");
    }

    if (!errorMessages.isEmpty()) {
      throw new DataException("Please check the form and try again:\n" + errorMessages.toString());
    }

    // Transform the fields and store...
    WebPage webPage;
    if (webPageBean.getId() > -1) {
      LOG.debug("Saving an existing record... ");
      webPage = WebPageRepository.findById(webPageBean.getId());
      if (webPage == null) {
        throw new DataException("The existing record could not be found");
      }
      // Capture the current XML before replacing it so edits can be restored later.
      if (StringUtils.isNotBlank(webPage.getPageXml()) &&
          !Strings.CS.equals(webPage.getPageXml(), webPageBean.getPageXml())) {
        long versionUserId = webPageBean.getModifiedBy() > 0 ? webPageBean.getModifiedBy() : webPageBean.getCreatedBy();
        saveWebPageVersion(webPage.getId(), webPage.getPageXml(), versionUserId, "Version saved before update");
      }
    } else {
      LOG.debug("Saving a new record... ");
      webPage = new WebPage();
      webPage.setEnabled(true);
    }
    webPage.setCreatedBy(webPageBean.getCreatedBy());
    webPage.setModifiedBy(webPageBean.getModifiedBy() > 0 ? webPageBean.getModifiedBy() : webPageBean.getCreatedBy());
    webPage.setLink(webPageBean.getLink());
    webPage.setRedirectUrl(webPageBean.getRedirectUrl());
    webPage.setTitle(webPageBean.getTitle());
    webPage.setKeywords(webPageBean.getKeywords());
    webPage.setDescription(webPageBean.getDescription());
    webPage.setImageUrl(webPageBean.getImageUrl());
    webPage.setComments(webPageBean.getComments());
    webPage.setPageXml(webPageBean.getPageXml());
    webPage.setSearchable(webPageBean.getSearchable());
    webPage.setShowInSitemap(webPageBean.getShowInSitemap());
    webPage.setDraft(webPageBean.getDraft());
    webPage.setSitemapPriority(webPageBean.getSitemapPriority());
    webPage.setSitemapChangeFrequency(webPageBean.getSitemapChangeFrequency());
    webPage.setTags(webPageBean.getTags());
    WebPage result = WebPageRepository.save(webPage);

    if (result != null) {
      // Refresh page text for search indexing
      RefreshWebPageTextCommand.refreshPageText(result);

      // Check for events
      boolean isNewWebPage = (webPageBean.getId() == -1 || webPageBean.getModified() == null);
      boolean justUpdatedInTheLastDay = !isNewWebPage &&
          webPage.getModified() != null &&
          DateCommand.isHoursOld(webPage.getModified(), 10);
      // Trigger events
      if (isNewWebPage) {
        WorkflowManager.triggerWorkflowForEvent(new WebPagePublishedEvent(result));
      } else if (justUpdatedInTheLastDay) {
        WorkflowManager.triggerWorkflowForEvent(new WebPageUpdatedEvent(result, webPageBean.getModifiedBy()));
      }
    }
    return result;
  }

  private static void saveWebPageVersion(long webPageId, String pageXml, long userId, String notes) {
    if (webPageId > 0 && StringUtils.isNotBlank(pageXml)) {
      WebPageVersion version = new WebPageVersion();
      version.setWebPageId(webPageId);
      version.setPageXml(pageXml);
      version.setCreatedBy(userId);
      version.setNotes(notes);
      WebPageVersionRepository.save(version);
    }
  }
}
