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

package com.simisinc.platform.presentation.widgets.cms;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveFilePartCommand;
import com.simisinc.platform.application.cms.SaveImageCommand;
import com.simisinc.platform.application.cms.SaveImageVersionCommand;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.application.cms.ValidateImageCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.cms.IntegrationAttachmentCommand;
import com.zeroio.platform.presentation.controller.FileModulesConstants;

/**
 * Handles image uploads
 *
 * @author matt rajkowski
 * @created 5/3/18 4:00 PM
 */
public class ImageUploadWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;

  private static Log LOG = LogFactory.getLog(ImageUploadWidget.class);

  public WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException {

    LOG.debug("ImageUploadWidget...");
    FileItem fileItem = null;
    try {
      fileItem = SaveFilePartCommand.saveFileToModule(FileModulesConstants.IMAGES, context);
    } catch (Exception e) {
      LOG.warn("An error occurred", e);
      context.setErrorMessage("An error occurred while saving the file: " + e.getMessage());
      return context;
    }

    long imageId = context.getParameterAsLong("imageId", -1);
    String imageWebPath = StringUtils.trimToNull(context.getParameter("imageWebPath"));
    Image versionTargetImage = null;
    if (imageId > -1) {
      versionTargetImage = ImageRepository.findById(imageId);
      if (versionTargetImage == null) {
        SaveFilePartCommand.cleanupFile(fileItem);
        context.setErrorMessage("The selected image could not be found");
        return context;
      }
    } else if (imageWebPath != null) {
      versionTargetImage = ImageRepository.findByWebPath(imageWebPath);
    }

    // Populate the fields
    Image imageBean = new Image();
    imageBean.setFilename(fileItem.getFilename());
    imageBean.setFileLength(fileItem.getFileLength());
    imageBean.setFileServerPath(fileItem.getFileServerPath());
    imageBean.setCreatedBy(context.getUserId());
    imageBean.setModifiedBy(context.getUserId());

    // Save the record
    Image image = null;
    try {
      ValidateImageCommand.checkFile(imageBean);
      if (versionTargetImage != null) {
        ImageVersion versionBean = new ImageVersion();
        versionBean.setFilename(imageBean.getFilename());
        versionBean.setFileServerPath(imageBean.getFileServerPath());
        versionBean.setFileLength(imageBean.getFileLength());
        versionBean.setFileType(imageBean.getFileType());
        versionBean.setWidth(imageBean.getWidth());
        versionBean.setHeight(imageBean.getHeight());
        versionBean.setCreatedBy(context.getUserId());

        ImageVersion version = SaveImageVersionCommand.addNewVersion(versionTargetImage.getId(), versionBean);
        if (version == null) {
          throw new DataException("Your information could not be saved due to a system error. Please try again.");
        }
        image = ImageRepository.findById(versionTargetImage.getId());
      } else {
        image = SaveImageCommand.saveImage(imageBean);
      }
      if (image == null) {
        throw new DataException("Your information could not be saved due to a system error. Please try again.");
      }
    } catch (DataException e) {
      // Clean up the file
      SaveFilePartCommand.cleanupFile(fileItem);
      context.setErrorMessage(e.getMessage());
      context.setRequestObject(imageBean);
      return context;
    }

    // Preserve integration-style reference when the request targeted a webPath-only image.
    String imageLocation = "/assets/img/" + image.getUrl();
    if (imageId == -1 && imageWebPath != null && imageWebPath.startsWith(IntegrationAttachmentCommand.INTEGRATION_PREFIX)) {
      imageLocation = "/assets/img/" + imageWebPath + "/" + UrlCommand.encodeUri(image.getFilename())
          + (image.getVersionNumber() > 1 ? "?v=" + image.getVersionNumber() : "");
    }

    // Return Json with the new image URL
    context.setJson("{\"location\": \"" + imageLocation + "\"}");
    return context;
  }
}
