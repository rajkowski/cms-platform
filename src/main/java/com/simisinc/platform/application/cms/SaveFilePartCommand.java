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

package com.simisinc.platform.application.cms;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.Part;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.zeroio.platform.presentation.controller.FileModulesConstants;

/**
 * Validates, retrieves http file parts, and saves file item objects
 *
 * @author matt rajkowski
 * @created 12/18/18 3:11 PM
 */
public class SaveFilePartCommand {

  private static Log LOG = LogFactory.getLog(SaveFilePartCommand.class);
  private static String GLOBAL_MODULE = FileModulesConstants.UPLOADS;

  public static FileItem saveFile(WidgetContext context) throws DataException {
    return saveFileToModule(GLOBAL_MODULE, context);
  }

  public static FileItem saveFileToModule(String serverModule, WidgetContext context) throws DataException {
    try {
      Part filePart = context.getPart("file");
      return saveFile(serverModule, filePart, context.getUserId());
    } catch (Exception e) {
      throw new DataException("Could not save the uploaded file: " + e.getMessage());
    }
  }

  public static FileItem saveFile(String serverModule, Part filePart, long userId) throws DataException {

    // Validate the parameters
    if (filePart == null || StringUtils.isBlank(filePart.getSubmittedFileName())) {
      throw new DataException("An uploaded file was not found");
    }

    // Validate the file length
    long fileLength = filePart.getSize();
    if (fileLength <= 0) {
      throw new DataException("The uploaded file was empty");
    }

    // Write the file to the specified path
    String serverModulePath = FileSystemCommand.generateFileServerSubPath(serverModule);
    String submittedFilename = null;
    String extension = null;
    File tempFile = null;
    try {
      LOG.debug("Found a file...");
      submittedFilename = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
      extension = FileSystemCommand.cleanExtension(FilenameUtils.getExtension(submittedFilename));
      tempFile = FileSystemCommand.generateTempFile(serverModule, userId, extension);
      filePart.write(tempFile.getAbsolutePath());
    } catch (Exception e) {
      // Clean up the file
      if (tempFile != null && tempFile.exists()) {
        LOG.warn("Deleting an uploaded file: " + tempFile.getAbsolutePath());
        tempFile.delete();
      }
      LOG.debug("Could not save the uploaded file: " + e.getMessage());
      throw new DataException("The uploaded file could not be saved");
    }

    if (tempFile == null || tempFile.length() <= 0) {
      throw new DataException("The uploaded file could not be saved");
    }

    // Populate the fields
    FileItem fileItemBean = new FileItem();
    fileItemBean.setFilename(submittedFilename);
    fileItemBean.setFileLength(tempFile.length());
    fileItemBean.setFileServerPath(serverModulePath + tempFile.getName());
    fileItemBean.setExtension(extension);
    fileItemBean.setFileHash(FileSystemCommand.getFileChecksum(tempFile));
    fileItemBean.setCreatedBy(userId);
    fileItemBean.setModifiedBy(userId);
    return fileItemBean;
  }

  public static void cleanupFile(FileItem fileItemBean) {
    if (fileItemBean == null) {
      return;
    }
    File tempFile = FileSystemCommand.getFileServerRootPath(fileItemBean.getFileServerPath());
    if (tempFile.exists()) {
      LOG.warn("Deleting an uploaded file: " + tempFile.getPath());
      tempFile.delete();
    }
  }

  /**
   * Processes multiple file parts from a request
   *
   * @param fileParts collection of file parts to process
   * @param userId the user ID creating the files
   * @return list of FileItem objects for the saved files
   * @throws DataException if any file cannot be processed
   */
  public static List<FileItem> saveFiles(Collection<Part> fileParts, long userId) throws DataException {
    return saveFilesToModule(GLOBAL_MODULE, fileParts, userId);
  }

  public static List<FileItem> saveFilesToModule(String serverModule, Collection<Part> fileParts, long userId) throws DataException {
    List<FileItem> savedFiles = new ArrayList<>();

    for (Part filePart : fileParts) {
      // Save each file part and create a FileItem object
      FileItem fileItemBean = null;
      try {
        fileItemBean = saveFile(serverModule, filePart, userId);
        if (fileItemBean == null) {
          // Skip non-file parts (like token, widget)
          LOG.warn("File part could not be saved: " + filePart.getName());
          continue;
        }
        savedFiles.add(fileItemBean);

      } catch (Exception e) {
        LOG.warn("Could not handle file: " + e.getMessage());
        // Clean up the file if it was created
        if (fileItemBean != null) {
          SaveFilePartCommand.cleanupFile(fileItemBean);
        }
        // Continue processing other files instead of throwing
        LOG.warn("Continuing with remaining files after error");
      }
    }

    if (savedFiles.isEmpty()) {
      throw new DataException("No files were successfully uploaded");
    }

    return savedFiles;
  }
}
