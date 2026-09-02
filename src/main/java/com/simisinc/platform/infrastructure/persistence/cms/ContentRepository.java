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

package com.simisinc.platform.infrastructure.persistence.cms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.application.cms.ResolveContentDirectivesCommand;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.infrastructure.cache.CacheManager;

/**
 * Persists and retrieves content objects
 *
 * @author matt rajkowski
 * @created 4/8/18 4:33 PM
 */
public class ContentRepository {

  private static Log LOG = LogFactory.getLog(ContentRepository.class);

  private static String TABLE_NAME = "content";
  private static String[] PRIMARY_KEY = new String[] { "content_id" };

  private static DataResult<Content> query(ContentSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("content.*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("content_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("content_unique_id = ?", specification.getUniqueId());
      }
      if (StringUtils.isNotBlank(specification.getSearchTerm())) {
        String quotedSearchTerm = "\"" + specification.getSearchTerm().trim() + "\"";
        String searchTermSeparated = specification.getSearchTerm().trim().replaceAll("\\s+", " OR ");
        String searchToUse = quotedSearchTerm + " OR " + searchTermSeparated;
        String searchTermPattern = "%" + specification.getSearchTerm().trim() + "%";

        select.SELECT(
            "ts_headline('english', content_text, websearch_to_tsquery('content_stem', ?), 'StartSel=${b}, StopSel=${/b}, MaxWords=30, MinWords=15, ShortWord=3, HighlightAll=FALSE, MaxFragments=2, FragmentDelimiter=\" ... \"') AS highlight",
          (Object[]) new Object[] { specification.getSearchTerm().trim() });
        select.SELECT("ts_rank_cd(tsv, websearch_to_tsquery('content_stem', ?)) AS rank", (Object[]) new Object[] { searchToUse });

        select.AND("(tsv @@ websearch_to_tsquery('content_stem', ?) OR content_unique_id LIKE ?)",
            searchToUse, searchTermPattern);
        select.ORDER_BY("rank DESC, content.modified DESC");
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(ContentRepository::buildRecord);
  }

  public static Content findByUniqueId(String contentUniqueId) {
    if (StringUtils.isBlank(contentUniqueId)) {
      return null;
    }
    return DB.SELECT("content.*")
        .FROM(TABLE_NAME)
        .WHERE("content_unique_id = ?", contentUniqueId)
        .returnRecord(ContentRepository::buildRecord);
  }

  public static List<Content> findAll() {
    return findAll(null, null);
  }

  public static List<Content> findAll(ContentSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("content_unique_id");
    return query(specification, constraints).getRecords();
  }

  public static Content save(Content record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static Content add(Content record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("content_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("content", StringUtils.trimToNull(record.getContent()))
        .FIELD("content_text",
            HtmlCommand.text(ResolveContentDirectivesCommand.resolveDirectives(StringUtils.trimToNull(record.getContent()))))
        .FIELD("draft_content", StringUtils.trimToNull(record.getDraftContent()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static Content update(Content record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("content", StringUtils.trimToNull(record.getContent()))
        .SET("content_text",
            HtmlCommand.text(ResolveContentDirectivesCommand.resolveDirectives(StringUtils.trimToNull(record.getContent()))))
        .SET("draft_content", StringUtils.trimToNull(record.getDraftContent()))
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (update.WHERE("content_unique_id = ?", StringUtils.trimToNull(record.getUniqueId())).execute()) {
      CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void publish(Content record) {
    if (StringUtils.isBlank(record.getUniqueId())) {
      return;
    }
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("content", "draft_content")
        .SET("draft_content", (String) null)
        .SET("content_text", HtmlCommand
            .text(ResolveContentDirectivesCommand.resolveDirectives(StringUtils.trimToNull(record.getContent()))))
        .WHERE("draft_content IS NOT NULL AND content_unique_id = ?", record.getUniqueId());
    if (update.execute()) {
      CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, record.getUniqueId());
    }
  }

  /**
   * Finds all content records whose content embeds the given uniqueId via ${uniqueId:...} directive
   */
  public static List<Content> findAllEmbeddingReferences(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    DataResult<Content> result = DB.SELECT("content.*")
        .FROM(TABLE_NAME)
        .WHERE("content LIKE ?", "%${uniqueId:" + uniqueId + "}%")
        .returnDataResult(ContentRepository::buildRecord);
    return result.getRecords();
  }

  /**
   * Updates content_text for all records that embed the given uniqueId
   */
  public static void updateEmbeddingContentText(String uniqueId) {
    List<Content> embeddingRecords = findAllEmbeddingReferences(uniqueId);
    if (embeddingRecords == null || embeddingRecords.isEmpty()) {
      return;
    }
    for (Content embedding : embeddingRecords) {
      Update update = DB.UPDATE(TABLE_NAME)
          .SET("content_text",
              HtmlCommand.text(
                  ResolveContentDirectivesCommand.resolveDirectives(StringUtils.trimToNull(embedding.getContent()))))
          .SET("modified", new Timestamp(System.currentTimeMillis()))
          .WHERE("content_unique_id = ?", embedding.getUniqueId());
      if (update.execute()) {
        CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, embedding.getUniqueId());
        LOG.debug("Updated content_text for embedding content: " + embedding.getUniqueId());
      }
    }
  }

  public static void removeDraft(Content record) {
    if (record == null || StringUtils.isBlank(record.getUniqueId())) {
      return;
    }
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("draft_content", (String) null)
        .WHERE("content_unique_id = ?", record.getUniqueId());
    if (update.execute()) {
      CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, record.getUniqueId());
    }
  }

  private static Content buildRecord(ResultSet rs) {
    try {
      Content record = new Content();
      record.setId(rs.getLong("content_id"));
      record.setUniqueId(rs.getString("content_unique_id"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setContent(rs.getString("content"));
      record.setDraftContent(rs.getString("draft_content"));
      record.setContentAsText(rs.getString("content_text"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      if (DB.hasColumn(rs, "highlight")) {
        record.setHighlight(rs.getString("highlight"));
      }
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
