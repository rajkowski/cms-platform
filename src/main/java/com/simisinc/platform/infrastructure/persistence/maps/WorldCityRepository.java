/*
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

package com.simisinc.platform.infrastructure.persistence.maps;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.maps.WorldCity;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.database.DataResult;
import com.simisinc.platform.infrastructure.database.SqlUtils;
import com.simisinc.platform.infrastructure.database.SqlWhere;

/**
 * Persists and retrieves world city objects
 *
 * @author matt rajkowski
 * @created 5/27/18 1:15 PM
 */
public class WorldCityRepository {

  private static Log LOG = LogFactory.getLog(WorldCityRepository.class);

  private static String TABLE_NAME = "world_cities";

  private static DataResult query(WorldCitySpecification specification, DataConstraints constraints) {
    SqlUtils select = new SqlUtils();
    SqlWhere where = DB.WHERE();
    SqlUtils orderBy = new SqlUtils();
    if (specification != null) {
      if (specification.getCity() != null) {
        where.AND("city = ?", specification.getCity().toLowerCase());
      }
      if (specification.getRegion() != null) {
        where.AND("region = ?", specification.getRegion().toUpperCase());
      }
      if (specification.getSearchCity() != null) {
        where.AND("city LIKE ?", specification.getSearchCity().toLowerCase() + "%");
      }
    }
    return DB.selectAllFrom(
        TABLE_NAME, select, where, orderBy, constraints, WorldCityRepository::buildRecord);
  }

  public static List<WorldCity> findAll(WorldCitySpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("population desc");
    DataResult result = query(specification, constraints);
    return (List<WorldCity>) result.getRecords();
  }

  public static WorldCity findByCityRegionCountry(String city, String region, String country) {
    if (StringUtils.isBlank(city)) {
      return null;
    }
    SqlWhere where = DB.WHERE();
    where.AND("city = ?", city.toLowerCase());
    if (region != null) {
      where.AND("region = ?", region.toUpperCase());
    }
    if (country != null) {
      where.AND("country = ?", country.toLowerCase());
    }
    return (WorldCity) DB.selectRecordFrom(
        TABLE_NAME,
        where,
        WorldCityRepository::buildRecord);
  }

  private static WorldCity buildRecord(ResultSet rs) {
    try {
      WorldCity record = new WorldCity();
      record.setCity(rs.getString("city"));
      record.setRegion(rs.getString("region"));
      record.setCountry(rs.getString("country"));
      record.setLatitude(rs.getDouble("latitude"));
      record.setLongitude(rs.getDouble("longitude"));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
