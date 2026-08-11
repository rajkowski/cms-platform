/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

package com.simisinc.platform.presentation.widgets.items;

import java.util.List;

import com.simisinc.platform.application.items.ItemCustomFieldCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.simisinc.platform.presentation.widgets.cms.PreferenceEntriesList;

/**
 * Displays the fields for an item
 *
 * @author matt rajkowski
 * @created 4/20/18 2:23 PM
 */
public class ItemFieldsWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  static String JSP = "/portal/custom-fields.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Load the authorized item
    Item item = LoadItemCommand.loadItemByUniqueId(context.getCoreData().get("itemUniqueId"));
    if (item == null) {
      return null;
    }

    // Use the fields preference to determine the object properties to be shown
    PreferenceEntriesList entriesList = context.getPreferenceAsDataList("fields");
    List<CustomField> fieldList = ItemCustomFieldCommand.renderDisplayValues(entriesList, item);

    // Append the custom fields if requested
    boolean showAllCustomFields = "true".equals(context.getPreferences().get("showAllCustomFields"));
    if (showAllCustomFields && item.getCustomFieldList() != null && !item.getCustomFieldList().isEmpty()) {
      fieldList.addAll(item.getCustomFieldList().values());
    }

    // Show the fields unless there are none
    if (fieldList.isEmpty()) {
      return context;
    }
    context.getRequest().setAttribute("fieldList", fieldList);

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Set preferences
    context.getRequest().setAttribute("searchLink", context.getPreferences().getOrDefault("searchLink", "/search"));

    // Show the JSP
    context.setJsp(JSP);
    return context;
  }
}
