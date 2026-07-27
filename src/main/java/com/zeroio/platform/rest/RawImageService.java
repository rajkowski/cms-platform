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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.ImageUrlCommand;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;
import com.zeroio.platform.application.cms.IntegrationAttachmentCommand;

/**
 * Handles image files only.
 * 
 * @created 7/24/26 8:00 AM
 * @author matt rajkowski
 */
public class RawImageService extends GenericRestService {

  private static Log log = LogFactory.getLog(RawImageService.class);
  private static final String ERROR_TITLE_KEY = "title";
  private static final Semaphore REQUEST_LOCK = new Semaphore(1);

  // GET /rawImage/{uri}
  @Override
  public ServiceResponse get(ServiceContext context) {
    // Example: /api/rawImage/assets/img/integration-550401003/image-2023-9-1_12-17-18-1.png

    // Strip the /api/rawImage prefix to recover the image URI
    String requestUri = context.getUri();
    int imagePrefixIdx = requestUri.indexOf("/rawImage/");
    if (imagePrefixIdx < 0) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put(ERROR_TITLE_KEY, "Invalid image URI");
      return response;
    }
    String imageUri = requestUri.substring(imagePrefixIdx + "/rawImage".length());

    log.debug("GET image uri: " + imageUri);

    Image image = ImageUrlCommand.decodeToImageRecord(imageUri);
    if (image == null) {
      image = IntegrationAttachmentCommand.loadImage(imageUri);
      if (image == null) {
        ServiceResponse response = new ServiceResponse(404);
        response.getError().put(ERROR_TITLE_KEY, "Image not found");
        return response;
      }
    }

    File file = FileSystemCommand.getFileServerRootPath(image.getFileServerPath());
    if (!file.isFile()) {
      log.warn("Server file does not exist: " + image.getFileServerPath());
      ServiceResponse response = new ServiceResponse(404);
      response.getError().put(ERROR_TITLE_KEY, "Image file not found on server");
      return response;
    }

    // Check for a last-modified header and return 304 if possible
    long lastModified = image.getModified().getTime();
    long headerValue = context.getRequest().getDateHeader("If-Modified-Since");
    if (lastModified <= headerValue + 1000) {
      ServiceResponse response = new ServiceResponse(HttpServletResponse.SC_NOT_MODIFIED);
      response.setHandledResponse(true);
      context.getResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return response;
    }

    // Set response headers
    context.getResponse().setDateHeader("Last-Modified", lastModified);
    context.getResponse().setContentType(image.getFileType());
    context.getResponse().setContentLength((int) file.length());

    // Stream the binary data
    try (FileInputStream in = new FileInputStream(file);
        OutputStream out = context.getResponse().getOutputStream()) {
      byte[] buf = new byte[8192];
      int count;
      while ((count = in.read(buf)) >= 0) {
        out.write(buf, 0, count);
      }
      out.flush();
    } catch (Exception e) {
      log.debug("Stream error: " + e.getMessage());
    }

    ServiceResponse response = new ServiceResponse(200);
    response.setHandledResponse(true);
    return response;
  }

  // POST /rawImage
  // Restores missing images on the file server from a posted multipart file and web path.
  @Override
  public ServiceResponse post(ServiceContext context) {

    if (!context.hasRole("admin")) {
      ServiceResponse response = new ServiceResponse(403);
      response.getError().put(ERROR_TITLE_KEY, "Not authorized");
      return response;
    }

    if (!REQUEST_LOCK.tryAcquire()) {
      ServiceResponse response = new ServiceResponse(429);
      response.getError().put(ERROR_TITLE_KEY, "Request is throttled");
      return response;
    }

    try {
      String webPath = StringUtils.trimToNull(context.getParameter("webPath"));
      if (webPath == null || !isSafeWebPath(webPath)) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put(ERROR_TITLE_KEY, "A valid webPath is required");
        return response;
      }

      Image image = loadImage(webPath);
      if (image == null) {
        ServiceResponse response = new ServiceResponse(404);
        response.getError().put(ERROR_TITLE_KEY, "Image metadata was not found");
        return response;
      }

      Path targetPath = resolveTargetPath(image);
      if (targetPath == null) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put(ERROR_TITLE_KEY, "Image metadata contains an invalid server path");
        return response;
      }

      File existingFile = targetPath.toFile();
      if (existingFile.isFile() && existingFile.length() == image.getFileLength()) {
        ServiceResponse response = new ServiceResponse(409);
        response.setData(donePayload("already exists"));
        return response;
      }

      Part filePart = context.getRequest().getPart("file");
      if (filePart == null || filePart.getSize() <= 0) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put(ERROR_TITLE_KEY, "Multipart file part 'file' is required");
        return response;
      }

      if (image.getFileLength() < 0) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put(ERROR_TITLE_KEY, "Image metadata has an invalid length");
        return response;
      }

      if (filePart.getSize() != image.getFileLength()) {
        ServiceResponse response = new ServiceResponse(422);
        response.getError().put(ERROR_TITLE_KEY, "Uploaded image length does not match expected length");
        return response;
      }

      Path parentPath = targetPath.getParent();
      if (parentPath != null) {
        Files.createDirectories(parentPath);
      }

      try (InputStream inputStream = filePart.getInputStream()) {
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
      }

      ServiceResponse response = new ServiceResponse(200);
      response.setData(okPayload("ok"));
      return response;

    } catch (Exception e) {
      log.error("Failed to restore image", e);
      ServiceResponse response = new ServiceResponse(500);
      response.getError().put(ERROR_TITLE_KEY, "There was an issue restoring the image");
      return response;
    } finally {
      REQUEST_LOCK.release();
    }
  }

  private static boolean isSafeWebPath(String webPath) {
    return !(StringUtils.isBlank(webPath)
        || webPath.contains("/")
        || webPath.contains("\\")
        || webPath.contains(".."));
  }

  private static Image loadImage(String webPath) {
    String resolvedWebPath = webPath;
    if (webPath.startsWith(IntegrationAttachmentCommand.INTEGRATION_PREFIX)) {
      String extractedWebPath = IntegrationAttachmentCommand.extractWebPath(webPath);
      if (StringUtils.isNotBlank(extractedWebPath)) {
        resolvedWebPath = extractedWebPath;
      }
    }
    return ImageRepository.findByWebPath(resolvedWebPath);
  }

  private static Path resolveTargetPath(Image image) {
    String serverRootPath = FileSystemCommand.getFileServerRootPathValue();
    if (StringUtils.isBlank(serverRootPath) || StringUtils.isBlank(image.getFileServerPath())) {
      return null;
    }

    Path rootPath = Path.of(serverRootPath).normalize();
    Path imagePath = Path.of(image.getFileServerPath());
    Path resolvedPath = rootPath.resolve(imagePath).normalize();

    if (!resolvedPath.startsWith(rootPath)) {
      return null;
    }
    return resolvedPath;
  }

  private static Map<String, Object> donePayload(String message) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", "done");
    payload.put("message", message);
    return payload;
  }

  private static Map<String, Object> okPayload(String message) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", "ok");
    payload.put("message", message);
    return payload;
  }
}
