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
package com.zeroio.platform.presentation.widgets.items;

import com.simisinc.platform.application.items.CheckCollectionPermissionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Displays a button and modal for browsing and restoring item versions.
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ItemVersionHistoryWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908898L;
  static String jsp = "/items/item-version-history-widget.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {
    // Common attributes
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Validate the item
    String itemUniqueId = context.getPreferences().getOrDefault("uniqueId", context.getCoreData().get("itemUniqueId"));
    Item item = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(itemUniqueId, context.getUserId());
    if (item == null) {
      return null;
    }

    // Check permissions
    boolean canEditItem = CheckCollectionPermissionCommand.userHasEditPermission(item.getCollectionId(),
        context.getUserId());
    if (!canEditItem) {
      return null;
    }

    context.getRequest().setAttribute("item", item);

    context.setJsp(jsp);
    return context;
  }
}
