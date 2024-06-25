/*
 * Copyright 2024 Matt Rajkowski (https://github.com/rajkowski)
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

package com.simisinc.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import com.simisinc.platform.application.cms.LoadStylesheetCommand;
import com.simisinc.platform.application.cms.MakeStaticSiteCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.infrastructure.cache.CacheManager;
import com.simisinc.platform.infrastructure.database.DataSource;
import com.simisinc.platform.infrastructure.database.DatabaseProperties;
import com.simisinc.platform.presentation.controller.PageTemplateEngine;

/**
 * Console app to make a static version of the site
 *
 * @author matt rajkowski
 * @created 6/22/2024 4:27 PM
 */
public class MakeStaticSiteApp {

  private static Log LOG = LogFactory.getLog(MakeStaticSiteApp.class);

  public static void main(String[] args) throws Exception {

    LOG.info("Starting...");

    System.setProperty("java.awt.headless", "true");

    Path currentPath = Paths.get(".").toAbsolutePath().normalize();

    // Determine the application's path
    File webAppPath = new File(currentPath.toFile(), "target/cms-platform");
    if (!webAppPath.isDirectory()) {
      LOG.error("Current path does not contain the web application in: " + webAppPath);
      System.exit(2);
    }
    LOG.info("Using webAppPath: " + webAppPath);

    // Connect to the database
    try (InputStream is = new FileInputStream(new File(webAppPath, "/WEB-INF/classes/database.properties"))) {
      Properties databaseProperties = DatabaseProperties.configureDatabaseProperties(is);
      DataSource.init(databaseProperties);
      LOG.info("Using database: " + databaseProperties.get("dataSource.databaseName"));
    } catch (Exception e) {
      LOG.error("Could not connect to the database");
      System.exit(2);
    }

    // Startup the CacheManager (Before any LoadSitePropertyCommand.loadByName() can be used)
    LOG.info("Startup the cache manager...");
    CacheManager.startup();

    // Use the fileLibrary for reading and outputting the static site to
    File fileLibraryPath = new File(FileSystemCommand.getFileServerRootPathValue());
    if (!fileLibraryPath.isDirectory()) {
      LOG.error("File Library path was not found at: " + fileLibraryPath);
      System.exit(2);
    }

    // Use the PageTemplateEngine for rendering html pages
    FileTemplateResolver templateResolver = new FileTemplateResolver();
    Map<String, String> widgetLibrary = WebPageXmlLayoutCommand.init(webAppPath);
    PageTemplateEngine.startup(templateResolver,
        webAppPath.getAbsolutePath() + File.separator + "WEB-INF" + File.separator + "html-templates" + File.separator, widgetLibrary);

    // The dynamic website files
    Properties templateEngineProperties = new Properties();
    templateEngineProperties.setProperty("webAppPath", webAppPath.getAbsolutePath());

    // Determine if the global stylesheet file exists
    LoadStylesheetCommand.init();

    // Export the whole site...
    MakeStaticSiteCommand.execute(templateEngineProperties);
  }
}
