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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;
import com.zeroio.platform.application.cms.IntegrationAttachmentCommand;

/**
 * Restores missing files on the file server from a posted multipart file and web path.
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class RawFileService extends GenericRestService {

  private static Log log = LogFactory.getLog(RawFileService.class);
  private static final String ERROR_TITLE_KEY = "title";

  // Global single-flight lock for this endpoint.
  private static final Semaphore REQUEST_LOCK = new Semaphore(1);

  // POST /rawFile
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

      FileItem fileItem = loadFileItem(webPath);
      if (fileItem == null) {
        ServiceResponse response = new ServiceResponse(404);
        response.getError().put(ERROR_TITLE_KEY, "File metadata was not found");
        return response;
      }

      Path targetPath = resolveTargetPath(fileItem);
      if (targetPath == null) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put(ERROR_TITLE_KEY, "File metadata contains an invalid server path");
        return response;
      }

      File existingFile = targetPath.toFile();
      if (existingFile.isFile() && existingFile.length() == fileItem.getFileLength()) {
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

      if (fileItem.getFileLength() < 0) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put(ERROR_TITLE_KEY, "File metadata has an invalid length");
        return response;
      }

      if (filePart.getSize() != fileItem.getFileLength()) {
        ServiceResponse response = new ServiceResponse(422);
        response.getError().put(ERROR_TITLE_KEY, "Uploaded file length does not match expected length");
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
      log.error("Failed to restore file", e);
      ServiceResponse response = new ServiceResponse(500);
      response.getError().put(ERROR_TITLE_KEY, "There was an issue restoring the file");
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

  private static FileItem loadFileItem(String webPath) {
    if (webPath.startsWith(IntegrationAttachmentCommand.INTEGRATION_PREFIX)) {
      return IntegrationAttachmentCommand.loadFileItem(webPath);
    }
    return FileItemRepository.findByWebPath(webPath);
  }

  private static Path resolveTargetPath(FileItem fileItem) {
    String serverRootPath = FileSystemCommand.getFileServerRootPathValue();
    if (StringUtils.isBlank(serverRootPath) || StringUtils.isBlank(fileItem.getFileServerPath())) {
      return null;
    }

    Path rootPath = Path.of(serverRootPath).normalize();
    Path filePath = Path.of(fileItem.getFileServerPath());
    Path resolvedPath = rootPath.resolve(filePath).normalize();

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
