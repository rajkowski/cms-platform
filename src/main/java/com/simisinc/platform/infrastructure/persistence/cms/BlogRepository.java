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
import com.simisinc.platform.domain.model.cms.Blog;

/**
 * Persists and retrieves blog objects
 *
 * @author matt rajkowski
 * @created 8/7/18 8:58 AM
 */
public class BlogRepository {

  private static Log LOG = LogFactory.getLog(BlogRepository.class);

  private static String TABLE_NAME = "blogs";
  private static String[] PRIMARY_KEY = new String[] { "blog_id" };

  private static DataResult<Blog> query(BlogSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("blogs.*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getId() > -1) {
        select.AND("blog_id = ?", specification.getId());
      }
      if (StringUtils.isNotBlank(specification.getUniqueId())) {
        select.AND("blog_unique_id = ?", specification.getUniqueId());
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(BlogRepository::buildRecord);
  }

  public static Blog findById(long blogId) {
    if (blogId == -1) {
      return null;
    }
    return DB.SELECT("blogs.*")
        .FROM(TABLE_NAME)
        .WHERE("blog_id = ?", blogId)
        .returnRecord(BlogRepository::buildRecord);
  }

  public static Blog findByUniqueId(String blogUniqueId) {
    if (StringUtils.isBlank(blogUniqueId)) {
      return null;
    }
    return DB.SELECT("blogs.*")
        .FROM(TABLE_NAME)
        .WHERE("blog_unique_id = ?", blogUniqueId)
        .returnRecord(BlogRepository::buildRecord);
  }

  public static List<Blog> findAll() {
    return findAll(null, null);
  }

  public static List<Blog> findAll(BlogSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("blog_id");
    DataResult result = query(specification, constraints);
    return (List<Blog>) result.getRecords();
  }

  public static Blog save(Blog record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static Blog add(Blog record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("blog_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("description", StringUtils.trimToNull(record.getDescription()))
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("enabled", record.getEnabled());
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static Blog update(Blog record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("blog_unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("enabled", record.getEnabled())
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .WHERE("blog_id = ?", record.getId());
    if (update.execute()) {
      //      CacheManager.invalidateKey(CacheManager.CONTENT_UNIQUE_ID_CACHE, record.getUniqueId());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static boolean remove(Blog record) {
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      BlogPostRepository.removeAll(connection, record);
      // Delete the record
      DB.DELETE().FROM(TABLE_NAME).WHERE("blog_id = ?", record.getId()).execute(connection);
      // Finish transaction
      transaction.commit();
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  private static Blog buildRecord(ResultSet rs) {
    try {
      Blog record = new Blog();
      record.setId(rs.getLong("blog_id"));
      record.setUniqueId(rs.getString("blog_unique_id"));
      record.setName(rs.getString("name"));
      record.setDescription(rs.getString("description"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setModified(rs.getTimestamp("modified"));
      record.setEnabled(rs.getBoolean("enabled"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
