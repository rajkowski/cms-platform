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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.FolderRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FolderSpecification;
import com.simisinc.platform.presentation.controller.UserSession;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns folder metadata for the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 10:10 AM
 */
public class DocumentLibraryAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(DocumentLibraryAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DocumentLibraryAjax...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"folders\":[],\"total\":0}");
      return context;
    }

    String searchTerm = context.getParameter("search");
    int limit = context.getParameterAsInt("limit", 100);
    int page = context.getParameterAsInt("page", 1);

    DataConstraints constraints = new DataConstraints();
    constraints.setDefaultColumnToSortBy("name");
    constraints.setPageNumber(page);
    constraints.setPageSize(limit);

    FolderSpecification specification = new FolderSpecification();
    long userId = context.getUserId();
    if (userId > -1) {
      specification.setForUserId(userId);
    } else {
      specification.setForUserId((long) UserSession.GUEST_ID);
    }

    List<Folder> folders = FolderRepository.findAll(specification, constraints);

    // Optional search filter (case-insensitive contains)
    if (StringUtils.isNotBlank(searchTerm)) {
      List<Folder> filtered = new ArrayList<>();
      String lowered = searchTerm.toLowerCase();
      for (Folder folder : folders) {
        String name = StringUtils.defaultString(folder.getName()).toLowerCase();
        if (name.contains(lowered)) {
          filtered.add(folder);
        }
      }
      folders = filtered;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"folders\": [");

    boolean first = true;
    for (Folder folder : folders) {
      if (!first) {
        sb.append(",");
      }
      first = false;

      sb.append("{");
      sb.append("\"id\":").append(folder.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(StringUtils.defaultString(folder.getName()))).append("\",");
      sb.append("\"summary\":\"").append(JsonCommand.toJson(StringUtils.defaultString(folder.getSummary()))).append("\",");
      sb.append("\"fileCount\":").append(folder.getFileCount()).append(",");
      sb.append("\"allowsGuests\":").append(folder.getAllowsGuests()).append(",");
      sb.append("\"hasAllowedGroups\":").append(folder.doAllowedGroupsCheck()).append(",");
      sb.append("\"hasCategories\":").append(folder.doCategoriesCheck());
      sb.append("}");
    }

    sb.append("],");
    sb.append("\"page\":").append(page).append(",");
    sb.append("\"limit\":").append(limit).append(",");
    sb.append("\"total\":").append(folders.size());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
