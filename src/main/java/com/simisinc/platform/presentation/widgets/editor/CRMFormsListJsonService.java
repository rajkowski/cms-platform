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

package com.simisinc.platform.presentation.widgets.editor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * JSON service to list all form categories (distinct form_unique_id values) with submission counts
 *
 * @author matt rajkowski
 * @created 2026-02-27
 */
public class CRMFormsListJsonService extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908896L;
  private static Log LOG = LogFactory.getLog(CRMFormsListJsonService.class);

  private static final String SQL_QUERY = "SELECT form_unique_id, " +
      "COUNT(*) AS total, " +
      "SUM(CASE WHEN claimed IS NULL AND dismissed IS NULL AND processed IS NULL AND flagged_as_spam = false THEN 1 ELSE 0 END) AS new_count, "
      +
      "MAX(created) AS last_submission " +
      "FROM form_data " +
      "GROUP BY form_unique_id " +
      "ORDER BY form_unique_id";

  public JsonServiceContext get(JsonServiceContext context) {

    // Check permissions
    if (!PermissionEngine.checkAccess(getClass().getName(), context.getUserSession())) {
      LOG.debug("No permission to: " + CRMFormsListJsonService.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    StringBuilder json = new StringBuilder();
    json.append("[");

    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      boolean first = true;
      while (rs.next()) {
        if (!first) {
          json.append(",");
        }
        first = false;
        String formUniqueId = rs.getString("form_unique_id");
        long total = rs.getLong("total");
        long newCount = rs.getLong("new_count");
        String lastSubmission = rs.getString("last_submission");
        json.append("{");
        json.append("\"formUniqueId\": \"").append(JsonCommand.toJson(formUniqueId)).append("\",");
        json.append("\"total\": ").append(total).append(",");
        json.append("\"newCount\": ").append(newCount).append(",");
        json.append("\"lastSubmission\": ").append(lastSubmission != null ? "\"" + JsonCommand.toJson(lastSubmission) + "\"" : "null");
        json.append("}");
      }
    } catch (SQLException se) {
      LOG.error("CRMFormsListJsonService SQL error: " + se.getMessage());
      return context.writeError("Database error");
    }

    json.append("]");
    context.setJson(json.toString());
    return context;
  }
}
