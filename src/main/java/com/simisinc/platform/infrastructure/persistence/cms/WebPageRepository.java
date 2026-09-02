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

import com.github.rajkowski.database.AutoRollback;
import com.github.rajkowski.database.AutoStartTransaction;
import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.ConditionGroup;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.DataResult;
import com.github.rajkowski.database.Insert;
import com.github.rajkowski.database.Select;
import com.github.rajkowski.database.Update;
import com.simisinc.platform.application.cms.TextCommand;
import com.simisinc.platform.application.cms.WebPageXmlLayoutCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.WebPage;
import com.simisinc.platform.presentation.controller.DataConstants;
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

  private static DataResult<WebPage> query(WebPageSpecification specification, DataConstraints constraints) {
    Select select = DB.SELECT("web_pages.*").FROM(TABLE_NAME).WHERE();
    if (specification != null) {
      if (StringUtils.isNotBlank(specification.getLink())) {
        select.AND("LOWER(link) = ?", specification.getLink().trim().toLowerCase());
      }
      if (specification.getEnabled() != DataConstants.UNDEFINED) {
        select.AND("enabled = ?", specification.getEnabled() == DataConstants.TRUE);
      }
      if (specification.getDraft() != DataConstants.UNDEFINED) {
        select.AND("draft = ?", specification.getDraft() == DataConstants.TRUE);
      }
      if (specification.getSearchable() != DataConstants.UNDEFINED) {
        select.AND("searchable = ?", specification.getSearchable() == DataConstants.TRUE);
      }
      if (specification.getInSitemap() != DataConstants.UNDEFINED) {
        select.AND("show_in_sitemap = ?", specification.getInSitemap() == DataConstants.TRUE);
      }
      if (specification.getHasRedirect() != DataConstants.UNDEFINED) {
        select.AND("has_redirect = ?", specification.getHasRedirect() == DataConstants.TRUE);
      }
      if (specification.getRegionTags() != null && specification.getRegionTags().length > 0) {
        ConditionGroup condition = ConditionGroup.build("web_pages.tags", specification.getRegionTags(), ConditionGroup.ANY);
        if (condition != null) {
          select.AND(condition.sql(), (Object[]) condition.values());
        }
      }
      if (specification.getFilterTags() != null && specification.getFilterTags().length > 0) {
        ConditionGroup filterCondition = ConditionGroup.build("web_pages.tags", specification.getFilterTags(), ConditionGroup.ALL);
        if (filterCondition != null) {
          select.AND(filterCondition.sql(), (Object[]) filterCondition.values());
        }
      }
      if (specification.getExcludeTags() != null && specification.getExcludeTags().length > 0) {
        ConditionGroup excludeCondition = ConditionGroup.build("web_pages.tags", specification.getExcludeTags(),
            ConditionGroup.NOT_ANY);
        if (excludeCondition != null) {
          select.AND(excludeCondition.sql(), (Object[]) excludeCondition.values());
        }
      }
      if (specification.getModifiedAfter() != null) {
        select.AND("web_pages.modified >= ?", specification.getModifiedAfter());
      }
      if (specification.getModifiedBefore() != null) {
        select.AND("web_pages.modified <= ?", specification.getModifiedBefore());
      }
      if (specification.getModifiedByUserIds() != null && specification.getModifiedByUserIds().length > 0) {
        long[] modifiedByUserIds = specification.getModifiedByUserIds();
        StringBuilder userCondition = new StringBuilder("web_pages.modified_by IN (");
        Object[] values = new Object[modifiedByUserIds.length];
        for (int i = 0; i < modifiedByUserIds.length; i++) {
          if (i > 0) {
            userCondition.append(", ");
          }
          userCondition.append("?");
          values[i] = modifiedByUserIds[i];
        }
        userCondition.append(")");
        select.AND(userCondition.toString(), values);
      }
      if (StringUtils.isNotBlank(specification.getSearchTerm())) {
        String term = specification.getSearchTerm().trim().toLowerCase();
        select.SELECT(
            "ts_headline('english', page_text, websearch_to_tsquery('web_page_stem', ?), 'StartSel=${b}, StopSel=${/b}, MaxWords=40, MinWords=30, ShortWord=3, HighlightAll=FALSE, MaxFragments=2, FragmentDelimiter=\" ... \"') AS highlight",
            (Object[]) new Object[] { term });
        select.SELECT(
            "(ts_rank_cd(web_pages.tsv, websearch_to_tsquery('web_page_stem', ?)) + CASE WHEN LOWER(web_pages.page_title) LIKE LOWER(?) THEN 200.0 ELSE 0.0 END + CASE WHEN LOWER(web_pages.page_title) LIKE LOWER(?) THEN 100.0 ELSE 0.0 END) AS rank",
            (Object[]) new Object[] { term, term + "%", "%" + term + "%" });
        select.AND("web_pages.tsv @@ websearch_to_tsquery('web_page_stem', ?)", term);
        select.ORDER_BY("rank DESC, web_pages.modified DESC");
      }
    }
    if (constraints != null) {
      select.WITH(constraints);
    }
    return select.returnDataResult(WebPageRepository::buildRecord);
  }

  public static WebPage findById(long id) {
    if (id == -1) {
      return null;
    }
    return DB.SELECT("web_pages.*")
        .FROM(TABLE_NAME)
        .WHERE("web_page_id = ?", id)
        .returnRecord(WebPageRepository::buildRecord);
  }

  public static WebPage findByLink(String link) {
    if (StringUtils.isBlank(link)) {
      return null;
    }
    return DB.SELECT("web_pages.*")
        .FROM(TABLE_NAME)
        .WHERE("LOWER(link) = ?", link.trim().toLowerCase())
        .returnRecord(WebPageRepository::buildRecord);
  }

  public static List<WebPage> findAll() {
    return findAll(null, null);
  }

  public static List<WebPage> findAll(WebPageSpecification specification, DataConstraints constraints) {
    if (constraints == null) {
      constraints = new DataConstraints();
    }
    constraints.setDefaultColumnToSortBy("link");
    return query(specification, constraints).getRecords();
  }

  /**
   * Retrieves all distinct tags from published web pages and items
   * @return List of unique tag strings, sorted alphabetically
   */
  public static List<String> findAllDistinctTags() {
    Select subquery = DB.SELECT("jsonb_array_elements_text(tags) AS tag")
        .FROM("web_pages")
        .WHERE("tags IS NOT NULL")
        .AND("enabled = ?", true)
        .AND("draft = ?", false)
        .UNION_ALL(DB.SELECT("jsonb_array_elements_text(tags) AS tag")
            .FROM("items")
            .WHERE("tags IS NOT NULL"));

    List<String> tags = DB.SELECT("DISTINCT tag")
        .FROM(subquery)
        .AS("all_tags")
        .WHERE("tag IS NOT NULL")
        .AND("tag <> ?", "")
        .ORDER_BY("tag")
        .returnList(rs -> rs.getString("tag"));

    return tags.stream().filter(StringUtils::isNotBlank).toList();
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

    Insert insert = DB.INSERT().INTO(TABLE_NAME)
        .FIELD("link", link)
        .FIELD("redirect_url", StringUtils.trimToNull(record.getRedirectUrl()))
        .FIELD("page_title", StringUtils.trimToNull(record.getTitle()))
        .FIELD("page_keywords", StringUtils.trimToNull(record.getKeywords()))
        .FIELD("page_description", StringUtils.trimToNull(record.getDescription()))
        .FIELD("draft", record.getDraft())
        .FIELD("enabled", record.isEnabled())
        .FIELD("searchable", record.isSearchable())
        .FIELD("show_in_sitemap", record.getShowInSitemap())
        .FIELD("created_by", record.getCreatedBy())
        .FIELD("modified_by", record.getModifiedBy())
        .FIELD("role_id_list", record.getRoleIdList())
        .FIELD("page_xml", record.getPageXml())
        .FIELD("draft_page_xml", StringUtils.trimToNull(record.getDraftPageXml()))
        .FIELD("comments", record.getComments())
        .FIELD("page_image_url", record.getImageUrl())
        .FIELD("has_redirect", StringUtils.trimToNull(record.getRedirectUrl()) != null)
        .FIELD("sitemap_priority", record.getSitemapPriority())
        .FIELD("sitemap_changefreq", StringUtils.trimToNull(record.getSitemapChangeFrequency()));
    if (record.getTags() != null && record.getTags().length > 0) {
      insert.FIELD("tags", JsonCommand.toJsonArray(record.getTags()), CastType.JSONB);
    }
    record.setId(insert.execute());
    if (record.getId() == Insert.NO_GENERATED_KEY) {
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

    Update update = DB.UPDATE(TABLE_NAME)
        .SET("link", link)
        .SET("redirect_url", StringUtils.trimToNull(record.getRedirectUrl()))
        .SET("page_title", StringUtils.trimToNull(record.getTitle()))
        .SET("page_keywords", StringUtils.trimToNull(record.getKeywords()))
        .SET("page_description", StringUtils.trimToNull(record.getDescription()))
        .SET("draft", record.getDraft())
        .SET("enabled", record.isEnabled())
        .SET("searchable", record.isSearchable())
        .SET("show_in_sitemap", record.getShowInSitemap())
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("modified_by", record.getModifiedBy())
        .SET("role_id_list", record.getRoleIdList())
        .SET("page_xml", record.getPageXml())
        .SET("draft_page_xml", StringUtils.trimToNull(record.getDraftPageXml()))
        .SET("comments", record.getComments())
        .SET("page_image_url", record.getImageUrl())
        .SET("has_redirect", StringUtils.trimToNull(record.getRedirectUrl()) != null)
        .SET("sitemap_priority", record.getSitemapPriority())
        .SET("sitemap_changefreq", StringUtils.trimToNull(record.getSitemapChangeFrequency()));
    if (record.getTags() != null && record.getTags().length > 0) {
      update.SET("tags", JsonCommand.toJsonArray(record.getTags()), CastType.JSONB);
    } else {
      update.SET("tags", (String) null, CastType.JSONB);
    }
    update.WHERE("web_page_id = ?", record.getId());
    if (update.execute().booleanValue()) {
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
    if (DB.UPDATE(TABLE_NAME)
        .SET("page_xml = draft_page_xml")
        .SET("draft_page_xml = null")
        .SET("draft = false")
        .WHERE("draft_page_xml IS NOT NULL AND web_page_id = ?", record.getId())
        .execute().booleanValue()) {
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
    Update update = DB.UPDATE(TABLE_NAME)
        .SET("modified", new Timestamp(modified))
        .SET("modified_by", userId);
    if (isFirstPublishAfterCreation) {
      update.SET("searchable", true)
          .SET("show_in_sitemap", true);
    }
    update.WHERE("web_page_id = ?", record.getId()).execute();
    // Now update the record for additional workflows
    record.setModifiedBy(userId);
    record.setModified(new Timestamp(modified));
    if (isFirstPublishAfterCreation) {
      record.setSearchable(true);
      record.setShowInSitemap(true);
    }
  }

  public static boolean archivePage(WebPage record, long userId) {
    if (record == null || record.getId() == -1) {
      return false;
    }
    // Explicitly set enabled to false (archived)
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("enabled", false)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("modified_by", userId)
        .WHERE("web_page_id = ?", record.getId())
        .execute();
    if (updated) {
      record.setEnabled(false);
      record.setModifiedBy(userId);
      // Note: We do NOT remove the page from cache when archiving
      // The page layout/content hasn't changed, only the enabled status
      // This allows admins/content-managers to still view the archived page
    }
    return updated;
  }

  public static boolean unarchivePage(WebPage record, long userId) {
    if (record == null || record.getId() == -1) {
      return false;
    }
    // Explicitly set enabled to true (unarchived)
    boolean updated = DB.UPDATE(TABLE_NAME)
        .SET("enabled", true)
        .SET("modified", new Timestamp(System.currentTimeMillis()))
        .SET("modified_by", userId)
        .WHERE("web_page_id = ?", record.getId())
        .execute();
    if (updated) {
      record.setEnabled(true);
      record.setModifiedBy(userId);
      // Note: We do NOT remove the page from cache when unarchiving
      // The page layout/content hasn't changed, only the enabled status
    }
    return updated;
  }

  public static void removeDraft(WebPage record) {
    if (record == null || record.getId() == -1) {
      return;
    }
    if (DB.UPDATE(TABLE_NAME)
        .SET("draft_page_xml", (String) null)
        .SET("draft", false)
        .WHERE("web_page_id = ?", record.getId())
        .execute()) {
      // Forces the page to re-cache
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
      DB.DELETE()
          .FROM(TABLE_NAME)
          .WHERE("web_page_id = ?", record.getId())
          .execute(connection);
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
    DB.UPDATE(TABLE_NAME)
        .SET("page_text", generatedText)
        .WHERE("web_page_id = ?", id)
        .execute();
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
    return DB.SELECT().FROM(TABLE_NAME)
        .WHERE("(page_xml LIKE ? OR draft_page_xml LIKE ?)", "%" + contentUniqueId + "%", "%" + contentUniqueId + "%")
        .returnList(WebPageRepository::buildRecord);
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
