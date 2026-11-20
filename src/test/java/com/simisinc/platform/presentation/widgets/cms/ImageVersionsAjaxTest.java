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

package com.simisinc.platform.presentation.widgets.cms;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.ImageVersion;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageVersionRepository;
import com.simisinc.platform.presentation.controller.JsonServiceContext;
import com.zeroio.platform.infrastructure.permission.Permission;

class ImageVersionsAjaxTest {

  @Test
  void versionsIncludeTheirOwnImageUrls() {
    JsonServiceContext context = mock(JsonServiceContext.class);
    when(context.getParameterAsLong("imageId", -1)).thenReturn(5L);

    Image image = new Image();
    image.setId(5L);
    image.setWebPath("202611200944");

    ImageVersion firstVersion = new ImageVersion();
    firstVersion.setId(11L);
    firstVersion.setImageId(5L);
    firstVersion.setVersionNumber(1);
    firstVersion.setFilename("original.png");

    ImageVersion secondVersion = new ImageVersion();
    secondVersion.setId(12L);
    secondVersion.setImageId(5L);
    secondVersion.setVersionNumber(2);
    secondVersion.setFilename("updated.png");

    try (var permissions = mockStatic(Permission.class);
        var images = mockStatic(ImageRepository.class);
        var versions = mockStatic(ImageVersionRepository.class)) {
      permissions.when(() -> Permission.check("cms.image.versions", null)).thenReturn(true);
      images.when(() -> ImageRepository.findById(5L)).thenReturn(image);
      versions.when(() -> ImageVersionRepository.findAllByImageId(5L)).thenReturn(List.of(firstVersion, secondVersion));

      new ImageVersionsAjax().get(context);
    }

    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(context).setJson(json.capture());
    assertTrue(json.getValue().contains(
        "\"url\":\"\\/assets\\/img\\/202611200944-5\\/original.png?versionId=11\""), json.getValue());
    assertTrue(json.getValue().contains(
        "\"url\":\"\\/assets\\/img\\/202611200944-5\\/updated.png?versionId=12\""), json.getValue());
  }
}
