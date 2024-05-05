/*
 * Copyright 2022 SimIS Inc. (https://www.simiscms.com)
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

package com.simisinc.platform.presentation.controller;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.simisinc.platform.domain.model.cms.WebPage;

/**
 * Turns raw page XML into Page objects
 *
 * @author matt rajkowski
 * @created 4/6/18 8:00 PM
 */
public class XMLPageFactory implements Serializable {

  static final long serialVersionUID = 536435325324169646L;
  private static Log LOG = LogFactory.getLog(XMLPageLoader.class);

  public static Page createPage(String pageName, WebPage webPage, Map<String, String> widgetLibrary)
      throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);

    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = null;
    try (InputStream is = IOUtils.toInputStream(webPage.getPageXml(), "UTF-8")) {
      document = builder.parse(is);
    }
    NodeList pageTags = document.getElementsByTagName("page");
    if (pageTags.getLength() > 0) {
      Element pageTag = (Element) pageTags.item(0);
      Page page = parsePageDocument(document, pageTag, widgetLibrary);
      page.setName(pageName);
      if (StringUtils.isNotBlank(webPage.getTitle())) {
        page.setTitle(webPage.getTitle());
      }
      if (StringUtils.isNotBlank(webPage.getKeywords())) {
        page.setKeywords(webPage.getKeywords());
      }
      if (StringUtils.isNotBlank(webPage.getDescription())) {
        page.setDescription(webPage.getDescription());
      }
      LOG.debug("Created page: " + page.getName());
      return page;
    }
    return null;
  }

  public static Page parsePageDocument(Document document, Element e, Map<String, String> widgetLibrary) {
    String aName = e.getAttribute("name");
    if (aName.contains("{")) {
      aName = aName.substring(0, aName.indexOf("{"));
    }
    String collectionUniqueId = e.getAttribute("collectionUniqueId");
    String itemUniqueId = e.getAttribute("itemUniqueId");
    Page page = new Page(aName, collectionUniqueId, itemUniqueId);
    if (e.hasAttribute("title")) {
      page.setTitle(e.getAttribute("title"));
    }
    if (e.hasAttribute("keywords")) {
      page.setKeywords(e.getAttribute("keywords"));
    }
    if (e.hasAttribute("description")) {
      page.setDescription(e.getAttribute("description"));
    }
    if (e.hasAttribute("role")) {
      String aRoles = e.getAttribute("role");
      if (aRoles.length() > 0) {
        List<String> roles = Stream.of(aRoles.split(","))
            .map(String::trim)
            .collect(toList());
        page.setRoles(roles);
      }
    }
    if (e.hasAttribute("group")) {
      String aGroups = e.getAttribute("group");
      if (aGroups.length() > 0) {
        List<String> groups = Stream.of(aGroups.split(","))
            .map(String::trim)
            .collect(toList());
        page.setGroups(groups);
      }
    }
    if (e.hasAttribute("class")) {
      // Applies the html class to the page
      page.setCssClass(e.getAttribute("class"));
    }
    XMLContainerCommands.appendSections(document, page.getSections(), e.getChildNodes(), widgetLibrary);
    return page;
  }
}
