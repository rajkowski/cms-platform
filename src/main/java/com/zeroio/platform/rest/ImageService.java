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
package com.zeroio.platform.rest;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.Part;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.SaveFilePartCommand;
import com.simisinc.platform.application.cms.SaveImageCommand;
import com.simisinc.platform.application.cms.ValidateImageCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;
import com.zeroio.platform.presentation.controller.FileModulesConstants;

/**
 * Accepts image uploads, stores image metadata, and writes files to the file system.
 * 
 * @created 7/24/26 8:00 AM
 * @author matt rajkowski
 */
public class ImageService extends GenericRestService {

  private static Log LOG = LogFactory.getLog(ImageService.class);

  // POST /image
  @Override
  public ServiceResponse post(ServiceContext context) {

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      ServiceResponse response = new ServiceResponse(403);
      response.getError().put("title", "Not authorized");
      return response;
    }

    FileItem fileItem = null;
    try {
      Part filePart = context.getRequest().getPart("file");
      fileItem = SaveFilePartCommand.saveFile(FileModulesConstants.IMAGES, filePart, context.getUserId());

      Image imageBean = new Image();
      imageBean.setFilename(fileItem.getFilename());
      imageBean.setFileLength(fileItem.getFileLength());
      imageBean.setFileServerPath(fileItem.getFileServerPath());
      imageBean.setCreatedBy(context.getUserId());
      imageBean.setModifiedBy(context.getUserId());

      ValidateImageCommand.checkFile(imageBean);
      Image image = SaveImageCommand.saveImage(imageBean);
      if (image == null) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put("title", "The image could not be saved");
        return response;
      }

      Map<String, Object> data = new LinkedHashMap<>();
      data.put("id", image.getId());
      data.put("filename", image.getFilename());
      data.put("location", "/assets/img/" + image.getUrl());
      data.put("size", image.getFileLength());
      data.put("contentType", image.getFileType());
      data.put("width", image.getWidth());
      data.put("height", image.getHeight());

      ServiceResponse response = new ServiceResponse(200);
      response.getMeta().put("type", "image");
      response.getMeta().put("id", image.getId());
      response.setData(data);
      return response;

    } catch (DataException e) {
      SaveFilePartCommand.cleanupFile(fileItem);
      LOG.warn("imageSaveError", e);
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put("title", e.getMessage());
      return response;
    } catch (Exception e) {
      SaveFilePartCommand.cleanupFile(fileItem);
      LOG.error("imageUploadError", e);
      ServiceResponse response = new ServiceResponse(500);
      response.getError().put("title", "An unexpected error occurred while uploading the image");
      return response;
    }
  }
}
