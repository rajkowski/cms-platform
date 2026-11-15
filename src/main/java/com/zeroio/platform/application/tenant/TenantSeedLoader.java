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
package com.zeroio.platform.application.tenant;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rajkowski.database.DB;
import com.simisinc.platform.application.DataException;
import com.simisinc.platform.application.cms.MakeContentUniqueIdCommand;
import com.simisinc.platform.application.cms.SaveBlogCommand;
import com.simisinc.platform.application.cms.SaveCalendarCommand;
import com.simisinc.platform.application.cms.SaveContentCommand;
import com.simisinc.platform.application.cms.SaveFolderCommand;
import com.simisinc.platform.application.cms.SaveFormDefinitionCommand;
import com.simisinc.platform.application.cms.SaveWebPageCommand;
import com.simisinc.platform.application.items.SaveCollectionCommand;
import com.simisinc.platform.domain.model.cms.Blog;
import com.simisinc.platform.domain.model.cms.Calendar;
import com.simisinc.platform.domain.model.cms.Content;
import com.simisinc.platform.domain.model.cms.Folder;
import com.simisinc.platform.domain.model.cms.FormDefinition;
import com.simisinc.platform.domain.model.cms.MenuTab;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.domain.model.items.Collection;
import com.simisinc.platform.domain.model.items.PrivacyType;
import com.simisinc.platform.infrastructure.persistence.SitePropertyRepository;
import com.simisinc.platform.infrastructure.persistence.cms.MenuTabRepository;
import com.simisinc.platform.infrastructure.web.WebApp;
import com.zeroio.platform.domain.model.tenant.Workspace;

public class TenantSeedLoader {

  private static Log LOG = LogFactory.getLog(TenantSeedLoader.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String SEED_FILE = "/WEB-INF/classes/database/tenant-seed.json";
  private static final String WEB_TEMPLATES_PAGE_DIR = "/WEB-INF/web-templates/page";

  private TenantSeedLoader() {
  }

  public static void seedDatabase(Workspace workspace) throws Exception {

    LOG.debug("Starting database seeding for workspace: " + workspace.getName());

    if (!WebApp.isAvailable()) {
      LOG.error("WebApp is not available");
      return;
    }

    try (InputStream inputStream = WebApp.getResourceAsStream(SEED_FILE)) {
      LOG.debug("Reading seed file: " + SEED_FILE);
      String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
      if (StringUtils.isBlank(json)) {
        LOG.error("Seed file is blank: " + SEED_FILE);
        return;
      }

      JsonNode seed = MAPPER.readTree(json);
      seedSiteProperties(seed.path("site_properties"));
      seedSiteProperties(seed.path("theme_properties"));
      seedMenuTabs(seed.path("navigation").path("menu_tabs"));
      seedWebPages(seed.path("web_pages"));
      seedContent(seed.path("content"));
      seedBlogs(seed.path("blogs"));
      seedFolders(seed.path("folders"));
      seedForms(seed.path("forms").path("form_definitions"));
      seedCollections(seed.path("collections"));
      seedCalendars(seed.path("calendars"));
    }
  }

  private static long firstUserId() {
    Long userId = DB.SELECT("MIN(user_id)").FROM("users").returnRecord(resultSet -> resultSet.getLong(1));
    return userId == null ? -1 : userId;
  }

  private static void seedMenuTabs(JsonNode tabs) {
    LOG.debug("Seeding menu tabs");
    if (tabs == null || !tabs.isArray()) {
      return;
    }
    for (JsonNode tab : tabs) {
      MenuTab menuTabBean = MenuTabRepository.findByLink(value(tab, "link"));
      if (menuTabBean != null) {
        continue;
      }
      menuTabBean = new MenuTab();
      menuTabBean.setTabOrder(tab.path("tab_order").asInt(100));
      menuTabBean.setName(value(tab, "name"));
      menuTabBean.setLink(value(tab, "link"));
      menuTabBean.setPageTitle(value(tab, "page_title"));
      menuTabBean.setDraft(tab.path("draft").asBoolean(false));
      menuTabBean.setEnabled(tab.path("enabled").asBoolean(true));
      MenuTab menuTab = MenuTabRepository.save(menuTabBean);
      if (menuTab == null) {
        throw new IllegalStateException("Failed to save web page: " + menuTabBean.getLink());
      }
    }
  }

  private static void seedWebPages(JsonNode pages) throws Exception {
    LOG.debug("Seeding web pages");
    if (pages == null || !pages.isArray()) {
      return;
    }
    long userId = firstUserId();
    for (JsonNode page : pages) {
      // Validate the link for the web page
      String link = value(page, "link");
      if (StringUtils.isBlank(link)) {
        continue;
      }
      if (exists("web_pages", "link = ?", link)) {
        continue;
      }

      // Determine the page layout
      String pageXmlFile = value(page, "page_xml_file");
      String pageXml = readOptionalFile(WEB_TEMPLATES_PAGE_DIR + "/" + pageXmlFile);
      if (StringUtils.isBlank(pageXml)) {
        throw new IllegalStateException(
            "Page XML was not found for: " + link + " (expected page_xml_file " + pageXmlFile + ")");
      }

      // Replace placeholder in the page XML with the link value
      String pageTitle = value(page, "page_title");
      String webPageName = MakeContentUniqueIdCommand.parseToValidValue(pageTitle);
      pageXml = Strings.CS.replace(pageXml, "${webPageName}", webPageName);

      // Prepare the web page bean with the necessary attributes before saving
      WebPage webPageBean = new WebPage();
      webPageBean.setLink(link);
      webPageBean.setTitle(pageTitle);
      webPageBean.setKeywords(value(page, "keywords"));
      webPageBean.setDescription(value(page, "description"));
      webPageBean.setComments(value(page, "comments"));
      webPageBean.setPageXml(pageXml);
      webPageBean.setSearchable(page.path("searchable").asBoolean(true));
      webPageBean.setShowInSitemap(page.path("show_in_sitemap").asBoolean(true));
      webPageBean.setDraft(false);
      webPageBean.setSitemapPriority(new BigDecimal(
          page.path("sitemap_priority").isMissingNode() ? 0.5d : page.path("sitemap_priority").asDouble()));
      webPageBean.setSitemapChangeFrequency(valueOrDefault(page, "sitemap_change_freq", "weekly"));
      // webPageBean.setTags();
      webPageBean.setTemplate(value(page, "template"));
      // webPageBean.setLocale(textOrDefault(page, "locale", "en"));
      webPageBean.setCreatedBy(userId);

      // Save the web page
      WebPage savedWebPage = SaveWebPageCommand.saveWebPage(webPageBean);
      if (savedWebPage == null) {
        throw new IllegalStateException("Failed to save web page: " + link);
      }
    }
  }

  private static void seedForms(JsonNode forms) {
    LOG.debug("Seeding forms");
    if (forms == null || !forms.isArray()) {
      return;
    }
    long userId = firstUserId();
    for (JsonNode form : forms) {
      if (exists("form_definitions", "name = ?", value(form, "name"))) {
        continue;
      }

      FormDefinition formDefinitionBean = new FormDefinition();
      formDefinitionBean.setName(value(form, "name"));
      formDefinitionBean.setCreatedBy(userId);

      try {
        FormDefinition savedFormDefinition = SaveFormDefinitionCommand.saveFormDefinition(formDefinitionBean);
        if (savedFormDefinition == null) {
          throw new IllegalStateException("Failed to save form definition: " + value(form, "name"));
        }
      } catch (Exception e) {
        throw new IllegalStateException("Failed to save form definition: " + value(form, "name"), e);
      }
    }
  }

  private static void seedCollections(JsonNode collections) throws DataException {
    LOG.debug("Seeding collections");
    if (collections == null || !collections.isArray()) {
      return;
    }
    long userId = firstUserId();
    for (JsonNode collection : collections) {
      if (exists("collections", "name = ?", value(collection, "name"))) {
        continue;
      }
      Collection collectionBean = new Collection();
      collectionBean.setName(value(collection, "name"));
      collectionBean.setDescription(value(collection, "description"));
      collectionBean.setCreatedBy(userId);
      collectionBean.setGuestPrivacyType(PrivacyType.PUBLIC);

      Collection savedCollection = SaveCollectionCommand.saveCollection(collectionBean);
      if (savedCollection == null) {
        throw new IllegalStateException("Failed to save collection: " + value(collection, "name"));
      }
    }
  }

  private static void seedCalendars(JsonNode calendars) throws DataException {
    LOG.debug("Seeding calendars");
    if (calendars == null || !calendars.isArray()) {
      return;
    }
    long userId = firstUserId();
    for (JsonNode calendar : calendars) {
      if (exists("calendars", "name = ?", value(calendar, "name"))) {
        continue;
      }
      Calendar calendarBean = new Calendar();
      calendarBean.setName(value(calendar, "name"));
      calendarBean.setDescription(value(calendar, "description"));
      // calendarBean.setHolidays(calendar.path("holidays").asBoolean(false));
      calendarBean.setColor(value(calendar, "color"));
      calendarBean.setEnabled(calendar.path("enabled").asBoolean(true));
      calendarBean.setCreatedBy(userId);

      Calendar savedCalendar = SaveCalendarCommand.saveCalendar(calendarBean);
      if (savedCalendar == null) {
        throw new IllegalStateException("Failed to save calendar: " + value(calendar, "name"));
      }
    }
  }

  private static void seedContent(JsonNode content) throws DataException {
    LOG.debug("Seeding content");
    if (content == null || !content.isArray()) {
      return;
    }
    long userId = firstUserId();
    for (JsonNode item : content) {
      String contentUniqueId = value(item, "content_unique_id");
      if (StringUtils.isBlank(contentUniqueId)) {
        continue;
      }
      if (exists("content", "content_unique_id = ?", contentUniqueId)) {
        continue;
      }
      Content contentBean = new Content();
      contentBean.setUniqueId(contentUniqueId);
      contentBean.setContent(value(item, "content"));
      // contentBean.setContentText(text(item, "content"));
      // contentBean.setLocale(textOrDefault(item, "locale", "en"));
      contentBean.setCreatedBy(userId);

      Content savedContent = SaveContentCommand.saveContent(contentBean, false);
      if (savedContent == null) {
        throw new IllegalStateException("Failed to save content: " + contentUniqueId);
      }
    }
  }

  private static void seedBlogs(JsonNode blogs) throws DataException {
    LOG.debug("Seeding blogs");
    if (blogs == null || !blogs.isArray()) {
      return;
    }
    long userId = firstUserId();
    for (JsonNode blog : blogs) {
      if (exists("blogs", "name = ?", value(blog, "name"))) {
        continue;
      }
      Blog blogBean = new Blog();
      blogBean.setName(value(blog, "name"));
      blogBean.setDescription(value(blog, "description"));
      blogBean.setCreatedBy(userId);
      blogBean.setModifiedBy(userId);
      blogBean.setEnabled(blog.path("enabled").asBoolean(true));

      Blog savedBlog = SaveBlogCommand.saveBlog(blogBean);
      if (savedBlog == null) {
        throw new IllegalStateException("Failed to save blog: " + value(blog, "name"));
      }
    }
  }

  private static void seedFolders(JsonNode folders) throws DataException {
    LOG.debug("Seeding folders");
    if (folders == null || !folders.isArray()) {
      return;
    }
    long userId = firstUserId();
    for (JsonNode folder : folders) {
      Folder folderBean = new Folder();
      folderBean.setName(value(folder, "name"));
      folderBean.setSummary(value(folder, "description"));
      folderBean.setCreatedBy(userId);
      folderBean.setModifiedBy(userId);
      folderBean.setGuestPrivacyType(PrivacyType.PUBLIC);

      Folder savedFolder = SaveFolderCommand.saveFolder(folderBean);
      if (savedFolder == null) {
        throw new IllegalStateException("Failed to save folder: " + value(folder, "name"));
      }
    }
  }

  private static void seedSiteProperties(JsonNode path) {
    LOG.debug("Seeding site properties");
    if (path == null || !path.isArray()) {
      return;
    }
    for (JsonNode property : path) {
      if (StringUtils.isBlank(value(property, "name"))) {
        continue;
      }
      if (value(property, "value") == null) {
        continue;
      }
      boolean updated = SitePropertyRepository.saveValue(
          value(property, "name"),
          value(property, "value"));
      if (!updated) {
        throw new IllegalStateException("Failed to update site property: " + value(property, "name"));
      }
    }
  }

  private static String readOptionalFile(String path) throws Exception {
    if (StringUtils.isBlank(path)) {
      return null;
    }
    try (InputStream inputStream = WebApp.getResourceAsStream(path)) {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    }
  }

  private static String value(JsonNode node, String fieldName) {
    return node == null ? null : valueOrDefault(node, fieldName, null);
  }

  private static String valueOrDefault(JsonNode node, String fieldName, String defaultValue) {
    if (node == null || node.path(fieldName).isMissingNode() || node.path(fieldName).isNull()) {
      return defaultValue;
    }
    String value = node.path(fieldName).asText();
    return StringUtils.isBlank(value) ? defaultValue : value;
  }

  private static boolean exists(String tableName, String whereClause, Object value) {
    return DB.SELECT("1").FROM(tableName).WHERE(whereClause, value).returnRecord(resultSet -> Boolean.TRUE) != null;
  }
}
