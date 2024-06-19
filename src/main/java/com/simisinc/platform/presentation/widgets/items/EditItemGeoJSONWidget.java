/*
 * Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.application.items.CheckCollectionPermissionCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.ItemRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Form to edit GeoJSON values
 *
 * @author matt rajkowski
 * @created 5/27/24 2:39 PM
 */
public class EditItemGeoJSONWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(EditItemGeoJSONWidget.class);

  static String JSP = "/items/item-geosjon-form.jsp";
  static String NEED_PERMISSION_JSP = "/items/item-need-edit-permission.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Determine the item and verify access
    long userId = context.getUserId();
    String itemUniqueId = context.getPreferences().get("uniqueId");
    if (itemUniqueId == null) {
      return null;
    }
    Item item = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(itemUniqueId, userId);
    if (item == null) {
      return null;
    }

    // Determine the collection
    Collection collection = LoadCollectionCommand.loadCollectionByIdForAuthorizedUser(item.getCollectionId(), userId);
    if (collection == null) {
      return null;
    }
    context.getRequest().setAttribute("collection", collection);

    // See if the user group can edit any item in this collection
    boolean canEditItem = CheckCollectionPermissionCommand.userHasEditPermission(collection.getId(), userId);
    if (!canEditItem) {
      context.setJsp(NEED_PERMISSION_JSP);
      return context;
    }

    // Form bean
    if (context.getRequestObject() != null) {
      context.getRequest().setAttribute("item", context.getRequestObject());
    } else {
      context.getRequest().setAttribute("item", item);
    }

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Preferences
    context.getRequest().setAttribute("returnPage", context.getPreferences().getOrDefault("returnPage",
        UrlCommand.getValidReturnPage(context.getParameter("returnPage"))));

    // Determine the cancel page
    String cancelUrl = context.getPreferences().get("cancelUrl");
    if (StringUtils.isBlank(cancelUrl)) {
      cancelUrl = "/show/" + item.getUniqueId();
    }
    context.getRequest().setAttribute("cancelUrl", cancelUrl);

    // Show the JSP
    context.setJsp(JSP);
    return context;
  }

  public WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException {

    LOG.info("EditItemGeoJSONWidget start...");

    // Determine the item and verify access
    long userId = context.getUserId();
    String itemUniqueId = context.getPreferences().get("uniqueId");
    if (itemUniqueId == null) {
      return null;
    }
    Item itemBean = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(itemUniqueId, userId);
    if (itemBean == null) {
      return null;
    }
    Item previousBean = LoadItemCommand.loadItemById(itemBean.getId());
    if (previousBean == null) {
      return null;
    }

    // Determine the collection
    Collection collection = LoadCollectionCommand.loadCollectionByIdForAuthorizedUser(previousBean.getCollectionId(),
        userId);

    // See if the user group can edit any item in this collection
    boolean canEditItem = CheckCollectionPermissionCommand.userHasEditPermission(itemBean.getCollectionId(), userId);
    if (!canEditItem) {
      context.setJsp(NEED_PERMISSION_JSP);
      return context;
    }

    // Populate the fields
    itemBean.setGeoJSON(context.getParameter("geoJSON"));
    itemBean.setModifiedBy(context.getUserId());
    itemBean.setIpAddress(context.getRequest().getRemoteAddr());

    // Save the item
    Item item = null;
    try {
      item = ItemRepository.updateGeoJSON(itemBean);
      if (item == null) {
        throw new DataException("GeoJSON information could not be saved due to a system error. Please try again.");
      }
    } catch (DataException e) {
      context.setErrorMessage(e.getMessage());
      context.setRequestObject(itemBean);
      return context;
    }

    // Determine the page to return to
    String returnPage = context.getPreferences().getOrDefault("returnPage",
        UrlCommand.getValidReturnPage(context.getParameter("returnPage")));
    if (StringUtils.isNotBlank(returnPage)) {
      // Go to the item (could be renamed)
      if (returnPage.startsWith("/show/")) {
        returnPage = "/show/" + item.getUniqueId();
      }
    } else {
      // Go to the overview page
      returnPage = collection.createListingsLink();
    }
    context.setSuccessMessage("The record was saved!");
    context.setRedirect(returnPage);
    return context;
  }
}
