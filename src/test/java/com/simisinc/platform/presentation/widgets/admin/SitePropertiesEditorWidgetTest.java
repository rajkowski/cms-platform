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
package com.simisinc.platform.presentation.widgets.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.Insert;
import com.simisinc.platform.WidgetBase;
import com.simisinc.platform.domain.model.SiteProperty;
import com.simisinc.platform.infrastructure.persistence.SitePropertyRepository;
import com.zeroio.platform.presentation.controller.SitePropertyValueConstants;

class SitePropertiesEditorWidgetTest extends WidgetBase {

  @Test
  void themeEditorOffersAnUnsavedHeaderSelectionField() {
    widgetContext.getPreferences().put("prefix", "theme");
    try (var repository = mockStatic(SitePropertyRepository.class)) {
      repository.when(() -> SitePropertyRepository.findAllByPrefix("theme")).thenReturn(List.of());
      new SitePropertiesEditorWidget().execute(widgetContext);
      List<?> properties = (List<?>) widgetContext.getRequest().getAttribute("sitePropertyList");
      assertEquals(1, properties.size());
      assertEquals("theme.header.name", ((SiteProperty) properties.get(0)).getName());
      assertEquals("", ((SiteProperty) properties.get(0)).getValue());
    }
  }

  @Test
  void headerSelectionMetadataUsesTheExistingPropertySchema() {
    SiteProperty property = new SiteProperty();
    property.setName("theme.header.name");
    property.setLabel("Header XML Definition");
    property.setValue("header.custom");
    property.setType(SitePropertyValueConstants.TEXT);
    Insert insert = mock(Insert.class, RETURNS_SELF);
    when(insert.execute()).thenReturn(42L);
    try (var database = mockStatic(DB.class)) {
      database.when(DB::INSERT).thenReturn(insert);
      assertSame(property, SitePropertyRepository.save(property));
      assertEquals(42, property.getId());
    }
  }
}