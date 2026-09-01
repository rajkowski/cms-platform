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

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.domain.model.maps.WorldCity;

/**
 * Persists and retrieves world city objects
 *
 * @author matt rajkowski
 * @created 5/27/18 1:15 PM
 */
public class WorldCityRepository {

  private static Log LOG = LogFactory.getLog(WorldCityRepository.class);

  private static String TABLE_NAME = "world_cities";

  private static DataResult<WorldCity> query(WorldCitySpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (specification.getCity() != null) {
        select.AND("city = ?", specification.getCity().toLowerCase());
      }
      if (specification.getRegion() != null) {
        select.AND("region = ?", specification.getRegion().toUpperCase());
      }
      if (specification.getSearchCity() != null) {
        select.AND("city LIKE ?", specification.getSearchCity().toLowerCase() + "%");
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(WorldCityRepository::buildRecord);
  }

  public static List<WorldCity> findAll(WorldCitySpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("population desc");
    return query(specification, constraints).getRecords();
  }

  public static WorldCity findByCityRegionCountry(String city, String region, String country) {
    if (StringUtils.isBlank(city)) {
      return null;
    }
    Select select = DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("city = ?", city.toLowerCase());
    if (region != null) {
      select.AND("region = ?", region.toUpperCase());
    }
    if (country != null) {
      select.AND("country = ?", country.toLowerCase());
    }
    return select.returnRecord(WorldCityRepository::buildRecord);
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
