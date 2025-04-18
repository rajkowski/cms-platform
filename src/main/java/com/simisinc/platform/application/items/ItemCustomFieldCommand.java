/*
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

package com.simisinc.platform.application.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.CustomFieldCommand;
import com.simisinc.platform.application.CustomFieldFormatCommand;
import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Category;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.infrastructure.persistence.items.CategoryRepository;
import com.simisinc.platform.presentation.widgets.cms.PreferenceEntriesList;

/**
 * Turns widget preferences into Form Fields
 *
 * @author matt rajkowski
 * @created 4/28/18 2:23 PM
 */
public class ItemCustomFieldCommand {

  private static Log LOG = LogFactory.getLog(ItemCustomFieldCommand.class);

  public static List<CustomField> renderDisplayValues(PreferenceEntriesList entriesList, Item item) {
    return parseCustomFields(entriesList, item, true);
  }

  public static List<CustomField> prepareFormValues(PreferenceEntriesList entriesList, Item item) {
    return parseCustomFields(entriesList, item, false);
  }

  public static List<CustomField> parseCustomFields(PreferenceEntriesList entriesList, Item item,
      boolean requireValue) {
    List<CustomField> fieldList = new ArrayList<>();
    for (Map<String, String> valueMap : entriesList) {
      try {

        // Start a custom field
        CustomField customField = CustomFieldCommand.createCustomField(valueMap);

        // Determine the field's value
        String objectParameter = valueMap.get("value");
        if (customField == null || StringUtils.isBlank(objectParameter)) {
          continue;
        }

        // Determine the values to show
        String value = null;
        if (objectParameter.startsWith("custom.")) {
          String customFieldName = objectParameter.substring("custom.".length());
          CustomField field = item.getCustomField(customFieldName);
          if (field != null) {
            value = field.getValue();
          }
        } else if ("categoryList".equals(objectParameter)) {
          List<Category> categoryList = CategoryRepository.findAllByItemId(item.getId());
          if (categoryList != null && !categoryList.isEmpty()) {
            for (Category category : categoryList) {
              if (value == null) {
                value = category.getName();
              } else {
                value += "; " + category.getName();
              }
            }
          }
        } else {
          value = BeanUtils.getProperty(item, objectParameter);
          if ("url".equals(objectParameter)) {
            Collection collection = LoadCollectionCommand.loadCollectionById(item.getCollectionId());
            Category category = LoadCategoryCommand.loadCategoryById(item.getCategoryId());
            if (StringUtils.isNotBlank(item.getUrlText())) {
              customField.setLabel(item.getUrlText());
            } else if (category != null && StringUtils.isNotBlank(category.getItemUrlText())) {
              customField.setLabel(category.getItemUrlText());
            } else if (collection != null && StringUtils.isNotBlank(collection.getItemUrlText())) {
              customField.setLabel(collection.getItemUrlText());
            }
          }
        }

        // Format the values given the rules
        CustomFieldFormatCommand.formatValue(customField, value);
        if (requireValue && StringUtils.isBlank(customField.getValue())) {
          continue;
        }
        fieldList.add(customField);
      } catch (Exception e) {
        LOG.error("Could not parse custom field: " + e.getMessage());
      }
    }
    return fieldList;
  }
}
