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

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.items.CheckCollectionPermissionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.zeroio.platform.application.items.SetCurrentItemVersionCommand;

/**
 * Restores an item from one of its previous versions.
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ItemVersionRestoreAjax extends GenericJsonService {

  @Override
  public JsonServiceContext post(JsonServiceContext context) {

    long itemId = context.getParameterAsLong("itemId", -1L);
    long versionId = context.getParameterAsLong("versionId", -1L);
    if (itemId <= 0 || versionId <= 0) {
      return context.writeError("Item ID and Version ID are required");
    }

    Item item = LoadItemCommand.loadItemById(itemId);
    if (item == null) {
      return context.writeError("The item could not be found");
    }
    Item authorizedItem = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(item.getUniqueId(), context.getUserId());
    if (authorizedItem == null) {
      return context.writeError("Permission denied");
    }

    boolean canEditItem = CheckCollectionPermissionCommand.userHasEditPermission(item.getCollectionId(), context.getUserId());
    if (!canEditItem) {
      return context.writeError("Permission denied");
    }

    try {
      Item restored = SetCurrentItemVersionCommand.setCurrentVersion(itemId, versionId, context.getUserId());
      context.setJson("{\"success\":true,\"message\":\"Version restored\",\"itemUniqueId\":\"" + restored.getUniqueId() + "\"}");
      return context;
    } catch (DataException e) {
      return context.writeError(e.getMessage());
    } catch (Exception e) {
      return context.writeError("Failed to restore version");
    }
  }
}
