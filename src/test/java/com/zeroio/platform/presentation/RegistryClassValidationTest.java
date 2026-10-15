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

package com.zeroio.platform.presentation;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class RegistryClassValidationTest {

  @Test
  void configuredWidgetAndServiceClassesAreLoadable() throws Exception {
    File webAppPath = new File("src/main/webapp");
    assertConfiguredClassesLoad(webAppPath, "WEB-INF/widgets/widget-library.xml", "widget");
    assertConfiguredClassesLoad(webAppPath, "WEB-INF/json-services/json-services.xml", "service");
    assertConfiguredClassesLoad(webAppPath, "WEB-INF/rest-services/rest-services.xml", "service");
  }

  private void assertConfiguredClassesLoad(File webAppPath, String registryPath, String entryName) throws Exception {
    File registryFile = new File(webAppPath, registryPath);
    Assertions.assertTrue(registryFile.isFile(), "Registry file not found: " + registryFile);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    Document document = factory.newDocumentBuilder().parse(registryFile);
    NodeList entries = document.getElementsByTagName(entryName);
    ClassLoader classLoader = getClass().getClassLoader();

    for (int index = 0; index < entries.getLength(); index++) {
      Element entry = (Element) entries.item(index);
      String className = entry.getAttribute("class").trim();
      Assertions.assertFalse(className.isEmpty(), "Missing class attribute in " + registryPath);
      try {
        Class.forName(className, false, classLoader);
      } catch (ClassNotFoundException exception) {
        Assertions.fail("Invalid class '" + className + "' in " + registryPath, exception);
      }
    }
  }
}