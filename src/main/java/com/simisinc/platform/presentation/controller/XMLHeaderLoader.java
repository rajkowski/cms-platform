/*
 * Copyright 2026 Matt Rajkowski (https://www.github.com/rajkowski)
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
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.simisinc.platform.domain.model.cms.WebContainer;
import com.zeroio.platform.presentation.widgets.cms.HeaderControlWidget;

/**
 * Processor for the header XML
 *
 * @author matt rajkowski
 * @created 1/17/21 10:48 AM
 */
public class XMLHeaderLoader implements Serializable {

  static final long serialVersionUID = 536435325324169646L;
  private static Log LOG = LogFactory.getLog(XMLHeaderLoader.class);

  public XMLHeaderLoader() {
  }

  public static String loadValidXmlFromURL(String layoutName, URL url) throws Exception {
    Document document = parseDocument(url);
    NodeList headers = document.getElementsByTagName("header");
    for (int index = 0; index < headers.getLength(); index++) {
      Element header = (Element) headers.item(index);
      if (!layoutName.equals(header.getAttribute("name"))) {
        continue;
      }
      TransformerFactory factory = TransformerFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
      var transformer = factory.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      StringWriter output = new StringWriter();
      transformer.transform(new DOMSource(header), new StreamResult(output));
      return output.toString();
    }
    return null;
  }

  public static Header loadFromURL(String layoutName, URL url) {
    try {
      Document document = parseDocument(url);
      return parseDocument(document, layoutName);
    } catch (Exception e) {
      LOG.error(e);
    }
    return null;
  }

  private static Document parseDocument(URL url)
      throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);

    DocumentBuilder builder = factory.newDocumentBuilder();
    try (InputStream is = url.openStream()) {
      return builder.parse(is);
    }
  }

  public static Header addFromXml(WebContainer webContainer)
      throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);

    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = null;
    try (InputStream is = IOUtils.toInputStream(webContainer.getContainerXml(), "UTF-8")) {
      document = builder.parse(is);
    }
    return parseDocument(document, webContainer.getName());
  }

  private static Header parseDocument(Document document, String layoutName) {
    NodeList containerTags = document.getElementsByTagName("header");
    int numberOfLayouts = containerTags.getLength();
    if (numberOfLayouts == 0) {
      return null;
    }
    Element container = null;
    if (numberOfLayouts == 1) {
      container = (Element) containerTags.item(0);
    } else {
      for (int i = 0; i < containerTags.getLength(); i++) {
        Element thisContainer = (Element) containerTags.item(i);
        String thisName = thisContainer.getAttribute("name");
        if (layoutName.equals(thisName)) {
          container = thisContainer;
          break;
        }
      }
    }
    if (container != null) {
      Header header = parseContainer(document, container);
      // @note set it back to the requested layout name regardless of what the document has
      header.setName(layoutName);
      LOG.debug("Created header: " + header.getName());
      return header;
    }
    return null;
  }

  private static Header parseContainer(Document document, Element container) {
    String layoutName = container.getAttribute("name");
    if (layoutName.contains("{")) {
      layoutName = layoutName.substring(0, layoutName.indexOf("{"));
    }
    Header header = new Header(layoutName);
    header.setTitle(container.getAttribute("title"));
    if (layoutName.isBlank()) {
      throw new IllegalArgumentException("A header requires a nonempty name");
    }
    String htmlId = container.getAttribute("id").trim();
    if (!htmlId.isEmpty()) {
      header.setHtmlId(htmlId);
    }
    header.setCssStyle(container.getAttribute("style"));
    header.setEnabled(readBoolean(container, "enabled", true));
    header.setSticky(readBoolean(container, "sticky", true));
    String scrollBehavior = container.getAttribute("scrollBehavior").trim();
    if (scrollBehavior.isEmpty()) {
      scrollBehavior = "hide-on-down";
    }
    if (!"hide-on-down".equals(scrollBehavior) && !"none".equals(scrollBehavior)) {
      throw new IllegalArgumentException("Invalid header scrollBehavior: " + scrollBehavior + "; expected hide-on-down or none");
    }
    header.setScrollBehavior(scrollBehavior);
    header.setScrollThreshold(readInteger(container, "scrollThreshold", 100, 0, 10000));
    if (container.hasAttribute("role")) {
      String aRoles = container.getAttribute("role");
      if (aRoles.length() > 0) {
        List<String> roles = Stream.of(aRoles.split(","))
            .map(String::trim)
            .collect(toList());
        header.setRoles(roles);
      }
    }
    if (container.hasAttribute("group")) {
      String aGroups = container.getAttribute("group");
      if (aGroups.length() > 0) {
        List<String> groups = Stream.of(aGroups.split(","))
            .map(String::trim)
            .collect(toList());
        header.setGroups(groups);
      }
    }
    if (container.hasAttribute("class")) {
      // Applies the html class to the header
      header.setCssClass(container.getAttribute("class"));
    }
    // Create a shared context for the page to track the overall row/column/widget sequences
    PageRenderContext pageRenderContext = new PageRenderContext();
    XMLContainerCommands.appendSections(pageRenderContext, document, header.getSections(), container.getChildNodes());
    Set<String> identities = new HashSet<>();
    Set<String> controls = new HashSet<>();
    validateIdentity(header.getHtmlId(), identities);
    for (Section section : header.getSections()) {
      validateIdentity(section.getHtmlId(), identities);
      for (Column column : section.getColumns()) {
        validateIdentity(column.getHtmlId(), identities);
        if ("none".equals(column.getWrapper())
            && (column.getHtmlId() != null || column.getCssClass() != null || column.getCssStyle() != null || column.isSticky())) {
          throw new IllegalArgumentException("Column wrapper=none requires absent id, class, style, and sticky settings");
        }
        for (Widget widget : column.getWidgets()) {
          validateIdentity(widget.getHtmlId(), identities);
          if ("none".equals(widget.getWrapper())
              && (widget.getHtmlId() != null || widget.getCssClass() != null || widget.getCssStyle() != null || widget.isSticky())) {
            throw new IllegalArgumentException("Widget wrapper=none requires absent id, class, style, and sticky settings");
          }
          // validateOptionalPreferences(header, widget);
          String control = null;
          if ("searchForm".equals(widget.getWidgetName()) || "headerAccountMenu".equals(widget.getWidgetName())
              || "mainMenu".equals(widget.getWidgetName())) {
            control = widget.getWidgetName();
          } else if ("headerControl".equals(widget.getWidgetName())) {
            String type = widget.getPreferences().get("type");
            if (!HeaderControlWidget.supportsType(type)) {
              throw new IllegalArgumentException("headerControl type must be region, appearance, askAI, or mobileMenu");
            }
            if (!"appearance".equals(type)) {
              control = type;
            }
          }
          if (control != null && !controls.add(control)) {
            throw new IllegalArgumentException("Headers support one instance of " + control);
          }
        }
      }
    }
    return header;
  }

  private static void validateIdentity(String identity, Set<String> identities) {
    if (identity == null || identity.isEmpty()) {
      return;
    }
    if (identity.chars().anyMatch(Character::isWhitespace) || !identities.add(identity)) {
      throw new IllegalArgumentException("Header IDs must be unique and contain no whitespace: " + identity);
    }
  }


  private static boolean readBoolean(Element element, String attribute, boolean defaultValue) {
    String value = element.getAttribute(attribute).trim();
    if (value.isEmpty()) {
      return defaultValue;
    }
    if ("true".equals(value) || "false".equals(value)) {
      return Boolean.parseBoolean(value);
    }
    LOG.warn(
        "Header " + element.getAttribute("name") + " has invalid " + attribute + "; expected true or false; using " + defaultValue);
    return defaultValue;
  }

  private static int readInteger(Element element, String attribute, int defaultValue, int minimum, int maximum) {
    String value = element.getAttribute(attribute).trim();
    if (value.isEmpty()) {
      return defaultValue;
    }
    try {
      int parsedValue = Integer.parseInt(value);
      if (parsedValue >= minimum && parsedValue <= maximum) {
        return parsedValue;
      }
    } catch (NumberFormatException exception) {
      LOG.debug("Invalid integer header preference: " + attribute);
    }
    LOG.warn("Header " + element.getAttribute("name") + " has invalid " + attribute + "; expected " + minimum + "-" + maximum
        + "; using " + defaultValue);
    return defaultValue;
  }
}
