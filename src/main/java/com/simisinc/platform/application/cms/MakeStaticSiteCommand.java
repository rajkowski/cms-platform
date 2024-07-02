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

package com.simisinc.platform.application.cms;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.granule.CSSFastMin;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.admin.SaveTextFileCommand;
import com.simisinc.platform.application.filesystem.FileSystemCommand;
import com.simisinc.platform.domain.model.cms.Blog;
import com.simisinc.platform.domain.model.cms.BlogPost;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.FileItem;
import com.simisinc.platform.domain.model.cms.Image;
import com.simisinc.platform.domain.model.cms.Stylesheet;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.persistence.cms.BlogPostRepository;
import com.simisinc.platform.infrastructure.persistence.cms.BlogPostSpecification;
import com.simisinc.platform.infrastructure.persistence.cms.ContentRepository;
import com.simisinc.platform.infrastructure.persistence.cms.FileItemRepository;
import com.simisinc.platform.infrastructure.persistence.cms.ImageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.StylesheetRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageRepository;
import com.simisinc.platform.infrastructure.persistence.cms.WebPageSpecification;
import com.simisinc.platform.presentation.controller.PageTemplateEngine;

/**
 * Make a static site version of the website and save it to the file system
 *
 * @author matt rajkowski
 * @created 12/28/23 7:08 PM
 */
public class MakeStaticSiteCommand {

  private static Log LOG = LogFactory.getLog(MakeStaticSiteCommand.class);

  public static boolean execute(Properties templateEngineProperties) throws Exception {

    long startTime = System.currentTimeMillis();

    // Verify the source website application path is valid
    String webAppPathValue = templateEngineProperties.getProperty("webAppPath");
    if (webAppPathValue == null) {
      LOG.error("Configuration missing webAppPathValue");
      return false;
    }
    File webAppPath = new File(webAppPathValue);
    if (!webAppPath.isDirectory()) {
      LOG.error("Directory not found: " + webAppPath);
      return false;
    }
    LOG.info("Using webAppPath: " + webAppPath);

    // Verify the source website file library path is valid
    File fileLibraryPath = FileSystemCommand.getFileServerRootPath();
    if (!fileLibraryPath.isDirectory()) {
      LOG.error("Directory not found: " + fileLibraryPath);
      return false;
    }
    LOG.info("Using fileLibraryPath: " + fileLibraryPath);

    // Determine the destination path where the static site will be created
    File exportDir = FileSystemCommand.getFileServerStaticSitePath();
    if (!exportDir.exists()) {
      exportDir.mkdirs();
    }
    LOG.info("Using static site path: " + exportDir);

    // Within the destination path, create a site directory for all of the website files
    File sitePath = new File(exportDir, "/site");
    sitePath.mkdir();

    // The website's assets
    File favIcon = new File(webAppPath, "/favicon.ico");
    if (favIcon.exists()) {
      FileUtils.copyFile(favIcon, new File(sitePath, "/favicon.ico"));
    }
    FileUtils.copyDirectory(new File(webAppPath, "/css"), new File(sitePath, "/css"));
    FileUtils.copyDirectory(new File(webAppPath, "/fonts"), new File(sitePath, "/fonts"));
    FileUtils.copyDirectory(new File(webAppPath, "/images"), new File(sitePath, "/images"));
    FileUtils.copyDirectory(new File(webAppPath, "/javascript"), new File(sitePath, "/javascript"));

    // The website's custom css files
    Stylesheet siteStylesheet = StylesheetRepository.findByWebPageId(-1);
    if (siteStylesheet != null) {
      // The stylesheet modified timestamp is used to allow long term cache
      CSSFastMin min = new CSSFastMin();
      String minCss = min.minimize(siteStylesheet.getCss());
      SaveTextFileCommand.save(minCss, new File(sitePath, "/css/stylesheet.css"));
    }

    // The website's custom images files
    List<Image> imageList = ImageRepository.findAll();
    LOG.info("Images: " + imageList.size());
    for (Image image : imageList) {
      // For example: assets/img/20200310012940-74/example-hero-fade.jpg
      File sourceImage = new File(fileLibraryPath, image.getFileServerPath());
      if (!sourceImage.exists()) {
        LOG.warn("File not found: " + sourceImage);
        continue;
      }
      File targetImage = new File(sitePath, "/assets/img/" + image.getWebPath() + "-" + image.getId() + "/" + image.getFilename());
      if (!targetImage.exists()) {
        FileUtils.copyFile(sourceImage, targetImage);
      }
    }

    // Assets (like videos, pdfs, etc)
    // alias /assets/view and /assets/file
    List<FileItem> fileList = FileItemRepository.findAll();
    LOG.info("Files: " + fileList.size());
    for (FileItem fileItem : fileList) {
      File sourceFile = new File(fileLibraryPath, fileItem.getFileServerPath());
      if (!sourceFile.exists()) {
        LOG.warn("File not found: " + sourceFile);
        continue;
      }
      File targetFile = new File(sitePath,
          "/assets/view/" + fileItem.getWebPath() + "-" + fileItem.getId() + "/" + fileItem.getFilename());
      if (!targetFile.exists()) {
        FileUtils.copyFile(sourceFile, targetFile);
      }
    }

    try {
      exportContent(webAppPath, sitePath, fileLibraryPath);
    } catch (Exception e) {
      LOG.error("**Error occurred: " + e.getMessage());
      return false;
    }

    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    LOG.info("Processing time: " + totalTime + " ms");
    LOG.info("Static site path: " + sitePath);
    return true;
  }

  private static boolean exportContent(File webAppPath, File sitePath, File fileLibraryPath) throws Exception {

    File exportDir = FileSystemCommand.getFileServerStaticSitePath();

    // Path for the site metadata
    File metadataPath = new File(exportDir, "/_meta");
    metadataPath.mkdir();

    // Site metadata: Web Page Meta, Layouts, CSS
    File webPagesPath = new File(metadataPath, "/pages");
    webPagesPath.mkdir();

    // Site metadata: CMS Content
    List<Content> contentList = ContentRepository.findAll();
    LOG.info("Content: " + contentList.size());
    if (!contentList.isEmpty()) {
      File contentPath = new File(metadataPath, "/content");
      contentPath.mkdir();
      for (Content content : contentList) {
        SaveTextFileCommand.save(content.getContent(), new File(contentPath, content.getUniqueId() + ".html"));
      }
    }

    // Start a sitemap
    StringBuilder xml = SitemapBuilderCommand.startSitemapXML();
    // Use the site's URL
    String siteUrl = LoadSitePropertyCommand.loadByName("site.url");
    if (StringUtils.isBlank(siteUrl)) {
      siteUrl = "https://localhost";
    }
    if (siteUrl.endsWith("/")) {
      siteUrl = siteUrl.substring(0, siteUrl.length() - 1);
    }
    // Use ISO 6801 date formatter
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ");

    // Find the published web pages
    WebPageSpecification specification = new WebPageSpecification();
    specification.setDraft(false);
    // specification.setHasRedirect(false);

    // Process the web pages
    List<WebPage> webPageList = WebPageRepository.findAll(specification, null);
    LOG.info("Web pages: " + webPageList.size());
    for (WebPage webPage : webPageList) {
      // Handle dynamic pages...
      if (webPage.getLink().endsWith("/*")) {
        // this could be a blog... look for <blogUniqueId>... query the blog posts and generate pages from this template
        String blogUniqueId = webPage.getLink().substring(1, webPage.getLink().indexOf("/*"));
        LOG.info("Checking for blog... " + blogUniqueId);
        Blog blog = LoadBlogCommand.loadBlogByUniqueId(blogUniqueId);
        if (blog != null) {
          LOG.info("Found blog: " + blog.getId());

          // Determine the criteria for valid blog posts
          BlogPostSpecification blogPostSpecification = new BlogPostSpecification();
          blogPostSpecification.setBlogId(blog.getId());
          blogPostSpecification.setPublishedOnly(true);
          blogPostSpecification.setStartDateIsBeforeNow(true);
          blogPostSpecification.setIsWithinEndDate(true);

          // Load the blog posts
          List<BlogPost> blogPostList = BlogPostRepository.findAll(blogPostSpecification, null);

          // Process the posts
          for (BlogPost blogPost : blogPostList) {
            // Turn the blog post into a web page
            String blogLink = StringUtils.replaceOnce(blogPost.getLink(), "*", blog.getUniqueId());
            webPage.setLink(blogLink);
            LOG.info("Blog post to export: " + blogLink);
            // Export the webpage
            int contentLength = exportWebPage(webPage, sitePath, webAppPath);
            exportMetadata(webPage, webPagesPath, webAppPath, fileLibraryPath, contentLength);
            SitemapBuilderCommand.appendUrlXml(xml, siteUrl, webPage, sdf);
          }
        }
        continue;
      }
      // Export the webpage
      int contentLength = exportWebPage(webPage, sitePath, webAppPath);
      exportMetadata(webPage, webPagesPath, webAppPath, fileLibraryPath, contentLength);
      SitemapBuilderCommand.appendUrlXml(xml, siteUrl, webPage, sdf);
    }

    // Save the final sitemap.xml
    SitemapBuilderCommand.endSitemapXML(xml);
    SaveTextFileCommand.save(xml.toString(), new File(sitePath, "sitemap.xml"));

    SaveTextFileCommand.save("User-agent: *\nAllow: /\nHost: " + siteUrl + "\nSitemap: " + siteUrl + "/sitemap.xml",
        new File(sitePath, "robots.txt"));

    // @todo export the redirects file

    return true;
  }

  /** The metadata describes the various CMS objects */
  public static boolean exportMetadata(WebPage webPage, File webPagesPath, File webAppPath, File fileLibraryPath, int contentLength)
      throws Exception {

    // Determine the filename
    String webPageRoot = ("/".equals(webPage.getLink()) ? "index" : webPage.getLink());
    if (webPage.getLink().indexOf("/") != webPage.getLink().lastIndexOf("/")) {
      // This page is in a subdirectory
      String linkPath = webPage.getLink().substring(0, webPage.getLink().lastIndexOf("/"));
      File linkPathFile = new File(webPagesPath, linkPath);
      linkPathFile.mkdirs();
    }

    // Site metadata: web page layout xml
    SaveTextFileCommand.save(webPage.getPageXml(), new File(webPagesPath, webPageRoot + ".xml"));

    // Site metadata: web page css
    Stylesheet stylesheet = StylesheetRepository.findByWebPageId(webPage.getId());
    if (stylesheet != null) {
      // CSSFastMin min = new CSSFastMin();
      // String minCss = min.minimize(stylesheet.getCss());
      SaveTextFileCommand.save(stylesheet.getCss(), new File(webPagesPath, webPageRoot + ".css"));
    }

    // Write out the JSON
    String metadata = WebPageMetadataCommand.getJSON(webPage, stylesheet, webPageRoot, contentLength);
    JsonNode webPageJsonData = JsonLoader.fromString(metadata);
    SaveTextFileCommand.save(webPageJsonData.toPrettyString() + "\n", new File(webPagesPath, webPageRoot + ".json"));

    return true;
  }

  /** Render the web page, save its HTML file and optional stylesheet file */
  public static int exportWebPage(WebPage webPage, File sitePath, File webAppPath) throws Exception {

    // Render the page and widgets
    LOG.debug("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    String htmlPage = PageTemplateEngine.render(webPage, webAppPath);
    if (htmlPage == null) {
      LOG.warn("No content found for " + webPage.getLink());
      return -1;
    }
    if (htmlPage.contains("\"${") || htmlPage.contains("[${")) {
      throw new Exception("Possible engine value not processed for link: " + webPage.getLink());
    }

    // Save the complete web page
    String htmlFile = ("/".equals(webPage.getLink()) ? "index" : webPage.getLink());
    LOG.info("Saving file: " + htmlFile);
    if (webPage.getLink().indexOf("/") != webPage.getLink().lastIndexOf("/")) {
      // Link is in a sub-directory
      String linkPath = webPage.getLink().substring(0, webPage.getLink().lastIndexOf("/"));
      File linkPathFile = new File(sitePath, linkPath);
      linkPathFile.mkdirs();
    }
    SaveTextFileCommand.save(htmlPage, new File(sitePath, htmlFile + ".html"));

    // Save the related stylesheet
    Stylesheet stylesheet = StylesheetRepository.findByWebPageId(webPage.getId());
    if (stylesheet != null) {
      // The stylesheet modified timestamp is used to allow long term cache
      CSSFastMin min = new CSSFastMin();
      String minCss = min.minimize(stylesheet.getCss());
      SaveTextFileCommand.save(minCss, new File(sitePath, "/css/stylesheet_" + stylesheet.getId() + ".css"));
    }
    return htmlPage.length();
  }

}
