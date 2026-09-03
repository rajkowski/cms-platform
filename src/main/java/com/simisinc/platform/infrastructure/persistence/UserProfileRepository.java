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

package com.simisinc.platform.infrastructure.persistence;

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
import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.CustomFieldListJSONCommand;
import com.simisinc.platform.application.cms.HtmlCommand;
import com.simisinc.platform.domain.model.UserProfile;

/**
 * Persists and retrieves user profile objects
 *
 * @author matt rajkowski
 * @created 7/17/22 8:08 AM
 */
public class UserProfileRepository {

  private static Log LOG = LogFactory.getLog(UserProfileRepository.class);

  private static String TABLE_NAME = "users";
  private static String[] PRIMARY_KEY = new String[] { "user_id" };

  private static DataResult<UserProfile> query(UserSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("users.*").FROM(TABLE_NAME);
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(UserProfileRepository::buildRecord);
  }

  public static UserProfile findByUniqueId(String uniqueId) {
    if (StringUtils.isBlank(uniqueId)) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("unique_id = ?", uniqueId)
        .returnRecord(UserProfileRepository::buildRecord);
  }

  public static UserProfile findByUserId(long userId) {
    if (userId == -1) {
      return null;
    }
    return DB.SELECT("*")
        .FROM(TABLE_NAME)
        .WHERE("user_id = ?", userId)
        .returnRecord(UserProfileRepository::buildRecord);
  }

  public static List<UserProfile> findAll(UserSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("first_name, last_name");
    return query(specification, constraints).getRecords();
  }

  public static UserProfile save(UserProfile record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return null;
  }

  private static UserProfile update(UserProfile record) {
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("unique_id", StringUtils.trimToNull(record.getUniqueId()))
        .SET("first_name", StringUtils.trimToNull(record.getFirstName()))
        .SET("last_name", StringUtils.trimToNull(record.getLastName()))
        .SET("organization", StringUtils.trimToNull(record.getOrganization()))
        .SET("nickname", StringUtils.trimToNull(record.getNickname()))
        .SET("description", StringUtils.trimToNull(record.getDescription()))
        .SET("description_text", HtmlCommand.text(StringUtils.trimToNull(record.getDescription())))
        .SET("email", StringUtils.trimToNull(record.getEmail()))
        .SET("title", StringUtils.trimToNull(record.getTitle()))
        .SET("department", StringUtils.trimToNull(record.getDepartment()))
        .SET("timezone", StringUtils.trimToNull(record.getTimeZone()))
        .SET("city", StringUtils.trimToNull(record.getCity()))
        .SET("state", StringUtils.trimToNull(record.getState()))
        .SET("country", StringUtils.trimToNull(record.getCountry()))
        .SET("postal_code", StringUtils.trimToNull(record.getPostalCode()))
        .SET("image_url", StringUtils.trimToNull(record.getImageUrl()))
        .SET("video_url", StringUtils.trimToNull(record.getVideoUrl()))
        .SET("modified_by", record.getModifiedBy())
        .SET("modified", new Timestamp(System.currentTimeMillis()));
    if (record.hasGeoPoint()) {
      update.SET("latitude", record.getLatitude())
          .SET("longitude", record.getLongitude())
          .SET("geom", record.getLatitude(), record.getLongitude(), CastType.GEOM);
    } else {
      update.SET("latitude", (Double) null)
          .SET("longitude", (Double) null)
          .SET("geom", 0, 0, CastType.GEOM);
    }
    if (record.getCustomFieldList() != null && !record.getCustomFieldList().isEmpty()) {
      update.SET("field_values", CustomFieldListJSONCommand.createJSONString(record.getCustomFieldList()), CastType.JSONB);
    } else {
      update.SET("field_values", (String) null, CastType.JSONB);
    }
    update.WHERE("user_id = ?", record.getId());
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      update.execute(connection);
      transaction.commit();
      return record;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage(), se);
    }
    return null;
  }

  private static UserProfile buildRecord(ResultSet rs) {
    try {
      UserProfile record = new UserProfile();
      record.setId(rs.getLong("user_id"));
      record.setUniqueId(rs.getString("unique_id"));
      record.setFirstName(rs.getString("first_name"));
      record.setLastName(rs.getString("last_name"));
      record.setOrganization(rs.getString("organization"));
      record.setNickname(rs.getString("nickname"));
      record.setEmail(rs.getString("email"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setTitle(rs.getString("title"));
      record.setDepartment(rs.getString("department"));
      record.setTimeZone(rs.getString("timezone"));
      record.setCity(rs.getString("city"));
      record.setState(rs.getString("state"));
      record.setCountry(rs.getString("country"));
      record.setPostalCode(rs.getString("postal_code"));
      record.setLatitude(rs.getDouble("latitude"));
      record.setLongitude(rs.getDouble("longitude"));
      record.setDescription(rs.getString("description"));
      record.setImageUrl(rs.getString("image_url"));
      record.setVideoUrl(rs.getString("video_url"));
      record.setCustomFieldList(CustomFieldListJSONCommand.populateFromJSONString(rs.getString("field_values")));
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }
}
