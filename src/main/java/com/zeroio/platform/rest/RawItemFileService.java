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

import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;

/**
 * Restores missing item-files on the file server from a posted multipart file and web path.
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class RawItemFileService extends GenericRestService {

  private static Log log = LogFactory.getLog(RawItemFileService.class);
  private static final String ERROR_TITLE_KEY = "title";
  private static final Semaphore REQUEST_LOCK = new Semaphore(1);

  /**
   * GET /rawItemFile/{webPath} 
   * Streams the item file data 
   *  
   */
  @Override
  public ServiceResponse get(ServiceContext context) {
    // A webPath is required to locate the file
    String webPath = StringUtils.trimToNull(context.getPathParam());
    if (webPath == null || !isSafeWebPath(webPath)) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put(ERROR_TITLE_KEY, "A valid webPath is required");
      return response;
    }
    ItemFileItem itemFile = ItemFileItemRepository.findByWebPath(webPath);
    if (itemFile == null) {
      ServiceResponse response = new ServiceResponse(404);
      response.getError().put(ERROR_TITLE_KEY, "File not found");
      return response;
    }

    // Check permissions of the Item record
    Item item = LoadItemCommand.loadItemById(itemFile.getItemId());
    if (item != null) {
      item = LoadItemCommand.loadItemForAuthorizedUser(item, context.getUser());
    }
    if (item == null) {
      ServiceResponse response = new ServiceResponse(404);
      response.getError().put(ERROR_TITLE_KEY, "Item was not found");
      return response;
    }

    // Resolve the target path and check if the file exists
    Path targetPath = resolveTargetPath(itemFile);
    if (targetPath == null) {
      ServiceResponse response = new ServiceResponse(400);
      response.getError().put(ERROR_TITLE_KEY, "File not found");
      return response;
    }
    File existingFile = targetPath.toFile();
    if (!existingFile.isFile()) {
      ServiceResponse response = new ServiceResponse(404);
      response.getError().put(ERROR_TITLE_KEY, "File not found");
      return response;
    }

    // Check for a last-modified header and return 304 if possible
    long lastModified = itemFile.getModified().getTime();
    long headerValue = context.getRequest().getDateHeader("If-Modified-Since");
    if (lastModified <= headerValue + 1000) {
      ServiceResponse response = new ServiceResponse(HttpServletResponse.SC_NOT_MODIFIED);
      response.setHandledResponse(true);
      context.getResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return response;
    }

    // Set response headers
    context.getResponse().setDateHeader("Last-Modified", lastModified);
    context.getResponse().setContentType(itemFile.getMimeType());
    context.getResponse().setContentLength((int) existingFile.length());

    // Stream the binary data
    try (FileInputStream in = new FileInputStream(existingFile);
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

  /**
   * POST /rawItemFile
   * Restores missing item-files on the file server from a posted multipart file and web path.
   */
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

      ItemFileItem itemFile = ItemFileItemRepository.findByWebPath(webPath);
      if (itemFile == null) {
        ServiceResponse response = new ServiceResponse(404);
        response.getError().put(ERROR_TITLE_KEY, "Item file metadata was not found");
        return response;
      }

      Path targetPath = resolveTargetPath(itemFile);
      if (targetPath == null) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put(ERROR_TITLE_KEY, "Item file metadata contains an invalid server path");
        return response;
      }

      File existingFile = targetPath.toFile();
      if (existingFile.isFile()) {
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

      if (itemFile.getFileLength() < 0) {
        ServiceResponse response = new ServiceResponse(400);
        response.getError().put(ERROR_TITLE_KEY, "Item file metadata has an invalid length");
        return response;
      }

      if (filePart.getSize() != itemFile.getFileLength()) {
        ServiceResponse response = new ServiceResponse(422);
        response.getError().put(ERROR_TITLE_KEY, "Uploaded item file length does not match expected length");
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
      log.error("Failed to restore item file", e);
      ServiceResponse response = new ServiceResponse(500);
      response.getError().put(ERROR_TITLE_KEY, "There was an issue restoring the item file");
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

  private static Path resolveTargetPath(ItemFileItem itemFile) {
    String serverRootPath = FileSystemCommand.getFileServerRootPathValue();
    if (StringUtils.isBlank(serverRootPath) || StringUtils.isBlank(itemFile.getFileServerPath())) {
      return null;
    }

    Path rootPath = Path.of(serverRootPath).normalize();
    Path itemFilePath = Path.of(itemFile.getFileServerPath());
    Path resolvedPath = rootPath.resolve(itemFilePath).normalize();

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
