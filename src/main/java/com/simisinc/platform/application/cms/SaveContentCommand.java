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

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    if (content == null) {
      LOG.debug("Saving new content record...");
      content = new Content();
      content.setUniqueId(contentBean.getUniqueId());
      content.setCreatedBy(contentBean.getCreatedBy());
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
    return ContentRepository.save(content);
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
  public static Content saveSafeContent(String contentUniqueId, String contentHtml, long userId, boolean publish) throws DataException {

    if (contentHtml == null) {
      throw new DataException("Content is required");
    }

    // Clean the content
    String cleanedContent = HtmlCommand.cleanContent(contentHtml);

    // Save the content
    Content content = ContentRepository.findByUniqueId(contentUniqueId);
    if (content == null) {
      LOG.debug("Saving new content record...");
      content = new Content();
      content.setUniqueId(contentUniqueId);
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
    return ContentRepository.save(content);

  }

}
