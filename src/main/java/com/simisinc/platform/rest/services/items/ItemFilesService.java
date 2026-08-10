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

package com.simisinc.platform.rest.services.items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileSpecification;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;
import com.simisinc.platform.rest.controller.ServiceResponseCommand;

/**
 * Returns a list of item files
 *
 * @author matt rajkowski
 * @created 7/28/26 10:15 AM
 */
public class ItemFilesService extends GenericRestService {

  private static Log LOG = LogFactory.getLog(ItemFilesService.class);

  private static final String EXPAND_DOCUMENT_TEXT = "documentText";

  // GET /item/{itemUniqueId}/files
  @Override
  public ServiceResponse get(ServiceContext context) {

    // Determine the item
    String itemUniqueId = context.getPathParam();
    Item item = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(itemUniqueId, context.getUserId());
    if (item == null) {
      ServiceResponse response = new ServiceResponse(404);
      response.getError().put("title", "Item was not found");
      return response;
    }

    // Validate access to the collection
    Collection collection = LoadCollectionCommand.loadCollectionByIdForAuthorizedUser(item.getCollectionId(),
        context.getUserId());
    if (collection == null) {
      LOG.warn("User does not have access to this collection");
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", "Item was not found");
      return response;
    }

    // Return the ItemDetailResponse if requested
    boolean expandDocumentText = false;
    String[] expand = context.getParameterAsArray("expand");
    if (expand != null) {
      for (String expandValue : expand) {
        if (EXPAND_DOCUMENT_TEXT.equals(expandValue)) {
          expandDocumentText = true;
        }
      }
    }

    // Retrieve all files for the item within user visibility
    ItemFileSpecification specification = new ItemFileSpecification();
    specification.setItemId(item.getId());
    specification.setForUserId(context.getUserId());
    specification.setIncludeDocumentText(expandDocumentText);

    // Enable paging
    int pageNumber = context.getParameterAsInt("page", 1);
    int pageSize = context.getParameterAsInt("size", 50);

    DataConstraints constraints = new DataConstraints(pageNumber, pageSize);
    constraints.setColumnToSortBy("created", "desc");

    List<ItemFileItem> itemFileList = ItemFileItemRepository.findAll(specification, constraints);
    List<Map<String, Object>> responseList = new ArrayList<>();
    for (ItemFileItem itemFile : itemFileList) {
      // Include filename, fileType, mimeType, fileLength, fileHash, webPath, created, modified
      Map<String, Object> entry = new LinkedHashMap<>();
      entry.put("uniqueId", itemFile.getWebPath());
      entry.put("filename", itemFile.getFilename());
      entry.put("fileType", itemFile.getFileType());
      entry.put("mimeType", itemFile.getMimeType());
      entry.put("fileLength", itemFile.getFileLength());
      entry.put("fileHash", itemFile.getFileHash());
      if (expandDocumentText) {
        entry.put("documentText", itemFile.getDocumentText());
      }
      entry.put("created", itemFile.getCreated() != null ? itemFile.getCreated().toInstant().toString() : null);
      entry.put("modified", itemFile.getModified() != null ? itemFile.getModified().toInstant().toString() : null);
      responseList.add(entry);
    }

    // Prepare the response
    ServiceResponse response = new ServiceResponse(200);
    ServiceResponseCommand.addMeta(response, "item-file", responseList, null);
    response.setData(responseList);
    return response;
  }
}
