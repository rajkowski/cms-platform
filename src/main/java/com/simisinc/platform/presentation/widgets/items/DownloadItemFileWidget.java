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

package com.simisinc.platform.presentation.widgets.items;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.application.items.LoadCollectionCommand;
import com.simisinc.platform.application.items.LoadItemCommand;
import com.simisinc.platform.application.items.LoadItemFileCommand;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.Item;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;
import com.simisinc.platform.presentation.controller.MultipartFileSender;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;
import com.zeroio.platform.application.cms.IntegrationAttachmentCommand;

/**
 * Description
 *
 * @author matt rajkowski
 * @created 4/19/2021 1:00 PM
 */
public class DownloadItemFileWidget extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(DownloadItemFileWidget.class);

  public WidgetContext execute(WidgetContext context) {

    // GET uri /show/*/assets/file/20180503171549-5/something.pdf
    // GET uri /show/*/assets/view/20180503171549-5/something.pdf
    // GET uri /show/*/assets/file/integration-638517504/example.drawio
    // GET uri /show/*/assets/file/integration-765265429-500/sample.pptx
    LOG.debug("Found request uri: " + context.getUri());

    // Verify access to the item
    String itemUniqueId = context.getPreferences().getOrDefault("uniqueId", context.getCoreData().get("itemUniqueId"));
    Item item = LoadItemCommand.loadItemByUniqueIdForAuthorizedUser(itemUniqueId, context.getUserId());
    if (item == null) {
      return null;
    }
    Collection collection = LoadCollectionCommand.loadCollectionByIdForAuthorizedUser(item.getCollectionId(),
        context.getUserId());
    if (collection == null) {
      return null;
    }

    // Use the request uri, and use the 5th element as the resource value

    LOG.debug("Using resource path: " + context.getResourcePath());
    // If the resource path contains a wildcard (e.g. /show/*/assets/view), truncate at /*
    // so the substring starts from the item unique ID segment, matching the uriParts index below
    String resourcePath = context.getResourcePath();
    if (resourcePath.contains("/*")) {
      resourcePath = resourcePath.substring(0, resourcePath.indexOf("/*"));
    }
    String resourceValue = context.getUri().substring(resourcePath.length() + 1);
    LOG.debug("Using resource value: " + resourceValue);
    String[] uriParts = resourceValue.split("/");
    if (uriParts.length < 5) {
      LOG.warn("Invalid request uri: " + context.getUri());
      return null;
    }
    resourceValue = uriParts[3];
    LOG.debug("Using web-path value: " + resourceValue);
    int dashIdx = resourceValue.lastIndexOf("-");
    if (dashIdx == -1) {
      LOG.warn("Invalid web-path value: " + resourceValue);
      return null;
    }

    // Determine the item file id
    long itemFileId = -1;
    if (resourceValue.startsWith(IntegrationAttachmentCommand.INTEGRATION_PREFIX)) {
      ItemFileItem integrationFileItem = IntegrationAttachmentCommand.loadItemFileItem(resourceValue);
      if (integrationFileItem != null) {
        itemFileId = integrationFileItem.getId();
      } else {
        return null;
      }
    } else {
      String itemFileIdValue = resourceValue.substring(dashIdx + 1);
      if (StringUtils.isNumeric(itemFileIdValue)) {
        itemFileId = Long.parseLong(itemFileIdValue);
      } else {
        LOG.warn("Invalid itemFileId parameter: " + resourceValue);
        return null;
      }
    }

    // Determine the file and access permissions
    ItemFileItem record;
    if (context.hasRole("admin")) {
      // The file can be downloaded
      record = LoadItemFileCommand.loadItemById(itemFileId);
    } else {
      // User must have view access in the folder's user group
      record = LoadItemFileCommand.loadFileByIdForAuthorizedUser(itemFileId, context.getUserId(), item.getId());
    }
    if (record == null) {
      LOG.warn("File record does not exist or no access: " + itemFileId);
      return null;
    }

    // Determine if this file is a remote URL
    if (record.getFileType() != null && record.getFileType().equals("URL")) {
      String url = record.getFilename();
      if (url.startsWith("http://") || url.startsWith("https://")) {
        // Update the download counter
        ItemFileItemRepository.incrementDownloadCount(record);
        // Redirect to the URL
        context.setRedirect(url);
        return context;
      }
    }

    // Make sure it exists
    File file = FileSystemCommand.getFileServerRootPath(record.getFileServerPath());
    if (!file.isFile()) {
      LOG.warn("Server file does not exist: " + record.getFileServerPath());
      return null;
    }

    // Determine if the file should be sent with the mime type
    boolean doView = "true".equals(context.getPreferences().get("view"));

    // Determine if the file is being viewed or downloaded
    String mimeType = record.getMimeType();
    long lastModified = record.getModified().getTime();
    if (doView && StringUtils.isNotBlank(mimeType)) {

      // @todo go through this and use for all downloads so pause/resume works on large files

      if (mimeType.startsWith("video/")) {
        try {
          MultipartFileSender.fromFile(file)
              .with(context.getRequest())
              .with(context.getResponse())
              .withMimeType(mimeType)
              .withFilename(record.getFilename())
              .serveResource();
        } catch (Exception e) {
          LOG.debug("Video aborted");
        } finally {
          context.setHandledResponse(true);
          // @todo determine if whole range or end was viewed to register a download count
          // Update the download counter
          //    FileItemRepository.incrementDownloadCount(record);
        }
        return context;
      }

      // The file is being viewed (in a new window)
      context.getResponse().setHeader("Content-Disposition",
          "inline; filename=\"" + UrlCommand.encodeUri(record.getFilename()) + "\";");

      // Check for a last-modified header and return 304 if possible
      long headerValue = context.getRequest().getDateHeader("If-Modified-Since");
      if (lastModified <= headerValue + 1000) {
        context.getResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        context.setHandledResponse(true);
        return context;
      }

    } else {
      // Force file to be downloaded
      mimeType = "application/octet-stream";
    }
    LOG.debug("Using mime type: " + mimeType);

    // Set header info
    context.getResponse().setDateHeader("Last-Modified", lastModified);
    context.getResponse().setContentType(mimeType);
    context.getResponse().setContentLength((int) file.length());

    // Check for head method
    if ("head".equalsIgnoreCase(context.getRequest().getMethod())) {
      context.setHandledResponse(true);
      return context;
    }

    // Stream the file
    try {
      FileInputStream in = new FileInputStream(file);
      OutputStream out = context.getResponse().getOutputStream();

      // Copy the contents of the file to the output stream
      byte[] buf = new byte[1024];
      int count = 0;
      while ((count = in.read(buf)) >= 0) {
        out.write(buf, 0, count);
      }
      out.close();
      in.close();
    } catch (Exception e) {
      LOG.debug("Stream error: " + e.getMessage());
    }

    // Update the download counter
    ItemFileItemRepository.incrementDownloadCount(record);

    // Return success
    context.setHandledResponse(true);
    return context;
  }
}
