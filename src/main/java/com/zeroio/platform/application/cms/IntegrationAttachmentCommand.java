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
package com.zeroio.platform.application.cms;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.items.ItemFileItem;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.infrastructure.persistence.items.ItemFileItemRepository;

/**
 * Resolves Integration-backed attachment identifiers from CMS asset URLs.
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
public class IntegrationAttachmentCommand {

  public static final String INTEGRATION_PREFIX = "confluence-";

  private static Log log = LogFactory.getLog(IntegrationAttachmentCommand.class);

  public static String reduceAttachmentUri(String uri) {
    if (StringUtils.isBlank(uri)) {
      return null;
    }

    int prefixIndex = uri.indexOf("/" + INTEGRATION_PREFIX);
    if (prefixIndex == -1) {
      return null;
    }

    String resourceValue = uri.substring(prefixIndex + 1);
    int slashIndex = resourceValue.indexOf("/");
    if (slashIndex == -1) {
      return null;
    }
    return resourceValue.substring(0, slashIndex);
  }

  public static String extractWebPath(String resourceValue) {
    AttachmentReference reference = parseReference(resourceValue);
    return reference != null ? reference.webPath() : null;
  }

  public static long extractAttachmentId(String resourceValue) {
    AttachmentReference reference = parseReference(resourceValue);
    return reference != null ? reference.recordId() : -1;
  }

  public static FileItem loadFileItem(String resourceValue) {
    AttachmentReference reference = parseReference(resourceValue);
    if (reference == null) {
      return null;
    }
    if (reference.hasRecordId()) {
      return FileItemRepository.findByWebPathAndId(reference.webPath(), reference.recordId());
    }
    return FileItemRepository.findByWebPath(reference.webPath());
  }

  public static Image loadImage(String uri) {
    String resourceValue = reduceAttachmentUri(uri);
    AttachmentReference reference = parseReference(resourceValue);
    if (reference == null) {
      return null;
    }
    if (reference.hasRecordId()) {
      return ImageRepository.findByWebPathAndId(reference.webPath(), reference.recordId());
    }
    return ImageRepository.findByWebPath(reference.webPath());
  }

  public static ItemFileItem loadItemFileItem(String resourceValue) {
    AttachmentReference reference = parseReference(resourceValue);
    if (reference == null) {
      return null;
    }
    if (reference.hasRecordId()) {
      return ItemFileItemRepository.findByWebPathAndFileId(reference.webPath(), reference.recordId());
    }
    return ItemFileItemRepository.findByWebPath(reference.webPath());
  }

  private static AttachmentReference parseReference(String resourceValue) {
    if (StringUtils.isBlank(resourceValue) || !resourceValue.startsWith(INTEGRATION_PREFIX)) {
      return null;
    }

    String suffix = resourceValue.substring(INTEGRATION_PREFIX.length());
    int separatorIndex = suffix.indexOf('-');
    if (separatorIndex == -1) {
      return new AttachmentReference(resourceValue, -1);
    }

    long recordId = parseLongValue(suffix.substring(separatorIndex + 1));
    if (recordId == -1) {
      return new AttachmentReference(resourceValue, -1);
    }

    String webPath = INTEGRATION_PREFIX + suffix.substring(0, separatorIndex);
    return new AttachmentReference(webPath, recordId);
  }

  private static long parseLongValue(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      log.warn("Invalid number attachment identifier: " + value);
      return -1;
    }
  }

  private record AttachmentReference(String webPath, long recordId) {
    private boolean hasRecordId() {
      return recordId > -1;
    }
  }
}