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
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.rest.controller.GenericRestService;
import com.simisinc.platform.rest.controller.ServiceContext;
import com.simisinc.platform.rest.controller.ServiceResponse;

/**
 * Uploads model files into a named folder on the configured model path.
 *
 * @author matt rajkowski
 * @created 11/18/26 2:45 PM
 */
public class RawModelFileService extends GenericRestService {

  private static final Log LOG = LogFactory.getLog(RawModelFileService.class);
  private static final String ERROR_TITLE_KEY = "title";
  private static final Pattern FOLDER_PATTERN = Pattern.compile("[a-z0-9]+");
  private static final Set<String> ALLOWED_FILENAMES = Set.of("model.onnx", "tokenizer.json");
  private static final Semaphore REQUEST_LOCK = new Semaphore(1);

  // POST /rawModelFile
  @Override
  public ServiceResponse post(ServiceContext context) {
    if (!context.hasRole("admin")) {
      return error(403, "Not authorized");
    }

    if (!REQUEST_LOCK.tryAcquire()) {
      return error(429, "Request is throttled");
    }

    try {
      String folder = StringUtils.trimToNull(context.getParameter("folder"));
      if (folder == null || !FOLDER_PATTERN.matcher(folder).matches()) {
        return error(400, "A valid folder is required using only lowercase letters and numbers");
      }

      HttpServletRequest request = context.getRequest();
      Part filePart = request == null ? null : request.getPart("file");
      if (filePart == null || StringUtils.isBlank(filePart.getSubmittedFileName())) {
        return error(400, "Multipart file part 'file' is required");
      }

      String filename = filePart.getSubmittedFileName();
      if (!ALLOWED_FILENAMES.contains(filename)) {
        return error(400, "Only model.onnx and tokenizer.json can be uploaded");
      }
      if (filePart.getSize() <= 0) {
        return error(400, "The uploaded file is empty");
      }

      // Get the model folder and resolve the target path for the uploaded file within it
      File modelFolder = FileSystemCommand.getFileServerModelsPath(folder);
      Path targetPath = resolveTargetPath(modelFolder, filename);
      if (targetPath == null) {
        return error(400, "The model folder is invalid");
      }
      if (Files.isSymbolicLink(targetPath)) {
        return error(400, "The model file path is invalid");
      }

      // Copy the uploaded file to the resolved target path
      try (InputStream inputStream = filePart.getInputStream()) {
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
      }

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("status", "ok");
      payload.put("folder", folder);
      payload.put("filename", filename);
      ServiceResponse response = new ServiceResponse(200);
      response.setData(payload);
      return response;
    } catch (Exception e) {
      LOG.error("Failed to upload model file", e);
      return error(500, "There was an issue uploading the model file");
    } finally {
      REQUEST_LOCK.release();
    }
  }

  static boolean isValidFolder(String folder) {
    return StringUtils.isNotBlank(folder) && FOLDER_PATTERN.matcher(folder).matches();
  }

  private static Path resolveTargetPath(File modelFolder, String filename) throws Exception {
    // Resolve the target path within the model folder
    Path modelPath = modelFolder.toPath().toAbsolutePath().normalize();
    Files.createDirectories(modelPath);
    Path realFolderPath = modelPath.toRealPath();

    // Resolve the target path for the filename within the real folder path
    Path targetPath = realFolderPath.resolve(filename).normalize();
    if (!targetPath.getParent().equals(realFolderPath)) {
      return null;
    }
    return targetPath;
  }

  private static ServiceResponse error(int status, String title) {
    ServiceResponse response = new ServiceResponse(status);
    response.getError().put(ERROR_TITLE_KEY, title);
    return response;
  }
}
