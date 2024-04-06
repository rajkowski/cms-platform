/*
 * Copyright 2024 Matt Rajkowski (https://www.github.com/rajkowski)
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

package com.simisinc.platform.application.cms;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;

/**
 * Functions for image urls
 *
 * @author matt rajkowski
 * @created 4/6/24 9:26 AM
 */
public class ImageUrlCommand {

  private static Log LOG = LogFactory.getLog(ImageUrlCommand.class);

  /**
   * Parses a uri and determines the image part
   * 
   * @param uri
   * @return
   */
  public static String reduceImageUri(String uri) {

    // GET uri /assets/img/20180503171549-5/logo.png
    // /20180503171549-5/logo.png
    if (StringUtils.isBlank(uri)) {
      return null;
    }

    // Could be a URL, continue if uri starts with /
    if (!uri.startsWith("/")) {
      return null;
    }

    // Expect a string in the form of "/2" or "a/2" or "a/b/2", then a possible trailing "/x"
    int begin = uri.indexOf("/2");
    if (begin == -1) {
      return null;
    }

    // The image part
    String resourceValue = uri.substring(begin + 1);
    if (resourceValue.contains("/")) {
    // Strip off content starting with the last /
    resourceValue = resourceValue.substring(0, resourceValue.indexOf("/"));
    }
    return resourceValue;
  }

  public static String extractWebPath(String resourceValue) {
    LOG.debug("Using resource value: " + resourceValue);
    int dashIdx = resourceValue.lastIndexOf("-");
    if (dashIdx == -1) {
      return null;
    }
    return resourceValue.substring(0, dashIdx);
  }

  public static long extractFileId(String resourceValue) {
    int dashIdx = resourceValue.lastIndexOf("-");
    if (dashIdx == -1) {
      return -1;
    }
    String fileIdValue = resourceValue.substring(dashIdx + 1);
    return Long.parseLong(fileIdValue);
  }

  public static Image decodeToImageRecord(String uri) {
    String resourceValue = reduceImageUri(uri);
    if (resourceValue == null) {
      return null;
    }
    String webPath = extractWebPath(resourceValue);
    long fileId = extractFileId(resourceValue);
    Image image = ImageRepository.findByWebPathAndId(webPath, fileId);
    if (image == null) {
      LOG.warn("Requested server image record does not exist: " + fileId);
    }
    return image;
  }

}
