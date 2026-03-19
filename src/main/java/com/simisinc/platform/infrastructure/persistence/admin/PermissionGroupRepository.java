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
package com.simisinc.platform.infrastructure.persistence.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.admin.PermissionGroup;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.database.DataResult;

/**
 * Loads permission groups and their members from the database.
 *
 * <p>DB rows with a {@code group_code} that matches a loaded XML group OVERRIDE
 * the policy text from the {@code .cedar} file. DB-only codes ADD new groups.
 *
 * @author matt rajkowski
 * @created 3/6/26 8:00 AM
 */
public class PermissionGroupRepository {

  private static Log LOG = LogFactory.getLog(PermissionGroupRepository.class);

  private static final String POLICIES_TABLE = "permission_policies";
  private static final String MEMBERS_TABLE = "permission_group_members";

  /**
   * Loads all enabled permission group policies from the database.
   *
   * @return list of PermissionGroup objects (without members populated)
   */
  @SuppressWarnings("unchecked")
  public static List<PermissionGroup> findAllPolicies() {
    DataResult result = DB.selectAllFrom(
        POLICIES_TABLE,
        DB.WHERE("enabled = ?", true),
        new DataConstraints().setDefaultColumnToSortBy("group_code"),
        PermissionGroupRepository::buildPolicy);
    if (result == null) {
      return new ArrayList<>();
    }
    return (List<PermissionGroup>) result.getRecords();
  }

  /**
   * Loads all permission group members from the database.
   *
   * @return map of group_code → list of (className, memberType) pairs as String[2]
   */
  public static Map<String, List<String[]>> findAllMembers() {
    Map<String, List<String[]>> memberMap = new LinkedHashMap<>();
    String sql = "SELECT group_code, class_name, member_type FROM " + MEMBERS_TABLE + " ORDER BY group_code";
    try (Connection conn = DB.getConnection();
         PreparedStatement pst = conn.prepareStatement(sql);
         ResultSet rs = pst.executeQuery()) {
      while (rs.next()) {
        String groupCode = rs.getString("group_code");
        String className = rs.getString("class_name");
        String memberType = rs.getString("member_type");
        memberMap.computeIfAbsent(groupCode, k -> new ArrayList<>()).add(new String[]{className, memberType});
      }
    } catch (SQLException e) {
      LOG.error("PermissionGroupRepository.findAllMembers error", e);
    }
    return memberMap;
  }

  private static PermissionGroup buildPolicy(ResultSet rs) {
    try {
      PermissionGroup group = new PermissionGroup();
      group.setId(DB.getLong(rs, "policy_id", -1L));
      group.setCode(rs.getString("group_code"));
      group.setName(rs.getString("group_name"));
      group.setCedarPolicyText(rs.getString("policy_text"));
      group.setEnabled(rs.getBoolean("enabled"));
      return group;
    } catch (SQLException e) {
      LOG.error("PermissionGroupRepository.buildPolicy error", e);
      return null;
    }
  }

}
