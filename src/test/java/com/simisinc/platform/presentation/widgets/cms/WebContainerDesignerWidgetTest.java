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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.domain.model.cms.WebContainer;
import com.simisinc.platform.infrastructure.persistence.cms.WebContainerRepository;

class WebContainerDesignerWidgetTest extends WidgetBase {

  @Test
  void bundledHeaderCanBeOpenedWithoutPublishingARecord() throws Exception {
    addQueryParameter(widgetContext, "name", "header.default");
    when(servletContext.getResource("/WEB-INF/web-layouts/header/header-layout.xml"))
        .thenReturn(Path.of("src/main/webapp/WEB-INF/web-layouts/header/header-layout.xml").toAbsolutePath().toUri().toURL());
    try (var repository = mockStatic(WebContainerRepository.class)) {
      new WebContainerDesignerWidget().execute(widgetContext);
      WebContainer container = (WebContainer) pageRequest.getAttribute("webContainer");
      assertNotNull(container);
      assertTrue(container.getContainerXml().contains("header.default"));
      assertTrue(container.getId() < 1);
      repository.verify(() -> WebContainerRepository.findByName("header.default"));
    }
  }
}