/*
 * Copyright 2026 Matt Rajkowski
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
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;

/**
 * Deletes images
 *
 * @author matt rajkowski
 * @created 2/4/26 12:55 PM
 */
public class DeleteImageCommand {

  private static Log LOG = LogFactory.getLog(DeleteImageCommand.class);

  public static boolean deleteImage(Image imageBean) throws DataException {

    // Verify the object
    if (imageBean == null || imageBean.getId() == -1) {
      throw new DataException("The image was not specified");
    }

    // Determine the files to delete
    List<ImageVersion> imageVersionList = ImageVersionRepository.findAllByImageId(imageBean.getId());

    LOG.debug("Version count: " + (imageVersionList != null ? imageVersionList.size() : 0));

    // Remove the image from the database
    if (ImageRepository.remove(imageBean)) {
      // Delete all the files/versions
      deleteImageVersionFiles(imageVersionList);
      return true;
    }
    return false;
  }

  private static void deleteImageVersionFiles(List<ImageVersion> imageVersionList) {
    if (imageVersionList == null) {
      return;
    }
    for (ImageVersion imageVersion : imageVersionList) {
      deleteImageVersionFile(imageVersion);
    }
  }

  private static void deleteImageVersionFile(ImageVersion imageVersion) {
    String fileServerPath = imageVersion.getFileServerPath();
    if (StringUtils.isBlank(fileServerPath)) {
      return;
    }
    File file = FileSystemCommand.getFileServerRootPath(fileServerPath);
    if (file.exists() && file.isFile()) {
      try {
        Files.delete(file.toPath());
      } catch (Exception e) {
        LOG.warn("Could not delete file: " + fileServerPath, e);
      }
    }
  }

}
