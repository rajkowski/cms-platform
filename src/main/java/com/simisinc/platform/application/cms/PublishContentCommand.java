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

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
  public static boolean publishContent(String uniqueId) throws DataException {

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
    ContentRepository.publish(content);

    LOG.debug("Published draft content for: " + uniqueId);
    return true;
  }
}
