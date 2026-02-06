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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;

/**
 * Utility class for common image scaling and writing operations
 * Removes duplicate code from multiple image manipulation commands
 *
 * @author matt rajkowski
 * @created 2/5/26 10:30 PM
 */
public class ImageScalingUtility {

  private static Log LOG = LogFactory.getLog(ImageScalingUtility.class);

  /**
   * Scale an image to the specified dimensions and write to output file
   * Handles color model preservation, rendering quality, and compression
   *
   * @param sourceImage The source BufferedImage to scale
   * @param targetWidth The target width in pixels
   * @param targetHeight The target height in pixels
   * @param fileType The file type (jpg, jpeg, png, etc.)
   * @param quality The compression quality for JPEG (0.0f - 1.0f)
   * @param outputFile The file to write the scaled image to
   * @param interpolation The interpolation hint (RenderingHints.VALUE_INTERPOLATION_*)
   * @throws DataException if scaling or writing fails
   */
  public static void scaleAndWriteImage(
      BufferedImage sourceImage,
      int targetWidth,
      int targetHeight,
      String fileType,
      float quality,
      File outputFile,
      Object interpolationHint) throws DataException {

    if (sourceImage == null) {
      throw new DataException("Source image is null");
    }

    if (targetWidth <= 0 || targetHeight <= 0) {
      throw new DataException("Invalid target dimensions: " + targetWidth + "x" + targetHeight);
    }

    try {
      // Prepare the target image with proper color model preservation
      BufferedImage scaledImage = null;
      if ("png".equalsIgnoreCase(fileType) && sourceImage.getColorModel().hasAlpha()) {
        // Preserve transparency for PNG images
        scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
      } else {
        // Use RGB for other image types
        scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
      }

      // Use high quality rendering hints and draw the scaled image
      Graphics2D g2d = scaledImage.createGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
      g2d.dispose();

      // Write the scaled image to file with appropriate compression
      writeImageToFile(scaledImage, fileType, quality, outputFile);

    } catch (DataException e) {
      throw e;
    } catch (Exception e) {
      throw new DataException("Error scaling image: " + e.getMessage());
    }
  }

  /**
   * Write a BufferedImage to file with compression optimization
   *
   * @param image The image to write
   * @param fileType The file type (jpg, jpeg, png, etc.)
   * @param quality The compression quality for JPEG (0.0f - 1.0f)
   * @param outputFile The file to write to
   * @throws DataException if writing fails
   */
  public static void writeImageToFile(
      BufferedImage image,
      String fileType,
      float quality,
      File outputFile) throws DataException {

    try {
      if ("jpg".equals(fileType) || "jpeg".equals(fileType)) {
        // Use JPEG writer with quality control
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(quality);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
          writer.setOutput(ios);
          writer.write(null, new IIOImage(image, null, null), writeParam);
        }
        writer.dispose();
      } else {
        // Use default writers for PNG and other formats
        boolean written = ImageIO.write(image, fileType, outputFile);
        if (!written) {
          throw new DataException("No appropriate writer found for image type: " + fileType);
        }
      }
    } catch (Exception e) {
      // Delete any partially created file
      if (outputFile.exists()) {
        outputFile.delete();
      }
      throw new DataException("Error writing image file: " + e.getMessage());
    }
  }

  /**
   * Extract the clean file type from a mime type
   * Handles mime types like "image/png" => "png" and "image/jpeg" => "jpg"
   *
   * @param fileType The file type or mime type
   * @return The clean file type (lowercase)
   */
  public static String extractFileType(String fileType) {
    if (fileType == null) {
      return "jpg";
    }

    String cleanType = fileType.toLowerCase();
    if (cleanType.contains("/")) {
      cleanType = cleanType.substring(cleanType.indexOf("/") + 1);
    }

    // Normalize jpeg to jpg
    if ("jpeg".equals(cleanType)) {
      return "jpg";
    }

    return cleanType;
  }
}
