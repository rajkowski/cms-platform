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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Timestamp;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;

/**
 * Generates thumbnails for images with optimal quality compression
 * Thumbnails are maximum 240x240 pixels while maintaining aspect ratio
 *
 * @author matt rajkowski
 * @created 1/31/26 4:30 PM
 */
public class GenerateThumbnailCommand {

  private static Log LOG = LogFactory.getLog(GenerateThumbnailCommand.class);

  private static final int MAX_THUMBNAIL_SIZE = 240;
  private static final float THUMBNAIL_QUALITY = 0.85f; // 85% quality for good compression

  /**
   * Generate a thumbnail for the given image
   *
   * @param image The image to generate a thumbnail for
   * @return The updated image with thumbnail information
   * @throws DataException if thumbnail generation fails
   */
  public static Image generateThumbnail(Image image) throws DataException {
    if (image == null || image.getId() <= 0) {
      throw new DataException("Invalid image");
    }

    // Check if original dimensions are smaller than thumbnail size
    if (image.getWidth() <= MAX_THUMBNAIL_SIZE && image.getHeight() <= MAX_THUMBNAIL_SIZE) {
      LOG.debug("Image is already smaller than thumbnail size, no need to create thumbnail");
      return image;
    }

    try {
      // Determine the web path names
      String pathToFile = image.getFileServerPath();
      // Input path: images/2026/01/31/1769895281482-f983693b-a31f-4f24-8e4c-d841c7e4277c-1.png
      // Output path: images/2026/01/31/1769895281482-f983693b-a31f-4f24-8e4c-d841c7e4277c-1-thumb.png
      String thumbnailPathToFile = pathToFile.substring(0, pathToFile.lastIndexOf(".")) + "-thumb" +
          pathToFile.substring(pathToFile.lastIndexOf("."));

      // Find the original image
      File originalFile = FileSystemCommand.getFileServerRootPath(pathToFile);
      if (!originalFile.exists()) {
        throw new DataException("Original image file not found for id: " + image.getId());
      }

      // Prepare the thumbnail file
      File thumbnailFile = FileSystemCommand.getFileServerRootPath(thumbnailPathToFile);

      // Choose the file type
      String fileType = image.getFileType().toLowerCase();
      if (fileType.contains("/")) {
        fileType = fileType.substring(fileType.indexOf("/") + 1);
      }

      // Read the original image
      BufferedImage originalImage = ImageIO.read(originalFile);
      if (originalImage == null) {
        throw new DataException("Could not read image file");
      }

      // Calculate new dimensions maintaining aspect ratio
      int originalWidth = originalImage.getWidth();
      int originalHeight = originalImage.getHeight();
      int thumbnailWidth;
      int thumbnailHeight;

      if (originalWidth > originalHeight) {
        thumbnailWidth = MAX_THUMBNAIL_SIZE;
        thumbnailHeight = (int) Math.round((double) originalHeight * MAX_THUMBNAIL_SIZE / originalWidth);
      } else {
        thumbnailHeight = MAX_THUMBNAIL_SIZE;
        thumbnailWidth = (int) Math.round((double) originalWidth * MAX_THUMBNAIL_SIZE / originalHeight);
      }

      LOG.debug("Creating thumbnail: " + thumbnailWidth + "x" + thumbnailHeight + " from " + originalWidth + "x" + originalHeight);

      // Prepare the target thumbnail image
      BufferedImage thumbnailImage = null;
      if ("png".equalsIgnoreCase(fileType) && originalImage.getColorModel().hasAlpha()) {
        // Preserve transparency for PNG images
        thumbnailImage = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_ARGB);
      } else {
        // Use RGB for other image types
        thumbnailImage = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
      }

      // Use high quality rendering hints and draw the scaled image
      Graphics2D g2d = thumbnailImage.createGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.drawImage(originalImage, 0, 0, thumbnailWidth, thumbnailHeight, null);
      g2d.dispose();

      // Write the thumbnail image to file with appropriate compression
      try {
        if ("jpg".equals(fileType) || "jpeg".equals(fileType)) {
          // Use JPEG writer with quality control
          ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
          ImageWriteParam writeParam = writer.getDefaultWriteParam();
          writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
          writeParam.setCompressionQuality(THUMBNAIL_QUALITY);

          try (ImageOutputStream ios = ImageIO.createImageOutputStream(thumbnailFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(thumbnailImage, null, null), writeParam);
          }
          writer.dispose();
        } else {
          // Use default writers for PNG and other formats
          boolean written = ImageIO.write(thumbnailImage, fileType, thumbnailFile);
          if (!written) {
            throw new DataException("No appropriate writer found for image type: " + fileType);
          }
        }
      } catch (Exception e) {
        // Delete any partially created thumbnail file
        if (thumbnailFile.exists()) {
          thumbnailFile.delete();
        }
        throw new DataException("Error writing thumbnail file: " + e.getMessage());
      }

      // Update image record with thumbnail information
      image.setProcessedPath(thumbnailPathToFile);
      image.setProcessedFileLength(thumbnailFile.length());
      image.setProcessedFileType(image.getFileType());
      image.setProcessedWidth(thumbnailWidth);
      image.setProcessedHeight(thumbnailHeight);
      image.setProcessed(new Timestamp(System.currentTimeMillis()));
      image.setModifiedBy(-1); // System operation

      // Save to database
      Image savedImage = ImageRepository.save(image);
      if (savedImage == null) {
        throw new DataException("Failed to save thumbnail information to database");
      }

      LOG.info("Thumbnail generated successfully: " + thumbnailFile.getAbsolutePath() + " (" + thumbnailFile.length() + " bytes)");
      return savedImage;

    } catch (Exception e) {
      LOG.error("Error generating thumbnail for image " + image.getId(), e);
      throw new DataException("Failed to generate thumbnail: " + e.getMessage());
    }
  }

  /**
   * Delete the thumbnail file for the given image
   *
   * @param image The image whose thumbnail should be deleted
   */
  public static void deleteThumbnail(Image image) {
    if (image == null || image.getProcessedPath() == null) {
      return;
    }

    try {
      File thumbnailFile = FileSystemCommand.getFileServerRootPath(image.getProcessedPath());
      if (thumbnailFile.exists()) {
        if (thumbnailFile.delete()) {
          LOG.debug("Deleted thumbnail file: " + image.getProcessedPath());
        } else {
          LOG.warn("Failed to delete thumbnail file: " + image.getProcessedPath());
        }
      }
    } catch (Exception e) {
      LOG.error("Error deleting thumbnail for image " + image.getId(), e);
    }
  }
}
