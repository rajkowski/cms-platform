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
package com.zeroio.platform.infrastructure.persistence.tenant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataResult;
import com.zeroio.platform.domain.model.tenant.DomainMapping;

public class DomainMappingRepository {

  private static final Log LOG = LogFactory.getLog(DomainMappingRepository.class);
  private static final String TABLE_NAME = "workspace_domains";

  private DomainMappingRepository() {
  }

  public static List<DomainMapping> findActiveByPattern(String hostPattern, boolean wildcard) {
    DataResult<DomainMapping> result = DB.SELECT("*").FROM(TABLE_NAME).WHERE("host_pattern = ?", hostPattern).AND("wildcard = ?", wildcard).AND("active = ?", true).returnDataResult(DomainMappingRepository::buildRecord);
    return result.getRecords();
  }

  public static List<DomainMapping> findByPattern(String hostPattern, boolean wildcard) {
    DataResult<DomainMapping> result = DB.SELECT("*").FROM(TABLE_NAME).WHERE("host_pattern = ?", hostPattern).AND("wildcard = ?", wildcard).returnDataResult(DomainMappingRepository::buildRecord);
    return result.getRecords();
  }

  private static DomainMapping buildRecord(ResultSet resultSet) {
    try {
      DomainMapping mapping = new DomainMapping();
      mapping.setId(resultSet.getLong("workspace_domain_id"));
      mapping.setWorkspaceId(resultSet.getLong("workspace_id"));
      mapping.setHostPattern(resultSet.getString("host_pattern"));
      mapping.setWildcard(resultSet.getBoolean("wildcard"));
      mapping.setActive(resultSet.getBoolean("active"));
      return mapping;
    } catch (SQLException e) {
      LOG.error("Unable to build workspace domain record", e);
      return null;
    }
  }
}
