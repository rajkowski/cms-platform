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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.zeroio.platform.domain.model.cms.ContentVersion;
import com.zeroio.platform.infrastructure.persistence.cms.ContentVersionRepository;
import com.zeroio.platform.infrastructure.scheduler.cms.RefreshWebPageTextIndexJob;

/**
 * Validates and saves content objects
 *
 * @author matt rajkowski
 * @created 4/26/18 11:12 AM
 */
public class SaveContentCommand {

  public static final String allowedChars = "abcdefghijklmnopqrstuvwyxz-1234567890";
  private static Log LOG = LogFactory.getLog(SaveContentCommand.class);

  /**
   * Helper method to save a content version
   *
   * @param contentId the content ID
   * @param contentText the content text to save
   * @param userId the user ID who created/modified the content
   * @param notes optional notes for this version
   */
  private static void saveContentVersion(long contentId, String contentText, long userId, String notes) {
    if (contentId > 0 && StringUtils.isNotBlank(contentText)) {
      ContentVersion version = new ContentVersion();
      version.setContentId(contentId);
      version.setContent(contentText);
      version.setCreatedBy(userId);
      version.setNotes(notes);
      ContentVersionRepository.save(version);
    }
  }

  /**
   * Saves content with HTML sanitization and proper metadata tracking
   *
   * @param contentBean the content object to save
   * @param isDraft whether to save as draft (true) or publish (false)
   * @return the saved content object
   * @throws DataException if validation fails
   */
  public static Content saveContent(Content contentBean, boolean isDraft) throws DataException {

    // Validate required fields
    if (contentBean == null) {
      throw new DataException("Content object is required");
    }
    if (StringUtils.isBlank(contentBean.getUniqueId())) {
      throw new DataException("Content uniqueId is required");
    }
    if (contentBean.getCreatedBy() == -1) {
      throw new DataException("The user saving this content was not set");
    }

    // Determine which content to clean (draft or published)
    String contentToClean = isDraft ? contentBean.getDraftContent() : contentBean.getContent();
    if (StringUtils.isBlank(contentToClean)) {
      throw new DataException("Content is required");
    }

    // Clean the content using HtmlCommand
    String cleanedContent = HtmlCommand.cleanContent(contentToClean);

    // Load existing content or create new
    Content content = ContentRepository.findByUniqueId(contentBean.getUniqueId());
    boolean isNewContent = (content == null);

    if (isNewContent) {
      content = new Content();
      content.setUniqueId(contentBean.getUniqueId());
      content.setCreatedBy(contentBean.getCreatedBy());
    } else {
      // Before updating published content, save the existing published content to content_versions table
      // Only save version if we're publishing (not just saving a draft) and there's existing published content
      if (!isDraft && StringUtils.isNotBlank(content.getContent())) {
        long versionUserId = content.getModifiedBy() != -1 ? content.getModifiedBy() : content.getCreatedBy();
        saveContentVersion(content.getId(), content.getContent(), versionUserId, "Version saved before update");
      }
    }

    // Determine if the content is immediately published or saved as draft
    if (isDraft) {
      // Save as draft (updates draft_content field only)
      content.setDraftContent(cleanedContent);
    } else {
      // Publish it (updates content field, clears draft_content)
      content.setContent(cleanedContent);
      content.setDraftContent(null);
    }

    // Track modified_by user
    content.setModifiedBy(contentBean.getModifiedBy() != -1 ? contentBean.getModifiedBy() : contentBean.getCreatedBy());

    // Save to repository (timestamps are handled by the database)
    Content saved = ContentRepository.save(content);

    // If content was published (not draft), refresh dependent web pages asynchronously
    if (!isDraft && saved != null) {
      LOG.debug("Enqueuing refresh for web pages dependent on content: " + saved.getUniqueId());
      ContentRepository.updateEmbeddingContentText(saved.getUniqueId());
      RefreshWebPageTextIndexJob.enqueueForContent(saved.getUniqueId());
    }

    return saved;
  }

  /**
   * Legacy method for backward compatibility
   *
   * @param contentUniqueId the unique identifier for the content
   * @param contentHtml the HTML content to save
   * @param userId the user ID performing the save
   * @param publish whether to publish (true) or save as draft (false)
   * @return the saved content object
   * @throws DataException if validation fails
   */
  public static Content saveSafeContent(String contentUniqueId, String contentHtml, long userId, boolean publish)
      throws DataException {

    if (contentHtml == null) {
      throw new DataException("Content is required");
    }

    // Clean the content
    String cleanedContent = HtmlCommand.cleanContent(contentHtml);

    // Load existing content or create new
    Content content = ContentRepository.findByUniqueId(contentUniqueId);
    boolean isNewContent = (content == null);

    if (isNewContent) {
      content = new Content();
      content.setUniqueId(contentUniqueId);
    } else {
      // Before updating published content, save the existing published content to content_versions table
      if (publish && StringUtils.isNotBlank(content.getContent())) {
        saveContentVersion(content.getId(), content.getContent(), userId, "Version saved before update");
      }
    }

    // Determine if the content is immediately published
    if (publish) {
      // Publish it
      content.setContent(cleanedContent);
      content.setDraftContent(null);
    } else {
      // Save as draft
      content.setDraftContent(cleanedContent);
    }
    content.setCreatedBy(userId);
    content.setModifiedBy(userId);
    Content saved = ContentRepository.save(content);
    if (saved != null && publish) {
      LOG.debug("Enqueuing refresh for web pages dependent on content: " + saved.getUniqueId());
      ContentRepository.updateEmbeddingContentText(saved.getUniqueId());
      RefreshWebPageTextIndexJob.enqueueForContent(saved.getUniqueId());
    }
    return saved;
  }

}
