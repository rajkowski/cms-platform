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
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.simisinc.platform.domain.model.cms.WebSearch;

/**
 * Persists and retrieves web search objects
 *
 * @author matt rajkowski
 * @created 3/5/2021 2:00 PM
 */
public class WebSearchRepository {

  private static Log LOG = LogFactory.getLog(WebSearchRepository.class);

  private static String TABLE_NAME = "web_searches";

  public static WebSearch save(WebSearch record) {
    return add(record);
  }

  private static WebSearch add(WebSearch record) {
    record.setId(DB.INSERT().INTO(TABLE_NAME)
        .FIELD("page_path", StringUtils.truncate(record.getPagePath(), 255))
        .FIELD("query", StringUtils.truncate(record.getQuery(), 255))
        .FIELD("ip_address", record.getIpAddress())
        .FIELD("session_id", record.getSessionId())
        .FIELD("is_logged_in", record.getIsLoggedIn())
        .execute());
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  public static boolean remove(WebSearch record) {
    try (Connection connection = DB.getConnection()) {
      return DB.DELETE().FROM(TABLE_NAME).WHERE("search_id = ?", record.getId()).execute(connection);
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    LOG.error("The delete failed!");
    return false;
  }
}
