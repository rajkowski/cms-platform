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

package com.simisinc.platform.infrastructure.persistence.cms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.github.rajkowski.database.CastType;
import com.github.rajkowski.database.ConditionGroup;
import com.github.rajkowski.database.DB;
import com.github.rajkowski.database.DataConstraints;
import com.github.rajkowski.database.Select;
import com.simisinc.platform.application.admin.LoadSitePropertyCommand;
import com.simisinc.platform.application.cms.UrlCommand;
import com.simisinc.platform.application.json.JsonCommand;
import com.simisinc.platform.domain.model.cms.SearchResult;
import com.simisinc.platform.presentation.controller.DataConstants;
import com.simisinc.platform.presentation.controller.UserSession;
import com.zeroio.platform.domain.model.cms.SearchCriteria;

/**
 * Searches pages, blog posts, calendar events, attachments and items with a single ranked query over each table's full text tsv
 *
 * @author matt rajkowski
 * @created 10/15/26 8:00 AM
 */
public class UnifiedSearchRepository {

  private static Log LOG = LogFactory.getLog(UnifiedSearchRepository.class);

  private static final String VIEW_NAME = "unified_search";
  private static final String HEADLINE_OPTIONS = "StartSel=${b}, StopSel=${/b}, MaxWords=30, MinWords=15, ShortWord=3, HighlightAll=FALSE, MaxFragments=2, FragmentDelimiter=\" ... \"";

  public static List<SearchResult> findAll(UnifiedSearchSpecification specification, DataConstraints constraints) {
    if (specification == null) {
      specification = new UnifiedSearchSpecification();
    }
    String term = StringUtils.trimToNull(specification.getSearchTerm());

    Select select = DB.SELECT("result_type", "result_id", "title", "description", "link", "tags", "modified", "modified_by",
        "file_name", "file_type", "mime_type", "extension");
    if (term == null) {
      select.SELECT("CAST(NULL AS TEXT) AS highlight");
      select.SELECT("CAST(0 AS DOUBLE PRECISION) AS rank");
    } else {
      String escaped = term.toLowerCase()
          .replace("!", "!!")
          .replace("%", "!%")
          .replace("_", "!_")
          .replace("[", "![");
      select.SELECT("ts_headline('english', headline_source, websearch_to_tsquery('english', ?), '" + HEADLINE_OPTIONS
          + "') AS highlight", (Object[]) new Object[] { term });
      // ts_rank_cd normalization 32 keeps ranks from different tables comparable
      select.SELECT("(ts_rank_cd(tsv, websearch_to_tsquery('english', ?), 32)"
          + " + CASE WHEN LOWER(title) LIKE ? ESCAPE '!' THEN 1.0 ELSE 0.0 END"
          + " + CASE WHEN LOWER(title) LIKE ? ESCAPE '!' THEN 0.5 ELSE 0.0 END) AS rank",
          (Object[]) new Object[] { term, escaped + "%", "%" + escaped + "%" });
    }
    select.FROM(VIEW_NAME).WHERE();

    if (term != null) {
      select.AND("tsv @@ websearch_to_tsquery('english', ?)", term);
    }
    if (hasValues(specification.getTypes())) {
      select.AND("result_type = ANY(?)", specification.getTypes(), CastType.ARRAY);
    }
    addVisibilityFilters(select, specification);
    addCommonFilters(select, specification);

    select.ORDER_BY("rank DESC, modified DESC, result_type, result_id");
    if (constraints != null) {
      select.PAGING(constraints);
    }
    return select.returnDataResult(UnifiedSearchRepository::buildRecord).getRecords();
  }

  private static boolean hasValues(String[] values) {
    return values != null && values.length > 0;
  }

  /** Each type's rules only apply to rows of that type */
  private static void addVisibilityFilters(Select select, UnifiedSearchSpecification specification) {
    select.AND("(result_type <> ? OR is_listed = ?)", SearchCriteria.PAGES, true);
    if (!specification.isContentManager()) {
      select.AND("(result_type <> ? OR is_public = ?)", SearchCriteria.PAGES, true);
    }
    select.AND("(result_type <> ? OR is_listed = ?)", SearchCriteria.POSTS, true);
    select.AND("(result_type <> ? OR start_date >= ?)", SearchCriteria.CALENDAR, startOfToday());
    if (!specification.isDataManager()) {
      select.AND("(result_type <> ? OR is_public = ?)", SearchCriteria.RESOURCES, true);
    }

    long userId = specification.getForUserId();
    if (userId == DataConstants.UNDEFINED) {
      return;
    }
    boolean isGuest = userId == UserSession.GUEST_ID;
    // Same folder rules as ItemFileSpecification, applied to all users
    if (isGuest) {
      select.AND(
          "(result_type <> ? OR EXISTS (SELECT 1 FROM item_folders WHERE item_folders.folder_id = unified_search.folder_id AND item_folders.allows_guests = true))",
          SearchCriteria.RESOURCE_FILES);
    } else {
      select.AND(
          "(result_type <> ? OR EXISTS (SELECT 1 FROM item_folders WHERE item_folders.folder_id = unified_search.folder_id AND (allows_guests = true OR (has_allowed_groups = true AND EXISTS (SELECT 1 FROM item_folder_groups WHERE item_folder_groups.folder_id = item_folders.folder_id AND view_all = true AND EXISTS (SELECT 1 FROM user_groups WHERE user_groups.group_id = item_folder_groups.group_id AND user_id = ?))))))",
          SearchCriteria.RESOURCE_FILES, userId);
    }
    if (!specification.isContentManager()) {
      if (isGuest) {
        select.AND(
            "(result_type <> ? OR EXISTS (SELECT 1 FROM folders WHERE folders.folder_id = unified_search.folder_id AND folders.allows_guests = true))",
            SearchCriteria.ATTACHMENTS);
      } else {
        select.AND(
            "(result_type <> ? OR EXISTS (SELECT 1 FROM folders WHERE folders.folder_id = unified_search.folder_id AND (allows_guests = true OR (has_allowed_groups = true AND EXISTS (SELECT 1 FROM folder_groups WHERE folder_groups.folder_id = folders.folder_id AND view_all = true AND EXISTS (SELECT 1 FROM user_groups WHERE user_groups.group_id = folder_groups.group_id AND user_id = ?))))))",
            SearchCriteria.ATTACHMENTS, userId);
      }
    }
    if (isGuest) {
      select.AND(
          "(result_type <> ? OR EXISTS (SELECT 1 FROM collections WHERE collections.collection_id = unified_search.collection_id AND collections.allows_guests = true))",
          SearchCriteria.RESOURCES);
    } else {
      select.AND(
          "(result_type <> ? OR EXISTS (SELECT 1 FROM collections WHERE collections.collection_id = unified_search.collection_id AND (collections.allows_guests = true OR (has_allowed_groups = true AND EXISTS (SELECT 1 FROM collection_groups WHERE collection_groups.collection_id = collections.collection_id AND view_all = true AND EXISTS (SELECT 1 FROM user_groups WHERE user_groups.group_id = collection_groups.group_id AND user_id = ?)) OR EXISTS (SELECT 1 FROM members WHERE unified_search.result_id = members.item_id AND user_id = ? AND approved IS NOT NULL)))))",
          SearchCriteria.RESOURCES, userId, userId);
    }
  }

  // Blog posts and events have no tags, so a tag filter naturally excludes them
  private static void addCommonFilters(Select select, UnifiedSearchSpecification specification) {
    if (hasValues(specification.getRegionTags())) {
      ConditionGroup condition = ConditionGroup.build("tags", specification.getRegionTags(), ConditionGroup.ANY);
      if (condition != null) {
        select.AND(condition.sql(), (Object[]) condition.values());
      }
    }
    if (hasValues(specification.getFilterTags())) {
      ConditionGroup condition = ConditionGroup.build("tags", specification.getFilterTags(), ConditionGroup.ALL);
      if (condition != null) {
        select.AND(condition.sql(), (Object[]) condition.values());
      }
    }
    if (specification.getModifiedAfter() != null) {
      select.AND("modified >= ?", specification.getModifiedAfter());
    }
    if (specification.getModifiedBefore() != null) {
      select.AND("modified <= ?", specification.getModifiedBefore());
    }
    long[] userIds = specification.getModifiedByUserIds();
    if (userIds != null && userIds.length > 0) {
      StringBuilder userCondition = new StringBuilder("modified_by IN (");
      Object[] values = new Object[userIds.length];
      for (int i = 0; i < userIds.length; i++) {
        if (i > 0) {
          userCondition.append(", ");
        }
        userCondition.append("?");
        values[i] = userIds[i];
      }
      userCondition.append(")");
      select.AND(userCondition.toString(), values);
    }
  }

  private static Timestamp startOfToday() {
    String timezone = LoadSitePropertyCommand.loadByName("site.timezone");
    ZoneId zoneId = StringUtils.isBlank(timezone) ? ZoneId.systemDefault() : ZoneId.of(timezone);
    return Timestamp.valueOf(ZonedDateTime.now(zoneId).toLocalDate().atStartOfDay());
  }

  private static SearchResult buildRecord(ResultSet rs) {
    try {
      SearchResult record = new SearchResult();
      String type = rs.getString("result_type");
      record.setType(type);
      record.setPageTitle(rs.getString("title"));
      record.setPageDescription(StringUtils.trimToNull(rs.getString("description")));
      record.setHighlight(StringUtils.trimToNull(rs.getString("highlight")));
      record.setTags(JsonCommand.fromJsonArray(rs.getString("tags")));
      record.setModified(rs.getTimestamp("modified"));
      record.setModifiedBy(rs.getLong("modified_by"));

      String link = rs.getString("link");
      if (SearchCriteria.ATTACHMENTS.equals(type)) {
        setAttachmentLinks(record, rs, "", link + "/" + UrlCommand.encodeUri(rs.getString("file_name")));
      } else if (SearchCriteria.RESOURCE_FILES.equals(type)) {
        // Link is /show/{uniqueId}/assets/{webPath-id}
        int index = link.indexOf("/assets/");
        String url = link.substring(index + "/assets/".length()) + "/" + UrlCommand.encodeUri(rs.getString("file_name"));
        setAttachmentLinks(record, rs, link.substring(0, index), url);
      } else {
        record.setLink(link);
      }
      return record;
    } catch (SQLException se) {
      LOG.error("buildRecord", se);
      return null;
    }
  }

  private static void setAttachmentLinks(SearchResult record, ResultSet rs, String prefix, String url) throws SQLException {
    String extension = rs.getString("extension");
    String fileType = StringUtils.defaultString(rs.getString("file_type"));
    String mimeType = StringUtils.defaultString(rs.getString("mime_type"));
    boolean isDiagram = "drawio".equalsIgnoreCase(extension) || "diagram".equalsIgnoreCase(fileType);
    boolean isViewable = mimeType.startsWith("image/") || mimeType.startsWith("video/")
        || "application/pdf".equalsIgnoreCase(mimeType) || "image".equalsIgnoreCase(fileType)
        || "video".equalsIgnoreCase(fileType) || "pdf".equalsIgnoreCase(fileType);
    String path;
    if (isDiagram && prefix.isEmpty()) {
      path = "/assets/drawio/" + url;
    } else if (isViewable) {
      path = prefix + "/assets/view/" + url;
    } else {
      path = prefix + "/assets/file/" + url;
    }
    record.setLink(path);
    record.setActionLink(path);
    if (isDiagram || isViewable) {
      record.setTitleLinkEnabled(true);
      record.setActionLabel("View attachment");
    } else {
      record.setTitleLinkEnabled(false);
      record.setActionLabel("Download attachment");
    }
  }
}
