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

package com.simisinc.platform.application.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.zeroio.platform.domain.model.tenant.WorkspaceContext;
import com.zeroio.platform.infrastructure.database.WorkspaceContextManager;

/**
 * Commands for working with the file system
 *
 * @author matt rajkowski
 * @created 4/25/18 9:56 AM
 */
public class FileSystemCommand {

  private static Log LOG = LogFactory.getLog(FileSystemCommand.class);
  private static final String CMS_PATH = "CMS_PATH";
  private static String configPath = null;
  private static String filesPath = null;
  private static String staticSitePath = null;
  private static String modelsPath = null;

  public static File getCMSPath() {
    // Check for environment variable first
    String envPath = System.getenv(CMS_PATH);
    if (StringUtils.isNotBlank(envPath)) {
      return new File(envPath);
    }
    // Determine the base path based on the operating system
    File basePath = null;
    if (SystemUtils.IS_OS_LINUX) {
      basePath = new File("/opt/cms-platform");
    } else {
      File userHome = SystemUtils.getUserHome();
      File webPath = new File(userHome, "Web");
      File cmsPath = new File(webPath, "cms-platform");
      basePath = cmsPath;
    }
    return basePath;
  }

  /** Return the serverConfigPath and add any specified subdirectories. */
  public static File getFileServerConfigPath(String... subdirectories) {
    return resolveFileServerPath(getFileServerConfigPathValue(), subdirectories);
  }

  /**
   * Return the static config path based on system environment and database
   * settings
   * 
   * @return
   */
  protected static String getFileServerConfigPathValue() {
    if (WorkspaceContextManager.getCurrentContext() != null) {
      return getFileServerPathValue("config", "system.configpath", configPath, false);
    }
    configPath = getFileServerPathValue("config", "system.configpath", configPath, false);
    return configPath;
  }

  /** Return the serverRootPath and add any specified subdirectories. */
  public static File getFileServerRootPath(String... subdirectories) {
    String serverRootPath = getFileServerRootPathValue();
    if (StringUtils.isBlank(serverRootPath)) {
      return null;
    }
    return resolveFileServerPath(serverRootPath, subdirectories);
  }

  /**
   * File storage path
   * 
   * @return
   */
  public static String getFileServerRootPathValue() {
    if (WorkspaceContextManager.getCurrentContext() != null) {
      return getFileServerPathValue("files", "system.filepath", filesPath, true);
    }
    filesPath = getFileServerPathValue("files", "system.filepath", filesPath, true);
    return filesPath;
  }

  private static String getFileServerPathValue(String directoryName, String propertyName, String cachedPath,
      boolean requireDirectory) {
    WorkspaceContext workspaceContext = WorkspaceContextManager.getCurrentContext();
    if (workspaceContext != null) {
      return getWorkspacePathValue(workspaceContext, directoryName, requireDirectory);
    }
    if (cachedPath != null) {
      return cachedPath;
    }
    String configuredPath = null;
    if (System.getenv().containsKey(CMS_PATH)) {
      configuredPath = new File(System.getenv(CMS_PATH), directoryName).getPath();
    }
    if (StringUtils.isBlank(configuredPath)) {
      configuredPath = LoadSitePropertyCommand.loadByName(propertyName);
    }
    if (configuredPath == null || StringUtils.isBlank(configuredPath)) {
      LOG.error("CMS_PATH environment variable can be set, or add '" + propertyName + "' to the database");
      return null;
    }
    if (!configuredPath.endsWith(File.separator)) {
      configuredPath += File.separator;
    }
    return configuredPath;
  }

  private static String getWorkspacePathValue(WorkspaceContext workspaceContext, String directoryName,
      boolean requireDirectory) {
    if (StringUtils.isBlank(workspaceContext.fileRoot())) {
      LOG.error("Workspace " + workspaceContext.workspaceId() + " has no configured file root");
      return null;
    }
    try {
      Path rootPath = Paths.get(workspaceContext.fileRoot()).toAbsolutePath().normalize().resolve(directoryName)
          .normalize();
      if (requireDirectory && !Files.isDirectory(rootPath)) {
        LOG.error("Workspace " + workspaceContext.workspaceId() + " file root is unavailable for: " + rootPath);
        return null;
      }
      return rootPath + File.separator;
    } catch (InvalidPathException e) {
      LOG.error("Workspace " + workspaceContext.workspaceId() + " file root is invalid", e);
      return null;
    }
  }

  private static File resolveFileServerPath(String serverRootPath, String... subdirectories) {
    if (StringUtils.isBlank(serverRootPath)) {
      return null;
    }
    try {
      Path rootPath = Paths.get(serverRootPath).toAbsolutePath().normalize();
      Path resolvedPath = rootPath;
      if (subdirectories != null) {
        for (String subdirectory : subdirectories) {
          resolvedPath = resolvedPath.resolve(subdirectory);
        }
      }
      resolvedPath = resolvedPath.normalize();
      if (!resolvedPath.startsWith(rootPath)) {
        LOG.error("Requested file path escapes the configured file root");
        return null;
      }
      return resolvedPath.toFile();
    } catch (InvalidPathException e) {
      LOG.error("Requested file path is invalid", e);
      return null;
    }
  }

  /**
   * Static site path
   * 
   * @return
   */
  private static String getFileServerStaticSitePathValue() {
    if (WorkspaceContextManager.getCurrentContext() != null) {
      return getFileServerPathValue("static-site", "system.staticsite.filepath", staticSitePath, false);
    }
    staticSitePath = getFileServerPathValue("static-site", "system.staticsite.filepath", staticSitePath, false);
    return staticSitePath;
  }

  /** Return a file reference for the static site path */
  public static File getFileServerStaticSitePath(String... subdirectories) {
    return resolveFileServerPath(getFileServerStaticSitePathValue(), subdirectories);
  }

  public static File getFileServerModelsPath(String... subdirectories) {
    return resolveFileServerPath(getFileServerModelsPathValue(), subdirectories);
  }

  private static String getFileServerModelsPathValue() {
    if (WorkspaceContextManager.getCurrentContext() != null) {
      return getFileServerPathValue("models", "system.models.filepath", modelsPath, false);
    }
    modelsPath = getFileServerPathValue("models", "system.models.filepath", modelsPath, false);
    return modelsPath;
  }

  /**
   * Generator for scalable sub-path values using dates
   * 
   * @param module
   * @return
   */
  public static String generateFileServerSubPath(String module) {
    Date date = new Date();
    LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    int year = localDate.getYear();
    int month = localDate.getMonthValue();
    int day = localDate.getDayOfMonth();
    String monthValue = (month < 10 ? "0" : "") + month;
    String dayValue = (day < 10 ? "0" : "") + day;
    String subPath = module + File.separator + year + File.separator + monthValue + File.separator + dayValue
        + File.separator;

    // Make sure the path exists
    File directory = new File(getFileServerRootPath(), subPath);
    directory.mkdirs();

    return subPath;
  }

  /**
   * Generator for unique filenames so it cannot collide with existing files
   * 
   * @param appendValue
   * @return
   */
  public static String generateUniqueFilename(long appendValue) {
    String filenameUUID = UUID.randomUUID().toString();
    if (appendValue < 0) {
      return System.currentTimeMillis() + "-" + filenameUUID;
    }
    return System.currentTimeMillis() + "-" + filenameUUID + "-" + appendValue;
  }

  /**
   * Reduces a file extension to a safe value (letters and digits only) so it
   * cannot carry path separators or
   * traversal sequences ("/", "\\", "..") into a file path.
   *
   * @param extension the raw extension, typically derived from a user-supplied
   *                  filename
   * @return the extension with everything but ASCII letters and digits removed
   *         (never null)
   */
  public static String cleanExtension(String extension) {
    if (extension == null) {
      return "";
    }
    return extension.replaceAll("[^A-Za-z0-9]", "");
  }

  /**
   * Method to determine a file's checksum
   * 
   * @param file
   * @return
   */
  public static String getFileChecksum(File file) {
    try {
      String method = "SHA-512";
      MessageDigest digest = MessageDigest.getInstance(method);

      try (FileInputStream fis = new FileInputStream(file)) {
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
        while ((bytesCount = fis.read(byteArray)) != -1) {
          digest.update(byteArray, 0, bytesCount);
        }
      }
      // Convert decimal to hexadecimal format
      byte[] bytes = digest.digest();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < bytes.length; i++) {
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
      }
      return method + ";" + sb.toString();
    } catch (Exception e) {
      LOG.error(e);
    }
    return null;
  }

  /**
   * Generator to return a filename for new files
   * 
   * @param folderName
   * @param userId
   * @param extension
   * @return
   */
  public static File generateTempFile(String folderName, long userId, String extension) {
    return generateTempFile(folderName, "", userId, extension);
  }

  public static File generateTempFile(String folderName, String filePrefix, long userId, String extension) {
    String serverRootPath = FileSystemCommand.getFileServerRootPathValue();
    String serverSubPath = FileSystemCommand.generateFileServerSubPath(folderName);
    String serverCompletePath = serverRootPath + serverSubPath;
    String uniqueFilename = (filePrefix != null ? filePrefix : "") + FileSystemCommand.generateUniqueFilename(userId);
    String cleanExtension = FileSystemCommand.cleanExtension(extension);
    if (StringUtils.isNotBlank(cleanExtension)) {
      cleanExtension = "." + cleanExtension;
    }
    return new File(serverCompletePath + uniqueFilename + cleanExtension);
  }

  public static boolean isModified(File file, long previousModifiedValue) {
    if (!file.exists()) {
      return false;
    }
    if (previousModifiedValue == file.lastModified()) {
      return false;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("File is newer: " + file.getAbsolutePath() + " lastModified: " + file.lastModified());
    }
    return true;
  }

  public static List<String> loadFileToList(File file) {
    List<String> list = new ArrayList<>();
    if (!file.exists()) {
      LOG.warn("File not found: " + file.getAbsolutePath());
      return list;
    }
    // Determine CSV settings
    CsvParserSettings parserSettings = new CsvParserSettings();
    parserSettings.setLineSeparatorDetectionEnabled(true);
    if (file.getAbsolutePath().endsWith(".csv")) {
      parserSettings.setHeaderExtractionEnabled(true);
    }
    // Read the file
    CsvParser parser = new CsvParser(parserSettings);
    try (InputStream inputStream = new FileInputStream(file)) {
      parser.beginParsing(inputStream);
      String[] row;
      while ((row = parser.parseNext()) != null) {
        if (row.length == 1) {
          String value = row[0];
          if (StringUtils.isNotBlank(value)) {
            list.add(value);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("File Error: " + e.getMessage());
    } finally {
      parser.stopParsing();
    }
    LOG.info("File loaded: " + file.getAbsolutePath() + " size: " + list.size());
    return list;
  }
}
