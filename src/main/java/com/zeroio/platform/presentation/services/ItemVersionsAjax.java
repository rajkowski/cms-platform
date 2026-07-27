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
package com.zeroio.platform.presentation.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.zeroio.platform.domain.model.items.ItemVersion;
import com.zeroio.platform.infrastructure.persistence.items.ItemVersionRepository;
import com.zeroio.platform.infrastructure.persistence.items.ItemVersionSpecification;

/**
 * Returns version history for a single item.
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ItemVersionsAjax extends GenericJsonService {

  @Override
  public JsonServiceContext get(JsonServiceContext context) {
    long itemId = context.getParameterAsLong("itemId", -1L);
    if (itemId <= 0) {
      return context.writeError("Invalid item ID");
    }

    if (!isAuthorized(context, itemId)) {
      return context.writeError("Permission denied");
    }

    ItemVersionSpecification specification = new ItemVersionSpecification();
    specification.setItemId(itemId);
    List<ItemVersion> versions = ItemVersionRepository.findAll(specification, new DataConstraints());
    Map<Long, User> userMap = getUserMap(versions);

    StringBuilder json = new StringBuilder();
    json.append("[");
    boolean first = true;
    for (ItemVersion version : versions) {
      if (!first) {
        json.append(",");
      }
      first = false;
      appendVersionJson(json, version, userMap.get(version.getCreatedBy()));
    }
    json.append("]");

    return context.writeOk(json.toString());
  }

  private static boolean isAuthorized(JsonServiceContext context, long itemId) {
    Item item = LoadItemCommand.loadItemById(itemId);
    if (item == null) {
      return false;
    }
    Item authorizedItem = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(item.getUniqueId(), context.getUserId());
    return authorizedItem != null;
  }

  private static Map<Long, User> getUserMap(List<ItemVersion> versions) {
    Map<Long, User> userMap = new HashMap<>();
    Set<Long> userIds = new HashSet<>();
    for (ItemVersion version : versions) {
      if (version.getCreatedBy() > 0) {
        userIds.add(version.getCreatedBy());
      }
    }
    for (Long userId : userIds) {
      User user = UserRepository.findByUserId(userId);
      if (user != null) {
        userMap.put(userId, user);
      }
    }
    return userMap;
  }

  private static void appendVersionJson(StringBuilder json, ItemVersion version, User user) {
    String createdByName = user != null ? StringUtils.defaultString(user.getFullName()) : "";
    json.append("{");
    json.append("\"versionId\":").append(version.getId()).append(",");
    json.append("\"itemId\":").append(version.getItemId()).append(",");
    json.append("\"name\":\"").append(JsonCommand.toJson(StringUtils.defaultString(version.getName()))).append("\",");
    json.append("\"createdBy\":").append(version.getCreatedBy()).append(",");
    json.append("\"createdByName\":\"").append(JsonCommand.toJson(createdByName)).append("\",");
    json.append("\"created\":\"").append(version.getCreated() != null ? version.getCreated().toString() : "").append("\",");
    json.append("\"versionData\":\"").append(JsonCommand.toJson(StringUtils.defaultString(version.getVersionData()))).append("\"");
    json.append("}");
  }
}
