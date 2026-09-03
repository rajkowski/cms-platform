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
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.cms.Wiki;
import com.simisinc.platform.domain.model.cms.WikiPage;

/**
 * Persists and retrieves wiki page objects
 *
 * @author matt rajkowski
 * @created 2/10/19 11:18 AM
 */
public class WikiPageRepository {

  private static Log LOG = LogFactory.getLog(WikiPageRepository.class);

  private static String TABLE_NAME = "wiki_pages";
  private static String[] PRIMARY_KEY = new String[] { "wiki_page_id" };

  private static Select createSelect(WikiPageSpecification specification) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("wiki_page_id = ?", specification.getId());
      }
      if (specification.getWikiId() > -1) {
        select.AND("wiki_id = ?", specification.getWikiId());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("page_unique_id = ?", specification.getUniqueId());
      }
      if (specification.getStartingDateRange() != null && specification.getEndingDateRange() != null) {
        select.AND("((start_date >= ? AND start_date < ?) OR (end_date >= ? AND end_date < ?))",
            specification.getStartingDateRange(), specification.getEndingDateRange(),
            specification.getStartingDateRange(), specification.getEndingDateRange());
      }
    }
    return select;
  }

  private static DataResult<WikiPage> query(WikiPageSpecification specification, DataConstraints constraints) {
    Select select = createSelect(specification);
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(WikiPageRepository::buildRecord);
  }

  public static WikiPage findByUniqueId(Long wikiId, String pageUniqueId) {
    if (StringUtils.isBlank(pageUniqueId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("wiki_id = ?", wikiId)
        .AND("page_unique_id = ?", pageUniqueId.toLowerCase())
        .returnRecord(WikiPageRepository::buildRecord);
  }

  public static WikiPage findById(Long wikiPageId) {
    if (wikiPageId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("wiki_page_id = ?", wikiPageId)
        .returnRecord(WikiPageRepository::buildRecord);
  }

  public static List<WikiPage> findAll() {
    return findAll(null, null);
  }

  public static List<WikiPage> findAll(WikiPageSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("wiki_page_id");
    DataResult result = query(specification, constraints);
    return (List<WikiPage>) result.getRecords();
  }

  public static long findCount(WikiPageSpecification specification) {
    return createSelect(specification).returnCount();
  }

  public static WikiPage save(WikiPage record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static long addDefaultPage(Connection connection, Wiki wiki) {
    return DB.INSERT().INTO(TABLE_NAME)
        .FIELD("wiki_id", wiki.getId())
        .FIELD("page_unique_id", "home")
        .FIELD("title", "Home")
        .FIELD("body", "Welcome to the " + wiki.getName() + " wiki!")
        .FIELD("created_by", wiki.getCreatedBy())
        .FIELD("modified_by", wiki.getModifiedBy())
        .execute(connection);
  }

  public static WikiPage add(WikiPage record) {
    record.setId(DB.INSERT().INTO(TABLE_NAME)
        .FIELD("wiki_id", record.getWikiId())
        .FIELD("page_unique_id", StringUtils.trim(record.getUniqueId()).toLowerCase())
        .FIELD("title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("body", StringUtils.trimToNull(record.getBody()))
        .FIELD("summary", StringUtils.trimToNull(record.getSummary()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static WikiPage update(WikiPage record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("wiki_id", record.getWikiId())
        .SET("page_unique_id", StringUtils.trim(record.getUniqueId()).toLowerCase())
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("body", StringUtils.trimToNull(record.getBody()))
        .SET("summary", StringUtils.trimToNull(record.getSummary()))
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("wiki_page_id = ?", record.getId())
        .execute();
    if (updated) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(WikiPage record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      DB.DELETE().FROM(TABLE_NAME).WHERE("wiki_page_id = ?", record.getId()).execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void removeAll(Connection connection, Wiki wiki) throws SQLException {
    DB.DELETE().FROM(TABLE_NAME).WHERE("wiki_id = ?", wiki.getId()).execute(connection);
  }

  private static WikiPage buildRecord(ResultSet rs) {
    try {
      WikiPage record = new WikiPage();
      record.setId(rs.getLong("wiki_page_id"));
      record.setWikiId(rs.getLong("wiki_id"));
      record.setUniqueId(rs.getString("page_unique_id"));
      record.setTitle(rs.getString("title"));
      record.setBody(rs.getString("body"));
      record.setSummary(rs.getString("summary"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
