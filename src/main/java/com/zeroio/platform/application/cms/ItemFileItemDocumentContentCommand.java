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

package com.zeroio.platform.application.cms;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;

/**
 * Command for extracting text from documents
 *
 * @author matt rajkowski
 * @created 8/6/26 5:00 PM
 */
public class ItemFileItemDocumentContentCommand {

  private static Log LOG = LogFactory.getLog(ItemFileItemDocumentContentCommand.class);

  /**
   * Updates the text index for the given item file item
   *
   * @param itemFileItem the item file item
   * @return true if successful, false otherwise
   */
  public static boolean updateTextIndex(ItemFileItem fileItem) {
    if (fileItem == null) {
      return false;
    }
    File file = FileSystemCommand.getFileServerRootPath(fileItem.getFileServerPath());
    if (!file.exists()) {
      LOG.warn("File does not exist: " + file.getPath());
      return false;
    }
    // Process PDF files
    if (fileItem.getFileType().equals("pdf")) {
      String text = ExtractTextFromPDFCommand.textFromFile(file);
      ItemFileItemRepository.updateDocumentText(fileItem, text);
      return true;
    }
    return false;
  }
}
