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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.application.cms.LoadWebPageCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.cms.WebPageHierarchy;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageHierarchyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;

class PageBreadcrumbWidgetTest extends WidgetBase {

  @Test
  void rendersOnlyTheCurrentPagesAncestorsInOrder() {
    WebPage home = new WebPage();
    home.setId(1);
    WebPage parent = new WebPage();
    parent.setId(2);
    WebPage current = new WebPage();
    current.setId(3);
    WebPageHierarchy hierarchy = new WebPageHierarchy();
    hierarchy.setPath("/1/2/3/");
    when(pageRequest.getPagePath()).thenReturn("/current");
    try (var loader = mockStatic(LoadWebPageCommand.class);
        var hierarchies = mockStatic(WebPageHierarchyRepository.class);
        var pages = mockStatic(WebPageRepository.class)) {
      loader.when(() -> LoadWebPageCommand.loadByLink("/current")).thenReturn(current);
      hierarchies.when(() -> WebPageHierarchyRepository.findByPageId(3)).thenReturn(hierarchy);
      pages.when(() -> WebPageRepository.findById(1)).thenReturn(home);
      pages.when(() -> WebPageRepository.findById(2)).thenReturn(parent);
      pages.when(() -> WebPageRepository.findById(3)).thenReturn(current);

      new PageBreadcrumbWidget().execute(widgetContext);
      assertEquals(List.of(home, parent), pageRequest.getAttribute("breadcrumbItems"));
      assertEquals(current, pageRequest.getAttribute("currentPage"));

      widgetContext.getPreferences().put("maxItems", "1");
      new PageBreadcrumbWidget().execute(widgetContext);
      assertEquals(List.of(parent), pageRequest.getAttribute("breadcrumbItems"));
    }
  }
}