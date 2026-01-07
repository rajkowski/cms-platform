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

package com.simisinc.platform.application.items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Collection;

/**
 * Methods to display information about a collection's table columns
 *
 * @author matt rajkowski
 * @created 6/1/2024 5:24 PM
 */
public class CollectionTableColumnsCommand {

  public static Map<String, CustomField> getPossibleTableColumns() {
    Map<String, CustomField> list = new LinkedHashMap<>();
    addToList(list, new CustomField("image", "Image"));
    addToList(list, new CustomField("name", "Name"));
    addToList(list, new CustomField("uniqueId", "Unique Id"));
    addToList(list, new CustomField("category", "Category"));
    addToList(list, new CustomField("summary", "Summary"));
    addToList(list, new CustomField("description", "HTML Description"));
    addToList(list, new CustomField("textDescription", "Text Description"));
    addToList(list, new CustomField("keywords", "Keywords"));
    addToList(list, new CustomField("geopoint", "Geo Point"));
    addToList(list, new CustomField("latitude", "Latitude"));
    addToList(list, new CustomField("longitude", "Longitude"));
    addToList(list, new CustomField("location", "Location"));
    addToList(list, new CustomField("street", "Street Address"));
    addToList(list, new CustomField("addressLine2", "Street Address Line 2"));
    addToList(list, new CustomField("addressLine3", "Street Address Line 3"));
    addToList(list, new CustomField("city", "City"));
    addToList(list, new CustomField("state", "State"));
    addToList(list, new CustomField("postalCode", "Postal Code"));
    addToList(list, new CustomField("country", "Country"));
    addToList(list, new CustomField("county", "County"));
    addToList(list, new CustomField("phoneNumber", "Phone Number"));
    addToList(list, new CustomField("email", "Email Address"));
    addToList(list, new CustomField("cost", "Cost"));
    addToList(list, new CustomField("startDate", "Start Date"));
    addToList(list, new CustomField("endDate", "End Date"));
    addToList(list, new CustomField("expectedDate", "Expected Date"));
    addToList(list, new CustomField("expirationDate", "Expiration Date"));
    addToList(list, new CustomField("url", "URL"));
    addToList(list, new CustomField("imageUrl", "Image URL"));
    addToList(list, new CustomField("barcode", "Barcode"));
    addToList(list, new CustomField("assignedTo", "Assigned To"));
    addToList(list, new CustomField("privacyType", "Privacy Type"));
    return list;
  }

  public static void addToList(Map<String, CustomField> list, CustomField field) {
    list.put(field.getName(), field);
  }

  public static Map<String, CustomField> createListFromSettings(Collection collection, String commaSeparatedValues) {
    if (collection == null) {
      return null;
    }
    // Prepare a list of columns to return
    Map<String, CustomField> tableColumnsList = new LinkedHashMap<>();

    // Determine the possible columns to display, including any custom fields
    Map<String, CustomField> possibleTableColumnList = CollectionTableColumnsCommand.getPossibleTableColumns();
    if (collection.getCustomFieldList() != null) {
      Map<String, CustomField> customFieldList = collection.getCustomFieldList();
      possibleTableColumnList.putAll(customFieldList);
    }

    // Use the widget preference first, if there is one
    if (StringUtils.isNotBlank(commaSeparatedValues)) {
      String[] columns = commaSeparatedValues.split(Pattern.quote(","));
      for (String name : columns) {
        CollectionTableColumnsCommand.addToList(tableColumnsList, possibleTableColumnList.get(name.trim()));
      }
    }

    // Next check the collection setting, if there is one
    if (tableColumnsList.isEmpty() && collection.getTableColumnsList() != null) {
      for (CustomField columnField : collection.getTableColumnsList().values()) {
        CollectionTableColumnsCommand.addToList(tableColumnsList, possibleTableColumnList.get(columnField.getName()));
      }
    }

    // Finally, just set some defaults
    if (tableColumnsList.isEmpty()) {
      CollectionTableColumnsCommand.addToList(tableColumnsList, possibleTableColumnList.get("name"));
    }

    return tableColumnsList;
  }
}
