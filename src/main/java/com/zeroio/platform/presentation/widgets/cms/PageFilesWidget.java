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
package com.zeroio.platform.presentation.widgets.cms;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.UserCommand;
import com.simisinc.platform.application.cms.DeleteFileCommand;
import com.simisinc.platform.application.cms.LoadWebPageCommand;
import com.simisinc.platform.application.cms.SaveFileCommand;
import com.simisinc.platform.application.cms.SaveFilePartCommand;
import com.simisinc.platform.application.cms.SaveFolderCommand;
import com.simisinc.platform.application.cms.ValidateFileCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.items.PrivacyType;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FolderRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.domain.model.cms.PageFile;
import com.zeroio.platform.infrastructure.persistence.cms.PageFileRepository;

/**
 * Displays the files attached to the current web page
 *
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class PageFilesWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908897L;

  protected static final Log LOG = LogFactory.getLog(PageFilesWidget.class);

  private static final Set<String> ALLOWED_UPLOAD_EXTENSIONS = new HashSet<>(Arrays.asList(
      "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "png", "jpg", "jpeg", "gif", "mp4", "mov", "zip"));

  static String JSP = "/cms/page-files-widget.jsp";

  @Override
  public WidgetContext execute(WidgetContext context) {

    // Make sure the page exists (what about externally defined pages? skip for now)
    WebPage webPage = LoadWebPageCommand.loadByLink(context.getRequest().getPagePath());
    if (webPage == null) {
      return null;
    }

    List<PageFile> fileList = PageFileRepository.findByWebPageId(webPage.getId());
    // Don't return null if list is empty - we still want to show the upload button
    if (fileList == null) {
      fileList = new java.util.ArrayList<>();
    }

    // Limit by type, except for admin
    String type = context.getPreference("type");
    if (!context.hasRole("admin") && type != null && !type.isBlank()) {
      Set<String> allowedTypes = Arrays.stream(type.split(","))
          .map(value -> value == null ? "" : value.trim())
          .map(value -> value.startsWith(".") ? value.substring(1) : value)
          .filter(StringUtils::isNotBlank)
          .map(value -> value.toLowerCase(Locale.ROOT))
          .collect(Collectors.toCollection(HashSet::new));
      if (!allowedTypes.isEmpty()) {
        fileList = fileList.stream()
            .filter(pageFile -> {
              String extension = pageFile.getExtension();
              if (extension == null || extension.isBlank()) {
                return false;
              }
              String normalizedExtension = extension.trim().toLowerCase(Locale.ROOT);
              return allowedTypes.contains(normalizedExtension);
            })
            .collect(Collectors.toList());
        // Don't return null even if filtered list is empty - still show upload button
      }
    }

    // Check if we should show the widget when there are no files
    if (fileList.isEmpty()) {

      // If the folder exists, check the user's role to determine if they can upload files
      // if the folder does not exist?
      // if (!context.hasRole("admin") &&
      //     !CheckFolderPermissionCommand.userHasAddPermission(folderId, context.getUserId())) {
      //   context.setJson("{\"success\": false, \"error\": \"Permission denied\"}");
      //   context.setSuccess(false);
      //   return context;
      // }

      // Check user role
      if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
        // Use a preference
        String showWhenEmpty = context.getPreferences().getOrDefault("showWhenEmpty", "false");
        if ("false".equalsIgnoreCase(showWhenEmpty) && fileList.isEmpty()) {
          return null;
        }
      }
    }

    // Determine the view mode
    String viewMode = context.getPreferences().getOrDefault("view", "table");
    context.getRequest().setAttribute("icon", context.getPreference("icon"));
    context.getRequest().setAttribute("title", context.getPreference("title"));
    context.getRequest().setAttribute("fileList", fileList);
    context.getRequest().setAttribute("viewMode", viewMode);
    context.getRequest().setAttribute("type", type);
    context.setJsp(JSP);
    return context;
  }

  @Override
  public WidgetContext post(WidgetContext context) throws InvocationTargetException, IllegalAccessException {
    LOG.debug("PageFilesWidget.post() called");

    String command = context.getParameter("command");
    if ("attach-existing".equalsIgnoreCase(command)) {
      return attachExisting(context);
    }

    // Route remove and delete commands to the delete() method
    if ("remove".equalsIgnoreCase(command) || "delete".equalsIgnoreCase(command)) {
      return delete(context);
    }

    // Check permissions first
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      LOG.warn("User does not have permission to upload files");
      context.setJson("{\"error\": \"Permission denied. You must be an admin or content-manager to upload files.\"}");
      context.setSuccess(false);
      return context;
    }

    // Make sure the page exists (what about externally defined pages? skip for now)
    WebPage webPage = LoadWebPageCommand.loadByLink(context.getRequest().getPagePath());
    if (webPage == null) {
      LOG.warn("WebPage not found for path: " + context.getRequest().getPagePath());
      context.setJson("{\"error\": \"Page not found\"}");
      context.setSuccess(false);
      return context;
    }

    LOG.debug("Uploading file for page: " + webPage.getId());

    // Get or create the folder for page attachments
    long folderId = -1;
    String folderIdStr = context.getPreference("folderId");

    if (folderIdStr != null && !folderIdStr.isBlank()) {
      // Use configured folder
      try {
        folderId = Long.parseLong(folderIdStr.trim());
        LOG.debug("Using configured folderId: " + folderId);
      } catch (NumberFormatException e) {
        LOG.error("Invalid folderId preference: " + folderIdStr);
      }
    }

    if (folderId == -1) {
      // Look for or create default folder
      Folder folder = FolderRepository.findByName("Page Attachments");
      if (folder == null) {
        LOG.info("Creating default 'Page Attachments' folder");
        try {
          // @todo who will have access to a newly created folder?
          Folder newFolder = new Folder();
          newFolder.setName("Page Attachments");
          newFolder.setSummary("Files attached to wiki pages");
          newFolder.setCreatedBy(context.getUserId());
          newFolder.setModifiedBy(context.getUserId());
          newFolder.setGuestPrivacyType(PrivacyType.PUBLIC);
          folder = SaveFolderCommand.saveFolder(newFolder);
          if (folder != null) {
            folderId = folder.getId();
            LOG.info("Created folder with id: " + folderId);
          }
        } catch (DataException e) {
          LOG.error("Failed to create default folder", e);
          context.setJson("{\"error\": \"Failed to create upload folder. Please contact administrator.\"}");
          context.setSuccess(false);
          return context;
        }
      } else {
        folderId = folder.getId();
        LOG.debug("Using existing 'Page Attachments' folder: " + folderId);
      }
    }

    if (folderId == -1) {
      context.setJson("{\"error\": \"Upload configuration error: unable to determine folder\"}");
      context.setSuccess(false);
      return context;
    }

    FileItem fileItemBean = null;
    try {
      LOG.debug("Attempting to save file part...");
      fileItemBean = SaveFilePartCommand.saveFile(context);
      if (fileItemBean == null) {
        LOG.warn("File part was not found in request");
        throw new DataException("A file was not found, please choose a file and try again");
      }
      LOG.debug("File part saved: " + fileItemBean.getFilename());

      String extension = fileItemBean.getExtension() == null ? ""
          : fileItemBean.getExtension().trim().toLowerCase(Locale.ROOT);
      if (!ALLOWED_UPLOAD_EXTENSIONS.contains(extension)) {
        LOG.warn("File extension not allowed: " + extension);
        throw new DataException(
            "This file type is not allowed. Allowed types: pdf, doc, docx, xls, xlsx, ppt, pptx, csv, png, jpg, jpeg, gif, mp4, mov, zip");
      }
      LOG.debug("File extension validated: " + extension);

      fileItemBean.setFolderId(folderId);
      fileItemBean.setCreatedBy(context.getUserId());
      fileItemBean.setModifiedBy(context.getUserId());

      LOG.debug("Validating file...");
      ValidateFileCommand.checkFile(fileItemBean);

      LOG.debug("Saving file to database...");
      FileItem fileItem = SaveFileCommand.saveFile(fileItemBean);
      if (fileItem == null) {
        throw new DataException("Your information could not be saved due to a system error. Please try again.");
      }
      LOG.debug("File saved with ID: " + fileItem.getId());

      // Create the page-file relationship
      LOG.debug("Creating page-file relationship...");
      PageFile pageFile = new PageFile();
      pageFile.setWebPageId(webPage.getId());
      pageFile.setFileId(fileItem.getId());
      pageFile.setCreatedBy(context.getUserId());
      PageFile savedPageFile = PageFileRepository.save(pageFile);
      if (savedPageFile == null) {
        LOG.error(
            "Failed to create page-file relationship for webPageId=" + webPage.getId() + " fileId=" + fileItem.getId());
        throw new DataException("Failed to associate file with page. Please try again.");
      }
      LOG.debug("Page-file relationship created with ID: " + savedPageFile.getId());

      String fileUrl = "/assets/file/" + fileItem.getUrl();
      String viewUrl = "/assets/view/" + fileItem.getUrl();
      LOG.info("File upload successful: " + fileUrl);

      // Return file metadata for dynamic insertion
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("success", true);
      params.put("location", fileUrl);
      Map<String, Object> file = new LinkedHashMap<>();
      file.put("id", fileItem.getId());
      file.put("pageFileId", savedPageFile.getId());
      file.put("filename", fileItem.getFilename());
      file.put("title", fileItem.getTitle() != null ? fileItem.getTitle() : fileItem.getFilename());
      file.put("extension", fileItem.getExtension() != null ? fileItem.getExtension() : "");
      file.put("url", fileItem.getUrl());
      file.put("viewUrl", viewUrl);
      file.put("downloadUrl", fileUrl);
      file.put("createdBy", UserCommand.name(context.getUserSession().getUser()));
      file.put("fileModified", fileItem.getModified() != null ? fileItem.getModified().getTime() : "");
      file.put("fileModifiedBy", UserCommand.name(fileItem.getModifiedBy()));
      params.put("file", file);

      StringBuilder json = JsonCommand.createJsonNode(params);
      context.setJson(json.toString());
      context.setSuccess(true);
      return context;
    } catch (DataException data) {
      SaveFilePartCommand.cleanupFile(fileItemBean);
      LOG.error("Error uploading file: " + data.getMessage(), data);
      context.setJson("{\"error\": \"" + data.getMessage().replace("\"", "\\\"") + "\", \"success\": false}");
      context.setSuccess(false);
      return context;
    } catch (Exception e) {
      SaveFilePartCommand.cleanupFile(fileItemBean);
      LOG.error("Unexpected error uploading file", e);
      context.setJson("{\"error\": \"An unexpected error occurred: " + e.getMessage().replace("\"", "\\\"")
          + "\", \"success\": false}");
      context.setSuccess(false);
      return context;
    }
  }

  /**
   * A file is being deleted
   *
   * @param context
   * @return
   */
  @Override
  public WidgetContext delete(WidgetContext context) {
    LOG.debug("PageFilesWidget.delete() called");

    String command = context.getParameter("command");
    boolean isRemove = "remove".equalsIgnoreCase(command);
    boolean isDelete = "delete".equalsIgnoreCase(command);

    if (isRemove) {
      if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
        LOG.warn("User does not have permission to remove files");
        context.setErrorMessage("Permission denied. You must be an admin or content-manager to remove attachments.");
        context.setSuccess(false);
        return context;
      }
    } else if (isDelete) {
      if (!context.hasRole("admin")) {
        LOG.warn("User does not have permission to delete files");
        context.setErrorMessage("Permission denied. You must be an admin to delete files.");
        context.setSuccess(false);
        return context;
      }
    } else {
      context.setErrorMessage("Invalid delete command");
      context.setSuccess(false);
      return context;
    }

    // Verify access to the page
    WebPage webPage = LoadWebPageCommand.loadByLink(context.getRequest().getPagePath());
    if (webPage == null) {
      LOG.warn("WebPage not found for path: " + context.getRequest().getPagePath());
      context.setErrorMessage("Page not found");
      context.setSuccess(false);
      return context;
    }

    // Get the page-file relationship ID from the parameter
    long pageFileId = context.getParameterAsLong("fileId", -1);
    if (pageFileId == -1) {
      LOG.warn("No fileId parameter provided");
      context.setErrorMessage("File ID not specified");
      context.setSuccess(false);
      return context;
    }

    // Load the page-file relationship
    PageFile pageFile = PageFileRepository.findById(pageFileId);
    if (pageFile == null) {
      LOG.warn("PageFile record not found: " + pageFileId);
      context.setErrorMessage("File attachment not found");
      context.setSuccess(false);
      return context;
    }

    // Verify it belongs to this page
    if (pageFile.getWebPageId() != webPage.getId()) {
      LOG.warn("PageFile " + pageFileId + " does not belong to page " + webPage.getId());
      context.setErrorMessage("File does not belong to this page");
      context.setSuccess(false);
      return context;
    }

    // Load the actual file record
    FileItem fileItem = FileItemRepository.findById(pageFile.getFileId());
    if (fileItem == null) {
      LOG.warn("FileItem not found: " + pageFile.getFileId());
      // Still try to remove the relationship even if file is missing
      if (PageFileRepository.remove(pageFile)) {
        context.setSuccessMessage("File attachment removed (file was already deleted)");
      } else {
        context.setErrorMessage("Could not remove file attachment");
        context.setSuccess(false);
      }
      return context;
    }

    // Remove attachment or delete file
    try {
      // First remove the page-file relationship
      boolean relationshipRemoved = PageFileRepository.remove(pageFile);
      if (!relationshipRemoved) {
        LOG.error("Failed to remove page-file relationship: " + pageFileId);
        context.setErrorMessage("Could not remove file attachment");
        context.setSuccess(false);
        return context;
      }
      LOG.debug("Page-file relationship removed: " + pageFileId);

      if (isRemove) {
        // Just remove the attachment, keep the file
        LOG.debug("Attachment removed from page: " + pageFileId);
        context.setSuccessMessage("Attachment removed from this page");
      } else if (isDelete) {
        // Delete the actual file
        boolean fileDeleted = DeleteFileCommand.deleteFile(fileItem);
        if (!fileDeleted) {
          LOG.error("Failed to delete file: " + fileItem.getId());
          context.setWarningMessage("File attachment removed but file could not be deleted from storage");
        } else {
          LOG.debug("File deleted successfully: " + fileItem.getId());
          context.setSuccessMessage("File deleted successfully");
        }
      }

      return context;
    } catch (DataException e) {
      LOG.error("Error deleting file: " + e.getMessage(), e);
      context.setErrorMessage("Error deleting file: " + e.getMessage());
      context.setSuccess(false);
      return context;
    } catch (Exception e) {
      LOG.error("Unexpected error deleting file", e);
      context.setErrorMessage("An unexpected error occurred while deleting the file");
      context.setSuccess(false);
      return context;
    }
  }

  private WidgetContext attachExisting(WidgetContext context) {
    LOG.debug("PageFilesWidget.attachExisting() called");

    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson(
          "{\"success\": false, \"error\": \"Permission denied. You must be an admin or content-manager to attach files.\"}");
      context.setSuccess(false);
      return context;
    }

    WebPage webPage = LoadWebPageCommand.loadByLink(context.getRequest().getPagePath());
    if (webPage == null) {
      context.setJson("{\"success\": false, \"error\": \"Page not found\"}");
      context.setSuccess(false);
      return context;
    }

    long fileId = context.getParameterAsLong("fileId", -1);
    if (fileId == -1) {
      context.setJson("{\"success\": false, \"error\": \"File ID not specified\"}");
      context.setSuccess(false);
      return context;
    }

    FileItem fileItem = FileItemRepository.findById(fileId);
    if (fileItem == null) {
      context.setJson("{\"success\": false, \"error\": \"File not found\"}");
      context.setSuccess(false);
      return context;
    }

    List<PageFile> existingFiles = PageFileRepository.findByWebPageId(webPage.getId());
    if (existingFiles != null) {
      for (PageFile existingFile : existingFiles) {
        if (existingFile.getFileId() == fileId) {
          context.setJson(buildAttachJson(context, fileItem, existingFile.getId(), true));
          context.setSuccess(true);
          return context;
        }
      }
    }

    PageFile pageFile = new PageFile();
    pageFile.setWebPageId(webPage.getId());
    pageFile.setFileId(fileId);
    pageFile.setCreatedBy(context.getUserId());
    PageFile savedPageFile = PageFileRepository.save(pageFile);
    if (savedPageFile == null) {
      context.setJson("{\"success\": false, \"error\": \"Failed to attach file to page\"}");
      context.setSuccess(false);
      return context;
    }

    context.setJson(buildAttachJson(context, fileItem, savedPageFile.getId(), false));
    context.setSuccess(true);
    return context;
  }

  private String buildAttachJson(WidgetContext context, FileItem fileItem, long pageFileId, boolean alreadyAttached) {
    String title = fileItem.getTitle() == null || fileItem.getTitle().isBlank() ? fileItem.getFilename()
        : fileItem.getTitle();
    if (title == null) {
      title = "";
    }

    Map<String, Object> params = new LinkedHashMap<>();
    params.put("success", true);
    params.put("alreadyAttached", alreadyAttached);
    Map<String, Object> file = new LinkedHashMap<>();
    file.put("id", fileItem.getId());
    file.put("pageFileId", pageFileId);
    file.put("filename", fileItem.getFilename());
    file.put("title", title);
    file.put("extension", fileItem.getExtension());
    file.put("url", fileItem.getUrl());
    file.put("viewUrl", "/assets/view/" + fileItem.getUrl());
    file.put("downloadUrl", "/assets/file/" + fileItem.getUrl());
    file.put("createdBy", UserCommand.name(context.getUserSession().getUser()));
    file.put("fileModified", fileItem.getModified() != null ? fileItem.getModified().getTime() : "");
    file.put("fileModifiedBy", UserCommand.name(fileItem.getModifiedBy()));
    params.put("file", file);
    return JsonCommand.createJsonNode(params).toString();
  }
}
