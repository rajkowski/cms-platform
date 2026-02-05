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

package com.simisinc.platform.application.cms;

import java.util.List;

import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;

/**
 * Loads the version history for an image
 *
 * @author matt rajkowski
 * @created 2/3/26 10:00 PM
 */
public class LoadImageVersionListCommand {

  /**
   * Gets all versions for an image, ordered by version number descending
   *
   * @param imageId the image to get versions for
   * @return list of ImageVersion objects, or null if image not found
   */
  public static List<ImageVersion> loadVersions(long imageId) {
    if (imageId == -1) {
      return null;
    }
    return ImageVersionRepository.findAllByImageId(imageId);
  }
}
