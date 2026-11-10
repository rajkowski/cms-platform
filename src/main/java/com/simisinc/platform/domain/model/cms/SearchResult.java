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

package com.simisinc.platform.domain.model.cms;

import java.sql.Timestamp;
import java.util.Map;

import com.simisinc.platform.domain.model.Entity;
import com.zeroio.platform.domain.model.cms.SearchCriteria;

/**
 * A search result to be included with the search results
 *
 * @author matt rajkowski
 * @created 8/28/19 3:19 PM
 */
public class SearchResult extends Entity {

  private long recordId = -1;
  private String type = null;
  private String link = null;
  private String highlight = null;
  private String pageTitle = null;
  private String pageDescription = null;
  private String sourceText = null;
  private String htmlExcerpt = null;
  private long relatedCategoryId = -1L;
  private String category = null;
  private String[] tags = null;
  private boolean titleLinkEnabled = true;
  private String actionLink = null;
  private String actionLabel = null;
  private long modifiedBy = -1;
  private Timestamp created = null;
  private Timestamp modified = null;
  private Map<String, String> customFields = null;

  public SearchResult() {
  }

  public long getRecordId() {
    return recordId;
  }

  public void setRecordId(long recordId) {
    this.recordId = recordId;
  }

  /** The kind of content, one of the SearchCriteria type constants */
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  /** The raw excerpt with ${b} and ${/b} match markers, not yet escaped */
  public String getHighlight() {
    return highlight;
  }

  public void setHighlight(String highlight) {
    this.highlight = highlight;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getPageTitle() {
    return pageTitle;
  }

  public void setPageTitle(String pageTitle) {
    this.pageTitle = pageTitle;
  }

  public String getPageDescription() {
    return pageDescription;
  }

  public void setPageDescription(String pageDescription) {
    this.pageDescription = pageDescription;
  }

  public String getSourceText() {
    return sourceText;
  }

  public void setSourceText(String sourceText) {
    this.sourceText = sourceText;
  }

  public String getHtmlExcerpt() {
    return htmlExcerpt;
  }

  public void setHtmlExcerpt(String htmlExcerpt) {
    this.htmlExcerpt = htmlExcerpt;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public long getRelatedCategoryId() {
    return relatedCategoryId;
  }

  public void setRelatedCategoryId(long relatedCategoryId) {
    this.relatedCategoryId = relatedCategoryId;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isTitleLinkEnabled() {
    return titleLinkEnabled;
  }

  public void setTitleLinkEnabled(boolean titleLinkEnabled) {
    this.titleLinkEnabled = titleLinkEnabled;
  }

  public String getActionLink() {
    return actionLink;
  }

  public void setActionLink(String actionLink) {
    this.actionLink = actionLink;
  }

  public String getActionLabel() {
    return actionLabel;
  }

  public void setActionLabel(String actionLabel) {
    this.actionLabel = actionLabel;
  }

  public long getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(long modifiedBy) {
    this.modifiedBy = modifiedBy;
  }

  public Timestamp getCreated() {
    return created;
  }

  public void setCreated(Timestamp created) {
    this.created = created;
  }

  public Timestamp getModified() {
    return modified;
  }

  public void setModified(Timestamp modified) {
    this.modified = modified;
  }

  public Map<String, String> getCustomFields() {
    return customFields;
  }

  public void setCustomFields(Map<String, String> customFields) {
    this.customFields = customFields;
  }

  /** The label shown beside a result */
  public String getTypeLabel() {
    if (SearchCriteria.POSTS.equals(type)) {
      return "Blog post";
    }
    if (SearchCriteria.CALENDAR.equals(type)) {
      return "Event";
    }
    if (SearchCriteria.ATTACHMENTS.equals(type)) {
      return "Attachment";
    }
    if (SearchCriteria.RESOURCES.equals(type)) {
      return "Resource";
    }
    if (SearchCriteria.RESOURCE_FILES.equals(type)) {
      return "Attachment";
    }
    if (SearchCriteria.PAGES.equals(type)) {
      return "Web page";
    }
    return null;
  }

}
