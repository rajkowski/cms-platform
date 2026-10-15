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
package com.zeroio.platform.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.simisinc.platform.domain.model.cms.WebContainer;
import com.simisinc.platform.presentation.controller.Header;
import com.simisinc.platform.presentation.controller.Widget;
import com.simisinc.platform.presentation.controller.XMLHeaderLoader;

class XMLHeaderLoaderTest {

  private Header load(String xml) throws Exception {
    WebContainer container = new WebContainer();
    container.setName("header.test");
    container.setContainerXml(xml);
    return XMLHeaderLoader.addFromXml(container);
  }

  @Test
  void unsafeXmlIsRejected() {
    assertThrows(SAXException.class, () -> load(
        "<!DOCTYPE header [<!ENTITY external SYSTEM 'file:///nonexistent-header-fixture'>]><header name='header.test'>&external;</header>"));
  }

  @Test
  void invalidScalarValuesUseModernDefaults() throws Exception {
    Header header = load(
        "<header name='header.test' enabled='sometimes' sticky='invalid' scrollThreshold='10001'><widget name='logo'/></header>");
    assertTrue(header.isEnabled());
    assertTrue(header.isSticky());
    assertEquals(100, header.getScrollThreshold());
  }

  @Test
  void invalidModernPlacementIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> load(
        "<header name='header.test'><section visibility='invalid'><widget name='logo'/></section></header>"));
  }

  @Test
  void unknownPreferencesAndInvalidBooleanValuesUseModernDefaults() throws Exception {
    Header header = load(
        "<header name='header.test'><widget name='mainMenu'><omitFirstHomeItem>invalid</omitFirstHomeItem><unknownOption>ignored</unknownOption></widget></header>");
    Widget widget = header.getSections().get(0).getColumns().get(0).getWidgets().get(0);
    assertTrue(widget.getPreferences().containsKey("unknownOption"));
  }

  @Test
  void conflictingWrapperMetadataIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> load(
        "<header name='header.test'><column wrapper='none' class='ignored'><widget name='logo'/></column></header>"));
  }

  @Test
  void duplicateInteractiveControlsAreRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> load("<header name='header.test'><widget name='searchForm'/><widget name='searchForm'/></header>"));
    assertThrows(IllegalArgumentException.class, () -> load(
        "<header name='header.test'><widget name='headerControl'><type>region</type></widget><widget name='headerControl'><type>region</type></widget></header>"));
  }

  @Test
  void duplicateModernIdsAreRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> load("<header name='header.test' id='brand'><widget name='logo' id='brand'/></header>"));
  }

  @Test
  void sharedWidgetReferencesRetainPlacementAndPreferences() throws Exception {
    Header header = load(
        "<headers><shared><widget id='brand' name='logo'></widget></shared><header name='header.test'><widget use='brand'/></header></headers>");
    Widget widget = header.getSections().get(0).getColumns().get(0).getWidgets().get(0);
    assertEquals("brand", widget.getHtmlId());
  }

  @Test
  void shortFormAndExplicitSequenceKeepDocumentOrder() throws Exception {
    Header header = load(
        "<header name='header.test'><widget name='logo' num='90'/><widget name='searchForm' num='2'/></header>");
    assertEquals("logo", header.getSections().get(0).getColumns().get(0).getWidgets().get(0).getWidgetName());
    assertEquals("searchForm", header.getSections().get(0).getColumns().get(0).getWidgets().get(1).getWidgetName());
    assertEquals(90, header.getSections().get(0).getColumns().get(0).getWidgets().get(0).getPageWidgetCount());
  }
}