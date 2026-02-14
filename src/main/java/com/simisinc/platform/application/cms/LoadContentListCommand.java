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

import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ContentSpecification;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Loads a paginated list of content blocks with optional search filtering
 *
 * @author matt rajkowski
 * @created 2/7/26 12:00 PM
 */
public class LoadContentListCommand {

  private static Log LOG = LogFactory.getLog(LoadContentListCommand.class);

  /**
   * Retrieves a paginated list of content blocks with optional search filtering
   *
   * @param searchTerm Optional search term to filter content by title or text
   * @param offset     The starting position for pagination (0-based)
   * @param limit      The maximum number of records to return
   * @return List of Content objects with search highlighting, or null if no results
   */
  public static List<Content> loadContentList(String searchTerm, int offset, int limit) {
    return loadContentList(searchTerm, offset, limit, null);
  }

  /**
   * Retrieves a paginated list of content blocks with optional search filtering and custom sort order
   *
   * @param searchTerm Optional search term to filter content by title or text
   * @param offset     The starting position for pagination (0-based)
   * @param limit      The maximum number of records to return
   * @param sortBy     Optional sort order: "alphabetical" or "recent" (default)
   * @return List of Content objects with search highlighting, or null if no results
   */
  public static List<Content> loadContentList(String searchTerm, int offset, int limit, String sortBy) {
    // Create ContentSpecification with search term if provided
    ContentSpecification specification = new ContentSpecification();
    if (StringUtils.isNotBlank(searchTerm)) {
      specification.setSearchTerm(searchTerm.trim());
    }

    // Create DataConstraints for pagination
    DataConstraints constraints = new DataConstraints();
    constraints.setPageSize(limit);

    // Convert offset to page number (1-based)
    if (limit > 0) {
      constraints.setPageNumber((offset / limit) + 1);
    } else {
      constraints.setPageNumber(1);
    }

    // Set sort order based on sortBy parameter
    if ("alphabetical".equalsIgnoreCase(sortBy)) {
      constraints.setColumnToSortBy("content_unique_id", "ASC");
    } else {
      // Default: most recently modified first
      constraints.setColumnToSortBy("modified", "DESC");
    }

    // Query repository
    List<Content> contentList = ContentRepository.findAll(specification, constraints);

    if (contentList == null) {
      LOG.debug("No content found for search term: " + searchTerm);
    } else {
      LOG.debug("Found " + contentList.size() + " content blocks");
    }

    return contentList;
  }
}
