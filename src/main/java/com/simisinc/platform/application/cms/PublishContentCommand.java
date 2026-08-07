/*
 * Copyright 2026 Matt Rajkowski
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.events.cms.WebPageUpdatedEvent;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.workflow.WorkflowManager;
import com.zeroio.platform.domain.model.cms.ContentVersion;
import com.zeroio.platform.infrastructure.persistence.cms.ContentVersionRepository;
import com.zeroio.platform.infrastructure.scheduler.cms.RefreshWebPageTextIndexJob;

/**
 * Publishes draft content to live
 *
 * @author matt rajkowski
 * @created 2/7/26 12:00 PM
 */
public class PublishContentCommand {

  private static Log LOG = LogFactory.getLog(PublishContentCommand.class);

  /**
   * Publishes draft content to live by atomically moving draft_content to content field
   *
   * @param uniqueId the unique identifier of the content to publish
   * @return true if publish was successful
   * @throws DataException if content not found or no draft exists
   */
  public static boolean publishContent(String uniqueId, long userId) throws DataException {

    // Validate input
    if (StringUtils.isBlank(uniqueId)) {
      throw new DataException("Content uniqueId is required");
    }

    // Load content by uniqueId
    Content content = ContentRepository.findByUniqueId(uniqueId);
    if (content == null) {
      throw new DataException("Content not found: " + uniqueId);
    }

    // Verify draft content exists
    if (StringUtils.isBlank(content.getDraftContent())) {
      throw new DataException("No draft content to publish for: " + uniqueId);
    }

    // Publish using repository's atomic update
    // This atomically moves draft_content to content and clears draft_content
    boolean published = publishContent(content, userId, null);
    if (!published) {
      throw new DataException("Failed to publish content for: " + uniqueId);
    }

    LOG.debug("Published draft content for: " + uniqueId);

    // Refresh dependent web pages asynchronously
    RefreshWebPageTextIndexJob.enqueueForContent(uniqueId);

    return true;
  }

  /**
   * Publishes draft content to live by atomically moving draft_content to content field
   *
   * @param content the content to publish
   * @param userId the ID of the user performing the publish
   * @param referringResourcePath the path of the referring web page, if any
   * @return true if publish was successful
   */
  public static boolean publishContent(Content content, long userId, String referringResourcePath) {
    // Verify draft content exists
    if (StringUtils.isBlank(content.getDraftContent())) {
      return false;
    }

    // Make a content version with the original content
    ContentVersion version = new ContentVersion();
    version.setContentId(content.getId());
    version.setContent(content.getContent());
    version.setCreatedBy(userId);
    version.setNotes("Version saved before publishing update");
    ContentVersionRepository.save(version);

    // Publish the content
    ContentRepository.publish(content);

    // Use the related web page if there is one
    WebPage webPage = LoadWebPageCommand.loadByLink(referringResourcePath);
    if (webPage != null) {
      // Mark the web page as modified
      WebPageRepository.markAsModifiedAndFindable(webPage, userId);

      // Trigger any necessary events or notifications
      WorkflowManager.triggerWorkflowForEvent(new WebPageUpdatedEvent(webPage, userId));
    }
    return true;
  }
}
