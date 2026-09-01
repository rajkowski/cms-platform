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
import com.simisinc.platform.application.cms.ThemeJSONCommand;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.domain.model.cms.Theme;

/**
 * Persists and retrieves theme objects
 *
 * @author matt rajkowski
 * @created 1/12/19 1:56 PM
 */
public class ThemeRepository {

  private static Log LOG = LogFactory.getLog(ThemeRepository.class);

  private static String TABLE_NAME = "themes";
  private static String[] PRIMARY_KEY = new String[] { "theme_id" };

  public static Theme findByName(String name) {
    return DB.SELECT("themes.*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(name) = ?", name.toLowerCase())
        .returnRecord(ThemeRepository::buildRecord);
  }

  public static Theme findById(long id) {
    return DB.SELECT("themes.*")
        .FROM(TABLE_NAME)
        .WHERE("theme_id = ?", id)
        .returnRecord(ThemeRepository::buildRecord);
  }

  public static List<Theme> findAll() {
    return DB.SELECT("themes.*")
        .FROM(TABLE_NAME)
        .WITH(new DataConstraints().setDefaultColumnToSortBy("theme_id"))
        .returnDataResult(ThemeRepository::buildRecord)
        .getRecords();
  }

  public static Theme save(Theme record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  public static Theme save(List<SiteProperty> sitePropertyList, String name) {
    Theme theme = new Theme(name);
    theme.setSiteProperties(sitePropertyList);
    return save(theme);
  }

  public static Theme add(Theme record) {
    record.setId(DB.INSERT().INTO(TABLE_NAME)
        .FIELD("name", StringUtils.trimToNull(record.getName()))
        .FIELD("entries", ThemeJSONCommand.createJSONString(record), CastType.JSONB)
        .execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static Theme update(Theme record) {
    if (DB.UPDATE(TABLE_NAME)
        .SET("name", StringUtils.trimToNull(record.getName()))
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("entries", ThemeJSONCommand.createJSONString(record), CastType.JSONB)
        .WHERE("theme_id = ?", record.getId())
        .execute()) {
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void remove(Theme record) {
    DB.DELETE().FROM(TABLE_NAME).WHERE("theme_id = ?", record.getId()).execute();
  }

  private static Theme buildRecord(ResultSet rs) {
    try {
      Theme record = new Theme();
      record.setId(rs.getLong("theme_id"));
      record.setName(rs.getString("name"));
      ThemeJSONCommand.populateFromJSONString(record, rs.getString("entries"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
