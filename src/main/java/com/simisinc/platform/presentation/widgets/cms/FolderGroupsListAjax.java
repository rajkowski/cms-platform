/*
 * Copyright 2026 Matt Rajkowski
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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.Group;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.FolderGroup;
import com.simisinc.platform.infrastructure.persistence.GroupRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FolderGroupRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FolderRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns folder groups and available groups for the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 11:10 AM
 */
public class FolderGroupsListAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(FolderGroupsListAjax.class);
  private static final String JSON_ID = "\"id\":";

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("FolderGroupsListAjax...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"success\": false, \"error\": \"Access denied\"}");
      context.setSuccess(false);
      return context;
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    if (folderId == -1) {
      context.setJson("{\"success\": false, \"error\": \"Folder ID required\"}");
      context.setSuccess(false);
      return context;
    }

    // @todo check access

    Folder folder = FolderRepository.findById(folderId);
    if (folder == null) {
      context.setJson("{\"success\": false, \"error\": \"Folder not found\"}");
      context.setSuccess(false);
      return context;
    }

    // Get folder groups and all groups
    List<FolderGroup> folderGroups = FolderGroupRepository.findAllByFolderId(folderId);
    List<Group> allGroups = GroupRepository.findAll();

    StringBuilder json = new StringBuilder();
    json.append("{");
    appendFolderJson(json, folder);
    json.append(",");
    appendFolderGroupsJson(json, folderGroups);
    json.append(",");
    appendAllGroupsJson(json, allGroups);
    json.append("}");

    context.setJson(json.toString());
    return context;
  }

  private void appendFolderJson(StringBuilder json, Folder folder) {
    json.append("\"folder\":{");
    json.append(JSON_ID).append(folder.getId()).append(",");
    json.append("\"name\":\"").append(escapeJson(folder.getName())).append("\"");
    json.append("}");
  }

  private void appendFolderGroupsJson(StringBuilder json, List<FolderGroup> folderGroups) {
    json.append("\"folderGroups\":[");
    if (folderGroups != null && !folderGroups.isEmpty()) {
      boolean first = true;
      for (FolderGroup fg : folderGroups) {
        if (!first) {
          json.append(",");
        }
        first = false;
        appendFolderGroupObject(json, fg);
      }
    }
    json.append("]");
  }

  private void appendFolderGroupObject(StringBuilder json, FolderGroup fg) {
    json.append("{");
    json.append(JSON_ID).append(fg.getId()).append(",");
    json.append("\"folderId\":").append(fg.getFolderId()).append(",");
    json.append("\"groupId\":").append(fg.getGroupId()).append(",");
    json.append("\"privacyType\":").append(fg.getPrivacyType()).append(",");
    json.append("\"addPermission\":").append(fg.getAddPermission()).append(",");
    json.append("\"editPermission\":").append(fg.getEditPermission()).append(",");
    json.append("\"deletePermission\":").append(fg.getDeletePermission());
    json.append("}");
  }

  private void appendAllGroupsJson(StringBuilder json, List<Group> allGroups) {
    json.append("\"allGroups\":[");
    if (allGroups != null && !allGroups.isEmpty()) {
      boolean first = true;
      for (Group group : allGroups) {
        if (!first) {
          json.append(",");
        }
        first = false;
        json.append("{");
        json.append(JSON_ID).append(group.getId()).append(",");
        json.append("\"name\":\"").append(escapeJson(group.getName())).append("\"");
        json.append("}");
      }
    }
    json.append("]");
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }
}
