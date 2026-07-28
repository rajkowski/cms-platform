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
package com.zeroio.platform.presentation.services;

import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.SaveFileCommand;
import com.simisinc.platform.application.cms.SaveFilePartCommand;
import com.simisinc.platform.application.cms.ValidateFileCommand;
import com.simisinc.platform.domain.model.User;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.infrastructure.database.AutoRollback;
import com.simisinc.platform.infrastructure.database.AutoStartTransaction;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.persistence.UserRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.simisinc.platform.presentation.services.GenericJsonService;
import com.zeroio.platform.domain.model.cms.PageFile;
import com.zeroio.platform.infrastructure.persistence.cms.PageFileRepository;

/**
 * JSON service for uploading new versions of page-attached files
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class WebPageFileVersionUploadJson extends GenericJsonService {

  private static Log LOG = LogFactory.getLog(WebPageFileVersionUploadJson.class);

  @Override
  public JsonServiceContext post(JsonServiceContext context) {
    try {
      long webPageFileId = context.getParameterAsLong("web_page_file_id", -1);
      if (webPageFileId == -1) {
        webPageFileId = context.getParameterAsLong("fileId", -1);
      }
      if (webPageFileId == -1) {
        return context.writeError("web_page_file_id is required");
      }

      PageFile pageFile = PageFileRepository.findById(webPageFileId);
      if (pageFile == null) {
        return context.writeError("Page file not found");
      }

      FileItem existingFile = FileItemRepository.findById(pageFile.getFileId());
      if (existingFile == null) {
        return context.writeError("File not found");
      }

      if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
        return context.writeError("You do not have permission to upload a new version of this file");
      }

      FileItem uploadedFile = SaveFilePartCommand.saveFile(context);
      if (uploadedFile == null) {
        return context.writeError("No file uploaded");
      }

      String submittedTitle = context.getParameter("title");
      if (submittedTitle == null || submittedTitle.trim().isEmpty()) {
        submittedTitle = uploadedFile.getFilename();
        int extensionIndex = submittedTitle.lastIndexOf('.');
        if (extensionIndex > 0) {
          submittedTitle = submittedTitle.substring(0, extensionIndex);
        }
      }

      uploadedFile.setId(existingFile.getId());
      uploadedFile.setFolderId(existingFile.getFolderId());
      uploadedFile.setSubFolderId(existingFile.getSubFolderId());
      uploadedFile.setCategoryId(existingFile.getCategoryId());
      uploadedFile.setTitle(submittedTitle);
      uploadedFile.setBarcode(existingFile.getBarcode());
      uploadedFile.setSummary(existingFile.getSummary());
      uploadedFile.setCreatedBy(existingFile.getCreatedBy());
      uploadedFile.setModifiedBy(context.getUserId());
      uploadedFile.setExpirationDate(existingFile.getExpirationDate());
      uploadedFile.setPrivacyType(existingFile.getPrivacyType());
      uploadedFile.setDefaultToken(existingFile.getDefaultToken());
      uploadedFile.setProcessed(existingFile.getProcessed());
      uploadedFile.setVersion(existingFile.getVersion());
      ValidateFileCommand.checkFile(uploadedFile);

      try (Connection connection = DB.getConnection();
          AutoStartTransaction a = new AutoStartTransaction(connection);
          AutoRollback transaction = new AutoRollback(connection)) {

        FileItem savedNewFile = SaveFileCommand.saveNewVersionOfFile(uploadedFile);
        if (savedNewFile == null) {
          SaveFilePartCommand.cleanupFile(uploadedFile);
          return context.writeError("Unable to save file");
        }

        transaction.commit();

        FileItem refreshedFile = FileItemRepository.findById(savedNewFile.getId());
        if (refreshedFile == null) {
          refreshedFile = savedNewFile;
        }

        String filename = savedNewFile.getFilename() != null ? savedNewFile.getFilename().replace("\"", "\\\"") : "";
        String title = savedNewFile.getTitle() != null ? savedNewFile.getTitle().replace("\"", "\\\"") : filename;
        String extension = savedNewFile.getExtension() != null ? savedNewFile.getExtension().replace("\"", "\\\"") : "";
        String createdBy = "System Administrator";
        User createdByUser = UserRepository.findByUserId(savedNewFile.getCreatedBy());
        if (createdByUser != null && createdByUser.getFullName() != null) {
          createdBy = createdByUser.getFullName().replace("\"", "\\\"");
        }
        String url = "/assets/file/" + savedNewFile.getWebPath() + "-" + savedNewFile.getId() + "/" + filename;
        String viewUrl = "/assets/view/" + savedNewFile.getWebPath() + "-" + savedNewFile.getId() + "/" + filename;
        String webPath = savedNewFile.getWebPath() != null ? savedNewFile.getWebPath().replace("\"", "\\\"") : "";
        String pageLink = context.getParameter("pageLink") != null ? context.getParameter("pageLink").replace("\"", "\\\"") : "";

        context.setJson("{"
            + "\"success\": true,"
            + "\"id\": " + refreshedFile.getId() + ","
            + "\"file_id\": " + refreshedFile.getId() + ","
            + "\"web_page_file_id\": " + pageFile.getId() + ","
            + "\"pageFileId\": " + pageFile.getId() + ","
            + "\"createdBy\": \"" + createdBy + "\","
            + "\"downloadUrl\": \"" + url + "\","
            + "\"extension\": \"" + extension + "\","
            + "\"filename\": \"" + filename + "\","
            + "\"pageLink\": \"" + pageLink + "\","
            + "\"title\": \"" + title + "\","
            + "\"url\": \"" + url + "\","
            + "\"viewUrl\": \"" + viewUrl + "\","
            + "\"webPath\": \"" + webPath + "\","
            + "\"version\": \"" + (refreshedFile.getVersion() != null ? refreshedFile.getVersion().replace("\"", "\\\"") : "") + "\""
            + "}");

        return context;
      }
    } catch (Exception e) {
      LOG.error("Upload failed", e);
      return context.writeError("Upload failed: " + e.getMessage());
    }
  }
}
