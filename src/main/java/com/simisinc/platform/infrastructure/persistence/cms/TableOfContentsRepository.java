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

import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.cms.TableOfContentsJSONCommand;
import com.simisinc.platform.domain.model.cms.TableOfContents;
import com.simisinc.platform.infrastructure.cache.CacheManager;

/**
 * Persists and retrieves table of contents objects
 *
 * @author matt rajkowski
 * @created 12/7/18 4:42 PM
 */
public class TableOfContentsRepository {

  private static Log LOG = LogFactory.getLog(TableOfContentsRepository.class);

  private static String TABLE_NAME = "table_of_contents";
  private static String[] PRIMARY_KEY = new String[] { "toc_id" };

  private static DataResult<TableOfContents> query(TableOfContentsSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() != -1) {
        select.AND("toc_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getTocUniqueId())) {
        select.AND("toc_unique_id = ?", specification.getTocUniqueId());
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(TableOfContentsRepository::buildRecord);
  }

  public static TableOfContents findByUniqueId(String tocUniqueId) {
    if (StringUtils.isBlank(tocUniqueId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("toc_unique_id = ?", tocUniqueId)
        .returnRecord(TableOfContentsRepository::buildRecord);
  }

  public static List<TableOfContents> findAll(TableOfContentsSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("toc_unique_id");
    DataResult<TableOfContents> result = query(specification, constraints);
    return result.getRecords();
  }

  public static TableOfContents save(TableOfContents record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static TableOfContents add(TableOfContents record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("toc_unique_id", StringUtils.trimToNull(record.getTocUniqueId()))
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("created_by", record.getCreatedBy() == -1 ? null : record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy() == -1 ? null : record.getModifiedBy())
        .FIELD("entries", TableOfContentsJSONCommand.createJSONString(record), CastType.JSONB);
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static TableOfContents update(TableOfContents record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("entries", TableOfContentsJSONCommand.createJSONString(record), CastType.JSONB)
        .WHERE("toc_id = ?", record.getId());
    if (update.execute().booleanValue()) {
      CacheManager.invalidateKey(CacheManager.TABLE_OF_CONTENTS_UNIQUE_ID_CACHE, record.getTocUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static TableOfContents buildRecord(ResultSet rs) {
    try {
      TableOfContents record = new TableOfContents();
      record.setId(rs.getLong("toc_id"));
      record.setTocUniqueId(rs.getString("toc_unique_id"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      TableOfContentsJSONCommand.populateFromJSONString(record, rs.getString("entries"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
