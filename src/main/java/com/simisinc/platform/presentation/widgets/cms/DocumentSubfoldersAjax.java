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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.SubFolder;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.SubFolderRepository;
import com.simisinc.platform.infrastructure.persistence.cms.SubFolderSpecification;
import com.simisinc.platform.presentation.controller.UserSession;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns subfolder metadata for the visual document editor
 */
public class DocumentSubfoldersAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908895L;
  private static Log LOG = LogFactory.getLog(DocumentSubfoldersAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("DocumentSubfoldersAjax...");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"subfolders\":[],\"total\":0}");
      return context;
    }

    long folderId = context.getParameterAsLong("folderId", -1);
    if (folderId <= 0) {
      context.setJson("{\"subfolders\":[],\"total\":0}");
      context.setSuccess(false);
      return context;
    }

    SubFolderSpecification specification = new SubFolderSpecification();
    specification.setFolderId(folderId);
    long userId = context.getUserId();
    if (userId > -1) {
      // Determine role which can see all document repositories
      if (!context.hasRole("admin")) {
        specification.setForUserId(userId);
      }
    } else {
      specification.setForUserId((long) UserSession.GUEST_ID);
    }

    DataConstraints constraints = new DataConstraints();
    constraints.setColumnToSortBy("name", "ASC");
    constraints.setPageSize(500);

    List<SubFolder> subfolders = SubFolderRepository.findAll(specification, constraints);

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"subfolders\": [");

    boolean first = true;
    for (SubFolder subFolder : subfolders) {
      if (!first) {
        sb.append(",");
      }
      first = false;

      sb.append("{");
      sb.append("\"id\":").append(subFolder.getId()).append(",");
      sb.append("\"folderId\":").append(subFolder.getFolderId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(StringUtils.defaultString(subFolder.getName()))).append("\",");
      sb.append("\"summary\":\"").append(JsonCommand.toJson(StringUtils.defaultString(subFolder.getSummary()))).append("\",");
      sb.append("\"fileCount\":").append(subFolder.getFileCount());
      sb.append("}");
    }

    sb.append("],");
    sb.append("\"total\":").append(subfolders.size());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
