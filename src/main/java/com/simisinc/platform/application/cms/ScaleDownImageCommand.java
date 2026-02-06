/*
 * Copyright 2026 Matt Rajkowski (https://www.github.com/rajkowski)
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

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;

/**
 * Scales down images proportionally while maintaining aspect ratio
 * Only allows scaling down (percentage less than 100%)
 *
 * @author matt rajkowski
 * @created 2/4/26 11:30 PM
 */
public class ScaleDownImageCommand {

  private static Log LOG = LogFactory.getLog(ScaleDownImageCommand.class);

  private static final float SCALED_IMAGE_QUALITY = 0.90f; // 90% quality for good quality/size tradeoff

  /**
   * Scale down the given image to the specified percentage
   * Saves the previous image as an ImageVersion before scaling
   *
   * @param image The image to scale down
   * @param scalePercentage The scale percentage (10-99)
   * @return The updated image with scaled dimensions
   * @throws DataException if scaling fails
   */
  public static Image scaleDownImage(Image image, int scalePercentage) throws DataException {
    if (image == null || image.getId() <= 0) {
      throw new DataException("Invalid image");
    }

    if (scalePercentage >= 100 || scalePercentage < 10) {
      throw new DataException("Scale percentage must be between 10 and 99");
    }

    try {
      // Determine the web path names
      String pathToFile = image.getFileServerPath();

      // Find the original image
      File originalFile = FileSystemCommand.getFileServerRootPath(pathToFile);
      if (!originalFile.exists()) {
        throw new DataException("Original image file not found for id: " + image.getId());
      }

      // Choose the file type
      String fileType = ImageScalingUtility.extractFileType(image.getFileType());

      // Read the original image
      BufferedImage originalImage = ImageIO.read(originalFile);
      if (originalImage == null) {
        throw new DataException("Could not read image file");
      }

      // Calculate new dimensions maintaining aspect ratio
      int originalWidth = originalImage.getWidth();
      int originalHeight = originalImage.getHeight();
      int scaledWidth = Math.round(originalWidth * scalePercentage / 100);
      int scaledHeight = Math.round(originalHeight * scalePercentage / 100);

      LOG.debug("Scaling image from " + originalWidth + "x" + originalHeight + " to " + scaledWidth + "x" + scaledHeight);

      // Generate a new unique filename for the scaled image
      String serverSubPath = FileSystemCommand.generateFileServerSubPath("images");
      String serverRootPath = FileSystemCommand.getFileServerRootPathValue();
      String serverCompletePath = serverRootPath + serverSubPath;

      String uniqueFilename = FileSystemCommand.generateUniqueFilename(image.getModifiedBy());
      String newFileServerPath = serverSubPath + uniqueFilename + "." + fileType;
      File scaledImageFile = new File(serverCompletePath + uniqueFilename + "." + fileType);

      // Use shared utility to scale and write the image
      ImageScalingUtility.scaleAndWriteImage(
          originalImage,
          scaledWidth,
          scaledHeight,
          fileType,
          SCALED_IMAGE_QUALITY,
          scaledImageFile,
          RenderingHints.VALUE_INTERPOLATION_BICUBIC);

      // Create a new version with the scaled image data
      ImageVersion versionBean = new ImageVersion();
      versionBean.setFilename(image.getFilename());
      versionBean.setFileServerPath(newFileServerPath);
      versionBean.setFileLength(scaledImageFile.length());
      versionBean.setFileType(image.getFileType());
      versionBean.setWidth(scaledWidth);
      versionBean.setHeight(scaledHeight);
      versionBean.setCreatedBy(image.getModifiedBy() > 0 ? image.getModifiedBy() : -1); // System operation if not set
      versionBean.setNotes("Auto-scaled down to " + scalePercentage + "% of original size");

      // Save this as a new version and update the image record as current
      ImageVersion savedVersion = SaveImageVersionCommand.addNewVersion(image.getId(), versionBean);
      if (savedVersion == null) {
        throw new DataException("Failed to save scaled image version to database");
      }

      // Retrieve the updated image record
      Image savedImage = ImageRepository.findById(image.getId());
      if (savedImage == null) {
        throw new DataException("Failed to retrieve updated image information from database");
      }

      LOG.info("Image scaled down successfully: " + originalFile.getAbsolutePath() + " (" + originalFile.length() + " bytes)");
      return savedImage;

    } catch (Exception e) {
      LOG.error("Error scaling down image " + image.getId(), e);
      throw new DataException("Failed to scale down image: " + e.getMessage());
    }
  }
}
