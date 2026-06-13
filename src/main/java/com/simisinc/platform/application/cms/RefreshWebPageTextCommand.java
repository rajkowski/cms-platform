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

package com.simisinc.platform.application.cms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;

/**
 * Orchestrates page text index updates: single-page rebuild, content-dependent rebuild, and full rebuild.
 *
 * @author matt rajkowski
 * @created 6/10/26 1:30 PM
 */
public class RefreshWebPageTextCommand {

  private static Log LOG = LogFactory.getLog(RefreshWebPageTextCommand.class);

  // Statistics for operation tracking
  public static class RebuildStats {
    public long scanned = 0;
    public long updated = 0;
    public long skipped = 0;
    public long failed = 0;

    @Override
    public String toString() {
      return String.format("RebuildStats{scanned=%d, updated=%d, skipped=%d, failed=%d}",
          scanned, updated, skipped, failed);
    }
  }

  /**
   * Refresh page text for a single page
   *
   * @param webPageId the ID of the page to refresh
   * @return true if successful
   */
  public static boolean refreshPageText(long webPageId) {
    if (webPageId == -1) {
      return false;
    }

    try {
      WebPage page = WebPageRepository.findById(webPageId);
      if (page == null) {
        LOG.warn("Page not found for refresh: " + webPageId);
        return false;
      }

      return refreshPageText(page);

    } catch (Exception e) {
      LOG.error("Error refreshing page text for ID: " + webPageId, e);
      return false;
    }
  }

  /**
   * Refresh page text for a specific WebPage record
   *
   * @param webPage the page to refresh
   * @return true if successful
   */
  public static boolean refreshPageText(WebPage webPage) {
    if (webPage == null || webPage.getId() == -1) {
      return false;
    }

    try {
      String generatedText = GenerateWebPageTextCommand.generatePageText(webPage);

      // Just update the field and save without modifying timestamps or triggering events
      WebPageRepository.updatePageText(webPage.getId(), generatedText);

      LOG.debug("Refreshed page text for page ID: " + webPage.getId());
      return true;

    } catch (Exception e) {
      LOG.error("Error refreshing page text for page ID: " + webPage.getId(), e);
      return false;
    }
  }

  /**
   * Refresh all pages that reference a content uniqueId
   *
   * @param contentUniqueId the content uniqueId to find dependent pages
   * @param stats optional RebuildStats object to track results
   * @return number of pages updated
   */
  public static int refreshPagesByContentUniqueId(String contentUniqueId, RebuildStats stats) {
    if (stats == null) {
      stats = new RebuildStats();
    }

    try {
      // Pages which reference the content uniqueId in their page XML or widget preferences
      List<WebPage> candidatePages = WebPageRepository.findByContentUniqueId(contentUniqueId);
      stats.scanned += candidatePages.size();
      Set<Long> refreshedPageIds = new HashSet<>();

      int updatedCount = 0;
      for (WebPage page : candidatePages) {
        if (refreshPageText(page)) {
          updatedCount++;
          stats.updated++;
          refreshedPageIds.add(page.getId());
        } else {
          stats.failed++;
        }
      }

      if (updatedCount > 0) {
        LOG.info("Updated " + updatedCount + " pages for content uniqueId: " + contentUniqueId);
      }

      // Pages which recursively reference the content uniqueId through embedded content
      List<WebPage> indirectlyAffectedPages = WebPageRepository.findIndirectlyAffectedPagesByContentUniqueId(contentUniqueId);
      stats.scanned += indirectlyAffectedPages.size();
      int indirectUpdatedCount = 0;
      for (WebPage page : indirectlyAffectedPages) {
        if (refreshedPageIds.contains(page.getId())) {
          stats.skipped++;
          continue;
        }
        if (refreshPageText(page)) {
          indirectUpdatedCount++;
          updatedCount++;
          stats.updated++;
          refreshedPageIds.add(page.getId());
        } else {
          stats.failed++;
        }
      }

      if (indirectUpdatedCount > 0) {
        LOG.info("Updated " + indirectUpdatedCount + " indirectly affected pages for content uniqueId: " + contentUniqueId);
      }

      return updatedCount;

    } catch (Exception e) {
      LOG.error("Error refreshing pages by content uniqueId: " + contentUniqueId, e);
      stats.failed++;
      return 0;
    }
  }

  /**
   * Refresh all pages that reference a content uniqueId
   */
  public static int refreshPagesByContentUniqueId(String contentUniqueId) {
    return refreshPagesByContentUniqueId(contentUniqueId, null);
  }

  /**
   * Full rebuild of all web pages with batching and statistics
   *
   * @param batchSize number of pages to process per batch
   * @param maxRecursionDepth max depth for embedded reference resolution
   * @return RebuildStats with operation results
   */
  public static RebuildStats rebuildAllPages(int batchSize, int maxRecursionDepth) {
    RebuildStats stats = new RebuildStats();

    if (batchSize <= 0) {
      batchSize = 50;
    }

    try {
      LOG.info("Starting full page text rebuild with batch size: " + batchSize);

      List<WebPage> allPages = WebPageRepository.findAll();
      stats.scanned = allPages.size();

      for (WebPage page : allPages) {
        try {
          String generatedText = GenerateWebPageTextCommand.generatePageText(page, maxRecursionDepth);
          if (generatedText != null) {
            WebPageRepository.updatePageText(page.getId(), generatedText);
            stats.updated++;
          } else {
            stats.skipped++;
          }
        } catch (Exception e) {
          LOG.error("Error processing page ID: " + page.getId(), e);
          stats.failed++;
        }
      }

      LOG.info("Page text rebuild complete: " + stats);
      return stats;

    } catch (Exception e) {
      LOG.error("Error during full page rebuild", e);
      return stats;
    }
  }

  /**
   * Full rebuild of all web pages using default batch size and recursion depth
   */
  public static RebuildStats rebuildAllPages() {
    return rebuildAllPages(50, 3);
  }

}
