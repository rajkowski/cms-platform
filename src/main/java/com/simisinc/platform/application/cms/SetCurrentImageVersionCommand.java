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

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;

/**
 * Sets a specific version as the current version of an image
 *
 * @author matt rajkowski
 * @created 2/3/26 10:00 PM
 */
public class SetCurrentImageVersionCommand {

  /**
   * Sets a specific version as the current version of an image
   *
   * @param imageId the image to update
   * @param versionId the version to set as current
   * @param userId the user making the change
   * @return the updated Image
   * @throws DataException if validation fails
   */
  public static Image setCurrentVersion(long imageId, long versionId, long userId) throws DataException {

    // Validate the required fields
    if (imageId == -1) {
      throw new DataException("An image id is required");
    }
    if (versionId == -1) {
      throw new DataException("A version id is required");
    }

    // Verify the image exists
    Image image = ImageRepository.findById(imageId);
    if (image == null) {
      throw new DataException("The image could not be found");
    }

    // Verify the version exists and belongs to this image
    ImageVersion version = ImageVersionRepository.findById(versionId);
    if (version == null) {
      throw new DataException("The version could not be found");
    }
    if (version.getImageId() != imageId) {
      throw new DataException("The version does not belong to this image");
    }

    // Mark all existing versions as not current
    ImageVersionRepository.markAsNotCurrent(imageId);

    // Mark this version as current
    version.setIsCurrent(true);
    version = ImageVersionRepository.save(version);
    if (version == null) {
      throw new DataException("The version could not be updated");
    }

    // Update the image record with this version's metadata
    image.setFilename(version.getFilename());
    image.setFileServerPath(version.getFileServerPath());
    image.setFileLength(version.getFileLength());
    image.setFileType(version.getFileType());
    image.setWidth(version.getWidth());
    image.setHeight(version.getHeight());
    image.setVersionNumber(version.getVersionNumber());
    image.setModifiedBy(userId);
    image = ImageRepository.save(image);

    return image;
  }
}
