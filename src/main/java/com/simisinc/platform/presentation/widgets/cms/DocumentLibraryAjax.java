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

import com.simisinc.platform.application.admin.PermissionEngine;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.FolderRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FolderSpecification;
import com.simisinc.platform.infrastructure.persistence.items.CollectionRepository;
import com.simisinc.platform.infrastructure.persistence.items.CollectionSpecification;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.controller.UserSession;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Returns folder metadata for the visual document editor
 *
 * @author matt rajkowski
 * @created 1/22/26 10:10 AM
 */
public class DocumentLibraryAjax extends GenericJsonService {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(DocumentLibraryAjax.class);

  @Override
  public JsonServiceContext get(JsonServiceContext context) {

    LOG.debug("DocumentLibraryAjax...");

    // Restrict access to editors
    // Check permissions
    if (!PermissionEngine.checkAccess("cms.document.library", context.getUserSession())) {
      LOG.debug("No permission to: " + DocumentLibraryAjax.class.getSimpleName());
      return context.writeError("Permission Denied");
    }

    String searchTerm = context.getParameter("search");
    int limit = context.getParameterAsInt("limit", 100);
    int page = context.getParameterAsInt("page", 1);

    DataConstraints constraints = buildConstraints(page, limit);

    FolderSpecification specification = new FolderSpecification();
    long userId = context.getUserId();
    if (userId > -1) {
      // Determine role which can see all document repositories
      if (!context.hasRole("admin")) {
        specification.setForUserId(userId);
      }
    } else {
      specification.setForUserId((long) UserSession.GUEST_ID);
    }

    List<Folder> folders = FolderRepository.findAll(specification, constraints);
    List<Collection> collections = loadAuthorizedCollections(context, userId, constraints);

    // Optional search filter (case-insensitive contains)
    if (StringUtils.isNotBlank(searchTerm)) {
      String lowered = searchTerm.toLowerCase();
      folders = filterFoldersByName(folders, lowered);
      collections = filterCollectionsByName(collections, lowered);
    }

    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"folders\": [");

    appendFoldersJson(sb, folders);

    sb.append("],");
    sb.append("\"collections\": [");

    appendCollectionsJson(sb, collections);

    sb.append("],");
    sb.append("\"page\":").append(page).append(",");
    sb.append("\"limit\":").append(limit).append(",");
    sb.append("\"total\":").append(folders.size() + collections.size());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }

  private DataConstraints buildConstraints(int page, int limit) {
    DataConstraints constraints = new DataConstraints();
    constraints.setColumnToSortBy("name", "ASC");
    constraints.setPageNumber(page);
    constraints.setPageSize(limit);
    return constraints;
  }

  private List<Collection> loadAuthorizedCollections(JsonServiceContext context, long userId, DataConstraints constraints) {
    CollectionSpecification collectionSpecification = new CollectionSpecification();
    if (userId > -1) {
      if (!context.hasRole("admin")) {
        collectionSpecification.setForUserId(userId);
      }
    } else {
      collectionSpecification.setForUserId((long) UserSession.GUEST_ID);
    }
    return CollectionRepository.findAll(collectionSpecification, constraints);
  }

  private List<Folder> filterFoldersByName(List<Folder> folders, String loweredSearch) {
    List<Folder> filtered = new ArrayList<>();
    for (Folder folder : folders) {
      String name = StringUtils.defaultString(folder.getName()).toLowerCase();
      if (name.contains(loweredSearch)) {
        filtered.add(folder);
      }
    }
    return filtered;
  }

  private List<Collection> filterCollectionsByName(List<Collection> collections, String loweredSearch) {
    List<Collection> filtered = new ArrayList<>();
    for (Collection collection : collections) {
      String name = StringUtils.defaultString(collection.getName()).toLowerCase();
      if (name.contains(loweredSearch)) {
        filtered.add(collection);
      }
    }
    return filtered;
  }

  private void appendFoldersJson(StringBuilder sb, List<Folder> folders) {
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
  }

  private void appendCollectionsJson(StringBuilder sb, List<Collection> collections) {
    boolean first = true;
    for (Collection collection : collections) {
      if (!first) {
        sb.append(",");
      }
      first = false;

      sb.append("{");
      sb.append("\"id\":").append(collection.getId()).append(",");
      sb.append("\"name\":\"").append(JsonCommand.toJson(StringUtils.defaultString(collection.getName()))).append("\",");
      sb.append("\"uniqueId\":\"").append(JsonCommand.toJson(StringUtils.defaultString(collection.getUniqueId()))).append("\",");
      sb.append("\"summary\":\"").append(JsonCommand.toJson(StringUtils.defaultString(collection.getDescription()))).append("\",");
      sb.append("\"itemCount\":").append(collection.getItemCount()).append(",");
      sb.append("\"hasAllowedGroups\":").append(collection.doAllowedGroupsCheck());
      sb.append("}");
    }
  }
}
