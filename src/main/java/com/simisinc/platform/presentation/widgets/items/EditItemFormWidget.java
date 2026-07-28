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

import com.simisinc.platform.application.CustomFieldListMergeCommand;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.application.items.CheckCollectionPermissionCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.application.items.SaveItemCommand;
import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.CategoryRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.simisinc.platform.presentation.widgets.cms.PreferenceEntriesList;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 8/15/18 8:53 AM
 */
public class EditItemFormWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  protected static Log LOG = LogFactory.getLog(EditItemFormWidget.class);

  static String FULL_FORM_JSP = "/items/item-full-form.jsp";
  static String BUSINESS_FORM_JSP = "/items/item-business-form.jsp";
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

    // Provide a category drop-down
    List<Category> categoryList = CategoryRepository.findAllByCollectionId(collection.getId());
    context.getRequest().setAttribute("categoryList", categoryList);

    // Split the list into multiple lists for the UI
    int columnSize = (int) Math.ceil((double) categoryList.size() / 2);
    if (columnSize > 0) {
      List<List<Category>> columnList = ListUtils.partition(categoryList, columnSize);
      if (columnList.size() > 0) {
        context.getRequest().setAttribute("categoryList1", columnList.get(0));
        if (columnList.size() > 1) {
          context.getRequest().setAttribute("categoryList2", columnList.get(1));
        }
      }
    }

    // Form bean
    if (context.getRequestObject() != null) {
      context.getRequest().setAttribute("item", context.getRequestObject());
    } else {
      context.getRequest().setAttribute("item", item);
    }

    // Combine the lists
    Map<String, CustomField> customFieldList = CustomFieldListMergeCommand.mergeCustomFieldLists(
        collection.getCustomFieldList(),
        item.getCustomFieldList());

    // If specific fields are configured, restrict which fields are shown
    PreferenceEntriesList entriesList = context.getPreferenceAsDataList("fields");
    if (!entriesList.isEmpty()) {
      Set<String> allowedFields = new LinkedHashSet<>();
      Map<String, String> fieldLabels = new LinkedHashMap<>();
      for (Map<String, String> entry : entriesList) {
        String objectParam = entry.get("value");
        String fieldName = entry.get("name");
        if (StringUtils.isNotBlank(objectParam)) {
          allowedFields.add(objectParam);
          if (StringUtils.isNotBlank(fieldName)) {
            fieldLabels.put(objectParam, fieldName);
          }
        }
      }
      if (!allowedFields.isEmpty()) {
        context.getRequest().setAttribute("allowedFields", allowedFields);
        context.getRequest().setAttribute("fieldLabels", fieldLabels);
        // Filter the custom field list to only include fields specified in the preference
        if (customFieldList != null) {
          Map<String, CustomField> filteredCustomFieldList = new LinkedHashMap<>();
          for (Map.Entry<String, CustomField> entry : customFieldList.entrySet()) {
            if (allowedFields.contains("custom." + entry.getKey())) {
              filteredCustomFieldList.put(entry.getKey(), entry.getValue());
            }
          }
          customFieldList = filteredCustomFieldList;
        }
      }
    }
    context.getRequest().setAttribute("customFieldList", customFieldList);

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
    context.setJsp(FULL_FORM_JSP);
    return context;
  }

  public WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException {

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
    BeanUtils.populate(itemBean, context.getParameterMap());
    itemBean.setModifiedBy(context.getUserId());
    itemBean.setIpAddress(context.getRequest().getRemoteAddr());

    // If a tags string was submitted, parse it into an array
    String tagsParam = context.getParameter("tags");
    if (tagsParam != null) {
      if (tagsParam.isBlank()) {
        itemBean.setTags(null);
      } else {
        String[] tagsArray = tagsParam.split(",");
        for (int i = 0; i < tagsArray.length; i++) {
          tagsArray[i] = tagsArray[i].trim();
        }
        itemBean.setTags(tagsArray);
      }
    }

    // Determine which custom fields are allowed (based on the fields preference, if set)
    PreferenceEntriesList fieldEntriesList = context.getPreferenceAsDataList("fields");
    Set<String> allowedCustomFieldKeys = null;
    if (!fieldEntriesList.isEmpty()) {
      allowedCustomFieldKeys = new LinkedHashSet<>();
      for (Map<String, String> entry : fieldEntriesList) {
        String objectParam = entry.get("value");
        if (StringUtils.isNotBlank(objectParam) && objectParam.startsWith("custom.")) {
          allowedCustomFieldKeys.add(objectParam.substring("custom.".length()));
        }
      }
    }

    // Handle the categories - if category fields were not submitted, preserve existing assignments
    if (context.getParameter("categoryId") == null) {
      itemBean.setCategoryId(previousBean.getCategoryId());
      itemBean.setCategoryIdList(previousBean.getCategoryIdList());
    } else {
      long mainCategoryId = itemBean.getCategoryId();
      if (mainCategoryId == 0) {
        mainCategoryId = -1;
      }
      List<Category> categoryList = CategoryRepository.findAllByCollectionId(itemBean.getCollectionId());
      List<Long> categoryIdList = new ArrayList<>();
      for (Category category : categoryList) {
        long categoryId = context.getParameterAsLong("categoryId" + category.getId());
        if (categoryId != -1) {
          categoryIdList.add(categoryId);
          if (mainCategoryId == -1) {
            mainCategoryId = categoryId;
          }
        }
      }
      if (mainCategoryId != -1 && !categoryIdList.contains(mainCategoryId)) {
        categoryIdList.add(mainCategoryId);
      }
      itemBean.setCategoryId(mainCategoryId);
      itemBean.setCategoryIdList(categoryIdList.toArray(new Long[0]));
    }

    // Determine custom fields to check for
    Map<String, CustomField> customFieldList = CustomFieldListMergeCommand.mergeCustomFieldLists(
        collection.getCustomFieldList(),
        previousBean.getCustomFieldList());

    // Check the request for custom field values
    if (customFieldList != null) {
      for (CustomField field : customFieldList.values()) {
        // Skip custom fields not in the allowed list when fields preference is specified
        if (allowedCustomFieldKeys != null && !allowedCustomFieldKeys.contains(field.getName())) {
          itemBean.addCustomField(field);
          continue;
        }
        String parameterName = context.getUniqueId() + field.getName();
        if ("multi-select list".equals(field.getType()) && field.getListOfOptions() != null) {
          String[] parameterValues = context.getParameterMap().get(parameterName);
          if (parameterValues != null && parameterValues.length > 0) {
            List<String> selectedValues = new ArrayList<>();
            for (String val : parameterValues) {
              if (field.getListOfOptions().containsKey(val)) {
                selectedValues.add(field.getListOfOptions().get(val));
              }
            }
            field.setValue(selectedValues.isEmpty() ? null : String.join(", ", selectedValues));
          } else {
            field.setValue(null);
          }
        } else {
          String parameterValue = context.getParameter(parameterName);
          if ("list".equals(field.getType()) && field.getListOfOptions() != null) {
            field.setValue(field.getListOfOptions().get(parameterValue));
          } else {
            field.setValue(parameterValue);
          }
        }
        itemBean.addCustomField(field);
      }
    }

    // Save the item
    Item item = null;
    try {
      item = SaveItemCommand.saveItem(itemBean);
      if (item == null) {
        throw new DataException("Your information could not be saved due to a system error. Please try again.");
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
    context.setSuccessMessage("Thanks, the record was saved!");
    context.setRedirect(returnPage);
    return context;
  }
}
