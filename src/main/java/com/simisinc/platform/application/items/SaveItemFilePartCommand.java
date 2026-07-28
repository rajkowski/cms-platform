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

package com.simisinc.platform.application.items;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveFilePartCommand;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.zeroio.platform.presentation.controller.FileModulesConstants;

/**
 * Validates and saves an item's uploaded file item object
 *
 * @author matt rajkowski
 * @created 4/19/2021 1:00 PM
 */
public class SaveItemFilePartCommand {

  private static Log LOG = LogFactory.getLog(SaveItemFilePartCommand.class);

  public static ItemFileItem saveFile(WidgetContext context, Item item) throws DataException {

    FileItem fileItem = null;
    try {
      fileItem = SaveFilePartCommand.saveFile(FileModulesConstants.ITEMS_UPLOADS, context.getPart("file"), context.getUserId());
    } catch (Exception e) {
      LOG.warn("Could not handle file: " + e.getMessage());
      // Clean up the file
      SaveFilePartCommand.cleanupFile(fileItem);
      throw new DataException("There was an issue with the file");
    }

    // Populate the fields
    ItemFileItem fileItemBean = new ItemFileItem();
    fileItemBean.setItemId(item.getId());
    fileItemBean.setFilename(fileItem.getFilename());
    fileItemBean.setFileLength(fileItem.getFileLength());
    fileItemBean.setFileServerPath(fileItem.getFileServerPath());
    fileItemBean.setExtension(fileItem.getExtension());
    fileItemBean.setCreatedBy(context.getUserId());
    fileItemBean.setModifiedBy(context.getUserId());
    return fileItemBean;
  }

  public static void cleanupFile(ItemFileItem fileItemBean) {
    if (fileItemBean == null) {
      return;
    }
    File tempFile = FileSystemCommand.getFileServerRootPath(fileItemBean.getFileServerPath());
    if (tempFile.exists()) {
      LOG.warn("Deleting an uploaded file: " + tempFile.getPath());
      tempFile.delete();
    }
  }
}
