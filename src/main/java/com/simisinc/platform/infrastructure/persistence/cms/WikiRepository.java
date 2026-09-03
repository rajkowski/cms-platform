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

/**
 * Persists and retrieves wiki objects
 *
 * @author matt rajkowski
 * @created 2/9/19 5:29 PM
 */
public class WikiRepository {

  private static Log LOG = LogFactory.getLog(WikiRepository.class);

  private static String TABLE_NAME = "wikis";
  private static String[] PRIMARY_KEY = new String[] { "wiki_id" };

  private static DataResult<Wiki> query(WikiSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("wiki_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("wiki_unique_id = ?", specification.getUniqueId());
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(WikiRepository::buildRecord);
  }

  public static Wiki findById(long wikiId) {
    if (wikiId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("wiki_id = ?", wikiId)
        .returnRecord(WikiRepository::buildRecord);
  }

  public static Wiki findByUniqueId(String wikiUniqueId) {
    if (StringUtils.isBlank(wikiUniqueId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("wiki_unique_id = ?", wikiUniqueId)
        .returnRecord(WikiRepository::buildRecord);
  }

  public static List<Wiki> findAll() {
    return findAll(null, null);
  }

  public static List<Wiki> findAll(WikiSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("wiki_id");
    return query(specification, constraints).getRecords();
  }

  public static Wiki save(Wiki record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static Wiki add(Wiki record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      record.setId(DB.INSERT().INTO(TABLE_NAME)
          .FIELD("wiki_unique_id", StringUtils.trimToNull(record.getUniqueId()))
          .FIELD("name", StringUtils.trimToNull(record.getName()))
          .FIELD("description", StringUtils.trimToNull(record.getDescription()))
          .FIELD("created_by", record.getCreatedBy())
          .FIELD("modified_by", record.getModifiedBy())
          .FIELD("enabled", record.getEnabled())
          .FIELD("starting_page", record.getStartingPage())
          .execute(connection));
      if (record.getStartingPage() == -1L) {
        long wikiPageId = WikiPageRepository.addDefaultPage(connection, record);
        DB.UPDATE(TABLE_NAME)
            .SET("starting_page", wikiPageId)
            .WHERE("wiki_id = ?", record.getId())
            .execute(connection);
      }
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("An id was not set!");
    return null;
  }

  public static Wiki update(Wiki record) {
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("wiki_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("enabled", record.getEnabled())
        .SET("starting_page", record.getStartingPage())
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("wiki_id = ?", record.getId())
        .execute();
    if (updated) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Wiki record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      WikiPageRepository.removeAll(connection, record);
      DB.DELETE().FROM(TABLE_NAME).WHERE("wiki_id = ?", record.getId()).execute(connection);
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static Wiki buildRecord(ResultSet rs) {
    try {
      Wiki record = new Wiki();
      record.setId(rs.getLong("wiki_id"));
      record.setUniqueId(rs.getString("wiki_unique_id"));
      record.setName(rs.getString("name"));
      record.setDescription(rs.getString("description"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setStartingPage(rs.getLong("starting_page"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
