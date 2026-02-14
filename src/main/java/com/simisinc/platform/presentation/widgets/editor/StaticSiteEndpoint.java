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
package com.simisinc.platform.presentation.widgets.editor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.jobrunr.scheduling.BackgroundJobRequest;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.LoadGitPublishSettingsCommand;
import com.simisinc.platform.application.cms.MakeStaticSiteCommand;
import com.simisinc.platform.application.cms.SaveGitPublishSettingsCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.GitPublishSettings;
import com.simisinc.platform.infrastructure.scheduler.cms.MakeStaticSiteJob;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Controller for managing static site generation
 *
 * @author matt rajkowski
 * @created 1/19/26 10:00 AM
 */
public class StaticSiteEndpoint extends GenericWidget {

  private static final String STATIC_SITE_PATH = "/web/static-sites"; // Path within the container

  /** The default GET, not used */
  public WidgetContext execute(WidgetContext context) {
    LOG.debug("StaticSiteEndpoint Execute...");
    return context;
  }

  /** The action handler which takes an 'action' parameter */
  public WidgetContext action(WidgetContext context) {

    LOG.debug("StaticSiteEndpoint Action...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-editor")) {
      LOG.debug("No permission to: " + StaticSiteEndpoint.class.getSimpleName());
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Determine the action
    try {
      String action = context.getParameter("action");
      if ("DOWNLOAD".equalsIgnoreCase(action)) {
        // The user is downloading a file
        return download(context);
      } else if ("LIST".equalsIgnoreCase(action)) {
        // The user is listing the files
        return list(context);
      } else if ("GET_GIT_SETTINGS".equalsIgnoreCase(action)) {
        // The user is getting Git settings
        return getGitSettings(context);
      }
      // No default action
      LOG.error("Unknown action: " + action);
      return null;
    } catch (Exception e) {
      LOG.error("Error in StaticSiteEndpoint", e);
      return null;
    }
  }

  /** Handles POST requests for static site generation and deletion */
  public WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException {

    LOG.debug("StaticSiteEndpoint POST...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-editor")) {
      LOG.debug("No permission to: " + StaticSiteEndpoint.class.getSimpleName());
      context.setJson("{\"success\":false,\"message\":\"Permission denied\"}");
      context.setSuccess(false);
      return context;
    }

    // Determine the action
    String action = context.getParameter("action");
    try {
      if ("generate".equals(action)) {
        return generate(context);
      } else if ("delete".equals(action)) {
        return delete(context);
      } else if ("saveGitSettings".equals(action)) {
        return saveGitSettings(context);
      }
    } catch (IOException e) {
      LOG.error("Error in StaticSiteEndpoint", e);
    }
    return null;
  }

  /** List the previous static site files */
  private WidgetContext list(WidgetContext context) throws IOException {

    // @todo the generator stores files in the fileLibrary, and records those in a database table
    // Read from there instead of the static site directory

    LOG.debug("Listing static site files...");
    Map<String, Object> result = new HashMap<>();
    List<Map<String, Object>> fileList = new ArrayList<>();

    /*
    File staticSiteDir = new File(STATIC_SITE_PATH);
    
    if (staticSiteDir.exists() && staticSiteDir.isDirectory()) {
      File[] files = staticSiteDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
      if (files != null) {
        for (File file : files) {
          Map<String, Object> fileInfo = new HashMap<>();
          fileInfo.put("name", file.getName());
          fileInfo.put("size", FileUtils.byteCountToDisplaySize(file.length()));
          fileInfo.put("modified", file.lastModified());
          fileList.add(fileInfo);
        }
      }
    }
    */

    result.put("files", fileList);
    result.put("isGenerating", MakeStaticSiteCommand.isJobRunning());

    String json = JsonCommand.createJsonNode(result).toString();
    context.setJson(json);
    context.setSuccess(true);
    return context;
  }

  /** Generate the static site */
  private WidgetContext generate(WidgetContext context) throws IOException {
    if (MakeStaticSiteCommand.isJobRunning()) {
      LOG.debug("A static site generation is already in progress.");
      sendErrorResponse(context.getResponse(), "A static site generation is already in progress.");
    } else {
      LOG.debug("Generating static site...");
      MakeStaticSiteJob staticSiteJob = new MakeStaticSiteJob();
      BackgroundJobRequest.enqueue(staticSiteJob);
      sendSuccessResponse(context.getResponse());
    }
    context.setHandledResponse(true);
    return context;
  }

  public WidgetContext delete(WidgetContext context) {
    LOG.debug("Deleting static site file...");

    // @todo the generator stores files in the fileLibrary, and records those in a database table
    // Read from there instead of the static site directory

    /*
    String fileName = context.getParameter("file");
    if (fileName == null || fileName.isEmpty()) {
      try {
        sendErrorResponse(context.getResponse(), "File name is required.");
      } catch (IOException e) {
        LOG.error("Error sending error response", e);
      }
    } else {
      Path fileToDelete = Paths.get(STATIC_SITE_PATH, fileName);
      try {
        Files.delete(fileToDelete);
        sendSuccessResponse(context.getResponse());
      } catch (IOException e) {
        try {
          sendErrorResponse(context.getResponse(), "Could not delete the file: " + e.getMessage());
        } catch (IOException ex) {
          LOG.error("Error sending error response", ex);
        }
      }
    }
     */
    try {
      sendErrorResponse(context.getResponse(), "Could not delete the file: Not implemented.");
    } catch (IOException e) {
      LOG.error("Error sending error response", e);
    }
    context.setHandledResponse(true);
    return context;
  }

  /** Download a static site file */
  private WidgetContext download(WidgetContext context) throws IOException {

    HttpServletResponse response = context.getResponse();
    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Not implemented.");

    /*
    String fileName = context.getParameter("file");
    if (fileName == null || fileName.isEmpty()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "File name is required.");
    } else {
      File fileToDownload = new File(STATIC_SITE_PATH, fileName);
      if (fileToDownload.exists() && !fileToDownload.isDirectory()) {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentLength((int) fileToDownload.length());
    
        try (OutputStream out = response.getOutputStream()) {
          Files.copy(fileToDownload.toPath(), out);
          out.flush();
        }
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found.");
      }
    }
    */
    context.setHandledResponse(true);
    return context;
  }

  private void sendJsonResponse(HttpServletResponse response, String json) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(json);
  }

  private void sendSuccessResponse(HttpServletResponse response) throws IOException {
    sendJsonResponse(response, "{\"status\":\"success\"}");
  }

  private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
    sendJsonResponse(response, "{\"status\":\"error\", \"message\":\"" + message + "\"}");
  }

  /** Get Git publish settings */
  private WidgetContext getGitSettings(WidgetContext context) throws IOException {
    LOG.debug("Getting Git publish settings...");

    GitPublishSettings settings = LoadGitPublishSettingsCommand.loadSettings();
    Map<String, Object> result = new HashMap<>();

    if (settings != null) {
      result.put("enabled", settings.getEnabled());
      result.put("gitProvider", settings.getGitProvider());
      result.put("repositoryUrl", settings.getRepositoryUrl());
      result.put("branchName", settings.getBranchName());
      result.put("baseBranch", settings.getBaseBranch());
      result.put("username", settings.getUsername());
      result.put("email", settings.getEmail());
      result.put("commitMessageTemplate", settings.getCommitMessageTemplate());
      result.put("autoCreatePr", settings.getAutoCreatePr());
      result.put("prTitleTemplate", settings.getPrTitleTemplate());
      result.put("prDescriptionTemplate", settings.getPrDescriptionTemplate());
      result.put("targetDirectory", settings.getTargetDirectory());
      // Don't send the access token to the client
    } else {
      // Return default values
      result.put("enabled", false);
      result.put("gitProvider", "github");
      result.put("branchName", "main");
      result.put("baseBranch", "main");
      result.put("commitMessageTemplate", "Static site update: ${timestamp}");
      result.put("autoCreatePr", true);
      result.put("prTitleTemplate", "Static site update: ${timestamp}");
      result.put("prDescriptionTemplate", "Automated static site export");
      result.put("targetDirectory", "/");
    }

    String json = JsonCommand.createJsonNode(result).toString();
    context.setJson(json);
    context.setSuccess(true);
    return context;
  }

  /** Save Git publish settings */
  private WidgetContext saveGitSettings(WidgetContext context) throws IOException {
    LOG.debug("Saving Git publish settings...");

    try {
      // Load existing settings or create new
      GitPublishSettings settings = LoadGitPublishSettingsCommand.loadSettings();
      if (settings == null) {
        settings = new GitPublishSettings();
        settings.setCreatedBy(context.getUserId());
      }
      settings.setModifiedBy(context.getUserId());

      // Update from request parameters
      String enabledParam = context.getParameter("enabled");
      settings.setEnabled("true".equals(enabledParam));

      String gitProvider = context.getParameter("gitProvider");
      if (gitProvider != null) {
        settings.setGitProvider(gitProvider);
      }

      String repositoryUrl = context.getParameter("repositoryUrl");
      if (repositoryUrl != null) {
        settings.setRepositoryUrl(repositoryUrl.trim());
      }

      String branchName = context.getParameter("branchName");
      if (branchName != null) {
        settings.setBranchName(branchName.trim());
      }

      String baseBranch = context.getParameter("baseBranch");
      if (baseBranch != null) {
        settings.setBaseBranch(baseBranch.trim());
      }

      String accessToken = context.getParameter("accessToken");
      if (accessToken != null && !accessToken.trim().isEmpty()) {
        settings.setAccessToken(accessToken.trim());
      }

      String username = context.getParameter("username");
      if (username != null) {
        settings.setUsername(username.trim());
      }

      String email = context.getParameter("email");
      if (email != null) {
        settings.setEmail(email.trim());
      }

      String commitMessageTemplate = context.getParameter("commitMessageTemplate");
      if (commitMessageTemplate != null) {
        settings.setCommitMessageTemplate(commitMessageTemplate.trim());
      }

      String autoCreatePr = context.getParameter("autoCreatePr");
      settings.setAutoCreatePr("true".equals(autoCreatePr));

      String prTitleTemplate = context.getParameter("prTitleTemplate");
      if (prTitleTemplate != null) {
        settings.setPrTitleTemplate(prTitleTemplate.trim());
      }

      String prDescriptionTemplate = context.getParameter("prDescriptionTemplate");
      if (prDescriptionTemplate != null) {
        settings.setPrDescriptionTemplate(prDescriptionTemplate.trim());
      }

      String targetDirectory = context.getParameter("targetDirectory");
      if (targetDirectory != null) {
        settings.setTargetDirectory(targetDirectory.trim());
      }

      // Save the settings
      SaveGitPublishSettingsCommand.saveSettings(settings);

      sendSuccessResponse(context.getResponse());
      context.setHandledResponse(true);
      return context;

    } catch (DataException e) {
      LOG.error("Data exception saving Git settings", e);
      sendErrorResponse(context.getResponse(), e.getMessage());
      context.setHandledResponse(true);
      return context;
    } catch (Exception e) {
      LOG.error("Error saving Git settings", e);
      sendErrorResponse(context.getResponse(), "Failed to save settings: " + e.getMessage());
      context.setHandledResponse(true);
      return context;
    }
  }
}