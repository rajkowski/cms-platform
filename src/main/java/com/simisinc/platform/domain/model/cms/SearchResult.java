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

import com.simisinc.platform.domain.model.Entity;
import java.sql.Timestamp;

/**
 * A search result to be included with the search results
 *
 * @author matt rajkowski
 * @created 8/28/19 3:19 PM
 */
public class SearchResult extends Entity {

  private String link = null;
  private String pageTitle = null;
  private String pageDescription = null;
  private String htmlExcerpt = null;
  private String[] tags = null;
  private boolean titleLinkEnabled = true;
  private String actionLink = null;
  private String actionLabel = null;
  private long modifiedBy = -1;
  private Timestamp modified = null;

  public SearchResult() {
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

  public Timestamp getModified() {
    return modified;
  }

  public void setModified(Timestamp modified) {
    this.modified = modified;
  }
}
