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

package com.simisinc.platform.application.datasets;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveFilePartCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.datasets.Dataset;
import com.simisinc.platform.infrastructure.persistence.datasets.DatasetRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.zeroio.platform.presentation.controller.FileModulesConstants;

/**
 * Functions for working with dataset files
 *
 * @author matt rajkowski
 * @created 2/7/2020 4:25 PM
 */
public class DatasetUploadFileCommand {

  private static Log LOG = LogFactory.getLog(DatasetUploadFileCommand.class);

  public static boolean handleUpload(WidgetContext context, Dataset dataset) {

    // Validate allowed file types for datasets
    String fileType = dataset.getFileType();
    int type = DatasetFileCommand.type(fileType);
    String extension = DatasetFileCommand.extension(type);
    if (extension == null) {
      context.setErrorMessage("File type not supported");
      context.setRequestObject(dataset);
      return false;
    }

    // Save the file and update the dataset object
    FileItem fileItem = null;
    try {
      fileItem = SaveFilePartCommand.saveFileToModule(FileModulesConstants.DATASETS, context);
      dataset.setFilename(fileItem.getFilename());
      dataset.setLastDownload(new Timestamp(System.currentTimeMillis()));
      if (dataset.getFilename() == null) {
        dataset.setFilename("data." + extension);
      }
      dataset.setFileType(fileType);
      dataset.setFileLength(fileItem.getFileLength());
      dataset.setFileServerPath(fileItem.getFileServerPath());
      dataset.setFileHash(fileItem.getFileHash());
    } catch (Exception e) {
      LOG.warn("An error occurred", e);
      context.setErrorMessage("An error occurred with the file");
      context.setRequestObject(dataset);
      return false;
    }

    // Determine the web path for downloads, can randomize, etc.
    Date created = new Date(System.currentTimeMillis());
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    dataset.setWebPath(sdf.format(created));

    // Verify the file content and enhance the dataset record
    if (!DatasetFileCommand.isValidDatasetFile(dataset, type)) {
      context.setErrorMessage("The file could not be validated");
      return false;
    }

    try {
      // Get a handle on the previous file if there is one
      Dataset previousDataset = DatasetRepository.findById(dataset.getId());
      // Update the dataset repository
      Dataset savedDataset = SaveDatasetCommand.saveDataset(dataset);
      if (savedDataset == null) {
        throw new DataException("Your information could not be saved due to a system error. Please try again.");
      }
      // Share the new id with the caller
      dataset.setId(savedDataset.getId());
      // Clean up the previous dataset file
      if (previousDataset != null) {
        DeleteDatasetCommand.deleteFile(previousDataset);
      }
      return true;
    } catch (DataException e) {
      // Clean up the file
      SaveFilePartCommand.cleanupFile(fileItem);
    }
    return false;
  }

}
