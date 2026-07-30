/*
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

package com.simisinc.platform.infrastructure.persistence.cms;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.simisinc.platform.application.cms.TextCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.infrastructure.database.AutoRollback;
import com.simisinc.platform.infrastructure.database.AutoStartTransaction;
import com.simisinc.platform.infrastructure.database.DB;
import com.simisinc.platform.infrastructure.database.DataConstraints;
import com.simisinc.platform.infrastructure.database.DataResult;
import com.simisinc.platform.infrastructure.database.SqlJoins;
import com.simisinc.platform.infrastructure.database.SqlUtils;
import com.simisinc.platform.infrastructure.database.SqlValue;
import com.simisinc.platform.infrastructure.database.SqlWhere;
import com.zeroio.platform.infrastructure.persistence.cms.PageFileRepository;
import com.zeroio.platform.infrastructure.persistence.cms.WebPageVersionRepository;

/**
 * Persists and retrieves web page objects
 *
 * @author matt rajkowski
 * @created 5/3/18 5:44 PM
 */
public class WebPageRepository {

  private static Log LOG = LogFactory.getLog(WebPageRepository.class);

  private static String TABLE_NAME = "web_pages";
  private static String[] PRIMARY_KEY = new String[] { "web_page_id" };
  private static final int MAX_INDIRECT_REFERENCE_DEPTH = 5;
  private static final int MAX_CONTENT_REFERENCE_NODES = 250;
  private static final int MAX_INDIRECT_PAGES = 500;

  private static DataResult query(WebPageSpecification specification, DataConstraints constraints) {
    SqlUtils select = new SqlUtils();
    SqlJoins joins = new SqlJoins();
    SqlWhere where = null;
    SqlUtils orderBy = null;

    if (specification != null) {

      where = DB.WHERE()
          .andAddIfHasValue("LOWER(link) = ?", specification.getLink())
          .andAddIfDataConstantExists("enabled = ?", specification.getEnabled())
          .andAddIfDataConstantExists("draft = ?", specification.getDraft())
          .andAddIfDataConstantExists("searchable = ?", specification.getSearchable())
          .andAddIfDataConstantExists("show_in_sitemap = ?", specification.getInSitemap())
          .andAddIfDataConstantExists("has_redirect = ?", specification.getHasRedirect());

      if (specification.getRegionTags() != null && specification.getRegionTags().length > 0) {
        where.AND("web_pages.tags", specification.getRegionTags(), SqlWhere.OR_OPERATOR);
      }

      if (specification.getFilterTags() != null && specification.getFilterTags().length > 0) {
        where.AND("web_pages.tags", specification.getFilterTags(), SqlWhere.AND_OPERATOR);
      }

      // Add modified date range filters
      if (specification.getModifiedAfter() != null) {
        where.AND("web_pages.modified >= ?", specification.getModifiedAfter());
      }
      if (specification.getModifiedBefore() != null) {
        where.AND("web_pages.modified <= ?", specification.getModifiedBefore());
      }

      // Add modified by user filter
      if (specification.getModifiedByUserIds() != null && specification.getModifiedByUserIds().length > 0) {
        StringBuilder userCondition = new StringBuilder("web_pages.modified_by IN (");
        Long[] userIdsBoxed = new Long[specification.getModifiedByUserIds().length];
        for (int i = 0; i < specification.getModifiedByUserIds().length; i++) {
          if (i > 0) {
            userCondition.append(",");
          }
          userCondition.append("?");
          userIdsBoxed[i] = specification.getModifiedByUserIds()[i];
        }
        userCondition.append(")");
        where.AND(userCondition.toString(), (Object[]) userIdsBoxed);
      }

      if (StringUtils.isNotBlank(specification.getSearchTerm())) {

        String term = specification.getSearchTerm().trim().toLowerCase();

        String quotedSearchTerm = "\"" + term + "\"";
        String searchTermSeparated = term.replaceAll("\\s+", " OR ");
        String searchToUse = quotedSearchTerm + " OR " + searchTermSeparated;
        String titleSearchPattern1 = term + "%";
        String titleSearchPattern2 = "%" + term + "%";

        select.add(
            "ts_headline('english', page_text, websearch_to_tsquery('web_page_stem', ?), 'StartSel=${b}, StopSel=${/b}, MaxWords=40, MinWords=30, ShortWord=3, HighlightAll=FALSE, MaxFragments=2, FragmentDelimiter=\" ... \"') AS highlight",
            term);
        select.add(
            "(ts_rank_cd(web_pages.tsv, websearch_to_tsquery('web_page_stem', ?)) + CASE WHEN LOWER(web_pages.page_title) LIKE LOWER(?) THEN 200.0 ELSE 0.0 END + CASE WHEN LOWER(web_pages.page_title) LIKE LOWER(?) THEN 100.0 ELSE 0.0 END) AS rank",
            new String[] { searchToUse, titleSearchPattern1, titleSearchPattern2 });

        where.AND("web_pages.tsv @@ websearch_to_tsquery('web_page_stem', ?)", searchToUse);

        // Override the order by for rank first
        orderBy = new SqlUtils();
        orderBy.add("rank DESC, web_pages.modified DESC");
      }
    }

    return DB.selectAllFrom(TABLE_NAME, select, joins, where, orderBy, constraints, WebPageRepository::buildRecord);
  }

  public static WebPage findById(long id) {
    if (id == -1) {
      return null;
    }
    return (WebPage) DB.selectRecordFrom(
        TABLE_NAME,
        DB.WHERE("web_page_id = ?", id),
        WebPageRepository::buildRecord);
  }

  public static WebPage findByLink(String link) {
    if (StringUtils.isBlank(link)) {
      return null;
    }
    return (WebPage) DB.selectRecordFrom(
        TABLE_NAME,
        DB.WHERE("LOWER(link) = ?", link),
        WebPageRepository::buildRecord);
  }

  public static List<WebPage> findAll() {
    return findAll(null, null);
  }

  public static List<WebPage> findAll(WebPageSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("link");
    DataResult result = query(specification, constraints);
    return (List<WebPage>) result.getRecords();
  }

  /**
   * Retrieves all distinct tags from published web pages and items
   * @return List of unique tag strings, sorted alphabetically
   */
  public static List<String> findAllDistinctTags() {
    String SQL_QUERY = "SELECT DISTINCT tag FROM ( " +
        "SELECT jsonb_array_elements_text(tags) AS tag FROM web_pages WHERE tags IS NOT NULL AND enabled = true AND draft = false "
        +
        "UNION ALL " +
        "SELECT jsonb_array_elements_text(tags) AS tag FROM items WHERE tags IS NOT NULL " +
        ") AS all_tags " +
        "WHERE tag IS NOT NULL AND tag <> '' " +
        "ORDER BY tag";
    List<String> tags = new ArrayList<>();
    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
        ResultSet rs = pst.executeQuery()) {
      while (rs.next()) {
        String tag = rs.getString("tag");
        if (StringUtils.isNotBlank(tag)) {
          tags.add(tag);
        }
      }
    } catch (SQLException se) {
      LOG.error("findAllDistinctTags", se);
    }
    return tags;
  }

  public static WebPage save(WebPage record) {
    if (record.getId() > -1) {
      return update(record);
    }
    return add(record);
  }

  private static WebPage add(WebPage record) {
    String link = StringUtils.trimToNull(record.getLink());
    if (link != null) {
      link = link.toLowerCase();
    }
    SqlUtils insertValues = new SqlUtils()
        .add("link", link)
        .add("redirect_url", StringUtils.trimToNull(record.getRedirectUrl()))
        .add("page_title", StringUtils.trimToNull(record.getTitle()))
        .add("page_keywords", StringUtils.trimToNull(record.getKeywords()))
        .add("page_description", StringUtils.trimToNull(record.getDescription()))
        .add("draft", record.getDraft())
        .add("enabled", record.isEnabled())
        .add("searchable", record.isSearchable())
        .add("show_in_sitemap", record.getShowInSitemap())
        .add("created_by", record.getCreatedBy())
        .add("role_id_list", record.getRoleIdList())
        .add("page_xml", record.getPageXml())
        .add("draft_page_xml", StringUtils.trimToNull(record.getDraftPageXml()))
        .add("comments", record.getComments())
        .add("page_image_url", record.getImageUrl())
        .add("has_redirect", StringUtils.trimToNull(record.getRedirectUrl()) != null)
        .add("sitemap_priority", record.getSitemapPriority())
        .add("sitemap_changefreq", StringUtils.trimToNull(record.getSitemapChangeFrequency()));
    if (record.getTags() != null && record.getTags().length > 0) {
      insertValues.add(new SqlValue("tags", SqlValue.JSONB_TYPE, JsonCommand.toJsonArray(record.getTags())));
    }
    record.setId(DB.insertInto(TABLE_NAME, insertValues, PRIMARY_KEY));
    if (record.getId() == -1) {
      LOG.error("An id was not set!");
      return null;
    }
    return record;
  }

  private static WebPage update(WebPage record) {
    String link = StringUtils.trimToNull(record.getLink());
    if (link != null) {
      link = link.toLowerCase();
    }
    // Before the update, retrieve the existing link for this page in case it changed
    WebPage previousRecord = WebPageRepository.findById(record.getId());
    // Update the record
    SqlUtils updateValues = new SqlUtils()
        .add("link", link)
        .add("redirect_url", StringUtils.trimToNull(record.getRedirectUrl()))
        .add("page_title", StringUtils.trimToNull(record.getTitle()))
        .add("page_keywords", StringUtils.trimToNull(record.getKeywords()))
        .add("page_description", StringUtils.trimToNull(record.getDescription()))
        .add("draft", record.getDraft())
        .add("enabled", record.isEnabled())
        .add("searchable", record.isSearchable())
        .add("show_in_sitemap", record.getShowInSitemap())
        .add("modified", new Timestamp(System.currentTimeMillis()))
        .add("modified_by", record.getModifiedBy())
        .add("role_id_list", record.getRoleIdList())
        .add("page_xml", record.getPageXml())
        .add("draft_page_xml", StringUtils.trimToNull(record.getDraftPageXml()))
        .add("comments", record.getComments())
        .add("page_image_url", record.getImageUrl())
        .add("has_redirect", StringUtils.trimToNull(record.getRedirectUrl()) != null)
        .add("sitemap_priority", record.getSitemapPriority())
        .add("sitemap_changefreq", StringUtils.trimToNull(record.getSitemapChangeFrequency()));
    if (record.getTags() != null && record.getTags().length > 0) {
      updateValues.add(new SqlValue("tags", SqlValue.JSONB_TYPE, JsonCommand.toJsonArray(record.getTags())));
    } else {
      updateValues.add(new SqlValue("tags", SqlValue.JSONB_TYPE, null));
    }
    if (DB.update(TABLE_NAME, updateValues, DB.WHERE("web_page_id = ?", record.getId()))) {
      // Force the page(s) to re-cache
      if (previousRecord != null) {
        WebPageXmlLayoutCommand.removeCustomPage(previousRecord.getLink());
      }
      WebPageXmlLayoutCommand.removeCustomPage(record.getLink());
      return record;
    }
    LOG.error("The update failed!");
    return null;
  }

  public static void publish(WebPage record) {
    if (record.getId() == -1) {
      return;
    }
    // Handle publishing and making sure there is content to publish
    if (DB.update(
        TABLE_NAME,
        "page_xml = draft_page_xml, draft_page_xml = null, draft = false",
        DB.WHERE("draft_page_xml IS NOT NULL AND web_page_id = ?", record.getId()))) {
      // Force the page to re-cache
      WebPageXmlLayoutCommand.removeCustomPage(record.getLink());
    }
  }

  /** Mark the web page as modified and move it to a findable state if content is published on the page */
  public static void markAsModifiedAndFindable(WebPage record, long userId) {
    if (record.getId() == -1) {
      return;
    }
    boolean isFirstPublishAfterCreation = record.getCreated() != null && record.getModified() != null
        && record.getModified().equals(record.getCreated());

    long modified = System.currentTimeMillis();
    SqlUtils updateValues = new SqlUtils()
        .add("modified", new Timestamp(modified))
        .add("modified_by", userId);
    if (isFirstPublishAfterCreation) {
      updateValues
          .add("searchable", true)
          .add("show_in_sitemap", true);
    }
    DB.update(TABLE_NAME, updateValues, DB.WHERE("web_page_id = ?", record.getId()));
    // Now update the record for additional workflows
    record.setModifiedBy(userId);
    record.setModified(new Timestamp(modified));
    if (isFirstPublishAfterCreation) {
      record.setSearchable(true);
      record.setShowInSitemap(true);
    }
  }

  public static void removeDraft(WebPage record) {
    if (record == null || record.getId() == -1) {
      return;
    }
    String setValues = "draft_page_xml = null, draft = false";
    if (DB.update(TABLE_NAME, setValues, DB.WHERE("web_page_id = ?", record.getId()))) {
      // Force the page to re-cache
      WebPageXmlLayoutCommand.removeCustomPage(record.getLink());
    }
  }

  public static boolean remove(WebPage record) {
    if (record == null || record.getId() == -1) {
      return false;
    }
    try (Connection connection = DB.getConnection();
        AutoStartTransaction a = new AutoStartTransaction(connection);
        AutoRollback transaction = new AutoRollback(connection)) {
      // Delete the references
      PageFileRepository.removeAll(connection, record);
      WebPageHierarchyRepository.remove(connection, record);
      WebPageVersionRepository.removeAll(connection, record);
      // Delete the record
      DB.deleteFrom(connection, TABLE_NAME, DB.WHERE("web_page_id = ?", record.getId()));
      // Finish transaction
      transaction.commit();
      // Force the page to re-cache
      WebPageXmlLayoutCommand.removeCustomPage(record.getLink());
      return true;
    } catch (SQLException se) {
      LOG.error("SQLException: " + se.getMessage());
    }
    return false;
  }

  public static void updatePageText(long id, String generatedText) {
    if (id == -1) {
      return;
    }
    SqlUtils updateValues = new SqlUtils()
        .add("page_text", generatedText);
    DB.update(TABLE_NAME, updateValues, DB.WHERE("web_page_id = ?", id));
  }

  private static WebPage buildRecord(ResultSet rs) {
    try {
      WebPage record = new WebPage();
      record.setId(rs.getLong("web_page_id"));
      record.setLink(rs.getString("link"));
      record.setRedirectUrl(rs.getString("redirect_url"));
      record.setTitle(rs.getString("page_title"));
      record.setKeywords(rs.getString("page_keywords"));
      record.setDescription(rs.getString("page_description"));
      record.setDraft(rs.getBoolean("draft"));
      record.setEnabled(rs.getBoolean("enabled"));
      record.setCreatedBy(rs.getLong("created_by"));
      record.setCreated(rs.getTimestamp("created"));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(rs.getLong("modified_by"));
      record.setRoleIdList(rs.getString("role_id_list"));
      record.setPageXml(rs.getString("page_xml"));
      record.setDraftPageXml(rs.getString("draft_page_xml"));
      record.setComments(rs.getString("comments"));
      record.setImageUrl(rs.getString("page_image_url"));
      record.setSearchable(rs.getBoolean("searchable"));
      record.setShowInSitemap(rs.getBoolean("show_in_sitemap"));
      record.setSitemapPriority(rs.getBigDecimal("sitemap_priority"));
      record.setSitemapChangeFrequency(rs.getString("sitemap_changefreq"));
      record.setTags(JsonCommand.fromJsonArray(rs.getString("tags")));
      if (DB.hasColumn(rs, "highlight")) {
        record.setHighlight(rs.getString("highlight"));
      } else {
        record.setHighlight(StringUtils.trimToNull(TextCommand.trim(rs.getString("page_text"), 512, true)));
      }
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  public static List<WebPage> findByContentUniqueId(String contentUniqueId) {
    if (StringUtils.isBlank(contentUniqueId)) {
      return new ArrayList<>();
    }
    // Query for pages where pageXml contains the uniqueId reference
    SqlWhere where = DB.WHERE();
    where.AND("(page_xml LIKE ? OR draft_page_xml LIKE ?)",
        new String[] { "%" + contentUniqueId + "%", "%" + contentUniqueId + "%" });
    return (List<WebPage>) DB.selectAllFrom(
        TABLE_NAME,
        where,
        new DataConstraints(),
        WebPageRepository::buildRecord).getRecords();
  }

  public static List<WebPage> findIndirectlyAffectedPagesByContentUniqueId(String contentUniqueId) {
    if (StringUtils.isBlank(contentUniqueId)) {
      return new ArrayList<>();
    }

    ArrayDeque<ContentReferenceNode> queue = new ArrayDeque<>();
    Set<String> visitedContentUniqueIds = new HashSet<>();
    Map<Long, WebPage> pageById = new HashMap<>();

    queue.add(new ContentReferenceNode(contentUniqueId, 0));
    visitedContentUniqueIds.add(contentUniqueId);

    int traversedNodes = 0;
    while (!queue.isEmpty() && canContinueTraversal(traversedNodes, pageById.size())) {
      ContentReferenceNode current = queue.poll();
      traversedNodes++;
      if (current.depth < MAX_INDIRECT_REFERENCE_DEPTH) {
        processParentReferences(current, queue, visitedContentUniqueIds, pageById);
      }
    }

    if (traversedNodes >= MAX_CONTENT_REFERENCE_NODES) {
      LOG.warn("Reached indirect content reference traversal limit for content uniqueId: " + contentUniqueId);
    }
    if (pageById.size() >= MAX_INDIRECT_PAGES) {
      LOG.warn("Reached indirect affected page limit for content uniqueId: " + contentUniqueId);
    }

    return new ArrayList<>(pageById.values());
  }

  private static boolean canContinueTraversal(int traversedNodes, int pageCount) {
    return traversedNodes < MAX_CONTENT_REFERENCE_NODES && pageCount < MAX_INDIRECT_PAGES;
  }

  private static void processParentReferences(ContentReferenceNode current,
      ArrayDeque<ContentReferenceNode> queue,
      Set<String> visitedContentUniqueIds,
      Map<Long, WebPage> pageById) {
    List<String> parentContentUniqueIds = findContentUniqueIdsReferencing(current.uniqueId);
    for (String parentUniqueId : parentContentUniqueIds) {
      if (canVisitParent(parentUniqueId, visitedContentUniqueIds) && pageById.size() < MAX_INDIRECT_PAGES) {
        visitedContentUniqueIds.add(parentUniqueId);
        addPagesByContentUniqueId(parentUniqueId, pageById);
        if (pageById.size() < MAX_INDIRECT_PAGES) {
          queue.add(new ContentReferenceNode(parentUniqueId, current.depth + 1));
        }
      }
    }
  }

  private static boolean canVisitParent(String parentUniqueId, Set<String> visitedContentUniqueIds) {
    return StringUtils.isNotBlank(parentUniqueId) && !visitedContentUniqueIds.contains(parentUniqueId);
  }

  private static void addPagesByContentUniqueId(String contentUniqueId, Map<Long, WebPage> pageById) {
    for (WebPage page : findByContentUniqueId(contentUniqueId)) {
      if (pageById.size() >= MAX_INDIRECT_PAGES) {
        return;
      }
      if (page != null && page.getId() > -1 && !pageById.containsKey(page.getId())) {
        pageById.put(page.getId(), page);
      }
    }
  }

  private static List<String> findContentUniqueIdsReferencing(String contentUniqueId) {
    List<String> referencedByUniqueIds = new ArrayList<>();
    String directivePattern = "%${uniqueId:" + contentUniqueId + "}%";
    String sql = "SELECT DISTINCT content_unique_id FROM content " +
        "WHERE content_unique_id <> ? AND (content LIKE ? OR draft_content LIKE ?)";

    try (Connection connection = DB.getConnection();
        PreparedStatement pst = connection.prepareStatement(sql)) {
      int i = 0;
      pst.setString(++i, contentUniqueId);
      pst.setString(++i, directivePattern);
      pst.setString(++i, directivePattern);
      try (ResultSet rs = pst.executeQuery()) {
        while (rs.next()) {
          referencedByUniqueIds.add(rs.getString("content_unique_id"));
        }
      }
    } catch (SQLException se) {
      LOG.error("findContentUniqueIdsReferencing", se);
    }

    return referencedByUniqueIds;
  }

  private static class ContentReferenceNode {
    private final String uniqueId;
    private final int depth;

    private ContentReferenceNode(String uniqueId, int depth) {
      this.uniqueId = uniqueId;
      this.depth = depth;
    }
  }
}
