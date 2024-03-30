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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.simisinc.platform.domain.model.cms.WebPage;

/**
 * Manages web pages and layouts for the web site
 *
 * @author matt rajkowski
 * @created 4/6/18 8:00 PM
 */
public class XMLPageLoader implements Serializable {

  static final long serialVersionUID = 536435325324169646L;
  private static Log LOG = LogFactory.getLog(XMLPageLoader.class);
  // The available pages (name = page)
  private Map<String, Page> pages;
  // The files to process
  private List<XMLPageLoaderFiles> files = new ArrayList<>();
  // The available widgets for pages
  private Map<String, String> widgetLibrary = new HashMap<>();

  public XMLPageLoader(Map<String, Page> pages) {
    this.pages = pages;
  }

  public Page get(String name) {
    return pages.get(name);
  }

  public boolean containsKey(String name) {
    return pages.containsKey(name);
  }

  public void remove(String name) {
    pages.remove(name);
  }

  public Map<String, String> getWidgetLibrary() {
    return widgetLibrary;
  }

  public void setWidgetLibrary(Map<String, String> widgetLibrary) {
    this.widgetLibrary = widgetLibrary;
  }

  public void loadWidgetLibrary(URL url) {
    try {
      Document document = parseDocument(url);
      loadWidgetLibrary(document);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadWidgetLibrary(Document document) {
    NodeList objectTags = document.getElementsByTagName("widget");
    for (int i = 0; i < objectTags.getLength(); i++) {
      Element objectTag = (Element) objectTags.item(i);
      String aName = objectTag.getAttribute("name");
      String cName = objectTag.getAttribute("class");
      widgetLibrary.put(aName, cName);
      LOG.debug("Found widget object: " + cName);
    }
  }

  public synchronized void addDirectory(File directory) throws MalformedURLException {
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        addDirectory(file);
        continue;
      }
      LOG.debug("Directory: " + directory + " found file: " + file);
      addFile(file.toURI().toURL());
    }
  }

  public synchronized void addFile(URL url) {
    LOG.debug("Adding URL: " + url.toString());
    files.add(new XMLPageLoaderFiles(url));
  }

  public synchronized void load() {
    try {
      for (XMLPageLoaderFiles file : files) {
        URL url = file.getUrl();
        LOG.info("Checking URL: " + url.toString());
        long lastModified = url.openConnection().getLastModified();
        if (file.getLastModified() == -1 || (lastModified > 0 && lastModified > file.getLastModified())) {
          LOG.info("Loading page layout: " + url.getPath());
          Document document = parseDocument(url);
          loadAllPages(document);
          file.setLastModified(lastModified);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Document parseDocument(URL url)
      throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);

    DocumentBuilder builder = factory.newDocumentBuilder();
    try (InputStream is = url.openStream()) {
      return builder.parse(is);
    }
  }

  public Page addFromXml(String pageName, WebPage webPage)
      throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
    Page page = XMLPageFactory.createPage(pageName, webPage, widgetLibrary);
    if (page != null) {
      pages.put(pageName, page);
    }
    return page;
  }

  private void loadAllPages(Document document) {
    NodeList pageTags = document.getElementsByTagName("page");
    LOG.info("Found page tags: " + pageTags.getLength());
    for (int i = 0; i < pageTags.getLength(); i++) {
      Element pageTag = (Element) pageTags.item(i);
      Page p = XMLPageFactory.parsePageDocument(document, pageTag, widgetLibrary);
      pages.put(p.getName(), p);
      LOG.debug("Found page: " + p.getName());
    }
  }

}
