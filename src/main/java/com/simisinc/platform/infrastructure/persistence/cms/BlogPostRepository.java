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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.domain.model.cms.Blog;
import com.simisinc.platform.domain.model.cms.BlogPost;
import com.simisinc.platform.presentation.controller.DataConstants;

/**
 * Persists and retrieves blog post objects
 *
 * @author matt rajkowski
 * @created 8/7/18 9:15 AM
 */
public class BlogPostRepository {

  private static Log LOG = LogFactory.getLog(BlogPostRepository.class);

  private static String TABLE_NAME = "blog_posts";
  private static String[] PRIMARY_KEY = new String[] { "post_id" };

  private static void appendWhereClause(Select select, BlogPostSpecification specification) {
    if (specification == null) {
      return;
    }
    if (specification.getId() > -1) {
      select.AND("post_id = ?", specification.getId());
    }
    if (specification.getBlogId() > -1) {
      select.AND("blog_id = ?", specification.getBlogId());
    }
    if (StringUtils.isNotBlank(specification.getUniqueId())) {
      select.AND("post_unique_id = ?", specification.getUniqueId());
    }
    if (specification.getPublishedOnly() != DataConstants.UNDEFINED) {
      if (specification.getPublishedOnly() == DataConstants.TRUE) {
        select.AND("published IS NOT NULL");
      } else {
        select.AND("published IS NULL");
      }
    }
    if (specification.getStartDateIsBeforeNow() != DataConstants.UNDEFINED) {
      if (specification.getStartDateIsBeforeNow() == DataConstants.TRUE) {
        // Show the ones which are active
        select.AND("start_date <= NOW()");
      }
    }
    if (specification.getIsWithinEndDate() != DataConstants.UNDEFINED) {
      if (specification.getIsWithinEndDate() == DataConstants.TRUE) {
        // Show the non-expiring and unexpired
        select.AND("(end_date IS NULL OR end_date >= NOW())");
      }
    }
    if (StringUtils.isNotBlank(specification.getSearchTerm())) {
      select.AND("tsv @@ websearch_to_tsquery('content_stem', ?)", specification.getSearchTerm().trim());
    }
  }

  private static DataResult<BlogPost> query(BlogPostSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT().FROM(TABLE_NAME);
    appendWhereClause(select, specification);
    if (specification != null && StringUtils.isNotBlank(specification.getSearchTerm())) {
      String searchTerm = specification.getSearchTerm().trim();
      select.SELECT(
          "ts_headline('english', body_text, websearch_to_tsquery('content_stem', ?), 'StartSel=${b}, StopSel=${/b}, MaxWords=30, MinWords=15, ShortWord=3, HighlightAll=FALSE, MaxFragments=2, FragmentDelimiter=\" ... \"') AS highlight",
          searchTerm);
      select.SELECT("ts_rank_cd(tsv, websearch_to_tsquery('content_stem', ?)) AS rank", searchTerm);
      select.ORDER_BY("rank DESC, post_id desc");
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(BlogPostRepository::buildRecord);
  }

  public static BlogPost findByUniqueId(Long blogId, String postUniqueId) {
    if (StringUtils.isBlank(postUniqueId)) {
      return null;
    }
    return DB.SELECT("blog_posts.*")
        .FROM(TABLE_NAME)
        .WHERE("blog_id = ?", blogId)
        .AND("post_unique_id = ?", postUniqueId)
        .returnRecord(BlogPostRepository::buildRecord);
  }

  public static BlogPost findById(Long blogPostId) {
    if (blogPostId == -1) {
      return null;
    }
    return DB.SELECT("blog_posts.*")
        .FROM(TABLE_NAME)
        .WHERE("post_id = ?", blogPostId)
        .returnRecord(BlogPostRepository::buildRecord);
  }

  public static List<BlogPost> findAll() {
    return findAll(null, null);
  }

  public static List<BlogPost> findAll(BlogPostSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("post_id");
    return query(specification, constraints).getRecords();
  }

  public static long findCount(BlogPostSpecification specification) {
    Select select = DB.SELECT().COUNT("*");
    appendWhereClause(select, specification);
    return select.returnCount();
  }

  public static BlogPost save(BlogPost record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static BlogPost add(BlogPost record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("blog_id", record.getBlogId())
        .FIELD("post_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("body", StringUtils.trimToNull(record.getBody()))
        .FIELD("body_text", HtmlCommand.text(StringUtils.trimToNull(record.getBody())))
        .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
        .FIELD("keywords", StringUtils.trimToNull(record.getKeywords()))
        .FIELD("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("published", record.getPublished())
        .FIELD("archived", record.getArchived())
        .FIELD("start_date", record.getStartDate())
        .FIELD("end_date", record.getEndDate());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static BlogPost update(BlogPost record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("post_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("body", StringUtils.trimToNull(record.getBody()))
        .SET("body_text", HtmlCommand.text(StringUtils.trimToNull(record.getBody())))
        .SET("summary", StringUtils.trimToNull(record.getSummary()))
        .SET("keywords", StringUtils.trimToNull(record.getKeywords()))
        .SET("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("published", record.getPublished())
        .SET("archived", record.getArchived())
        .SET("start_date", record.getStartDate())
        .SET("end_date", record.getEndDate())
        .WHERE("post_id = ?", record.getId());
    if (update.execute()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(BlogPost record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      //        ItemCategoryRepository.removeAll(connection, record);
      //        CollectionRepository.updateItemCount(connection, record.getCollectionId(), -1);
      //        CategoryRepository.updateItemCount(connection, record.getCategoryId(), -1);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("post_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Blog blog) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("blog_id = ?", blog.getId()).execute(connection);
  }

  private static BlogPost buildRecord(ResultSet rs) {
    try {
      BlogPost record = new BlogPost();
      record.setId(rs.getLong("post_id"));
      record.setBlogId(rs.getLong("blog_id"));
      record.setUniqueId(rs.getString("post_unique_id"));
      record.setTitle(rs.getString("title"));
      record.setBody(rs.getString("body"));
      record.setSummary(rs.getString("summary"));
      record.setSummaryText(HtmlCommand.text(StringUtils.trimToNull(record.getSummary())));
      record.setImageUrl(rs.getString("image_url"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setPublished(rs.getTimestamp("published"));
      record.setArchived(rs.getTimestamp("archived"));
      record.setStartDate(rs.getTimestamp("start_date"));
      record.setEndDate(rs.getTimestamp("end_date"));
      record.setKeywords(rs.getString("keywords"));
      record.setBodyText(rs.getString("body_text"));
      // Additional fields
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
