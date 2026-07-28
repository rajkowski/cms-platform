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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.ValidateFileCommand;
import com.simisinc.platform.application.items.CheckCollectionPermissionCommand;
import com.simisinc.platform.application.items.CheckItemFolderPermissionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.application.items.SaveItemFileCommand;
import com.simisinc.platform.application.items.SaveItemFilePartCommand;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;

/**
 * Uploads a new version of an existing file for an item
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class ItemFileVersionUploadAjax extends GenericJsonService {

  private static Log LOG = LogFactory.getLog(ItemFileVersionUploadAjax.class);

  @Override
  public JsonServiceContext post(JsonServiceContext context) {

    try {
      // Check the required item record
      String itemUniqueId = context.getParameter("itemId");
      Item item = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(
          itemUniqueId, context.getUserId());
      if (item == null) {
        return context.writeError("Invalid item");
      }

      // Check for the required fileId
      long fileId = context.getParameterAsLong("fileId", -1);
      if (fileId == -1) {
        return context.writeError("File ID required");
      }

      // Check for the existing file
      ItemFileItem existingFile = ItemFileItemRepository.findById(fileId);
      if (existingFile == null) {
        return context.writeError("File not found");
      }

      // Check permissions for the user to add a file to the item
      boolean hasPermission = false;
      if (context.hasRole("admin") ||
          CheckCollectionPermissionCommand.userHasEditPermission(item.getCollectionId(), context.getUserId()) ||
          CheckItemFolderPermissionCommand.userHasEditPermission(existingFile.getFolderId(), context.getUserId())) {
        hasPermission = true;
      }
      if (!hasPermission) {
        return context.writeError("You do not have permission to upload a new version of this file");
      }

      // Save the file to the file system, populate the file bean, and validate
      ItemFileItem itemFileBean = SaveItemFilePartCommand.saveFile(context, item);
      if (itemFileBean == null) {
        return context.writeError("No file uploaded");
      }

      // Copy identifying info from the existing record to become new version
      itemFileBean.setId(existingFile.getId());
      itemFileBean.setFolderId(existingFile.getFolderId());
      itemFileBean.setSubFolderId(existingFile.getSubFolderId());
      itemFileBean.setCategoryId(existingFile.getCategoryId());
      itemFileBean.setTitle(existingFile.getTitle());
      itemFileBean.setSummary(existingFile.getSummary());

      // Validate and add additional info to the file bean
      ValidateFileCommand.checkFile(itemFileBean);

      // Save as a new version
      ItemFileItem saved = SaveItemFileCommand.saveNewVersionOfFile(itemFileBean);
      if (saved == null) {
        SaveItemFilePartCommand.cleanupFile(itemFileBean);
        return context.writeError("Unable to save file");
      }

      String fileSize = "";
      if (saved.getFileLength() > 0) {
        // simple size (you can format better if needed)
        fileSize = saved.getFileLength() + " bytes";
      }

      context.setJson("{"
          + "\"success\": true,"
          + "\"id\": " + saved.getId() + ","
          + "\"filename\": \"" + saved.getFilename() + "\","
          + "\"url\": \"/show/" + itemUniqueId + "/assets/file/" + saved.getUrl() + "\","
          + "\"size\": \"" + fileSize + "\""
          + "}");

      return context;

    } catch (Exception e) {
      LOG.error("Upload failed", e);
      return context.writeError("Upload failed");
    }
  }
}