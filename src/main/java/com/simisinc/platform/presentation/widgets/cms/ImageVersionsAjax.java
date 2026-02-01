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
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;
import com.simisinc.platform.presentation.controller.WidgetContext;
import com.simisinc.platform.presentation.widgets.GenericWidget;

/**
 * Returns image version history for the visual image editor
 *
 * @author matt rajkowski
 * @created 1/31/26 9:40 AM
 */
public class ImageVersionsAjax extends GenericWidget {

  static final long serialVersionUID = -8484048371911908896L;
  private static Log LOG = LogFactory.getLog(ImageVersionsAjax.class);

  @Override
  public WidgetContext execute(WidgetContext context) {

    LOG.debug("ImageVersionsAjax...");

    // Restrict access to editors
    if (!context.hasRole("admin") && !context.hasRole("content-manager")) {
      context.setJson("{\"versions\":[]}");
      return context;
    }

    long imageId = context.getParameterAsLong("imageId", -1);
    if (imageId == -1) {
      context.setJson("{\"success\": false, \"error\": \"Image 'ID' required\"}");
      context.setSuccess(false);
      return context;
    }

    // Query versions
    List<ImageVersion> versions = ImageVersionRepository.findAllByImageId(imageId);

    // Build JSON response
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"versions\":[");

    boolean first = true;
    for (ImageVersion version : versions) {
      if (!first) {
        sb.append(",");
      }
      first = false;

      sb.append("{");
      sb.append("\"id\":").append(version.getId()).append(",");
      sb.append("\"versionNumber\":").append(version.getVersionNumber()).append(",");
      sb.append("\"filename\":\"").append(JsonCommand.toJson(version.getFilename())).append("\",");
      sb.append("\"fileLength\":").append(version.getFileLength()).append(",");
      sb.append("\"fileType\":\"").append(JsonCommand.toJson(StringUtils.defaultString(version.getFileType()))).append("\",");
      sb.append("\"width\":").append(version.getWidth()).append(",");
      sb.append("\"height\":").append(version.getHeight()).append(",");
      sb.append("\"isCurrent\":").append(version.getIsCurrent()).append(",");

      // Format created timestamp
      if (version.getCreated() != null) {
        sb.append("\"created\":\"").append(JsonCommand.toJson(version.getCreated().toString())).append("\"");
      } else {
        sb.append("\"created\":null");
      }

      // Add notes if present
      if (StringUtils.isNotBlank(version.getNotes())) {
        sb.append(",\"notes\":\"").append(JsonCommand.toJson(version.getNotes())).append("\"");
      }

      sb.append("}");
    }

    sb.append("]");
    sb.append("}");

    context.setJson(sb.toString());
    return context;
  }
}
