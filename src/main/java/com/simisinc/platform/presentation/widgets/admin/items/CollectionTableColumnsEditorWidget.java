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

package com.simisinc.platform.presentation.widgets.admin.items;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.items.CollectionTableColumnsCommand;
import com.simisinc.platform.domain.model.CustomField;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.infrastructure.persistence.items.CollectionRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Editor for managing a collection's table columns
 *
 * @author matt rajkowski
 * @created 5/26/2024 8:12 AM
 */
public class CollectionTableColumnsEditorWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  private static Log LOG = LogFactory.getLog(CollectionTableColumnsEditorWidget.class);

  static String JSP = "/admin/collection-table-columns-editor.jsp";

  public WidgetContext execute(WidgetContext context) {

    // Standard request items
    context.getRequest().setAttribute("icon", context.getPreferences().get("icon"));
    context.getRequest().setAttribute("title", context.getPreferences().get("title"));

    // Determine the collection
    long collectionId = context.getParameterAsLong("collectionId");
    Collection collection = CollectionRepository.findById(collectionId);
    if (collection == null) {
      context.setErrorMessage("Error. Collection was not found.");
      return context;
    }
    context.getRequest().setAttribute("collection", collection);

    // Determine the fields for the editor, including any custom fields
    Map<String, CustomField> inactiveTableColumnList = CollectionTableColumnsCommand.getPossibleTableColumns();
    if (collection.getCustomFieldList() != null) {
      Map<String, CustomField> customFieldList = collection.getCustomFieldList();
      inactiveTableColumnList.putAll(customFieldList);
    }

    // Split the fields up into active/inactive based on the actual table columns
    Map<String, CustomField> activeTableColumnList = new LinkedHashMap<>();
    if (collection.getTableColumnsList() != null) {
      for (CustomField activeField : collection.getTableColumnsList().values()) {
        activeTableColumnList.put(activeField.getName(), activeField);
        inactiveTableColumnList.remove(activeField.getName());
      }
    }

    // Set the current value for the editor
    String tableColumnsValue = activeTableColumnList.entrySet().stream()
        .map(e -> e.getKey())
        .collect(Collectors.joining("|"));
    context.getRequest().setAttribute("tableColumnsValue", tableColumnsValue);
    context.getRequest().setAttribute("activeTableColumnsList", new ArrayList<>(activeTableColumnList.values()));
    context.getRequest().setAttribute("inactiveTableColumnsList", new ArrayList<>(inactiveTableColumnList.values()));

    // Show the editor
    context.setJsp(JSP);
    return context;
  }

  public WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException {

    // Check the collection
    long collectionId = context.getParameterAsLong("collectionId");
    Collection collection = CollectionRepository.findById(collectionId);
    if (collection == null) {
      context.setErrorMessage("Collection was not found");
      return context;
    }

    // Determine form request parameters
    String tableColumnsValue = context.getParameter("tableColumnsValue");
    LOG.debug("tableColumnsValue = " + tableColumnsValue);

    // Reset the list
    if (collection.getTableColumnsList() != null) {
      collection.getTableColumnsList().clear();
    }

    // Add any values
    if (StringUtils.isNotBlank(tableColumnsValue)) {

      // Determine the fields allowed, including any custom fields
      Map<String, CustomField> referenceList = CollectionTableColumnsCommand.getPossibleTableColumns();
      if (collection.getCustomFieldList() != null) {
        Map<String, CustomField> customFieldList = collection.getCustomFieldList();
        referenceList.putAll(customFieldList);
      }

      // Verify the posted fields and set them
      String[] tableColumnList = tableColumnsValue.split(Pattern.quote("|"));
      for (String name : tableColumnList) {
        LOG.debug("Adding: " + name);
        collection.addTableColumn(referenceList.get(name));
      }
    }

    try {
      // Update the repository
      CollectionRepository.updateTableColumns(collection);
      context.setSuccessMessage("The changes were saved");
    } catch (Exception e) {
      context.setErrorMessage(e.getMessage());
    }

    // Determine the page to return to
    context.setRedirect("/admin/collection-table-columns-editor?collectionId=" + collection.getId());
    return context;
  }

}
