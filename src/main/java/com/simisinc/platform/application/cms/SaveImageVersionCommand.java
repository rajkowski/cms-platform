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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;

/**
 * Validates and saves image version objects
 *
 * @author matt rajkowski
 * @created 2/3/26 10:00 PM
 */
public class SaveImageVersionCommand {

  private static Log LOG = LogFactory.getLog(SaveImageVersionCommand.class);

  /**
   * Adds a new version to an existing image and marks it as the current version
   *
   * @param imageId the image to add a version to
   * @param versionBean the version data to save
   * @return the saved ImageVersion
   * @throws DataException if validation fails
   */
  public static ImageVersion addNewVersion(long imageId, ImageVersion versionBean) throws DataException {

    // Validate the required fields
    if (imageId == -1) {
      throw new DataException("An image id is required");
    }
    if (StringUtils.isBlank(versionBean.getFilename())) {
      throw new DataException("A file name is required");
    }
    if (StringUtils.isBlank(versionBean.getFileServerPath())) {
      LOG.error("The developer needs to set a path");
      throw new DataException("A system path error occurred");
    }
    if (versionBean.getCreatedBy() == -1) {
      throw new DataException("The user creating this record was not set");
    }

    // Verify the image exists
    Image image = ImageRepository.findById(imageId);
    if (image == null) {
      throw new DataException("The image could not be found");
    }

    // Get the current version number
    List<ImageVersion> existingVersions = ImageVersionRepository.findAllByImageId(imageId);
    int nextVersionNumber = 1;
    if (existingVersions != null && !existingVersions.isEmpty()) {
      nextVersionNumber = existingVersions.get(0).getVersionNumber() + 1;
    }

    // Mark all existing versions as not current
    ImageVersionRepository.markAsNotCurrent(imageId);

    // Create the new version
    ImageVersion version = new ImageVersion();
    version.setImageId(imageId);
    version.setVersionNumber(nextVersionNumber);
    version.setFilename(versionBean.getFilename());
    version.setFileServerPath(versionBean.getFileServerPath());
    version.setFileLength(versionBean.getFileLength());
    version.setFileType(versionBean.getFileType());
    version.setWidth(versionBean.getWidth());
    version.setHeight(versionBean.getHeight());
    version.setIsCurrent(true);
    version.setCreatedBy(versionBean.getCreatedBy());
    version.setNotes(versionBean.getNotes());

    // Save the version
    version = ImageVersionRepository.save(version);
    if (version == null) {
      throw new DataException("The version could not be saved");
    }

    // Update the image record with the new current version metadata
    image.setFilename(version.getFilename());
    image.setFileServerPath(version.getFileServerPath());
    image.setFileLength(version.getFileLength());
    image.setFileType(version.getFileType());
    image.setWidth(version.getWidth());
    image.setHeight(version.getHeight());
    image.setVersionNumber(version.getVersionNumber());
    image.setModifiedBy(versionBean.getCreatedBy());
    ImageRepository.save(image);

    return version;
  }
}
