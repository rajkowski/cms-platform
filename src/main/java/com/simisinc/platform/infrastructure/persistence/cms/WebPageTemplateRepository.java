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
import com.simisinc.platform.domain.model.cms.WebPageTemplate;

/**
 * Persists and retrieves web page template objects
 *
 * @author matt rajkowski
 * @created 5/7/18 7:35 AM
 */
public class WebPageTemplateRepository {

  private static Log LOG = LogFactory.getLog(WebPageTemplateRepository.class);

  private static String TABLE_NAME = "web_page_templates";
  private static String[] PRIMARY_KEY = new String[] { "template_id" };

  private static DataResult<WebPageTemplate> query(WebPageTemplateSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME);
    if (specification != null && specification.getId() != -1) {
      select.WHERE("template_id = ?", specification.getId());
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(WebPageTemplateRepository::buildRecord);
  }

  public static WebPageTemplate findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("template_id = ?", id)
        .returnRecord(WebPageTemplateRepository::buildRecord);
  }

  public static WebPageTemplate findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("name = ?", name)
        .returnRecord(WebPageTemplateRepository::buildRecord);
  }

  public static List<WebPageTemplate> findAll() {
    return findAll(null, null);
  }

  public static List<WebPageTemplate> findAll(WebPageTemplateSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("template_order, name");
    return query(specification, constraints).getRecords();
  }

  public static WebPageTemplate save(WebPageTemplate record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static WebPageTemplate add(WebPageTemplate record) {
    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("image_path", StringUtils.trimToNull(record.getImagePath()))
        .FIELD("page_xml", StringUtils.trimToNull(record.getPageXml()))
        .FIELD("template_order", record.getTemplateOrder())
        .FIELD("description", StringUtils.trimToNull(record.getDescription()))
        .FIELD("css", StringUtils.trimToNull(record.getCss()))
        .FIELD("category", StringUtils.trimToNull(record.getCategory()));
    record.setId(insert.execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static WebPageTemplate update(WebPageTemplate record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("image_path", StringUtils.trimToNull(record.getImagePath()))
        .SET("page_xml", StringUtils.trimToNull(record.getPageXml()))
        .SET("template_order", record.getTemplateOrder())
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("css", StringUtils.trimToNull(record.getCss()))
        .SET("category", StringUtils.trimToNull(record.getCategory()));
    if (update.WHERE("template_id = ?", record.getId()).execute()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  private static WebPageTemplate buildRecord(ResultSet rs) {
    try {
      WebPageTemplate record = new WebPageTemplate();
      record.setId(rs.getLong("template_id"));
      record.setName(rs.getString("name"));
      record.setImagePath(rs.getString("image_path"));
      record.setPageXml(rs.getString("page_xml"));
      record.setTemplateOrder(rs.getInt("template_order"));
      record.setDescription(rs.getString("description"));
      //      WebPageTemplateRuleListJSONCommand.populateFromJSONString(record, rs.getString("rules"));
      record.setCss(rs.getString("css"));
      record.setCategory(rs.getString("category"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
