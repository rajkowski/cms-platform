/*
 * Copyright 2026 Matt Rajkowski
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

package com.simisinc.platform.presentation.widgets.cms;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageSpecification;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns image library data for the visual image editor
 * Only returns images the user has permission to edit
 *
 * @author matt rajkowski
 * @created 1/21/26 9:00 PM
 */
public class ImageLibraryAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908893L;
  private static Log LOG = LogFactory.getLog(ImageLibraryAjax.class);

  public WidgetContext execute(WidgetContext context) {

    LOG.debug("ImageLibraryAjax...");

    // Check permissions: only allow content editors and admins
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      LOG.debug("No permission to access image library");
      context.setJson("{\"images\":[],\"total\":0}");
      return context;
    }

    // Get pagination parameters
    int page = context.getParameterAsInt("page", 1);
    int limit = context.getParameterAsInt("limit", 20);
    String searchTerm = context.getParameter("search");

    // Build constraints
    DataConstraints constraints = new DataConstraints();
    constraints.setDefaultColumnToSortBy("created DESC");
    constraints.setPageSize(limit);
    constraints.setPageNumber(page);

    // Build specification (for future search functionality)
    ImageSpecification specification = null;
    if (StringUtils.isNotBlank(searchTerm)) {
      specification = new ImageSpecification();
      specification.setSearchTerm(searchTerm);
    }

    // Query images
    List<Image> images = ImageRepository.findAll(specification, constraints);

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"images\":[");

    boolean first = true;
    for (Image image : images) {
      if (!first) {
        sb.append(",");
      }
      first = false;

      sb.append("{");
      sb.append("\"id\":").append(image.getId()).append(",");
      sb.append("\"filename\":\"").append(JsonCommand.toJson(image.getFilename())).append("\",");
      sb.append("\"url\":\"").append(JsonCommand.toJson("/assets/img/" + image.getUrl())).append("\",");
      sb.append("\"width\":").append(image.getWidth()).append(",");
      sb.append("\"height\":").append(image.getHeight()).append(",");
      sb.append("\"fileLength\":").append(image.getFileLength()).append(",");
      sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(image.getFileType()))).append("\",");

        // Include thumbnail information if available
        sb.append("\"hasThumbnail\":").append(image.hasThumbnail()).append(",");
        if (image.hasThumbnail()) {
          sb.append("\"thumbnailUrl\":\"").append(JsonCommand.toJson("/assets/img/" + image.getThumbnailUrl())).append("\",");
          sb.append("\"thumbnailWidth\":").append(image.getProcessedWidth()).append(",");
          sb.append("\"thumbnailHeight\":").append(image.getProcessedHeight()).append(",");
        }

      // Format created timestamp
      if (image.getCreated() != null) {
        sb.append("\"created\":\"").append(JsonCommand.toJson(image.getCreated().toString())).append("\"");
      } else {
        sb.append("\"created\":null");
      }

      sb.append("}");
    }

    sb.append("],");
    sb.append("\"page\":").append(page).append(",");
    sb.append("\"limit\":").append(limit).append(",");
    sb.append("\"total\":").append(images.size());
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
